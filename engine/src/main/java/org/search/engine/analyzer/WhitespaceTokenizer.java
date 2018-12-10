package org.search.engine.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WhitespaceTokenizer implements Tokenizer{

    private static final char WHITE_SPACE = ' ';

    @Override
    public List<String> tokenize(String content) {
        if (content != null && !content.isEmpty()) {
            List<String> tokens = new ArrayList<>();
            StringBuilder token = new StringBuilder();
            boolean isToken = false;
            for(int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);
                if (ch == WHITE_SPACE && isToken) {
                    tokens.add(token.toString());
                    token = new StringBuilder();
                    isToken = false;
                } else if (ch != WHITE_SPACE) {
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

}
