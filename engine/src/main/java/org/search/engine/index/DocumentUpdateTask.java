package org.search.engine.index;

import org.search.engine.analyzer.Tokenizer;
import org.search.engine.model.Document;
import org.search.engine.model.EventType;
import org.search.engine.model.IndexationEvent;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

/**
 * Update task compare difference of new file with already indexed and
 * add or remove only old and new tokens. Old tokens which were not changed
 * stay as it is.
 */
class DocumentUpdateTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentUpdateTask.class);

    private final SearchEngineTree index;
    private final Document updatingDocument;
    private final Tokenizer tokenizer;
    private final BlockingQueue<IndexationEvent> documentQueue;

    DocumentUpdateTask(Document updatingDocument, SearchEngineTree index, Tokenizer tokenizer, BlockingQueue<IndexationEvent> documentQueue) {
        this.index = index;
        this.updatingDocument = updatingDocument;
        this.documentQueue = documentQueue;
        this.tokenizer = tokenizer;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        int documentId = updatingDocument.getId();

        //Old tokens which should be removed from index
        Set<String> oldDocumentTokens = index.getKeys(documentId);

        try (Stream<String> lines = Files.lines(updatingDocument.getPath())) {
            lines.forEach(line -> tokenizer.tokenize(line).forEach(token -> {
                if (oldDocumentTokens.contains(token)) {
                    oldDocumentTokens.remove(token);
                } else {
                    //It's a new token, should be added to the index
                    try {
                        documentQueue.put(new IndexationEvent(EventType.ADD, documentId, token));
                    } catch (InterruptedException ex) {
                        LOG.warn("Put ADD to queue interrupted", ex);
                    }
                }
            }));
            oldDocumentTokens.forEach(it -> {
                try {
                    documentQueue.put(new IndexationEvent(EventType.REMOVE, documentId, it));
                } catch (InterruptedException ex) {
                    LOG.warn("Put DELETE to queue interrupted", ex);
                }
            });
            long end = System.currentTimeMillis();
            LOG.debug("Update index for file: {} took {}ms", updatingDocument.getPath(), (end - start));
        } catch (IOException ex) {
            LOG.warn("Update index for file: {} finished with exception", updatingDocument.getPath());
        }
    }
}
