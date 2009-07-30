package com.affymetrix.genometry.servlets.das2manager;

import java.util.Set;

public class AnnotationGrouping implements Owned {
    
    private Integer            idAnnotationGrouping;
    private String             name;
    private String             description;
    private Integer            idParentAnnotationGrouping;
    private AnnotationGrouping parentAnnotationGrouping;
    private Set                annotationGroupings;
    private Set                annotations;
    private Integer            idUser;
    private Integer            idGenomeVersion;

    
    public Integer getIdAnnotationGrouping() {
        return idAnnotationGrouping;
    }
    public void setIdAnnotationGrouping(Integer idAnnotationGrouping) {
        this.idAnnotationGrouping = idAnnotationGrouping;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Set getAnnotationGroupings() {
        return annotationGroupings;
    }
    public void setAnnotationGroupings(Set annotationGroupings) {
        this.annotationGroupings = annotationGroupings;
    }
    public Integer getIdParentAnnotationGrouping() {
        return idParentAnnotationGrouping;
    }
    public void setIdParentAnnotationGrouping(Integer idParentAnnotationGrouping) {
        this.idParentAnnotationGrouping = idParentAnnotationGrouping;
    }
    public Set getAnnotations() {
        return annotations;
    }
    public void setAnnotations(Set annotations) {
        this.annotations = annotations;
    }
	public AnnotationGrouping getParentAnnotationGrouping() {
    	return parentAnnotationGrouping;
    }
	public void setParentAnnotationGrouping(
            AnnotationGrouping parentAnnotationGrouping) {
    	this.parentAnnotationGrouping = parentAnnotationGrouping;
    }
	
	@SuppressWarnings("unchecked")
	public boolean hasVisibility(String codeVisibility) {
		boolean hasVisibility = false;
		for(Annotation a : (Set<Annotation>)this.annotations) {
			if (a.getCodeVisibility().equals(codeVisibility)) {
				hasVisibility = true;
				break;
			}
		}
		return hasVisibility;
		
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
	
	@SuppressWarnings("unchecked")
	public boolean isSecurityGroup(Integer idSecurityGroup) {
		boolean isSecurityGroup = false;
		for(Annotation a : (Set<Annotation>)this.annotations) {
			if (a.isSecurityGroup(idSecurityGroup)) {
				isSecurityGroup = true;
				break;
			}
		}
		return isSecurityGroup;
	}
	public Integer getIdGenomeVersion() {
    	return idGenomeVersion;
    }
	public void setIdGenomeVersion(Integer idGenomeVersion) {
    	this.idGenomeVersion = idGenomeVersion;
    } 
}
