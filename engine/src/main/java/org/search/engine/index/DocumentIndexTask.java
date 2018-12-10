package org.search.engine.index;

import com.sun.istack.internal.NotNull;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

public class DocumentIndexTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentIndexTask.class);

    private final FilesystemNotificationManager notificationManager;
    private final List<Document> indexedDocuments;
    private final SearchEngineTree index;
    private final Document indexingDocument;
    private final Tokenizer tokenizer;

    DocumentIndexTask(Document indexingDocument, SearchEngineTree index, List<Document> indexedDocuments,
                      FilesystemNotificationManager notificationManager, Tokenizer tokenizer) {
        this.index = index;
        this.indexedDocuments = indexedDocuments;
        this.indexingDocument = indexingDocument;
        this.notificationManager = notificationManager;
        this.tokenizer = tokenizer;
    }

    @Override
    public void run() {
        try (Stream<String> lines = Files.lines(indexingDocument.getPath())) {
            long start = System.currentTimeMillis();

            lines.forEach(line -> tokenizer.tokenize(line).forEach(token -> {
                index.putMergeOnConflict(token, indexingDocument.getId());
            }));

            long end = System.currentTimeMillis();
            LOG.debug("Indexation of file: {} took {}ms", indexingDocument.getPath(), (end - start));
            if (indexingDocument.isTracked()) {
                notificationManager.registerFile(indexingDocument.getPath());
            }
            indexedDocuments.add(indexingDocument);
        } catch (IOException ex) {
            LOG.warn("Indexation of file: {} finished with exception", indexingDocument.getPath(), ex);
        }
    }
}
