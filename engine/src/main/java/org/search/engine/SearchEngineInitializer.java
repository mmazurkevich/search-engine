package org.search.engine;

import org.nustaq.serialization.FSTConfiguration;
import org.search.engine.index.IndexationEventListener;
import org.search.engine.model.Document;
import org.search.engine.model.IndexChanges;
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
import java.util.stream.Stream;

public class SearchEngineInitializer implements IndexationEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngineInitializer.class);

    private static final FSTConfiguration config = FSTConfiguration.createDefaultConfiguration();
    private static final String APP_FOLDER = System.getProperty("user.home") + "/.search-engine";
    private static final String TRACKED_FILES_FILE = "/files.se";
    private static final String TRACKED_FOLDERS_FILE = "/folders.se";
    private static final String INDEX_FILE = "/index.se";
    private static final String INDEXED_DOCUMENTS_FILE = "/documents.se";

    //Unique concurrent document Id generator
    private AtomicInteger uniqueDocumentId;
    private Map<Path, Document> indexedDocuments;
    private SearchEngineTree index;
    // Files which changes tracked by system and were registered in the system by track. CopyOnWriteArrayList
    // used because of possibility of concurrent changes came from watch service and by user itself
    private Set<Path> trackedFiles;
    // Folders which changes tracked by system and were registered in the system by track.
    private Set<Path> trackedFolders;
    private IndexChanges indexChanges;


    SearchEngineInitializer() {
        Path folderPath = Paths.get(APP_FOLDER);
        if (!Files.exists(folderPath)) {
            try {
                Files.createDirectory(folderPath);
            } catch (IOException ex) {
                LOG.warn("Can't create app folder engine work without saving");
            }
        }
        config.registerClass(TreeNode.class, String.class, AtomicInteger.class, HashMap.class);
        uniqueDocumentId = new AtomicInteger();
        indexedDocuments = new ConcurrentHashMap<>();
        index = new SearchEngineConcurrentTree();
        trackedFiles = ConcurrentHashMap.newKeySet();
        trackedFolders = ConcurrentHashMap.newKeySet();
        loadIndex(true);
    }

    @Override
    public void onIndexationFinished() {
        SearchEngineExecutors.getExecutorService().execute(() -> {
            saveTrackedFiles();
            saveTrackedFolders();
            saveIndex();
            saveIndexedDocuments();
            LOG.info("Search engine saving cache finished");
        });
    }

    @Override
    public void onIndexationProgress(int progress) { }

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

    IndexChanges getIndexChanges() {
        return indexChanges;
    }

    void invalidateCache() {
        Stream.of(Paths.get(APP_FOLDER + TRACKED_FILES_FILE), Paths.get(APP_FOLDER + TRACKED_FOLDERS_FILE),
                Paths.get(APP_FOLDER + INDEX_FILE), Paths.get(APP_FOLDER + INDEXED_DOCUMENTS_FILE))
                .forEach(this::removeFileIfExist);

        LOG.info("Cache invalidated");
    }

    public void loadIndex(boolean calculateChanges) {
        if (!initializeTrackedFiles() || !initializeTrackedFolders() || !initializeIndex()
                || !initializeIndexedDocuments()) {
            uniqueDocumentId.set(0);
            indexedDocuments.clear();
            index.clear();
            trackedFiles.clear();
            trackedFolders.clear();
            LOG.info("Initialized empty engine");
        } else {
            if (calculateChanges) {
                calculateIndexChanges();
            }
            LOG.info("Engine loaded from cache");
        }
    }

    private void calculateIndexChanges() {
        LOG.info("Start calculating index changes");
        Set<Path> oldFolders = new HashSet<>();
        Set<Path> newFolders = new HashSet<>();

        Set<Path> checkedFiles = new HashSet<>();
        Set<Path> newFiles = new HashSet<>();
        Set<Path> oldFiles = new HashSet<>();
        Set<Path> changedFiles = new HashSet<>();

        trackedFiles.forEach(file -> {
            if (!Files.exists(file)) {
                oldFiles.add(file);
            } else {
                try {
                    checkedFiles.add(file);
                    Document document = indexedDocuments.get(file);
                    if (document != null && Files.getLastModifiedTime(file).toMillis() > document.getModificationTimestamp()) {
                        changedFiles.add(file);
                    }
                } catch (IOException ex) {
                    LOG.warn("Exception in calculation file changes: {}", file, ex);
                }
            }
        });


        trackedFolders.forEach(folder -> {
            if (!Files.exists(folder)) {
                oldFolders.add(folder);
            } else {
                try (Stream<Path> paths = Files.walk(folder)) {
                    paths.forEach(path -> {
                        if (Files.isRegularFile(path)) {
                            checkedFiles.add(path);

                            Document document = indexedDocuments.get(path);
                            if (document != null) {
                                try {
                                    if (Files.getLastModifiedTime(path).toMillis() > document.getModificationTimestamp()) {
                                        changedFiles.add(path);
                                    }
                                } catch (IOException ex) {
                                    LOG.warn("Exception in calculation file changes: {}", path, ex);
                                }
                            } else {
                                newFiles.add(path);
                            }

                        }
                        if (Files.isDirectory(path)) {
                            if (!trackedFolders.contains(path)) {
                                newFolders.add(path);
                            }
                        }
                    });
                } catch (IOException ex) {
                    LOG.warn("Exception in walking through the folder", ex);
                }
            }
        });

        Set<Path> indexedFiles = new HashSet<>(indexedDocuments.keySet());
        indexedFiles.removeAll(checkedFiles);
        oldFiles.addAll(indexedFiles);
        indexChanges = new IndexChanges(oldFolders, newFolders, newFiles, oldFiles, changedFiles);
        LOG.info("Finish calculating index changes");
    }

    private boolean initializeTrackedFiles() {
        Path filePath = Paths.get(APP_FOLDER + TRACKED_FILES_FILE);
        if (Files.exists(filePath)) {
            try {
                byte[] fileBytes = Files.readAllBytes(filePath);
                List<String> paths = (ArrayList<String>) config.asObject(fileBytes);
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
                index.setRoot(root);
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

                int maxId = 0;
                for (Map.Entry<String, SerializableDocument> entry : documents.entrySet()) {
                    SerializableDocument document = entry.getValue();
                    Path path = Paths.get(entry.getKey());
                    indexedDocuments.put(path, new Document(document.getId(), document.isTracked(), path, document.getModificationTimestamp()));
                    if (document.getId() > maxId) {
                        maxId = document.getId();
                    }
                }
                uniqueDocumentId.set(maxId);
                LOG.info("IndexedDocuments loaded from file");
                return true;
            } catch (IOException | ClassCastException e) {
                LOG.warn("Can't read indexedDocuments from file", e);
                return false;
            }
        }
        return false;
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
            TreeNode root = index.getRoot();
            byte[] objectBytes = config.asByteArray(root);
            Files.write(filePath, objectBytes);
        } catch (IOException e) {
            LOG.warn("Can't save index state", e);
        }
    }

    private void saveIndexedDocuments() {
        try {
            Path filePath = Paths.get(APP_FOLDER + INDEXED_DOCUMENTS_FILE);
            Map<String, SerializableDocument> collection = new HashMap<>();
            indexedDocuments.forEach((key, value) -> collection.put(key.toAbsolutePath().toString(), new SerializableDocument(value.getId(), value.isTracked(),
                    value.getPath().toAbsolutePath().toString(), value.getModificationTimestamp())));
            byte[] objectBytes = config.asByteArray(collection);
            Files.write(filePath, objectBytes);
        } catch (IOException e) {
            LOG.warn("Can't save indexed documents state", e);
        }
    }

    private void removeFileIfExist(Path filePath) {
        if (Files.exists(filePath)) {
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                LOG.warn("Can't remove file {} during invalidation", filePath);
            }
        }
    }
}
