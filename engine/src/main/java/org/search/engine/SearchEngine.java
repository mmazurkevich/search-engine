package org.search.engine;

import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.exception.SearchEngineInitializationException;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.index.DocumentIndexManager;
import org.search.engine.search.SearchManager;
import org.search.engine.model.SearchResult;
import org.search.engine.search.SimpleSearchManager;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.List;

/**
 * The main class of the library for in-memory documents/folders indexation.
 * It can be customized by using different implementations of tokenizer.
 * You can user your oun implementation of it for splitting the your document's
 * to the needed lexemes. Under the hood this lib use radix tree structure
 * for storing lexemes and document identifiers. NOTE: Each instance of this class
 * contains it's own index.
 */
public class SearchEngine {

    private final WatchService watchService;
    private final DocumentIndexManager indexManager;
    private final SearchManager searchManager;
    private final FilesystemNotifier filesystemManager;
    private final SearchEngineInitializer engineInitializer;

    public SearchEngine() {
        this(new StandardTokenizer());
    }

    public SearchEngine(Tokenizer tokenizer) {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            engineInitializer = new SearchEngineInitializer();

            filesystemManager = new FilesystemNotificationManager(watchService, engineInitializer.getTrackedFiles(),
                    engineInitializer.getTrackedFolders());
            searchManager = new SimpleSearchManager(engineInitializer.getIndex(), engineInitializer.getIndexedDocuments(),
                    tokenizer);
            indexManager = new DocumentIndexManager(engineInitializer.getIndex(), engineInitializer.getIndexedDocuments(),
                    filesystemManager, tokenizer, engineInitializer.getUniqueDocumentId());
            indexManager.addListener(engineInitializer);
        } catch (IOException e) {
            throw new SearchEngineInitializationException("Can't initialize filesystem WatchService or can't create app system folder");
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
    public List<SearchResult> search(String searchQuery) {
        return searchManager.searchByQuery(searchQuery);
    }

    @Override
    protected void finalize() throws IOException {
        if (watchService != null)
            watchService.close();
    }
}
