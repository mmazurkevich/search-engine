package org.search.engine.index;

import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.model.Document;
import org.search.engine.model.EventType;
import org.search.engine.model.IndexationEvent;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * Removal task responsible for delete file from index and unregister it in
 * notifier.
 */
class DocumentRemoveTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentRemoveTask.class);

    private final Map<Path, Document> indexedDocuments;
    private final SearchEngineTree index;
    private final Document removableDocument;
    private final BlockingQueue<IndexationEvent> documentQueue;
    private final FilesystemNotifier notificationManager;

    DocumentRemoveTask(Document removableDocument, SearchEngineTree index, Map<Path, Document> indexedDocuments,
                       BlockingQueue<IndexationEvent> documentQueue, FilesystemNotifier notificationManager) {
        this.index = index;
        this.indexedDocuments = indexedDocuments;
        this.removableDocument = removableDocument;
        this.documentQueue = documentQueue;
        this.notificationManager = notificationManager;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        int documentId = removableDocument.getId();
        //Old tokens which should be removed from index
        Set<String> oldDocumentTokens = index.getKeys(documentId);
        oldDocumentTokens.forEach(it -> {
            try {
                documentQueue.put(new IndexationEvent(EventType.REMOVE, documentId, it));
            } catch (InterruptedException ex) {
                LOG.warn("Put DELETE to queue interrupted", ex);
            }
        });
        indexedDocuments.remove(removableDocument.getPath());
        if (removableDocument.isTracked()) {
            notificationManager.unregisterFile(removableDocument.getPath());
        }
        long end = System.currentTimeMillis();
        LOG.debug("Removing file: {}  from index took {}ms", removableDocument.getPath(), (end - start));
    }
}
