package org.search.engine;

import io.reactivex.subjects.ReplaySubject;
import org.junit.Before;
import org.junit.Test;
import org.search.engine.analyzer.WhitespaceTokenizer;
import org.search.engine.index.IndexationEventListener;
import org.search.engine.model.SearchResultEvent;
import org.search.engine.model.SearchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SearchEngineTest {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngineTest.class);

    private SearchEngine searchEngine;

    private IndexationEventListener listener = new IndexationEventListener() {
        @Override
        public void onIndexationProgress(int progress) { }

        @Override
        public void onIndexationFinished() { }
    };

    @Before
    public void setUp() {
        searchEngine = new SearchEngine(new WhitespaceTokenizer());
        searchEngine.initialize(progress -> { });
        searchEngine.invalidateCache();
    }

    @Test
    public void testFolderIndexationAndSearch() throws URISyntaxException, InterruptedException {
        URL resource = SearchEngineTest.class.getResource("/testFolder");
        searchEngine.indexFolder(resource.toURI().getRawPath(), listener);

        Thread.sleep(2000);

        String searchQuery = "mila";
        ReplaySubject<SearchResultEvent> replaySubject = searchEngine.search(searchQuery, SearchType.EXACT_MATCH);

        List<SearchResultEvent> results = new ArrayList<>();
        replaySubject.subscribe(results::add);
        assertEquals(2, results.size());
        LOG.debug("Document1: {}, Document2: {}", results.get(0).getFileName(), results.get(1).getFileName());

        resource = SearchEngineTest.class.getResource("/TestFileOne.txt");
        searchEngine.indexFile(resource.toURI().getRawPath());

        Thread.sleep(2000);

        searchQuery = "relieve";
        replaySubject = searchEngine.search(searchQuery, SearchType.EXACT_MATCH);

        results = new ArrayList<>();
        replaySubject.subscribe(results::add);
        assertEquals(1, results.size());
        LOG.debug("Document: {}", results.get(0).getFileName());
        searchEngine.invalidateCache();
    }
}