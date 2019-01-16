package org.search.engine;

import io.reactivex.subjects.ReplaySubject;
import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.exception.SearchEngineInitializationException;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.index.DocumentIndexManager;
import org.search.engine.index.IndexationEventListener;
import org.search.engine.model.SearchResultEvent;
import org.search.engine.model.SearchType;
import org.search.engine.search.SearchManager;
import org.search.engine.search.SimpleSearchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;

/**
 * The main class of the library for in-memory documents/folders indexation.
 * It can be customized by using different implementations of tokenizer.
 * You can user your oun implementation of it for splitting the your document's
 * to the needed lexemes. Under the hood this lib use radix tree structure
 * for storing lexemes and document identifiers. NOTE: Each instance of this class
 * contains it's own index.
 */
public class SearchEngine {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngine.class);

    private final WatchService watchService;
    private final Tokenizer tokenizer;
    private DocumentIndexManager indexManager;
    private SearchManager searchManager;
    private FilesystemNotifier filesystemManager;
    private SearchEngineInitializer engineInitializer;

    public SearchEngine() {
        this(new StandardTokenizer());
    }

    public SearchEngine(Tokenizer tokenizer) {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            this.tokenizer = tokenizer;
        } catch (IOException e) {
            throw new SearchEngineInitializationException("Can't initialize filesystem WatchService or can't create app system folder");
        }
    }

    public void initialize(SearchEngineInitializationListener listener) {
        engineInitializer = new SearchEngineInitializer(listener);

        filesystemManager = new FilesystemNotificationManager(watchService, engineInitializer.getTrackedFiles(),
                engineInitializer.getTrackedFolders(), listener);
        searchManager = new SimpleSearchManager(engineInitializer.getIndex(), engineInitializer.getIndexedDocuments(),
                tokenizer);
        indexManager = new DocumentIndexManager(engineInitializer.getIndex(), engineInitializer.getIndexedDocuments(),
                filesystemManager, tokenizer, engineInitializer.getUniqueDocumentId(), engineInitializer.getIndexChanges());
        indexManager.addListener(engineInitializer);
    }

    /**
     * Method for indexing folder documents.
     *
     * @param path The path to the indexing folder
     */
    public void indexFolder(String path, IndexationEventListener listener) {
        if (indexManager != null) {
            indexManager.indexFolder(path, listener);
        } else {
            listener.onIndexationFinished();
            LOG.warn("Search engine not yet initialized");
        }
    }

    /**
     * Method for indexing document.
     *
     * @param path The path to the indexing document
     */
    public void indexFile(String path) {
        if (indexManager != null) {
            indexManager.indexFile(path);
        } else {
            LOG.warn("Search engine not yet initialized");
        }
    }

    public void cancelFolderIndexation() {
        if (indexManager != null) {
            indexManager.removeListener(engineInitializer);
            indexManager.cancelIndexation();
            filesystemManager.invalidateCache();
            indexManager.invalidateCache();
            engineInitializer.loadIndex(false, progress -> { });
            filesystemManager.applyIndexChangesIfNeeded(progress -> { });
            indexManager.addListener(engineInitializer);
        } else {
            LOG.warn("Search engine not yet initialized");
        }
    }

    /**
     * Method for searching certain query in the current index
     *
     * @param searchQuery The query which should be searched in the index
     * @return The list of documents which contains the searched lexeme
     */
    public ReplaySubject<SearchResultEvent> search(String searchQuery, SearchType searchType) {
        if (searchManager != null) {
            return searchManager.searchByQuery(searchQuery, searchType);
        } else {
            LOG.warn("Search engine not yet initialized");
            return null;
        }
    }

    public void invalidateCache() {
        if (filesystemManager != null && indexManager != null && engineInitializer != null) {
            filesystemManager.invalidateCache();
            indexManager.invalidateCache();
            engineInitializer.invalidateCache();
        } else {
            LOG.warn("Search engine not yet initialized");
        }
    }

    @Override
    protected void finalize() throws IOException {
        if (watchService != null)
            watchService.close();
    }
}
