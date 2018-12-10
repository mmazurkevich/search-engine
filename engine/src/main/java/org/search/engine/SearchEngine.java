package org.search.engine;

import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.exception.SearchEngineInitializationException;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.index.Document;
import org.search.engine.index.DocumentIndexManager;
import org.search.engine.search.IndexSearchManager;
import org.search.engine.tree.SearchEngineConcurrentTree;
import org.search.engine.tree.SearchEngineTree;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SearchEngine {

    private final SearchEngineTree index;
    private final List<Document> indexedDocuments;
    private final WatchService watchService;
    private final DocumentIndexManager indexManager;
    private final IndexSearchManager searchManager;
    private final FilesystemNotificationManager filesystemManager;

    public SearchEngine() {
        this(new StandardTokenizer());
    }

    public SearchEngine(Tokenizer tokenizer) {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            index = new SearchEngineConcurrentTree();
            indexedDocuments = new CopyOnWriteArrayList<>();
            filesystemManager = new FilesystemNotificationManager(watchService);
            indexManager = new DocumentIndexManager(index, indexedDocuments, filesystemManager, tokenizer);
            searchManager = new IndexSearchManager(index, indexedDocuments);
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
