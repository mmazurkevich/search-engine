package org.search.engine.index;

import org.search.engine.analyzer.Tokenizer;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class IndexationSchedulerTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IndexationSchedulerTask.class);

    private final BlockingQueue<DocumentLine> documentLinesQueue;
    private final SearchEngineTree index;
    private final Tokenizer tokenizer;
    private boolean isFinished;

    IndexationSchedulerTask(BlockingQueue<DocumentLine> documentLinesQueue, SearchEngineTree index, Tokenizer tokenizer) {
        this.documentLinesQueue = documentLinesQueue;
        this.index = index;
        this.tokenizer = tokenizer;
    }

    @Override
    public void run() {
        while (!documentLinesQueue.isEmpty()) {
            DocumentLine documentLine = documentLinesQueue.poll();
            if (documentLine != null) {
                int documentId = documentLine.getDocumentId();
                tokenizer.tokenize(documentLine.getDocumentRow())
                        .forEach(token -> index.putMergeOnConflict(token, documentId));
            }
            isFinished = true;
        }
        if (isFinished) {
            LOG.info("Indexation finished, queue is empty");
            isFinished = false;
        }
    }
}
