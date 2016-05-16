% Copyright (c) Microsoft Corporation

function addJarsToJavaClasspath(directory)
% Add each .jar file in the specified directory to MATLAB's Java classpath.

updateJavaClasspath(directory, true);

end