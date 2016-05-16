% Copyright (c) Microsoft Corporation

function installTFS()
%Installs the TFS/MATLAB integration

fprintf('Beginning installation...\n\n');

fprintf('Updating Java class path...\n');
parentDir = fileparts(which(mfilename()));
distDir = fullfile(parentDir, 'dist');
integrationDir = fullfile(distDir, 'integration');
sdkDir = fullfile(distDir, 'TFS-SDK', 'redist', 'lib');
updateJavaClasspath(integrationDir, true);
updateJavaClasspath(sdkDir, true);
fprintf('Java class path updated.\n\n');

storeTfsSettings();

fprintf('Installation complete. Please restart MATLAB.\n');

end