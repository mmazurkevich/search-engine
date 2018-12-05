package org.search.engine.index;

import java.nio.file.Path;

class Document {

    private int id;
    private Path path;
    private Path parent;

    Document(int id, Path path) {
        this.id = id;
        this.path = path;
        this.parent = path.getParent();
    }

    int getId() {
        return id;
    }

    Path getPath() {
        return path;
    }

    Path getParent() {
        return parent;
    }
}
