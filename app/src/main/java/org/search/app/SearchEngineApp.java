package org.search.app;

import org.search.engine.SearchEngine;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
        final JPanel panel = new JPanel();

        JButton searchButton = new JButton("Search");
        JTextField searchField = createSearchField();
        JTextArea searchResultArea = createSearchResultArea();
        JScrollPane scrollPane = createScrollForSearchResult(searchResultArea);
        searchButton.addActionListener(e -> {
            SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    return searchEngine.search(searchField.getText()).stream()
                            .collect(Collectors.joining("\n"));
                }

                @Override
                protected void done() {
                    try {
                        searchResultArea.setText(get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            };
            worker.execute();
        });

        panel.add(searchField);
        panel.add(searchButton);
        panel.add(scrollPane);
        return panel;
    }

    private JTextField createSearchField() {
        final JTextField searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(480, 28));
        return searchField;
    }

    private JTextArea createSearchResultArea() {
        final JTextArea searchResultArea = new JTextArea();
        searchResultArea.setEditable(false);
        return searchResultArea;
    }

    private JScrollPane createScrollForSearchResult(JTextArea searchResults) {
        final JScrollPane scrollPane = new JScrollPane(searchResults, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(560, 300));
        return scrollPane;
    }
}
