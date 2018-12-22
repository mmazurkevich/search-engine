package org.search.engine.model;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * Data class which contains brief information about documents which indexed
 * by the library. Internal identifier of the document in the lib, path to the
 * file itself and to it's parent. Boolean flag if user added this file manually
 * or it was during folder indexation.
 */
public class Document implements Serializable {

    private static final long serialVersionUID = 7240996246763128397L;

    private int id;
    private boolean tracked;
    private Path path;
    private Path parent;
    private long modificationTimestamp;

    public Document(int id, boolean tracked, Path path, long modificationTimestamp) {
        this.id = id;
        this.tracked = tracked;
        this.path = path;
        this.parent = path.getParent();
        this.modificationTimestamp = modificationTimestamp;
    }

    public int getId() {
        return id;
    }

    public boolean isTracked() {
        return tracked;
    }

    public Path getPath() {
        return path;
    }

    public Path getParent() {
        return parent;
    }

    public long getModificationTimestamp() {
        return modificationTimestamp;
    }
}
