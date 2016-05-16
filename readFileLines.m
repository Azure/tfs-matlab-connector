% Copyright (c) Microsoft Corporation

function allLines = readFileLines(filePath)
% Reads all lines in the specified file

allLines = {};
if ~exist(filePath, 'file')
    return;
end

fid = fopen(filePath, 'r');
line = fgetl(fid);
while ~isnumeric(line)
    allLines = [allLines, line]; %#ok<AGROW>
    line = fgetl(fid);
end
fclose(fid);

end

