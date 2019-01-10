package org.search.engine.search;

import io.reactivex.subjects.ReplaySubject;
import org.search.engine.analyzer.Tokenizer;
import org.search.engine.model.Document;
import org.search.engine.model.EventType;
import org.search.engine.model.SearchResultEvent;
import org.search.engine.model.SearchType;
import org.search.engine.tree.SearchEngineTree;
import org.search.engine.tree.SearchTreeTrackChangesListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple search manager which search by single word and return matched results
 * by mapping them in indexed documents list.
 */
public class SimpleSearchManager implements SearchManager, SearchTreeTrackChangesListener {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleSearchManager.class);

    private final SearchEngineTree index;
    private final Map<Path, Document> indexedDocuments;
    private final Tokenizer tokenizer;
    private List<DocumentMatchedRows> documentMatchedRowsList;
    private ReplaySubject<SearchResultEvent> subject;
    private String trackedLexeme;
    private SearchType trackedSearchType;

    public SimpleSearchManager(SearchEngineTree index, Map<Path, Document> indexedDocuments, Tokenizer tokenizer) {
        this.index = index;
        this.indexedDocuments = indexedDocuments;
        this.tokenizer = tokenizer;
        index.setTrackChangesListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReplaySubject<SearchResultEvent> searchByQuery(String searchQuery, SearchType searchType) {
        if (subject != null) {
            subject.onComplete();
        }
        trackedLexeme = searchQuery;
        trackedSearchType = searchType;
        documentMatchedRowsList = new ArrayList<>();
        if (searchQuery != null && !searchQuery.isEmpty()) {
            LOG.debug("Searching documents by query: {} with search type: {}", searchQuery, searchType);
            Set<Integer> value = index.getValue(searchQuery);
            if (!value.isEmpty()) {
                documentMatchedRowsList = indexedDocuments.entrySet().stream()
                        .filter(entry -> value.contains(entry.getValue().getId()))
                        .filter(entry -> Files.exists(entry.getValue().getPath()))
                        .map(entry -> getDocumentMatchedRows(entry.getValue().getId(), entry.getValue().getPath()))
                        .limit(100)
                        .collect(Collectors.toList());
                LOG.debug("Found documents: {}", documentMatchedRowsList.size());
            }
        }

        subject = ReplaySubject.create();
        documentMatchedRowsList.forEach(it -> {
            String fileName = it.getFileName().toAbsolutePath().toString();
            for (Map.Entry<Integer, List<Integer>> row : it.getRowNumbers().entrySet()) {
                subject.onNext(new SearchResultEvent(fileName, row.getKey(), row.getValue(), EventType.ADD));
            }
        });
        return subject;
    }

    @Override
    public String getTrackedLexeme() {
        if (trackedLexeme == null)
            return "";
        return trackedLexeme;
    }

    @Override
    public SearchType getTrackedSearchType() {
        if (trackedSearchType == null)
            return SearchType.EXACT_MATCH;
        return trackedSearchType;
    }

    @Override
    public void onTrackedLexemeAdd(int documentId) {
        if (documentMatchedRowsList.size() < 100) {
            Optional<Document> optional = indexedDocuments.entrySet().stream()
                    .filter(entry -> entry.getValue().getId() == documentId)
                    .map(Map.Entry::getValue)
                    .findFirst();
            if (optional.isPresent()) {
                Document document = optional.get();
                String fileName = document.getPath().toAbsolutePath().toString();
                DocumentMatchedRows documentMatchedRows = getDocumentMatchedRows(document.getId(), document.getPath());
                for (Map.Entry<Integer, List<Integer>> row : documentMatchedRows.getRowNumbers().entrySet()) {
                    subject.onNext(new SearchResultEvent(fileName, row.getKey(), row.getValue(), EventType.ADD));
                }
                documentMatchedRowsList.add(documentMatchedRows);
                LOG.debug("Handled add tracked lexeme to document: {}", fileName);
            }
        }
    }

    @Override
    public void onTrackedLexemeUpdated(int documentId) {
        Optional<DocumentMatchedRows> optional = documentMatchedRowsList.stream()
                .filter(it -> it.getDocumentId() == documentId)
                .findFirst();
        if (optional.isPresent()) {
            DocumentMatchedRows oldMatchedRows = optional.get();
            String fileName = oldMatchedRows.getFileName().toAbsolutePath().toString();
            Map<Integer, List<Integer>> oldRowNumbers = oldMatchedRows.getRowNumbers();
            DocumentMatchedRows newMatchedRows = getDocumentMatchedRows(oldMatchedRows.getDocumentId(), oldMatchedRows.getFileName());
            newMatchedRows.getRowNumbers().forEach((key, value) -> {
                if (!oldRowNumbers.containsKey(key)) {
                    //Add new matched rows to the results
                    subject.onNext(new SearchResultEvent(fileName, key, value, EventType.ADD));
                } else {
                    //Update old rows with new positions
                    subject.onNext(new SearchResultEvent(fileName, key, value, EventType.UPDATE));
                    oldRowNumbers.remove(key);
                }
            });
            //Remove old matched rows
            for (Map.Entry<Integer, List<Integer>> row : oldRowNumbers.entrySet()) {
                subject.onNext(new SearchResultEvent(fileName, row.getKey(), row.getValue(), EventType.REMOVE));
            }
            oldMatchedRows.setRowNumbers(newMatchedRows.getRowNumbers());
            LOG.debug("Handled update tracked lexeme in the document: {}", fileName);
        }
    }

    @Override
    public void onTrackedLexemeRemoved(int documentId) {
        Optional<DocumentMatchedRows> optional = documentMatchedRowsList.stream()
                .filter(it -> it.getDocumentId() == documentId)
                .findFirst();
        if (optional.isPresent()) {
            DocumentMatchedRows documentMatchedRows = optional.get();
            String fileName = documentMatchedRows.getFileName().toAbsolutePath().toString();
            for (Map.Entry<Integer, List<Integer>> row : documentMatchedRows.getRowNumbers().entrySet()) {
                subject.onNext(new SearchResultEvent(fileName, row.getKey(), row.getValue(), EventType.REMOVE));
            }
            documentMatchedRowsList.remove(documentMatchedRows);
            LOG.debug("Handled remove tracked lexeme from document: {}", documentMatchedRows.getFileName());
        }
    }

    private DocumentMatchedRows getDocumentMatchedRows(int documentId, Path filePath) {
        int[] rowNumber = {0};
        Map<Integer, List<Integer>> matchedRows = new LinkedHashMap<>();
        try (Stream<String> lines = Files.lines(filePath)) {
            lines.forEach(line -> {
                rowNumber[0]++;
                List<Integer> positionsInRow = new ArrayList<>();
                tokenizer.tokenize(line).forEach(token -> {
                    if (trackedSearchType == SearchType.EXACT_MATCH && token.getContent().equals(trackedLexeme)) {
                        positionsInRow.add(token.getPositionInRow());
                    } else if (trackedSearchType == SearchType.START_WITH && token.getContent().startsWith(trackedLexeme)) {
                        positionsInRow.add(token.getPositionInRow());
                    }
                });
                if (!positionsInRow.isEmpty()) {
                    matchedRows.put(rowNumber[0], positionsInRow);
                }
            });
        } catch (IOException | UncheckedIOException ex) {
            LOG.warn("Finding matched rows in file : {} finished with exception", filePath);
        }
        return new DocumentMatchedRows(documentId, filePath, matchedRows);
    }

    private class DocumentMatchedRows {

        private int documentId;
        private Path fileName;
        private Map<Integer, List<Integer>> rowNumbers;

        private DocumentMatchedRows(int documentId, Path fileName, Map<Integer, List<Integer>> rowNumbers) {
            this.documentId = documentId;
            this.fileName = fileName;
            this.rowNumbers = rowNumbers;
        }

        public int getDocumentId() {
            return documentId;
        }

        private Path getFileName() {
            return fileName;
        }

        private Map<Integer, List<Integer>> getRowNumbers() {
            return rowNumbers;
        }

        public void setRowNumbers(Map<Integer, List<Integer>> rowNumbers) {
            this.rowNumbers = rowNumbers;
        }
    }
}