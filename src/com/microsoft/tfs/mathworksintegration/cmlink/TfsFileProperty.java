
// Copyright (c) Microsoft Corporation
package com.microsoft.tfs.mathworksintegration.cmlink;

import com.mathworks.cmlink.api.FileProperty;

/**
 * Implementation of the {FileProperty} interface.
 */
public class TfsFileProperty implements FileProperty {

	private String name;
	private String type;
	private boolean isMod;
	
    /**
     * Initializes a TfsFileProperty instance.
     */
	public TfsFileProperty(String name, String type, boolean isModification) {
		this.name = name;
		this.type = type;
		this.isMod = isModification;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isModification() {
		return this.isMod;
	}

}
