package org.search.engine.index;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DocumentIndexTaskTest extends AbstractDocumentIndexationTest {

    private DocumentIndexTask task;
    private Document indexingDocument;

    @Mock
    private FilesystemNotifier notificationManager;

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

        Set<Integer> searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());
        assertTrue(searchResult.contains(documentId));
    }

}