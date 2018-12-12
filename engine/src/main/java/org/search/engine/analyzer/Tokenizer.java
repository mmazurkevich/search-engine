package org.search.engine.analyzer;

import java.util.List;

/**
 * API for classes which are responsible for splitting incoming content to
 * the separate leksems
 */
public interface Tokenizer {

    /**
     * Method which split incoming string to the certain tokens by the entire rules
     *
     * @param content The string which should be divided by the certain rules
     * @return The list of splitted tokens
     */
    List<String> tokenize(String content);
}
