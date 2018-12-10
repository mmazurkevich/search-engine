package org.search.engine.index;

import org.search.engine.tree.SearchEngineConcurrentTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DocumentRemoveTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentRemoveTask.class);

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
        LOG.info("Removing file: {}  from index took {}ms", removableDocument.getPath(), (end - start));
    }
}