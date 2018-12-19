package org.search.app.model;

public class RowFile {

    private String filePath;
    private int rowNumber;

    public RowFile(String filePath, int rowNumber) {
        this.filePath = filePath;
        this.rowNumber = rowNumber;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getRowNumber() {
        return rowNumber;
    }
}
