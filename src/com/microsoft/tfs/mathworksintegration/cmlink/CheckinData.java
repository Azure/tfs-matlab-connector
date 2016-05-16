// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

/**
 * Stores information needed to checkin changes.
 */
public class CheckinData {

    private int[] workItemIds;
    private String comment;
    private boolean submitCheckin;

    /**
     * Initializes a CheckinData instance.
     * @param workItemIds
     *     The TFS WorkItem ids to associate with this checkin.
     * @param comment
     *     The checkin comment to use.
     */
    public CheckinData(int[] workItemIds, String comment) {
        this(true, workItemIds, comment);
    }
    
    /**
     * Initializes a CheckinData instance which indicates that the 
     * checkin operation should not proceed.
     */
    public static CheckinData NoSubmit() {
        return new CheckinData(false, null, null);	
    }

    /**
     * Initializes a CheckinData instance.
     * @param submitCheckin
     *     Whether the checkin operation should proceed.
     * @param workItemIds
     *     The TFS work item ids to associate this checkin with.
     * @param comment
     *     The checkin comment to use.
     */
    private CheckinData(boolean submitCheckin, int[] workItemIds, String comment) {
        this.submitCheckin = submitCheckin;
        this.workItemIds = workItemIds;
        this.comment = comment;
    }

    /**
     * Gets whether the checkin operation should proceed.
     */
    public boolean shouldSubmit() {
        return this.submitCheckin;
    }

    /**
     * Gets the TFS WorkItem ids to associate with this checkin. 
     */
    public int[] getWorkItemIds() {
        return this.workItemIds;
    }

    /**
     * Gets the checkin comment to use.
     */
    public String getCheckinComment() {
        return this.comment;
    }
}