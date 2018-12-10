package org.search.engine.index;

import org.search.engine.analyzer.Tokenizer;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.tree.SearchEngineConcurrentTree;
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
    private final SearchEngineConcurrentTree index;
    private final Document indexingDocument;
    private final Tokenizer tokenizer;
    private final boolean shouldTrack;

    DocumentIndexTask(Document indexingDocument, SearchEngineConcurrentTree index, List<Document> indexedDocuments,
                      FilesystemNotificationManager notificationManager, Tokenizer tokenizer, boolean shouldTrack) {
        this.index = index;
        this.indexedDocuments = indexedDocuments;
        this.indexingDocument = indexingDocument;
        this.notificationManager = notificationManager;
        this.tokenizer = tokenizer;
        this.shouldTrack = shouldTrack;
    }

    @Override
    public void run() {
        //Index needed document and check in exception should it be rolled back
        try (Stream<String> lines = Files.lines(indexingDocument.getPath())) {
            long start = System.currentTimeMillis();

            lines.forEach(line -> tokenizer.tokenize(line).forEach(token -> {
                index.putMergeOnConflict(token, indexingDocument.getId());
            }));

            long end = System.currentTimeMillis();
            LOG.debug("Indexation of file: {} took {}ms", indexingDocument.getPath(), (end - start));
            if (shouldTrack) {
                notificationManager.registerFile(indexingDocument.getPath());
            }
            indexedDocuments.add(indexingDocument);
        } catch (IOException e) {
            LOG.warn("Indexation of file: {} finished with exception", indexingDocument.getPath());
        }
    }
}
