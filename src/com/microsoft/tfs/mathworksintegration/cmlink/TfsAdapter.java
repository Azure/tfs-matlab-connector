// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.mathworks.cmlink.api.AdapterSupportedFeature;
import com.mathworks.cmlink.api.ApplicationInteractor;
import com.mathworks.cmlink.api.ConfigurationManagementException;
import com.mathworks.cmlink.api.FileState;
import com.mathworks.cmlink.api.IntegerRevision;
import com.mathworks.cmlink.api.LocalStatus;
import com.mathworks.cmlink.api.Revision;
import com.mathworks.cmlink.api.customization.CoreAction;
import com.mathworks.cmlink.api.customization.CustomizationWidgetFactory;
import com.mathworks.cmlink.api.version.r14a.CMAdapter;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.CheckinException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpecParseException;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.mathworksintegration.cmlink.errorlisteners.TfsErrorListener;
import com.microsoft.tfs.mathworksintegration.cmlink.errorlisteners.TfsPendDeleteErrorListener;
import com.microsoft.tfs.mathworksintegration.cmlink.errorlisteners.TfsPendUndoErrorListener;

/**
 * Implementation of the {@link CMAdapter} interface which performs
 * TFS version control file operations.
 */
public class TfsAdapter extends TfsBase implements CMAdapter {

    private static final int MaxHistoryCount = Integer.MAX_VALUE;
    private static final char ChangesetRangeDelimiter = '-';

    private Workspace workspace;
    private final Collection<AdapterSupportedFeature> supportedFeatures;
    private final File sandboxRoot;
    private final ICheckinDataProvider checkinDataProvider;

