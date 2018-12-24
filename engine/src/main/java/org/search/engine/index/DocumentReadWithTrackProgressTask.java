package org.search.engine.index;

import org.search.engine.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Task responsible for document indexation, and registration file in the filesystem
 * notifier. It's done for concurrent indexation of different files.
 */
class DocumentReadWithTrackProgressTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentReadWithTrackProgressTask.class);

    private final Map<Path, Document> indexedDocuments;
    private final BlockingQueue<DocumentLine> documentLinesQueue;
    private final Document indexingDocument;
    private final IndexationEventListener listener;
    private final AtomicInteger documentCount;
    private final double percentage;

    DocumentReadWithTrackProgressTask(Document indexingDocument, Map<Path, Document> indexedDocuments, BlockingQueue<DocumentLine> documentLinesQueue,
                                      IndexationEventListener listener, AtomicInteger documentCount, double percentage) {
        this.indexedDocuments = indexedDocuments;
        this.indexingDocument = indexingDocument;
        this.documentLinesQueue = documentLinesQueue;
        this.listener = listener;
        this.documentCount = documentCount;
        this.percentage = percentage;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        int documentId = indexingDocument.getId();
        try (Stream<String> lines = Files.lines(indexingDocument.getPath())) {
            lines.forEach(line -> {
                try {
                    documentLinesQueue.put(new DocumentLine(documentId, line));
                } catch (InterruptedException ex) {
                    LOG.warn("Put line of file: {} to queue interrupted", indexingDocument.getPath(), ex);
                }
            });
        } catch (IOException ex) {
            LOG.warn("Reading of file: {} finished with exception", indexingDocument.getPath(), ex);
        } catch (UncheckedIOException ex) {
            LOG.warn("Unsupported character encoding detected for file: {}", indexingDocument.getPath());
        }
        long end = System.currentTimeMillis();
        LOG.debug("Reading of file: {} took {}ms", indexingDocument.getPath(), (end - start));
        indexedDocuments.put(indexingDocument.getPath(), indexingDocument);
        listener.onIndexationProgress((int) (documentCount.incrementAndGet() / percentage));
    }
}
