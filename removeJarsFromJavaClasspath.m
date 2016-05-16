% Copyright (c) Microsoft Corporation

function removeJarsFromJavaClasspath(directory)
% Remove each .jar file in the specified directory from MATLAB's Java classpath.

updateJavaClasspath(directory, false);

end