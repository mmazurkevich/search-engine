package org.search.engine.index;

import java.nio.file.Path;

public class Document {

    private int id;
    private boolean tracked;
    private Path path;
    private Path parent;

    public Document(int id, boolean tracked, Path path) {
        this.id = id;
        this.tracked = tracked;
        this.path = path;
        this.parent = path.getParent();
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
}
