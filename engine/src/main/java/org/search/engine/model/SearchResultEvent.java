package org.search.engine.model;

import java.util.List;

public class SearchResultEvent {

    private String fileName;
    private int rowNumber;
    private List<Integer> positions;
    private EventType eventType;

    public SearchResultEvent(String fileName, int rowNumber, List<Integer> positions, EventType eventType) {
        this.fileName = fileName;
        this.rowNumber = rowNumber;
        this.positions = positions;
        this.eventType = eventType;
    }

    public String getFileName() {
        return fileName;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public List<Integer> getPositions() {
        return positions;
    }

    public EventType getEventType() {
        return eventType;
    }
}
