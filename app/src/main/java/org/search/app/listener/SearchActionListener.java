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

    public SearchActionListener(SearchEngine searchEngine, JTextField searchField, JSearchResultTable searchResultTable) {
        this.searchEngine = searchEngine;
        this.searchField = searchField;
        this.searchResultTable = searchResultTable;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String searchQuery = searchField.getText();
        SearchWorker worker = new SearchWorker(searchEngine, searchQuery, searchResultTable);
        worker.execute();
    }
}
