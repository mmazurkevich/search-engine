package org.search.app.worker;

import io.reactivex.subjects.ReplaySubject;
import org.search.app.component.JSearchResultTable;
import org.search.app.model.RowFile;
import org.search.app.model.SearchResultTableModel;
import org.search.engine.SearchEngine;
import org.search.engine.model.SearchResultEvent;
import org.search.engine.model.SearchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class SearchWorker extends SwingWorker<ReplaySubject<SearchResultEvent>, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(SearchWorker.class);

    private final SearchEngine searchEngine;
    private final String searchQuery;
    private final JSearchResultTable searchResultTable;
    private final ButtonGroup searchOptionsGroup;

    public SearchWorker(SearchEngine searchEngine, String searchQuery, JSearchResultTable searchResultTable, ButtonGroup searchOptionsGroup) {
        this.searchEngine = searchEngine;
        this.searchQuery = searchQuery;
        this.searchResultTable = searchResultTable;
        this.searchOptionsGroup = searchOptionsGroup;
    }

    @Override
    protected ReplaySubject<SearchResultEvent> doInBackground() {
        SearchType searchType = SearchType.valueOf(searchOptionsGroup.getSelection().getActionCommand());
        return searchEngine.search(searchQuery, searchType);
    }

    @Override
    protected void done() {
        try {
            searchResultTable.getSelectionModel().clearSelection();
            SearchResultTableModel model = searchResultTable.getModel();
            model.setData(new ArrayList<>());
            model.setSearchQuery(searchQuery);
            model.fireTableDataChanged();
            ReplaySubject<SearchResultEvent> subject = get();
            if (subject != null) {
                subject.subscribe(it -> {
                    List<RowFile> data = model.getData();
                    if (it != null) {
                        switch (it.getEventType()) {
                            case ADD:
                                data.add(new RowFile(it.getFileName(), it.getRowNumber(), it.getPositions()));
                                model.fireTableRowsInserted(data.size() - 1, data.size() - 1);
                                break;
                            case UPDATE:
                                int selectedRow = searchResultTable.getSelectedRow();
                                for (int i = 0; i < data.size(); i++) {
                                    RowFile rowFile = data.get(i);
                                    if (rowFile.getFilePath().equals(it.getFileName()) && rowFile.getRowNumber() == it.getRowNumber()) {
                                        rowFile.setPositions(it.getPositions());
                                        if (selectedRow == i) {
                                            searchResultTable.clearSelection();
                                        }
                                    }
                                }
                                break;
                            case REMOVE:
                                int rowIndex = 0;
                                for (int i = 0; i < data.size(); i++) {
                                    RowFile rowFile = data.get(i);
                                    if (rowFile.getFilePath().equals(it.getFileName()) && rowFile.getRowNumber() == it.getRowNumber()) {
                                        rowIndex = i;
                                    }
                                }
                                model.fireTableRowsDeleted(rowIndex, rowIndex);
                                data.remove(rowIndex);
                                break;
                        }
                    }
                }, it -> LOG.warn("Exception in handling event for search result"));
            }
        } catch (InterruptedException | ExecutionException | CancellationException ex) {
            LOG.warn("Getting search result task was canceled");
        }
    }
}
