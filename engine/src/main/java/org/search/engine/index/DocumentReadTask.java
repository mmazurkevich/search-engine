package org.search.engine.index;

import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.model.Document;
import org.search.engine.model.IndexationEvent;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Task responsible for document indexation, and registration file in the filesystem
 * notifier. It's done for concurrent indexation of different files.
 */
class DocumentReadTask extends AbstractDocumentReadTask implements Runnable {

    private final FilesystemNotifier notificationManager;

    DocumentReadTask(Document indexingDocument, Map<Path, Document> indexedDocuments, BlockingQueue<IndexationEvent> documentLinesQueue,
                     FilesystemNotifier notificationManager) {
        super(indexingDocument, indexedDocuments, documentLinesQueue);
        this.notificationManager = notificationManager;
    }

    @Override
    public void run() {
        readFile();
        if (indexingDocument.isTracked()) {
            notificationManager.registerFile(indexingDocument.getPath());
        }
    }
}