    /**
     * Initializes a TfsAdapter instance.
     * @param rootDirectory
     *     The sandbox root directory.
     * @param applicationInteractor
     *     The {@link ApplicationInteractor} to use with this adapter.
     * @param checkinDataProvider
     *     The {@link ICheckinDataProvider} to use to get checkin information.
     */
    public TfsAdapter(File rootDirectory, ApplicationInteractor applicationInteractor, 
        ICheckinDataProvider checkinDataProvider) {
        super();

        this.sandboxRoot = rootDirectory;
        this.checkinDataProvider = checkinDataProvider;

        this.supportedFeatures = EnumSet.of(
            AdapterSupportedFeature.CUSTOM_COMMIT_COMMENT_DIALOG,
            AdapterSupportedFeature.EXPORT,
            AdapterSupportedFeature.FOLDERS_VERSIONED,
            AdapterSupportedFeature.GET_CONFLICT_REVISION,
            AdapterSupportedFeature.GET_REVISION,
            AdapterSupportedFeature.IS_LATEST,
            AdapterSupportedFeature.LATEST_REVISION,
            AdapterSupportedFeature.LIST_REVISIONS,
            AdapterSupportedFeature.LOCK,
            AdapterSupportedFeature.MOVE,
            AdapterSupportedFeature.RESOLVE
            );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildCustomActions(CustomizationWidgetFactory widgetFactory) {
        super.buildCustomActions(widgetFactory);

        widgetFactory.createActionWidget("Branch", null,
            new CoreAction() {
            @Override
            public void execute() throws ConfigurationManagementException {
                branchWithPrompt();
            }

            @Override
            public String getDescription() {
                return "Branch a file or directory.";
            }

            @Override
            public boolean canCancel() {
                return true;
            }
        });

        widgetFactory.createActionWidget("Merge", null,
            new CoreAction() {
            @Override
            public void execute() throws ConfigurationManagementException {
                mergeWithPrompt();
            }

            @Override
            public String getDescription() {
                return "Merge a file or directory.";
            }

            @Override
            public boolean canCancel() {
                return true;
            }
        });
    }

    // These are public to enable testing.
    /**
     * Performs a TFS branch operation.
     * @param branchInfo
     *     Contains the information needed to perform the branch operation.
     * @throws ConfigurationManagementException
     */
    public void branch(BranchMergeInformation branchInfo) throws ConfigurationManagementException {
        String changesetSpecifier = branchInfo.getChangesetSpecifier();
        VersionSpec versionSpec = null;
        if (changesetSpecifier == null || changesetSpecifier.isEmpty()) {
            // If blank, use the latest version.
            versionSpec = LatestVersionSpec.INSTANCE;
        }
        else {
            try {
                versionSpec = VersionSpec.parseSingleVersionFromSpec(changesetSpecifier, null);
            }
            catch (VersionSpecParseException ex) {
                throw new ConfigurationManagementException(ex);
            }
        }

        TfsErrorListener errorListener = new TfsErrorListener();
        AddErrorListener(errorListener);
        try {
            getWorkspace().pendBranch(
                branchInfo.getSourcePath(),
                branchInfo.getTargetPath(),
                versionSpec,
                LockLevel.UNCHANGED,
                RecursionType.FULL,
                GetOptions.NONE,
                PendChangesOptions.NONE);
        }
        finally {
            RemoveErrorListenerAndProcessErrors(errorListener);
        }
    }

    /**
     * Performs a TFS merge operation.
     * @param mergeInfo
     *     Contains the information needed to perform the merge operation.
     * @throws ConfigurationManagementException
     */
    public void merge(BranchMergeInformation mergeInfo) throws ConfigurationManagementException {
        VersionSpec versionFrom = null;
        VersionSpec versionTo = null;
        String changesetSpecifier = mergeInfo.getChangesetSpecifier();
        if (changesetSpecifier == null || changesetSpecifier.isEmpty()) {
            // If blank, merge all changes.
            versionTo = LatestVersionSpec.INSTANCE;
        }
        else {
            try {
                int delimiterIndex = changesetSpecifier.indexOf(ChangesetRangeDelimiter);
                if (delimiterIndex == -1) {
                    // If a single changeset is specified, only merge the one change.
                    versionFrom = VersionSpec.parseSingleVersionFromSpec(changesetSpecifier, null);
                    versionTo = versionFrom;
                }
                else {
                    // TODO: Could probably do some more input validation. 
                    String fromChangeset = changesetSpecifier.substring(0, delimiterIndex).trim();
                    String toChangeset = changesetSpecifier.substring(delimiterIndex + 1).trim();

                    versionFrom = VersionSpec.parseSingleVersionFromSpec(fromChangeset, null);
                    versionTo = VersionSpec.parseSingleVersionFromSpec(toChangeset, null);
                }
            }
            catch (VersionSpecParseException ex) {
                throw new ConfigurationManagementException(ex);
            }
        }
        getWorkspace().merge(mergeInfo.getSourcePath(), 
            mergeInfo.getTargetPath(),
            versionFrom,
            versionTo,
            LockLevel.UNCHANGED,
            RecursionType.FULL,
            MergeFlags.NONE);
    }

    // Prompt the user for branch information and perform the branch operation.
    private void branchWithPrompt() throws ConfigurationManagementException {
        BranchMergeInformation branchInfo = promptForBranchOrMergeInformation("Branch", 
            "Changeset (Leave blank for latest version):");

        if (branchInfo != null) {
            branch(branchInfo);
        }
    }

    // Prompt the user for merge information and perform the merge operation.
    private void mergeWithPrompt() throws ConfigurationManagementException {
        BranchMergeInformation mergeInfo = promptForBranchOrMergeInformation("Merge", 
            String.format("Changeset (For a range, use the format C1%cC2. Leave blank for all changes.)",
                ChangesetRangeDelimiter));

        if (mergeInfo != null) {
            merge(mergeInfo);
        }
    }

    // Prompt the user for information required to perform either a branch or a merge operation.
    // TODO: Investigate using TEE UIs.
    private BranchMergeInformation promptForBranchOrMergeInformation(String title, String changeLabel) {
        final JLabel sourceLabel = new JLabel("Source Path:");
        final JTextField sourceText = new JTextField();
        final JButton sourceTextBrowseButton = new JButton("Browse");
        final JPanel sourcePanel = new JPanel();
        sourcePanel.setLayout(new BoxLayout(sourcePanel, BoxLayout.X_AXIS));
        sourcePanel.add(sourceText);
        sourcePanel.add(sourceTextBrowseButton);	
        final JFileChooser sourceFileChooser = new JFileChooser();
        sourceFileChooser.setCurrentDirectory(this.sandboxRoot);
        sourceFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        sourceTextBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int fileResult = sourceFileChooser.showOpenDialog(sourceTextBrowseButton);
                if (fileResult == JFileChooser.APPROVE_OPTION) {
                    sourceText.setText(sourceFileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        final JLabel changesetLabel = new JLabel(changeLabel);
        final JTextField changesetText = new JTextField();
        final JLabel targetLabel = new JLabel("Target Path:");
        final JTextField targetText = new JTextField();
        final JButton targetTextBrowseButton = new JButton("Browse");
        final JPanel targetPanel = new JPanel();
        targetPanel.setLayout(new BoxLayout(targetPanel, BoxLayout.X_AXIS));
        targetPanel.add(targetText);
        targetPanel.add(targetTextBrowseButton);	
        final JFileChooser targetFileChooser = new JFileChooser();
        targetFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        targetFileChooser.setCurrentDirectory(this.sandboxRoot);
        targetTextBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int fileResult = targetFileChooser.showOpenDialog(targetTextBrowseButton);
                if (fileResult == JFileChooser.APPROVE_OPTION) {
                    targetText.setText(targetFileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        JComponent[] components = new JComponent[] { sourceLabel, sourcePanel, changesetLabel, changesetText, 
            targetLabel, targetPanel };

        int result = JOptionPane.showConfirmDialog(null, components, title, JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE, null);

        if (result == JOptionPane.CANCEL_OPTION) {
            return null;
        }

        String sourcePath = sourceText.getText().trim();
        String targetPath = targetText.getText().trim();
        String changesetSpecifier = changesetText.getText().trim();

        return new BranchMergeInformation(sourcePath, targetPath, changesetSpecifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect() throws ConfigurationManagementException {
        super.connect();
        this.workspace = Utilities.getWorkspaceForLocalPath(this.sandboxRoot.getAbsolutePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(Collection<File> files) throws ConfigurationManagementException {
        ArrayList<String> newFilePaths = new ArrayList<String>();
        for (File file : files) {
            String filePath = file.getAbsolutePath();
            newFilePaths.add(filePath);
        }

        String[] pathArray = newFilePaths.toArray(new String[newFilePaths.size()]);
        if (pathArray.length > 0) {
            TfsErrorListener errorListener = new TfsErrorListener();
            AddErrorListener(errorListener);
            try {
            	getWorkspace().pendAdd(pathArray, false, null, LockLevel.UNCHANGED, GetOptions.NONE, 
                    PendChangesOptions.NONE);
            }
            finally {
                RemoveErrorListenerAndProcessErrors(errorListener);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTag(Collection<File> files, String tagName, String comment) 
        throws ConfigurationManagementException {
        // This method will be deprecated - no implementation.
        throw new ConfigurationManagementException("Tag features not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTagRecursively(File directory, String tagName, String comment) 
        throws ConfigurationManagementException {
        // This method will be deprecated - no implementation.
    	throw new ConfigurationManagementException("Tag features not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canCommitEmpty() throws ConfigurationManagementException {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkin(Collection<File> files, String comment) throws ConfigurationManagementException {
        ArrayList<ItemSpec> fileSpecs = new ArrayList<ItemSpec>();
        for (File file : files) {
            fileSpecs.add(new ItemSpec(file.getAbsolutePath(), RecursionType.NONE));
        }
        ItemSpec[] specArray = fileSpecs.toArray(new ItemSpec[fileSpecs.size()]);
        checkin(specArray, comment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkin(File directory, String comment) throws ConfigurationManagementException {
        ItemSpec[] dirSpec = new ItemSpec[] { new ItemSpec(directory.getAbsolutePath(), RecursionType.FULL) };
        checkin(dirSpec, comment);
    }

    // Get checkin data and checkin the specified files.
    private void checkin(ItemSpec[] fileSpecs, String comment) throws ConfigurationManagementException {
        scanForChanges();

        if (fileSpecs.length > 0) {
            PendingSet pendingSet = getWorkspace().getPendingChanges(fileSpecs, false);
            if (pendingSet != null)
            {
                PendingChange[] pendingChanges = pendingSet.getPendingChanges();
                if (pendingChanges != null)
                {
                    CheckinData checkinData = this.checkinDataProvider.getData();
                    if (!checkinData.shouldSubmit()) {
                        // User hit Cancel, so abort
                        return;
                    }
                    // If a comment is provided to this method, use it.  Otherwise get it from the ICheckinDataProvider.
                    String checkinComment = comment != null && !comment.isEmpty() ? comment : checkinData.getCheckinComment();
                    // Get the TFS WorkItems to associate with the checkin.
                    int[] workItemIds = checkinData.getWorkItemIds();
                    WorkItemCheckinInfo[] associatedWorkItems = new WorkItemCheckinInfo[workItemIds.length];
                    for (int i = 0; i < workItemIds.length; i++) {
                        WorkItem workItem = Utilities.getTfsConnection().getWorkItemClient().getWorkItemByID(workItemIds[i]);
                        if (workItem == null) {
                            throw new ConfigurationManagementException("WorkItem " + workItemIds[i] + " not found.");
                        }
                        associatedWorkItems[i] = new WorkItemCheckinInfo(workItem);
                    }

                    try {
                    	getWorkspace().checkIn(pendingChanges, checkinComment, null, associatedWorkItems, null);
                    }
                    catch (CheckinException ex) {
                        throw new ConfigurationManagementException(ex);
                    }
                }
            }
        }
    }

    // Force a scan of the workspace to pick up any recent changes.
    // TODO: Investigate the scan() and isScanNecessary() methods for possible improvement.
    private void scanForChanges() throws ConfigurationManagementException {
        try { 
        	getWorkspace().getWorkspaceWatcher().forceFullScan(); 
        }
        catch (IOException ex) { 
            throw new ConfigurationManagementException(ex); 
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkout(Collection<File> files) throws ConfigurationManagementException {
        // NOTE: This method is tied to the LOCK adapter feature. We explicitly request a lock
        // rather than doing the Visual Studio "Checkout for edit" operation.

        ArrayList<ItemSpec> fileSpecs = new ArrayList<ItemSpec>();
        for (File file : files) {
            fileSpecs.add(new ItemSpec(file.getAbsolutePath(), RecursionType.NONE));
        }

        ItemSpec[] specArray = fileSpecs.toArray(new ItemSpec[fileSpecs.size()]);
        if (specArray.length > 0) {
            TfsErrorListener errorListener = new TfsErrorListener();
            AddErrorListener(errorListener);
            try {
            	getWorkspace().setLock(specArray, LockLevel.CHECKIN, GetOptions.NONE, PendChangesOptions.NONE);
            }
            finally {
                RemoveErrorListenerAndProcessErrors(errorListener);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean doTagsNeedComments() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void export(Map<File, Revision> revisionMap, Map<File, File> destinationMap) 
        throws ConfigurationManagementException {

        for (Map.Entry<File, Revision> entry : revisionMap.entrySet()) {
            File file = entry.getKey();
            Revision revision = entry.getValue();
            File destinationFile = destinationMap.get(file);

            String itemPath = getPathFromRevision(revision);
            VersionSpec versionSpec = VersionSpec.parseSingleVersionFromSpec(revision.getStringRepresentation(), null);
            Item item = getWorkspace().getClient().getItem(itemPath, versionSpec, DeletedState.NON_DELETED,
                GetItemsOptions.INCLUDE_SOURCE_RENAMES);
            // Can't download a directory
            if (item.getItemType() == ItemType.FILE) {
                item.downloadFile(getWorkspace().getClient(), destinationFile.getAbsolutePath());
            }
        }
    }

    // Get the path associated with a file revision. This is needed if the file was ever renamed.
    private String getPathFromRevision(Revision revision) throws ConfigurationManagementException {
        Map<String, String> revisionInfo = revision.getRevisionInfo();
        if (revisionInfo != null && revisionInfo.containsKey(Utilities.RevisionInfoKey_Path)) {
            return revisionInfo.get(Utilities.RevisionInfoKey_Path);
        }
        throw new ConfigurationManagementException("Internal error: Revision did not have path data.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<File, FileState> getFileState(Collection<File> files) throws ConfigurationManagementException {
        Map<File, FileState> fileStateMap = new HashMap<>();
        ArrayList<ItemSpec> fileSpecs = new ArrayList<ItemSpec>();

        for (File file : files) {
            fileSpecs.add(new ItemSpec(file.getAbsolutePath(), RecursionType.NONE));
        }

        ItemSpec[] specArray = fileSpecs.toArray(new ItemSpec[fileSpecs.size()]);
        if (specArray.length > 0) {

            Map<String, TfsFileState> statesByPath = getTrackedFileStatesByPath(specArray);
            for (File file : files) {
                String path = file.getAbsolutePath();
                if (statesByPath.containsKey(path)) {
                    fileStateMap.put(file, statesByPath.get(path));
                }
                else {
                    // This file is not tracked by source control. Add an empty TfsFileState
                    // to the map to indicate this.
                    fileStateMap.put(file, new TfsFileState());
                }
            }
        }

        return fileStateMap;
    }

    // Gets the TFS file state of the specified files, if they're tracked by TFS.
    private Map<String, TfsFileState> getTrackedFileStatesByPath(ItemSpec[] fileSpecs) 
        throws ConfigurationManagementException {
        // TODO: Revisit algorithm. Goal was to minimize the # of server calls,
        // but this results in more looping through collections client side.  
        // Might also be able to reorganize to improve re-use between the recursive and
        // non-recursive callers too.

        scanForChanges();

        Map<String, TfsFileState> fileStateMap = new HashMap<String, TfsFileState>();

        ExtendedItem[][] extendedItemsPerItemSpec = getWorkspace().getExtendedItems(
            fileSpecs,
            DeletedState.NON_DELETED, 
            ItemType.ANY,
            GetItemsOptions.NONE);
        boolean isRecursive = fileSpecs.length == 1 && fileSpecs[0].getRecursionType() == RecursionType.FULL;

        Map<String, Conflict> conflictsByLocalPath = new HashMap<String, Conflict>();

        Map<String, ExtendedItem> extendedItemsByPath = new HashMap<String, ExtendedItem>();
        for (ExtendedItem[] itemSpecExtendedItems : extendedItemsPerItemSpec) {
            // In the recursive case, we only get 1 ExtendedItem[], with one ExtendedItem per file
            if (isRecursive) {
                for (ExtendedItem extendedItem : itemSpecExtendedItems) {
                    // For a pending Delete, extendedItem.getLocalItem() returns null instead of the file path,
                    // so use the workspace's mapping function.
                    String localPath = getWorkspace().getMappedLocalPath(extendedItem.getTargetServerItem());
                    extendedItemsByPath.put(localPath, extendedItem);
                }
            }
            else {
                // Size 0 means the file is not tracked by source control.
                // In the non-recursive case, we should only have 1 ExtendedItem per ItemSpec.
                if (itemSpecExtendedItems.length > 0) {
                    ExtendedItem extendedItem = itemSpecExtendedItems[0];
                    String localPath = getWorkspace().getMappedLocalPath(extendedItem.getTargetServerItem());
                    extendedItemsByPath.put(localPath, extendedItem);
                }
            }
        }

        Conflict[] conflicts = getWorkspace().queryConflicts(null);
        if (conflicts != null) {
            for (Conflict conflict : conflicts) {
                conflictsByLocalPath.put(conflict.getTargetLocalItem(), conflict);
            }
        }

        for (Map.Entry<String, ExtendedItem> entry : extendedItemsByPath.entrySet()) {
            String localPath = entry.getKey();
            ExtendedItem extendedItem = entry.getValue();
            Conflict conflict = null;
            if (conflictsByLocalPath.containsKey(localPath)) {
                conflict = conflictsByLocalPath.get(localPath);
            }
            fileStateMap.put(localPath, new TfsFileState(extendedItem, conflict));
        }

        return fileStateMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getForbiddenFileNames() {
        // TODO: Are there any others?
        ArrayList<String> forbiddenNames = new ArrayList<String>();
        forbiddenNames.add(Utilities.TfsLocalWorkspaceFolder);

        return forbiddenNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getLatest(Collection<File> files) throws ConfigurationManagementException {
        ArrayList<GetRequest> getRequests = new ArrayList<GetRequest>();
        for (File file : files) {
            ItemSpec spec = new ItemSpec(file.getAbsolutePath(), RecursionType.NONE);
            getRequests.add(new GetRequest(spec, LatestVersionSpec.INSTANCE));
        }

        GetRequest[] requestArray = getRequests.toArray(new GetRequest[getRequests.size()]);
        if (requestArray.length > 0) {
        	getWorkspace().get(requestArray, GetOptions.NONE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRepositorySpecifier(File sandboxDirectory) throws ConfigurationManagementException {
        return getWorkspace().getMappedServerPath(sandboxDirectory.getAbsolutePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getRevision(Map<File, Revision> revisions) throws ConfigurationManagementException {
        ArrayList<GetRequest> getRequests = new ArrayList<GetRequest>();
        for (Map.Entry<File, Revision> entry : revisions.entrySet()) {
            Revision revision = entry.getValue();
            String path = getPathFromRevision(revision);
            ItemSpec spec = new ItemSpec(path, RecursionType.NONE);
            getRequests.add(new GetRequest(spec, 
                VersionSpec.parseSingleVersionFromSpec(revision.getStringRepresentation(), null)));
        }

        GetRequest[] requestArray = getRequests.toArray(new GetRequest[getRequests.size()]);
        if (requestArray.length > 0) {
        	getWorkspace().get(requestArray, GetOptions.NONE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Revision getRevisionCausingConflict(File file) throws ConfigurationManagementException {
        FileState fileState = getFileState(Collections.singleton(file)).get(file);

        if (fileState.getLocalStatus() != LocalStatus.CONFLICTED) {
            throw new ConfigurationManagementException("File " + file + " is not conflicted");
        }
        if(TfsFileState.class.isInstance(fileState)) {
            TfsFileState tfsState = (TfsFileState)fileState;
            return tfsState.getConflictRevision();
        }
        else
        {
            throw new ConfigurationManagementException(
                "Internal error: File State map contained an unexpected type.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<File, FileState> getStateForAllKnownFilesRecursively(File root) 
        throws ConfigurationManagementException {

        Map<File, FileState> fileStateMap = new HashMap<File, FileState>();

        ItemSpec fileSpec = new ItemSpec(root.getAbsolutePath(), RecursionType.FULL);
        Map<String, TfsFileState> statesByPath = getTrackedFileStatesByPath(new ItemSpec[] { fileSpec });
        for (Map.Entry<String, TfsFileState> entry : statesByPath.entrySet()) {
            String path = entry.getKey();
            fileStateMap.put(new File(path), statesByPath.get(path));
        }

        return fileStateMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getTags(File file) throws ConfigurationManagementException {
        // This method will be deprecated - no implementation.
    	throw new ConfigurationManagementException("Tag features not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFeatureSupported(AdapterSupportedFeature feature) {
        return this.supportedFeatures.contains(feature);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<File, Boolean> isLatest(Collection<File> files) throws ConfigurationManagementException {
        Map<File, FileState> states = getFileState(files);
        Map<File, Boolean> isLatestMap = new HashMap<File, Boolean>();

        for (Map.Entry<File, FileState> entry : states.entrySet()) {
            FileState state = entry.getValue();
            if(TfsFileState.class.isInstance(state)) {
                TfsFileState tfsState = (TfsFileState)state;
                isLatestMap.put(entry.getKey(), tfsState.isLatest());
            }
            else
            {
                throw new ConfigurationManagementException(
                    "Internal error: File State map contained an unexpected type.");
            }
        }

        return isLatestMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<File, Boolean> isStored(Collection<File> files) throws ConfigurationManagementException {
        HashMap<File, Boolean> isStoredMap = new HashMap<File, Boolean>();

        for (File file : files) {
            String serverPath = getWorkspace().getMappedServerPath(file.getAbsolutePath());
            boolean isMapped = getWorkspace().serverPathExists(serverPath);
            isStoredMap.put(file, isMapped);
        }

        return isStoredMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Revision> listRevisions(File file) throws ConfigurationManagementException {
        ArrayList<Revision> revisions = new ArrayList<Revision>();
        Changeset[] changesets;

        try {
            // TODO: Figure out how to get full history when a file has been branched. This
            // implementation only gets the history up to the branch point.
            // Investigate the getBranchHistory() method.
            changesets = getWorkspace().queryHistory(
                file.getAbsolutePath(),
                LatestVersionSpec.INSTANCE,
                0,
                RecursionType.NONE,
                null,
                null,
                null,
                MaxHistoryCount,
                true,
                false,
                false,
                false);
        }
        catch (ServerPathFormatException ex) {
            throw new ConfigurationManagementException(ex);
        }

        if (changesets != null) {
            for (Changeset change : changesets) {
                HashMap<String, String> revisionInfo = new HashMap<String, String>();
                revisionInfo.put(Utilities.RevisionInfoKey_User, change.getCommitter());
                revisionInfo.put(Utilities.RevisionInfoKey_Date, DateFormat.getInstance().format(change.getDate().getTime()));

                StringBuilder builder = new StringBuilder();
                Change[] changes = change.getChanges();
                if (changes.length > 0) {
                    for (Change c : changes) {
                        builder.append(c.getChangeType().toUIString(true));
                        builder.append(",");
                    }
                    builder.setLength(builder.length() - 1); // Trim last ',' character

                    // The path is stored on the Revision because if a file is renamed, the original  
                    // path is required for some operations.
                    // TODO - Is there a scenario where multiple Changes will have different paths?
                    revisionInfo.put(Utilities.RevisionInfoKey_Path, changes[0].getItem().getServerItem());
                }
                revisionInfo.put(Utilities.RevisionInfoKey_Changes, builder.toString());
                revisionInfo.put(Utilities.RevisionInfoKey_Comment, change.getComment());

                revisions.add(new IntegerRevision(change.getChangesetID(), revisionInfo));
            }
        }

        return revisions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveFile(File oldLocation, File newLocation) throws ConfigurationManagementException {
        TfsErrorListener errorListener = new TfsErrorListener();
        AddErrorListener(errorListener);
        try {
        	getWorkspace().pendRename(
                oldLocation.getAbsolutePath(),
                newLocation.getAbsolutePath(),
                LockLevel.UNCHANGED,
                GetOptions.NONE,
                false,
                PendChangesOptions.NONE);
        }
        finally {
            RemoveErrorListenerAndProcessErrors(errorListener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Collection<File> files) throws ConfigurationManagementException {
        ArrayList<ItemSpec> fileSpecs = new ArrayList<ItemSpec>();
        for (File file : files) {
            // For delete, the directory's contents are deleted too, even with RecusrionType.NONE.
            fileSpecs.add(new ItemSpec(file.getAbsolutePath(), RecursionType.NONE));
        }

        ItemSpec[] specArray = fileSpecs.toArray(new ItemSpec[fileSpecs.size()]);
        if (specArray.length > 0) {
            TfsPendDeleteErrorListener errorListener = new TfsPendDeleteErrorListener();
            AddErrorListener(errorListener);
            try {
            	getWorkspace().pendDelete(
                    fileSpecs.toArray(new ItemSpec[fileSpecs.size()]),
                    LockLevel.UNCHANGED, 
                    GetOptions.NONE,
                    PendChangesOptions.NONE);
            }
            finally {
                RemoveErrorListenerAndProcessErrors(errorListener);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeTag(Collection<File> files, String tagName, String comment) throws ConfigurationManagementException {
        // This method will be deprecated - no implementation.
    	throw new ConfigurationManagementException("Tag features not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeTag(String tagName, String comment, File fileInSandbox) throws ConfigurationManagementException {
        // This method will be deprecated - no implementation.
    	throw new ConfigurationManagementException("Tag features not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resolveConflict(File file) throws ConfigurationManagementException {
        // SVN example just accepts the sandbox copy. Assume the user has compared the conflicting versions
        // and made their changes.
        Conflict conflict = getConflictForFile(file);

        if (conflict.getType().getValue() == ConflictType.LOCAL.getValue()) {
            // Seems that in order to resolve a LOCAL conflict, you need to re-issue a get call
            // and re-issue the conflict query to refresh it to a GET/CHECKIN/etc. conflict before
            // it can be resolved.
            // TODO: Is there a better way to handle this?
            getLatest(Collections.singleton(file));
            conflict = getConflictForFile(file);
        }
        conflict.setResolution(Resolution.ACCEPT_YOURS);
        getWorkspace().resolveConflict(conflict);
        if (!conflict.isResolved())	{
            // TODO: Is there a listener to get details on why? Failing to resolve a LOCAL
            // conflict doesn't seem to trigger anything with the NonFatalErrorListener.
            throw new ConfigurationManagementException("Failed to resolve conflict");
        }
    }

    // Get the conflict information for the specified file.
    private Conflict getConflictForFile(File file) throws ConfigurationManagementException {
        FileState fileState = getFileState(Collections.singleton(file)).get(file);

        if (fileState.getLocalStatus() != LocalStatus.CONFLICTED) {
            throw new ConfigurationManagementException("File " + file + " is not conflicted");
        }
        if(TfsFileState.class.isInstance(fileState)) {
            TfsFileState tfsState = (TfsFileState)fileState;
            Conflict conflict = tfsState.getConflict();

            return conflict;
        }
        else
        {
            throw new ConfigurationManagementException(
                "Internal error: File State map contained an unexpected type.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uncheckout(Collection<File> files) throws ConfigurationManagementException {
        ArrayList<ItemSpec> fileSpecs = new ArrayList<ItemSpec>();
        for (File file : files) {
            // Want full recursion on undo based on SDK test cases
            fileSpecs.add(new ItemSpec(file.getAbsolutePath(), RecursionType.FULL));
        }

        ItemSpec[] specArray = fileSpecs.toArray(new ItemSpec[fileSpecs.size()]);
        if (specArray.length > 0) {
            TfsPendUndoErrorListener errorListener = new TfsPendUndoErrorListener();
            AddErrorListener(errorListener);
            try {
            	getWorkspace().undo(specArray, GetOptions.NONE);
            }
            finally {
                RemoveErrorListenerAndProcessErrors(errorListener);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(File directory) throws ConfigurationManagementException {
        ItemSpec spec = new ItemSpec(directory.getAbsolutePath(), RecursionType.FULL);
        GetRequest request = new GetRequest(spec, LatestVersionSpec.INSTANCE);

        // Use GET_ALL to force re-download. The MATLAB UI pops up a warning about this,
        // indicating this is the expected behavior.
        getWorkspace().get(request, GetOptions.GET_ALL);
    }

    // Add a listener that gets information about errors that occur during TFS operations.
    private void AddErrorListener(TfsErrorListener errorListener) throws ConfigurationManagementException {
    	getWorkspace().getClient().getEventEngine().addNonFatalErrorListener(errorListener);
    }

    // Remove the listener that gets information about errors that occur during TFS operations.
    private void RemoveErrorListenerAndProcessErrors(TfsErrorListener errorListener) 
        throws ConfigurationManagementException {
    	getWorkspace().getClient().getEventEngine().removeNonFatalErrorListener(errorListener);
        errorListener.ProcessErrors();
    }
    
    // Gets the cached Workspace for this adapter, refreshing it if necessary.
    private Workspace getWorkspace() throws ConfigurationManagementException {
        // Refresh the cached workspace if the TFS connection has changed.
        if (this.workspace.getClient().getConnection().isClosed()) {
            Workspace newWorkspace = Utilities.getWorkspaceForLocalPath(this.sandboxRoot.getAbsolutePath());
            if (newWorkspace == null) {
            	throw new ConfigurationManagementException("No Workspace found for directory " +
            	    this.sandboxRoot.getAbsolutePath() + " under current TFS connection.");
            }
            this.workspace = newWorkspace;
        }
        return this.workspace;
    }
}
