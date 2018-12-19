package org.search.app.listener;

import org.search.app.component.JSearchResultTable;
import org.search.app.model.SearchResultTableModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FileSelectionListener implements ListSelectionListener {

    private final SearchResultTableModel tableModel;
    private final JSearchResultTable searchResultTable;
    private final JTextArea documentPreview;

    public FileSelectionListener(SearchResultTableModel tableModel, JSearchResultTable searchResultTable, JTextArea documentPreview) {
        this.tableModel = tableModel;
        this.searchResultTable = searchResultTable;
        this.documentPreview = documentPreview;
    }


    @Override
    public void valueChanged(ListSelectionEvent e) {
        String filePath = tableModel.getValueAt(searchResultTable.getSelectedRow(), 0);
        FileReader reader = null;
        try {
            reader = new FileReader(filePath);
            documentPreview.read(reader, filePath);
            int startIndex = documentPreview.getLineStartOffset(20);
            DefaultHighlighter.DefaultHighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
            documentPreview.getHighlighter().addHighlight(startIndex, startIndex + 10, painter);
        } catch (IOException | BadLocationException e1) {
            e1.printStackTrace();
        }
    }
}


