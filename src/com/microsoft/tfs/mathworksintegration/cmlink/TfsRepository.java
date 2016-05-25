// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import java.awt.Frame;
import java.io.File;
import java.util.Collection;
import java.util.EnumSet;

import javax.swing.JOptionPane;

import com.mathworks.cmlink.api.ApplicationInteractor;
import com.mathworks.cmlink.api.ConfigurationManagementException;
import com.mathworks.cmlink.api.RepositorySupportedFeature;
import com.mathworks.cmlink.api.version.r14a.CMRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;

/**
 * Implementation of the {@link CMRepository} interface which performs 
 * TFS Workspace management operations.
 */
public class TfsRepository extends TfsBase implements CMRepository {

    private final Collection<RepositorySupportedFeature> supportedFeatures;

    /**
     * Initializes a TfsRepository instance.
     * @param interactor
     *     The {@link ApplicationInteractor} to use with this TfsRepository.
     */
    public TfsRepository(ApplicationInteractor interactor) {
        super();

        this.supportedFeatures = EnumSet.of(
            RepositorySupportedFeature.REPOSITORY_BROWSER
            );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String browseForRepositoryPath(String currentlySelectedPath, Frame parentFrame) 
        throws ConfigurationManagementException {
        // TODO: Investigate if there's a TEE file browsing UI.
        String path = (String) JOptionPane.showInputDialog(
            parentFrame,
            "Specify a TFS Project Path (ex: $/ProjectName/Folder)",
            "TFS Project Path",
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            currentlySelectedPath);
        if (path == null) {
            path = currentlySelectedPath;
        }

        return path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void convertFolderToSandbox(File location) throws ConfigurationManagementException {
        // TODO: Before we can create a Workspace at the specified location, we'd need to figure 
        // out the correct TFS server path to map it to.  Since that information isn't passed in,
        // we could use a popup?  But in that case, why wouldn't the user just invoke the
        // retrieveSandboxFromRepository() method instead?
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFeatureSupported(RepositorySupportedFeature feature) {
        return this.supportedFeatures.contains(feature);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void retrieveSandboxFromRepository(String repositorySpecifier, File sandboxRoot) 
        throws ConfigurationManagementException {

        if (!isReady()) {
            connect();
        }

        VersionControlClient versionControlClient = Utilities.getTfsConnection().getVersionControlClient();
        Workspace workspace = Utilities.getWorkspaceForLocalPath(sandboxRoot.getAbsolutePath());
        if (workspace != null) {
            throw new ConfigurationManagementException("A sandbox already exists at the specified local path");
        }

        try {
            // TODO: Investigate if there's a TEE Workspace creation UI.
            workspace = versionControlClient.createWorkspace(
                null,
                "MathworksSandbox" + System.currentTimeMillis(), 
                null,
                WorkspaceLocation.LOCAL,
                WorkspaceOptions.SET_FILE_TO_CHECKIN,
                WorkspacePermissionProfile.getPrivateProfile());

            // Map the workspace
            WorkingFolder workingFolder = new WorkingFolder(repositorySpecifier, sandboxRoot.getAbsolutePath());
            workspace.createWorkingFolder(workingFolder);

            // Download the files to the local machine
            // TODO: Possibly refactor so this can share code with the TfsAdapter sync methods?
            ItemSpec itemSpec = new ItemSpec(workingFolder.getLocalItem(), RecursionType.FULL);
            GetRequest getRequest = new GetRequest(itemSpec, LatestVersionSpec.INSTANCE);
            workspace.get(getRequest, GetOptions.NONE);
        }
        catch (Exception ex) {
            throw new ConfigurationManagementException(ex);
        }
    }

}
