package org.search.app;

import com.atlascopco.hunspell.Hunspell;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

public class HunspellCheck {

    public static void main(String[] args) throws URISyntaxException {
        URL resource = HunspellCheck.class.getResource("/index.dic");
        URL resource2 = HunspellCheck.class.getResource("/index.aff");
        System.out.println(resource);
        System.out.println(resource2);
//        "/home/mmazurkevich/Projects/search-engine/app/src/main/resources/index.dic"
//        "/home/mmazurkevich/Projects/search-engine/app/src/main/resources/index.aff"
        Hunspell speller = new Hunspell(Paths.get(resource.toURI()).toString(),
                Paths.get(resource2.toURI()).toString());
        String wordToCheck = "ditc"; // the word that you want to check
        if (!speller.spell(wordToCheck) ) {
            List<String> suggestions = speller.suggest(wordToCheck);
            System.out.println(suggestions);
        }
    }
}
