// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.mathworks.cmlink.api.FileProperty;
import com.mathworks.cmlink.api.IntegerRevision;
import com.mathworks.cmlink.api.LocalStatus;
import com.mathworks.cmlink.api.Revision;
import com.mathworks.cmlink.api.version.r16b.FileState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;

/**
 * Extension of the {@FileState} class which contains TFS specific state information.
 */
public class TfsFileState implements FileState {

    private static final int DefaultVersion = 0;

    private ExtendedItem extendedItem;
    private Conflict conflict;

    /**
     * Initializes a TfsFileState instance.
     */
    public TfsFileState() {
        this(null, null);
    }

    /**
     * Initializes a TfsFileState instance.
     * @param extendedItem
     *     The {@link ExtendedItem} containing TFS information about the file.
     * @param conflict
     *     The {@link Conflict} containing information about any conflicts.
     */
    public TfsFileState(ExtendedItem extendedItem, Conflict conflict) {
        this.extendedItem = extendedItem;
        this.conflict = conflict;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalStatus getLocalStatus() {
        if (this.extendedItem == null) {
            return LocalStatus.NOT_UNDER_CM;
        }
        else if (!this.extendedItem.hasLocalChange()) {
            return LocalStatus.UNMODIFIED;
        }
        else {
            if (this.conflict != null) {
                return LocalStatus.CONFLICTED; 
            }

            ChangeType tfsChange = this.extendedItem.getPendingChange();
            if (tfsChange.contains(ChangeType.ADD) || tfsChange.contains(ChangeType.RENAME) ||
                tfsChange.contains(ChangeType.BRANCH)) {
                // Renames and branches show up with a LocalStatus of ADDED, but we add an entry
            	// to the properties collection to clarify.
                return LocalStatus.ADDED;
            }
            else if (tfsChange.contains(ChangeType.DELETE)) {
                return LocalStatus.DELETED;
            }
            else if (tfsChange.contains(ChangeType.EDIT) || tfsChange.contains(ChangeType.MERGE)) {
                return LocalStatus.MODIFIED;
            }
            // Locks are tracked separately with the hasLock() method.
            // If the status has multiple changes, one of which is a lock (ex: "edit, lock"), then the 
            // checks above will catch that and show the correct status (ex: MODIFIED). If the file only 
            // has a lock but no other changes, then we just show UNMODIFIED, since the SDK test cases 
            // expect this. As a result, this check must happen after all the others.
            else if (tfsChange.contains(ChangeType.LOCK)) {
                return LocalStatus.UNMODIFIED;
            }

            return LocalStatus.UNKNOWN;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<FileProperty> getProperties() {
        ArrayList<FileProperty> properties = new ArrayList<FileProperty>();

        if (this.extendedItem != null && this.extendedItem.hasLocalChange()) {
            ChangeType tfsChange = this.extendedItem.getPendingChange();
            if (tfsChange.contains(ChangeType.RENAME)) {
        	    TfsFileProperty renamedProperty = new TfsFileProperty("rename", "rename", true);
        	    properties.add(renamedProperty);
            }
            if (tfsChange.contains(ChangeType.BRANCH)) {
        	    TfsFileProperty branchProperty = new TfsFileProperty("branch", "branch", true);
        	    properties.add(branchProperty);
            }
            if (tfsChange.contains(ChangeType.MERGE)) {
        	    TfsFileProperty mergeProperty = new TfsFileProperty("merge", "merge", true);
        	    properties.add(mergeProperty);
            }
        }
        TfsFileProperty latestProperty = new TfsFileProperty(
            isLatest() ? "latest" : "not latest",
            "latest",
            false);
        properties.add(latestProperty);
        
        return properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Revision getRevision() {
        Revision revision = null;

        if (this.extendedItem != null) {
            int versionNumber = this.extendedItem.getLocalVersion();
            // SDK tests expects null instead of Revision(0) when a file has not been downloaded for the first time.
            if (versionNumber != DefaultVersion && this.extendedItem.getLatestVersion() != DefaultVersion) {
                // The path is stored on the Revision because if a file is renamed, the original path is 
                // required for some operations.
                HashMap<String, String> revisionInfo = new HashMap<String, String>();
                revisionInfo.put(Utilities.RevisionInfoKey_Path, extendedItem.getTargetServerItem());
                revision = new IntegerRevision(versionNumber, revisionInfo);
            }
        }

        return revision;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasLock() {
        return this.extendedItem != null && this.extendedItem.getLockLevel() != LockLevel.NONE;
    }

    /**
     * Whether this is the latest revision of the file.
     */
    public boolean isLatest() {
        return this.extendedItem == null || this.extendedItem.getLocalVersion() == this.extendedItem.getLatestVersion();
    }

    /**
     * Gets the conflict information for this file.
     */
    public Conflict getConflict() {
        return this.conflict;
    }
    
    /**
     * Gets the base revision for a conflict. If there is no conflict, then return null.
     */
    public Revision getBaseConflictRevision() {
        Revision revision = null;
        if (this.conflict != null) {
            HashMap<String, String> revisionInfo = new HashMap<String, String>();
            revisionInfo.put(Utilities.RevisionInfoKey_Path, this.conflict.getBaseServerItem());
            revision = new IntegerRevision(this.conflict.getBaseVersion(), revisionInfo);
        }

        return revision;
    }

    /**
     * Gets the revision causing the conflict. If there is no conflict, then return null.
     */
    public Revision getTheirConflictRevision() {
        Revision revision = null;
        if (this.conflict != null) {
            HashMap<String, String> revisionInfo = new HashMap<String, String>();
            revisionInfo.put(Utilities.RevisionInfoKey_Path, this.conflict.getTheirServerItem());
            revision = new IntegerRevision(this.conflict.getTheirVersion(), revisionInfo);
        }

        return revision;
    }

    /**
     * Get the latest revision for the file.
     */
    public Revision getLatestRevision() {
        Revision revision = null;
        if (this.extendedItem != null) {	
            HashMap<String, String> revisionInfo = new HashMap<String, String>();
            revisionInfo.put(Utilities.RevisionInfoKey_Path, extendedItem.getTargetServerItem());
            revision = new IntegerRevision(this.extendedItem.getLatestVersion(), revisionInfo);
        }

        return revision;
    }
}
