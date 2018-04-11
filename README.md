# tfs-matlab-connector

This project provides TFS Version Control integration in MATLAB and Simulink.

http://www.mathworks.com/matlabcentral/fileexchange/61483-source-control-integration-software-development-kit

http://www.mathworks.com/help/matlab/matlab_prog/about-mathworks-source-control-integration.html

http://www.mathworks.com/help/simulink/ug/write-a-source-control-adapter-with-the-sdk.html

This project is built on top of the TFS JAVA SDK, which is part of the Team Explorer Everywhere project:
https://github.com/Microsoft/team-explorer-everywhere

### Installing and using the app
Open MATLAB and double click the "TFS Version Control Integration.mlappinstall" file. Follow the prompts to install the app. See appReadMe.txt for more details on setup and usage.

### Building the source code with Ant

1. Install MATLAB, at least version R2016b.
2. Install the Java 8 Development Kit and the Java 7 Runtime. The MATLAB SDK uses Java 1.7 as its target, so in theory JDK 1.7 should be sufficient, but only the JDK 8 + JRE 7 configuration has been validated.
   * http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
   * http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html
3. Install Apache Ant(TM) version 1.9.7.
   * http://ant.apache.org/bindownload.cgi
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
6. Download junit-4.12.jar, hamcrest-all-1.3.jar, and hamcrest-core-1.3.jar. The MathWorks test framework has a dependency on them.
   * http://search.maven.org/#search|gav|1|g:%22junit%22%20AND%20a:%22junit%22
   * http://search.maven.org/#search|gav|1|g:%22org.hamcrest%22%20AND%20a:%22hamcrest-core%22
   * http://search.maven.org/#search|gav|1|g:%22org.hamcrest%22%20AND%20a:%22hamcrest-all%22

   By default, it's expected that junit-4.12.jar, hamcrest-all-1.3.jar, and hamcrest-core-1.3.jar are saved in the test folder, but a custom folder can be used (see next step).
7. Navigate to the src directory and run:
   * ant compile

   Note that the build.xml file needs the path to your MATLAB installation, the Java 7 runtime directory containing rt.jar, and the test dependency jar files. Default Windows values are provided in the file, but you can overwrite them on the command line:
   * ant compile "-Dmatlab.root.dir=D:/MATLAB/R2017a" "-Djre7.lib.dir=D:/jre7/lib" "-Dtest.dependency.dir=D:/java/test"
8. To delete all build output, run:
   * ant clean

### dist

This directory contains the distributables consumed by MATLAB to integrate with TFS Version Control. While build outputs aren't typically checked into a GitHub repository, the MATLAB File Exchange's GitHub integration appears to only pull from sources for now. (MATLAB .m files don't need to be compiled)

The dist/integration directory contains the .jar built following the steps in the "Building with Ant" section above.

The dist/TFS-SDK directory contains the TFS Java SDK redistributable. The contents of this directory were compiled from the releases/14.0.3 branch of the Team Explorer Everywhere GitHub repository: https://github.com/Microsoft/team-explorer-everywhere

### Tests

The automated test cases are written in two different classes:
   * src/com/mathworks/cmlink/sdk/tests/TAdapter.java

     This class is included in the MATLAB SDK http://www.mathworks.com/matlabcentral/fileexchange/61483-source-control-integration-software-development-kit.

     Some of the test cases have been modified to account for TFS-specific behaviors, primarily around how directories are handled. These test changes are indicated by the comment "CHANGE FROM ORIGINAL TEST".

     The test rule in src/com/mathworks/cmlink/sdk/tests/util/SourceControlSetupRule.java has also been modified to add some custom post-test cleanup of TFS Workspaces.
   * src/com/microsoft/tfs/mathworksintegration/tests/cmlink/TTfsAdapter.java

     This class contains additional tests for the TfsAdapter and its unique features.

Before running these tests, the following pre-requisite setup must be performed:
   * Open MATLAB and navigate to your local copy of this repository.
   * Run the storeTfsSettings.m script.
   * Supply a TFS server that uses Windows authentication. Windows authentication is used to avoid credential prompts during the execution of the tests.
   * On the TFS server, create a WorkItem to associate test commits with. Set the KnownWorkItemId value in src/com/microsoft/tfs/mathworksintegration/tests/cmlink/TestCheckinDataProvider.java to this value. The default is 1.
   * On the TFS server, create a project where test checkins can be made. Set the TestCheckinPath value in src/com/microsoft/tfs/mathworksintegration/tests/cmlink/TfsTestEnvironment.java to a path under this new project where test checkins will be made. The default value is $/Test/CmlinkTests.

To launch the tests, navigate to the src directory and run:
   * ant compile
   * ant runTests

For manual testing, open MATLAB and navigate to your local copy of this repository. Run the installTFS.m script and restart MATLAB. You can now execute TFS operations from the MATLAB UI. Run the uninstallTFS.m script when complete. See appReadMe.html for some more details on usage.

***
This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
