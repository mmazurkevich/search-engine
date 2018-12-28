package org.search.engine.model;

import java.util.List;
import java.util.Map;

public class SearchResult {

    private String fileName;
    private Map<Integer, List<Integer>> rowNumbers;

    public SearchResult(String fileName, Map<Integer, List<Integer>> rowNumbers) {
        this.fileName = fileName;
        this.rowNumbers = rowNumbers;
    }

    public String getFileName() {
        return fileName;
    }

    public Map<Integer, List<Integer>> getRowNumbers() {
        return rowNumbers;
    }
}
