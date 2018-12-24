package org.search.engine.tree.util;

import org.search.engine.tree.TreeNode;

import java.io.Serializable;
import java.util.AbstractList;

public class AtomicReferenceArrayList extends AbstractList<TreeNode> implements Serializable {

    private static final long serialVersionUID = 7249096246673128397L;
    private final TreeNode[] atomicReferenceArray;

    public AtomicReferenceArrayList(TreeNode[] atomicReferenceArray) {
        this.atomicReferenceArray = atomicReferenceArray;
    }

    @Override
    public TreeNode get(int index) {
        return atomicReferenceArray[index];
    }

    @Override
    public int size() {
        return atomicReferenceArray.length;
    }
}
