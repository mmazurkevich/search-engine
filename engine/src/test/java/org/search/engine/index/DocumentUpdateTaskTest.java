package org.search.engine.index;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.model.Document;
import org.search.engine.model.IndexationEvent;
import org.search.engine.model.SearchType;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DocumentUpdateTaskTest extends AbstractDocumentIndexationTest {

    private DocumentUpdateTask updateTask;
    private IndexationSchedulerTask scheduler;
    private Path filePath;

    @Before
    public void setUp() throws IOException {
        filePath = Paths.get("./TestFileTwo.txt");
        Files.write(filePath, Collections.singletonList("Example of text for test file"), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);

        Document updatedDocument = new Document(documentId, false, filePath, 1);
        indexedDocuments = new ConcurrentHashMap<>();
        index = new SearchEngineConcurrentTree();
        Tokenizer tokenizer = new StandardTokenizer();
        BlockingQueue<IndexationEvent> documentLinesQueue = new LinkedBlockingQueue<>();
        DocumentReadTask indexTask = new DocumentReadTask(updatedDocument, indexedDocuments, documentLinesQueue, null);
        scheduler = new IndexationSchedulerTask(documentLinesQueue, index, new StandardTokenizer(), new ArrayList<>());
        indexTask.run();
        scheduler.run();
        updateTask = new DocumentUpdateTask(updatedDocument, index, tokenizer, documentLinesQueue);
    }

    @After
    public void tearDown() throws IOException {
        Files.delete(filePath);
    }

    @Test
    public void testDocumentUpdateIndex() throws IOException {
        assertEquals(6, index.size());

        String searchQuery = "another";
        Set<Integer> searchResult = index.getValue(searchQuery, SearchType.EXACT_MATCH);
        assertTrue(searchResult.isEmpty());

        Files.write(filePath, Collections.singletonList(" another thing"), StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
        updateTask.run();
        scheduler.run();

        assertEquals(8, index.size());
        searchResult = index.getValue(searchQuery, SearchType.EXACT_MATCH);
        assertEquals(1, searchResult.size());
    }

}