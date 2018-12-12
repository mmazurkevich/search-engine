package org.search.engine.tree;

import gnu.trove.set.hash.TIntHashSet;
import org.search.engine.tree.util.AtomicReferenceArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

class TreeNode {

    private final CharSequence charSequence;

    private TreeNode parent;

    private AtomicReferenceArray<TreeNode> outgoingNodes;

    private List<TreeNode> outgoingNodesAsList;

    private TIntHashSet value;

    TreeNode(CharSequence charSequence, TreeNode parent, TIntHashSet value) {
        this.charSequence = charSequence;
        this.parent = parent;
        this.value = value;
    }

    TreeNode(CharSequence charSequence, TreeNode parent, TIntHashSet value, List<TreeNode> outgoingNodes) {
        this.parent = parent;
        this.charSequence = charSequence;
        this.value = value;
        setOutgoingNodes(outgoingNodes);
    }

    CharSequence getCharSequence() {
        return charSequence;
    }

    TreeNode getParent() {
        return parent;
    }

    void setParent(TreeNode parent) {
        this.parent = parent;
    }

    Character getFirstCharSequenceLetter() {
        return charSequence.charAt(0);
    }

    TIntHashSet getValue() {
        return value;
    }

    void setValue(TIntHashSet value) {
        this.value = value;
    }

    TreeNode getOutgoingNode(Character charSequence) {
        if (outgoingNodes == null) {
            return null;
        }

        int index = binarySearch(outgoingNodes, charSequence);
        if (index < 0) {
            return null;
        }
        return outgoingNodes.get(index);
    }

    List<TreeNode> getOutgoingNodes() {
        if (outgoingNodesAsList == null)
            return Collections.emptyList();
        return outgoingNodesAsList;
    }

    void setOutgoingNodes(List<TreeNode> treeNodes) {
        TreeNode[] childNodeArray = treeNodes.toArray(new TreeNode[treeNodes.size()]);

        Arrays.sort(childNodeArray, Comparator.comparing(TreeNode::getFirstCharSequenceLetter));
        this.outgoingNodes = new AtomicReferenceArray<>(childNodeArray);
        this.outgoingNodesAsList = new AtomicReferenceArrayList<>(this.outgoingNodes);
    }

    void updateOutgoingNode(TreeNode childNode) {
        if (outgoingNodes == null) {
            throw new IllegalStateException("Cannot update the reference to the following child node for the edge starting with '" + childNode.getFirstCharSequenceLetter() + "', no such edge already exists: " + childNode);
        }

        int index = binarySearch(outgoingNodes, childNode.getFirstCharSequenceLetter());
        if (index < 0) {
            throw new IllegalStateException("Cannot update the reference to the following child node for the edge starting with '" + childNode.getFirstCharSequenceLetter() + "', no such edge already exists: " + childNode);
        }
        outgoingNodes.set(index, childNode);
    }

    private int binarySearch(AtomicReferenceArray<TreeNode> childNodes, Character firstCharacter) {
        // inspired by Collections#indexedBinarySearch()
        int low = 0;
        int high = childNodes.length() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            TreeNode midVal = childNodes.get(mid);
            int cmp = midVal.getFirstCharSequenceLetter().compareTo(firstCharacter);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Node{");
        sb.append("edge=").append(charSequence);
        sb.append(", value=").append(value);
        sb.append(", edges=").append(getOutgoingNodes());
        sb.append("}");
        return sb.toString();
    }
}