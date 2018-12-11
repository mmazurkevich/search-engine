package org.search.engine.index;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DocumentIndexTaskTest extends AbstractDocumentIndexationTest {

    private DocumentIndexTask task;
    private Document indexingDocument;

    @Mock
    private FilesystemNotificationManager notificationManager;

    @Before
    public void setUp() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        URL resource = DocumentIndexTaskTest.class.getResource(fileTitle);
        filePath = Paths.get(resource.toURI());
        indexingDocument = new Document(documentId, true, filePath);
        indexedDocuments = new ArrayList<>();
        index = new SearchEngineConcurrentTree();
        task = new DocumentIndexTask(indexingDocument, index, indexedDocuments, notificationManager, new StandardTokenizer());
    }

    @Test
    public void testDocumentIndexation() {

        assertTrue(indexedDocuments.isEmpty());

        task.run();
        verify(notificationManager, times(1)).registerFile(filePath);

        assertEquals(7, index.size());
        assertEquals(1, indexedDocuments.size());
        assertTrue(indexedDocuments.contains(indexingDocument));

        List<Integer> searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());
        assertEquals(documentId, searchResult.get(0).intValue());
    }

}