package org.search.engine.search;

import org.search.engine.tree.SearchEngineConcurrentTree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndexSearchManager {

    private final SearchEngineConcurrentTree index;

    public IndexSearchManager(SearchEngineConcurrentTree index) {
        this.index = index;
    }

    public List<String> searchByQuery(String searchQuery) {
        if (searchQuery == null || searchQuery.isEmpty())
            return Collections.emptyList();
        else
            System.out.println(index.prettyPrint());
            //TODO:: Rewrite
            return Collections.singletonList(index.getValueForExactKey(searchQuery).toString());
    }

}
