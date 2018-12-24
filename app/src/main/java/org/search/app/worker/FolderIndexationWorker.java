package org.search.app.worker;

import org.search.engine.SearchEngine;
import org.search.engine.index.IndexationEventListener;

import javax.swing.*;
import java.util.List;

public class FolderIndexationWorker extends SwingWorker<Void, String> implements IndexationEventListener {

    private final SearchEngine searchEngine;
    private final JPanel progressBarPanel;
    private final JProgressBar progressBar;
    private final String folderPath;

    public FolderIndexationWorker(SearchEngine searchEngine, JPanel progressBarPanel, JProgressBar progressBar, String folderPath) {
        this.searchEngine = searchEngine;
        this.progressBarPanel = progressBarPanel;
        this.progressBar = progressBar;
        this.folderPath = folderPath;
    }

    @Override
    protected Void doInBackground() {
        publish("Start");
        searchEngine.indexFolder(folderPath);
        return null;
    }

    @Override
    protected void process(List<String> list) {
        list.forEach(it -> {
            if (it.equals("Start")) {
                progressBarPanel.setVisible(true);
                progressBar.setValue(0);
            } else {
                progressBarPanel.setVisible(false);
            }
        });
    }

//    @Override
//    public void onIndexationProgress(int progress) {
//        publish("Start");
//    }

    @Override
    public void onIndexationFinished() {
        publish("Finish");
    }
}
