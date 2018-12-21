package org.search.engine.index;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.model.Document;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DocumentRemoveTaskTest extends AbstractDocumentIndexationTest {

    private DocumentRemoveTask removeTask;
    @Mock
    private FilesystemNotifier notificationManager;

    @Before
    public void setUp() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        URL resource = DocumentRemoveTaskTest.class.getResource(fileTitle);
        filePath = Paths.get(resource.toURI());
        Document removableDocument = new Document(documentId, true, filePath);
        indexedDocuments = new ConcurrentHashMap<>();
        index = new SearchEngineConcurrentTree();
        BlockingQueue<DocumentLine> documentLinesQueue = new LinkedBlockingQueue<>();
        DocumentReadTask indexTask = new DocumentReadTask(removableDocument, indexedDocuments, documentLinesQueue, notificationManager);
        IndexationSchedulerTask scheduler = new IndexationSchedulerTask(documentLinesQueue, index, new StandardTokenizer());
        indexTask.run();
        scheduler.run();
        removeTask = new DocumentRemoveTask(removableDocument, index, indexedDocuments, notificationManager);
    }

    @Test
    public void testDocumentRemoveFromIndex() {
        assertEquals(7, index.size());

        Set<Integer> searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());

        removeTask.run();
        verify(notificationManager, times(1)).registerFile(filePath);

        assertEquals(0, index.size());
        assertEquals(0, indexedDocuments.size());

        searchResult = index.getValue(searchQuery);
        assertTrue(searchResult.isEmpty());
    }

}