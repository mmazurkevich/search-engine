package org.search.engine.index;

import org.search.engine.SearchEngineExecutors;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.filesystem.FilesystemEvent;
import org.search.engine.filesystem.FilesystemEventListener;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class DocumentIndexManager implements FilesystemEventListener {

    private static final Logger LOG = Logger.getLogger(DocumentIndexManager.class.getName());

    private final FilesystemNotificationManager notificationManager;
    private final List<Document> indexedDocuments;
    private final ExecutorService indexingExecutorService;
    private final SearchEngineConcurrentTree index;
    private final Tokenizer tokenizer;
    private final AtomicInteger uniqueDocumentId;

    public DocumentIndexManager(SearchEngineConcurrentTree index, FilesystemNotificationManager notificationManager,
                                Tokenizer tokenizer) {
        this.index = index;
        this.notificationManager = notificationManager;
        this.tokenizer = tokenizer;
        indexedDocuments = new ArrayList<>();
        uniqueDocumentId = new AtomicInteger();
        indexingExecutorService = SearchEngineExecutors.getDocumentIndexingExecutor();
        notificationManager.addListener(this);
    }

    @Override
    public void onFileChanged(FilesystemEvent event, Path filePath) {
        System.out.println("Handling event:" + event + " for file: " + filePath);
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
        //Doesn't handle folder MODIFIED event
        System.out.println("Handling event:" + event + " for folder: " + folderPath);
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
                } catch (IOException e) {
                    LOG.warning("Unhandled DELETE event for folder: " + folderPath.toAbsolutePath().toString());
                    e.printStackTrace();
                }
                break;
        }
    }

    public void indexFolder(String path) {
        Path folderPath = Paths.get(path).normalize();
        indexFolder(folderPath);
    }

    public void indexFile(String path) {
        Path filePath = Paths.get(path).normalize();
        indexFile(filePath, true);
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
                LOG.warning("Doesn't have access to the folder");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void indexFile(Path filePath, boolean shouldTrack) {
        try {
            if (hasAccess(filePath) && !isFileIndexed(filePath)) {
                Document document = new Document(uniqueDocumentId.incrementAndGet(), shouldTrack, filePath);
                DocumentIndexTask task = new DocumentIndexTask(document, index, indexedDocuments, notificationManager,
                        tokenizer, shouldTrack);
                indexingExecutorService.execute(task);
            } else {
                LOG.warning("Doesn't have access to the file");
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            DocumentRemoveTask task = new DocumentRemoveTask(removableDocument, index, indexedDocuments);
            indexingExecutorService.execute(task);
        }
    }

    private void reindexFile(Path filePath) {
        try {
            if (hasAccess(filePath)) {
                Document removableDocument = null;
                for (Document document : indexedDocuments) {
                    if (Files.isSameFile(filePath, document.getPath())) {
                        removableDocument = document;
                    }
                }
                if (removableDocument != null) {
                    DocumentRemoveTask task = new DocumentRemoveTask(removableDocument, index, indexedDocuments);
                    indexingExecutorService.submit(task).get();
                    indexFile(filePath, removableDocument.isTracked());
                }
            } else {
                LOG.warning("Doesn't have access to the file");
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
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
