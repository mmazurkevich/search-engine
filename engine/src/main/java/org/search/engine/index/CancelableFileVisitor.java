package org.search.engine.index;

import org.search.engine.filesystem.FilesystemNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

public class CancelableFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger LOG = LoggerFactory.getLogger(CancelableFileVisitor.class);

    private final FilesystemNotifier notificationManager;
    private final DocumentIndexManager indexManager;
    private final long initialCount;
    private long visitedFilesCount = 0;
    private boolean canceled;
    private boolean finished;
    private Set<Path> visitedFiles = new HashSet<>();
    private Set<Path> visitedFolders = new HashSet<>();

    public CancelableFileVisitor(FilesystemNotifier notificationManager, DocumentIndexManager indexManager, Path folderPath) {
        this.notificationManager = notificationManager;
        this.indexManager = indexManager;
        initialCount = getFilesCount(folderPath);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        indexManager.indexFile(file, false);
        visitedFiles.add(file);
        visitedFilesCount++;
        return getFileVisitResult();
    }

    @Override
    public FileVisitResult postVisitDirectory(Path folder, IOException exc) {
        notificationManager.registerFolder(folder);
        visitedFolders.add(folder);
        return getFileVisitResult();
    }

    public Set<Path> getVisitedFiles() {
        return visitedFiles;
    }

    public Set<Path> getVisitedFolders() {
        return visitedFolders;
    }

    public void cancel() {
        canceled = true;
        LOG.warn("Canceling indexation task");
    }

    public void finish() {
        finished = true;
        LOG.warn("Indexation task finished");
    }

    public boolean isCanceled() {
        return canceled && finished;
    }

    public boolean isFinished() {
        return !canceled && finished;
    }

    private long getFilesCount(Path folderPath) {
        long count = 0;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(folderPath)) {
            for (Path path : dirStream) {
                if (Files.isDirectory(path)) {
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

    private FileVisitResult getFileVisitResult() {
        if (canceled)
            return FileVisitResult.TERMINATE;
        else
            return FileVisitResult.CONTINUE;
    }
}
