package org.search.engine.tree.util;

/**
 * @author Niall Gallagher
 */
public class CharSequencesUtil {

    /**
     * Private constructor, not used.
     */
    CharSequencesUtil() {
    }

    public static CharSequence getCommonPrefix(CharSequence first, CharSequence second) {
        int minLength = Math.min(first.length(), second.length());
        for (int i = 0; i < minLength; i++) {
            if (first.charAt(i) != second.charAt(i)) {
                return first.subSequence(0, i);
            }
        }
        return first.subSequence(0, minLength);
    }

    public static CharSequence subtractPrefix(CharSequence main, CharSequence prefix) {
        int startIndex = prefix.length();
        int mainLength = main.length();
        if (startIndex > mainLength) {
            return "";
        }
        return main.subSequence(startIndex, mainLength);
    }

    public static CharSequence concatenate(final CharSequence first, final CharSequence second) {
        return new StringBuilder().append(first).append(second);
    }

    public static String toString(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        if (charSequence instanceof String) {
            return (String)charSequence;
        }
        return new StringBuilder(charSequence.length()).append(charSequence).toString();
    }
}
