package org.search.engine.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;
import static org.search.engine.filesystem.FilesystemEvent.*;

public class FilesystemNotificationScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemNotificationScheduler.class);

    private final WatchService watchService;
    private final Map<WatchKey, Path> registeredFolders;
    private final List<WatchServiceEventListener> listeners = new ArrayList<>();

    FilesystemNotificationScheduler(Map<WatchKey, Path> registeredFolders, WatchService watchService) {
        this.registeredFolders = registeredFolders;
        this.watchService = watchService;
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
        } catch (InterruptedException ex) {
            LOG.warn("Scheduler interrupted while retrieves watch keys", ex);
        }
    }

    void addListener(WatchServiceEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
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
