package org.search.app.listener;

import org.search.app.component.JSearchResultTable;
import org.search.app.worker.SearchWorker;
import org.search.engine.SearchEngine;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SearchActionListener implements ActionListener {

    private final SearchEngine searchEngine;
    private final JTextField searchField;
    private final JSearchResultTable searchResultTable;
    private final ButtonGroup searchOptionsGroup;

    public SearchActionListener(SearchEngine searchEngine, JTextField searchField, JSearchResultTable searchResultTable,
                                ButtonGroup searchOptionsGroup) {
        this.searchEngine = searchEngine;
        this.searchField = searchField;
        this.searchResultTable = searchResultTable;
        this.searchOptionsGroup = searchOptionsGroup;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String searchQuery = searchField.getText();
        SearchWorker worker = new SearchWorker(searchEngine, searchQuery, searchResultTable, searchOptionsGroup);
        worker.execute();
    }
}
