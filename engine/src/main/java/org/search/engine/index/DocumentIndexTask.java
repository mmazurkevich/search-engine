package org.search.engine.index;

import gnu.trove.set.hash.TIntHashSet;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DocumentIndexTask implements Runnable {

    private static final Logger LOG = Logger.getLogger(DocumentIndexTask.class.getName());

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
            LOG.info("Indexation of file: " + indexingDocument.getPath() + " took " + (end - start) + "ms");
            if (shouldTrack) {
                notificationManager.registerFile(indexingDocument.getPath());
            }
            indexedDocuments.add(indexingDocument);
        } catch (IOException e) {
            LOG.warning("Indexation of file: " + indexingDocument.getPath() + " finished with exception");
        }
    }
}
