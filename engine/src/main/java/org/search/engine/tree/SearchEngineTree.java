package org.search.engine.tree;

import java.util.List;
import java.util.Set;

public interface SearchEngineTree {

    void putMergeOnConflict(CharSequence key, int value);

    Set<String> getKeys(int value);

    List<Integer> getValue(CharSequence key);

    void removeByKey(CharSequence key);

    void removeByValue(int value);

    int size();
}
