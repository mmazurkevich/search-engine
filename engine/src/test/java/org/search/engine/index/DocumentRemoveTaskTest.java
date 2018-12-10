package org.search.engine.index;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.tree.SearchEngineConcurrentTree;
import org.search.engine.tree.SearchEngineTree;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DocumentRemoveTaskTest extends AbstractDocumentTaskTest {

    private DocumentRemoveTask removeTask;

    @Before
    public void setUp() throws URISyntaxException {
        URL resource = DocumentIndexTaskTest.class.getResource(fileTitle);
        filePath = Paths.get(resource.toURI());
        Document removableDocument = new Document(documentId, false, filePath);
        indexedDocuments = new ArrayList<>();
        index = new SearchEngineConcurrentTree();
        DocumentIndexTask indexTask = new DocumentIndexTask(removableDocument, index, indexedDocuments, null, new StandardTokenizer());
        indexTask.run();
        removeTask = new DocumentRemoveTask(removableDocument, index, indexedDocuments);
    }

    @Test
    public void testDocumentRemoveFromIndex() {
        assertEquals(7, index.size());

        String searchQuery = "surfeits";
        List<Integer> searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());

        removeTask.run();
        assertEquals(0, index.size());
        assertEquals(0, indexedDocuments.size());

        searchResult = index.getValue(searchQuery);
        assertTrue(searchResult.isEmpty());
    }

}