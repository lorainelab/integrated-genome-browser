package com.affymetrix.igb.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class GetVersion extends Task {
	private File manifestfile;

	public File getManifestfile() {
		return manifestfile;
	}

	public void setManifestfile(File manifestfile) {
		this.manifestfile = manifestfile;
	}

	private void validate() {
        if (manifestfile==null) throw new BuildException("manifest file not set");
     }

    @Override
	public void execute() throws BuildException {
		try {
			validate();
			InputStream istr = new FileInputStream(manifestfile);
			Manifest manifest = new Manifest(istr);
			Attributes attributes = manifest.getMainAttributes();
			String versionString = attributes.getValue("Bundle-Version");
	        if (versionString==null) throw new BuildException("no version found in manifest");
	        System.out.println("version="+versionString);
	        getProject().setProperty("Bundle-Version", versionString);
		}
		catch (Exception x) {
			throw new BuildException(x);
		}
	}
}
