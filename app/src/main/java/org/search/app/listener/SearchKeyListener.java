package org.search.app.listener;

import org.search.app.HunspellCheck;
import org.search.app.component.JSearchResultTable;
import org.search.app.component.SpellCheckPainter;
import org.search.app.worker.SearchWorker;
import org.search.engine.SearchEngine;
import org.search.engine.model.SearchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.awt.event.KeyEvent.*;

public class SearchKeyListener implements KeyListener {

    private static final Logger LOG = LoggerFactory.getLogger(SearchKeyListener.class);

    private static final List<Integer> invalidKeys = Arrays.asList(VK_ALT, VK_CONTROL, VK_CAPS_LOCK, VK_PAGE_UP, VK_PAGE_DOWN, VK_ESCAPE, VK_HOME, VK_END,
            VK_LEFT, VK_RIGHT, VK_UP, VK_DOWN, VK_SHIFT, VK_F1, VK_F2, VK_F3, VK_F4, VK_F5, VK_F6, VK_F7, VK_F8, VK_F9, VK_F10, VK_F11, VK_F12);
    private final SearchEngine searchEngine;
    private final JTextField searchField;
    private final JSearchResultTable searchResultTable;
    private final ButtonGroup searchOptionsGroup;
    private final SpellCheckPainter painter;
    private final HunspellCheck hunspellCheck;
    private final JLabel possibleSuggestions;
    private SearchWorker searchWorker;

    public SearchKeyListener(SearchEngine searchEngine, JTextField searchField, JSearchResultTable searchResultTable,
                             ButtonGroup searchOptionsGroup, HunspellCheck hunspellCheck, JLabel possibleSuggestions) {
        this.searchEngine = searchEngine;
        this.searchField = searchField;
        this.searchResultTable = searchResultTable;
        this.searchOptionsGroup = searchOptionsGroup;
        this.painter = new SpellCheckPainter(Color.RED);
        this.hunspellCheck = hunspellCheck;
        this.possibleSuggestions = possibleSuggestions;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!invalidKeys.contains(e.getKeyCode())) {
            if (searchWorker != null && !searchWorker.isDone()) {
                searchWorker.cancelSearch();
            }
            handleEvent();
        }
    }

    private void handleEvent() {
        String searchQuery = searchField.getText();
        possibleSuggestions.setText("");

        List<String> searchQueries = new ArrayList<>();
        searchQueries.add(searchQuery);

        SearchType searchType = SearchType.valueOf(searchOptionsGroup.getSelection().getActionCommand());
        List<String> suggestions = spellCheck(searchQuery);

        if (searchType == SearchType.WITH_SUGGESTIONS) {
            suggestions.forEach(it -> {
                if (it.length() == searchQuery.length()) {
                    searchQueries.add(it);
                }
            });
            possibleSuggestions.setText("Possible suggestions: " + String.join(",", searchQueries));
        }

        searchWorker = new SearchWorker(searchEngine, searchQueries, searchResultTable, searchType);
        searchWorker.execute();
    }

    private List<String> spellCheck(String searchQuery) {
        searchField.getHighlighter().removeAllHighlights();
        searchField.setComponentPopupMenu(null);
        if (hunspellCheck != null) {
            List<String> suggestions = hunspellCheck.getSuggestions(searchQuery);
            if (suggestions != null && !suggestions.isEmpty()) {
                try {
                    searchField.getHighlighter().addHighlight(0, searchQuery.length(), painter);
                } catch (BadLocationException e) {
                    LOG.warn("Can't highlight spell checked word");
                }
                JPopupMenu popup = new JPopupMenu();
                suggestions.forEach(it -> {
                    JMenuItem menuItem = new JMenuItem(it);
                    menuItem.addActionListener(actionEvent -> {
                        searchField.setText(it);
                        handleEvent();
                    });
                    popup.add(menuItem);
                });
                searchField.setComponentPopupMenu(popup);
                return suggestions;
            }
        }
        return Collections.emptyList();
    }

}