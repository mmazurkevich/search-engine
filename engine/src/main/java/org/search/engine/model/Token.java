package org.search.engine.model;

public class Token {

    private String content;
    private int positionInRow;

    public Token(String content, int positionInRow) {
        this.content = content;
        this.positionInRow = positionInRow;
    }

    public String getContent() {
        return content;
    }

    public int getPositionInRow() {
        return positionInRow;
    }
}
