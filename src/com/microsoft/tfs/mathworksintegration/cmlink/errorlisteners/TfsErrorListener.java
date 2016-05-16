// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink.errorlisteners;

import java.util.ArrayList;

import com.mathworks.cmlink.api.ConfigurationManagementException;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorListener;

/**
 * Implementation of the {@link NonFataErrorListener} interface which processes
 * errors that occur during TFS operations launched from MATLAB.
 */
public class TfsErrorListener implements NonFatalErrorListener {

    protected ArrayList<NonFatalErrorEvent> errors;

    /**
     * Initializes a TfsErrorListener instance.
     */
    public TfsErrorListener() {
        this.errors = new ArrayList<NonFatalErrorEvent>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNonFatalError(NonFatalErrorEvent errorEvent) {
        this.errors.add(errorEvent);
    }

    /**
     * Processes errors that occurred during a TFS operation launched from MATLAB.
     * If one or more errors occurred, an exception is thrown containing details
     * about each error.
     * @throws ConfigurationManagementException
     */
    public void ProcessErrors() throws ConfigurationManagementException {
        if (errors.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (NonFatalErrorEvent error : errors) {
            sb.append(error.getFailure().getCode());
            sb.append(" ");
            sb.append(error.getMessage());
            sb.append("; ");
        }

        throw new ConfigurationManagementException(sb.toString());
    }
}
