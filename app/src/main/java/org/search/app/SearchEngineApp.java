package org.search.app;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.search.app.component.JSearchResultTable;
import org.search.app.listener.FileSelectionListener;
import org.search.app.listener.SearchActionListener;
import org.search.app.listener.SearchKeyListener;
import org.search.app.model.SearchResultTableModel;
import org.search.app.worker.FolderIndexationWorker;
import org.search.app.worker.InitializationWorker;
import org.search.engine.SearchEngine;
import org.search.engine.model.SearchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.net.URISyntaxException;
import java.util.Collections;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

class SearchEngineApp extends JFrame {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngineApp.class);

    private final SearchEngine searchEngine;
    private HunspellCheck hunspellCheck = null;

    //UI components
    private ButtonGroup searchOptionsGroup;
    private JButton searchButton;
    private JButton cancelButton;
    private JTextField searchField;
    private RSyntaxTextArea documentPreview;
    private JSearchResultTable searchResultTable;
    private JPanel progressBarPanel;
    private JLabel progressDescription;
    private JLabel possibleSuggestions;
    private JProgressBar progressBar;
    private FolderIndexationWorker indexationWorker;

    SearchEngineApp() {
        this.searchEngine = new SearchEngine();
        try {
            this.hunspellCheck = new HunspellCheck();
        } catch (URISyntaxException e) {
            LOG.warn("Can't initialize hunspell, application will work without spellcheck");
        }
        initUI();
        //Initialize index in thread for not blocking UI
        InitializationWorker initializationWorker = new InitializationWorker(cancelButton, searchField, progressBarPanel,
                progressDescription, progressBar, searchEngine);
        initializationWorker.execute();
    }

    private void initUI() {
        setTitle("Search Engine App");
        setSize(800, 600);
        createProgressPanel();
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
                indexationWorker = new FolderIndexationWorker(searchEngine, progressBarPanel, progressBar, progressDescription,
                        chooser.getSelectedFile().getPath(), folderIndexMenuItem);
                folderIndexMenuItem.setEnabled(false);
                progressBarPanel.setVisible(true);
                progressBar.setValue(0);
                progressDescription.setText("Prepare to indexation");
                indexationWorker.execute();
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
            possibleSuggestions.setText("");
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
        searchOptionsGroup = new ButtonGroup();
        searchField = new JTextField();
        possibleSuggestions = new JLabel();

        documentPreview = createDocumentPreviewArea();

        final SearchResultTableModel tableModel = new SearchResultTableModel();
        searchResultTable = new JSearchResultTable(tableModel);
        searchResultTable.setFillsViewportHeight(true);
        final JScrollPane documentPreviewScrollPane = new RTextScrollPane(documentPreview, true);
        documentPreviewScrollPane.setPreferredSize(new Dimension(0, 150));
        searchResultTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultTable.getSelectionModel().addListSelectionListener(new FileSelectionListener(tableModel,
                searchResultTable, documentPreview));
        final JScrollPane scrollPane = new JScrollPane(searchResultTable, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);

        searchButton.addActionListener(new SearchActionListener(searchEngine, searchField, searchResultTable, searchOptionsGroup,
                hunspellCheck, possibleSuggestions));
        searchField.addKeyListener(new SearchKeyListener(searchEngine, searchField, searchResultTable, searchOptionsGroup,
                hunspellCheck, possibleSuggestions));

        //Creating search form with options
        panel.add(createSearchPanelWithOptions(), BorderLayout.PAGE_START);
        panel.add(scrollPane, BorderLayout.CENTER);

        //Creating bottom part of UI: Document preview and progress bar
        JPanel documentPreviewAndProgressPanel = new JPanel();
        documentPreviewAndProgressPanel.setLayout(new BoxLayout(documentPreviewAndProgressPanel, BoxLayout.Y_AXIS));

        JPanel documentPreviewPanel = new JPanel(new BorderLayout());
        documentPreviewPanel.add(documentPreviewScrollPane, BorderLayout.PAGE_END);

        documentPreviewAndProgressPanel.add(documentPreviewPanel);
        documentPreviewAndProgressPanel.add(progressBarPanel);
        panel.add(documentPreviewAndProgressPanel, BorderLayout.PAGE_END);

        return panel;
    }

    private JPanel createSearchPanelWithOptions() {
        JPanel searchPanelWithOptions = new JPanel();
        searchPanelWithOptions.setLayout(new BoxLayout(searchPanelWithOptions, BoxLayout.Y_AXIS));

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        JRadioButton exactMatchButton = new JRadioButton("Exact match");
        exactMatchButton.setActionCommand(SearchType.EXACT_MATCH.name());
        exactMatchButton.addActionListener(it -> searchButton.doClick());
        exactMatchButton.setSelected(true);

        JRadioButton startWithButton = new JRadioButton("Start with");
        startWithButton.setActionCommand(SearchType.START_WITH.name());
        startWithButton.addActionListener(it -> searchButton.doClick());

        JRadioButton withSuggestionsButton = new JRadioButton("With suggestions");
        withSuggestionsButton.setActionCommand(SearchType.WITH_SUGGESTIONS.name());
        withSuggestionsButton.addActionListener(it -> searchButton.doClick());

        searchOptionsGroup.add(exactMatchButton);
        searchOptionsGroup.add(startWithButton);
        searchOptionsGroup.add(withSuggestionsButton);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.LINE_AXIS));
        optionsPanel.add(exactMatchButton);
        optionsPanel.add(startWithButton);
        optionsPanel.add(withSuggestionsButton);

        JPanel searchOptionsPanel = new JPanel(new BorderLayout());
        searchOptionsPanel.add(optionsPanel, BorderLayout.EAST);
        searchOptionsPanel.add(possibleSuggestions, BorderLayout.WEST);

        searchPanelWithOptions.add(searchPanel);
        searchPanelWithOptions.add(searchOptionsPanel);
        return searchPanelWithOptions;
    }

    private void createProgressPanel() {
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionEvent -> {
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() {
                    searchEngine.cancelFolderIndexation();
                    indexationWorker.cancel();
                    return null;
                }
            };
            worker.execute();
        });

        progressDescription = new JLabel();

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        progressBarPanel = new JPanel(new BorderLayout());
        progressBarPanel.add(progressDescription, BorderLayout.WEST);
        progressBarPanel.add(progressBar, BorderLayout.CENTER);
        progressBarPanel.add(cancelButton, BorderLayout.EAST);
        progressBarPanel.setVisible(false);
    }

    private RSyntaxTextArea createDocumentPreviewArea() {
        AbstractTokenMakerFactory abstractTokenMakerFactory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        abstractTokenMakerFactory.putMapping("text/kotlin", "org.search.app.model.KotlinTokenMaker");

        final RSyntaxTextArea documentPreviewArea = new RSyntaxTextArea();
        documentPreviewArea.setPopupMenu(null);
        documentPreviewArea.setEditable(false);
        return documentPreviewArea;
    }

}
