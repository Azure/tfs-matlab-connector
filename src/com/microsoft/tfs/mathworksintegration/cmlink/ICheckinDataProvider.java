// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import com.mathworks.cmlink.api.ConfigurationManagementException;

/**
 * Gets information required to checkin changes.
 */
public interface ICheckinDataProvider {

    /**
     * Gets information required to checkin changes.
     * @throws ConfigurationManagementException
     */
    public CheckinData getData() throws ConfigurationManagementException;
}