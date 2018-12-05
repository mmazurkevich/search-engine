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
package org.search.engine.tree.radix.node.concrete.charsequence;

import org.search.engine.tree.Node;
import org.search.engine.tree.TreeNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * @author Niall Gallagher
 */
public class DefaultCharSequenceNodeTest {

    @Test
    public void testToString() throws Exception {
        Node node = new TreeNode("FOO", null, Collections.<Node>emptyList());
        Assert.assertEquals("Node{edge=FOO, value=null, edges=[]}", node.toString());

    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateOutgoingEdge_NonExistentEdge() throws Exception {
        Node node = new TreeNode("FOO", null, Collections.<Node>emptyList());
        node.updateOutgoingEdge(new TreeNode("BAR", null, Collections.<Node>emptyList()));
    }
}
