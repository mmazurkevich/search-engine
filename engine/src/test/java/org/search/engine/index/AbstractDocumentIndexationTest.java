package org.search.engine.index;

import org.search.engine.tree.SearchEngineTree;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

abstract class AbstractDocumentIndexationTest {

    final int documentId = 1;
    final String fileTitle = "/TestFileOne.txt";
    final String folderTitle = "/testFolder";
    final String searchQuery = "surfeits";
    Path filePath;
    Map<Path, Document> indexedDocuments;
    SearchEngineTree index;
}
