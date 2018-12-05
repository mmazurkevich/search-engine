package org.search.engine.analyzer;

import java.util.List;

public interface Tokenizer {

    List<String> tokenize(String content);
}
