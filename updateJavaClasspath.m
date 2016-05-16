% Copyright (c) Microsoft Corporation

function updateJavaClasspath(directory, isInstall)
% Add or remove each .jar file in the specified directory depending on
% whether this is for installation or uninstallation.

jars = getJarFiles(directory);
classpathFilePath = fullfile(prefdir, 'javaclasspath.txt');

lines = readFileLines(classpathFilePath);
[~, jarIndexes] = intersect(lines, jars);
lines(jarIndexes) = [];
if isInstall
    lines = [jars, lines];
end

fid = fopen(classpathFilePath, 'w');
for i = 1:length(lines)
    fprintf(fid, '%s\n', lines{i});
end
fclose(fid);

end