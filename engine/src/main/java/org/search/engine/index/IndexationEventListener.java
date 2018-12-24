package org.search.engine.index;

public interface IndexationEventListener {

    void onIndexationProgress(int progress);

    void onIndexationFinished();
}
