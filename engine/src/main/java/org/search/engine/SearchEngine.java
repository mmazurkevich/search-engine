package org.search.engine;

import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.exception.SearchEngineInitializationException;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.index.Document;
import org.search.engine.index.DocumentIndexManager;
import org.search.engine.search.IndexSearchManager;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SearchEngine {

    private static final Logger LOG = Logger.getLogger(SearchEngine.class.getName());

    private final SearchEngineConcurrentTree index;
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
            indexedDocuments = new ArrayList<>();
            filesystemManager = new FilesystemNotificationManager(watchService);
            indexManager = new DocumentIndexManager(index, indexedDocuments, filesystemManager, tokenizer);
            searchManager = new IndexSearchManager(index, indexedDocuments);
        } catch (IOException e) {
            throw new SearchEngineInitializationException();
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
