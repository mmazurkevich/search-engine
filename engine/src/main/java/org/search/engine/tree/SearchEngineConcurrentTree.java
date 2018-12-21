package org.search.engine.tree;

import gnu.trove.set.hash.TIntHashSet;
import org.search.engine.tree.util.CharSequencesUtil;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Implementation of concurrent radix tree which store char sequences in the nodes and
 * the list of values with unique identifiers of the indexed entities. Tree contains the lock
 * for all modification operations. So get operation are not blocked but others wait until lock
 * will be retrieved. This tree use less memory for storing indexed words in the tree and by
 * using int ids as identifiers. It's important because of it's an in memory index. We
 * don't reduce performans by using tree and use less memory. This class can be improved
 * by using partial locks for the each branch of tree.
 */
public class SearchEngineConcurrentTree implements SearchEngineTree, Serializable {

    private static final long serialVersionUID = 7249096246763182397L;
    // Lock for modification operations
    private final Lock writeLock = new ReentrantLock();
    private volatile TreeNode root;

    public SearchEngineConcurrentTree() {
        this.root = createNode("", null, null, Collections.emptyList(), true);
    }

    public SearchEngineConcurrentTree(TreeNode root) {
        this.root = root;
    }

    public TreeNode getRoot() {
        return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putMergeOnConflict(CharSequence key, int value) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("The key argument was null or zero-length");
        }

