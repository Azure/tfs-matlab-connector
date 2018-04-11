// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.tests.cmlink;

import com.microsoft.tfs.mathworksintegration.cmlink.BranchMergeInformation;
import com.microsoft.tfs.mathworksintegration.cmlink.TfsFileProperty;
import com.microsoft.tfs.mathworksintegration.cmlink.TfsAdapter;
import com.microsoft.tfs.mathworksintegration.cmlink.Utilities;
import com.mathworks.cmlink.api.ConflictedRevisions;
import com.mathworks.cmlink.api.FileProperty;
import com.mathworks.cmlink.api.LocalStatus;
import com.mathworks.cmlink.api.Revision;
import com.mathworks.cmlink.api.version.r16b.CMAdapter;
import com.mathworks.cmlink.api.version.r16b.FileState;
import com.mathworks.cmlink.sdk.tests.TAdapter;
import com.mathworks.cmlink.sdk.tests.util.FileCreation;
import com.mathworks.cmlink.sdk.tests.util.SourceControlSetupRule;
import com.mathworks.toolbox.shared.computils.file.ChecksumGenerator;
import com.mathworks.toolbox.shared.computils.file.FileUtil;

import org.junit.Test;
import org.junit.AfterClass;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.mathworks.cmlink.sdk.tests.util.Matchers.allValues;
import static com.mathworks.cmlink.sdk.tests.util.Matchers.haveStatus;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * An extension of the {@link TAdapter} class for use with tests.
 * Contains additional tests specifically for the TFS source control adapter.
 */
public class TTfsAdapter extends TAdapter {

    /**
     * Initializes a TTfsAdapter instance.
     */
    public TTfsAdapter() throws Exception {
        super(new TfsTestEnvironment());

        // TODO: Currently, the tests require you to run the storeTfsSettings.m function and to use
        // an on-premise TFS server with Windows credentials to avoid credential prompts.
        // Would be nice to make this more generalized.
    }

    /**
     * Closes the TFS connection once all tests are complete.
     */
    @AfterClass
    public static void CloseTfsConnection() {
        try {
            Utilities.getTfsConnection().close();
        }
        catch (Exception ex) {
            // Test suite is over, do nothing.
        }
    }

    @Test
    public void testLatestFileProperty() throws Exception {
        File primarySandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter primaryAdapter = fSourceControlSetupRule.getCMAdapterFor(primarySandbox);

        File secondarySandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter secondaryAdapter = fSourceControlSetupRule.getCMAdapterFor(secondarySandbox);
        
        // Add a file and create conflicting change
        File file = FileCreation.createTempFileContainingText(primarySandbox);
        Collection<File> fileAsCollection = Collections.singleton(file);
        primaryAdapter.add(fileAsCollection);
        primaryAdapter.checkin(fileAsCollection, "add file");
        secondaryAdapter.update(secondarySandbox);
        
        FileCreation.modifyFiles(fileAsCollection);
        primaryAdapter.checkin(fileAsCollection, "update file");
        
        Collection<File> fileAsCollectionInSecondary = changeRoot(fileAsCollection, secondarySandbox, primarySandbox);
        File fileInSecondary = fileAsCollectionInSecondary.iterator().next();
        FileCreation.modifyFiles(fileAsCollectionInSecondary);
        
        // Verify the latest property
        Collection<FileProperty> primaryProperties =
            primaryAdapter.getFileState(fileAsCollection).get(file).getProperties();
        Collection<FileProperty> secondaryProperties =
            secondaryAdapter.getFileState(fileAsCollectionInSecondary).get(fileInSecondary).getProperties();
        
        TfsFileProperty primaryLatestProperty = null;
        for (FileProperty prop : primaryProperties) {
            if (prop.getType() == "latest") {
                primaryLatestProperty = (TfsFileProperty)prop;
                break;
            }
        }
        assertThat("Primary adapter file has a property called 'latest'", primaryLatestProperty.getName(), is("latest"));
        
        TfsFileProperty secondaryLatestProperty = null;
        for (FileProperty prop : secondaryProperties) {
            if (prop.getType() == "latest") {
                secondaryLatestProperty = (TfsFileProperty)prop;
                break;
            }
        }
        assertThat("Secondary adapter file has a property called 'not latest'",
            secondaryLatestProperty.getName(),is("not latest"));
    }

    @Test
    public void testFileStateProperties() throws Exception {
        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);

        // Add a file
        File mainBranch = new File(sandbox, "Main");
        mainBranch.mkdir();
        File file = FileCreation.createTempFileContainingText(mainBranch);
        Collection<File> fileAsCollection = Collections.singleton(file);
        adapter.add(fileAsCollection);
        adapter.checkin(fileAsCollection, "add file");
        
