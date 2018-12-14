package org.search.engine.search;

import org.search.engine.index.Document;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple search manager which search by single word and return matched results
 * by mapping them in indexed documents list.
 */
public class SimpleSearchManager implements SearchManager{

    private static final Logger LOG = LoggerFactory.getLogger(SimpleSearchManager.class);

    private final SearchEngineTree index;
    private final List<Document> indexedDocuments;

    public SimpleSearchManager(SearchEngineTree index, List<Document> indexedDocuments) {
        this.index = index;
        this.indexedDocuments = indexedDocuments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> searchByQuery(String searchQuery) {
        if (searchQuery != null && !searchQuery.isEmpty()) {
            LOG.debug("Searching documents by query: {}", searchQuery);
            Set<Integer> value = index.getValue(searchQuery);
            if (!value.isEmpty()) {
                List<String> results = indexedDocuments.stream().filter(document -> value.contains(document.getId()))
                        .map(document -> document.getPath().toAbsolutePath().toString())
                        .collect(Collectors.toList());
                LOG.debug("Found documents: {}", results.size());
                return results;
            }
        }
        return Collections.emptyList();
    }

}
