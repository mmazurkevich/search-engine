package org.search.app.listener;

import org.search.app.component.JSearchResultTable;
import org.search.app.worker.SearchWorker;
import org.search.engine.SearchEngine;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.List;

import static java.awt.event.KeyEvent.*;

public class SearchKeyListener implements KeyListener {

    private static final List<Integer> invalidKeys = Arrays.asList(VK_ALT, VK_CONTROL, VK_CAPS_LOCK, VK_PAGE_UP, VK_PAGE_DOWN, VK_ESCAPE, VK_HOME, VK_END,
            VK_LEFT, VK_RIGHT, VK_UP, VK_DOWN, VK_SHIFT, VK_F1, VK_F2, VK_F3, VK_F4, VK_F5, VK_F6, VK_F7, VK_F8, VK_F9, VK_F10, VK_F11, VK_F12);
    private final SearchEngine searchEngine;
    private final JTextField searchField;
    private final JSearchResultTable searchResultTable;

    public SearchKeyListener(SearchEngine searchEngine, JTextField searchField, JSearchResultTable searchResultTable) {
        this.searchEngine = searchEngine;
        this.searchField = searchField;
        this.searchResultTable = searchResultTable;
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!invalidKeys.contains(e.getKeyCode())) {
            String searchQuery = searchField.getText();
            SearchWorker worker = new SearchWorker(searchEngine, searchQuery, searchResultTable);
            worker.execute();
        }

    }

}