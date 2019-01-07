package org.search.app.listener;

import org.apache.commons.io.FilenameUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.search.app.component.JSearchResultTable;
import org.search.app.model.RowFile;
import org.search.app.model.SearchResultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileSelectionListener implements ListSelectionListener {

    private static final Logger LOG = LoggerFactory.getLogger(FileSelectionListener.class);

    private final SearchResultTableModel tableModel;
    private final JSearchResultTable searchResultTable;
    private final RSyntaxTextArea documentPreview;
    private final DefaultHighlighter.DefaultHighlightPainter painter;
    private List<Object> previousHighlights = new ArrayList<>();
    private String previousFile;

    public FileSelectionListener(SearchResultTableModel tableModel, JSearchResultTable searchResultTable, RSyntaxTextArea documentPreview) {
        this.tableModel = tableModel;
        this.searchResultTable = searchResultTable;
        this.documentPreview = documentPreview;
        this.painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (searchResultTable.getSelectedRow() >= 0) {
            RowFile rowFile = getSelectedRowFile();
            String filePath = rowFile.getFilePath();
            if (previousFile == null || !previousFile.equals(filePath) || documentPreview.getText().isEmpty()) {
                try (FileReader reader = new FileReader(filePath)) {
                    documentPreview.read(reader, filePath);
                    setPreviewSyntax(FilenameUtils.getExtension(filePath.toLowerCase()));
                } catch (IOException ex) {
                    LOG.warn("Exception during loading file");
                    documentPreview.setText("");
                }
                previousFile = filePath;
            }
            try {
                highlightAndScrollRow(rowFile.getRowNumber(), rowFile.getPositions());
            } catch (BadLocationException ex) {
                LOG.warn("Exception during highlighting file");
                documentPreview.setText("");
            }
        } else {
            documentPreview.setText("");
        }
    }

    private RowFile getSelectedRowFile() {
        return tableModel.getRowFile(searchResultTable.getSelectedRow());
    }

    private void setPreviewSyntax(String fileExtension) {
        switch (fileExtension) {
            case "java":
                documentPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                break;
            case "html":
                documentPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
                break;
            case "xml":
            case "pom":
                documentPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                break;
            case "groovy":
            case "gradle":
                documentPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
                break;
            case "js":
                documentPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                break;
            case "py":
                documentPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
                break;
            case "rb":
                documentPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
                break;
            case "kt":
            case "kts":
                documentPreview.setSyntaxEditingStyle("text/kotlin");
                break;
            default:
                documentPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                break;
        }
    }

    private void highlightAndScrollRow(int rowNumber, List<Integer> positions) throws BadLocationException {
        String searchQuery = tableModel.getSearchQuery();
        if (!previousHighlights.isEmpty()) {
            previousHighlights.forEach(it -> documentPreview.getHighlighter().removeHighlight(it));
        }
        previousHighlights = new ArrayList<>();
        int startIndex = documentPreview.getLineStartOffset(rowNumber - 1);
        for (int position: positions) {
            int startPosition = startIndex + position;
            int endPosition = startPosition + searchQuery.length();
            Object highlight = documentPreview.getHighlighter().addHighlight(startPosition, endPosition, painter);
            previousHighlights.add(highlight);
        }
        documentPreview.setCaretPosition(startIndex);
    }
}


