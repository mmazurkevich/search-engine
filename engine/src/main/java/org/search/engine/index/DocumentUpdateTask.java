package org.search.engine.index;

import org.search.engine.analyzer.Tokenizer;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Stream;

public class DocumentUpdateTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentUpdateTask.class);

    private final SearchEngineTree index;
    private final Document updatingDocument;
    private final Tokenizer tokenizer;

    DocumentUpdateTask(Document updatingDocument, SearchEngineTree index, Tokenizer tokenizer) {
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
            LOG.debug("Update index for file: {} took {}ms", updatingDocument.getPath(), (end - start));
        } catch (IOException ex) {
            LOG.warn("Update index for file: {} finished with exception", updatingDocument.getPath());
        }
    }
}
