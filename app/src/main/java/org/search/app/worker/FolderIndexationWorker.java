package org.search.app.worker;

import org.search.engine.SearchEngine;
import org.search.engine.index.IndexationEventListener;

import javax.swing.*;
import java.util.List;

public class FolderIndexationWorker extends SwingWorker<Void, Integer> implements IndexationEventListener {

    private final SearchEngine searchEngine;
    private final JPanel progressBarPanel;
    private final JProgressBar progressBar;
    private final String folderPath;
    private final JMenuItem menuItem;
    private final JLabel progressDescription;

    public FolderIndexationWorker(SearchEngine searchEngine, JPanel progressBarPanel, JProgressBar progressBar, JLabel progressDescription,
                                  String folderPath, JMenuItem menuItem) {
        this.searchEngine = searchEngine;
        this.progressBarPanel = progressBarPanel;
        this.progressBar = progressBar;
        this.folderPath = folderPath;
        this.menuItem = menuItem;
        this.progressDescription = progressDescription;
    }

    @Override
    protected Void doInBackground() {
        searchEngine.indexFolder(folderPath, this);
        return null;
    }

    @Override
    protected void process(List<Integer> list) {
        list.forEach(it -> {
            if (it >= 0 && it < 100) {
                if (!progressBarPanel.isVisible()) {
                    progressBarPanel.setVisible(true);
                }
                progressDescription.setText("Indexation");
                progressBar.setValue(it);
            } else if (it == -1) {
                menuItem.setEnabled(true);
                progressDescription.setText("Indexation finished");
                progressBarPanel.setVisible(false);
            }
        });
    }

    @Override
    public void onIndexationProgress(int progress) {
        publish(progress);
    }

    @Override
    public void onIndexationFinished() {
        publish(-1);
    }
}
