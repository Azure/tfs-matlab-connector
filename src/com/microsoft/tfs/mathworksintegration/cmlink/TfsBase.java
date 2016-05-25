// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import java.util.Collection;
import java.util.EnumSet;

import com.mathworks.cmlink.api.ConfigurationManagementException;
import com.mathworks.cmlink.api.InteractorSupportedFeature;
import com.mathworks.cmlink.api.customization.CoreAction;
import com.mathworks.cmlink.api.customization.CustomizationWidgetFactory;
import com.mathworks.cmlink.api.version.r14a.CMInteractor;

/**
 * Implementation of the {@link CMInteractor} interface which handles TFS connections.
 */
public class TfsBase implements CMInteractor {

    private final Collection<InteractorSupportedFeature> supportedFeatures;
    private boolean isConnected;
    
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

        // This lets the user change which TFS server/project collection they're using.
        widgetFactory.createActionWidget("Change TFS Connection", null, new CoreAction() {
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

        // This text shows up in the View Details dialog under the "TFS info:" label.
        String infoString = null;
        try {
            String url = Utilities.getTfsConnection().getBaseURI().toString();
            infoString = "URL: " + url;
        }
        catch (Exception ex) {
        	// Just don't display anything
        }
        widgetFactory.createLabelWidget(infoString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect() throws ConfigurationManagementException {
        if (Utilities.getTfsConnection() != null) {
            this.isConnected = true;
        }
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
        return this.isConnected;
    }

}
