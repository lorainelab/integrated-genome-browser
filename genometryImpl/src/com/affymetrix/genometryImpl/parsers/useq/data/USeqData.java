package com.affymetrix.genometryImpl.parsers.useq.data;

import java.io.*;

import com.affymetrix.genometryImpl.parsers.useq.*;

/**Parent Container for a sorted Data[] and it's associated SliceInfo.
 * @author david.nix@hci.utah.edu*/
public class USeqData {

	//fields
	protected SliceInfo sliceInfo;
	protected File binaryFile;
	/**Currently not used by useq archives, will be written to and read from binary file.*/
	protected String header = "";

	
	//methods
	public SliceInfo getSliceInfo() {
		return sliceInfo;
	}
	public File getBinaryFile() {
		return binaryFile;
	}
	public String getHeader() {
		return header;
	}
	public void setHeader(String header) {
		this.header = header;
	}
	public void setSliceInfo(SliceInfo sliceInfo) {
		this.sliceInfo = sliceInfo;
	}
	public void setBinaryFile(File binaryFile) {
		this.binaryFile = binaryFile;
	}
}
