This app make provides an integration between MATLAB and TFS:

Configuring the integration:

To use this app it must be first configured. To do this perform the 
following steps:

The following steps are required to perform this configuration.
1. Run the following command at the command prompt: installTFS
   You will be prompted for your default TFS URL. The URL must include the 
   project collection (ex: http://MyTfsServer:8080/tfs/DefaultCollection).
   You will also be asked whether you're using an on-premise TFS server with 
   Windows auth. If you are, then MATLAB will connect to TFS automatically.
   Otherwise, you will be prompted for login credentials.
3. Restart MATLAB after the installation completes.

Usage instructions:

1. When you navigate to a Workspace directory, MATLAB will automatically detect 
   that it's a Workspace and display TFS status icons. Note that it may take 
   a few seconds for the status icons to appear while MATLAB connects to TFS 
   for the first time. You can also select a file, right click, and look under 
   the "Source Control" menu to find various source control operations that 
   can be performed.
       Note: See the known issues section, some newer format TFS workspaces
             might not be correctly detected. You can workaround this issue
             by creating a workspace using the MATLAB integration. The next
             section of the document describes how to do this.
            
2. If you want to change the TFS project collection you're connected to, you 
   can right click MATLAB's file explorer and select Source Control -> Connect 
   to TFS.

3. If you want to change your default TFS project collection URL, run the 
   storeTfsSettings.m MATLAB function and restart MATLAB.

Creating a workspace using MATLAB:

   1. In MATLAB's current folder tools navigate to the folder where you want
      to create the workspace.
   2. Right click in the current folder tool and select the context menu 
      "Source Control"->"Manage Files".
   3. Select Team Foundation Server in the Source control integration dropdown.
   4. For the repository path, enter the source location on the TFS Server 
     (ex: $/MyProject/Src/Main)
   5. Enter the local directory where you'd like to download the files.
   6. Click the Retrieve button
	
Uninstall instructions:
1. Navigate to the app directory and run the following command: uninstallTFS
    This will remove all configuration information stored with the app.
2. Restart MATLAB
3. Now uninstall the app: In the MATLAB APPS toolbar, select the TFS 
   Integration App right click and select "Uninstall"
	   
Known issues:
   - The TFS Java SDK uses an older version of the TFS APIs than Visual
     Studio.  Workspaces you create with the Java SDK are usable in Visual Studio, 
     but Workspaces created in Visual Studio have inconsistent behavior with 
     the SDK. For the best experience, you should create a new Workspace in the 
     MATLAB UI.
   - The "Tag" features are not implemented - this feature will be deprecated 
     in a future release of MATLAB.
