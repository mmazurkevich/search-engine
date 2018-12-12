package org.search.engine.exception;

/**
 * Exceptions which signals that lib can't startup due to some reasons.
 * The main case for now it's exceptions during WatchService initialization,
 * which is the case that system can't track file/folder changes
 */
public class SearchEngineInitializationException extends RuntimeException {

    public SearchEngineInitializationException() {
    }

    public SearchEngineInitializationException(String message) {
        super(message);
    }
}
