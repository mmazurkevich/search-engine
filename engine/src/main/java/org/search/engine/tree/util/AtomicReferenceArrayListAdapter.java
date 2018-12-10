package org.search.engine.tree.util;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Wraps an {@link AtomicReferenceArray} to implement read-only methods of the {@link java.util.List} interface.
 * <p/>
 * This enables binary search of an {@link AtomicReferenceArray}, using
 * {@link java.util.Collections#binarySearch(java.util.List, Object)}.
 *
 * @author Niall Gallagher
 */
public class AtomicReferenceArrayListAdapter<T> extends AbstractList<T> implements Serializable {

    private final AtomicReferenceArray<T> atomicReferenceArray;

    public AtomicReferenceArrayListAdapter(AtomicReferenceArray<T> atomicReferenceArray) {
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
