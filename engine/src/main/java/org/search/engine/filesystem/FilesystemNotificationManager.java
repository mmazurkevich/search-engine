package org.search.engine.filesystem;

import org.search.engine.SearchEngineExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;
import static org.search.engine.filesystem.FilesystemEvent.DELETED;

public class FilesystemNotificationManager implements FilesystemNotificationScheduler.WatchServiceEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemNotificationManager.class);

    private static final int FILES_TRACK_DELAY_IN_SEC = 2;
    private final WatchService watchService;
    private final Map<WatchKey, Path> registeredFolders;
    private final List<Path> trackedFiles;
    private final List<Path> trackedFolders;
    private final List<FilesystemEventListener> listeners;
    private ScheduledExecutorService notificationExecutor;


    public FilesystemNotificationManager(WatchService watchService) {
        this.watchService = watchService;
        registeredFolders = new ConcurrentHashMap<>();
        trackedFiles = new CopyOnWriteArrayList<>();
        trackedFolders = new CopyOnWriteArrayList<>();
        listeners = new ArrayList<>();
    }

    public void addListener(FilesystemEventListener listener) {
        listeners.add(listener);
    }

    public boolean removeListener(FilesystemEventListener listener) {
        if (listener != null)
            return listeners.remove(listener);
        else
            return false;
    }

    @Override
    public void onFolderEvent(FilesystemEvent event, Path folderPath) {
        if (trackedFolders.contains(folderPath)) {
            if (event == DELETED) {
                unregisterFolder(folderPath);
            }
            listeners.forEach(it -> it.onFolderChanged(event, folderPath));
        } else if (event == DELETED) {
            try {
                for (Path file : trackedFiles) {
                    Path parentPath = file.getParent();
                    if (Files.isSameFile(folderPath, parentPath)) {
                        onFileEvent(DELETED, file);
                    }
                }
            } catch (IOException ex) {
                LOG.warn("Unhandled DELETE event for folder: {}", folderPath.toAbsolutePath(), ex);
            }
        }
    }

    @Override
    public void onFileEvent(FilesystemEvent event, Path filePath) {
        final Path folderPath = filePath.getParent();
        final boolean isTrackedFile = trackedFiles.contains(filePath);
        final boolean isTrackedFolder = trackedFolders.contains(folderPath);
        if (isTrackedFile || isTrackedFolder) {
            //Find folder which we should not track any more and remove it
            if (isTrackedFile && event == DELETED) {
                unregisterFile(filePath);
                if (!isTrackedFolder) {
                    unregisterFolder(folderPath);
                }
            }
            listeners.forEach(it -> it.onFileChanged(event, filePath));
        }
    }

    public void registerFile(Path filePath) {
        if (filePath != null) {
            scheduleNotificationIfNeeded();
            trackedFiles.add(filePath);
            LOG.debug("File {} registered", filePath.toAbsolutePath());
            Path folderPath = filePath.getParent();
            registerFolder(folderPath, false);
        }
    }

    public void registerFolder(Path folderPath) {
        if (folderPath != null) {
            registerFolder(folderPath, true);
        }
    }

    private void registerFolder(Path folderPath, boolean shouldTrack) {
        try {
            if (!registeredFolders.containsValue(folderPath)) {
                scheduleNotificationIfNeeded();
                WatchKey key = folderPath.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                registeredFolders.put(key, folderPath);
                LOG.debug("Folder {} registered", folderPath.toAbsolutePath());
            } else {
                LOG.debug("Folder already registered");
            }
            if (!trackedFolders.contains(folderPath) && shouldTrack)
                trackedFolders.add(folderPath);
        } catch (IOException ex) {
            LOG.warn("Exception during folder registration in notification manager", ex);
        }
    }

    private void unregisterFile(Path filePath) {
        trackedFiles.remove(filePath);
    }

    private void unregisterFolder(Path folderPath) {
        WatchKey key = null;
        for (Map.Entry<WatchKey, Path> entry : registeredFolders.entrySet()) {
            if (entry.getValue().equals(folderPath)) {
                key = entry.getKey();
            }
        }
        if (key != null) {
            registeredFolders.remove(key);
            trackedFolders.remove(folderPath);
            key.cancel();
            LOG.debug("Unregistered folder: " + folderPath);
        }
    }

    private void scheduleNotificationIfNeeded() {
        if (notificationExecutor == null) {
            notificationExecutor = SearchEngineExecutors.getNotificationManagerExecutor();
            FilesystemNotificationScheduler notificationScheduler = new FilesystemNotificationScheduler(registeredFolders, watchService);
            notificationScheduler.addListener(this);
            notificationExecutor.scheduleWithFixedDelay(notificationScheduler, 0, FILES_TRACK_DELAY_IN_SEC, TimeUnit.SECONDS);
        }
    }

}
