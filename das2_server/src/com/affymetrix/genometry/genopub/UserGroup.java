package com.affymetrix.genometry.genopub;

import java.util.Set;

public class UserGroup {
	private Integer    idUserGroup;
	private String     name;
    private Set        members;
    private Set        collaborators;
    private Set        managers;
	
	public Integer getIdUserGroup() {
    	return idUserGroup;
    }
	public void setIdUserGroup(Integer isUserGroup) {
    	this.idUserGroup = isUserGroup;
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
