package org.search.engine.search;

import org.search.engine.index.Document;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IndexSearchManager {

    private static final Logger LOG = LoggerFactory.getLogger(IndexSearchManager.class);

    private final SearchEngineTree index;
    private final List<Document> indexedDocuments;

    public IndexSearchManager(SearchEngineTree index, List<Document> indexedDocuments) {
        this.index = index;
        this.indexedDocuments = indexedDocuments;
    }

    public List<String> searchByQuery(String searchQuery) {
        if (searchQuery != null && !searchQuery.isEmpty()) {
            LOG.debug("Searching documents by query: {}", searchQuery);
            List<Integer> value = index.getValue(searchQuery);
            if (!value.isEmpty()) {
                LOG.debug("Founded documents count: {}", value.size());
                return indexedDocuments.stream().filter(document -> value.contains(document.getId()))
                        .map(document -> document.getPath().toAbsolutePath().toString())
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

}
