package org.search.engine.search;

import org.search.engine.index.Document;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IndexSearchManager {

    private final SearchEngineConcurrentTree index;
    private final List<Document> indexedDocuments;

    public IndexSearchManager(SearchEngineConcurrentTree index, List<Document> indexedDocuments) {
        this.index = index;
        this.indexedDocuments = indexedDocuments;
    }

    public List<String> searchByQuery(String searchQuery) {
        if (searchQuery != null && !searchQuery.isEmpty()) {
            System.out.println(index);
            List<Integer> value = index.getValue(searchQuery);
            if (!value.isEmpty()) {
                return indexedDocuments.stream().filter(document -> value.contains(document.getId()))
                        .map(document -> document.getPath().toAbsolutePath().toString())
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

}
