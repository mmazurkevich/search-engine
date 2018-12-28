package org.search.app.model;

import java.util.List;

public class RowFile {

    private String filePath;
    private int rowNumber;
    private List<Integer> positions;

    public RowFile(String filePath, int rowNumber, List<Integer> positions) {
        this.filePath = filePath;
        this.rowNumber = rowNumber;
        this.positions = positions;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public List<Integer> getPositions() {
        return positions;
    }
}
