/**
 * Copyright 2012-2013 Niall Gallagher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.search.engine.tree.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Niall Gallagher
 */
public class CharSequencesTest {

    @Test
    public void testGetCommonPrefix() throws Exception {
        Assert.assertEquals("BAN", CharSequencesUtil.getCommonPrefix("BANANA", "BANDANA"));
        Assert.assertEquals("BAN", CharSequencesUtil.getCommonPrefix("BAN", "BANDANA"));
        Assert.assertEquals("BAN", CharSequencesUtil.getCommonPrefix("BANANA", "BAN"));
        Assert.assertEquals("", CharSequencesUtil.getCommonPrefix("BANANA", "ABANANA"));
        Assert.assertEquals("", CharSequencesUtil.getCommonPrefix("", "BANANA"));
        Assert.assertEquals("", CharSequencesUtil.getCommonPrefix("BANANA", ""));
        Assert.assertEquals("T", CharSequencesUtil.getCommonPrefix("TOAST", "TEAM"));
    }

    @Test
    public void testSubtractPrefix() throws Exception {
        Assert.assertEquals("ANA", CharSequencesUtil.subtractPrefix("BANANA", "BAN"));
        Assert.assertEquals("", CharSequencesUtil.subtractPrefix("BANANA", "BANANA"));
        Assert.assertEquals("", CharSequencesUtil.subtractPrefix("BANANA", "BANANAS"));
        Assert.assertEquals("BANANA", CharSequencesUtil.subtractPrefix("BANANA", ""));
        Assert.assertEquals("", CharSequencesUtil.subtractPrefix("", "BANANAS"));
    }

    @Test
    public void testConcatenate() throws Exception {
        CharSequence first = "APPLE";
        CharSequence second = "ORANGE";
        CharSequence concatenated = CharSequencesUtil.concatenate(first, second);
        Assert.assertEquals("APPLEORANGE", new StringBuilder().append(concatenated).toString());
    }

    @Test
    public void testToString_NullArgument() {
        //noinspection NullableProblems
        Assert.assertNull(CharSequencesUtil.toString(null));
    }

    @Test
    public void testConstructor() {
        Assert.assertNotNull(new CharSequencesUtil());
    }
}
