package org.search.engine.search;

import org.search.engine.model.SearchResult;
import org.search.engine.model.SearchType;

import java.util.List;

/**
 * API for handling search queries to the created index of documents
 */
public interface SearchManager {

    /**
     * Search query in the separate index and return the matched paths
     *
     * @param searchQuery Search query which should be searched in the index
     * @return list of matched entities
     */
    List<SearchResult> searchByQuery(String searchQuery, SearchType searchType);
}
