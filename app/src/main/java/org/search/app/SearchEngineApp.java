package org.search.app;

import org.search.app.component.JSearchResultTable;
import org.search.app.listener.FileSelectionListener;
import org.search.app.listener.SearchActionListener;
import org.search.app.listener.SearchKeyListener;
import org.search.app.model.SearchResultTableModel;
import org.search.engine.SearchEngine;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

class SearchEngineApp extends JFrame {

    private final SearchEngine searchEngine;

    //UI components
    private JButton searchButton;
    private JTextField searchField;
    private JTextArea documentPreview;
    private JSearchResultTable searchResultTable;

    SearchEngineApp() {
        this.searchEngine = new SearchEngine();
        initUI();
        //Initialize index in thread for not blocking UI
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                searchEngine.initialize();
                return null;
            }
        };
        worker.execute();
    }

    private void initUI() {
        setTitle("Search Engine App");
        setSize(800, 600);
        getContentPane().add(createUIPanel());
        createMenuBar();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        final JMenu fileMenu = new JMenu("Index");

        //Initialize menu item for folder index
        final JMenuItem folderIndexMenuItem = new JMenuItem("Index folder");
        folderIndexMenuItem.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select folder for indexation");

            if (chooser.showDialog(this, "Index") == JFileChooser.APPROVE_OPTION) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        searchEngine.indexFolder(chooser.getSelectedFile().getPath());
                        return null;
                    }
                };
                worker.execute();
            }
        });

        //Initialize menu item for file index
        final JMenuItem fileIndexMenuItem = new JMenuItem("Index file");
        fileIndexMenuItem.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setDialogTitle("Select file for indexation");

            if (chooser.showDialog(this, "Index") == JFileChooser.APPROVE_OPTION) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

                    @Override
                    protected Void doInBackground() {
                        searchEngine.indexFile(chooser.getSelectedFile().getPath());
                        return null;
                    }
                };
                worker.execute();
            }
        });

        //Initialize menu item for invalidate cache
        final JMenuItem invalidateCacheMenuItem = new JMenuItem("Invalidate cache");
        invalidateCacheMenuItem.addActionListener(event -> {
            searchEngine.invalidateCache();
            searchField.setText("");
            documentPreview.setText("");
            searchResultTable.getSelectionModel().clearSelection();
            searchResultTable.getModel().setData(Collections.emptyList());
            searchResultTable.getModel().fireTableDataChanged();
        });

        fileMenu.add(fileIndexMenuItem);
        fileMenu.add(folderIndexMenuItem);
        fileMenu.add(invalidateCacheMenuItem);
        menuBar.add(fileMenu);

        setJMenuBar(menuBar);
    }

    private JPanel createUIPanel() {
        final JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        searchButton = new JButton("Search");
        searchField = new JTextField();
        documentPreview = createDocumentPreviewArea();

        final SearchResultTableModel tableModel = new SearchResultTableModel();
        searchResultTable = new JSearchResultTable(tableModel);
        searchResultTable.setFillsViewportHeight(true);
        final JScrollPane documentPreviewScrollPane = new JScrollPane(documentPreview, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        documentPreviewScrollPane.setPreferredSize(new Dimension(0, 150));
        searchResultTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultTable.getSelectionModel().addListSelectionListener(new FileSelectionListener(tableModel, searchResultTable, documentPreview));
        final JScrollPane scrollPane = new JScrollPane(searchResultTable, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);

        searchButton.addActionListener(new SearchActionListener(searchEngine, searchField, searchResultTable));
        searchField.addKeyListener(new SearchKeyListener(searchEngine, searchField, searchResultTable));

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        panel.add(searchPanel, BorderLayout.PAGE_START);
        panel.add(scrollPane, BorderLayout.CENTER);

        panel.add(documentPreviewScrollPane, BorderLayout.PAGE_END);

        return panel;
    }

    private JTextArea createDocumentPreviewArea() {
        final JTextArea documentPreviewArea = new JTextArea();
        documentPreviewArea.setEditable(false);
        return documentPreviewArea;
    }

}
