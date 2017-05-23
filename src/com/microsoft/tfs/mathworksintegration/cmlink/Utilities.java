// Copyright (c) Microsoft Corporation

package com.microsoft.tfs.mathworksintegration.cmlink;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.google.common.io.Files;
import com.mathworks.cmlink.api.ConfigurationManagementException;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.exceptions.TFSUnauthorizedException;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.URIUtils;

/**
 * Helper functions for the MATLAB/TFS integration.
 */
public class Utilities {

    public static final String TfsLocalWorkspaceFolder = "$tf";
    public static final String RevisionInfoKey_User = "User";
    public static final String RevisionInfoKey_Date = "Date";
    public static final String RevisionInfoKey_Changes = "Changes";
    public static final String RevisionInfoKey_Comment = "Comment";
    public static final String RevisionInfoKey_Path = "Path";

    private static final String TfsNativeJvmSetting = "com.microsoft.tfs.jni.native.base-directory";
    private static final String TfsNativeRedistPathFromDistDirectory = "TFS-SDK/redist/native";
    private static final String TfsSettingsFileName = "TfsSettings.txt";
    private static final String Utf8 = "UTF-8";

    private static TFSTeamProjectCollection teamProjectCollection;

    private static boolean isNativeRedistJvmPropertySet;

    // TODO: Should this be threadsafe?
    /**
     * Connects to a TFS team project collection.
     * @param forcePrompt
     *     Force a prompt to the user for their TFS connection information.
     * @throws ConfigurationManagementException
     */
    public static void connectToTfs(boolean forcePrompt) throws ConfigurationManagementException {
        File distDirectory = getDistDirectory();
        setNativeRedistJvmSetting(distDirectory);

        String endpoint = null;
        boolean onPremiseWindows = false; 
        List<String> settings = getSavedTfsSettings(distDirectory);	
        if (settings != null) {
            endpoint = settings.get(0);
            onPremiseWindows = Boolean.valueOf(settings.get(1));
        }

        boolean showConnectionPrompt = true;
        if (!forcePrompt && endpoint != null && onPremiseWindows) {
            showConnectionPrompt = false;
        }

        Credentials creds = null;
        TFSTeamProjectCollection collection = null;
        boolean isConnected = false;
        boolean authError = false;
        boolean endpointNotFound = false;
        TfsConnectionData connectionData = null;

        while (!isConnected) {
            if (showConnectionPrompt) {
                connectionData = showConnectionPrompt(endpoint, authError, endpointNotFound);
                if (connectionData.isCanceled()) {
                	break;
                }
                creds = connectionData.getCredentials();
                endpoint = connectionData.getEndpoint();
            }

            if (creds == null) {
                creds = new DefaultNTCredentials();
            }

            try {
                collection = new TFSTeamProjectCollection(URIUtils.newURI(endpoint), creds);
                collection.ensureAuthenticated();
                isConnected = true;
            }
            catch (TFSUnauthorizedException ex) {
                // User will be informed of login failure and prompted for creds again.
                creds = null;
                authError = true;
                endpointNotFound = false;
                showConnectionPrompt = true;
            }
            catch (TECoreException ex) {
                String message = ex.getMessage();
                creds = null;
                authError = false;
                showConnectionPrompt = true;

                // Check for endpoint not found error and prompt user again.
                // TODO: Is there a better way to check for this?
                if (message != null && message.contains("HTTP status: 404")) {
                    endpointNotFound = true;
                }
            }
        }

        if (isConnected) {
            if (teamProjectCollection != null) {
                teamProjectCollection.close();
            }
            teamProjectCollection = collection;
        }
    }

