// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.tests.cmlink;

import java.io.File;

import com.mathworks.cmlink.api.version.r16b.CMAdapterFactory;
import com.mathworks.cmlink.sdk.tests.CMTestEnvironment;
import com.mathworks.cmlink.util.interactor.NullApplicationInteractor;
import com.microsoft.tfs.mathworksintegration.cmlink.TfsAdapterFactory;
import com.microsoft.tfs.mathworksintegration.cmlink.TfsRepository;

/**
 * An implementation of the {@link CMTestEnvironment} interface for use with tests.
 */
public class TfsTestEnvironment implements CMTestEnvironment {

    // Enter the TFS server path where test files should be checked in.
    private static final String TestCheckinPath = "$/Test/CmlinkTests";

    /**
     * {@inheritDoc}
     */
    @Override
    public String createRepository(File repositoryDir) throws Exception {
        return TestCheckinPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSandbox(String repositorySpecifierString, File sandboxDir) throws Exception {
        TfsRepository repository = new TfsRepository(new NullApplicationInteractor());
        repository.connect();
        repository.retrieveSandboxFromRepository(repositorySpecifierString, sandboxDir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CMAdapterFactory getAdapterFactory() {
        return new TfsAdapterFactory(new TestCheckinDataProvider());
    }
}
