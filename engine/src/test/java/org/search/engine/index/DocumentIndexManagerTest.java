package org.search.engine.index;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.filesystem.FilesystemEvent;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.model.Document;
import org.search.engine.tree.SearchEngineConcurrentTree;
import org.search.engine.tree.SearchEngineTree;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("Run without all test for not to affect them, BUT they all works")
public class DocumentIndexManagerTest extends AbstractDocumentIndexationTest {

    private WatchService watchService;
    private FilesystemNotifier notificationManager;
    private DocumentIndexManager indexManager;
    private Path createdFile;
    private Path createdFolder;
    private IndexationEventListener listener;

    @Before
    public void setUp() throws IOException, InterruptedException {
        watchService = FileSystems.getDefault().newWatchService();
        notificationManager = new FilesystemNotificationManager(watchService, new HashSet<>(), new HashSet<>());
        indexedDocuments = new ConcurrentHashMap<>();
        index = new SearchEngineConcurrentTree();
        indexManager = new DocumentIndexManager(index, indexedDocuments, notificationManager, new StandardTokenizer(), new AtomicInteger(), null);
        listener = new IndexationEventListener() {
            @Override
            public void onIndexationProgress(int progress) { }

            @Override
            public void onIndexationFinished() { }
        };
        Thread.sleep(2000);
    }

    @After
    public void tearDown() throws Exception {
        watchService.close();
        if (createdFile != null) {
            Files.delete(createdFile);
        }
        if (createdFolder != null) {
            Files.delete(createdFolder);
        }
    }

    @Test
    public void testIndexFile() throws URISyntaxException, InterruptedException {
        String filePath = DocumentIndexManagerTest.class.getResource(fileTitle).toURI().getRawPath();
        new Thread(() -> indexManager.indexFile(filePath)).start();

        waitForSize(indexedDocuments, 1);
        assertEquals(1, indexedDocuments.size());

        waitForSize(index, 7);
        Set<Integer> searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());
        assertTrue(searchResult.contains(documentId));
    }

    @Test
    public void testIndexFileTwice() throws URISyntaxException, InterruptedException {
        String filePath = DocumentIndexManagerTest.class.getResource(fileTitle).toURI().getRawPath();

        new Thread(() -> indexManager.indexFile(filePath)).start();

        waitForSize(indexedDocuments, 1);
        assertEquals(1, indexedDocuments.size());

        indexManager.indexFile(filePath);
        assertEquals(1, indexedDocuments.size());
    }

    @Test
    public void testUpdateIndexedFile() throws InterruptedException, IOException, URISyntaxException {
        final String searchQuery = "singletonList";
        URL resource = DocumentIndexManagerTest.class.getResource(folderTitle);
        createdFile = Paths.get(resource.toURI().getRawPath() + "/four.txt");
        Files.write(createdFile, Collections.singletonList("Text example"), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        new Thread(() -> indexManager.indexFile(createdFile.toString())).start();

        waitForSize(indexedDocuments, 1);
        assertEquals(1, indexedDocuments.size());

        waitForSize(index, 2);
        assertEquals(2, index.size());
        Set<Integer> searchResult = index.getValue("example");
        assertEquals(1, searchResult.size());

        Files.write(createdFile, Collections.singletonList(searchQuery), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        new Thread(() -> indexManager.onFileChanged(FilesystemEvent.MODIFIED, createdFile)).start();

        waitForSize(index, 3);
        searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());
    }

    @Test
    public void testDeleteIndexedFile() throws URISyntaxException, InterruptedException {
        URL resource = DocumentIndexManagerTest.class.getResource(fileTitle);
        indexManager.indexFile(resource.toURI().getRawPath());

        waitForSize(indexedDocuments, 1);
        assertEquals(1, indexedDocuments.size());

        Path filePath = Paths.get(resource.toURI());
        indexManager.onFileChanged(FilesystemEvent.DELETED, filePath);

        waitForSize(indexedDocuments, 0);
        assertTrue(indexedDocuments.isEmpty());
    }

    @Test
    public void testIndexFolder() throws URISyntaxException, InterruptedException {
        URL resource = DocumentIndexManagerTest.class.getResource(folderTitle);
        indexManager.indexFolder(resource.toURI().getRawPath(), listener);

        waitForSize(indexedDocuments, 2);
        assertEquals(2, indexedDocuments.size());
        String searchQuery = "mila";

        waitForSize(index, 11);
        Set<Integer> searchResult = index.getValue(searchQuery);
        assertEquals(2, searchResult.size());
        assertTrue(searchResult.contains(1));
        assertTrue(searchResult.contains(2));
    }

    @Test
    public void testAddFileToTrackedFolder() throws URISyntaxException, InterruptedException, IOException {
        final String searchQuery = "singletonList";
        URL resource = DocumentIndexManagerTest.class.getResource(folderTitle);
        Path folderPath = Paths.get(resource.toURI());
        indexManager.indexFolder(resource.toURI().getRawPath(), listener);

        waitForSize(indexedDocuments, 2);
        assertEquals(2, indexedDocuments.size());

        createdFile = Paths.get(folderPath.toAbsolutePath() + "/tree.txt");
        Files.write(createdFile, Collections.singletonList(searchQuery), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        indexManager.onFileChanged(FilesystemEvent.CREATED, createdFile);

        waitForSize(indexedDocuments, 3);
        assertEquals(3, indexedDocuments.size());

        waitForSize(index, 12);
        Set<Integer> searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());
    }

    @Test
    public void testRemoveTrackedFolder() throws URISyntaxException, InterruptedException, IOException {
        URL resource = DocumentIndexManagerTest.class.getResource(fileTitle);
        createdFolder = Paths.get(Paths.get(resource.toURI()).getParent().toAbsolutePath() + "/test");
        Files.createDirectory(createdFolder);

        createdFile = Paths.get(createdFolder.toAbsolutePath() + "/tree.txt");
        Files.write(createdFile, Collections.singletonList(searchQuery), StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        new Thread(() -> indexManager.indexFolder(createdFolder.toString(), listener)).start();

        waitForSize(indexedDocuments, 1);
        assertEquals(1, indexedDocuments.size());

        waitForSize(index, 1);
        Set<Integer> searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());

        indexManager.onFolderChanged(FilesystemEvent.DELETED, createdFolder);

        waitForSize(indexedDocuments, 0);
        assertTrue(indexedDocuments.isEmpty());

        waitForSize(index, 0);
        searchResult = index.getValue(searchQuery);
        assertTrue(searchResult.isEmpty());
    }

    private void waitForSize(Map<Path, Document> map, int expectedSize) throws InterruptedException {
        while (map.size() != expectedSize) {
            Thread.sleep(100);
        }
    }

    private void waitForSize(SearchEngineTree index, int expectedSize) throws InterruptedException {
        while (index.size() != expectedSize) {
            Thread.sleep(100);
        }
    }
}