    // Gets the top level distributable directory relative to the currently executing code.
    private static File getDistDirectory() throws ConfigurationManagementException{
        File currentJarPath;
        try {
            currentJarPath = new File(Utilities.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException ex) {
            throw new ConfigurationManagementException(ex);
        }
        File jarDirectory = currentJarPath.getParentFile();
        return jarDirectory.getParentFile();
    }

    // Fetch the user's saved TFS settings
    private static List<String> getSavedTfsSettings(File parentDirectory) {
        List<String> settings = null;
        try {
            File settingsFile = new File(parentDirectory, TfsSettingsFileName);
            settings = Files.readLines(settingsFile, Charset.forName(Utf8));
        }
        catch (IOException ex) {
            // Just continue - we'll have to prompt the user for login info.
        }

        return settings;
    }

    // Set the JVM setting required by the native components of the TFS SDK redistributable.
    private static void setNativeRedistJvmSetting(File distDirectory) throws ConfigurationManagementException {
        if (!isNativeRedistJvmPropertySet) {
            File nativeRedistDirectory = new File(distDirectory, TfsNativeRedistPathFromDistDirectory);
            String nativeRedistLocation = nativeRedistDirectory.getAbsolutePath();

            System.setProperty(TfsNativeJvmSetting, nativeRedistLocation);
            isNativeRedistJvmPropertySet = true;
        }
    }

    // Gets the stored TFS project collection endpoint
    public static String getStoredEndpoint() {
    	String endpoint = null;
        try {
        	File distDirectory = getDistDirectory();
            List<String> settings = getSavedTfsSettings(distDirectory);
            if (settings != null) {
                endpoint = settings.get(0);
            }
        }
        catch (Exception ex) {
        	// Just don't return anything
        }
    	
    	return endpoint;
    }
    
    // Initialize the TFSTeamProjectCollection, or retrieve the previously initialized instance.
    public static TFSTeamProjectCollection getTfsConnection() throws ConfigurationManagementException {
        if (teamProjectCollection == null) {
            connectToTfs(false);	
        }

        return teamProjectCollection;
    }

    // Get the TFS Workspace associated with the given local path.
    public static Workspace getWorkspaceForLocalPath(String localPath) throws ConfigurationManagementException {
        // There are version compatibility issues with this method. The Java SDK uses an older version than
        // Visual Studio does.  Workspaces created by the Java SDK can be used in Visual Studio, but the
        // opposite is not true.  Trying to fetch a Workspace created in VS with the Java SDK will not fully work.
        TFSTeamProjectCollection collection = getTfsConnection();

        Workspace workspace = collection.getVersionControlClient().tryGetWorkspace(localPath);

        // Refreshes the cache on the local machine.
        if (workspace != null) {
            workspace.refreshIfNeeded();
        }

        return workspace;
    }
    
    // Prompt the user for some TFS connection info
    private static TfsConnectionData showConnectionPrompt(String endpoint, boolean authError, boolean endpointNotFound) {
    	Credentials creds = null;
    	
        // TODO: Investigate ability to integrate with TEE UIs
        JLabel errorLabel = null;
        if (authError) {
            errorLabel = new JLabel("Access denied. Verify your credentials and try again.");
            errorLabel.setForeground(Color.RED);
        }
        else if (endpointNotFound) {
            errorLabel = new JLabel("Endpoint not found. Verify your URL and try again.");
            errorLabel.setForeground(Color.RED);
        }

        JLabel endpointLabel = new JLabel("TFS Project Collection Endpoint:");
        JTextField endpointText = new JTextField(endpoint);
        JLabel loginLabel = new JLabel("Login information (leave blank to use Windows credentials)");
        JLabel userLabel = new JLabel("  Username:");
        JTextField userText = new JTextField();
        JLabel passwordLabel = new JLabel("  Password:");
        JPasswordField passwordField = new JPasswordField();
        JComponent[] components = new JComponent[] {
            errorLabel,
            endpointLabel,
            endpointText,
            loginLabel,
            userLabel,
            userText,
            passwordLabel,
            passwordField };

        int result = JOptionPane.showConfirmDialog(
            null,
            components,
            "TFS Connection",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null);		
        if (result == JOptionPane.CANCEL_OPTION) {
            return TfsConnectionData.Canceled();
        }

        endpoint = endpointText.getText();

        String user = userText.getText();
        char[] password = passwordField.getPassword();
        if ((user != null && !user.isEmpty()) && (password != null && password.length > 0)) {
            creds = new UsernamePasswordCredentials(user, String.valueOf(password));
            for (int i = 0; i < password.length; i++) {
                password[i] = 0;
            }
            passwordField.setText("");
        }
        
    	return new TfsConnectionData(endpoint, creds);
    }
}
