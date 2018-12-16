package org.search.engine;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.search.engine.analyzer.WhitespaceTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SearchEngineTest {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngineTest.class);

    private SearchEngine searchEngine;

    @Before
    public void setUp() {
        searchEngine = new SearchEngine(new WhitespaceTokenizer());
    }

    @Ignore
    @Test
    public void testDocumentIndexationAndSearch() throws URISyntaxException, InterruptedException {
        URL resource = SearchEngineTest.class.getResource("/TestFileOne.txt");
        searchEngine.indexFile(resource.toURI().getRawPath());

        Thread.sleep(2000);

        String searchQuery = "relieve";
        List<String> searchResult = searchEngine.search(searchQuery);
        assertEquals(1, searchResult.size());
        LOG.debug("Document: {}", searchResult.get(0));
    }

    @Test
    public void testFolderIndexationAndSearch() throws URISyntaxException, InterruptedException {
        URL resource = SearchEngineTest.class.getResource("/testFolder");
        searchEngine.indexFolder(resource.toURI().getRawPath());

        Thread.sleep(2000);

        String searchQuery = "mila";
        List<String> searchResult = searchEngine.search(searchQuery);
        assertEquals(2, searchResult.size());
        LOG.debug("Document1: {}, Document2: {}", searchResult.get(0), searchResult.get(1));
    }
}