// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import com.mathworks.cmlink.api.ConflictedRevisions;
import com.mathworks.cmlink.api.Revision;

/**
 * Implementation of the {@ConflictedRevisions} interface.
 */
public class TfsConflictedRevisions implements ConflictedRevisions {

    private Revision baseRevision;
    private Revision theirRevision;
    
    /**
     * Initializes a TfsConflictedRevisions instance.
     */
    public TfsConflictedRevisions(Revision base, Revision theirs) {
    	this.baseRevision = base;
    	this.theirRevision = theirs;
    }
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Revision getBaseRevision() {
		return this.baseRevision;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Revision getTheirsRevision() {
		return this.theirRevision;
	}

}
