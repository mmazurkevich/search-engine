package org.search.engine.analyzer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class splits content by punctuation marks if it meets this symbol, then token will end up
 */
public class StandardTokenizer implements Tokenizer {

    private static final Set<Character> punctuationMarks = Stream.of('.', ',', '!', '?', ':', ';', '"', '\'', '(', ')',
            '[', ']', '/', '-', '“', '”', ' ', '<', '>', '{', '}', '+', '*', '^', '#', '~', '%', '$', '@')
            .collect(Collectors.toCollection(HashSet::new));

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> tokenize(String content) {
        if (content != null && !content.isEmpty()) {
            List<String> tokens = new ArrayList<>();
            StringBuilder token = new StringBuilder();
            boolean isToken = false;
            for (int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);
                if (isPunctuationMark(ch) && isToken) {
                    tokens.add(token.toString());
                    token = new StringBuilder();
                    isToken = false;
                } else if (!isPunctuationMark(ch)) {
                    token.append(ch);
                    if (!isToken) {
                        isToken = true;
                    }
                }
            }
            if (token.length() > 0) {
                tokens.add(token.toString());
            }
            return tokens;
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isPunctuationMark(char ch) {
        return punctuationMarks.contains(ch);
    }
}
