package org.search.engine.index;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.model.Document;
import org.search.engine.model.IndexationEvent;
import org.search.engine.model.SearchType;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DocumentIndexationTest extends AbstractDocumentIndexationTest {

    private DocumentReadTask task;
    private IndexationSchedulerTask scheduler;
    private Document indexingDocument;

    @Mock
    private FilesystemNotifier notificationManager;

    @Before
    public void setUp() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        URL resource = DocumentIndexationTest.class.getResource(fileTitle);
        filePath = Paths.get(resource.toURI());
        indexingDocument = new Document(documentId, true, filePath, 1);
        indexedDocuments = new ConcurrentHashMap<>();
        index = new SearchEngineConcurrentTree();
        BlockingQueue<IndexationEvent> documentLinesQueue = new LinkedBlockingQueue<>();
        task = new DocumentReadTask(indexingDocument, indexedDocuments, documentLinesQueue, notificationManager);
        scheduler = new IndexationSchedulerTask(documentLinesQueue, index, new StandardTokenizer(), new ArrayList<>());
    }

    @Test
    public void testDocumentIndexation() {

        assertTrue(indexedDocuments.isEmpty());

        task.run();
        verify(notificationManager, times(1)).registerFile(filePath);
        scheduler.run();

        assertEquals(7, index.size());
        assertEquals(1, indexedDocuments.size());
        assertTrue(indexedDocuments.containsValue(indexingDocument));

        Set<Integer> searchResult = index.getValue(searchQuery, SearchType.EXACT_MATCH);
        assertEquals(1, searchResult.size());
        assertTrue(searchResult.contains(documentId));
    }

}