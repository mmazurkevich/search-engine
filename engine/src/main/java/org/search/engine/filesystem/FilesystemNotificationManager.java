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
import static org.search.engine.filesystem.FilesystemEvent.CREATED;
import static org.search.engine.filesystem.FilesystemEvent.DELETED;

public class FilesystemNotificationManager implements FilesystemNotificationScheduler.WatchServiceEventListener, FilesystemNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemNotificationManager.class);

    private static final int FILES_TRACK_DELAY_IN_SEC = 2;
    // Registered folder by notification folder can be registered but not be actually tracked by user,
    // it's because of tracking files we should register it's folder for track changes with file itself
    private final Map<WatchKey, Path> registeredFolders = new ConcurrentHashMap<>();
    // Files which changes tracked by system and were registered in the system by track. CopyOnWriteArrayList
    // used because of possibility of concurrent changes came from watch service and by user itself
    private final List<Path> trackedFiles = new CopyOnWriteArrayList<>();
    // Folders which changes tracked by system and were registered in the system by track.
    private final List<Path> trackedFolders = new CopyOnWriteArrayList<>();
    private final List<FilesystemEventListener> listeners = new ArrayList<>();
    private final WatchService watchService;
    private ScheduledExecutorService notificationExecutor;

    public FilesystemNotificationManager(WatchService watchService) {
        this.watchService = watchService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean registerFile(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path must not be null");
        }
        if (!trackedFiles.contains(filePath)) {
            scheduleNotificationIfNeeded();
            trackedFiles.add(filePath);
            LOG.debug("File {} registered", filePath.toAbsolutePath());
            Path folderPath = filePath.getParent();

            //Hack for tracking folder delete itself
            if (folderPath.getParent() != null) {
                registerFolder(folderPath.getParent(), false);
            }
            return registerFolder(folderPath, false);
        } else {
            LOG.debug("File already registered");
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean registerFolder(Path folderPath) {
        if (folderPath == null) {
            throw new IllegalArgumentException("Folder path must not be null");
        }
        //Hack for tracking folder delete itself
        if (folderPath.getParent() != null) {
            registerFolder(folderPath.getParent(), false);
        }
        return registerFolder(folderPath, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unregisterFile(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path must not be null");
        }
        return trackedFiles.remove(filePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unregisterFolder(Path folderPath) {
        // For the case then folder which is the child of removing contains no files
        // but also should be removed from trackedFolders list
        List<Path> foldersToRemove = new ArrayList<>();
        trackedFolders.forEach(it -> {
           if (it.startsWith(folderPath)){
               foldersToRemove.add(it);
           }
        });
        if (foldersToRemove.size() > 1) {
            LOG.debug("Child folder with no files also will be unregistered");
        }
        boolean wasRemoved = false;
        for (Path folder: foldersToRemove) {
            if (registeredFolders.containsValue(folder)) {
                WatchKey key = null;
                for (Map.Entry<WatchKey, Path> entry : registeredFolders.entrySet()) {
                    if (entry.getValue().equals(folder)) {
                        key = entry.getKey();
                    }
                }
                if (key != null) {
                    registeredFolders.remove(key);
                    trackedFolders.remove(folder);
                    key.cancel();
                    wasRemoved = true;
                    LOG.debug("Unregistered folder: " + folder);
                }
            }
        }
        return wasRemoved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFolderEvent(FilesystemEvent event, Path folderPath) {
        //Created folder in tracked folder, it should be tracked
        if (trackedFolders.contains(folderPath.getParent()) && event == CREATED) {
            listeners.forEach(it -> it.onFolderChanged(event, folderPath));
        } else if (trackedFolders.contains(folderPath)) {
            //Delete of tracking folder
            if (event == DELETED) {
                unregisterFolder(folderPath);
            }
            listeners.forEach(it -> it.onFolderChanged(event, folderPath));
        } else if (event == DELETED) {
            try {
                //Delete of not tracking folder but containing tracked file
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileEvent(FilesystemEvent event, Path filePath) {
        final Path folderPath = filePath.getParent();
        final boolean isTrackedFile = trackedFiles.contains(filePath);
        final boolean isTrackedFolder = trackedFolders.contains(folderPath);
        if (isTrackedFile || isTrackedFolder) {
            //Find folder which we should not track any more and remove it
            if (isTrackedFile && event == DELETED) {
                if (!isTrackedFolder) {
                    unregisterFolder(folderPath);
                }
            }
            listeners.forEach(it -> it.onFileChanged(event, filePath));
        }
    }

    public void addListener(FilesystemEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public boolean removeListener(FilesystemEventListener listener) {
        if (listener != null)
            return listeners.remove(listener);
        else
            return false;
    }

    private boolean registerFolder(Path folderPath, boolean shouldTrack) {
        try {
            boolean isRegistered = false;
            if (!registeredFolders.containsValue(folderPath)) {
                scheduleNotificationIfNeeded();
                WatchKey key = folderPath.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                registeredFolders.put(key, folderPath);
                LOG.debug("Folder {} registered", folderPath.toAbsolutePath());
                isRegistered = true;
            } else {
                LOG.debug("Folder already registered {}", folderPath);
            }
            if (!trackedFolders.contains(folderPath) && shouldTrack)
                trackedFolders.add(folderPath);
            return isRegistered;
        } catch (IOException ex) {
            LOG.warn("Exception during folder registration in notification manager", ex);
            return false;
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
