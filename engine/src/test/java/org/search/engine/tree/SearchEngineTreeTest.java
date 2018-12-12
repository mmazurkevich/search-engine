package org.search.engine.tree;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchEngineTreeTest {

    private SearchEngineTree tree;

    @Before
    public void setUp() {
        tree = new SearchEngineConcurrentTree();
    }

    @Test
    public void testAddToRoot() {
        tree.putMergeOnConflict("A", 1);
        String expected = "○\n" +
                "└── ○ A {1}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testChildNodeSorting() {
        tree.putMergeOnConflict("B", 1);
        tree.putMergeOnConflict("A", 2);
        String expected = "○\n" +
                "├── ○ A {2}\n" +
                "└── ○ B {1}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testAppendChild() {
        tree.putMergeOnConflict("FOO", 1);
        tree.putMergeOnConflict("FOOBAR", 2);

        String expected = "○\n" +
                "└── ○ FOO {1}\n" +
                "    └── ○ BAR {2}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testSplitEdge() {
        tree.putMergeOnConflict("FOOBAR", 1);
        tree.putMergeOnConflict("FOO", 2);

        String expected = "○\n" +
                "└── ○ FOO {2}\n" +
                "    └── ○ BAR {1}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testSplitWithNode() {
        tree.putMergeOnConflict("FOOBAR", 1);
        tree.putMergeOnConflict("FOOD", 2);

        String expected = "○\n" +
                "└── ○ FOO\n" + // not explicitly inserted FOO
                "    ├── ○ BAR {1}\n" +
                "    └── ○ D {2}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testSplitNodesAndMove() {
        tree.putMergeOnConflict("TEST", 1);
        tree.putMergeOnConflict("TEAM", 2);
        tree.putMergeOnConflict("TOAST", 3);

        String expected = "○\n" +
                "└── ○ T\n" +  // node added automatically
                "    ├── ○ E\n" +
                "    │   ├── ○ AM {2}\n" +
                "    │   └── ○ ST {1}\n" +
                "    └── ○ OAST {3}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testPutWithMergeOnConflict() {
        tree.putMergeOnConflict("FOO", 1);
        tree.putMergeOnConflict("FOO", 2);

        assertEquals(Arrays.asList(2, 1), tree.getValue("FOO"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArgumentValidationOnNull() {
        tree.putMergeOnConflict(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArgumentValidationOnEmpty() {
        tree.putMergeOnConflict("", 1);
    }

    @Test
    public void testSize() {
        assertEquals(0, tree.size());
        tree.putMergeOnConflict("TEST", 1);
        assertEquals(1, tree.size());
        tree.putMergeOnConflict("TEAM", 2);
        assertEquals(2, tree.size());
        tree.putMergeOnConflict("TOAST", 3);
        assertEquals(3, tree.size());

        tree.removeByKey("FOO");
        assertEquals(3, tree.size());
        tree.removeByKey("TOAST");
        assertEquals(2, tree.size());
        tree.removeByKey("TEAM");
        assertEquals(1, tree.size());
        tree.removeByKey("TEST");
        assertEquals(0, tree.size());
    }

    @Test
    public void testGetValue() {
        tree.putMergeOnConflict("TEST", 1);
        tree.putMergeOnConflict("TEAM", 2);
        tree.putMergeOnConflict("TOAST", 3);

        assertEquals(Collections.singletonList(1), tree.getValue("TEST"));
        assertEquals(Collections.singletonList(2), tree.getValue("TEAM"));
        assertEquals(Collections.singletonList(3), tree.getValue("TOAST"));
        assertTrue(tree.getValue("T").isEmpty());
        assertTrue(tree.getValue("TE").isEmpty());
        assertTrue(tree.getValue("E").isEmpty());
        assertTrue(tree.getValue("").isEmpty());
    }

    @Test
    public void testRemoveByKeyWithMoreThanOneChildNode() {
        tree.putMergeOnConflict("FOO", 1);
        tree.putMergeOnConflict("FOOBAR", 2);
        tree.putMergeOnConflict("FOOD", 3);

        String expected = "○\n" +
                "└── ○ FOO {1}\n" +
                "    ├── ○ BAR {2}\n" +
                "    └── ○ D {3}\n";
        assertEquals(expected, tree.toString());

        tree.removeByKey("FOO"); // value removed, but node stay
        expected = "○\n" +
                "└── ○ FOO\n" +
                "    ├── ○ BAR {2}\n" +
                "    └── ○ D {3}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testRemoveByKeyWithExactlyOneChildNode() {
        tree.putMergeOnConflict("FOO", 1);
        tree.putMergeOnConflict("FOOBAR", 2);
        tree.putMergeOnConflict("FOOBARBAZ", 3);

        String expected = "○\n" +
                "└── ○ FOO {1}\n" +
                "    └── ○ BAR {2}\n" +
                "        └── ○ BAZ {3}\n";
        assertEquals(expected, tree.toString());

        tree.removeByKey("FOO"); //FOO and BAR merged and the value copied

        expected = "○\n" +
                "└── ○ FOOBAR {2}\n" +
                "    └── ○ BAZ {3}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testRemoveByKeyWithoutChildNodes() {
        tree.putMergeOnConflict("FOO", 1);
        tree.putMergeOnConflict("BAR", 2);

        String expected = "○\n" +
                "├── ○ BAR {2}\n" +
                "└── ○ FOO {1}\n";
        assertEquals(expected, tree.toString());

        tree.removeByKey("FOO");
        expected = "○\n" +
                "└── ○ BAR {2}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testRemoveByKeyFromRoot() {
        tree.putMergeOnConflict("FOO", 1);

        String expected;
        expected = "○\n" +
                "└── ○ FOO {1}\n";
        assertEquals(expected, tree.toString());

        tree.removeByKey("FOO");

        expected = "○\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testRemoveByKeyOneStepFromRoot() {
        tree.putMergeOnConflict("FOO", 1);
        tree.putMergeOnConflict("FOOBAR", 2);

        String expected = "○\n" +
                "└── ○ FOO {1}\n" +
                "    └── ○ BAR {2}\n";
        assertEquals(expected, tree.toString());

        tree.removeByKey("FOOBAR");

        expected = "○\n" +
                "└── ○ FOO {1}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testRemoveByKeyImplicitNodeNode() {
        tree.putMergeOnConflict("FOOBAR", 1);
        tree.putMergeOnConflict("FOOD", 2);

        String expected = "○\n" +
                "└── ○ FOO\n" +
                "    ├── ○ BAR {1}\n" +
                "    └── ○ D {2}\n";
        assertEquals(expected, tree.toString());

        tree.removeByKey("FOO");
        expected = "○\n" +
                "└── ○ FOO\n" +
                "    ├── ○ BAR {1}\n" +
                "    └── ○ D {2}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testRemoveByKeyMergeSplittedNode() {
        tree.putMergeOnConflict("TEST", 1);
        tree.putMergeOnConflict("TEAM", 2);
        tree.putMergeOnConflict("TOAST", 3);

        String expected = "○\n" +
                "└── ○ T\n" +
                "    ├── ○ E\n" +
                "    │   ├── ○ AM {2}\n" +
                "    │   └── ○ ST {1}\n" +
                "    └── ○ OAST {3}\n";
        assertEquals(expected, tree.toString());

        tree.removeByKey("TEST");
        expected = "○\n" +
                "└── ○ T\n" +
                "    ├── ○ EAM {2}\n" +
                "    └── ○ OAST {3}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testRemoveByKeyDoNotMergeSplittedNode() {
        tree.putMergeOnConflict("TEST", 1);
        tree.putMergeOnConflict("TEAM", 2);
        tree.putMergeOnConflict("TOAST", 3);
        tree.putMergeOnConflict("TE", 4);

        String expected = "○\n" +
                "└── ○ T\n" +
                "    ├── ○ E {4}\n" +
                "    │   ├── ○ AM {2}\n" +
                "    │   └── ○ ST {1}\n" +
                "    └── ○ OAST {3}\n";
        assertEquals(expected, tree.toString());

        tree.removeByKey("TEST");
        expected = "○\n" +
                "└── ○ T\n" +
                "    ├── ○ E {4}\n" +
                "    │   └── ○ AM {2}\n" +
                "    └── ○ OAST {3}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testRemoveByKeyNoSuchKey() {
        tree.putMergeOnConflict("FOO", 1);
        tree.putMergeOnConflict("BAR", 2);

        String expected = "○\n" +
                "├── ○ BAR {2}\n" +
                "└── ○ FOO {1}\n";
        assertEquals(expected, tree.toString());

        tree.removeByKey("BAZ");

        expected = "○\n" +
                "├── ○ BAR {2}\n" +
                "└── ○ FOO {1}\n";
        assertEquals(expected, tree.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveByKeyArgumentValidation() {
        tree.removeByKey(null);
    }

    @Test
    public void testRemoveByValue() {
        tree.putMergeOnConflict("TEST", 1);
        tree.putMergeOnConflict("TEAM", 2);
        tree.putMergeOnConflict("TOAST", 3);
        tree.putMergeOnConflict("TOOST", 2);
        tree.putMergeOnConflict("TE", 2);

        String expected = "○\n" +
                "└── ○ T\n" +
                "    ├── ○ E {2}\n" +
                "    │   ├── ○ AM {2}\n" +
                "    │   └── ○ ST {1}\n" +
                "    └── ○ O\n" +
                "        ├── ○ AST {3}\n" +
                "        └── ○ OST {2}\n";
        assertEquals(expected, tree.toString());

        tree.removeByValue(2);
        expected = "○\n" +
                "└── ○ T\n" +
                "    ├── ○ EST {1}\n" +
                "    └── ○ OAST {3}\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testRemoveByValueEmptyRootResult() {
        tree.putMergeOnConflict("TEST", 1);
        tree.putMergeOnConflict("TEAM", 1);

        String expected = "○\n" +
                "└── ○ TE\n" +
                "    ├── ○ AM {1}\n" +
                "    └── ○ ST {1}\n";
        assertEquals(expected, tree.toString());

        tree.removeByValue(1);
        expected = "○\n";
        assertEquals(expected, tree.toString());
    }

    @Test
    public void testGetKeysByValue() {
        tree.putMergeOnConflict("TEST", 1);
        tree.putMergeOnConflict("TEAM", 2);
        tree.putMergeOnConflict("TOAST", 3);
        tree.putMergeOnConflict("TOOST", 2);
        tree.putMergeOnConflict("TE", 2);

        String expected = "○\n" +
                "└── ○ T\n" +
                "    ├── ○ E {2}\n" +
                "    │   ├── ○ AM {2}\n" +
                "    │   └── ○ ST {1}\n" +
                "    └── ○ O\n" +
                "        ├── ○ AST {3}\n" +
                "        └── ○ OST {2}\n";
        assertEquals(expected, tree.toString());

        Set<String> keys = tree.getKeys(2);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("TE"));
        assertTrue(keys.contains("TEAM"));
        assertTrue(keys.contains("TOOST"));
    }

}
