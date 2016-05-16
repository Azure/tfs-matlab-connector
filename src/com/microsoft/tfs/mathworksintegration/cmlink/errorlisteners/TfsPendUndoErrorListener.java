// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink.errorlisteners;

import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;

/**
 * Extension of the {@link TfsErrorListener} which processes errors that occur
 * during a TFS Pend Undo operation.
 */
public class TfsPendUndoErrorListener extends TfsErrorListener {

    private final String NoChangesErrorCode = "ItemNotCheckedOutException"; 

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNonFatalError(NonFatalErrorEvent errorEvent) {
        // No need to raise exceptions on this error - no undo is performed if no
        // pending changes are present.
        if (errorEvent.getFailure().getCode().equals(NoChangesErrorCode)) {
            return;
        }

        super.onNonFatalError(errorEvent);
    }
}