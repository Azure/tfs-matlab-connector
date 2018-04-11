// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.tests.cmlink;

import com.mathworks.cmlink.api.ConfigurationManagementException;
import com.microsoft.tfs.mathworksintegration.cmlink.CheckinData;
import com.microsoft.tfs.mathworksintegration.cmlink.ICheckinDataProvider;

/**
 * An implementation of the {@link ICheckinDataProvider} interface for use with tests.
 * Provides a known WorkItem to associate the commit with while avoiding a user prompt.
 */
public class TestCheckinDataProvider implements ICheckinDataProvider {

    // Supply your known WorkItem to associate test commits with.
    private final int KnownWorkItemId = 1;

    /**
     * {@inheritDoc}
     */
    @Override
    public CheckinData getData(String comment) throws ConfigurationManagementException {
        return new CheckinData(new int[] { KnownWorkItemId }, comment);
    }
}
