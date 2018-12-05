package org.search.engine.filesystem;

import java.nio.file.Path;

public interface FilesystemEventListener {

    void onFileChanged(FilesystemEvent event, Path filePath);

    void onFolderChanged(FilesystemEvent event, Path folderPath);
}
