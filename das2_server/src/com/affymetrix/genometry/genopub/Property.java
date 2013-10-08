package com.affymetrix.genometry.genopub;

import java.util.Set;


public class Property implements Owned {
    
    private Integer idProperty;
    private String  name;
    private String  isActive;
    private Integer idUser;
    private String  codePropertyType;
    private Integer sortOrder;
    private Set     options;
    
    public Integer getIdProperty() {
        return idProperty;
    }
    public void setIdProperty(Integer idProperty) {
        this.idProperty = idProperty;
    }    
    public String getIsActive() {
        return isActive;
    }
    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
	public Integer getIdUser() {
    	return idUser;
    }
	public void setIdUser(Integer idUser) {
    	this.idUser = idUser;
    }
	public boolean isOwner(Integer idUser) {
		if (this.getIdUser() != null && this.getIdUser().equals(idUser)) {
			return true;
		} else {
			return false;
		}
	}
	public boolean isUserGroup(Integer idUserGroup) {
		return false;
	}
  public String getCodePropertyType() {
    return codePropertyType;
  }
  public void setCodePropertyType(String codePropertyType) {
    this.codePropertyType = codePropertyType;
  }
  public Set getOptions() {
    return options;
  }
  public void setOptions(Set options) {
    this.options = options;
  }
  public Integer getSortOrder() {
    return sortOrder;
  }
  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

}
