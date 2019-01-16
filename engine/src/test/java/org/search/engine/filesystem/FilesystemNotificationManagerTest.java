package org.search.engine.filesystem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.search.engine.filesystem.FilesystemEvent.*;

public class FilesystemNotificationManagerTest implements FilesystemEventListener {

    private WatchService watchService;
    private FilesystemNotificationManager notificationManager;
    private Path lastChangedPath;
    private FilesystemEvent lastChangedEvent;
    private ChangedEntity lastChangedEntity;


    @Before
    public void setUp() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        notificationManager = new FilesystemNotificationManager(watchService, new HashSet<>(), new HashSet<>(), progress -> { });
        notificationManager.addListener(this);
    }

    @After
    public void tearDown() throws Exception {
        watchService.close();
    }

    @Test
    public void testTrackFileAndGetModifiedFileEvent() {
        Path filePath = Paths.get("./TestFileOne.txt");
        notificationManager.registerFile(filePath);
        notificationManager.onFileEvent(MODIFIED, filePath);
        assertLastHandledEvent(MODIFIED, filePath, ChangedEntity.FILE);
    }

    @Test
    public void testAddNewFileToRegisteredFolder() {
        //Register file in the manager, parent folder should be also registered
        Path fileOnePath = Paths.get("./TestFileOne.txt");
        notificationManager.registerFile(fileOnePath);

        //Fire event creating new file in the tracked dir, doesn't affect notification because we track only file
        Path fileTwoPath = Paths.get("./TestFileTwo.txt");
        notificationManager.onFileEvent(CREATED, fileTwoPath);

        assertNull(lastChangedEvent);
        assertNull(lastChangedPath);
        assertNull(lastChangedEntity);
    }

    @Test
    public void testRemoveTrackedFileAndStopWatchingFolder() {
        Path filePath = Paths.get("./TestFileOne.txt");
        notificationManager.registerFile(filePath);

        //Fire event delete file in the tracked dir, folder should be removed from watcher
        notificationManager.onFileEvent(DELETED, filePath);

        assertLastHandledEvent(DELETED, filePath, ChangedEntity.FILE);
        notificationManager.unregisterFile(filePath);

        notificationManager.onFileEvent(MODIFIED, filePath);
        //We didn't get events any more
        assertLastHandledEvent(DELETED, filePath, ChangedEntity.FILE);
    }

    @Test
    public void testRemoveRegisteredFolderByTrackedFile() {
        Path filePath = Paths.get("./TestFileOne.txt");
        notificationManager.registerFile(filePath);

        Path folderPath = Paths.get("./");
        notificationManager.onFolderEvent(DELETED, folderPath.toAbsolutePath());
        assertLastHandledEvent(DELETED, filePath, ChangedEntity.FILE);
    }

    @Test
    public void testTrackFolderAndGetModifiedFileEvent() {
        Path folderPath = Paths.get("./").toAbsolutePath();
        notificationManager.registerFolder(folderPath);
        notificationManager.onFolderEvent(MODIFIED, folderPath);
        assertLastHandledEvent(MODIFIED, folderPath, ChangedEntity.FOLDER);
    }

    @Test
    public void testAddFileInTrackedFolder() {
        Path folderPath = Paths.get("./").toAbsolutePath();
        notificationManager.registerFolder(folderPath);
        Path filePath = Paths.get("./TestFileOne.txt").toAbsolutePath();
        notificationManager.onFileEvent(CREATED, filePath);
        assertLastHandledEvent(CREATED, filePath, ChangedEntity.FILE);
    }

    @Test
    public void testRemoveTrackedFolder() {
        Path folderPath = Paths.get("./").toAbsolutePath();
        notificationManager.registerFolder(folderPath);
        notificationManager.onFolderEvent(DELETED, folderPath);
        assertLastHandledEvent(DELETED, folderPath, ChangedEntity.FOLDER);
    }

    @Override
    public void onFileChanged(FilesystemEvent event, Path filePath) {
        lastChangedEvent = event;
        lastChangedPath = filePath;
        lastChangedEntity = ChangedEntity.FILE;
    }

    @Override
    public void onFolderChanged(FilesystemEvent event, Path folderPath) {
        lastChangedEvent = event;
        lastChangedPath = folderPath;
        lastChangedEntity = ChangedEntity.FOLDER;
    }

    private void assertLastHandledEvent(FilesystemEvent event, Path path, ChangedEntity changedEntity) {
        assertEquals(event, lastChangedEvent);
        assertEquals(path, lastChangedPath);
        assertEquals(changedEntity, lastChangedEntity);
    }

    private enum ChangedEntity {
        FILE, FOLDER
    }
}