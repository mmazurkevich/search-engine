package org.search.app;

import io.reactivex.subjects.ReplaySubject;
import org.junit.Before;
import org.junit.Test;
import org.search.engine.SearchEngine;
import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.index.IndexationEventListener;
import org.search.engine.model.SearchResultEvent;
import org.search.engine.model.SearchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class SearchEngineAppTest {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngineApp.class);
    private static final int RANDOM_WORDS_COUNT = 100;

    private Tokenizer tokenizer;
    private SearchEngine searchEngine;
    private IndexationEventListener listener;
    private boolean isFinished;

    @Before
    public void setUp() {
        tokenizer = new StandardTokenizer();
        searchEngine = new SearchEngine(tokenizer);
        searchEngine.initialize();
        searchEngine.invalidateCache();
        listener = new IndexationEventListener() {
            @Override
            public void onIndexationProgress(int progress) { }

            @Override
            public void onIndexationFinished() {
                isFinished = true;
            }
        };
    }

    @Test
    public void testSearchEngineApp() throws URISyntaxException, InterruptedException {
        URL resource = SearchEngineApp.class.getResource("/shakespeare");

        searchEngine.indexFolder(resource.toURI().getRawPath(), listener);
        while (!isFinished) {
            Thread.sleep(500);
        }

        //Getting random file for check
        URL defaultResource = SearchEngineApp.class.getResource("/shakespeare/poetry/a_lovers_complaint.txt");
        Path filePath = Paths.get(defaultResource.toURI());
        try {
            List<Path> paths = Files.walk(Paths.get(resource.toURI()))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            Collections.shuffle(paths);
            filePath = paths.get(0);
        } catch (IOException ex) {
            LOG.warn("Exception in walking through the folder", ex);
        }
        String fileName = filePath.toAbsolutePath().toString();

        LOG.debug("File name: {}", fileName);
        Set<String> dictionary = new HashSet<>();
        try (Stream<String> lines = Files.lines(filePath)) {
            lines.forEach(line -> tokenizer.tokenize(line).forEach(it -> dictionary.add(it.getContent())));
        } catch (IOException ex) {
            LOG.warn("Reading of file finished with exception");
        }
        List<String> list = new ArrayList<>(dictionary);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        List<Future> futures = new ArrayList<>();

        Collections.shuffle(list);
        list.stream()
                .limit(RANDOM_WORDS_COUNT)
                .forEach(it -> futures.add(executorService.submit(new SearchTask(it, searchEngine, fileName))));

        futures.forEach(it -> {
            try {
                assertEquals(true, it.get());
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Exception of getting result from searching task");
            }
        });
        searchEngine.invalidateCache();
    }

    class SearchTask implements Callable<Boolean> {

        private final String searchQuery;
        private final SearchEngine searchEngine;
        private final String fileName;

        SearchTask(String searchQuery, SearchEngine searchEngine, String fileName) {
            this.searchQuery = searchQuery;
            this.searchEngine = searchEngine;
            this.fileName = fileName;
        }

        @Override
        public Boolean call() {
            ReplaySubject<SearchResultEvent> replaySubject = searchEngine.search(searchQuery, SearchType.EXACT_MATCH);
            List<SearchResultEvent> results = new ArrayList<>();
            replaySubject.subscribe(results::add);
            return results.stream().anyMatch(it -> it.getFileName().equals(fileName));
        }
    }
}