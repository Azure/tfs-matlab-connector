# tfs-matlab-connector

This project provides TFS Version Control integration in MATLAB and Simulink.  
http://www.mathworks.com/help/matlab/matlab_prog/about-mathworks-source-control-integration.html  
http://www.mathworks.com/help/simulink/ug/write-a-source-control-adapter-with-the-sdk.html

This project is built on top of the TFS JAVA SDK, which is part of the Team Explorer Everywhere project:  
https://github.com/Microsoft/team-explorer-everywhere

### Installing and using the app
Open MATLAB and double click the "TFS Version Control Integration.mlappinstall" file. Follow the prompts to install the app. See appReadMe.txt for more details on setup and usage.

### Building the source code with Ant

1. Install MATLAB, at least version R2014a. 
2. Install the Java 8 Development Kit and the Java 7 Runtime. The MATLAB SDK uses Java 1.7 as its target, so in theory JDK 1.7 should be sufficient, but only the JDK 8 + JRE 7 configuration has been validated.  
   http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html  
   http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html
3. Install Apache Ant(TM) version 1.9.7.
   http://ant.apache.org/bindownload.cgi
4. Set the JAVA_HOME environment variable.
   ex: 
   * (Windows) SET JAVA_HOME=C:\Program Files\Java\jdk1.8.0_65
   * (Mac) JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_65/Contents/Home
   * (Linux) JAVA_HOME=~/java/jdk1.8.0_65
5. Ensure that the JAVA_HOME bin directory and the Ant bin directory are on the PATH.
   ex:
   * (Windows) SET PATH=C:\Program Files\apache-ant-1.9.7\bin;%JAVA_HOME%\bin;%PATH%
   * (Mac) PATH=~/apache-ant-1.9.7/bin:$JAVA_HOME/bin:$PATH
   * (Linux) PATH=~/apache-ant-1.9.7/bin:$JAVA_HOME/bin:$PATH
6. Navigate to the src directory and run: 
   * ant compile   
   Note that the build.xml file needs the path to your MATLAB installation and the Java 7 runtime directory containing rt.jar. Default Windows values are provided in the file, but you can overwrite them on the command line: 
   * ant compile "-Dmatlab.root.dir=D:/MATLAB/R2015a" "-Djre7.lib.dir=D:/jre7/lib"
7. To delete all build output, run:
   * ant clean
   
### dist

This directory contains the distributables consumed by MATLAB to integrate with TFS Version Control. While build outputs aren't typically checked into a GitHub repository, the MATLAB File Exchange's GitHub integration appears to only pull from sources for now. (MATLAB .m files don't need to be compiled) 

The dist/integration directory contains the .jar built following the steps in the "Building with Ant" section above.

The dist/TFS-SDK directory contains the TFS Java SDK redistributable. The contents of this directory were compiled from the releases/14.0.3 branch of the Team Explorer Everywhere GitHub repository: https://github.com/Microsoft/team-explorer-everywhere

### Tests

Automated tests for this project will be uploaded in the future. The MATLAB SDK contains some automated test cases which can be run as a starting point, but not all of the tests will pass because of some TFS-specific behaviors.  
http://www.mathworks.com/help/simulink/ug/write-a-source-control-adapter-with-the-sdk.html

For manual testing, open the MATLAB UI and navigate to your local copy of this repository. Run the installTFS.m script and restart MATLAB. You can now execute TFS operations from the MATLAB UI. Run the uninstallTFS.m script when complete. See appReadMe.html for some more details on usage.

***
This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
