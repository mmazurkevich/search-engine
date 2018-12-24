package org.search.app.listener;

import org.search.app.component.JSearchResultTable;
import org.search.app.model.SearchResultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;

public class FileSelectionListener implements ListSelectionListener {

    private static final Logger LOG = LoggerFactory.getLogger(FileSelectionListener.class);

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
            String filePath = getSelectedFile();
            int rowNumber = getMatchedRow();
            if (previousFile == null || !previousFile.equals(filePath) || documentPreview.getText().isEmpty()) {
                try (FileReader reader = new FileReader(filePath)) {
                    documentPreview.read(reader, filePath);
                } catch (IOException ex) {
                    LOG.warn("Exception during loading file");
                    documentPreview.setText("");
                }
                previousFile = filePath;
            }
            try {
                highlightAndScrollRow(rowNumber);
            } catch (BadLocationException ex) {
                LOG.warn("Exception during highlighting file");
                documentPreview.setText("");
            }
        } else {
            documentPreview.setText("");
        }
    }

    private String getSelectedFile() {
        return tableModel.getValueAt(searchResultTable.getSelectedRow(), 0);
    }

    private int getMatchedRow() {
        return Integer.parseInt(tableModel.getValueAt(searchResultTable.getSelectedRow(), 1));
    }

    private void highlightAndScrollRow(int rowNumber) throws BadLocationException {
        if (previousHighlight != null) {
            documentPreview.getHighlighter().removeHighlight(previousHighlight);
        }
        int startIndex = documentPreview.getLineStartOffset(rowNumber - 1);
        int endIndex = documentPreview.getLineEndOffset(rowNumber - 1);
        previousHighlight = documentPreview.getHighlighter().addHighlight(startIndex, endIndex, painter);
        documentPreview.setCaretPosition(startIndex);
    }
}


