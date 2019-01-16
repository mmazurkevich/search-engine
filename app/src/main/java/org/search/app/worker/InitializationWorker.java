package org.search.app.worker;

import org.search.engine.SearchEngine;
import org.search.engine.SearchEngineInitializationListener;

import javax.swing.*;
import java.util.List;

public class InitializationWorker extends SwingWorker<Void, Long> implements SearchEngineInitializationListener {

    private static final Long STARTED = -1L;
    private static final Long FINISHED = -2L;

    private final JButton cancelButton;
    private final JTextField searchField;
    private final JPanel progressBarPanel;
    private final JLabel progressDescription;
    private final JProgressBar progressBar;
    private final SearchEngine searchEngine;

    public InitializationWorker(JButton cancelButton, JTextField searchField, JPanel progressBarPanel,
                                JLabel progressDescription, JProgressBar progressBar, SearchEngine searchEngine) {
        this.cancelButton = cancelButton;
        this.searchField = searchField;
        this.progressBarPanel = progressBarPanel;
        this.progressDescription = progressDescription;
        this.progressBar = progressBar;
        this.searchEngine = searchEngine;
    }

    @Override
    protected Void doInBackground() throws Exception {
        publish(STARTED);
        searchEngine.initialize(this);
        publish(FINISHED);
        return null;
    }

    @Override
    protected void process(List<Long> list) {
        list.forEach(it -> {
            if (it >= 0 && it < 100) {
                progressDescription.setText("Reading index from disk...");
                progressBar.setValue(it.intValue());
            } else if (it.equals(FINISHED)) {
                cancelButton.setVisible(true);
                progressBar.setValue(0);
                progressDescription.setText("");
                progressBarPanel.setVisible(false);
                searchField.setEditable(true);
            } else if (it.equals(STARTED)) {
                cancelButton.setVisible(false);
                progressBar.setValue(0);
                progressDescription.setText("Prepare for reading index...");
                progressBarPanel.setVisible(true);
                searchField.setEditable(false);
            }
        });
    }

    @Override
    public void onInitializationProgress(long progress) {
        publish(progress);
    }
}
