package org.search.engine.tree;

import org.search.engine.model.SearchType;

import java.util.List;

public interface SearchTreeTrackChangesListener {

    List<String> getTrackedLexeme();
    SearchType getTrackedSearchType();
    void onTrackedLexemeAdd(int value);
    void onTrackedLexemeUpdated(int value);
    void onTrackedLexemeRemoved(int value);
}
