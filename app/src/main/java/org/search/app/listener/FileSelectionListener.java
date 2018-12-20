package org.search.app.listener;

import org.search.app.component.JSearchResultTable;
import org.search.app.model.SearchResultTableModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;

public class FileSelectionListener implements ListSelectionListener {

    private final SearchResultTableModel tableModel;
    private final JSearchResultTable searchResultTable;
    private final JTextArea documentPreview;
    private final DefaultHighlighter.DefaultHighlightPainter painter;
    private Object previousHighlight;
    private String previousFile;

    public FileSelectionListener(SearchResultTableModel tableModel, JSearchResultTable searchResultTable, JTextArea documentPreview) {
        this.tableModel = tableModel;
        this.searchResultTable = searchResultTable;
        this.documentPreview = documentPreview;
        this.painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    }


    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (searchResultTable.getSelectedRow() >= 0) {
            String filePath = tableModel.getValueAt(searchResultTable.getSelectedRow(), 0);
            int rowNumber = Integer.parseInt(tableModel.getValueAt(searchResultTable.getSelectedRow(), 1));
            if (previousFile == null || !previousFile.equals(filePath)) {
                try {
                    FileReader reader = new FileReader(filePath);
                    documentPreview.read(reader, filePath);
                    previousFile = filePath;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            try {
                if (previousHighlight != null) {
                    documentPreview.getHighlighter().removeHighlight(previousHighlight);
                }
                int startIndex = documentPreview.getLineStartOffset(rowNumber - 1);
                int endIndex = documentPreview.getLineEndOffset(rowNumber - 1);
                previousHighlight = documentPreview.getHighlighter().addHighlight(startIndex, endIndex, painter);
                Rectangle viewRect = documentPreview.modelToView(startIndex);
                documentPreview.scrollRectToVisible(viewRect);
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        } else {
            documentPreview.setText("");
        }
    }
}


