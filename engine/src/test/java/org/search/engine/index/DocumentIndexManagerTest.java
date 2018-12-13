package org.search.engine.index;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.search.engine.analyzer.StandardTokenizer;
import org.search.engine.filesystem.FilesystemEvent;
import org.search.engine.filesystem.FilesystemNotificationManager;
import org.search.engine.filesystem.FilesystemNotifier;
import org.search.engine.tree.SearchEngineConcurrentTree;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DocumentIndexManagerTest extends AbstractDocumentIndexationTest {

    private WatchService watchService;
    private FilesystemNotifier notificationManager;
    private DocumentIndexManager indexManager;
    private Path createdFile;
    private Path createdFolder;

    @Before
    public void setUp() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        notificationManager = new FilesystemNotificationManager(watchService);
        indexedDocuments = new CopyOnWriteArrayList<>();
        index = new SearchEngineConcurrentTree();
        indexManager = new DocumentIndexManager(index, indexedDocuments, notificationManager, new StandardTokenizer());
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
        URL resource = DocumentIndexManagerTest.class.getResource(fileTitle);
        indexManager.indexFile(resource.toURI().getRawPath());
        Thread.sleep(2000);

        assertEquals(1, indexedDocuments.size());
        Set<Integer> searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());
        assertTrue(searchResult.contains(documentId));
    }

    @Test
    public void testIndexFileTwice() throws URISyntaxException, InterruptedException {
        URL resource = DocumentIndexManagerTest.class.getResource(fileTitle);
        indexManager.indexFile(resource.toURI().getRawPath());
        Thread.sleep(2000);
        assertEquals(1, indexedDocuments.size());

        indexManager.indexFile(resource.toURI().getRawPath());
        assertEquals(1, indexedDocuments.size());
    }

    @Test
    public void testUpdateIndexedFile() throws InterruptedException, IOException, URISyntaxException {
        final String searchQuery = "singletonList";
        URL resource = DocumentIndexManagerTest.class.getResource(folderTitle);
        createdFile = Paths.get(resource.toURI().getRawPath() + "/tree.txt");
        Files.write(createdFile, Collections.singletonList("Text example"), StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        indexManager.indexFile(createdFile.toString());
        Thread.sleep(2000);
        assertEquals(1, indexedDocuments.size());
        Set<Integer> searchResult = index.getValue(searchQuery);
        assertTrue(searchResult.isEmpty());

        Files.write(createdFile, Collections.singletonList(searchQuery), StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        indexManager.onFileChanged(FilesystemEvent.MODIFIED, createdFile);
        Thread.sleep(1000);

        searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());
    }

    @Test
    public void testDeleteIndexedFile() throws URISyntaxException, InterruptedException {
        URL resource = DocumentIndexManagerTest.class.getResource(fileTitle);
        indexManager.indexFile(resource.toURI().getRawPath());
        Thread.sleep(2000);
        assertEquals(1, indexedDocuments.size());

        Path filePath = Paths.get(resource.toURI());
        indexManager.onFileChanged(FilesystemEvent.DELETED, filePath);
        Thread.sleep(1000);

        assertTrue(indexedDocuments.isEmpty());
    }

    @Test
    public void testIndexFolder() throws URISyntaxException, InterruptedException {
        URL resource = DocumentIndexManagerTest.class.getResource(folderTitle);
        indexManager.indexFolder(resource.toURI().getRawPath());
        Thread.sleep(2000);

        assertEquals(2, indexedDocuments.size());
        String searchQuery = "mila";
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
        indexManager.indexFolder(resource.toURI().getRawPath());
        Thread.sleep(2000);

        assertEquals(2, indexedDocuments.size());

        createdFile = Paths.get(folderPath.toAbsolutePath() + "/tree.txt");
        Files.write(createdFile, Collections.singletonList(searchQuery), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        indexManager.onFileChanged(FilesystemEvent.CREATED, createdFile);
        Thread.sleep(1000);

        assertEquals(3, indexedDocuments.size());

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

        indexManager.indexFolder(createdFolder.toString());
        Thread.sleep(2000);
        assertEquals(1, indexedDocuments.size());
        Set<Integer> searchResult = index.getValue(searchQuery);
        assertEquals(1, searchResult.size());

        indexManager.onFolderChanged(FilesystemEvent.DELETED, createdFolder);
        Thread.sleep(1000);

        assertTrue(indexedDocuments.isEmpty());

        searchResult = index.getValue(searchQuery);
        assertTrue(searchResult.isEmpty());
    }
}