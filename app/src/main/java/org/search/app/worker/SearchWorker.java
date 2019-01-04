package org.search.app.worker;

import org.search.app.component.JSearchResultTable;
import org.search.app.model.RowFile;
import org.search.engine.SearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class SearchWorker extends SwingWorker<List<RowFile>, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(SearchWorker.class);

    private final SearchEngine searchEngine;
    private final String searchQuery;
    private final JSearchResultTable searchResultTable;

    public SearchWorker(SearchEngine searchEngine, String searchQuery, JSearchResultTable searchResultTable) {
        this.searchEngine = searchEngine;
        this.searchQuery = searchQuery;
        this.searchResultTable = searchResultTable;
    }

    @Override
    protected List<RowFile> doInBackground() {
        return searchEngine.search(searchQuery).stream().map(it -> {
            List<RowFile> fileRows = new ArrayList<>();
            for (Map.Entry<Integer, List<Integer>> row: it.getRowNumbers().entrySet()) {
                fileRows.add(new RowFile(it.getFileName(), row.getKey(), row.getValue()));
            }
            return fileRows;
        }).flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    protected void done() {
        try {
            searchResultTable.getSelectionModel().clearSelection();
            searchResultTable.getModel().setData(get());
            searchResultTable.getModel().fireTableDataChanged();
        } catch (InterruptedException | ExecutionException ex) {
            LOG.warn("Execution exception during getting the result of search", ex);
        }
    }
}
