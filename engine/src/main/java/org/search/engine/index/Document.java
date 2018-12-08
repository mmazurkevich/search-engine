package org.search.engine.index;

import java.nio.file.Path;

class Document {

    private int id;
    private boolean tracked;
    private Path path;
    private Path parent;

    Document(int id, boolean tracked, Path path) {
        this.id = id;
        this.tracked = tracked;
        this.path = path;
        this.parent = path.getParent();
    }

    int getId() {
        return id;
    }

    public boolean isTracked() {
        return tracked;
    }

    Path getPath() {
        return path;
    }

    Path getParent() {
        return parent;
    }
}
