package org.search.engine.analyzer;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

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
        List<String> tokens = tokenizer.tokenize(content);
        assertNotNull(tokens);
        assertEquals(5, tokens.size());
        assertEquals(expectedTokens, tokens.toString());
    }

    @Test
    public void testEmptyContentTokenize() {
        String content = "";
        List<String> tokens = tokenizer.tokenize(content);
        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }

    @Test
    public void testNullContentTokenize() {
        List<String> tokens = tokenizer.tokenize(null);
        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }
}