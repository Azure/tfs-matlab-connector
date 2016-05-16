// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import java.util.Collection;
import java.util.EnumSet;

import com.mathworks.cmlink.api.ConfigurationManagementException;
import com.mathworks.cmlink.api.InteractorSupportedFeature;
import com.mathworks.cmlink.api.customization.CoreAction;
import com.mathworks.cmlink.api.customization.CustomizationWidgetFactory;
import com.mathworks.cmlink.api.version.r14a.CMInteractor;
import com.microsoft.tfs.core.TFSTeamProjectCollection;

/**
 * Implementation of the {@link CMInteractor} interface which handles TFS connections.
 */
public class TfsBase implements CMInteractor {

    protected TFSTeamProjectCollection teamProjectCollection;

    private final Collection<InteractorSupportedFeature> supportedFeatures;

    /**
     * Initializes a TfsBase instance.
     */
    public TfsBase() {
        this.supportedFeatures = EnumSet.of(
            InteractorSupportedFeature.CONNECTION
            );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildCustomActions(CustomizationWidgetFactory widgetFactory) {

        // Connect to a TFS server. This lets the user change which server/project collection they're using.
        widgetFactory.createActionWidget("Connect to TFS", null, new CoreAction() {
            @Override
            public void execute() throws ConfigurationManagementException {

                Utilities.connectToTfs(true);;
            }

            @Override
            public String getDescription() {
                return "Connect to a TFS server project collection.";
            }

            @Override
            public boolean canCancel() {
                return true;
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect() throws ConfigurationManagementException {
        this.teamProjectCollection = Utilities.getTfsConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect() throws ConfigurationManagementException {
        // The TFSTeamProjectCollection instance is shared across multiple classes,
        // so we don't want to clean it up in case someone else is still using it.
        // There will only ever be a single active instance of the 
    	// TFSTeamProjectCollection for the duration of the MATLAB process.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getShortSystemName() {
        return "TFS";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSystemName() {
        return "Team Foundation Server";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFeatureSupported(InteractorSupportedFeature feature) {
        return this.supportedFeatures.contains(feature);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
        return this.teamProjectCollection != null;
    }

}
