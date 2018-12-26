package org.search.engine.index;

import org.search.engine.model.Document;
import org.search.engine.model.EventType;
import org.search.engine.model.IndexationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

abstract class AbstractDocumentReadTask {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDocumentReadTask.class);

    private final Map<Path, Document> indexedDocuments;
    private final BlockingQueue<IndexationEvent> documentLinesQueue;
    final Document indexingDocument;

    AbstractDocumentReadTask(Document indexingDocument, Map<Path, Document> indexedDocuments, BlockingQueue<IndexationEvent> documentLinesQueue) {
        this.indexedDocuments = indexedDocuments;
        this.indexingDocument = indexingDocument;
        this.documentLinesQueue = documentLinesQueue;
    }

    void readFile() {
        long start = System.currentTimeMillis();
        int documentId = indexingDocument.getId();
        try (Stream<String> lines = Files.lines(indexingDocument.getPath())) {
            lines.forEach(line -> {
                try {
                    documentLinesQueue.put(new IndexationEvent(EventType.ADD_LINE, documentId, line));
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
    }
}
