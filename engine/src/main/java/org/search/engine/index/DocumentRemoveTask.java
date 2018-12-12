package org.search.engine.index;

import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Removal task responsible for delete file from index and unregister it in
 * notifier.
 */
class DocumentRemoveTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentRemoveTask.class);

    private final List<Document> indexedDocuments;
    private final SearchEngineTree index;
    private final Document removableDocument;
    private final FilesystemNotifier notificationManager;

    DocumentRemoveTask(Document removableDocument, SearchEngineTree index, List<Document> indexedDocuments,
                       FilesystemNotifier notificationManager) {
        this.index = index;
        this.indexedDocuments = indexedDocuments;
        this.removableDocument = removableDocument;
        this.notificationManager = notificationManager;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        index.removeByValue(removableDocument.getId());
        indexedDocuments.remove(removableDocument);
        if (removableDocument.isTracked()) {
            notificationManager.unregisterFile(removableDocument.getPath());
        }
        long end = System.currentTimeMillis();
        LOG.debug("Removing file: {}  from index took {}ms", removableDocument.getPath(), (end - start));
    }
}
