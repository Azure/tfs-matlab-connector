% Copyright (c) Microsoft Corporation

function appReadMe
% Displays information on using this app.
location = fileparts(which(mfilename));
web(fullfile(location, 'appReadMe.txt'));

end

