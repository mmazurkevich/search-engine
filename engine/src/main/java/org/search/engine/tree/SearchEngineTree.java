/**
 * Copyright 2012-2013 Niall Gallagher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.search.engine.tree;

import gnu.trove.set.hash.TIntHashSet;

/**
 * API of a radix tree, that is a tree which allows values to be looked up based on <i>prefixes</i> of the keys
 * with which they were associated, as well as based on exact matches for keys. A radix tree essentially allows
 * <i><u>"equals"</u></i> and <i><u>"starts with"</u></i> lookup.
 * <p/>
 * See documentation on each method for details.
 *
 * @param <T> The type of the values associated with keys in the tree
 *
 * @author Niall Gallagher
 */
public interface SearchEngineTree<T> {

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
    TIntHashSet put(CharSequence key, TIntHashSet value);

    /**
     * Removes the value associated with the given key (exact match).
     * If no value is associated with the key, does nothing.
     *
     * @param key The key for which an associated value should be removed
     * @return True if a value was removed (and therefore was associated with the key), false if no value was
     * associated/removed
     */
    boolean remove(CharSequence key);

    /**
     * Returns the value associated with the given key (exact match), or returns null if no such value
     * is associated with the key.
     *
     * @param key The key with which a sought value might be associated
     * @return The value associated with the given key (exact match), or null if no value was associated with the key
     */
    TIntHashSet getValueForExactKey(CharSequence key);

    /**
     * Counts the number of keys/values stored in the tree.
     * <p/>
     * In the current implementation, <b>this is an expensive operation</b>, having O(n) time complexity.
     *
     * @return The number of keys/values stored in the tree
     */
    int size();
}
