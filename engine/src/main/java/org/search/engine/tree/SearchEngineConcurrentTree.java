package org.search.engine.tree;

import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import org.search.engine.tree.util.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of {@link SearchEngineTree} which supports lock-free concurrent reads, and allows items to be added to and
 * to be removed from the tree <i>atomically</i> by background thread(s), without blocking reads.
 * <p/>
 * Unlike reads, writes require locking of the tree (locking out other writing threads only; reading threads are never
 * blocked). Currently write locks are coarse-grained; in fact they are tree-level. In future branch-level write locks
 * might be added, but the current implementation is targeted at high concurrency read-mostly use cases.
 *
 * @author Niall Gallagher
 */
public class SearchEngineConcurrentTree implements SearchEngineTree, Serializable {

    protected volatile TreeNode root;

    // Write operations acquire write lock, read operations are lock-free.
    private final Lock writeLock = new ReentrantLock();

    public SearchEngineConcurrentTree() {
        this.root = createNode("", null, null, Collections.emptyList(), true);
    }

    // ------------- Helper methods for serializing writes -------------

    private void acquireWriteLock() {
        writeLock.lock();
    }

    private void releaseWriteLock() {
        writeLock.unlock();
    }

    // ------------- Public API methods -------------

    @Override
    public void putMergeOnConflict(CharSequence key, int value) {
        if (key == null) {
            throw new IllegalArgumentException("The key argument was null");
        }
        if (key.length() == 0) {
            throw new IllegalArgumentException("The key argument was zero-length");
        }

        TIntHashSet newValues = new TIntHashSet();
        newValues.add(value);

        acquireWriteLock();
        try {
            // Note we search the tree here after we have acquired the write lock...
            SearchResult searchResult = searchTree(key);
            Classification classification = searchResult.classification;

            TreeNode parentNode = searchResult.nodeFound.parent;
            switch (classification) {
                case EXACT_MATCH: {
                    // Search found an exact match for all edges leading to this node.
                    // -> Add or update the value in the node found, by replacing
                    // the existing node with a new node containing the value...

                    // First check if existing node has a value, and if we are allowed to overwrite it.
                    // Return early without overwriting if necessary...
                    TIntHashSet existingValue = searchResult.nodeFound.getValue();
                    if (existingValue != null) {
                        existingValue.add(value);
                    }
                    break;
                }
                case KEY_ENDS_MID_EDGE: {
                    // Search ran out of characters from the key while in the middle of an edge in the node.
                    // -> Split the node in two: Create a new parent node storing the new value,
                    // and a new child node holding the original value and edges from the existing node...
                    CharSequence keyCharsFromStartOfNodeFound = key.subSequence(searchResult.charsMatched - searchResult.charsMatchedInNodeFound, key.length());
                    CharSequence commonPrefix = CharSequencesUtil.getCommonPrefix(keyCharsFromStartOfNodeFound, searchResult.nodeFound.getIncomingEdge());
                    CharSequence suffixFromExistingEdge = CharSequencesUtil.subtractPrefix(searchResult.nodeFound.getIncomingEdge(), commonPrefix);

                    // Create new nodes...
                    TreeNode newChild = createNode(suffixFromExistingEdge, null, searchResult.nodeFound.getValue(), searchResult.nodeFound.getOutgoingEdges(), false);

                    TreeNode newParent = createNode(commonPrefix, parentNode, newValues, Collections.singletonList(newChild), false);
                    //Update parent for newly created child
                    newChild.setParent(newParent);

                    // Add the new parent to the parent of the node being replaced (replacing the existing node)...
                    parentNode.updateOutgoingEdge(newParent);
                    break;
                }
                case INCOMPLETE_MATCH_TO_END_OF_EDGE: {
                    // Search found a difference in characters between the key and the start of all child edges leaving the
                    // node, the key still has trailing unmatched characters.
                    // -> Add a new child to the node, containing the trailing characters from the key.

                    // NOTE: this is the only branch which allows an edge to be added to the root.
                    // (Root node's own edge is "" empty string, so is considered a prefixing edge of every key)

                    // Create a new child node containing the trailing characters...
                    boolean isRoot = searchResult.nodeFound == root;

                    CharSequence keySuffix = key.subSequence(searchResult.charsMatched, key.length());

                    TreeNode newChild = createNode(keySuffix, null, newValues, Collections.emptyList(), false);

                    // Clone the current node adding the new child...
                    List<TreeNode> edges = new ArrayList<>(searchResult.nodeFound.getOutgoingEdges().size() + 1);
                    edges.addAll(searchResult.nodeFound.getOutgoingEdges());
                    edges.add(newChild);

                    // Re-add the cloned node to its parent node...
                    TreeNode clonedNode;
                    if (isRoot) {
                        clonedNode = createNode(searchResult.nodeFound.getIncomingEdge(), null, searchResult.nodeFound.getValue(), edges, isRoot);
                        this.root = clonedNode;
                    } else {
                        clonedNode = createNode(searchResult.nodeFound.getIncomingEdge(), parentNode, searchResult.nodeFound.getValue(), edges, isRoot);
                        parentNode.updateOutgoingEdge(clonedNode);
                    }
                    edges.forEach(it -> it.setParent(clonedNode));
//                    newChild.setParent(clonedNode);
//                    searchResult.nodeFound.getOutgoingEdges().forEach();
                    break;
                }
                case INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE: {
                    // Search found a difference in characters between the key and the characters in the middle of the
                    // edge in the current node, and the key still has trailing unmatched characters.
                    // -> Split the node in three:
                    // Let's call node found: NF
                    // (1) Create a new node N1 containing the unmatched characters from the rest of the key, and the
                    // value supplied to this method
                    // (2) Create a new node N2 containing the unmatched characters from the rest of the edge in NF, and
                    // copy the original edges and the value from NF unmodified into N2
                    // (3) Create a new node N3, which will be the split node, containing the matched characters from
                    // the key and the edge, and add N1 and N2 as child nodes of N3
                    // (4) Re-add N3 to the parent node of NF, effectively replacing NF in the tree

                    CharSequence keyCharsFromStartOfNodeFound = key.subSequence(searchResult.charsMatched - searchResult.charsMatchedInNodeFound, key.length());
                    CharSequence commonPrefix = CharSequencesUtil.getCommonPrefix(keyCharsFromStartOfNodeFound, searchResult.nodeFound.getIncomingEdge());
                    CharSequence suffixFromExistingEdge = CharSequencesUtil.subtractPrefix(searchResult.nodeFound.getIncomingEdge(), commonPrefix);
                    CharSequence suffixFromKey = key.subSequence(searchResult.charsMatched, key.length());

                    // Create new nodes...
                    TreeNode n1 = createNode(suffixFromKey, null, newValues, Collections.emptyList(), false);
                    TreeNode n2 = createNode(suffixFromExistingEdge, null, searchResult.nodeFound.getValue(), searchResult.nodeFound.getOutgoingEdges(), false);
                    TreeNode n3 = createNode(commonPrefix, parentNode, null, Arrays.asList(n1, n2), false);

                    parentNode.updateOutgoingEdge(n3);

                    n1.setParent(n3);
                    n2.setParent(n3);
                    break;
                }
                default: {
                    // This is a safeguard against a new enum constant being added in future.
                    throw new IllegalStateException("Unexpected classification for search result: " + searchResult);
                }
            }
        } finally {
            releaseWriteLock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TIntHashSet getValueForExactKey(CharSequence key) {
        SearchResult searchResult = searchTree(key);
        if (searchResult.classification.equals(Classification.EXACT_MATCH)) {
            return searchResult.nodeFound.getValue();
        }
        return null;
    }

    public boolean removeByValue(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("The key argument was null");
        }
        acquireWriteLock();

        try {
            System.out.println(prettyPrint());

            boolean nodeWasRemoved;
            do {
                nodeWasRemoved = removeFirstApplicableNode(value);
            } while (nodeWasRemoved);

            System.out.println(prettyPrint());
        } finally {
            releaseWriteLock();
        }

        return false;
    }

    private boolean removeFirstApplicableNode(Integer value) {
        Queue<TreeNode> nodesQueue = new LinkedList<>();
        nodesQueue.add(root);
        TreeNode currentNode;
        //Breadth-first traversing
        while ((currentNode = nodesQueue.poll()) != null) {
            List<TreeNode> childNodes = currentNode.getOutgoingEdges();
            if (childNodes != null && !childNodes.isEmpty()) {
                nodesQueue.addAll(childNodes);
            }

            TIntHashSet nodeValue = currentNode.getValue();
            if (nodeValue != null && nodeValue.contains(value)) {
                TIntHashSet nodeValues = currentNode.getValue();
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

    private boolean remove(TreeNode node) {
//            if (node.getValue() == null) {
//                // This node was created automatically as a split between two branches (implicit node).
//                // No need to remove it...
//                return false;
//            }
        // Proceed with deleting the node...
        List<TreeNode> childEdges = node.getOutgoingEdges();
        if (childEdges.size() > 1) {
            // This node has more than one child, so if we delete the value from this node, we still need
            // to leave a similar node in place to act as the split between the child edges.
            // Just delete the value associated with this node.
            // -> Clone this node without its value, preserving its child nodes...
            TreeNode cloned = createNode(node.getIncomingEdge(), node.parent, null, node.getOutgoingEdges(), false);
            // Re-add the replacement node to the parent...
            node.parent.updateOutgoingEdge(cloned);
        } else if (childEdges.size() == 1) {
            // Node has one child edge.
            // Create a new node which is the concatenation of the edges from this node and its child,
            // and which has the outgoing edges of the child and the value from the child.
            TreeNode child = childEdges.get(0);
            CharSequence concatenatedEdges = CharSequencesUtil.concatenate(node.getIncomingEdge(), child.getIncomingEdge());
            TreeNode mergedNode = createNode(concatenatedEdges, node.parent, child.getValue(), child.getOutgoingEdges(), false);
            // Re-add the merged node to the parent...
            node.parent.updateOutgoingEdge(mergedNode);
        } else {
            // Node has no children. Delete this node from its parent,
            // which involves re-creating the parent rather than simply updating its child edge
            // (this is why we need parentNodesParent).
            // However if this would leave the parent with only one remaining child edge,
            // and the parent itself has no value (is a split node), and the parent is not the root node
            // (a special case which we never merge), then we also need to merge the parent with its
            // remaining child.

            List<TreeNode> currentEdgesFromParent = node.parent.getOutgoingEdges();
            // Create a list of the outgoing edges of the parent which will remain
            // if we remove this child...
            // Use a non-resizable list, as a sanity check to force ArrayIndexOutOfBounds...
            List<TreeNode> newEdgesOfParent = Arrays.asList(new TreeNode[node.parent.getOutgoingEdges().size() - 1]);
            for (int i = 0, added = 0, numParentEdges = currentEdgesFromParent.size(); i < numParentEdges; i++) {
                TreeNode parentEdgesNode = currentEdgesFromParent.get(i);
                if (parentEdgesNode != node) {
                    newEdgesOfParent.set(added++, parentEdgesNode);
                }
            }

            // Note the parent might actually be the root node (which we should never merge)...
            boolean parentIsRoot = (node.parent.parent == null);
            TreeNode newParent;
            if (newEdgesOfParent.size() == 1 && node.parent.getValue() == null && !parentIsRoot) {
                // Parent is a non-root split node with only one remaining child, which can now be merged.
                TreeNode parentsRemainingChild = newEdgesOfParent.get(0);
                // Merge the parent with its only remaining child...
                CharSequence concatenatedEdges = CharSequencesUtil.concatenate(node.parent.getIncomingEdge(), parentsRemainingChild.getIncomingEdge());
                newParent = createNode(concatenatedEdges, null, parentsRemainingChild.getValue(), parentsRemainingChild.getOutgoingEdges(), parentIsRoot);
                parentsRemainingChild.getOutgoingEdges().forEach(it -> it.setParent(newParent));
            } else {
                // Parent is a node which either has a value of its own, has more than one remaining
                // child, or is actually the root node (we never merge the root node).
                // Create new parent node which is the same as is currently just without the edge to the
                // node being deleted...
                newParent = createNode(node.parent.getIncomingEdge(), null, node.parent.getValue(), newEdgesOfParent, parentIsRoot);
            }
            // Re-add the parent node to its parent...
            if (parentIsRoot) {
                // Replace the root node...
                this.root.setOutgoingEdge(newParent.outgoingEdgesAsList);
            } else {
                // Re-add the parent node to its parent...
                newParent.setParent(node.parent.parent);
                node.parent.parent.updateOutgoingEdge(newParent);
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(CharSequence key) {
        if (key == null) {
            throw new IllegalArgumentException("The key argument was null");
        }
        acquireWriteLock();
        try {
            SearchResult searchResult = searchTree(key);
            Classification classification = searchResult.classification;
            switch (classification) {
                case EXACT_MATCH: {
                    if (searchResult.nodeFound.getValue() == null) {
                        // This node was created automatically as a split between two branches (implicit node).
                        // No need to remove it...
                        return false;
                    }
                    TreeNode parentNode = searchResult.nodeFound.parent;
                    // Proceed with deleting the node...
                    List<TreeNode> childEdges = searchResult.nodeFound.getOutgoingEdges();
                    if (childEdges.size() > 1) {
                        // This node has more than one child, so if we delete the value from this node, we still need
                        // to leave a similar node in place to act as the split between the child edges.
                        // Just delete the value associated with this node.
                        // -> Clone this node without its value, preserving its child nodes...
                        TreeNode cloned = createNode(searchResult.nodeFound.getIncomingEdge(), parentNode, null, searchResult.nodeFound.getOutgoingEdges(), false);
                        // Re-add the replacement node to the parent...
                        parentNode.updateOutgoingEdge(cloned);
                    } else if (childEdges.size() == 1) {
                        // Node has one child edge.
                        // Create a new node which is the concatenation of the edges from this node and its child,
                        // and which has the outgoing edges of the child and the value from the child.
                        TreeNode child = childEdges.get(0);
                        CharSequence concatenatedEdges = CharSequencesUtil.concatenate(searchResult.nodeFound.getIncomingEdge(), child.getIncomingEdge());
                        TreeNode mergedNode = createNode(concatenatedEdges, parentNode, child.getValue(), child.getOutgoingEdges(), false);
                        // Re-add the merged node to the parent...
                        parentNode.updateOutgoingEdge(mergedNode);
                    } else {
                        // Node has no children. Delete this node from its parent,
                        // which involves re-creating the parent rather than simply updating its child edge
                        // (this is why we need parentNodesParent).
                        // However if this would leave the parent with only one remaining child edge,
                        // and the parent itself has no value (is a split node), and the parent is not the root node
                        // (a special case which we never merge), then we also need to merge the parent with its
                        // remaining child.

                        List<TreeNode> currentEdgesFromParent = parentNode.getOutgoingEdges();
                        // Create a list of the outgoing edges of the parent which will remain
                        // if we remove this child...
                        // Use a non-resizable list, as a sanity check to force ArrayIndexOutOfBounds...
                        List<TreeNode> newEdgesOfParent = Arrays.asList(new TreeNode[parentNode.getOutgoingEdges().size() - 1]);
                        for (int i = 0, added = 0, numParentEdges = currentEdgesFromParent.size(); i < numParentEdges; i++) {
                            TreeNode node = currentEdgesFromParent.get(i);
                            if (node != searchResult.nodeFound) {
                                newEdgesOfParent.set(added++, node);
                            }
                        }

                        // Note the parent might actually be the root node (which we should never merge)...
                        boolean parentIsRoot = (parentNode == root);
                        TreeNode newParent;
                        if (newEdgesOfParent.size() == 1 && parentNode.getValue() == null && !parentIsRoot) {
                            // Parent is a non-root split node with only one remaining child, which can now be merged.
                            TreeNode parentsRemainingChild = newEdgesOfParent.get(0);
                            // Merge the parent with its only remaining child...
                            CharSequence concatenatedEdges = CharSequencesUtil.concatenate(parentNode.getIncomingEdge(), parentsRemainingChild.getIncomingEdge());
                            newParent = createNode(concatenatedEdges, null, parentsRemainingChild.getValue(), parentsRemainingChild.getOutgoingEdges(), parentIsRoot);
                        } else {
                            // Parent is a node which either has a value of its own, has more than one remaining
                            // child, or is actually the root node (we never merge the root node).
                            // Create new parent node which is the same as is currently just without the edge to the
                            // node being deleted...
                            newParent = createNode(parentNode.getIncomingEdge(), null, parentNode.getValue(), newEdgesOfParent, parentIsRoot);
                        }
                        // Re-add the parent node to its parent...
                        if (parentIsRoot) {
                            // Replace the root node...
                            this.root = newParent;
                        } else {
                            // Re-add the parent node to its parent...
                            newParent.setParent(parentNode.parent);
                            parentNode.parent.updateOutgoingEdge(newParent);
                        }
                    }
                    return true;
                }
                default: {
                    return false;
                }
            }
        } finally {
            releaseWriteLock();
        }
    }

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
            stack.addAll(current.getOutgoingEdges());
            if (current.getValue() != null) {
                count++;
            }
        }
    }

    // ------------- Helper method for put() -------------

    private TreeNode createNode(CharSequence edgeCharacters, TreeNode parent, TIntHashSet value, List<TreeNode> childNodes, boolean isRoot) {
        if (edgeCharacters == null) {
            throw new IllegalStateException("The edgeCharacters argument was null");
        }
        if (!isRoot && edgeCharacters.length() == 0) {
            throw new IllegalStateException("Invalid edge characters for non-root node: " + CharSequencesUtil.toString(edgeCharacters));
        }
        if (childNodes == null) {
            throw new IllegalStateException("The childNodes argument was null");
        }
        ensureNoDuplicateEdges(childNodes);

        if (childNodes.isEmpty()) {
            // Leaf node with value
            return new TreeNode(edgeCharacters, parent, value);
        } else if (value == null) {
            // Non-leaf node... Node with null value
            return new TreeNode(edgeCharacters, parent, null, childNodes);
        } else {
            return new TreeNode(edgeCharacters, parent, value, childNodes);
        }
    }

    // ------------- Helper method for searching the tree and associated SearchResult object -------------

    /**
     * Traverses the tree and finds the node which matches the longest prefix of the given key.
     * <p/>
     * The node returned might be an <u>exact match</u> for the key, in which case {@link SearchResult#charsMatched}
     * will equal the length of the key.
     * <p/>
     * The node returned might be an <u>inexact match</u> for the key, in which case {@link SearchResult#charsMatched}
     * will be less than the length of the key.
     * <p/>
     * There are two types of inexact match:
     * <ul>
     * <li>
     * An inexact match which ends evenly at the boundary between a node and its children (the rest of the key
     * not matching any children at all). In this case if we we wanted to add nodes to the tree to represent the
     * rest of the key, we could simply add child nodes to the node found.
     * </li>
     * <li>
     * An inexact match which ends in the middle of a the characters for an edge stored in a node (the key
     * matching only the first few characters of the edge). In this case if we we wanted to add nodes to the
     * tree to represent the rest of the key, we would have to split the node (let's call this node found: NF):
     * <ol>
     * <li>
     * Create a new node (N1) which will be the split node, containing the matched characters from the
     * start of the edge in NF
     * </li>
     * <li>
     * Create a new node (N2) which will contain the unmatched characters from the rest of the edge
     * in NF, and copy the original edges from NF unmodified into N2
     * </li>
     * <li>
     * Create a new node (N3) which will be the new branch, containing the unmatched characters from
     * the rest of the key
     * </li>
     * <li>
     * Add N2 as a child of N1
     * </li>
     * <li>
     * Add N3 as a child of N1
     * </li>
     * <li>
     * In the <b>parent node of NF</b>, replace the edge pointing to NF with an edge pointing instead
     * to N1. If we do this step atomically, reading threads are guaranteed to never see "invalid"
     * data, only either the old data or the new data
     * </li>
     * </ol>
     * </li>
     * </ul>
     * The {@link SearchResult#classification} is an enum value based on its classification of the
     * match according to the descriptions above.
     *
     * @param key a key for which the node matching the longest prefix of the key is required
     * @return A {@link SearchResult} object which contains the node matching the longest prefix of the key, its
     * parent node, the number of characters of the key which were matched in total and within the edge of the
     * matched node, and a {@link SearchResult#classification} of the match as described above
     */
    SearchResult searchTree(CharSequence key) {
        TreeNode currentNode = root;
        int charsMatched = 0, charsMatchedInNodeFound = 0;

        final int keyLength = key.length();
        outer_loop:
        while (charsMatched < keyLength) {
            TreeNode nextNode = currentNode.getOutgoingEdge(key.charAt(charsMatched));
            if (nextNode == null) {
                // Next node is a dead end...
                //noinspection UnnecessaryLabelOnBreakStatement
                break outer_loop;
            }

            currentNode = nextNode;
            charsMatchedInNodeFound = 0;
            CharSequence currentNodeEdgeCharacters = currentNode.getIncomingEdge();
            for (int i = 0, numEdgeChars = currentNodeEdgeCharacters.length(); i < numEdgeChars && charsMatched < keyLength; i++) {
                if (currentNodeEdgeCharacters.charAt(i) != key.charAt(charsMatched)) {
                    // Found a difference in chars between character in key and a character in current node.
                    // Current node is the deepest match (inexact match)....
                    break outer_loop;
                }
                charsMatched++;
                charsMatchedInNodeFound++;
            }
        }
        return new SearchResult(key, currentNode, charsMatched, charsMatchedInNodeFound);
    }

    /**
     * Encapsulates results of searching the tree for a node for which a given key is a prefix. Encapsulates the node
     * found, its parent node, its parent's parent node, and the number of characters matched in the current node and
     * in total.
     * <p/>
     * Also classifies the search result so that algorithms in methods which use this SearchResult, when adding nodes
     * and removing nodes from the tree, can select appropriate strategies based on the classification.
     */
    enum Classification {
        EXACT_MATCH,
        INCOMPLETE_MATCH_TO_END_OF_EDGE,
        INCOMPLETE_MATCH_TO_MIDDLE_OF_EDGE,
        KEY_ENDS_MID_EDGE,
        INVALID // INVALID is never used, except in unit testing
    }

    class SearchResult {
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

            // Classify this search result...
            this.classification = classify(key, nodeFound, charsMatched, charsMatchedInNodeFound);
        }

        protected Classification classify(CharSequence key, TreeNode nodeFound, int charsMatched, int charsMatchedInNodeFound) {
            if (charsMatched == key.length()) {
                if (charsMatchedInNodeFound == nodeFound.getIncomingEdge().length()) {
                    return Classification.EXACT_MATCH;
                } else if (charsMatchedInNodeFound < nodeFound.getIncomingEdge().length()) {
                    return Classification.KEY_ENDS_MID_EDGE;
                }
            } else if (charsMatched < key.length()) {
                if (charsMatchedInNodeFound == nodeFound.getIncomingEdge().length()) {
                    return Classification.INCOMPLETE_MATCH_TO_END_OF_EDGE;
                } else if (charsMatchedInNodeFound < nodeFound.getIncomingEdge().length()) {
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

    // ------------- Helper method for pretty-printing tree (not public API) -------------

    class TreeNode {


        // Characters in the edge arriving at this node from a parent node.
        // Once assigned, we never modify this...
        private final CharSequence incomingEdgeCharSequence;

        //Can be null for root
        private TreeNode parent;

        // References to child nodes representing outgoing edges from this node.
        // Once assigned we never add or remove references, but we do update existing references to point to new child
        // nodes provided new edges start with the same first character...
        private AtomicReferenceArray<TreeNode> outgoingEdges;

        // A read-only List wrapper around the outgoingEdges AtomicReferenceArray...
        private List<TreeNode> outgoingEdgesAsList;

        // An arbitrary value which the application associates with a key matching the path to this node in the tree.
        // This value can be null...
        private final TIntHashSet value;

        TreeNode(CharSequence edgeCharSequence, TreeNode parent, TIntHashSet value) {
            this.incomingEdgeCharSequence = edgeCharSequence;
            this.parent = parent;
            this.value = value;
        }

        TreeNode(CharSequence edgeCharSequence, TreeNode parent, TIntHashSet value, List<TreeNode> outgoingEdges) {
            this.parent = parent;
            this.incomingEdgeCharSequence = edgeCharSequence;
            this.value = value;
            setOutgoingEdge(outgoingEdges);
        }

        boolean isRoot() {
            return parent == null;
        }

        CharSequence getIncomingEdge() {
            return incomingEdgeCharSequence;
        }

        TreeNode getParent() {
            return parent;
        }

        void setParent(TreeNode parent) {
            this.parent = parent;
        }

        Character getIncomingEdgeFirstCharacter() {
            return incomingEdgeCharSequence.charAt(0);
        }

        TIntHashSet getValue() {
            return value;
        }

        TreeNode getOutgoingEdge(Character edgeFirstCharacter) {
            if (outgoingEdges == null) {
                return null;
            }
            // Binary search for the index of the node whose edge starts with the given character.
            // Note that this binary search is safe in the face of concurrent modification due to constraints
            // we enforce on use of the array, as documented in the binarySearchForEdge method...
            int index = binarySearchForEdge(outgoingEdges, edgeFirstCharacter);
            if (index < 0) {
                // No such edge exists...
                return null;
            }
            // Atomically return the child node at this index...
            return outgoingEdges.get(index);
        }

        void setOutgoingEdge(List<TreeNode> outgoingEdges) {
            TreeNode[] childNodeArray = outgoingEdges.toArray(new TreeNode[outgoingEdges.size()]);
            // Sort the child nodes...
            Arrays.sort(childNodeArray, new Comparator<TreeNode>() {
                @Override
                public int compare(TreeNode o1, TreeNode o2) {
                    return o1.getIncomingEdgeFirstCharacter().compareTo(o2.getIncomingEdgeFirstCharacter());
                }
            });
            this.outgoingEdges = new AtomicReferenceArray<>(childNodeArray);
            this.outgoingEdgesAsList = new AtomicReferenceArrayListAdapter<>(this.outgoingEdges);
        }

        void updateOutgoingEdge(TreeNode childNode) {
            if (outgoingEdges == null) {
                throw new IllegalStateException("Cannot update the reference to the following child node for the edge starting with '" + childNode.getIncomingEdgeFirstCharacter() + "', no such edge already exists: " + childNode);
            }
            // Binary search for the index of the node whose edge starts with the given character.
            // Note that this binary search is safe in the face of concurrent modification due to constraints
            // we enforce on use of the array, as documented in the binarySearchForEdge method...
            int index = binarySearchForEdge(outgoingEdges, childNode.getIncomingEdgeFirstCharacter());
            if (index < 0) {
                throw new IllegalStateException("Cannot update the reference to the following child node for the edge starting with '" + childNode.getIncomingEdgeFirstCharacter() + "', no such edge already exists: " + childNode);
            }
            // Atomically update the child node at this index...
            outgoingEdges.set(index, childNode);
        }

        List<TreeNode> getOutgoingEdges() {
            if (outgoingEdgesAsList == null)
                return Collections.emptyList();
            return outgoingEdgesAsList;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Node{");
            sb.append("edge=").append(incomingEdgeCharSequence);
            sb.append(", value=").append(value);
            sb.append(", edges=").append(getOutgoingEdges());
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * Returns the index of the node in the given {@link AtomicReferenceArray} whose edge starts with the given
     * first character.
     * <p/>
     * This method expects that some constraints are enforced on the {@link AtomicReferenceArray}:
     * <ul>
     * <li>
     * The array must already be in ascending sorted order of the first character of the edge for each node
     * </li>
     * <li>
     * No entries in the array can be null
     * </li>
     * <li>
     * Any existing node in the array cannot be swapped concurrently for another unless the edge associated
     * with the other node also starts with the same first character
     * </li>
     * </ul>
     * If these constraints are enforced as expected, then this method will have deterministic behaviour even in the
     * face of concurrent modification.
     *
     * @param childNodes         An {@link AtomicReferenceArray} of {@link } objects, which is used in accordance with
     *                           the constraints documented in this method
     * @param edgeFirstCharacter The first character of the edge for which the associated node is required
     * @return The index of the node representing the indicated edge, or a value < 0 if no such node exists in the
     * array
     */
    int binarySearchForEdge(AtomicReferenceArray<TreeNode> childNodes, Character edgeFirstCharacter) {
        // inspired by Collections#indexedBinarySearch()
        int low = 0;
        int high = childNodes.length() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            TreeNode midVal = childNodes.get(mid);
            int cmp = midVal.getIncomingEdgeFirstCharacter().compareTo(edgeFirstCharacter);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }

    /**
     * Throws an exception if any nodes in the given list represent edges having the same first character.
     *
     * @param nodes The list of nodes to validate
     * @throws IllegalStateException If a duplicate edge is detected
     */
    void ensureNoDuplicateEdges(List<TreeNode> nodes) {
        // Sanity check that no two nodes specify an edge with the same first character...
        Set<Character> uniqueChars = new HashSet<Character>(nodes.size());
        for (TreeNode node : nodes) {
            uniqueChars.add(node.getIncomingEdgeFirstCharacter());
        }
        if (nodes.size() != uniqueChars.size()) {
            throw new IllegalStateException("Duplicate edge detected in list of nodes supplied: " + nodes);
        }
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        prettyPrint(root, sb, "", true, true);
        return sb.toString();
    }

    private void prettyPrint(TreeNode node, Appendable sb, String prefix, boolean isTail, boolean isRoot) {
        try {
            StringBuilder label = new StringBuilder();
            if (isRoot) {
                label.append("○");
                if (node.getIncomingEdge().length() > 0) {
                    label.append(" ");
                }
            }
            label.append(node.getIncomingEdge());
            if (node.getValue() != null) {
                label.append(" (").append(node.getValue()).append(")");
            }
            sb.append(prefix).append(isTail ? isRoot ? "" : "└── ○ " : "├── ○ ").append(label).append("\n");
            List<TreeNode> children = node.getOutgoingEdges();
            for (int i = 0; i < children.size() - 1; i++) {
                prettyPrint(children.get(i), sb, prefix + (isTail ? isRoot ? "" : "    " : "│   "), false, false);
            }
            if (!children.isEmpty()) {
                prettyPrint(children.get(children.size() - 1), sb, prefix + (isTail ? isRoot ? "" : "    " : "│   "), true, false);
            }
        } catch (IOException ioException) {
            // Rethrow the checked exception as a runtime exception...
            throw new IllegalStateException(ioException);
        }
    }
}
