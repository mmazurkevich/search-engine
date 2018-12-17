package org.search.app.listener;

import org.search.app.worker.SearchWorker;
import org.search.engine.SearchEngine;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SearchKeyListener implements KeyListener {

    private final SearchEngine searchEngine;
    private final JTextField searchField;
    private final JTextArea searchResultArea;

    public SearchKeyListener(SearchEngine searchEngine, JTextField searchField, JTextArea searchResultArea) {
        this.searchEngine = searchEngine;
        this.searchField = searchField;
        this.searchResultArea = searchResultArea;
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) {
        SearchWorker worker = new SearchWorker(searchEngine, searchField, searchResultArea);
        worker.execute();
    }
}
