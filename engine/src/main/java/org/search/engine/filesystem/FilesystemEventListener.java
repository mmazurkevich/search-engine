package org.search.engine.filesystem;

import java.nio.file.Path;

/**
 * API for getting events from the filesystem notification manager about
 * changes in the registered entities. Cass should implement this interface
 * and subscribe on events in the notification manger.
 */
public interface FilesystemEventListener {

    /**
     * Method for handling file changes events coming from the filesystem
     *
     * @param event Event of that type of action happened with file
     * @param filePath Path to the changed file
     */
    void onFileChanged(FilesystemEvent event, Path filePath);

    /**
     * Method for handling folder changes events
     *
     * @param event Event of that type of action happened with folder
     * @param folderPath Path to the changed folder
     */
    void onFolderChanged(FilesystemEvent event, Path folderPath);
}
