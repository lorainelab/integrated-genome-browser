package com.affymetrix.genometry.servlets.das2manager;

import java.util.Set;

public class SecurityGroup {
	private Integer    idSecurityGroup;
	private String     name;
    private Set        members;
    private Set        collaborators;
    private Set        managers;
	
	public Integer getIdSecurityGroup() {
    	return idSecurityGroup;
    }
	public void setIdSecurityGroup(Integer idSecurityGroup) {
    	this.idSecurityGroup = idSecurityGroup;
    }
	public String getName() {
    	return name;
    }
	public void setName(String name) {
    	this.name = name;
    }
	public Set getMembers() {
    	return members;
    }
	public void setMembers(Set members) {
    	this.members = members;
    }
	public Set getCollaborators() {
    	return collaborators;
    }
	public void setCollaborators(Set collaborators) {
    	this.collaborators = collaborators;
    }
	public Set getManagers() {
    	return managers;
    }
	public void setManagers(Set managers) {
    	this.managers = managers;
    }	

}
