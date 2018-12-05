package org.search.engine.exception;

public class SearchEngineInitializationException extends RuntimeException {

    public SearchEngineInitializationException() {
    }

    public SearchEngineInitializationException(String message) {
        super(message);
    }
}
