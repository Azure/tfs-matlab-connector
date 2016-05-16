% Copyright (c) Microsoft Corporation

function uninstallTFS()
% Uninstalls the TFS/MATLAB integration.

fprintf('Uninstalling TFS integration...\n\n');
parentDir = fileparts(which(mfilename()));
distDir = fullfile(parentDir, 'dist');
integrationDir = fullfile(distDir, 'integration');
sdkDir = fullfile(distDir, 'TFS-SDK', 'redist', 'lib');
updateJavaClasspath(integrationDir, false);
updateJavaClasspath(sdkDir, false);
fprintf('Uninstallation complete. Please restart MATLAB.\n');

end