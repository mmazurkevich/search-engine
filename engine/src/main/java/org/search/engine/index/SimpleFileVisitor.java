package org.search.engine.index;

import org.search.engine.filesystem.FilesystemNotifier;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class SimpleFileVisitor extends java.nio.file.SimpleFileVisitor<Path> {

    private final FilesystemNotifier notificationManager;
    private final DocumentIndexManager indexManager;

    public SimpleFileVisitor(FilesystemNotifier notificationManager, DocumentIndexManager indexManager) {
        this.notificationManager = notificationManager;
        this.indexManager = indexManager;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        indexManager.indexFile(file, false);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path folder, IOException exc) {
        notificationManager.registerFolder(folder);
        return FileVisitResult.CONTINUE;
    }
}
