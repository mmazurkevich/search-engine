package org.search.engine.model;

import org.search.engine.index.IndexationEventListener;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class IndexationTracker {

    private boolean canceled;
    private IndexationEventListener listener;
    private Path indexingFolder;
    private List<Future> indexingFutures;

    public IndexationTracker(IndexationEventListener listener, Path indexingFolder) {
        this.listener = listener;
        this.indexingFolder = indexingFolder;
        indexingFutures = new ArrayList<>();
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public IndexationEventListener getListener() {
        return listener;
    }

    public void setListener(IndexationEventListener listener) {
        this.listener = listener;
    }

    public Path getIndexingFolder() {
        return indexingFolder;
    }

    public void setIndexingFolder(Path indexingFolder) {
        this.indexingFolder = indexingFolder;
    }

    public List<Future> getIndexingFutures() {
        return indexingFutures;
    }

    public void setIndexingFutures(List<Future> indexingFutures) {
        this.indexingFutures = indexingFutures;
    }
}
