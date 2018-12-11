package org.search.engine.index;

import org.search.engine.tree.SearchEngineTree;

import java.nio.file.Path;
import java.util.List;

abstract class AbstractDocumentIndexationTest {

    final int documentId = 1;
    final String fileTitle = "/TestFileOne.txt";
    final String folderTitle = "/testFolder";
    final String searchQuery = "surfeits";
    Path filePath;
    List<Document> indexedDocuments;
    SearchEngineTree index;
}
