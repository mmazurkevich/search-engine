package org.search.engine.model;

import java.nio.file.Path;
import java.util.Set;

public class IndexChanges {

    private Set<Path> oldFolders;
    private Set<Path> newFolders;

    private Set<Path> newFiles;
    private Set<Path> oldFiles;
    private Set<Path> changedFiles;

    public IndexChanges(Set<Path> oldFolders, Set<Path> newFolders, Set<Path> newFiles, Set<Path> oldFiles, Set<Path> changedFiles) {
        this.oldFolders = oldFolders;
        this.newFolders = newFolders;
        this.newFiles = newFiles;
        this.oldFiles = oldFiles;
        this.changedFiles = changedFiles;
    }

    public Set<Path> getOldFolders() {
        return oldFolders;
    }

    public Set<Path> getNewFolders() {
        return newFolders;
    }

    public Set<Path> getNewFiles() {
        return newFiles;
    }

    public Set<Path> getOldFiles() {
        return oldFiles;
    }

    public Set<Path> getChangedFiles() {
        return changedFiles;
    }
}
