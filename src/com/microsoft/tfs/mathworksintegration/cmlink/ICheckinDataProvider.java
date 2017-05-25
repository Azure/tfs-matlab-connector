// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import com.mathworks.cmlink.api.ConfigurationManagementException;

/**
 * Gets information required to checkin changes.
 */
public interface ICheckinDataProvider {

    /**
     * Gets information required to checkin changes.
     * @param comment
     *   Existing checkin comment - if the commit is made from MATLAB, then the user
     *   may have already provided a comment.
     * @throws ConfigurationManagementException
     */
    public CheckinData getData(String comment) throws ConfigurationManagementException;
}