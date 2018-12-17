package org.search.app.listener;

import org.search.app.worker.SearchWorker;
import org.search.engine.SearchEngine;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SearchActionListener implements ActionListener {

    private final SearchEngine searchEngine;
    private final JTextField searchField;
    private final JTextArea searchResultArea;

    public SearchActionListener(SearchEngine searchEngine, JTextField searchField, JTextArea searchResultArea) {
        this.searchEngine = searchEngine;
        this.searchField = searchField;
        this.searchResultArea = searchResultArea;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SearchWorker worker = new SearchWorker(searchEngine, searchField, searchResultArea);
        worker.execute();
    }
}
