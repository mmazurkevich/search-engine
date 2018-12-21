package org.search.engine;

import org.nustaq.serialization.FSTConfiguration;
import org.search.engine.index.IndexationEventListener;
import org.search.engine.model.Document;
import org.search.engine.model.SerializableDocument;
import org.search.engine.tree.SearchEngineConcurrentTree;
import org.search.engine.tree.SearchEngineTree;
import org.search.engine.tree.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SearchEngineInitializer implements IndexationEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngineInitializer.class);

    private static final FSTConfiguration config = FSTConfiguration.createDefaultConfiguration();
    private static final String APP_FOLDER = System.getProperty("user.home") + "/.search-engine";
    private static final String TRACKED_FILES_FILE = "/files.se";
    private static final String TRACKED_FOLDERS_FILE = "/folders.se";
    private static final String INDEX_FILE = "/index.se";
    private static final String INDEXED_DOCUMENTS_FILE = "/documents.se";
    private static final String DOCUMENT_ID_FILE = "/id.se";

    //Unique concurrent document Id generator
    private AtomicInteger uniqueDocumentId;
    private Map<Path, Document> indexedDocuments;
    private SearchEngineTree index;
    // Files which changes tracked by system and were registered in the system by track. CopyOnWriteArrayList
    // used because of possibility of concurrent changes came from watch service and by user itself
    private Set<Path> trackedFiles;
    // Folders which changes tracked by system and were registered in the system by track.
    private Set<Path> trackedFolders;


    SearchEngineInitializer() throws IOException {
        Path folderPath = Paths.get(APP_FOLDER);
        if (!Files.exists(folderPath)) {
            Files.createDirectory(folderPath);
        }
        config.registerClass(TreeNode.class, String.class, AtomicInteger.class, HashMap.class);
        if (!initializeTrackedFiles() || !initializeTrackedFolders() || !initializeIndex()
                || !initializeIndexedDocuments() || !initializeDocumentId()) {
            uniqueDocumentId = new AtomicInteger();
            indexedDocuments = new ConcurrentHashMap<>();
            index = new SearchEngineConcurrentTree();
            trackedFiles = ConcurrentHashMap.newKeySet();
            trackedFolders = ConcurrentHashMap.newKeySet();
            LOG.info("Initialized empty engine");
        }
    }

    @Override
    public void onIndexationFinished() {
        saveEngineState();
    }

    AtomicInteger getUniqueDocumentId() {
        return uniqueDocumentId;
    }

    Map<Path, Document> getIndexedDocuments() {
        return indexedDocuments;
    }

    SearchEngineTree getIndex() {
        return index;
    }

    Set<Path> getTrackedFiles() {
        return trackedFiles;
    }

    Set<Path> getTrackedFolders() {
        return trackedFolders;
    }

    private boolean initializeTrackedFiles() {
        Path filePath = Paths.get(APP_FOLDER + TRACKED_FILES_FILE);
        if (Files.exists(filePath)) {
            try {
                byte[] fileBytes = Files.readAllBytes(filePath);
                List<String> paths = (ArrayList<String>) config.asObject(fileBytes);
                trackedFiles = ConcurrentHashMap.newKeySet();
                paths.forEach(it -> trackedFiles.add(Paths.get(it)));
                LOG.info("TrackedFiles loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read tracked files from file", e);
                return false;
            }
        }
        return false;
    }

    private boolean initializeTrackedFolders() {
        Path filePath = Paths.get(APP_FOLDER + TRACKED_FOLDERS_FILE);
        if (Files.exists(filePath)) {
            try {
                byte[] fileBytes = Files.readAllBytes(filePath);
                List<String> paths = (ArrayList<String>) config.asObject(fileBytes);
                trackedFolders = ConcurrentHashMap.newKeySet();
                paths.forEach(it -> trackedFolders.add(Paths.get(it)));
                LOG.info("TrackedFolders loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read tracked folders from file", e);
                return false;
            }
        }
        return false;
    }

    private boolean initializeIndex() {
        Path filePath = Paths.get(APP_FOLDER + INDEX_FILE);
        if (Files.exists(filePath)) {
            try {
                byte[] fileBytes = Files.readAllBytes(filePath);
                TreeNode root = (TreeNode) config.asObject(fileBytes);
                index = new SearchEngineConcurrentTree(root);
                LOG.info("Index loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read index from file", e);
                return false;
            }
        }
        return false;
    }

    private boolean initializeIndexedDocuments() {
        Path filePath = Paths.get(APP_FOLDER + INDEXED_DOCUMENTS_FILE);
        if (Files.exists(filePath)) {
            try {
                byte[] fileBytes = Files.readAllBytes(filePath);
                Map<String, SerializableDocument> documents = (HashMap<String, SerializableDocument>) config.asObject(fileBytes);
                indexedDocuments = new ConcurrentHashMap<>();
                documents.forEach((key, document) -> {
                    Path path = Paths.get(key);
                    indexedDocuments.put(path, new Document(document.getId(), document.isTracked(), path));
                });
                LOG.info("IndexedDocuments loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read indexedDocuments from file", e);
                return false;
            }
        }
        return false;
    }

    private boolean initializeDocumentId() {
        Path filePath = Paths.get(APP_FOLDER + DOCUMENT_ID_FILE);
        if (Files.exists(filePath)) {
            try {
                byte[] fileBytes = Files.readAllBytes(filePath);
                uniqueDocumentId = (AtomicInteger) config.asObject(fileBytes);
                LOG.info("UniqueDocumentId loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read unique document's id from file", e);
                return false;
            }
        }
        return false;
    }

    private void saveEngineState() {
        saveTrackedFiles();
        saveTrackedFolders();
        saveIndex();
        saveIndexedDocuments();
        saveDocumentId();
    }

    private void saveTrackedFiles() {
        try {
            Path filePath = Paths.get(APP_FOLDER + TRACKED_FILES_FILE);
            byte[] objectBytes = config.asByteArray(trackedFiles.stream()
                    .map(it -> it.toAbsolutePath().toString())
                    .collect(Collectors.toList())
            );
            Files.write(filePath, objectBytes);
        } catch (IOException e) {
            LOG.warn("Can't save tracked files state", e);
        }
    }

    private void saveTrackedFolders() {
        try {
            Path filePath = Paths.get(APP_FOLDER + TRACKED_FOLDERS_FILE);
            byte[] objectBytes = config.asByteArray(trackedFolders.stream()
                    .map(it -> it.toAbsolutePath().toString())
                    .collect(Collectors.toList())
            );
            Files.write(filePath, objectBytes);
        } catch (IOException e) {
            LOG.warn("Can't save tracked folders state", e);
        }
    }

    private void saveIndex() {
        try {
            Path filePath = Paths.get(APP_FOLDER + INDEX_FILE);
            byte[] objectBytes = config.asByteArray(index.getRoot());
            Files.write(filePath, objectBytes);
        } catch (IOException e) {
            LOG.warn("Can't save index state", e);
        }
    }

    private void saveIndexedDocuments() {
        try {
            Path filePath = Paths.get(APP_FOLDER + INDEXED_DOCUMENTS_FILE);
            byte[] objectBytes = config.asByteArray(indexedDocuments.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().toAbsolutePath().toString(),
                            e -> {
                                Document document = e.getValue();
                                return new SerializableDocument(document.getId(), document.isTracked(), document.getPath().toAbsolutePath().toString());
                            })));
            Files.write(filePath, objectBytes);
        } catch (IOException e) {
            LOG.warn("Can't save indexed documents state", e);
        }
    }

    private void saveDocumentId() {
        try {
            Path filePath = Paths.get(APP_FOLDER + DOCUMENT_ID_FILE);
            byte[] objectBytes = config.asByteArray(uniqueDocumentId);
            Files.write(filePath, objectBytes);
        } catch (IOException e) {
            LOG.warn("Can't save document's id state");
        }
    }
}
