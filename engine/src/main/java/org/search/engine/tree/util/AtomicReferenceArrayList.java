package org.search.engine.tree.util;

import java.util.AbstractList;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class AtomicReferenceArrayList<T> extends AbstractList<T> {

    private final AtomicReferenceArray<T> atomicReferenceArray;

    public AtomicReferenceArrayList(AtomicReferenceArray<T> atomicReferenceArray) {
        this.atomicReferenceArray = atomicReferenceArray;
    }

    @Override
    public T get(int index) {
        return atomicReferenceArray.get(index);
    }

    @Override
    public int size() {
        return atomicReferenceArray.length();
    }
}
