package org.search.engine.tree;

import java.util.List;
import java.util.Set;

/**
 * API of a radix tree, that is a tree which allows values to be looked up based on <i>prefixes</i> of the keys
 * with which they were associated, as well as based on exact matches for keys. A radix tree essentially allows
 * <i><u>"equals"</u></i> and <i><u>"starts with"</u></i> lookup.
 * <p/>
 * See documentation on each method for details.
 *
 * @author Niall Gallagher
 */
public interface SearchEngineTree {

    /**
     * Associates the given value with the given key; replacing any previous value associated with the key.
     * Returns the previous value associated with the key, if any.
     * <p/>
     * This operation is performed atomically.
     *
     * @param key The key with which the specified value should be associated
     * @param value The value to associate with the key, which cannot be null
     * @return The previous value associated with the key, if there was one, otherwise null
     */
    void putMergeOnConflict(CharSequence key, int value);

    void removeByKey(CharSequence key);
    /**
     * Removes the value associated with the given key (exact match).
     * If no value is associated with the key, does nothing.
     *
     * @param value The key for which an associated value should be removed
     * @return True if a value was removed (and therefore was associated with the key), false if no value was
     * associated/removed
     */
    void removeByValue(int value);

    /**
     * Returns the value associated with the given key (exact match), or returns null if no such value
     * is associated with the key.
     *
     * @param key The key with which a sought value might be associated
     * @return The value associated with the given key (exact match), or null if no value was associated with the key
     */
    List<Integer> getValue(CharSequence key);

    Set<String> getKeys(int value);

    /**
     * Counts the number of keys/values stored in the tree.
     * <p/>
     * In the current implementation, <b>this is an expensive operation</b>, having O(n) time complexity.
     *
     * @return The number of keys/values stored in the tree
     */
    int size();
}
