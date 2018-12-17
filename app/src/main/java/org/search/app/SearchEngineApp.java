package org.search.app;

import org.search.app.listener.SearchActionListener;
import org.search.app.listener.SearchKeyListener;
import org.search.engine.SearchEngine;

import javax.swing.*;
import java.awt.*;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

class SearchEngineApp extends JFrame {

    private final SearchEngine searchEngine;

    SearchEngineApp() {
        this.searchEngine = new SearchEngine();
        initUI();
    }

    private void initUI() {
        setTitle("Search Engine App");
        setSize(600, 400);
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

        fileMenu.add(fileIndexMenuItem);
        fileMenu.add(folderIndexMenuItem);
        menuBar.add(fileMenu);

        setJMenuBar(menuBar);
    }

    private JPanel createUIPanel() {
        final JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        final JButton searchButton = new JButton("Search");
        final JTextField searchField = new JTextField();
        final JTextArea searchResultArea = createSearchResultArea();
        final JScrollPane scrollPane = new JScrollPane(searchResultArea, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);

        searchButton.addActionListener(new SearchActionListener(searchEngine, searchField, searchResultArea));
        searchField.addKeyListener(new SearchKeyListener(searchEngine, searchField, searchResultArea));

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField,BorderLayout.CENTER);
        searchPanel.add(searchButton,BorderLayout.EAST);

        panel.add(searchPanel, BorderLayout.PAGE_START);
        panel.add(scrollPane, BorderLayout.CENTER);

        final JTextArea documentPreviewArea = createDocumentPreviewArea();
        panel.add(documentPreviewArea, BorderLayout.PAGE_END);

        return panel;
    }

    private JTextArea createSearchResultArea() {
        final JTextArea searchResultArea = new JTextArea();
        searchResultArea.setEditable(false);
        return searchResultArea;
    }

    private JTextArea createDocumentPreviewArea() {
        final JTextArea documentPreviewArea = new JTextArea();
        documentPreviewArea.setPreferredSize(new Dimension(560, 100));
        documentPreviewArea.setEditable(false);
        return documentPreviewArea;
    }

}