        // Rename the file
        File renamedFile = new File(file.getAbsolutePath() + ".1");
        adapter.moveFile(file, renamedFile);

        // Verify the rename property
        Collection<File> renamedAsCollection = Collections.singleton(renamedFile);
        Collection<FileProperty> properties = adapter.getFileState(renamedAsCollection).get(renamedFile).getProperties();
        TfsFileProperty renameProperty = null;
        for (FileProperty prop : properties) {
            if (prop.getType() == "rename") {
                renameProperty = (TfsFileProperty)prop;
                break;
            }
        }

        assertThat("File has a property called 'rename'", renameProperty, is(not(nullValue())));
        assertThat("File 'rename' property is a modification", renameProperty.isModification(), is(true));

        adapter.uncheckout(renamedAsCollection);

        // Branch the file
        File branchDir = FileUtil.fullFile(sandbox, "Branch_" + System.currentTimeMillis());
        File branchedFile = getBranchedFilePath(file, branchDir);
        Collection<File> branchedFileAsCollection = Collections.singleton(branchedFile);

        TfsAdapter tfsAdapter = (TfsAdapter)adapter;
        BranchMergeInformation branchInfo = new BranchMergeInformation(file.getAbsolutePath(),
            branchedFile.getAbsolutePath(), null);
        tfsAdapter.branch(branchInfo);

        // Verify the branch property
        properties = adapter.getFileState(branchedFileAsCollection).get(branchedFile).getProperties();
        TfsFileProperty branchProperty = null;
        for (FileProperty prop : properties) {
            if (prop.getType() == "branch") {
                branchProperty = (TfsFileProperty)prop;
                break;
            }
        }
        assertThat("File has a property called 'branch'", branchProperty, is(not(nullValue())));
        assertThat("File 'branch' property is a modification", branchProperty.isModification(), is(true));

        adapter.checkin(branchedFileAsCollection, "branch file");

        // Update the file and merge it back
        FileCreation.modifyFiles(branchedFileAsCollection);
        adapter.checkin(branchedFileAsCollection, "edit branched file");

        BranchMergeInformation mergeInfo = new BranchMergeInformation(branchedFile.getAbsolutePath(),
            file.getAbsolutePath(), null);
        tfsAdapter.merge(mergeInfo);

        // Verify the merge property
        properties = adapter.getFileState(fileAsCollection).get(file).getProperties();
        TfsFileProperty mergeProperty = null;
        for (FileProperty prop : properties) {
            if (prop.getType() == "merge") {
                mergeProperty = (TfsFileProperty)prop;
                break;
            }
        }
        assertThat("File has a property called 'merge'", mergeProperty, is(not(nullValue())));
        assertThat("File 'merge' property is a modification", mergeProperty.isModification(), is(true));

