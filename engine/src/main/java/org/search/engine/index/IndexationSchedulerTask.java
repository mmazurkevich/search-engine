package org.search.engine.index;

import org.search.engine.analyzer.Tokenizer;
import org.search.engine.model.IndexationEvent;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Scheduler task responsible for handling document queue and
 * update index
 */
public class IndexationSchedulerTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IndexationSchedulerTask.class);

    private final BlockingQueue<IndexationEvent> documentLinesQueue;
    private final SearchEngineTree index;
    private final Tokenizer tokenizer;
    private final List<IndexationEventListener> listeners;
    private boolean isFinished;

    IndexationSchedulerTask(BlockingQueue<IndexationEvent> documentLinesQueue, SearchEngineTree index, Tokenizer tokenizer, List<IndexationEventListener> listeners) {
        this.documentLinesQueue = documentLinesQueue;
        this.index = index;
        this.tokenizer = tokenizer;
        this.listeners = listeners;
    }

    @Override
    public void run() {
        while (!documentLinesQueue.isEmpty()) {
            IndexationEvent indexationEvent = documentLinesQueue.poll();
            if (indexationEvent != null) {
                switch (indexationEvent.getType()) {
                    case ADD:
                        index.putMergeOnConflict(indexationEvent.getContent(), indexationEvent.getDocumentId());
                        break;
                    case ADD_LINE:
                        int documentId = indexationEvent.getDocumentId();
                        tokenizer.tokenize(indexationEvent.getContent())
                                .forEach(token -> index.putMergeOnConflict(token, documentId));
                        break;
                    case REMOVE:
                        index.removeByKeyAndValue(indexationEvent.getContent(), indexationEvent.getDocumentId());
                        break;
                }
            }
            isFinished = true;
        }

        if (isFinished) {
            LOG.info("Indexation finished, queue is empty");
            listeners.forEach(IndexationEventListener::onIndexationFinished);
            isFinished = false;
        }
    }
}
