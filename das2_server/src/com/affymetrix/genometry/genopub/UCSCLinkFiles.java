package com.affymetrix.genometry.genopub;

import java.io.File;

public class UCSCLinkFiles {
	//fields
	private File[] filesToLink;
	private boolean converting;
	
	//constructor
	public UCSCLinkFiles(File[] filesToLink, boolean converting){
		this.filesToLink = filesToLink;
		this.converting = converting;
	}

	public File[] getFilesToLink() {
		return filesToLink;
	}

	public void setFilesToLink(File[] filesToLink) {
		this.filesToLink = filesToLink;
	}

	public boolean isConverting() {
		return converting;
	}

	public void setConverting(boolean converting) {
		this.converting = converting;
	}
	
}
