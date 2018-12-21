package org.search.engine.search;

import org.search.engine.analyzer.Tokenizer;
import org.search.engine.model.Document;
import org.search.engine.model.SearchResult;
import org.search.engine.tree.SearchEngineTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple search manager which search by single word and return matched results
 * by mapping them in indexed documents list.
 */
public class SimpleSearchManager implements SearchManager{

    private static final Logger LOG = LoggerFactory.getLogger(SimpleSearchManager.class);

    private final SearchEngineTree index;
    private final Map<Path, Document> indexedDocuments;
    private final Tokenizer tokenizer;


    public SimpleSearchManager(SearchEngineTree index,  Map<Path, Document> indexedDocuments, Tokenizer tokenizer) {
        this.index = index;
        this.indexedDocuments = indexedDocuments;
        this.tokenizer = tokenizer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SearchResult> searchByQuery(String searchQuery) {
        if (searchQuery != null && !searchQuery.isEmpty()) {
            LOG.debug("Searching documents by query: {}", searchQuery);
            Set<Integer> value = index.getValue(searchQuery);
            if (!value.isEmpty()) {
                List<SearchResult> results = indexedDocuments.entrySet().stream()
                        .filter(entry -> value.contains(entry.getValue().getId()))
                        .map(entry -> {
                            Path filePath = entry.getValue().getPath();
                            int[] rowNumber = {0};
                            List<Integer> matchedRows = new ArrayList<>();
                            try (Stream<String> lines = Files.lines(filePath)) {
                                lines.forEach(line -> {
                                    rowNumber[0]++;
                                    if (tokenizer.tokenize(line).contains(searchQuery)) {
                                        matchedRows.add(rowNumber[0]);
                                    }
                                });
                            } catch (IOException ex) {
                                LOG.warn("Finding matched rows in file : {} finished with exception", filePath, ex);
                            }
                            return new SearchResult(filePath.toAbsolutePath().toString(), matchedRows);
                        }).collect(Collectors.toList());
                LOG.debug("Found documents: {}", results.size());
                return results;
            }
        }
        return Collections.emptyList();
    }

}
