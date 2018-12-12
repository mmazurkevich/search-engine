package org.search.engine;

import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.exception.SearchEngineInitializationException;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.index.Document;
import org.search.engine.index.DocumentIndexManager;
import org.search.engine.search.SearchManager;
import org.search.engine.search.SimpleSearchManager;
import org.search.engine.tree.SearchEngineConcurrentTree;
import org.search.engine.tree.SearchEngineTree;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SearchEngine {

    private final List<Document> indexedDocuments = new CopyOnWriteArrayList<>();
    private final SearchEngineTree index = new SearchEngineConcurrentTree();
    private final WatchService watchService;
    private final DocumentIndexManager indexManager;
    private final SearchManager searchManager;
    private final FilesystemNotifier filesystemManager;

    public SearchEngine() {
        this(new StandardTokenizer());
    }

    public SearchEngine(Tokenizer tokenizer) {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            filesystemManager = new FilesystemNotificationManager(watchService);
            indexManager = new DocumentIndexManager(index, indexedDocuments, filesystemManager, tokenizer);
            searchManager = new SimpleSearchManager(index, indexedDocuments);
        } catch (IOException e) {
            throw new SearchEngineInitializationException("Can't initialize filesystem WatchService can't track file changes");
        }
    }

    public void indexFolder(String path) {
        indexManager.indexFolder(path);
    }

    public void indexFile(String path) {
        indexManager.indexFile(path);
    }

    public List<String> search(String searchQuery) {
        return searchManager.searchByQuery(searchQuery);
    }

    @Override
    protected void finalize() throws IOException {
        if (watchService != null)
            watchService.close();
    }
}
