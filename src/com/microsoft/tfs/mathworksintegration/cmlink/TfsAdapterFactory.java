// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;

import com.mathworks.cmlink.api.ApplicationInteractor;
import com.mathworks.cmlink.api.ConfigurationManagementException;
import com.mathworks.cmlink.api.version.r16b.CMAdapter;
import com.mathworks.cmlink.api.version.r16b.CMAdapterFactory;
import com.mathworks.cmlink.api.version.r16b.CMRepository;
import com.microsoft.tfs.core.config.httpclient.DefaultHTTPClientFactory;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;

/**
 * Implementation of the {@link CMAdapterFactory} interface which creates
 * {@link TfsAdapter} instances.
 */
public class TfsAdapterFactory implements CMAdapterFactory {

    static {
        // Set the TFS Java SDK SimpleLog output levels to minimize the 
        // amount of data written to the MATLAB output window.
        // This code is executed when MATLAB starts up.
        Log log = LogFactory.getLog(SOAPService.class);
        if (log.getClass() == SimpleLog.class) {
            ((SimpleLog)log).setLevel(SimpleLog.LOG_LEVEL_ERROR);
        }

        log = LogFactory.getLog(DefaultHTTPClientFactory.class);
        if (log.getClass() == SimpleLog.class) {
            ((SimpleLog)log).setLevel(SimpleLog.LOG_LEVEL_ERROR);
        }
    }

    private final ICheckinDataProvider checkinDataProvider;

    /**
     * Initializes a TfsAdapterFactory instance.
     */
    public TfsAdapterFactory() {
        this(new CheckinDataUserPrompt());
    }

    /**
     * Initializes a TfsAdapterFactory instance.
     * @param checkinDataProvider
     *     The {@link ICheckinDataProvider} to use with the {@link TfsAdapter}
     *     instances created by this factory.
     */
    public TfsAdapterFactory(ICheckinDataProvider checkinDataProvider) {
        this.checkinDataProvider = checkinDataProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CMAdapter getAdapterForThisSandboxDir(File directory, ApplicationInteractor interactor)
        throws ConfigurationManagementException {	

        TfsAdapter adapter = new TfsAdapter(directory, interactor, this.checkinDataProvider);
        adapter.connect();
        return adapter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "TFS version control integration.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "TFS";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CMRepository getRepository(ApplicationInteractor interactor) {
        TfsRepository repository = new TfsRepository(interactor);
        return repository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirSandboxForThisAdapter(File directory) {
        // TFS local workspaces contain a hidden "$tf" folder at their root.
        // The TFS SDK needs a connection to actually query for workspaces, but
        // we don't want to show UI in this method to prompt for credentials, 
        // since MATLAB may still be initializing when the adapter factories
        // are created and the startup directory is checked.

        if (directory == null) {
            return false;
        }

        File workspaceDir = new File(directory, Utilities.TfsLocalWorkspaceFolder);
        if (workspaceDir.exists()) {
            return true;
        }

        return isDirSandboxForThisAdapter(directory.getParentFile());
    }

}
