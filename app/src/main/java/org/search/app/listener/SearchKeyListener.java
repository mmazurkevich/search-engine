package org.search.app.listener;

import org.search.app.component.JSearchResultTable;
import org.search.app.worker.SearchWorker;
import org.search.engine.SearchEngine;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SearchKeyListener implements KeyListener {

    private final SearchEngine searchEngine;
    private final JTextField searchField;
    private final JSearchResultTable searchResultTable;

    public SearchKeyListener(SearchEngine searchEngine, JTextField searchField, JSearchResultTable searchResultTable) {
        this.searchEngine = searchEngine;
        this.searchField = searchField;
        this.searchResultTable = searchResultTable;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        SearchWorker worker = new SearchWorker(searchEngine, searchField, searchResultTable);
        worker.execute();
    }

    @Override
    public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) { }

}
