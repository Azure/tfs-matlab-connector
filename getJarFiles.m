% Copyright (c) Microsoft Corporation

function jarPaths = getJarFiles(directory)
% Gets all .jar file paths in the specified directory.

childFiles = dir(directory);
jarIndexes = [];
for i = 1:length(childFiles)
    if java.lang.String(childFiles(i).name).endsWith('.jar')
        jarIndexes = [jarIndexes i]; %#ok<AGROW>
    end
end

jarFiles = childFiles(jarIndexes);
jarPaths = cell(1, length(jarFiles));
for i = 1:length(jarFiles)
    jarPaths{i} = fullfile(directory, jarFiles(i).name); 
end

end