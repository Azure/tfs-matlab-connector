/* Copyright 2010-2016 The MathWorks, Inc. */
package com.mathworks.cmlink.sdk.tests.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

public class FileCreation {

    private FileCreation() {
        //Non-instantiable.
    }

    private final static String[] EXTENSIONS = {".txt", ".mdl", ".m", ".mat"};
    private static int sExtensionIndex = 0;

    public static File createTempFileContainingText(File rootDirectory) throws IOException {
        if (!rootDirectory.exists()) {
            createDirectory(rootDirectory);
        }

        //Create a file and check it is not stored:
        File fileTemp = File.createTempFile("myfile", getExtension()); // in tempdir
        String filename = rootDirectory.getAbsolutePath() + File.separator + fileTemp.getName();

        File file = new File(filename);
        createFileContainingText(file);

        return file;
    }

    private static String getExtension() {
        sExtensionIndex++;
        if (sExtensionIndex >= EXTENSIONS.length) {
            sExtensionIndex = 0;
        }
        return EXTENSIONS[sExtensionIndex];
    }

    public static void createFileContainingText(File file) throws IOException {

        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            createDirectory(parentDir);
        }
        if (!file.createNewFile()) {
            throw new IOException("Could not create file " + file);
        }

        try (FileWriter fileWrite = new FileWriter(file)) {
            fileWrite.write("Created file: " + file + "\nfor use by TAdapter.");
        }
    }

    public static void modifyFiles(Collection<File> files) throws IOException {
        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("MODIFIED for test " + Math.random());
            }
        }
    }

    public static void createDirectory(File dir) throws IOException {
        if (dir.exists()) {
            throw new IOException("File already exists " + dir);
        }
        if (!dir.mkdirs()) {
            throw new IOException("Could not create directory " + dir);
        }

    }

    public static File changeRoot(File file, File oldRoot, File newRoot) {

        String relativePath = getRelativePath(oldRoot, file);
        return new File(newRoot, relativePath);
    }

    public static String getRelativePath(File fileRoot, File fileChild) {

        return getRelativePath(fileRoot.getAbsolutePath(), fileChild.getAbsolutePath());


    }

    public static String getRelativePath(String fileRootPath, String fileChildPath) {
        int rootLength = fileRootPath.length();
        String relativePath;
        if (fileChildPath.length() == rootLength) {
            relativePath = "";
        } else {
            relativePath = fileChildPath.substring(rootLength + 1);
        }
        return relativePath;
    }

}
