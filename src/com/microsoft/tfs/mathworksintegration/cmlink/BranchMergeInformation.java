// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

/**
 * Stores information used in branch or merge operations.
 */
public class BranchMergeInformation {

    private final String sourcePath;
    private final String targetPath;
    private final String changesetSpecifier;

    /**
     * Initializes a BranchMergeInformation instance.
     * @param sourcePath
     *     The source path to use in the branch or merge operation.
     * @param targetPath
     *     The destination path to use in the branch or merge operation.
     * @param changesetSpecifier
     *     Specifies which changeset(s) to branch or merge.
     */
    public BranchMergeInformation(String sourcePath, String targetPath, String changesetSpecifier) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.changesetSpecifier = changesetSpecifier;
    }

    /**
     * Gets the source path to use in the branch or merge operation.
     */
    public String getSourcePath() {
        return this.sourcePath;
    }

    /**
     * Gets the destination path to use in the branch or merge operation.
     */
    public String getTargetPath() {
        return this.targetPath;
    }

    /**
     * Gets the specifier indicating which changeset(s) to branch or merge.
     */
    public String getChangesetSpecifier() {
        return this.changesetSpecifier;
    }
}
