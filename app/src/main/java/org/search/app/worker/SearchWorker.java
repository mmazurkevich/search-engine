package org.search.app.worker;

import org.search.engine.SearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class SearchWorker extends SwingWorker<String, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(SearchWorker.class);

    private final SearchEngine searchEngine;
    private final JTextField searchField;
    private final JTextArea searchResultArea;

    public SearchWorker(SearchEngine searchEngine, JTextField searchField, JTextArea searchResultArea) {
        this.searchEngine = searchEngine;
        this.searchField = searchField;
        this.searchResultArea = searchResultArea;
    }

    @Override
    protected String doInBackground() {
        return searchEngine.search(searchField.getText()).stream()
                .collect(Collectors.joining("\n"));
    }

    @Override
    protected void done() {
        try {
            searchResultArea.setText(get());
        } catch (InterruptedException | ExecutionException ex) {
            LOG.warn("Execution exception during getting the result of search", ex);
        }
    }
}
