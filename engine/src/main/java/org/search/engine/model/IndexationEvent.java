package org.search.engine.model;

public class IndexationEvent {

    private final EventType type;
    private final int documentId;
    private final String content;

    public IndexationEvent(EventType type, int documentId, String content) {
        this.type = type;
        this.documentId = documentId;
        this.content = content;
    }

    public EventType getType() {
        return type;
    }

    public int getDocumentId() {
        return documentId;
    }

    public String getContent() {
        return content;
    }
}
