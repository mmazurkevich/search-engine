package org.search.engine.index;

import org.search.engine.SearchEngineExecutors;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.filesystem.FilesystemEvent;
import org.search.engine.filesystem.FilesystemEventListener;
import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class responsible for indexation of folders or file file also handle events coming from
 * filesystem notifier. Each action start separate task to handle actions independently
 * and to speed up process of indexation.
 */
public class DocumentIndexManager implements FilesystemEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentIndexManager.class);
    private static final int QUEUE_CAPACITY = 3_000_000;

    //Unique concurrent document Id generator
    private final AtomicInteger uniqueDocumentId = new AtomicInteger();
    private final FilesystemNotifier notificationManager;
    private final Map<Path, Document> indexedDocuments;
    private final BlockingQueue<DocumentLine> documentLinesQueue;
    private final SearchEngineTree index;
    private final Tokenizer tokenizer;
    private final ExecutorService indexingExecutorService;
    private ScheduledExecutorService indexationExecutor;

    public DocumentIndexManager(SearchEngineTree index,  Map<Path, Document> indexedDocuments, FilesystemNotifier notificationManager,
                                Tokenizer tokenizer) {
        this.notificationManager = notificationManager;
        this.indexedDocuments = indexedDocuments;
        this.documentLinesQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        this.tokenizer = tokenizer;
        this.index = index;
        this.indexingExecutorService = SearchEngineExecutors.getExecutorService();
        notificationManager.addListener(this);
    }

    /**
     * Method validate incoming params and start method for concurrent files indexation in given folder
     *
     * @param path The path to the folder which should be indexed
     */
    public void indexFolder(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Folder path must not be null or empty");
        }
        Path folderPath = Paths.get(path).normalize();
        indexFolder(folderPath);
    }

    /**
     * Method validate incoming params and start method for concurrent file indexation
     *
     * @param path The path to the file which should be indexed
     */
    public void indexFile(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("File path must not be null or empty");
        }
        Path filePath = Paths.get(path).normalize();
        indexFile(filePath, true);
    }

    @Override
    public void onFileChanged(FilesystemEvent event, Path filePath) {
        LOG.debug("Handling event: {}  for file: {}", event, filePath);
        switch (event) {
            case CREATED:
                indexFile(filePath, false);
                break;
            case MODIFIED:
                if (isFileIndexed(filePath)) {
                    reindexFile(filePath);
                }
                break;
            case DELETED:
                for (Map.Entry<Path, Document> documentEntry : indexedDocuments.entrySet()) {
                    Document document = documentEntry.getValue();
                    if (filePath.equals(document.getPath())) {
                        removeDocumentFromIndex(document);
                    }
                }
                break;
        }
    }

    @Override
    public void onFolderChanged(FilesystemEvent event, Path folderPath) {
        LOG.debug("Handling event: {}  for folder: {}", event, folderPath);
        switch (event) {
            case CREATED:
                indexFolder(folderPath);
                break;
            case DELETED:
                for (Map.Entry<Path, Document> documentEntry : indexedDocuments.entrySet()) {
                    Document document = documentEntry.getValue();
                    if (document.getPath().startsWith(folderPath)) {
                        removeDocumentFromIndex(document);
                        notificationManager.unregisterFolder(document.getParent());
                    }
                }
                break;
        }
    }

    private void indexFolder(Path folderPath) {
        try {
            //Check that folder is registered and should not be indexed again (not a clean solution)
            if (hasAccess(folderPath) && !notificationManager.isFolderRegistered(folderPath)) {
                //Register indexing folder parent for tracking itself folder delete
                if (folderPath.getParent() != null) {
                    notificationManager.registerParentFolder(folderPath.getParent());
                }
                Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        indexFile(file, false);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        notificationManager.registerFolder(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                LOG.warn("Folder already indexed or no access to folder: {}", folderPath.toAbsolutePath());
            }
        } catch (IOException ex) {
            LOG.warn("Folder indexation with exception: {}", folderPath.toAbsolutePath(), ex);
        }
    }

    private void indexFile(Path filePath, boolean shouldTrack) {
        try {
            if (hasAccess(filePath) && !isFileIndexed(filePath)) {
                Document document = new Document(uniqueDocumentId.incrementAndGet(), shouldTrack, filePath);
                DocumentReadTask task = new DocumentReadTask(document, indexedDocuments, documentLinesQueue, notificationManager);
                indexingExecutorService.execute(task);
                scheduleIndexationIfNeeded();
            } else {
                LOG.warn("File already indexed or no access to file: {}", filePath.toAbsolutePath());
            }
        } catch (IOException ex) {
            LOG.warn("File indexation with exception: {}", filePath.toAbsolutePath(), ex);
        }
    }

    private void removeDocumentFromIndex(Document removableDocument) {
        if (removableDocument != null) {
            DocumentRemoveTask task = new DocumentRemoveTask(removableDocument, index, indexedDocuments, notificationManager);
            indexingExecutorService.execute(task);
        }
    }

    private void reindexFile(Path filePath) {
        try {
            if (hasAccess(filePath)) {
                Document updatingDocument = null;
                for (Map.Entry<Path, Document> documentEntry : indexedDocuments.entrySet()) {
                    Document document = documentEntry.getValue();
                    if (Files.isSameFile(filePath, document.getPath())) {
                        updatingDocument = document;
                    }
                }

                if (updatingDocument != null) {
                    DocumentUpdateTask task = new DocumentUpdateTask(updatingDocument, index, tokenizer);
                    indexingExecutorService.submit(task);
                }
            } else {
                LOG.warn("Doesn't have access to the file: {}", filePath.toAbsolutePath());
            }
        } catch (IOException ex) {
            LOG.warn("File reindexation with exception: {}", filePath.toAbsolutePath(), ex);
        }
    }

    private boolean isFileIndexed(Path filePath) {
        return indexedDocuments.containsKey(filePath);
    }

    private boolean hasAccess(Path path) throws IOException {
        if (Files.isRegularFile(path))
            return Files.isReadable(path) && !Files.isHidden(path);
        else
            return Files.exists(path) && Files.isDirectory(path);
    }

    private void scheduleIndexationIfNeeded() {
        if (indexationExecutor == null) {
            indexationExecutor = SearchEngineExecutors.getScheduledExecutor();
            int schedulerThreads = SearchEngineExecutors.getSchedulerThreads();
            for (int i= 0; i < schedulerThreads; i++) {
                IndexationSchedulerTask indexScheduler = new IndexationSchedulerTask(documentLinesQueue, index, tokenizer);
                indexationExecutor.scheduleWithFixedDelay(indexScheduler, 0,1, TimeUnit.SECONDS);
            }
        }
    }
}
