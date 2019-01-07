package org.search.app;

import com.atlascopco.hunspell.Hunspell;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class HunspellCheck {

    private final Hunspell hunspell;

    public HunspellCheck() throws URISyntaxException {
        URL dict = HunspellCheck.class.getResource("/index.dic");
        URL affix = HunspellCheck.class.getResource("/index.aff");
        hunspell = new Hunspell(Paths.get(dict.toURI()).toString(),
                Paths.get(affix.toURI()).toString());
    }

    public List<String> getSuggestions(String wordToCheck) {
        if (!hunspell.spell(wordToCheck)) {
             return hunspell.suggest(wordToCheck);
        }
        return Collections.emptyList();
    }

}
