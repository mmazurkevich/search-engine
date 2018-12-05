package org.search.engine.filesystem;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.search.engine.filesystem.FilesystemEvent.CREATED;
import static org.search.engine.filesystem.FilesystemEvent.DELETED;
import static org.search.engine.filesystem.FilesystemEvent.MODIFIED;

public class FilesystemNotificationScheduler implements Runnable {

    private static final Logger LOG = Logger.getLogger(FilesystemNotificationScheduler.class.getName());

    private final WatchService watchService;
    private final Map<WatchKey, Path> registeredFolders;
    private final List<WatchServiceEventListener> listeners;

    FilesystemNotificationScheduler(Map<WatchKey, Path> registeredFolders, WatchService watchService) {
        this.registeredFolders = registeredFolders;
        this.watchService = watchService;
        listeners = new ArrayList<>();
    }

    public void run() {
        try {
            WatchKey key = watchService.take();
            if (key != null) {

                Path folderPath = registeredFolders.get(key);
                if (folderPath != null) {

                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path affectedPath = folderPath.resolve((Path) event.context());

                        if (Files.isDirectory(affectedPath)) {
                            if (event.kind().equals(ENTRY_CREATE)) {
                                listeners.forEach(it -> it.onFolderEvent(CREATED, affectedPath));
                            } else if (event.kind().equals(ENTRY_DELETE)) {
                                listeners.forEach(it -> it.onFolderEvent(DELETED, affectedPath));
                            } else if (event.kind().equals(ENTRY_MODIFY)) {
                                listeners.forEach(it -> it.onFolderEvent(MODIFIED, affectedPath));
                            }
                        } else {
                            if (event.kind().equals(ENTRY_CREATE)) {
                                listeners.forEach(it -> it.onFileEvent(CREATED, affectedPath));
                            } else if (event.kind().equals(ENTRY_DELETE)) {
                                listeners.forEach(it -> it.onFileEvent(DELETED, affectedPath));
                            } else if (event.kind().equals(ENTRY_MODIFY)) {
                                listeners.forEach(it -> it.onFileEvent(MODIFIED, affectedPath));
                            }
                        }
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        registeredFolders.remove(key);
                    }
                }
            }
        } catch (InterruptedException e) {
            LOG.warning("Scheduler interrupted while retrieves watch keys");
        }
    }

    void addListener(WatchServiceEventListener listener) {
        listeners.add(listener);
    }

    boolean removeListener(WatchServiceEventListener listener) {
        if (listener != null)
            return listeners.remove(listener);
        else
            return false;
    }

    interface WatchServiceEventListener {

        void onFolderEvent(FilesystemEvent event, Path folderPath);

        void onFileEvent(FilesystemEvent event, Path filePath);
    }
}
