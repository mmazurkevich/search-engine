package org.search.app.listener;

import org.search.app.HunspellCheck;
import org.search.app.component.JSearchResultTable;
import org.search.app.worker.SearchWorker;
import org.search.engine.SearchEngine;
import org.search.engine.model.SearchType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchActionListener implements ActionListener {

    private final SearchEngine searchEngine;
    private final JTextField searchField;
    private final JSearchResultTable searchResultTable;
    private final ButtonGroup searchOptionsGroup;
    private final HunspellCheck hunspellCheck;
    private final JLabel possibleSuggestions;

    public SearchActionListener(SearchEngine searchEngine, JTextField searchField, JSearchResultTable searchResultTable,
                                ButtonGroup searchOptionsGroup, HunspellCheck hunspellCheck, JLabel possibleSuggestions) {
        this.searchEngine = searchEngine;
        this.searchField = searchField;
        this.searchResultTable = searchResultTable;
        this.searchOptionsGroup = searchOptionsGroup;
        this.hunspellCheck = hunspellCheck;
        this.possibleSuggestions = possibleSuggestions;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String searchQuery = searchField.getText();
        possibleSuggestions.setText("");

        List<String> searchQueries = new ArrayList<>();
        searchQueries.add(searchQuery);

        SearchType searchType = SearchType.valueOf(searchOptionsGroup.getSelection().getActionCommand());
        if (searchType == SearchType.WITH_SUGGESTIONS && hunspellCheck != null) {
            searchQueries.addAll(hunspellCheck.getSuggestions(searchQuery).stream()
                    .filter(it -> it.length() == searchQuery.length())
                    .collect(Collectors.toList()));
            possibleSuggestions.setText("Possible suggestions: " + String.join(",", searchQueries));
        }

        SearchWorker worker = new SearchWorker(searchEngine, searchQueries, searchResultTable, searchType);
        worker.execute();
    }
}
