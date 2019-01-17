package org.search.engine.search;

import io.reactivex.subjects.ReplaySubject;
import org.search.engine.model.SearchResultEvent;
import org.search.engine.model.SearchType;

import java.util.List;

/**
 * API for handling search queries to the created index of documents
 */
public interface SearchManager {

    /**
     * Search query in the separate index and return the matched paths
     *
     * @param searchQueries Search query which should be searched in the index
     * @return list of matched entities
     */
    ReplaySubject<SearchResultEvent> searchByQuery(List<String> searchQueries, SearchType searchType);

    void cancelPrevious();
}
