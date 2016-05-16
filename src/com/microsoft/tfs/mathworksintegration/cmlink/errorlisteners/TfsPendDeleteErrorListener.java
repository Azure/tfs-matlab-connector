// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink.errorlisteners;

import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;

/**
 * Extension of the {@link TfsErrorListener} which processes errors that occur
 * during a TFS Pend Delete operation.
 */
public class TfsPendDeleteErrorListener extends TfsErrorListener {

    private final String PendingParentDeleteErrorCode = "PendingParentDeleteException"; 

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNonFatalError(NonFatalErrorEvent errorEvent) {
        // In the case of a delete operation, this error is harmless and the user will
        // get the desired result, so there's no need to raise an exception.
        if (errorEvent.getFailure().getCode().equals(PendingParentDeleteErrorCode)) {
            return;
        }

        super.onNonFatalError(errorEvent);
    }
}