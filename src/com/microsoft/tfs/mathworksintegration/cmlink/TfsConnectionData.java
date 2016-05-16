// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import com.microsoft.tfs.core.httpclient.Credentials;

/**
 * Stores information required to connect to TFS
 */
public class TfsConnectionData {
    private String endpoint;
    private Credentials credentials;
    private boolean canceled;
    	
    /**
     * Initializes a TfsConnectionData instance.
     * @param endpoint
     *     The TFS team project collection endpoint.
     * @param credentials
     *     The {@link Credentials} to use when connecting with TFS.
     */
    public TfsConnectionData(String endpoint, Credentials credentials) {
        this(false, endpoint, credentials);
    }
    
    /**
     * Initializes a TfsConnectionData instance which indicates that the
     * connection operation should be canceled.
     */
    public static TfsConnectionData Canceled() {
    	return new TfsConnectionData(true, null, null);
    }
        
    private TfsConnectionData(boolean isCanceled, String endpoint, Credentials credentials) {
        this.endpoint = endpoint;
        this.credentials = credentials;
        this.canceled = isCanceled;
    }
        
    /**
     * Gets the TFS team project collection endpoint.
     */
    public String getEndpoint() {
        return this.endpoint;
    }
    
    /**
     * Gets the {@link Credentials} to use when connection with TFS.
     */
    public Credentials getCredentials() {
        return this.credentials;
    }
    
    /**
     * Gets whether the TFS connection attempt should be canceled.
     */
    public boolean isCanceled() {
    	return this.canceled;
    }
}
