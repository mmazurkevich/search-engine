package org.search.engine.index;

import org.search.engine.SearchEngineExecutors;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.filesystem.FilesystemEvent;
import org.search.engine.filesystem.FilesystemEventListener;
import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.model.Document;
import org.search.engine.model.IndexChanges;
import org.search.engine.model.IndexationEvent;
import org.search.engine.model.IndexationTracker;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Class responsible for indexation of folders or file file also handle events coming from
 * filesystem notifier. Each action start separate task to handle actions independently
 * and to speed up process of indexation.
 */
public class DocumentIndexManager implements FilesystemEventListener, IndexationEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentIndexManager.class);
    private static final int QUEUE_CAPACITY = 500_000;

    //Unique concurrent document Id generator
    private final AtomicInteger uniqueDocumentId;
    private final FilesystemNotifier notificationManager;
    private final Map<Path, Document> indexedDocuments;
    private final BlockingQueue<IndexationEvent> documentQueue;
    private final SearchEngineTree index;
    private final Tokenizer tokenizer;
    private final ExecutorService indexingExecutorService;
    private ScheduledExecutorService indexationExecutor;
    private final List<IndexationEventListener> listeners = new CopyOnWriteArrayList<>();

    //Tracking current indexation
    private IndexationTracker currentIndexationTracker;

    public DocumentIndexManager(SearchEngineTree index, Map<Path, Document> indexedDocuments, FilesystemNotifier notificationManager,
                                Tokenizer tokenizer, AtomicInteger uniqueDocumentId, IndexChanges indexChanges) {
        this.notificationManager = notificationManager;
        this.indexedDocuments = indexedDocuments;
        this.documentQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        this.tokenizer = tokenizer;
        this.index = index;
        this.uniqueDocumentId = uniqueDocumentId;
        this.indexingExecutorService = SearchEngineExecutors.getExecutorService();
        applyIndexChangesIfNeeded(indexChanges);
        notificationManager.addListener(this);
        scheduleIndexationIfNeeded();
        listeners.add(this);
    }

    /**
     * Method validate incoming params and start method for concurrent files indexation in given folder
     *
     * @param path The path to the folder which should be indexed
     */
    public void indexFolder(String path, IndexationEventListener listener) {
        if (path == null || path.isEmpty() || listener == null) {
            throw new IllegalArgumentException("Folder path must not be null or empty");
        }
        Path folderPath = Paths.get(path).normalize();
        try {
            //Check that folder is registered and should not be indexed again (not a clean solution)
            if (hasAccess(folderPath) && !notificationManager.isFolderRegistered(folderPath)) {
                //Register indexing folder parent for tracking itself folder delete
                if (folderPath.getParent() != null) {
                    notificationManager.registerParentFolder(folderPath.getParent());
                }

                currentIndexationTracker = new IndexationTracker(listener, folderPath);
                LOG.info("Calculating overall files count");
                double percentage = (double) getFilesCount(folderPath) / 100;
                AtomicInteger documentCount = new AtomicInteger(0);
                Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        indexFileWithTrackProgress(file, documentCount, percentage);
                        return getFileVisitResult();
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        if (Files.isHidden(path)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path folder, IOException exc) throws IOException {
                        notificationManager.registerFolder(folder);
                        return getFileVisitResult();
                    }

                    private FileVisitResult getFileVisitResult() {
                        if (currentIndexationTracker.isCanceled())
                            return FileVisitResult.TERMINATE;
                        else
                            return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                listener.onIndexationFinished();
                LOG.warn("Folder already indexed or no access to folder: {}", folderPath.toAbsolutePath());
            }
        } catch (IOException ex) {
            LOG.warn("Folder indexation with exception: {}", folderPath.toAbsolutePath(), ex);
        }
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
                notificationManager.unregisterFolder(folderPath);
                List<Document> documents = indexedDocuments.entrySet().stream()
                        .filter(entry -> entry.getValue().getPath().startsWith(folderPath))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList());
                documents.forEach(it -> {
                    removeDocumentFromIndex(it);
                    notificationManager.unregisterFolder(it.getParent());
                });
                break;
        }
    }

    public void cancelIndexation() {
        if (currentIndexationTracker != null) {
            currentIndexationTracker.setCanceled(true);
            currentIndexationTracker.getIndexingFutures().forEach(it -> {
                if (!it.isDone()) {
                    it.cancel(false);
                }
            });
            documentQueue.clear();
            //In case of long rollback comment this line due to long delete from index
            //onFolderChanged(FilesystemEvent.DELETED, currentIndexationTracker.getIndexingFolder());
        } else {
            LOG.info("There is nothing to cancel");
        }
    }

    public void addListener(IndexationEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public boolean removeListener(IndexationEventListener listener) {
        if (listener != null)
            return listeners.remove(listener);
        else
            return false;
    }


    @Override
    public void onIndexationFinished() {
        if (currentIndexationTracker != null) {
            currentIndexationTracker.getListener().onIndexationFinished();
        }
    }

    @Override
    public void onIndexationProgress(int progress) {
    }

    public void invalidateCache() {
        uniqueDocumentId.set(0);
        indexedDocuments.clear();
        index.clear();
        LOG.info("Cache invalidated");
    }

    private void applyIndexChangesIfNeeded(IndexChanges indexChanges) {
        if (indexChanges != null) {
            indexChanges.getNewFiles().forEach(file -> onFileChanged(FilesystemEvent.CREATED, file));
            indexChanges.getChangedFiles().forEach(file -> onFileChanged(FilesystemEvent.MODIFIED, file));
            indexChanges.getOldFiles().forEach(file -> onFileChanged(FilesystemEvent.DELETED, file));

            indexChanges.getNewFolders().forEach(folder -> onFolderChanged(FilesystemEvent.CREATED, folder));
            indexChanges.getOldFolders().forEach(folder -> onFolderChanged(FilesystemEvent.DELETED, folder));
            LOG.info("Finish applying index changes");
        }
    }

    private void indexFolder(Path folderPath) {
        try {
            //Check that folder is registered and should not be indexed again (not a clean solution)
            if (hasAccess(folderPath) && !notificationManager.isFolderRegistered(folderPath)) {
                Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        indexFile(file, false);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        if (Files.isHidden(path)) {
                            LOG.debug("Folder hidden and will be skipped {}", path);
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path folder, IOException exc) throws IOException {
                        notificationManager.registerFolder(folder);
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

    private void indexFileWithTrackProgress(Path filePath, AtomicInteger documentCount, double percentage) {
        try {
            if (hasAccess(filePath) && !isFileIndexed(filePath)) {
                Document document = new Document(uniqueDocumentId.incrementAndGet(), false, filePath,
                        Files.getLastModifiedTime(filePath).toMillis());
                DocumentReadWithTrackProgressTask task = new DocumentReadWithTrackProgressTask(document, indexedDocuments, documentQueue,
                        currentIndexationTracker.getListener(), documentCount, percentage);
                Future<?> submit = indexingExecutorService.submit(task);
                currentIndexationTracker.getIndexingFutures().add(submit);
            } else {
                LOG.warn("File already indexed or no access to file: {}", filePath.toAbsolutePath());
            }
        } catch (IOException ex) {
            LOG.warn("File indexation with exception: {}", filePath.toAbsolutePath(), ex);
        }
    }

    private void indexFile(Path filePath, boolean shouldTrack) {
        try {
            if (hasAccess(filePath) && !isFileIndexed(filePath)) {
                Document document = new Document(uniqueDocumentId.incrementAndGet(), shouldTrack, filePath,
                        Files.getLastModifiedTime(filePath).toMillis());
                DocumentReadTask task = new DocumentReadTask(document, indexedDocuments, documentQueue, notificationManager);
                indexingExecutorService.execute(task);
            } else {
                LOG.warn("File already indexed or no access to file: {}", filePath.toAbsolutePath());
            }
        } catch (IOException ex) {
            LOG.warn("File indexation with exception: {}", filePath.toAbsolutePath(), ex);
        }
    }

    private void removeDocumentFromIndex(Document removableDocument) {
        if (removableDocument != null) {
            DocumentRemoveTask task = new DocumentRemoveTask(removableDocument, index, indexedDocuments, documentQueue, notificationManager);
            indexingExecutorService.execute(task);
        }
    }

    private void reindexFile(Path filePath) {
        if (hasAccess(filePath)) {
            Document updatingDocument = null;
            for (Map.Entry<Path, Document> documentEntry : indexedDocuments.entrySet()) {
                Document document = documentEntry.getValue();
                if (filePath.equals(document.getPath())) {
                    updatingDocument = document;
                }
            }

            if (updatingDocument != null) {
                DocumentUpdateTask task = new DocumentUpdateTask(updatingDocument, index, tokenizer, documentQueue);
                indexingExecutorService.submit(task);
            }
        } else {
            LOG.warn("Doesn't have access to the file: {}", filePath.toAbsolutePath());
        }
    }

    private boolean isFileIndexed(Path filePath) {
        return indexedDocuments.containsKey(filePath);
    }

    private boolean hasAccess(Path path) {
        if (Files.isRegularFile(path))
            return Files.isReadable(path);
        else
            return Files.exists(path) && Files.isDirectory(path);
    }

    private void scheduleIndexationIfNeeded() {
        if (indexationExecutor == null) {
            indexationExecutor = SearchEngineExecutors.getScheduledExecutor();
            IndexationSchedulerTask indexScheduler = new IndexationSchedulerTask(documentQueue, index, tokenizer, listeners);
            indexationExecutor.scheduleWithFixedDelay(indexScheduler, 0, 1, TimeUnit.SECONDS);
        }
    }

    private long getFilesCount(Path folderPath) {
        long count = 0;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(folderPath)) {
            for (Path path : dirStream) {
                if (Files.isDirectory(path) && !Files.isHidden(path)) {
                    count += getFilesCount(path);
                } else {
                    count++;
                }
            }
        } catch (IOException ex) {
            LOG.warn("Exception during files count calculation");
        }
        return count;
    }

}
