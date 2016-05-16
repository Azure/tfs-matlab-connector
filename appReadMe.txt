Install instructions:
1. Install the MATLAB app.
2. Open the MATLAB UI and navigate to the directory where you installed the 
   app. Run the following command: installTFS
   You will be prompted for your default TFS URL. The URL must include the 
   project collection (ex: http://MyTfsServer:8080/tfs/DefaultCollection).
   You will also be asked whether you're using an on-premise TFS server with 
   Windows auth. If you are, then MATLAB will connect to TFS automatically.
   Otherwise, you will be prompted for login credentials.
3. Restart MATLAB after the installation completes.

Usage instructions:
1. Open MATLAB and create a TFS Workspace. 
   - In MATLAB's explorer menu, right click, and select Source Control -> 
     Manage Files. 
   - Select Team Foundation Server in the Source control integration dropdown.
   - For the repository path, enter the source location on the TFS Server 
     (ex: $/MyProject/Src/Main)
   - Enter the local directory where you'd like to download the files.
   - Click the Retrieve button
2. When you navigate to a Workspace directory, MATLAB will automatically detect 
   that it's a Workspace and display TFS status icons. Note that it may take 
   a few seconds for the status icons to appear while MATLAB connects to TFS 
   for the first time. You can also select a file, right click, and look under 
   the "Source Control" menu to find various source control operations that 
   can be performed.
3. If you want to change the TFS project collection you're connected to, you 
   can right click MATLAB's file explorer and select Source Control -> Connect 
   to TFS.
4. If you want to change your default TFS project collection URL, run the 
   storeTfsSettings.m MATLAB function and restart MATLAB.
	
Uninstall instructions:
1. Navigate to the app directory and run the following command: uninstallTFS
2. Restart MATLAB
3. In the MATLAB APPS toolbar, right click the app and select "Uninstall"
	   
Known issues:
   - The TFS Java SDK uses an older version of the TFS APIs than Visual
     Studio.  Workspaces you create with the Java SDK are usable in Visual Studio, 
     but Workspaces created in Visual Studio have inconsistent behavior with 
     the SDK. For the best experience, you should create a new Workspace in the 
     MATLAB UI.
   - The "Tag" features are not implemented - this feature will be deprecated 
     in a future release of MATLAB.