package org.search.engine.filesystem;

import java.nio.file.Path;

/**
 * API interface which allow to register file or folder on track changes
 * and be notified about entity changes. This API helps to quick switch
 * implementations. Currently system use {@link FilesystemNotificationManager}
 * implementation.
 */
public interface FilesystemNotifier {

    /**
     * Method check does folder registered in the notifier
     *
     * @param folderPath The path to the registered folder
     * @return identify if the folder is registered in the notifier
     */
    boolean isFolderRegistered(Path folderPath);

    /**
     * Method check does file registered in the notifier
     *
     * @param filePath The path to the registered file
     * @return identify if the file is registered in the notifier
     */
    boolean isFileRegistered(Path filePath);

    /**
     * Method register certain file to be tracked in the system
     *
     * @param filePath The path to the file which want to be tracked
     * @return identify if the file was registered in the notifier
     */
    boolean registerFile(Path filePath);


    /**
     * Method register certain folder to be tracked in the system
     *
     * @param folderPath The path to the registered folder
     * @return identify if the folder was registered in the notifier
     */
    boolean registerFolder(Path folderPath);

    /**
     * Method register indexing folder parent for tracking itself folder delete
     *
     * @param folderPath The path to the registered folder
     * @return identify if the folder was registered in the notifier
     */
    boolean registerParentFolder(Path folderPath);

    /**
     * Method unregister file from the been tracked by notification service
     *
     * @param filePath The path to the file which should be unregistered
     * @return identify if the file was successfully unregistered
     */
    boolean unregisterFile(Path filePath);


    /**
     * Method unregister folder from the been tracked by notification service
     *
     * @param folderPath The path to the folder which should be unregistered
     * @return identify if the folder was successfully unregistered
     */
    boolean unregisterFolder(Path folderPath);


    /**
     * Register listener to be notified of changes within registered folders or files
     *
     * @param listener The listener which should be notified about changes. It should
     *                 implement interface {@link FilesystemEventListener}
     */
    void addListener(FilesystemEventListener listener);


    /**
     * Unregister listener from been notified
     *
     * @param listener The listener which should be removed from notifiers.
     * @return identifier about successful removals
     */
    boolean removeListener(FilesystemEventListener listener);
}
