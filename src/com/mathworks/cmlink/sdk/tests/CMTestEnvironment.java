/* Copyright 2011 The MathWorks, Inc. */

/* Copyright (c) Microsoft Corporation */
package com.mathworks.cmlink.sdk.tests;

import com.mathworks.cmlink.api.version.r16b.CMAdapterFactory;

import java.io.File;

/**
 * This class is used to set up a test environment for a CM Adapter implementation.
 */
public interface CMTestEnvironment {
    /**
     * Create a repository in the specified repository.
     *
     * @param repositoryDir The directory in which to create a repository.
     * @return A repository specifier String.
     * @throws Exception
     */
    String createRepository(File repositoryDir) throws Exception;

    /**
     * Creates a sandbox for a specified repository location.
     *
     * @param repositorySpecifierString The repository for which the sandbox will be created.
     * @param sandboxDir                The location of the sandbox to create.
     * @throws Exception
     */
    void createSandbox(String repositorySpecifierString, File sandboxDir) throws Exception;

    /**
     * Gets the CMAdapterFactory for the instance being tested.
     *
     * @return The instance of CMAdapterFactory to be tested.
     */
    CMAdapterFactory getAdapterFactory();
}
