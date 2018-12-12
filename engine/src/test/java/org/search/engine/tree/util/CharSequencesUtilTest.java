package org.search.engine.tree.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CharSequencesUtilTest {

    @Test
    public void testGetCommonPrefix() {
        assertEquals("BAN", CharSequencesUtil.getCommonPrefix("BANANA", "BANDANA"));
        assertEquals("BAN", CharSequencesUtil.getCommonPrefix("BAN", "BANDANA"));
        assertEquals("BAN", CharSequencesUtil.getCommonPrefix("BANANA", "BAN"));
        assertEquals("", CharSequencesUtil.getCommonPrefix("BANANA", "ABANANA"));
        assertEquals("", CharSequencesUtil.getCommonPrefix("", "BANANA"));
        assertEquals("", CharSequencesUtil.getCommonPrefix("BANANA", ""));
        assertEquals("T", CharSequencesUtil.getCommonPrefix("TOAST", "TEAM"));
    }

    @Test
    public void testSubtractPrefix() {
        assertEquals("ANA", CharSequencesUtil.subtractPrefix("BANANA", "BAN"));
        assertEquals("", CharSequencesUtil.subtractPrefix("BANANA", "BANANA"));
        assertEquals("", CharSequencesUtil.subtractPrefix("BANANA", "BANANAS"));
        assertEquals("BANANA", CharSequencesUtil.subtractPrefix("BANANA", ""));
        assertEquals("", CharSequencesUtil.subtractPrefix("", "BANANAS"));
    }

    @Test
    public void testConcatenate() {
        CharSequence first = "APPLE";
        CharSequence second = "ORANGE";
        CharSequence concatenated = CharSequencesUtil.concatenate(first, second);
        assertEquals("APPLEORANGE", new StringBuilder().append(concatenated).toString());
    }

}
