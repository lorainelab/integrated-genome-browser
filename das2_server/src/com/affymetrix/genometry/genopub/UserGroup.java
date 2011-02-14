package com.affymetrix.genometry.genopub;

import java.io.Serializable;
import java.util.Set;

public class UserGroup implements Serializable {
	private Integer    idUserGroup;
	private String     name;
	private String     contact;
	private String     email;
	private Set        members;
	private Set        collaborators;
	private Set        managers;
	private Set        institutes;

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
	public Set getInstitutes() {
	    return institutes;
	}
	public void setInstitutes(Set institutes) {
	    this.institutes = institutes;
	}
	public String getEmail() {
    	return email;
    }
	public void setEmail(String email) {
    	this.email = email;
    }
	public String getContact() {
    	return contact;
    }
	public void setContact(String contact) {
    	this.contact = contact;
    }	

}