        adapter.uncheckout(fileAsCollection);
    }

    @Test
    public void testSyncFilesAndGetStatesRecursively() throws Exception {
        File primarySandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter primaryAdapter = fSourceControlSetupRule.getCMAdapterFor(primarySandbox);

        File secondarySandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter secondaryAdapter = fSourceControlSetupRule.getCMAdapterFor(secondarySandbox);

        // Create a new directory and add some files to it.
        File newDirectory = new File(primarySandbox, "RecursiveTestDir" + System.currentTimeMillis());
        Collection<File> files = new ArrayList<>();
        files.add(newDirectory);
        int numTextFiles = 5;
        int totalFileCount = numTextFiles + 1; // +1 for the directory
        for (int counter = 0; counter < numTextFiles; counter++) {
            File file = FileCreation.createTempFileContainingText(newDirectory);
            files.add(file);
        }
        primaryAdapter.add(files);
        primaryAdapter.checkin(files, "recursive test check-in");

        // Verify the new directory doesn't exist on the local machine yet in the secondary sandbox.
        File newDirectoryInSecondary =
            changeRoot(Collections.singleton(newDirectory), secondarySandbox, primarySandbox).iterator().next();
        assertThat("Before the sync, the new directory shouldn't exist locally in the secondary sandbox.",
            newDirectoryInSecondary.exists(),
            is(false));

        // Recursively sync all files in the secondary sandbox
        secondaryAdapter.update(secondarySandbox);

        // Recursively get the state for each item in the new directory in the secondary sandbox.
        Map<File, FileState> secondaryStateMap = secondaryAdapter.getStateForAllKnownFilesRecursively(newDirectoryInSecondary);

        // Validate that we have an entry for each file, and that each file was pulled down to disk with the correct state.
        assertThat("After syncing the new directory and getting its state map, there should be an entry for the directory and each new file.",
            secondaryStateMap.keySet().size(), is(equalTo(totalFileCount)));

        for (Map.Entry<File, FileState> entry : secondaryStateMap.entrySet()) {
            File file = entry.getKey();
            FileState state = entry.getValue();

            assertThat("File should exist locally after recursive sync", file.exists(), is(true));
            assertThat("File state", state.getLocalStatus(), is(equalTo(LocalStatus.UNMODIFIED)));
        }
    }

    @Test
    public void testBranchMergeSingleFile() throws Exception {
        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);

        // Create the original file
        File mainBranch = new File(sandbox, "Main");
        mainBranch.mkdir();
        File mainFile = FileCreation.createTempFileContainingText(mainBranch);
        long originalChecksum = ChecksumGenerator.getCRC32Checksum(mainFile);
        Collection<File> mainFileAsCollection = Collections.singleton(mainFile);
        adapter.add(mainFileAsCollection);
        adapter.checkin(mainFileAsCollection, "add test file");

        // Branch the file
        File branchDir = FileUtil.fullFile(sandbox, "Branch_" + System.currentTimeMillis());
        File branchedFile = getBranchedFilePath(mainFile, branchDir);
        Collection<File> branchedFileAsCollection = Collections.singleton(branchedFile);

        TfsAdapter primaryTfsAdapter = (TfsAdapter)adapter;
        BranchMergeInformation branchInfo = new BranchMergeInformation(mainFile.getAbsolutePath(),
            branchedFile.getAbsolutePath(), null);
        primaryTfsAdapter.branch(branchInfo);

        assertThat(adapter.getFileState(branchedFileAsCollection), allValues(haveStatus(LocalStatus.ADDED)));
        adapter.checkin(branchedFileAsCollection, "create branched file");

        long branchedChecksum = ChecksumGenerator.getCRC32Checksum(branchedFile);
        assertThat("Branched file checksum", branchedChecksum, is(equalTo(originalChecksum)));

        // Edit the branched file
        FileCreation.modifyFiles(branchedFileAsCollection);
        adapter.checkin(branchedFileAsCollection, "edit branched file");
        long modifiedBranchedChecksum = ChecksumGenerator.getCRC32Checksum(branchedFile);

        // Merge the edit back to the main file
        BranchMergeInformation mergeInfo = new BranchMergeInformation(branchedFile.getAbsolutePath(),
            mainFile.getAbsolutePath(), null);
        primaryTfsAdapter.merge(mergeInfo);

        assertThat(adapter.getFileState(mainFileAsCollection), allValues(haveStatus(LocalStatus.MODIFIED)));
        adapter.checkin(mainFileAsCollection, "merged file edits");

        long modifiedMainChecksum = ChecksumGenerator.getCRC32Checksum(mainFile);
        assertThat("Main file checksum", modifiedMainChecksum, is(equalTo(modifiedBranchedChecksum)));
    }

    private File getBranchedFilePath(File file, File branchDir) {
        return new File(branchDir, file.getName());
    }

    @Test
    public void testBranchMergeDirectory() throws Exception {
        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);

        // Create the original files
        File mainBranch = new File(sandbox, "Main");
        mainBranch.mkdir();
        ArrayList<File> mainFiles = new ArrayList<File>();
        for (int i = 0; i < 3; i++) {
            mainFiles.add(FileCreation.createTempFileContainingText(mainBranch));
        }
        Map<File, Long> originalChecksums = ChecksumGenerator.getCRC32CheckSums(mainFiles);
        adapter.add(mainFiles);
        adapter.checkin(mainFiles, "add test files");

        // Branch the entire Main directory
        File branchDir = FileUtil.fullFile(sandbox, "Branch_" + System.currentTimeMillis());
        ArrayList<File> branchedFiles = new ArrayList<File>();
        for (File file : mainFiles) {
            branchedFiles.add(getBranchedFilePath(file, branchDir));
        }

        TfsAdapter primaryTfsAdapter = (TfsAdapter)adapter;
        BranchMergeInformation branchInfo = new BranchMergeInformation(mainBranch.getAbsolutePath(),
            branchDir.getAbsolutePath(), null);
        primaryTfsAdapter.branch(branchInfo);

        assertThat(adapter.getFileState(branchedFiles), allValues(haveStatus(LocalStatus.ADDED)));

        adapter.checkin(branchedFiles, "create branched files");

        Map<File, Long> branchedChecksums = ChecksumGenerator.getCRC32CheckSums(branchedFiles);
        assertThat("Checksums match", originalChecksums.values().containsAll(branchedChecksums.values()), is(true));

        // Edit the branched files
        FileCreation.modifyFiles(branchedFiles);
        adapter.checkin(branchedFiles, "edit branched files");
        Map<File, Long> modifiedBranchChecksums = ChecksumGenerator.getCRC32CheckSums(branchedFiles);

        // Merge the entire Branch directory back to Main
        BranchMergeInformation mergeInfo = new BranchMergeInformation(branchDir.getAbsolutePath(),
            mainBranch.getAbsolutePath(), null);
        primaryTfsAdapter.merge(mergeInfo);

        assertThat(adapter.getFileState(mainFiles), allValues(haveStatus(LocalStatus.MODIFIED)));
        adapter.checkin(mainFiles, "merged file edits");

        Map<File, Long> modifiedMainChecksums = ChecksumGenerator.getCRC32CheckSums(mainFiles);
        assertThat("Checksums match", modifiedBranchChecksums.values().containsAll(modifiedMainChecksums.values()), is(true));
    }

    @Test
    public void testMergeConflictResolution() throws Exception {
        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);

        // Create the original file
        File mainBranch = new File(sandbox, "Main");
        mainBranch.mkdir();
        File mainFile = FileCreation.createTempFileContainingText(mainBranch);
        Collection<File> mainFileAsCollection = Collections.singleton(mainFile);
        adapter.add(mainFileAsCollection);
        adapter.checkin(mainFileAsCollection, "add test file");

        // Branch the file
        File branchDir = FileUtil.fullFile(sandbox, "Branch_" + System.currentTimeMillis());
        File branchedFile = getBranchedFilePath(mainFile, branchDir);
        Collection<File> branchedFileAsCollection = Collections.singleton(branchedFile);

        TfsAdapter primaryTfsAdapter = (TfsAdapter)adapter;
        BranchMergeInformation branchInfo = new BranchMergeInformation(mainFile.getAbsolutePath(),
            branchedFile.getAbsolutePath(), null);
        primaryTfsAdapter.branch(branchInfo);
        adapter.checkin(branchedFileAsCollection, "create branched file");

        // Edit the branched file
        FileCreation.modifyFiles(branchedFileAsCollection);
        adapter.checkin(branchedFileAsCollection, "edit branched file");
        Revision modifiedBranchRevision = adapter.getFileState(branchedFileAsCollection).get(branchedFile).getRevision();

        // Create a conflicting change in the main file
        FileCreation.modifyFiles(mainFileAsCollection);
        adapter.checkin(mainFileAsCollection, "edit main file");
        long modifiedChecksum = ChecksumGenerator.getCRC32Checksum(mainFile);

        // Merge the branched file back to the main file
        BranchMergeInformation mergeInfo = new BranchMergeInformation(branchedFile.getAbsolutePath(),
            mainFile.getAbsolutePath(), null);
        primaryTfsAdapter.merge(mergeInfo);

        assertThat(adapter.getFileState(mainFileAsCollection), allValues(haveStatus(LocalStatus.CONFLICTED)));
        ConflictedRevisions conflictRevision = adapter.getRevisionCausingConflict(mainFile);
        assertThat("Revision causing conflict", conflictRevision.getTheirsRevision().getStringRepresentation(),
            is(equalTo(modifiedBranchRevision.getStringRepresentation())));

        // Resolve conflict
        adapter.resolveConflict(mainFile);
        assertThat(adapter.getFileState(mainFileAsCollection), allValues(haveStatus(LocalStatus.MODIFIED)));
        adapter.checkin(mainFileAsCollection, "merged file edits");

        long modifiedChecksumAfterMerge = ChecksumGenerator.getCRC32Checksum(mainFile);
        assertThat("Main file checksum after merge", modifiedChecksumAfterMerge, is(equalTo(modifiedChecksum)));
    }

    @Test
    public void testBranchSpecificChange() throws Exception {
        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);

        // Create the original file
        File mainBranch = new File(sandbox, "Main");
        mainBranch.mkdir();
        File mainFile = FileCreation.createTempFileContainingText(mainBranch);
        long originalChecksum = ChecksumGenerator.getCRC32Checksum(mainFile);
        Collection<File> mainFileAsCollection = Collections.singleton(mainFile);
        adapter.add(mainFileAsCollection);
        adapter.checkin(mainFileAsCollection, "add test file");
        Revision originalCheckin = adapter.getFileState(mainFileAsCollection).get(mainFile).getRevision();

        // Modify the files to create a new checkin
        FileCreation.modifyFiles(mainFileAsCollection);
        adapter.checkin(mainFileAsCollection, "edit test file");

        // Branch the file, but from the original changeset
        File branchDir = FileUtil.fullFile(sandbox, "Branch_" + System.currentTimeMillis());
        File branchedFile = getBranchedFilePath(mainFile, branchDir);
        Collection<File> branchedFileAsCollection = Collections.singleton(branchedFile);

        TfsAdapter primaryTfsAdapter = (TfsAdapter)adapter;
        BranchMergeInformation branchInfo = new BranchMergeInformation(mainFile.getAbsolutePath(),
            branchedFile.getAbsolutePath(), originalCheckin.getStringRepresentation());
        primaryTfsAdapter.branch(branchInfo);

        adapter.checkin(branchedFileAsCollection, "create branched file");

        long branchedChecksum = ChecksumGenerator.getCRC32Checksum(branchedFile);
        assertThat("Branched file checksum", branchedChecksum, is(equalTo(originalChecksum)));
    }

    @Test
    public void testMergeSpecificChanges() throws Exception {
        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);

        // Create the original file
        File mainBranch = new File(sandbox, "Main");
        mainBranch.mkdir();
        File mainFile = FileCreation.createTempFileContainingText(mainBranch);
        Collection<File> mainFileAsCollection = Collections.singleton(mainFile);
        adapter.add(mainFileAsCollection);
        adapter.checkin(mainFileAsCollection, "add test file");

        // Branch the file
        File branchDir = FileUtil.fullFile(sandbox, "Branch_" + System.currentTimeMillis());
        File branchedFile = getBranchedFilePath(mainFile, branchDir);
        Collection<File> branchedFileAsCollection = Collections.singleton(branchedFile);

        TfsAdapter primaryTfsAdapter = (TfsAdapter)adapter;
        BranchMergeInformation branchInfo = new BranchMergeInformation(mainFile.getAbsolutePath(),
            branchedFile.getAbsolutePath(), null);
        primaryTfsAdapter.branch(branchInfo);

        adapter.checkin(branchedFileAsCollection, "create branched file");

        // Edit the branched file three times
        FileCreation.modifyFiles(branchedFileAsCollection);
        adapter.checkin(branchedFileAsCollection, "edit branched files");
        long modifiedBranchedChecksum1 = ChecksumGenerator.getCRC32Checksum(branchedFile);
        Revision modifiedBranchRevision1 = adapter.getFileState(branchedFileAsCollection).get(branchedFile).getRevision();

        FileCreation.modifyFiles(branchedFileAsCollection);
        adapter.checkin(branchedFileAsCollection, "edit branched files");
        Revision modifiedBranchRevision2 = adapter.getFileState(branchedFileAsCollection).get(branchedFile).getRevision();

        FileCreation.modifyFiles(branchedFileAsCollection);
        adapter.checkin(branchedFileAsCollection, "edit branched files");
        long modifiedBranchedChecksum3 = ChecksumGenerator.getCRC32Checksum(branchedFile);
        Revision modifiedBranchRevision3 = adapter.getFileState(branchedFileAsCollection).get(branchedFile).getRevision();

        // Merge only the first change
        BranchMergeInformation mergeInfo = new BranchMergeInformation(branchedFile.getAbsolutePath(),
            mainFile.getAbsolutePath(), modifiedBranchRevision1.getStringRepresentation());
        primaryTfsAdapter.merge(mergeInfo);

        assertThat(adapter.getFileState(mainFileAsCollection), allValues(haveStatus(LocalStatus.MODIFIED)));
        adapter.checkin(mainFile, "merged single change");

        long modifiedMainChecksum = ChecksumGenerator.getCRC32Checksum(mainFile);
        assertThat("Modified file checksum", modifiedMainChecksum, is(equalTo(modifiedBranchedChecksum1)));

        // Merge the remaining changes by specifying a range
        mergeInfo = new BranchMergeInformation(branchedFile.getAbsolutePath(), mainFile.getAbsolutePath(),
            modifiedBranchRevision2.getStringRepresentation() + "-" + modifiedBranchRevision3.getStringRepresentation());
        primaryTfsAdapter.merge(mergeInfo);

        assertThat(adapter.getFileState(mainFileAsCollection), allValues(haveStatus(LocalStatus.MODIFIED)));
        adapter.checkin(mainFile, "merged changes");

        modifiedMainChecksum = ChecksumGenerator.getCRC32Checksum(mainFile);
        assertThat("Modified file checksum", modifiedMainChecksum, is(equalTo(modifiedBranchedChecksum3)));
    }
}
