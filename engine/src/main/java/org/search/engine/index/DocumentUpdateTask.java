package org.search.engine.index;

import org.search.engine.analyzer.Tokenizer;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DocumentUpdateTask implements Runnable {

    private static final Logger LOG = Logger.getLogger(DocumentUpdateTask.class.getName());

    private final SearchEngineConcurrentTree index;
    private final Document updatingDocument;
    private final Tokenizer tokenizer;

    DocumentUpdateTask(Document updatingDocument, SearchEngineConcurrentTree index, Tokenizer tokenizer) {
        this.index = index;
        this.updatingDocument = updatingDocument;
        this.tokenizer = tokenizer;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        int documentId = updatingDocument.getId();

        //Old tokens which should be removed from index
        Set<String> oldDocumentTokens = index.getKeys(documentId);
        try (Stream<String> lines = Files.lines(updatingDocument.getPath())) {
            lines.forEach(line -> tokenizer.tokenize(line).forEach(token -> {
                if (oldDocumentTokens.contains(token)) {
                    oldDocumentTokens.remove(token);
                } else {
                    //It's a new token, should be added to the index
                    index.putMergeOnConflict(token, documentId);
                }
            }));
            oldDocumentTokens.forEach(index::removeByKey);
            long end = System.currentTimeMillis();
            LOG.info("Update index for file: " + updatingDocument.getPath() + " took " + (end - start) + "ms");
        } catch (IOException e) {
            LOG.warning("Update index for file: " + updatingDocument.getPath() + " finished with exception");
        }
    }
}
