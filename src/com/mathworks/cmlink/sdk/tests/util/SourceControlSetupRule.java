/* Copyright 2016 The MathWorks, Inc. */

/* Copyright (c) Microsoft Corporation */
package com.mathworks.cmlink.sdk.tests.util;

import com.mathworks.cmlink.api.ConfigurationManagementException;
import com.mathworks.cmlink.api.version.r16b.CMAdapter;
import com.mathworks.cmlink.api.version.r16b.CMAdapterFactory;
import com.mathworks.cmlink.sdk.tests.CMTestEnvironment;
import com.mathworks.cmlink.util.interactor.NullApplicationInteractor;
import com.mathworks.toolbox.shared.computils.file.FileDeleter;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.mathworksintegration.cmlink.Utilities;

import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class SourceControlSetupRule implements TestRule {

    private final TemporaryFolder fRootFolder = new TemporaryFolder();
    private final CMTestEnvironment fCMTestEnvironment;
    private final ArrayList<Closeable> fClosables;
    private int fSandboxCount = 0;
    private String fRepositoryPath;

    public SourceControlSetupRule(CMTestEnvironment testEnvironment) {
        fCMTestEnvironment = testEnvironment;
        fClosables = new ArrayList<>();
    }

    @Override
    public Statement apply(final Statement statement, Description description) {

        return fRootFolder.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final File repositoryDir = fRootFolder.newFolder("Repository location");
                fRepositoryPath = fCMTestEnvironment.createRepository(repositoryDir);
                try {
                    statement.evaluate();
                } finally {
                    for (Closeable closeable : fClosables) {
                        closeable.close();
                    }
                }
            }

        }, description);
    }

    public File newFolder() throws Exception{
        return fRootFolder.newFolder();
    }

    public File newSandbox() throws Exception {

        File sandboxDir = fRootFolder.newFolder("Sandbox " + ++fSandboxCount);
        fCMTestEnvironment.createSandbox(fRepositoryPath, sandboxDir);
        return sandboxDir;
    }

    public CMAdapter getCMAdapterFor(File file) throws Exception {
        CMAdapterFactory adapterFactory = fCMTestEnvironment.getAdapterFactory();
        final CMAdapter cmAdapter = adapterFactory.getAdapterForThisSandboxDir(file, new NullApplicationInteractor());
        cmAdapter.connect();
        
        final File f = new File(file.getAbsolutePath());
        fClosables.add(new Closeable() {
            @Override
            public void close() throws Exception {
                // Delete the test files on the TFS server
                cmAdapter.update(f);
                try {
                    cmAdapter.remove(Collections.singleton(f));
                    cmAdapter.checkin(f, "test cleanup");
                }
                catch (ConfigurationManagementException ex) {
                    // If the test didn't check anything in, then there won't be anything to remove
                    // from the server, so just continue.
                    if (!ex.getMessage().startsWith("ItemNotFoundException")) {
                        throw ex;
                    }
                }

                // Delete the TFS Workspace
                Workspace workspace = Utilities.getWorkspaceForLocalPath(f.getAbsolutePath());
                workspace.getClient().deleteWorkspace(workspace);

                cmAdapter.disconnect();

                // Delete all remaining test files on the local machine
                File repo = new File(fRepositoryPath);
                FileDeleter.deleteDirectoryIfItExists(repo);
                if (repo.exists()) {
                    throw new IOException("Could not delete root directory:\n\t" + repo);
                }
            }
        });
        return cmAdapter;
    }

    private interface Closeable {
        void close() throws Exception;
    }
}
