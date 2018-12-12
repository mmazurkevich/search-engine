package org.search.engine.index;

import org.search.engine.SearchEngineExecutors;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.filesystem.FilesystemEvent;
import org.search.engine.filesystem.FilesystemEventListener;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class DocumentIndexManager implements FilesystemEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentIndexManager.class);

    private final ExecutorService indexingExecutorService = SearchEngineExecutors.getDocumentIndexingExecutor();
    private final AtomicInteger uniqueDocumentId = new AtomicInteger();
    private final FilesystemNotificationManager notificationManager;
    private final List<Document> indexedDocuments;
    private final SearchEngineTree index;
    private final Tokenizer tokenizer;

    public DocumentIndexManager(SearchEngineTree index, List<Document> indexedDocuments, FilesystemNotificationManager notificationManager,
                                Tokenizer tokenizer) {
        this.notificationManager = notificationManager;
        this.indexedDocuments = indexedDocuments;
        this.tokenizer = tokenizer;
        this.index = index;
        notificationManager.addListener(this);
    }

    public void indexFolder(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Folder path must not be null or empty");
        }
        Path folderPath = Paths.get(path).normalize();
        indexFolder(folderPath);
    }

    public void  indexFile(String path) {
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
                reindexFile(filePath);
                break;
            case DELETED:
                removeFileFromIndex(filePath);
                break;
        }
    }

    @Override
    public void onFolderChanged(FilesystemEvent event, Path folderPath) {
        //We doesn't handle folder MODIFIED event
        LOG.debug("Handling event: {}  for folder: {}", event, folderPath);
        switch (event) {
            case CREATED:
                indexFolder(folderPath);
                break;
            case DELETED:
                try {
                    for (Document document : indexedDocuments) {
                        if (Files.isSameFile(folderPath, document.getParent())) {
                            removeFileFromIndex(document.getPath());
                        }
                    }
                } catch (IOException ex) {
                    LOG.warn("Unhandled DELETE event for folder: {}", folderPath.toAbsolutePath(), ex);
                }
                break;
        }
    }

    private void indexFolder(Path folderPath) {
        try {
            if (hasAccess(folderPath)) {
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
                LOG.warn("Doesn't have access to the folder: {}", folderPath.toAbsolutePath());
            }
        } catch (IOException ex) {
            LOG.warn("Folder indexation with exception: {}", folderPath.toAbsolutePath(), ex);
        }
    }

    private void indexFile(Path filePath, boolean shouldTrack) {
        try {
            if (hasAccess(filePath) && !isFileIndexed(filePath)) {
                Document document = new Document(uniqueDocumentId.incrementAndGet(), shouldTrack, filePath);
                DocumentIndexTask task = new DocumentIndexTask(document, index, indexedDocuments, notificationManager,
                        tokenizer);
                indexingExecutorService.execute(task);
            } else {
                LOG.warn("Doesn't have access to the file: {}", filePath.toAbsolutePath());
            }
        } catch (IOException ex) {
            LOG.warn("File indexation with exception: {}", filePath.toAbsolutePath(), ex);
        }
    }

    private void removeFileFromIndex(Path filePath) {
        Document removableDocument = null;
        for (Document document : indexedDocuments) {
            if (filePath.equals(document.getPath())) {
                removableDocument = document;
            }
        }
        if (removableDocument != null) {
            DocumentRemoveTask task = new DocumentRemoveTask(removableDocument, index, indexedDocuments, notificationManager);
            indexingExecutorService.execute(task);
        }
    }

    private void reindexFile(Path filePath) {
        try {
            if (hasAccess(filePath)) {
                Document updatingDocument = null;
                for (Document document : indexedDocuments) {
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

    private boolean isFileIndexed(Path filePath) throws IOException {
        for (Document document : indexedDocuments) {
            if (Files.isSameFile(filePath, document.getPath())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAccess(Path path) throws IOException {
        if (Files.isRegularFile(path))
            return Files.isReadable(path) && !Files.isHidden(path);
        else
            return Files.exists(path) && Files.isDirectory(path);
    }
}
