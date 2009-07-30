package com.affymetrix.genometry.servlets.das2manager;

public class Visibility {

	public static final String		MEMBERS = "MEM";
	public static final String		MEMBERS_AND_COLLABORATORS = "MEMCOL";
	public static final String		PUBLIC = "PUBLIC";
	
	private String codeVisibility;
	private String name;
	
	public String getCodeVisibility() {
    	return codeVisibility;
    }
	public void setCodeVisibility(String codeVisibility) {
    	this.codeVisibility = codeVisibility;
    }
	public String getName() {
    	return name;
    }
	public void setName(String name) {
    	this.name = name;
    }
	
	

}
