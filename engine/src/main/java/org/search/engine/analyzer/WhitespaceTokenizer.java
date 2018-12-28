package org.search.engine.analyzer;

import org.search.engine.model.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One of the simplest tokenizers which end up the word then meet the white
 * space symbol
 */
public class WhitespaceTokenizer implements Tokenizer {

    private static final char WHITE_SPACE = ' ';

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Token> tokenize(String content) {
        if (content != null && !content.isEmpty()) {
            List<Token> tokens = new ArrayList<>();
            StringBuilder token = new StringBuilder();
            boolean isToken = false;
            int position = 0;
            for (int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);
                if (ch == WHITE_SPACE && isToken) {
                    tokens.add(new Token(token.toString(), position));
                    token = new StringBuilder();
                    isToken = false;
                } else if (ch != WHITE_SPACE) {
                    token.append(ch);
                    if (!isToken) {
                        isToken = true;
                        position = i;
                    }
                }
            }
            if (token.length() > 0) {
                tokens.add(new Token(token.toString(), position));
            }
            return tokens;
        } else {
            return Collections.emptyList();
        }
    }

}