        writeLock.lock();
        try {
            SearchResult searchResult = searchTree(key);
            Classification classification = searchResult.classification;

            TreeNode parentNode = searchResult.nodeFound.getParent();
            switch (classification) {
                case EXACT_MATCH: {
                    // Node already exist and we add new value to already existing list or creating new one with given value
                    final TIntHashSet existingValue = searchResult.nodeFound.getValue();
                    if (existingValue != null) {
                        existingValue.add(value);
                    } else {
                        TIntHashSet newValues = new TIntHashSet();
                        newValues.add(value);
                        searchResult.nodeFound.setValue(newValues);
                    }
                    break;
                }
                case KEY_ENDS_MID_EDGE: {
                    // Split the node in two: Create a new parent node storing the new value, and a new child node
                    // holding the original value and edges from the existing node
                    TIntHashSet newValues = new TIntHashSet();
                    newValues.add(value);

                    CharSequence keyCharsFromStartOfNodeFound = key.subSequence(searchResult.charsMatched - searchResult.charsMatchedInNodeFound, key.length());
                    CharSequence commonPrefix = CharSequencesUtil.getCommonPrefix(keyCharsFromStartOfNodeFound, searchResult.nodeFound.getCharSequence());
                    CharSequence suffixFromExistingEdge = CharSequencesUtil.subtractPrefix(searchResult.nodeFound.getCharSequence(), commonPrefix);

                    TreeNode newChild = createNode(suffixFromExistingEdge, null, searchResult.nodeFound.getValue(), searchResult.nodeFound.getOutgoingNodes(), false);
                    TreeNode newParent = createNode(commonPrefix, parentNode, newValues, Collections.singletonList(newChild), false);
                    newChild.setParent(newParent);
                    parentNode.updateOutgoingNode(newParent);
                    break;
                }
                case INCOMPLETE_MATCH_TO_END_OF_EDGE: {
                    boolean isRoot = searchResult.nodeFound == root;
                    // Add a new child to the node, containing end characters from the key. This is the only branch
                    // which allows an edge to be added to the root.
                    TIntHashSet newValues = new TIntHashSet();
                    newValues.add(value);

                    CharSequence keySuffix = key.subSequence(searchResult.charsMatched, key.length());
                    TreeNode newChild = createNode(keySuffix, null, newValues, Collections.emptyList(), false);

                    List<TreeNode> edges = new ArrayList<>(searchResult.nodeFound.getOutgoingNodes().size() + 1);
                    edges.addAll(searchResult.nodeFound.getOutgoingNodes());
                    edges.add(newChild);

                    TreeNode clonedNode;
                    if (isRoot) {
                        clonedNode = createNode(searchResult.nodeFound.getCharSequence(), null, searchResult.nodeFound.getValue(), edges, isRoot);
                        this.root = clonedNode;
                    } else {
                        clonedNode = createNode(searchResult.nodeFound.getCharSequence(), parentNode, searchResult.nodeFound.getValue(), edges, isRoot);
                        parentNode.updateOutgoingNode(clonedNode);
                    }
                    edges.forEach(it -> it.setParent(clonedNode));
                    break;
                }
                case INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE: {
                    // Create a new node containing the unmatched characters from the end of the key. Create a new
                    // node containing the unmatched characters of founded node, and copy the original edges and the value .
                    // Creating new node containing both nodes and re-add it to it's parent
                    TIntHashSet newValues = new TIntHashSet();
                    newValues.add(value);

                    CharSequence keyCharsFromStartOfNodeFound = key.subSequence(searchResult.charsMatched - searchResult.charsMatchedInNodeFound, key.length());
                    CharSequence commonPrefix = CharSequencesUtil.getCommonPrefix(keyCharsFromStartOfNodeFound, searchResult.nodeFound.getCharSequence());
                    CharSequence suffixFromExistingEdge = CharSequencesUtil.subtractPrefix(searchResult.nodeFound.getCharSequence(), commonPrefix);
                    CharSequence suffixFromKey = key.subSequence(searchResult.charsMatched, key.length());

                    TreeNode n1 = createNode(suffixFromKey, null, newValues, Collections.emptyList(), false);
                    TreeNode n2 = createNode(suffixFromExistingEdge, null, searchResult.nodeFound.getValue(), searchResult.nodeFound.getOutgoingNodes(), false);
                    searchResult.nodeFound.getOutgoingNodes().forEach(it -> it.setParent(n2));

                    TreeNode n3 = createNode(commonPrefix, parentNode, null, Arrays.asList(n1, n2), false);
                    parentNode.updateOutgoingNode(n3);

                    n1.setParent(n3);
                    n2.setParent(n3);
                    break;
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Integer> getValue(CharSequence key) {
        if (key == null) {
            throw new IllegalArgumentException("The key argument was null or zero-length");
        }

        SearchResult searchResult = searchTree(key);
        if (searchResult.classification.equals(Classification.EXACT_MATCH)) {
            TIntHashSet nodeValues = searchResult.nodeFound.getValue();
            if (nodeValues != null) {
                return Arrays.stream(nodeValues.toArray()).boxed().collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getKeys(int value) {
        List<TreeNode> suitableNodes = new ArrayList<>();
        Queue<TreeNode> nodesQueue = new LinkedList<>();
        nodesQueue.add(root);
        TreeNode currentNode;
        // Breadth-first traversing finding nodes with matched value
        while ((currentNode = nodesQueue.poll()) != null) {
            List<TreeNode> childNodes = currentNode.getOutgoingNodes();
            if (childNodes != null && !childNodes.isEmpty()) {
                nodesQueue.addAll(childNodes);
            }

            TIntHashSet nodeValue = currentNode.getValue();
            if (nodeValue != null && nodeValue.contains(value)) {
                suitableNodes.add(currentNode);
            }
        }

        // Go through it's parents and creating list of keys for value
        Set<String> keys = new HashSet<>();
        suitableNodes.forEach(it -> {
            TreeNode node = it;
            StringBuilder sb = new StringBuilder();
            while (node.getParent() != null) {
                sb.insert(0, node.getCharSequence());
                node = node.getParent();
            }
            keys.add(sb.toString());
        });
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeByKeyAndValue(CharSequence key, int value) {
        if (key == null) {
            throw new IllegalArgumentException("The key argument was null");
        }
        writeLock.lock();
        try {
            SearchResult searchResult = searchTree(key);
            if (searchResult.classification == Classification.EXACT_MATCH && searchResult.nodeFound.getValue() != null) {
                TIntHashSet nodeValues = searchResult.nodeFound.getValue();
                if (nodeValues.size() > 1) {
                    nodeValues.remove(value);
                } else {
                    remove(searchResult.nodeFound);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeByValue(int value) {
        writeLock.lock();
        try {
            boolean nodeWasRemoved;
            do {
                nodeWasRemoved = removeFirstApplicableNode(value);
            } while (nodeWasRemoved);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        Deque<TreeNode> stack = new LinkedList<>();
        stack.push(this.root);
        int count = 0;
        while (true) {
            if (stack.isEmpty()) {
                return count;
            }
            TreeNode current = stack.pop();
            stack.addAll(current.getOutgoingNodes());
            if (current.getValue() != null) {
                count++;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        prettyPrint(root, sb, "", true, true);
        return sb.toString();
    }

    private TreeNode createNode(CharSequence edgeCharacters, TreeNode parent, TIntHashSet value, List<TreeNode> childNodes, boolean isRoot) {
        if (edgeCharacters == null) {
            throw new IllegalStateException("The edgeCharacters argument was null");
        }
        if (!isRoot && edgeCharacters.length() == 0) {
            throw new IllegalStateException("Invalid edge characters for non-root node");
        }
        if (childNodes == null) {
            throw new IllegalStateException("The childNodes argument was null");
        }
        ensureNoDuplicateNodes(childNodes);

        if (childNodes.isEmpty()) {
            return new TreeNode(edgeCharacters, parent, value); // Leaf node with value
        } else if (value == null) {
            return new TreeNode(edgeCharacters, parent, null, childNodes);  // Non-leaf node with null value
        } else {
            return new TreeNode(edgeCharacters, parent, value, childNodes);
        }
    }

    private boolean removeFirstApplicableNode(int value) {
        Queue<TreeNode> nodesQueue = new LinkedList<>();
        nodesQueue.add(root);
        TreeNode currentNode;

        // Breadth-first traversing and finding acceptable nodes with given value
        while ((currentNode = nodesQueue.poll()) != null) {
            List<TreeNode> childNodes = currentNode.getOutgoingNodes();
            if (childNodes != null && !childNodes.isEmpty()) {
                nodesQueue.addAll(childNodes);
            }

            TIntHashSet nodeValue = currentNode.getValue();
            if (nodeValue != null && nodeValue.contains(value)) {
                TIntHashSet nodeValues = currentNode.getValue();
                // If matched node contains mode then one element in the list remove value. Otherwise remove node from tree
                if (nodeValues.size() > 1) {
                    nodeValues.remove(value);
                } else {
                    remove(currentNode);
                    return true;
                }
            }
        }
        return false;
    }

    private void remove(TreeNode node) {
        List<TreeNode> childEdges = node.getOutgoingNodes();
        if (childEdges.size() > 1) {
            // Clone this node without its value, saving its child nodes
            TreeNode cloned = createNode(node.getCharSequence(), node.getParent(), null, node.getOutgoingNodes(), false);
            node.getParent().updateOutgoingNode(cloned);
        } else if (childEdges.size() == 1) {
            // Create a new node which is the concatenation of the edges from this node and its child
            TreeNode child = childEdges.get(0);

            CharSequence concatenatedEdges = CharSequencesUtil.concatenate(node.getCharSequence(), child.getCharSequence());
            TreeNode mergedNode = createNode(concatenatedEdges, node.getParent(), child.getValue(), child.getOutgoingNodes(), false);
            node.getParent().updateOutgoingNode(mergedNode);
        } else {
            // Node has no children. Delete this node from its parent

            List<TreeNode> currentEdgesFromParent = node.getParent().getOutgoingNodes();
            // Create a list of the outgoing edges of the parent which will remain if we remove this child
            List<TreeNode> newEdgesOfParent = Arrays.asList(new TreeNode[node.getParent().getOutgoingNodes().size() - 1]);
            for (int i = 0, added = 0, numParentEdges = currentEdgesFromParent.size(); i < numParentEdges; i++) {
                TreeNode parentEdgesNode = currentEdgesFromParent.get(i);
                if (parentEdgesNode != node) {
                    newEdgesOfParent.set(added++, parentEdgesNode);
                }
            }

            // Note the parent might actually be the root node
            boolean parentIsRoot = (node.getParent().getParent() == null);
            TreeNode newParent;
            if (newEdgesOfParent.size() == 1 && node.getParent().getValue() == null && !parentIsRoot) {
                // Parent is a non-root split node with only one remaining child
                TreeNode parentsRemainingChild = newEdgesOfParent.get(0);
                CharSequence concatenatedEdges = CharSequencesUtil.concatenate(node.getParent().getCharSequence(), parentsRemainingChild.getCharSequence());
                newParent = createNode(concatenatedEdges, null, parentsRemainingChild.getValue(), parentsRemainingChild.getOutgoingNodes(), parentIsRoot);
                parentsRemainingChild.getOutgoingNodes().forEach(it -> it.setParent(newParent));
            } else {
                // Create new parent node which is the same as is currently just without the edge to the node being deleted
                newParent = createNode(node.getParent().getCharSequence(), null, node.getParent().getValue(), newEdgesOfParent, parentIsRoot);
            }

            if (parentIsRoot) {
                if (newParent.getOutgoingNodes() == null) {
                    this.root.setOutgoingNodes(Collections.emptyList());
                } else {
                    this.root.setOutgoingNodes(newParent.getOutgoingNodes());
                }
            } else {
                node.getParent().getParent().updateOutgoingNode(newParent);
                newParent.setParent(node.getParent().getParent());
            }
        }
    }

    private SearchResult searchTree(CharSequence key) {
        TreeNode currentNode = root;
        int charsMatched = 0, charsMatchedInNodeFound = 0;

        final int keyLength = key.length();
        outer_loop:
        while (charsMatched < keyLength) {
            TreeNode nextNode = currentNode.getOutgoingNode(key.charAt(charsMatched));
            if (nextNode == null) {
                // Next node is a dead end
                break outer_loop;
            }

            currentNode = nextNode;
            charsMatchedInNodeFound = 0;
            CharSequence currentNodeEdgeCharacters = currentNode.getCharSequence();
            for (int i = 0, numEdgeChars = currentNodeEdgeCharacters.length(); i < numEdgeChars && charsMatched < keyLength; i++) {
                if (currentNodeEdgeCharacters.charAt(i) != key.charAt(charsMatched)) {
                    // Found a difference in chars between character in key and a character in current node.
                    break outer_loop;
                }
                charsMatched++;
                charsMatchedInNodeFound++;
            }
        }
        return new SearchResult(key, currentNode, charsMatched, charsMatchedInNodeFound);
    }

    private void ensureNoDuplicateNodes(List<TreeNode> nodes) {
        // Sanity check that no two nodes specify an edge with the same first character...
        Set<Character> uniqueChars = new HashSet<>(nodes.size());
        for (TreeNode node : nodes) {
            uniqueChars.add(node.getFirstCharSequenceLetter());
        }
        if (nodes.size() != uniqueChars.size()) {
            throw new IllegalStateException("Duplicate edge detected in list of nodes supplied: " + nodes);
        }
    }

    private void prettyPrint(TreeNode node, Appendable sb, String prefix, boolean isTail, boolean isRoot) {
        try {
            StringBuilder label = new StringBuilder();
            if (isRoot) {
                label.append("○");
                if (node.getCharSequence().length() > 0) {
                    label.append(" ");
                }
            }
            label.append(node.getCharSequence());
            if (node.getValue() != null) {
                label.append(" ").append(node.getValue());
            }
            sb.append(prefix).append(isTail ? isRoot ? "" : "└── ○ " : "├── ○ ").append(label).append("\n");
            List<TreeNode> children = node.getOutgoingNodes();
            for (int i = 0; i < children.size() - 1; i++) {
                prettyPrint(children.get(i), sb, prefix + (isTail ? isRoot ? "" : "    " : "│   "), false, false);
            }
            if (!children.isEmpty()) {
                prettyPrint(children.get(children.size() - 1), sb, prefix + (isTail ? isRoot ? "" : "    " : "│   "), true, false);
            }
        } catch (IOException ioException) {
            throw new IllegalStateException(ioException);
        }
    }

    /**
     * Encapsulates results of searching the tree for a node for which a given key is a prefix, and the number
     * of characters matched in the current node and in total. Also classifies the search result algorithms in
     * methods  use this when adding nodes and removing nodes from the tree.
     */
    private class SearchResult {
        final CharSequence key;
        final TreeNode nodeFound;
        final int charsMatched;
        final int charsMatchedInNodeFound;
        final Classification classification;


        SearchResult(CharSequence key, TreeNode nodeFound, int charsMatched, int charsMatchedInNodeFound) {
            this.key = key;
            this.nodeFound = nodeFound;
            this.charsMatched = charsMatched;
            this.charsMatchedInNodeFound = charsMatchedInNodeFound;
            this.classification = classify(key, nodeFound, charsMatched, charsMatchedInNodeFound);
        }

        private Classification classify(CharSequence key, TreeNode nodeFound, int charsMatched, int charsMatchedInNodeFound) {
            if (charsMatched == key.length()) {
                if (charsMatchedInNodeFound == nodeFound.getCharSequence().length()) {
                    return Classification.EXACT_MATCH;
                } else if (charsMatchedInNodeFound < nodeFound.getCharSequence().length()) {
                    return Classification.KEY_ENDS_MID_EDGE;
                }
            } else if (charsMatched < key.length()) {
                if (charsMatchedInNodeFound == nodeFound.getCharSequence().length()) {
                    return Classification.INCOMPLETE_MATCH_TO_END_OF_EDGE;
                } else if (charsMatchedInNodeFound < nodeFound.getCharSequence().length()) {
                    return Classification.INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE;
                }
            }
            throw new IllegalStateException("Unexpected failure to classify SearchResult: " + this);
        }

        @Override
        public String toString() {
            return "SearchResult{" +
                    "key=" + key +
                    ", nodeFound=" + nodeFound +
                    ", charsMatched=" + charsMatched +
                    ", charsMatchedInNodeFound=" + charsMatchedInNodeFound +
                    ", classification=" + classification +
                    '}';
        }
    }

    private enum Classification {
        EXACT_MATCH,
        INCOMPLETE_MATCH_TO_END_OF_EDGE,
        INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE,
        KEY_ENDS_MID_EDGE
    }

}
