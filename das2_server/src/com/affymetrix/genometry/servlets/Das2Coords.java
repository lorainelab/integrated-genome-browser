package com.affymetrix.genometry.servlets;

public class Das2Coords {
	String urid;
	String authority;
	String taxid;
	String version;
	String source;
	String test_range;


	public Das2Coords(String urid, String authority, String taxid,
			String version, String source, String test_range) {
		this.urid = urid;
		this.authority = authority;
		this.taxid = taxid;
		this.version = version;
		this.source = source;
		this.test_range = test_range;
	}

	public String getURI() { return urid; }
	public String getAuthority() { return authority; }
	public String getTaxid() { return taxid; }
	public String getVersion() { return version; }
	public String getSource() { return source; }
	public String getTestRange() { return test_range; }

}
