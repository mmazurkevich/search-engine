package org.search.engine.tree;

import java.util.Set;

/**
 * Interface for tree of indexing tokens contains of letters and stored values in some nodes.
 * API contains only needed methods for working with indexation engine. This structure inspired
 * by the implementation of concurrent radix tree which reduce memory consumption on storing lexemes.
 * But this structure tuned by using trove4j to reduce memory consumption by storing values in the nodes.
 * @see <a href="https://bitbucket.org/trove4j/trove">Trove4j</a>
 * @see <a href="https://github.com/npgall/concurrent-trees">Concurrent-trees</a>
 * @see <a href="https://oscarforner.com/2016/02/26/Prefix_trees__Comparison_between_Trie__Ternary_Search_Tree_and_Radix_Tree">Trees comparison</a>
 */
public interface SearchEngineTree {

    TreeNode getRoot();

    void setRoot(TreeNode root);

    /**
     * Method put the given lexeme to the tree with value. But because of storing in the node
     * list of values during conflict it adds this value to the currently exists list.
     *
     * @param key CharSequence contains indexing lexeme
     * @param value Identifier of indexed entity
     */
    void putMergeOnConflict(CharSequence key, int value);

    void update(CharSequence key, int value);

    /**
     * Get set of lexeme indexed by the certain value. Dictionary of indexed document.
     * It used for comparing old and new document during document modification to avoid redundant
     * reindexation of already existing lexeme.
     *
     * @param value Identifier of indexed entity
     * @return Dictionary of indexed entity
     */
    Set<String> getKeys(int value);

    /**
     * Get the set of unique identifiers of indexed entity by the certain lexeme which start with.
     *
     * @param key CharSequence of lexeme
     * @return Identifiers of indexed entity containing this lexeme
     */
    Set<Integer> getValue(CharSequence key);
    /**
     * Method removes value from tree node and if value's list size is less equal one, remove node from tree.
     *
     * @param key CharSequence of searched lexeme
     * @param value Identifier of indexed entity which should be removed
     */
    void removeByKeyAndValue(CharSequence key, int value);

    /**
     * Remove specific value from the all nodes in the tree. If tre contains list of size one,
     * this node will be removed
     *
     * @param value Identifier of indexed entity which should be removed from all nodes
     */
    void removeByValue(int value);

    /**
     * Return the size of all node in the tree which contains values
     *
     * @return number of nodes with value in the tree
     */
    int size();

    void setTrackChangesListener(SearchTreeTrackChangesListener listener);

    void clear();
}
