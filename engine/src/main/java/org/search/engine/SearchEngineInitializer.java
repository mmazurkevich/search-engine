package org.search.engine;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;
import org.search.engine.index.Document;
import org.search.engine.index.IndexationEventListener;
import org.search.engine.tree.SearchEngineConcurrentTree;
import org.search.engine.tree.SearchEngineTree;
import org.search.engine.tree.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        config.registerClass(TreeNode.class, String.class, AtomicInteger.class, ConcurrentHashMap.class);
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
                trackedFiles = (ConcurrentHashMap.KeySetView) config.asObject(fileBytes);
                LOG.info("Tracked files loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read tracked files from file");
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
                trackedFolders = (ConcurrentHashMap.KeySetView) config.asObject(fileBytes);
                LOG.info("Tracked folders loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read tracked folders from file");
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
                index = (SearchEngineConcurrentTree) config.asObject(fileBytes);
                LOG.info("Index loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read index from file");
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
                indexedDocuments = (ConcurrentHashMap) config.asObject(fileBytes);
                LOG.info("Indexed documents loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read indexed documents from file");
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
                LOG.info("Unique document's id loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read unique document's id from file");
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
            LOG.warn("Can't save tracked files state");
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
            LOG.warn("Can't save tracked folders state");
        }
    }

    private void saveIndex() {
        try {
            Path filePath = Paths.get(APP_FOLDER + INDEX_FILE);
            FileOutputStream file = new FileOutputStream(APP_FOLDER + INDEX_FILE);
            ObjectOutputStream out = new ObjectOutputStream(file);

            // Method for serialization of object
            out.writeObject(index.getRoot());

            out.close();
            file.close();
            //TODO:: rewrite
//            byte[] objectBytes = config.asByteArray(index.getRoot());
//            Files.write(filePath, objectBytes);
        } catch (IOException e) {
            LOG.warn("Can't save index state", e);
        }
    }

    private void saveIndexedDocuments() {
        try {
            Path filePath = Paths.get(APP_FOLDER + INDEXED_DOCUMENTS_FILE);
            byte[] objectBytes = config.asByteArray(indexedDocuments);
            Files.write(filePath, objectBytes);
        } catch (IOException e) {
            LOG.warn("Can't save indexed documents state");
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
