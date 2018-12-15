package org.search.engine.index;

class DocumentLine {

    private int documentId;
    private String documentRow;

    DocumentLine(int documentId, String documentRow) {
        this.documentId = documentId;
        this.documentRow = documentRow;
    }

    int getDocumentId() {
        return documentId;
    }

    String getDocumentRow() {
        return documentRow;
    }
}
