package org.search.engine.index;

import org.search.engine.model.Document;
import org.search.engine.model.IndexationEvent;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task responsible for document indexation, and registration file in the filesystem
 * notifier. Task also notify listeners for the progress of indexation.
 */
class DocumentReadWithTrackProgressTask extends AbstractDocumentReadTask implements Runnable {

    private final IndexationEventListener listener;
    private final AtomicInteger documentCount;
    private final double percentage;

    DocumentReadWithTrackProgressTask(Document indexingDocument, Map<Path, Document> indexedDocuments, BlockingQueue<IndexationEvent> documentLinesQueue,
                                      IndexationEventListener listener, AtomicInteger documentCount, double percentage) {
        super(indexingDocument, indexedDocuments, documentLinesQueue);
        this.listener = listener;
        this.documentCount = documentCount;
        this.percentage = percentage;
    }

    @Override
    public void run() {
        readFile();
        listener.onIndexationProgress((int) (documentCount.incrementAndGet() / percentage));
    }
}
