package org.search.engine.tree;

import org.search.engine.model.SearchType;

public interface SearchTreeTrackChangesListener {

    String getTrackedLexeme();
    SearchType getTrackedSearchType();
    void onTrackedLexemeAdd(int value);
    void onTrackedLexemeUpdated(int value);
    void onTrackedLexemeRemoved(int value);
}
