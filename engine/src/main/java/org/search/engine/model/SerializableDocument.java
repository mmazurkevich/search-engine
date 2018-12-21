package org.search.engine.model;

import java.io.Serializable;

public class SerializableDocument implements Serializable {

    private static final long serialVersionUID = 7240996246763128397L;

    private int id;
    private boolean tracked;
    private String path;

    public SerializableDocument(int id, boolean tracked, String path) {
        this.id = id;
        this.tracked = tracked;
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public boolean isTracked() {
        return tracked;
    }

    public String getPath() {
        return path;
    }

}
