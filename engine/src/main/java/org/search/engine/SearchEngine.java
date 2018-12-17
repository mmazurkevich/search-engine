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
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main class of the library for in-memory documents/folders indexation.
 * It can be customized by using different implementations of tokenizer.
 * You can user your oun implementation of it for splitting the your document's
 * to the needed lexemes. Under the hood this lib use radix tree structure
 * for storing lexemes and document identifiers. NOTE: Each instance of this class
 * contains it's own index.
 */
public class SearchEngine {

    private final Map<Path, Document> indexedDocuments = new ConcurrentHashMap<>();
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

    /**
     * Method for indexing folder documents.
     *
     * @param path The path to the indexing folder
     */
    public void indexFolder(String path) {
        indexManager.indexFolder(path);
    }

    /**
     * Method for indexing document.
     *
     * @param path The path to the indexing document
     */
    public void indexFile(String path) {
        indexManager.indexFile(path);
    }

    /**
     * Method for searching certain query in the current index
     *
     * @param searchQuery The query which should be searched in the index
     * @return The list of documents which contains the searched lexeme
     */
    public List<String> search(String searchQuery) {
        return searchManager.searchByQuery(searchQuery);
    }

    @Override
    protected void finalize() throws IOException {
        if (watchService != null)
            watchService.close();
    }
}
