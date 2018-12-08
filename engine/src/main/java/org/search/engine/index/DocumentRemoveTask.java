package org.search.engine.index;

import org.search.engine.tree.SearchEngineConcurrentTree;

import java.util.List;
import java.util.logging.Logger;

public class DocumentRemoveTask implements Runnable {

    private static final Logger LOG = Logger.getLogger(DocumentRemoveTask.class.getName());

    private final List<Document> indexedDocuments;
    private final SearchEngineConcurrentTree index;
    private final Document removableDocument;

    DocumentRemoveTask(Document removableDocument, SearchEngineConcurrentTree index, List<Document> indexedDocuments) {
        this.index = index;
        this.indexedDocuments = indexedDocuments;
        this.removableDocument = removableDocument;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        index.removeByValue(removableDocument.getId());
        indexedDocuments.remove(removableDocument);
        long end = System.currentTimeMillis();
        LOG.info("Removing file: " + removableDocument.getPath() + " from index took " + (end - start) + "ms");
    }
}
