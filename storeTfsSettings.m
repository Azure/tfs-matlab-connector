% Copyright (c) Microsoft Corporation

function storeTfsSettings()
% Stores the user's TFS connection settings.

fprintf('Getting user settings...\n\n');

currentPath = mfilename('fullpath');
[currentDir, ~, ~] = fileparts(currentPath);
path = fullfile(currentDir, 'dist', 'TfsSettings.txt');
tfsSettingsFile = fopen(path, 'wt');

defaultTfsUrl = input('Enter your default TFS project collection URL (ex: http://MyTfsServer:8080/tfs/DefaultCollection): ', 's');
defaultTfsUrlFileLine = sprintf('%s\n', defaultTfsUrl);
fwrite(tfsSettingsFile, defaultTfsUrlFileLine, 'char');

onPremWindowsServer = input('Are you using an on-premise TFS server with Windows credentials? [y/n]: ', 's');
if (~isempty(onPremWindowsServer) && lower(onPremWindowsServer(1)) == 'y')
    fwrite(tfsSettingsFile, 'true', 'char');
else
    fwrite(tfsSettingsFile, 'false', 'char');
end

fclose(tfsSettingsFile);
fprintf('\nUser settings saved.\n\n');

end