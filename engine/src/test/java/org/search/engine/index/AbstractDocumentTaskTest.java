package org.search.engine.index;

import org.search.engine.tree.SearchEngineTree;

import java.nio.file.Path;
import java.util.List;

abstract class AbstractDocumentTaskTest {

    final int documentId = 1;
    final String fileTitle = "/TestFileOne.txt";
    Path filePath;
    List<Document> indexedDocuments;
    SearchEngineTree index;
}
