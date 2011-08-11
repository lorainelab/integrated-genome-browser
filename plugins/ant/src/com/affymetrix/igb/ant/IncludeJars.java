package com.affymetrix.igb.ant;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class IncludeJars extends Task {
	private List<FileSet> filesets = new ArrayList<FileSet>();
	private File propertiesfile;

	public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }

	public File getPropertiesfile() {
		return propertiesfile;
	}

	public void setPropertiesfile(File propertiesfile) {
		this.propertiesfile = propertiesfile;
	}

	private void validate() {
        if (filesets.size()<1) throw new BuildException("fileset not set");
        if (propertiesfile==null) throw new BuildException("bundles properties file not set");
     }

    private String getProperty(String jarFileName) throws Exception {
		JarFile jar = new JarFile(jarFileName);
		Manifest mf = jar.getManifest();
		Attributes attributes = mf.getMainAttributes();
		String symbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
		String versionString = attributes.getValue(Constants.BUNDLE_VERSION);
		Version version = new Version(versionString);
		String category = attributes.getValue(Constants.BUNDLE_CATEGORY);
		if (category == null) {
			category = "";
		}
		return symbolicName + ";" + version + "=" + category;
    }

    @Override
	public void execute() throws BuildException {
		try {
			validate();
			propertiesfile.delete();
			propertiesfile.createNewFile();
			PrintStream ps = new PrintStream(propertiesfile);

	        for (FileSet fs: filesets) {
	            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
	            String[] includedFiles = ds.getIncludedFiles();
	            String dir = ds.getBasedir().getPath();
	            for (int i=0; i<includedFiles.length; i++) {
	                String filename = (dir + "/" + includedFiles[i]).replace('\\','/');
	    			ps.println(getProperty(filename));
	            }
	        }
			ps.flush();
			ps.close();
		}
		catch (Exception x) {
			throw new BuildException(x);
		}
	}
}
