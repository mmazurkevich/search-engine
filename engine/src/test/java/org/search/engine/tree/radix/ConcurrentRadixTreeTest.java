///**
// * Copyright 2012-2013 Niall Gallagher
// * <p>
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.search.engine.tree.radix;
//
//import org.junit.Test;
//import org.search.engine.tree.util//import org.search.engine.tree.util//import org.search.engine.tree.Node;
//import org.search.engine.tree.radix.node.concrete.charsequence.*;
//import org.search.engine.tree.utilort org.search.engine.tree.PrettyPrintableorg.search.engine.tree.testutil.TestUtility;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.Assert.*;
//
///**
// * @author Niall Gallagher
// */
//public class ConcurrentRadixTreeTest {
//
//    @Test
//    public void testBuildTreeByHand() {
//        // Build the tree by hand, as if the following strings were added: B, BA, BAN, BANDANA, BANAN, BANANA
//
//        //    ○
//        //    └── ○ B (1)
//        //        └── ○ A (2)
//        //            └── ○ N (3)
//        //                ├── ○ AN (5)
//        //                │   └── ○ A (6)
//        //                └── ○ DANA (4)
//
//        final Node root, n1, n2, n3, n4, n5, n6;
//        n6 = createNode("A", 6, Collections.<Node>emptyList(), false);
//        n5 = createNode("AN", 5, Arrays.asList(n6), false);
//        n4 = createNode("DANA", 4, Collections.<Node>emptyList(), false);
//        n3 = createNode("N", 3, Arrays.asList(n4, n5), false); // note: it should sort alphabetically such that n5 is first
//        n2 = createNode("A", 2, Arrays.asList(n3), false);
//        n1 = createNode("B", 1, Arrays.asList(n2), false);
//        //noinspection NullableProblems
//        root = createNode("", null, Arrays.asList(n1), true);
//
//        String expected =
//                "○\n" +
//                        "└── ○ B (1)\n" +
//                        "    └── ○ A (2)\n" +
//                        "        └── ○ N (3)\n" +
//                        "            ├── ○ AN (5)\n" +
//                        "            │   └── ○ A (6)\n" +
//                        "            └── ○ DANA (4)\n";
//
//        String actual = PrettyPrinter.prettyPrint(wrapNodeForPrinting(root));
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testPut_AddToRoot() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("A", 1);
//        String expected =
//                "○\n" +
//                        "└── ○ A (1)\n";
//        String actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testPut_ChildNodeSorting() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("B", 1);
//        tree.put("A", 2);
//        String expected =
//                "○\n" +
//                        "├── ○ A (2)\n" +
//                        "└── ○ B (1)\n";
//        String actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testPut_AppendChild() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOO", 1);
//        tree.put("FOOBAR", 2);
//
//        String expected =
//                "○\n" +
//                        "└── ○ FOO (1)\n" +
//                        "    └── ○ BAR (2)\n";
//        String actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testPut_SplitEdge() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOOBAR", 1);
//        tree.put("FOO", 2);
//
//        String expected =
//                "○\n" +
//                        "└── ○ FOO (2)\n" +
//                        "    └── ○ BAR (1)\n";
//        String actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testPut_SplitWithImplicitNode() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOOBAR", 1);
//        tree.put("FOOD", 2);
//
//        String expected =
//                "○\n" +
//                        "└── ○ FOO\n" + // We never explicitly inserted FOO
//                        "    ├── ○ BAR (1)\n" +
//                        "    └── ○ D (2)\n";
//        String actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testPut_SplitAndMove() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("TEST", 1);
//        tree.put("TEAM", 2);
//        tree.put("TOAST", 3);
//
//        String expected =
//                "○\n" +
//                        "└── ○ T\n" +             // implicit node added automatically
//                        "    ├── ○ E\n" +         // implicit node added automatically
//                        "    │   ├── ○ AM (2)\n" +
//                        "    │   └── ○ ST (1)\n" +
//                        "    └── ○ OAST (3)\n";
//        String actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testPut_OverwriteValue() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//
//        Integer existing;
//        existing = tree.put("FOO", 1);
//        assertNull(existing);
//        existing = tree.put("FOO", 2);
//        assertNotNull(existing);
//
//        assertEquals(Integer.valueOf(1), existing);
//        assertEquals(Integer.valueOf(2), tree.getValueForExactKey("FOO"));
//    }
//
//    @Test
//    public void testPutIfAbsent_DoNotOverwriteValue() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//
//        Integer existing = tree.putIfAbsent("FOO", 1);
//        assertNull(existing);
//
//        existing = tree.putIfAbsent("FOO", 2);
//        assertNotNull(existing);
//
//        assertEquals(Integer.valueOf(1), existing);
//        assertEquals(Integer.valueOf(1), tree.getValueForExactKey("FOO"));
//    }
//
//    @Test
//    public void testPutIfAbsent_SplitNode() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//
//        //    ○
//        //    └── ○ FOO             // implicit node added automatically
//        //        ├── ○ BAR (1)
//        //        └── ○ D (1)
//
//        Integer existing;
//        existing = tree.putIfAbsent("FOOBAR", 1);
//        assertNull(existing);
//        existing = tree.putIfAbsent("FOOD", 1);
//        assertNull(existing);
//
//        // This tests 'overwrite' set to true and exact match for node,
//        // but no existing value to return (i.e. implicit node above)...
//
//        //    ○
//        //    └── ○ FOO (2)
//        //        ├── ○ BAR (1)
//        //        └── ○ D (1)
//
//        existing = tree.putIfAbsent("FOO", 2);
//        assertNull(existing);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testPutInternal_ArgumentValidation1() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        //noinspection NullableProblems
//        tree.put(null, 1);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testPutInternal_ArgumentValidation2() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        //noinspection NullableProblems
//        tree.put("FOO", null);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testPutInternal_ArgumentValidation3() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        //noinspection NullableProblems
//        tree.put("", 1);
//    }
//
//    @Test(expected = IllegalStateException.class)
//    public void testPutInternal_InvalidClassification() {
//        // Create a dummy subclass of SearchResult which returns an INVALID classification...
//        class InvalidSearchResult extends ConcurrentRadixTree.SearchResult {
//            InvalidSearchResult(CharSequence key, Node nodeFound, int charsMatched, int charsMatchedInNodeFound, Node parentNode, Node parentNodesParent) {
//                super(key, nodeFound, charsMatched, charsMatchedInNodeFound, parentNode, parentNodesParent);
//            }
//
//            @Override
//            protected Classification classify(CharSequence key, Node nodeFound, int charsMatched, int charsMatchedInNodeFound) {
//                return Classification.INVALID;
//            }
//        }
//        // Override searchTree() to return the InvalidSearchResult...
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>() {
//            @Override
//            SearchResult searchTree(CharSequence key) {
//                return new InvalidSearchResult("FOO", root, 4, 4, null, null);
//
//            }
//        };
//        // We expect put() to throw an IllegalStateException
//        // when it encounters the unsupported INVALID classification...
//        tree.put("FOO", 1);
//    }
//
//    @Test
//    public void testSize() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        assertEquals(0, tree.size());
//        tree.put("TEST", 1);
//        assertEquals(1, tree.size());
//        tree.put("TEAM", 2);
//        assertEquals(2, tree.size());
//        tree.put("TOAST", 3);
//        assertEquals(3, tree.size());
//
//        tree.remove("FOO");
//        assertEquals(3, tree.size()); // no change
//        tree.remove("TOAST");
//        assertEquals(2, tree.size());
//        tree.remove("TEAM");
//        assertEquals(1, tree.size());
//        tree.remove("TEST");
//        assertEquals(0, tree.size());
//    }
//
//    @Test
//    public void testGet() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("TEST", 1);
//        tree.put("TEAM", 2);
//        tree.put("TOAST", 3);
//
//        //    ○
//        //    └── ○ T               // implicit node added automatically
//        //        ├── ○ E           // implicit node added automatically
//        //        │   ├── ○ AM (2)
//        //        │   └── ○ ST (1)
//        //        └── ○ OAST (3)
//
//        assertEquals(Integer.valueOf(1), tree.getValueForExactKey("TEST"));
//        assertEquals(Integer.valueOf(2), tree.getValueForExactKey("TEAM"));
//        assertEquals(Integer.valueOf(3), tree.getValueForExactKey("TOAST"));
//        assertNull(tree.getValueForExactKey("T"));
//        assertNull(tree.getValueForExactKey("TE"));
//        assertNull(tree.getValueForExactKey("E")); // sanity check, no such edge from root
//        assertNull(tree.getValueForExactKey("")); // sanity check, root never has a value
//    }
//
//    @Test
//    public void testRemove_MoreThanOneChildEdge() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOO", 1);
//        tree.put("FOOBAR", 2);
//        tree.put("FOOD", 3);
//
//        //    ○
//        //    └── ○ FOO (1)
//        //        ├── ○ BAR (2)
//        //        └── ○ D (3)
//
//        String expected, actual;
//        expected =
//                "○\n" +
//                        "└── ○ FOO (1)\n" +
//                        "    ├── ○ BAR (2)\n" +
//                        "    └── ○ D (3)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//
//        boolean removed = tree.remove("FOO");
//        assertTrue(removed);
//
//        //    ○
//        //    └── ○ FOO         // value removed from FOO, but node needs to stay (as implicit node)
//        //        ├── ○ BAR (2)
//        //        └── ○ D (3)
//
//        expected =
//                "○\n" +
//                        "└── ○ FOO\n" +
//                        "    ├── ○ BAR (2)\n" +
//                        "    └── ○ D (3)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testRemove_ExactlyOneChildEdge() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOO", 1);
//        tree.put("FOOBAR", 2);
//        tree.put("FOOBARBAZ", 3);
//
//        //    ○
//        //    └── ○ FOO (1)
//        //        └── ○ BAR (2)
//        //            └── ○ BAZ (3)
//
//
//        String expected, actual;
//        expected =
//                "○\n" +
//                        "└── ○ FOO (1)\n" +
//                        "    └── ○ BAR (2)\n" +
//                        "        └── ○ BAZ (3)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//
//        boolean removed = tree.remove("FOO");
//        assertTrue(removed);
//
//        //    ○
//        //    └── ○ FOOBAR (2)          // Edges FOO and BAR merged,
//        //        └── ○ BAZ (3)         // and the value and child edges from BAR also copied into merged node
//
//        expected =
//                "○\n" +
//                        "└── ○ FOOBAR (2)\n" +
//                        "    └── ○ BAZ (3)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testRemove_ZeroChildEdges_DirectChildOfRoot() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOO", 1);
//        tree.put("BAR", 2);
//
//        //    ○
//        //    ├── ○ BAR (2)
//        //    └── ○ FOO (1)
//
//        String expected, actual;
//        expected =
//                "○\n" +
//                        "├── ○ BAR (2)\n" +
//                        "└── ○ FOO (1)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//
//        boolean removed = tree.remove("FOO");
//        assertTrue(removed);
//
//        //    ○                 // FOO removed, which involved recreating the root to change its child edges
//        //    └── ○ BAR (2)
//
//        expected =
//                "○\n" +
//                        "└── ○ BAR (2)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testRemove_LastRemainingKey() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOO", 1);
//
//        //    ○
//        //    └── ○ FOO (1)
//
//        String expected, actual;
//        expected =
//                "○\n" +
//                        "└── ○ FOO (1)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//
//        boolean removed = tree.remove("FOO");
//        assertTrue(removed);
//
//        //    ○                 // FOO removed, which involved recreating the root with no remaining edges
//
//        expected =
//                "○\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testRemove_ZeroChildEdges_OneStepFromRoot() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOO", 1);
//        tree.put("FOOBAR", 2);
//
//        //    ○
//        //    └── ○ FOO (1)
//        //        └── ○ BAR (2)
//
//
//        String expected, actual;
//        expected =
//                "○\n" +
//                        "└── ○ FOO (1)\n" +
//                        "    └── ○ BAR (2)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//
//        boolean removed = tree.remove("FOOBAR");
//        assertTrue(removed);
//
//        //    ○
//        //    └── ○ FOO (1)     // BAR removed, which involved recreating FOO and re-adding it to root node
//
//        expected =
//                "○\n" +
//                        "└── ○ FOO (1)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testRemove_ZeroChildEdges_SeveralStepsFromRoot() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOO", 1);
//        tree.put("FOOBAR", 2);
//        tree.put("FOOBARBAZ", 3);
//
//        //    ○
//        //    └── ○ FOO (1)
//        //        └── ○ BAR (2)
//        //            └── ○ BAZ (3)
//
//
//        String expected, actual;
//        expected =
//                "○\n" +
//                        "└── ○ FOO (1)\n" +
//                        "    └── ○ BAR (2)\n" +
//                        "        └── ○ BAZ (3)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//
//        boolean removed = tree.remove("FOOBARBAZ");
//        assertTrue(removed);
//
//        //    ○
//        //    └── ○ FOO (1)
//        //        └── ○ BAR (2) // BAZ removed, which involved recreating BAR and re-adding it to its parent FOO
//
//        expected =
//                "○\n" +
//                        "└── ○ FOO (1)\n" +
//                        "    └── ○ BAR (2)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testRemove_DoNotRemoveSplitNode() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOOBAR", 1);
//        tree.put("FOOD", 2);
//
//        //    ○
//        //    └── ○ FOO             // implicit node added automatically
//        //        ├── ○ BAR (1)
//        //        └── ○ D (2)
//
//
//        String expected, actual;
//        expected =
//                "○\n" +
//                        "└── ○ FOO\n" +
//                        "    ├── ○ BAR (1)\n" +
//                        "    └── ○ D (2)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//
//        boolean removed = tree.remove("FOO");
//        assertFalse(removed);
//
//        expected =
//                "○\n" +
//                        "└── ○ FOO\n" +             // we expect no change
//                        "    ├── ○ BAR (1)\n" +
//                        "    └── ○ D (2)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testRemove_MergeSplitNode() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("TEST", 1);
//        tree.put("TEAM", 2);
//        tree.put("TOAST", 3);
//
//        String expected, actual;
//        expected =
//                "○\n" +
//                        "└── ○ T\n" +
//                        "    ├── ○ E\n" +
//                        "    │   ├── ○ AM (2)\n" +
//                        "    │   └── ○ ST (1)\n" +
//                        "    └── ○ OAST (3)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//
//        boolean removed = tree.remove("TEST");
//        assertTrue(removed);
//
//        expected =
//                "○\n" +
//                        "└── ○ T\n" +
//                        "    ├── ○ EAM (2)\n" +
//                        "    └── ○ OAST (3)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testRemove_DoNotMergeSplitNodeWithValue() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("TEST", 1);
//        tree.put("TEAM", 2);
//        tree.put("TOAST", 3);
//        tree.put("TE", 4);
//
//        String expected, actual;
//        expected =
//                "○\n" +
//                        "└── ○ T\n" +
//                        "    ├── ○ E (4)\n" +
//                        "    │   ├── ○ AM (2)\n" +
//                        "    │   └── ○ ST (1)\n" +
//                        "    └── ○ OAST (3)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//
//        boolean removed = tree.remove("TEST");
//        assertTrue(removed);
//
//        expected =
//                "○\n" +
//                        "└── ○ T\n" +
//                        "    ├── ○ E (4)\n" +
//                        "    │   └── ○ AM (2)\n" +
//                        "    └── ○ OAST (3)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void testRemove_NoSuchKey() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        tree.put("FOO", 1);
//        tree.put("BAR", 2);
//
//        String expected, actual;
//        expected =
//                "○\n" +
//                        "├── ○ BAR (2)\n" +
//                        "└── ○ FOO (1)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//
//        boolean removed = tree.remove("BAZ");
//        assertFalse(removed);
//
//        expected =
//                "○\n" +                     // we expect no change
//                        "├── ○ BAR (2)\n" +
//                        "└── ○ FOO (1)\n";
//        actual = PrettyPrinter.prettyPrint(tree);
//        assertEquals(expected, actual);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testRemove_ArgumentValidation() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        //noinspection NullableProblems
//        tree.remove(null);
//    }
//
//    @Test
//    public void testSearchTree() {
//        ConcurrentRadixTree<Integer> tree = new ConcurrentRadixTree<Integer>();
//        // Build the tree by hand, as if the following strings were added: B, BA, BAN, BANDANA, BANAN, BANANA
//
//        //    ○
//        //    └── ○ B (1)
//        //        └── ○ A (2)
//        //            └── ○ N (3)
//        //                ├── ○ AN (5)
//        //                │   └── ○ A (6)
//        //                └── ○ DANA (4)
//
//        final Node root, n1, n2, n3, n4, n5, n6;
//        n6 = createNode("A", 6, Collections.<Node>emptyList(), false);
//        n5 = createNode("AN", 5, Arrays.asList(n6), false);
//        n4 = createNode("DANA", 4, Collections.<Node>emptyList(), false);
//        n3 = createNode("N", 3, Arrays.asList(n4, n5), false); // note: it should sort these such that n5 is first
//        n2 = createNode("A", 2, Arrays.asList(n3), false);
//        n1 = createNode("B", 1, Arrays.asList(n2), false);
//        //noinspection NullableProblems
//        root = createNode("", null, Arrays.asList(n1), true);
//
//        // Overwrite the tree's default root with the one built by hand...
//        tree.root = root;
//
//        // Sanity checks to assert that we built tree as expected....
//        String expected =
//                "○\n" +
//                        "└── ○ B (1)\n" +
//                        "    └── ○ A (2)\n" +
//                        "        └── ○ N (3)\n" +
//                        "            ├── ○ AN (5)\n" +
//                        "            │   └── ○ A (6)\n" +
//                        "            └── ○ DANA (4)\n";
//
//        assertEquals(expected, PrettyPrinter.prettyPrint(tree));
//        assertEquals(2, n3.getOutgoingEdges().size());
//        assertTrue(n3.getOutgoingEdges().contains(n4));
//        assertTrue(n3.getOutgoingEdges().contains(n5));
//
//        // Search for non-existing node. Should return root, with null parent, 0 charsMatched...
//        assertEquals(tree.root, tree.searchTree("Z").nodeFound);
//        assertNull(tree.searchTree("Z").parentNode);
//        assertNull(tree.searchTree("Z").parentNodesParent);
//        assertEquals(0, tree.searchTree("Z").charsMatched);
//
//        // Search for first child node "B". Should return n1, parent should be root, 1 charsMatched...
//        assertEquals(n1, tree.searchTree("B").nodeFound);
//        assertEquals(tree.root, tree.searchTree("B").parentNode);
//        assertEquals(null, tree.searchTree("B").parentNodesParent);
//        assertEquals(1, tree.searchTree("B").charsMatched);
//
//        // Search for node with split and multi-char child node at "BAN". Should return n3, parent n2, 3 charsMatched...
//        assertEquals(n3, tree.searchTree("BAN").nodeFound);
//        assertEquals(n2, tree.searchTree("BAN").parentNode);
//        assertEquals(n1, tree.searchTree("BAN").parentNodesParent);
//        assertEquals(3, tree.searchTree("BAN").charsMatched);
//
//        // Search for node with multi-char label (exact match) at "BANAN". Should return n5, parent n3, 5 charsMatched...
//        assertEquals(n5, tree.searchTree("BANAN").nodeFound);
//        assertEquals(n3, tree.searchTree("BANAN").parentNode);
//        assertEquals(n2, tree.searchTree("BANAN").parentNodesParent);
//        assertEquals(5, tree.searchTree("BANAN").charsMatched);
//
//        // Search for node with multi-char label (inexact match) at "BANA". Should return n5, parent n3, 4 charsMatched...
//        assertEquals(n5, tree.searchTree("BANA").nodeFound);
//        assertEquals(n3, tree.searchTree("BANA").parentNode);
//        assertEquals(n2, tree.searchTree("BANA").parentNodesParent);
//        assertEquals(4, tree.searchTree("BANA").charsMatched);
//
//        // Search for the last node in "BANANA". Should return n6, parent n5, 6 charsMatched...
//        assertEquals(n6, tree.searchTree("BANANA").nodeFound);
//        assertEquals(n5, tree.searchTree("BANANA").parentNode);
//        assertEquals(n3, tree.searchTree("BANANA").parentNodesParent);
//        assertEquals(6, tree.searchTree("BANANA").charsMatched);
//
//        // Search for string longer than anything in tree, differing after leaf node "BANANA".
//        // Should return n6, parent n5, 6 chars matched...
//        assertEquals(n6, tree.searchTree("BANANAS").nodeFound);
//        assertEquals(n5, tree.searchTree("BANANAS").parentNode);
//        assertEquals(n3, tree.searchTree("BANANAS").parentNodesParent);
//        assertEquals(6, tree.searchTree("BANANAS").charsMatched);
//
//        // Search for string longer than anything in tree, differing before split at "BAN".
//        // Should return n2, parent n1, 2 chars matched...
//        assertEquals(n2, tree.searchTree("BAR").nodeFound);
//        assertEquals(n1, tree.searchTree("BAR").parentNode);
//        assertEquals(tree.root, tree.searchTree("BAR").parentNodesParent);
//        assertEquals(2, tree.searchTree("BAR").charsMatched);
//
//        // Search for string longer than anything in tree, differing immediately after split at "BAN".
//        // Should return n3, parent n2, 3 chars matched...
//        assertEquals(n3, tree.searchTree("BANS").nodeFound);
//        assertEquals(n2, tree.searchTree("BANS").parentNode);
//        assertEquals(n1, tree.searchTree("BANS").parentNodesParent);
//        assertEquals(3, tree.searchTree("BANS").charsMatched);
//
//        // Search for string longer than anything in tree, differing in multi-char node "BANDANA".
//        // Should return n4, parent n3, 5 chars matched...
//        assertEquals(n4, tree.searchTree("BANDAIDS").nodeFound);
//        assertEquals(n3, tree.searchTree("BANDAIDS").parentNode);
//        assertEquals(n2, tree.searchTree("BANDAIDS").parentNodesParent);
//        assertEquals(5, tree.searchTree("BANDAIDS").charsMatched);
//    }
//
//    @Test(expected = IllegalStateException.class)
//    public void testSearchResult_FailureToClassify1() {
//        // Testing the various (unlikely) ways to fall through classification to have the exception thrown...
//        new ConcurrentRadixTree.SearchResult("DUMMY", null, 70, 70, null, null);
//    }
//
//    @Test(expected = IllegalStateException.class)
//    public void testSearchResult_FailureToClassify2() {
//        // Testing the various (unlikely) ways to fall through classification to have the exception thrown...
//        Node dummyNodeFound = createNode("DUMMY", 1, Collections.<Node>emptyList(), false);
//        new ConcurrentRadixTree.SearchResult("DUMMY", dummyNodeFound, 5, 70, null, null);
//
//    }
//
//    @Test(expected = IllegalStateException.class)
//    public void testSearchResult_FailureToClassify3() {
//        // Testing the various (unlikely) ways to fall through classification to have the exception thrown...
//        Node dummyNodeFound = createNode("DUMMY", 1, Collections.<Node>emptyList(), false);
//        new ConcurrentRadixTree.SearchResult("DUMMY", dummyNodeFound, 4, 70, null, null);
//    }
//
//    @Test
//    public void testSerialization() {
//        ConcurrentRadixTree<Integer> tree1 = new ConcurrentRadixTree<Integer>();
//        tree1.put("TEST", 1);
//        tree1.put("TEAM", 2);
//        tree1.put("TOAST", 3);
//
//        ConcurrentRadixTree<Integer> tree2 = TestUtility.deserialize(ConcurrentRadixTree.class, TestUtility.serialize(tree1));
//        assertEquals(PrettyPrinter.prettyPrint(tree1), PrettyPrinter.prettyPrint(tree2));
//    }
//
//    private Node createNode(CharSequence edgeCharacters, Object value, List<Node> childNodes, boolean isRoot) {
//        if (edgeCharacters == null) {
//            throw new IllegalStateException("The edgeCharacters argument was null");
//        }
//        if (!isRoot && edgeCharacters.length() == 0) {
//            throw new IllegalStateException("Invalid edge characters for non-root node: " + CharSequences.toString(edgeCharacters));
//        }
//        if (childNodes == null) {
//            throw new IllegalStateException("The childNodes argument was null");
//        }
//        NodeUtil.ensureNoDuplicateEdges(childNodes);
//
//
//        if (childNodes.isEmpty()) {
//            // Leaf node...
//            return new CharSequenceNodeDefault(edgeCharacters, value);
//        } else {
//            // Non-leaf node...
//            if (value == null) {
//                return new CharSequenceNodeDefault(edgeCharacters, null, childNodes);
//            } else {
//                return new CharSequenceNodeDefault(edgeCharacters, value, childNodes);
//            }
//        }
//    }
//
//    private static PrettyPrintable wrapNodeForPrinting(final Node node) {
//        return new PrettyPrintable() {
//            @Override
//            public Node getNode() {
//                return node;
//            }
//        };
//    }
//}
