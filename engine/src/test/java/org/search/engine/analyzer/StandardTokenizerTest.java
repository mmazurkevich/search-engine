package org.search.engine.analyzer;

import org.junit.Before;
import org.junit.Test;
import org.search.engine.model.Token;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class StandardTokenizerTest {

    private StandardTokenizer tokenizer;

    @Before
    public void setUp() {
        tokenizer = new StandardTokenizer();
    }

    @Test
    public void testSimpleContentTokenize() {
        String content = "I'am \" ? test, !  ,  example string. ";
        String expectedTokens = "[I, am, test, example, string]";
        List<Token> tokens = tokenizer.tokenize(content);
        assertNotNull(tokens);
        assertEquals(5, tokens.size());
        assertEquals(expectedTokens, tokens.stream().map(Token::getContent).collect(Collectors.toList()).toString());
    }

    @Test
    public void testEmptyContentTokenize() {
        String content = "";
        List<Token> tokens = tokenizer.tokenize(content);
        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }

    @Test
    public void testNullContentTokenize() {
        List<Token> tokens = tokenizer.tokenize(null);
        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }
}