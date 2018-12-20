package org.search.engine.search;

import java.util.List;

public class SearchResult {

    private String fileName;
    private List<Integer> rowNumbers;

    public SearchResult(String fileName, List<Integer> rowNumbers) {
        this.fileName = fileName;
        this.rowNumbers = rowNumbers;
    }

    public String getFileName() {
        return fileName;
    }

    public List<Integer> getRowNumbers() {
        return rowNumbers;
    }
}
