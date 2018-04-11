/* Copyright 2010-2016 The MathWorks, Inc. */

/* Copyright (c) Microsoft Corporation */
package com.mathworks.cmlink.sdk.tests;

import com.mathworks.cmlink.api.AdapterSupportedFeature;
import com.mathworks.cmlink.api.ConfigurationManagementException;
import com.mathworks.cmlink.api.ConflictedRevisions;
import com.mathworks.cmlink.api.LocalStatus;
import com.mathworks.cmlink.api.Revision;
import com.mathworks.cmlink.api.version.r16b.CMAdapter;
import com.mathworks.cmlink.api.version.r16b.FileState;
import com.mathworks.cmlink.sdk.tests.util.FileCreation;
import com.mathworks.cmlink.sdk.tests.util.SourceControlSetupRule;
import com.mathworks.toolbox.shared.computils.file.ChecksumGenerator;
import com.mathworks.toolbox.shared.computils.file.FileSorter;
import com.mathworks.toolbox.shared.computils.file.FileUtil;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mathworks.cmlink.sdk.tests.util.Matchers.allValues;
import static com.mathworks.cmlink.sdk.tests.util.Matchers.are;
import static com.mathworks.cmlink.sdk.tests.util.Matchers.areLocked;
import static com.mathworks.cmlink.sdk.tests.util.Matchers.emptyString;
import static com.mathworks.cmlink.sdk.tests.util.Matchers.moreRecentThan;
import static com.mathworks.cmlink.sdk.tests.util.Matchers.hasItems;
import static com.mathworks.cmlink.sdk.tests.util.Matchers.haveStatus;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class TAdapter {

    @Rule
    public final SourceControlSetupRule fSourceControlSetupRule;

    protected CMTestEnvironment fCMTestEnvironment;
    private int fNumFilesUnderCM = 10;

    /**
     * This constructor should be called by the sub-class with a CMTestEnvironment that sets up the CMAdapter being
     * tested.
     *
     * @param testEnvironment The defines the CMAdapter being tested.
     * @throws Exception If an error occurs setting up the test environment.
     */
    public TAdapter(CMTestEnvironment testEnvironment) throws Exception {
        fSourceControlSetupRule = new SourceControlSetupRule(testEnvironment);
        fCMTestEnvironment = testEnvironment;
    }

    @Test
    public void testRemovingParentDirectoryRemovesChildFiles() throws Exception {

        File sandboxDir = fSourceControlSetupRule.newSandbox();
        CMAdapter cmAdapter = fSourceControlSetupRule.getCMAdapterFor(sandboxDir);

        File rootFile = new File(sandboxDir, "root");
        File childFile = new File(rootFile, "child");
        FileCreation.createFileContainingText(childFile);

        // CHANGE FROM ORIGINAL TEST
        // TFS won't let you pend a "delete" on a parent directory whose children
        // have a pending "add" on them. Update the test to add both files initially,
        // then delete just the parent, and verify that both files are gone.
        cmAdapter.add(Collections.singleton(rootFile));
        cmAdapter.add(Collections.singleton(childFile));
        cmAdapter.checkin(sandboxDir, "Adding files");

        cmAdapter.remove(Collections.singleton(rootFile));
        cmAdapter.checkin(sandboxDir, "Removing Files");

        Collection<File> allFiles = new ArrayList<>();
        allFiles.add(rootFile);
        allFiles.add(childFile);
        assertThat(cmAdapter.getFileState(allFiles), allValues(haveStatus(LocalStatus.NOT_UNDER_CM)));
    }

    @Test
    public void testUnAddingParentDirectoryUnAddsChildFiles() throws Exception {

        File sandboxDir = fSourceControlSetupRule.newSandbox();
        CMAdapter cmAdapter = fSourceControlSetupRule.getCMAdapterFor(sandboxDir);

        File rootFile = new File(sandboxDir, "root");

        File childFile = new File(rootFile, "child");
        FileCreation.createFileContainingText(childFile);

        cmAdapter.add(Collections.singleton(rootFile));

        cmAdapter.add(Collections.singleton(childFile));
        cmAdapter.uncheckout(Collections.singleton(rootFile));

        Collection<File> allFiles = new ArrayList<>();
        allFiles.add(rootFile);
        allFiles.add(childFile);
        assertThat(cmAdapter.getFileState(allFiles), allValues(haveStatus(LocalStatus.NOT_UNDER_CM)));
    }

    @Test
    public void testRemovingFilesFromCM() throws Exception {
        File sandboxDir = fSourceControlSetupRule.newSandbox();
        CMAdapter cmAdapter = fSourceControlSetupRule.getCMAdapterFor(sandboxDir);
        Collection<File> files = addCollectionOfFilesToCM(cmAdapter, sandboxDir);
        removeCollectionOfFilesFromCM(files, sandboxDir, cmAdapter);
    }

    @Test
    public void testMove() throws Exception {

        File sandboxDir = fSourceControlSetupRule.newSandbox();
        CMAdapter cmAdapter = fSourceControlSetupRule.getCMAdapterFor(sandboxDir);
        assumeTrue(cmAdapter.isFeatureSupported(AdapterSupportedFeature.MOVE) );

        Collection<File> files = buildACollectionOfTempFilesContainingText(fNumFilesUnderCM, sandboxDir);

        cmAdapter.add(files);
        //Test that it is ok to move added files.
        for (File location : files) {
            File newLocation = getMovedFileName(location);
            cmAdapter.moveFile(location, newLocation);
            cmAdapter.moveFile(newLocation, location);
        }

        cmAdapter.checkin(files, "Initial Commit");

        for (File location : files) {

            File newLocation = getMovedFileName(location);
            cmAdapter.moveFile(location, newLocation);
            cmAdapter.checkin(sandboxDir, " move file");

            //The moved file should now be under source control and unmodified.
            assertThat(cmAdapter.getFileState(Collections.singleton(newLocation)), allValues(haveStatus(LocalStatus.UNMODIFIED)));

            cmAdapter.moveFile(newLocation, location);
            cmAdapter.checkin(sandboxDir, "moved back");
        }
    }

    private static File getMovedFileName(File fileToMove) {

        String filename =
                FileUtil.getNameWithExtensionStripped(fileToMove) + "_moved"
                        + FileUtil.getFileExtension(fileToMove);

        return new File(
                fileToMove.getParentFile().getAbsoluteFile(),
                filename
        );
    }


    @Test
    public void testAddRemove() throws Exception {

        File sandboxDir = fSourceControlSetupRule.newSandbox();
        CMAdapter cmAdapter = fSourceControlSetupRule.getCMAdapterFor(sandboxDir);

        Collection<File> files = buildACollectionOfTempFilesContainingText(fNumFilesUnderCM, sandboxDir);
        Collection<File> filesWithDirsRemoved = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                filesWithDirsRemoved.add(file);
            }
        }

        cmAdapter.add(files);
        assertThat(cmAdapter.getFileState(files), allValues(haveStatus(LocalStatus.ADDED)));

        String comments = "ADDED \"some\" files.";
        cmAdapter.checkin(files, comments);
        assertThat(cmAdapter.getFileState(files), allValues(haveStatus(LocalStatus.UNMODIFIED)));

        //Let's try to re-add the same files and watch the CM fail in a sensible way.
        try {
            cmAdapter.add(files);
            fail("You should not be able to add a set of files to the repository twice.");
        } catch (ConfigurationManagementException exception) {
            // Expected
        }

        //Remove the files and make sure we can add them again.
        cmAdapter.remove(files);
        // CHANGE FROM ORIGINAL TEST
        // When deleting a directory, TFS will delete all of the directory's contents.
        // However, only the directory itself is marked with a pending delete. No change
        // is reported for the contents. Instead of checking that all files are marked
        // with a pending Delete, verify that after checkin, all files are removed.
        assertThat(cmAdapter.getFileState(getImmediateChildrenFiles(files, sandboxDir)), allValues(haveStatus(LocalStatus.DELETED)));
        assertThat(cmAdapter.getFileState(getNestedChildrenFiles(files, sandboxDir)), allValues(haveStatus(LocalStatus.UNMODIFIED)));

        cmAdapter.checkin(files, "Removed files");

        for (File file : filesWithDirsRemoved) {
            if (!file.exists()) {
                FileCreation.createFileContainingText(file);
            }
        }
        // CHANGE FROM ORIGINAL TEST
        // Remove update call since we're not on SVN and don't need to workaround SVN issues.
        cmAdapter.add(files);
        assertThat(cmAdapter.getFileState(stripOutDirectories(files)), allValues(haveStatus(LocalStatus.ADDED)));

        cmAdapter.checkin(files, comments);
        //The files should now be back under version control.
        assertThat(cmAdapter.getFileState(stripOutDirectories(files)), allValues(haveStatus(LocalStatus.UNMODIFIED)));
    }

    @Test
    public void testRevisions() throws Exception {

        File primarySandboxDir = fSourceControlSetupRule.newSandbox();
        CMAdapter primaryAdapter = fSourceControlSetupRule.getCMAdapterFor(primarySandboxDir);

        File secondarySandboxDir = fSourceControlSetupRule.newSandbox();
        CMAdapter secondaryAdapter = fSourceControlSetupRule.getCMAdapterFor(secondarySandboxDir);

        Collection<File> filesInPrimarySandbox = addCollectionOfFilesToCM(primaryAdapter, primarySandboxDir);

        Collection<File> filesInSecondarySandbox = changeRoot(filesInPrimarySandbox, secondarySandboxDir, primarySandboxDir);
        assertThat(primaryAdapter.getFileState(filesInPrimarySandbox), allValues(haveStatus(LocalStatus.UNMODIFIED)));

        if (primaryAdapter.isFeatureSupported(AdapterSupportedFeature.IS_LATEST)) {
            // CHANGE FROM ORIGINAL TEST
            // The isLatest() values in the secondary adapter should be false until the user
            // syncs the new files created by the primary adapter.
            assertThat(secondaryAdapter.isLatest(filesInSecondarySandbox), allValues(are(false)));
        }

        if (secondaryAdapter.isFeatureSupported(AdapterSupportedFeature.LATEST_REVISION)) {
            Map<File, FileState> fileStates = secondaryAdapter.getFileState(filesInSecondarySandbox);
            for (FileState state : fileStates.values()) {
                //Files not under source control should return a null Revision instance.
                Revision revision = state.getRevision();
                assertThat(revision, is(nullValue()));
            }
        }

        secondaryAdapter.update(secondarySandboxDir);
        //Ensure that clashes are handled correctly.
        assertThat(secondaryAdapter.getFileState(filesInSecondarySandbox), allValues(haveStatus(LocalStatus.UNMODIFIED)));
        assertThat(primaryAdapter.getFileState(filesInPrimarySandbox), allValues(haveStatus(LocalStatus.UNMODIFIED)));

        primaryAdapter.checkout(filesInPrimarySandbox);
        FileCreation.modifyFiles(filesInPrimarySandbox);
        String comment = ("Checked in by testRevisions");
        primaryAdapter.checkin(filesInPrimarySandbox, comment);

        Map<File, FileState> primaryCurrentRevisions =
                primaryAdapter.getFileState(filesInPrimarySandbox);

        Map<File, FileState> secondaryCurrentRevisions =
                secondaryAdapter.getFileState(filesInSecondarySandbox);

        for (File file : primaryCurrentRevisions.keySet()) {

            if (file.isDirectory()) {
                continue;
            }

            String relativeLocation = FileUtil.getRelativePath(primarySandboxDir, file);
            File secondarySandboxFile = new File(secondarySandboxDir, relativeLocation);

            Revision primaryRevision = primaryCurrentRevisions.get(file).getRevision();
            Revision secondaryRevision = secondaryCurrentRevisions.get(secondarySandboxFile).getRevision();
            assertThat(primaryRevision, moreRecentThan(secondaryRevision));
            assertThat(secondaryRevision, not(moreRecentThan(secondaryRevision)));
        }

        if (primaryAdapter.isFeatureSupported(AdapterSupportedFeature.IS_LATEST)) {
            // CHANGE FROM ORIGINAL TEST
            // Strip out directories, since the directory itself has not been altered and will
            // be considered the latest revision.
            assertThat(secondaryAdapter.isLatest(stripOutDirectories(filesInSecondarySandbox)), allValues(are(false)));
        }

        secondaryAdapter.getLatest(filesInSecondarySandbox);

        if (primaryAdapter.isFeatureSupported(AdapterSupportedFeature.IS_LATEST)) {
            assertThat(secondaryAdapter.isLatest(filesInSecondarySandbox), allValues(are(true)));
        }

        secondaryAdapter.checkout(filesInSecondarySandbox);
        FileCreation.modifyFiles(filesInSecondarySandbox);

        secondaryAdapter.checkin(filesInSecondarySandbox, comment);

        if (primaryAdapter.isFeatureSupported(AdapterSupportedFeature.IS_LATEST)) {
            assertThat(secondaryAdapter.isLatest(filesInSecondarySandbox), allValues(are(true)));
            // CHANGE FROM ORIGINAL TEST
            // Strip out directories, since the directory itself has not been altered and will
            // be considered the latest revision.
            assertThat(primaryAdapter.isLatest(stripOutDirectories(filesInPrimarySandbox)), allValues(are(false)));
        }
        primaryAdapter.update(primarySandboxDir);

        if (primaryAdapter.isFeatureSupported(AdapterSupportedFeature.IS_LATEST)) {
            assertThat(primaryAdapter.isLatest(filesInPrimarySandbox), allValues(are(true)));
        }
        //Check that all files have a revision.
        for (File file : filesInPrimarySandbox) {
            assertThat(primaryAdapter.listRevisions(file), is(not(empty())));
        }
    }

    /**
     * This test will fail for source control systems which require exclusive file locking for editing.
     * If such behaviour is expected then it is ok for this test to fail.
     */
    @Test
    public void testConflictResolution() throws Exception {
        File primarySandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter primaryAdapter = fSourceControlSetupRule.getCMAdapterFor(primarySandbox);

        File secondarySandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter secondaryAdapter = fSourceControlSetupRule.getCMAdapterFor(secondarySandbox);

        Collection<File> filesInPrimarySandbox = addCollectionOfFilesToCM(primaryAdapter, primarySandbox);
        filesInPrimarySandbox = stripOutDirectories(filesInPrimarySandbox);
        generateConflictedFilesInPrimarySandbox(filesInPrimarySandbox, primaryAdapter, primarySandbox, secondaryAdapter, secondarySandbox);

        Collection<File> filesInSecondarySandbox = changeRoot(filesInPrimarySandbox, secondarySandbox, primarySandbox);

        assertThat(secondaryAdapter.getFileState(filesInSecondarySandbox), allValues(haveStatus(LocalStatus.CONFLICTED)));

        assumeTrue(secondaryAdapter.isFeatureSupported(AdapterSupportedFeature.RESOLVE) );

        for (final File file : filesInSecondarySandbox) {
            long contentsBeforeResolve = ChecksumGenerator.getCRC32Checksum(file);
            secondaryAdapter.resolveConflict(file);
            //Resolving the file shouldn't change it.
            assertThat(contentsBeforeResolve, equalTo(ChecksumGenerator.getCRC32Checksum(file)));
        }
        assertThat(secondaryAdapter.getFileState(filesInSecondarySandbox), allValues(haveStatus(LocalStatus.MODIFIED)));

        secondaryAdapter.checkin(secondarySandbox, "Resolved conflicts");
        //Now that the files are checked in they should be in the unmodified state.
        assertThat(secondaryAdapter.getFileState(filesInSecondarySandbox), allValues(haveStatus(LocalStatus.UNMODIFIED)));

    }

    private Collection<File> generateConflictedFilesInPrimarySandbox(Collection<File> filesInPrimarySandbox,
                                                                     CMAdapter primaryAdapter,
                                                                     File primarySandbox,
                                                                     CMAdapter secondaryAdapter,
                                                                     File secondarySandbox) throws Exception {

        Collection<File> filesInSecondarySandbox = changeRoot(filesInPrimarySandbox, secondarySandbox, primarySandbox);

        secondaryAdapter.update(secondarySandbox);

        filesInSecondarySandbox = stripOutDirectories(filesInSecondarySandbox);

        FileCreation.modifyFiles(filesInPrimarySandbox);
        FileCreation.modifyFiles(filesInSecondarySandbox);

        String comment = "Checking in files";
        primaryAdapter.checkin(filesInPrimarySandbox, comment);

        //Put a new file in to the sandbox and commit it to push the conflict cause more than one revision ahead.
        File newFile = FileCreation.createTempFileContainingText(primarySandbox);
        primaryAdapter.add(Collections.singleton(newFile));
        primaryAdapter.checkin(Collections.singleton(newFile), comment);

        try {
            secondaryAdapter.update(secondarySandbox);
        } catch (ConfigurationManagementException exception) {
            //In the case of a conflict an exception may or may not be thrown.
        }

        return filesInPrimarySandbox;
    }

    @Test
    public void testConflictSourceExport() throws Exception {
        File primarySandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter primaryAdapter = fSourceControlSetupRule.getCMAdapterFor(primarySandbox);

        File secondarySandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter secondaryAdapter = fSourceControlSetupRule.getCMAdapterFor(secondarySandbox);

        assumeTrue(primaryAdapter.isFeatureSupported(AdapterSupportedFeature.GET_CONFLICT_REVISION) );

        Collection<File> filesInPrimarySandbox = addCollectionOfFilesToCM(primaryAdapter, primarySandbox);
        filesInPrimarySandbox = stripOutDirectories(filesInPrimarySandbox);
        Map<File, Long> baseChecksums = ChecksumGenerator.getCRC32CheckSums(filesInPrimarySandbox);

        Collection<File> filesInSecondarySandbox = changeRoot(filesInPrimarySandbox, secondarySandbox, primarySandbox);

        generateConflictedFilesInPrimarySandbox(filesInPrimarySandbox, primaryAdapter, primarySandbox, secondaryAdapter, secondarySandbox);

        assertThat(secondaryAdapter.getFileState(filesInSecondarySandbox), allValues(haveStatus(LocalStatus.CONFLICTED)));

        Map<File, Long> conflictCausingFilesChecksums = ChecksumGenerator.getCRC32CheckSums(filesInPrimarySandbox);

        Map<File, Revision> theirsRevisionMap = new HashMap<>();
        Map<File, Revision> baseRevisionMap = new HashMap<>();

        for (File file : filesInSecondarySandbox) {
            ConflictedRevisions conflictedRevisions = secondaryAdapter.getRevisionCausingConflict(file);
            theirsRevisionMap.put(file, conflictedRevisions.getTheirsRevision());
            Revision baseRevision = conflictedRevisions.getBaseRevision();
            if (baseRevision != null) {
                baseRevisionMap.put(file, baseRevision);
            }
        }

        verifyExportMap(theirsRevisionMap, conflictCausingFilesChecksums, secondaryAdapter, secondarySandbox, primarySandbox);
        if (!baseRevisionMap.isEmpty()) {
            verifyExportMap(baseRevisionMap, baseChecksums, secondaryAdapter, secondarySandbox, primarySandbox);
        }
    }

    private void verifyExportMap(Map<File, Revision> revisionMap, Map<File, Long> expectedChecksums,
                                 CMAdapter secondaryAdapter,
                                 File secondarySandbox,
                                 File primarySandbox
    ) throws Exception {
        File theirsExportDir = fSourceControlSetupRule.newFolder();
        Map<File, File> exportMapping = new HashMap<>();
        Collection<File> filesInSandbox = revisionMap.keySet();
        for (File file : filesInSandbox) {
            File exportTarget = FileUtil.changeRoot(file, secondarySandbox, theirsExportDir);
            exportMapping.put(file, exportTarget);
        }
        secondaryAdapter.export(revisionMap, exportMapping);
        for (File file : filesInSandbox) {
            File conflictCausingFile = FileUtil.changeRoot(file, secondarySandbox, primarySandbox);
            File exportTarget = exportMapping.get(file);
            assertThat(
                    ChecksumGenerator.getCRC32Checksum(exportTarget),
                    equalTo(expectedChecksums.get(conflictCausingFile))
            );
        }
    }

    /*
    * This method takes a file collection and returns a collection which only contains the non-directory entries in
    * the original collection.
    */
    private static Collection<File> stripOutDirectories(Collection<File> files) {

        Collection<File> noDirs = new ArrayList<>();
        for (File file : files) {
            // CHANGE FROM ORIGINAL TEST
            // When TFS marks a file with the "remove" status, the file is actually
            // deleted from the local machine, and isFile() returns false. Add a
            // check against !file.exists() to the if condition.
            if (file.isFile() || !file.exists()) {
                noDirs.add(file);
            }
        }
        return noDirs;
    }


    @Test
    public void testRevert() throws Exception {

        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);

        assumeTrue(adapter.isFeatureSupported(AdapterSupportedFeature.GET_REVISION) );

        Collection<File> files = buildACollectionOfTempFilesContainingText(fNumFilesUnderCM, sandbox);

        adapter.add(files);

        String comment = "Test comment written by TAdapter";
        adapter.checkin(files, comment);

        Map<File, Long> originalChecksums = ChecksumGenerator.getCRC32CheckSums(files);

        Map<File, Revision> originalRevisions = getRevisionsFor(adapter, files);

        int numRevisions = 4;
        for (int i = 0; i < numRevisions; i++) {
            adapter.checkout(files);
            FileCreation.modifyFiles(files);
            adapter.checkin(files, comment);
        }

        adapter.getRevision(originalRevisions);

        Map<File, Long> checksumsAfterRevert = ChecksumGenerator.getCRC32CheckSums(files);
        for (File file : originalChecksums.keySet()) {
            //Check sum of file content should be reverted back to its original value.
            assertThat(originalChecksums.get(file), is(checksumsAfterRevert.get(file)));
        }
    }

    private static Map<File, Revision> getRevisionsFor(CMAdapter cmAdapter, Collection<File> files) throws ConfigurationManagementException {
        Map<File, Revision> revisionMap = new HashMap<>();
        for (Map.Entry<File, FileState> entry : cmAdapter.getFileState(files).entrySet()) {
            revisionMap.put(entry.getKey(), entry.getValue().getRevision());
        }
        return revisionMap;
    }

    @Test
    public void testExport() throws Exception {

        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);

        assumeTrue(adapter.isFeatureSupported(AdapterSupportedFeature.EXPORT) );

        Collection<File> files = addCollectionOfFilesToCM(adapter, sandbox);

        Map<File, Long> originalChecksums = ChecksumGenerator.getCRC32CheckSums(files);
        Map<File, Revision> revisionMap = getRevisionsFor(adapter, files);

        File exportDir = fSourceControlSetupRule.newFolder();

        Map<File, File> exportFiles = reRootFilePaths(files, exportDir, sandbox);

        adapter.export(revisionMap, exportFiles);
        Map<File, Long> exportChecksums = ChecksumGenerator.getCRC32CheckSums(exportFiles.values());

        assertThat(originalChecksums.values(), hasItems(exportChecksums.values()));

        assumeTrue(adapter.isFeatureSupported(AdapterSupportedFeature.MOVE) );

        File moveLocation = new File(sandbox, "Moved");
        Map<File, File> fileMoves = reRootFilePaths(
                files,
                moveLocation,
                sandbox
        );
        FileCreation.createDirectory(moveLocation);
        adapter.add(Collections.singleton(moveLocation));
        files = FileSorter.ascendingSort(files);
        for (File location : files) {

            File newLocation = fileMoves.get(location);

            if (!newLocation.exists()) {
                adapter.moveFile(location, newLocation);
            }
        }

        adapter.checkin(sandbox, "test");

        Map<File, File> movedExportFiles =
                reRootFilePaths(fileMoves.values(), exportDir, sandbox);

        Map<File, Revision> movedRevisionMap = new HashMap<>();
        for (File file : revisionMap.keySet()) {
            movedRevisionMap.put(fileMoves.get(file), revisionMap.get(file));
        }
        adapter.export(movedRevisionMap, movedExportFiles);
    }

    @Test
    public void testHasRepositorySpecifier() throws Exception {
        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);
        String repositorySpecifier = adapter.getRepositorySpecifier(sandbox);
        assertThat(repositorySpecifier, is(not(emptyString())));
    }

    @Test
    public void testBasic() throws Exception {

        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);
        File file = FileCreation.createTempFileContainingText(sandbox);

        Collection<File> files = Collections.singleton(file);

        // Add this file and check that it has been added.
        adapter.add(files);

        assertThat(adapter.getFileState(files), allValues(haveStatus(LocalStatus.ADDED)));


        String comment = "Initial revision created by unit test";

        adapter.checkin(files, comment);
        assertThat(adapter.getFileState(files), allValues(haveStatus(LocalStatus.UNMODIFIED)));

        // Checkout this file and make sure it is checked out.
        adapter.checkout(files);
        assertThat(adapter.getFileState(files), allValues(haveStatus(LocalStatus.UNMODIFIED)));

        if (adapter.isFeatureSupported(AdapterSupportedFeature.LOCK)) {
            assertThat(adapter.getFileState(files), allValues(areLocked()));
        }

        // Modify this file by adding some text to it.

        try (FileWriter w = new FileWriter(file)) {
            w.write("This file was modified to test the " + adapter.getSystemName() +
                    " fAdapter.");
        }

        // Check the file in and make sure it is checked in and that the revision has incremented to version 2.
        String comment2 = "Second revision created by unit test";

        adapter.checkin(files, comment2);

        assertThat(adapter.getFileState(files), allValues(haveStatus(LocalStatus.UNMODIFIED)));
        if (adapter.isFeatureSupported(AdapterSupportedFeature.LOCK)) {
            assertThat(adapter.getFileState(files), allValues(not(areLocked())));
        }

    }

    private Collection<File> addCollectionOfFilesToCM(CMAdapter adapter, File sanbdox)
            throws ConfigurationManagementException, IOException {

        Collection<File> files = buildACollectionOfTempFilesContainingText(fNumFilesUnderCM, sanbdox);
        adapter.add(files);
        assertThat(adapter.getFileState(files), allValues(haveStatus(LocalStatus.ADDED)));
        String comment = "ADDED some files.";
        adapter.checkin(files, comment);
        assertThat(adapter.getFileState(files), allValues(haveStatus(LocalStatus.UNMODIFIED)));
        assertThat(adapter.isStored(stripOutDirectories(files)), allValues(are(true)));
        return files;
    }

    // CHANGE FROM ORIGINAL TEST
    // Add a sandbox root argument to handle TFS behavior around directory deletes.
    private void removeCollectionOfFilesFromCM(Collection<File> files, File sandboxRoot, CMAdapter adapter)
            throws ConfigurationManagementException {

        adapter.remove(files);
        files = stripOutDirectories(files);
        // CHANGE FROM ORIGINAL TEST
        // When deleting a directory, TFS will delete all of the directory's contents.
        // However, only the directory itself is marked with a pending delete. No change
        // is reported for the contents. Instead of checking that all files are marked
        // with a pending Delete, verify that after checkin, all files are removed.
        assertThat(adapter.getFileState(getImmediateChildrenFiles(files, sandboxRoot)), allValues(haveStatus(LocalStatus.DELETED)));
        assertThat(adapter.getFileState(getNestedChildrenFiles(files, sandboxRoot)), allValues(haveStatus(LocalStatus.UNMODIFIED)));
    }

    @Test
    /* This test ensures that checked out files are modifiable. */
    public void testCheckinCheckOut() throws Exception {

        File sandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter adapter = fSourceControlSetupRule.getCMAdapterFor(sandbox);

        Collection<File> files = addCollectionOfFilesToCM(adapter, sandbox);

        adapter.checkout(files);
        FileCreation.modifyFiles(files);
        adapter.checkin(files, "Test check-in");
        adapter.checkout(files);
        FileCreation.modifyFiles(files);
        adapter.checkin(files, "Test");
        adapter.checkout(files);
        FileCreation.modifyFiles(files);
        adapter.checkin(sandbox, "Test recursive check-in");

        //It should be possible to check a file out after un-checking it out.
        adapter.checkout(files);
        adapter.uncheckout(files);
        adapter.checkout(files);
        FileCreation.modifyFiles(files);
        adapter.checkin(sandbox, "Test recursive check-in");
    }

    @Test
    /*
     * This tests ensures that checked out files can't be checked out in another sandbox.
     */
    public void testCheckoutLocking() throws Exception {

        File primarySandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter primaryAdapter = fSourceControlSetupRule.getCMAdapterFor(primarySandbox);

        File secondarySandbox = fSourceControlSetupRule.newSandbox();
        CMAdapter secondaryAdapter = fSourceControlSetupRule.getCMAdapterFor(secondarySandbox);

        assumeTrue(primaryAdapter.isFeatureSupported(AdapterSupportedFeature.LOCK) );

        Collection<File> files = addCollectionOfFilesToCM(primaryAdapter, primarySandbox);
        primaryAdapter.checkout(files);
        
        // CHANGE FROM ORIGINAL TEST
        // Secondary adapter needs to pull down new files before it can check them out.
        secondaryAdapter.update(secondarySandbox);

        try {
            secondaryAdapter.checkout(
                    changeRoot(files, secondarySandbox, primarySandbox)
            );
            fail("Checking out files in the second sandbox should throw an exception");
        } catch (ConfigurationManagementException exception) {
            //Expected
        }
        
        // CHANGE FROM ORIGINAL TEST
        // Unlock the files so that there's no interference with other tests
        // (ex: deleting directories with locked files)
        primaryAdapter.uncheckout(files);
    }

    private Map<File, File> reRootFilePaths(
            Collection<File> files, File newRoot, File oldRoot) throws IOException {

        Map<File, File> movedFiles = new HashMap<>();

        for (File file : files) {

            movedFiles.put(
                    file,
                    FileUtil.changeRoot(file, oldRoot, newRoot)
            );
        }

        return movedFiles;
    }

    protected Collection<File> changeRoot(Collection<File> files, File newRoot, File oldRoot) throws Exception {
        Map<File, File> fileMap = reRootFilePaths(files, newRoot, oldRoot);
        return fileMap.values();
    }

    private Collection<File> buildACollectionOfTempFilesContainingText(int numFiles, File root)
            throws ConfigurationManagementException, IOException {

        List<String> subFolders = new ArrayList<>();
        subFolders.add("");
        subFolders.add("+Level1Dir");

        subFolders.add(getDirectoryNameWithAtCharacter());
        subFolders.add("Level3Dir");

        File currentDirectory = root;

        Collection<File> files = new ArrayList<>();

        for (String subFolder : subFolders) {

            currentDirectory = new File(currentDirectory, subFolder);

            if (!subFolder.isEmpty()) {
                files.add(currentDirectory);
            }

            for (int counter = 0; counter < numFiles; counter++) {
                File file = FileCreation.createTempFileContainingText(currentDirectory);
                files.add(file);
            }
        }
        return files;
    }

    /*
     * Returns a collection which only contains the files that are immediate children of the root.
     */
    private static Collection<File> getImmediateChildrenFiles(Collection<File> files, File root) {
        Collection<File> children = new ArrayList<>();
        String rootPath = root.getAbsolutePath();
        for (File file : files) {
            String parentPath = file.getParentFile().getAbsolutePath();
            if (parentPath != null && parentPath.equals(rootPath)) {
                children.add(file);
            }
        }
        return children;
    }

    /*
     * Returns a collection which only contains the files that are "nested" children of the root
     * (below the immediate children, but not including the immediate children themselves).
     */
    private static Collection<File> getNestedChildrenFiles(Collection<File> files, File root) {
        Collection<File> nestedChildren = new ArrayList<>();
        String rootPath = root.getAbsolutePath();
        for (File file : files) {
            String parentPath = file.getParentFile().getAbsolutePath();
            if (parentPath != null && parentPath.startsWith(rootPath) && !parentPath.equals(rootPath)) {
                nestedChildren.add(file);
            }
        }
        return nestedChildren;
    }

    protected String getDirectoryNameWithAtCharacter() {
        return "@Level2Dir";
    }

}
