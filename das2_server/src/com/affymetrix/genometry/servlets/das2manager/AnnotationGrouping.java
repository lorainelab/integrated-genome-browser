package com.affymetrix.genometry.servlets.das2manager;

import java.util.Set;

public class AnnotationGrouping {
    
    private Integer            idAnnotationGrouping;
    private String             name;
    private String             description;
    private Integer            idParentAnnotationGrouping;
    private AnnotationGrouping parentAnnotationGrouping;
    private Set                annotationGroupings;
    private Set                annotations;
    private Integer            idUserGroup;
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

	public Integer getIdGenomeVersion() {
    	return idGenomeVersion;
    }
	public void setIdGenomeVersion(Integer idGenomeVersion) {
    	this.idGenomeVersion = idGenomeVersion;
    }
	public Integer getIdUserGroup() {
    	return idUserGroup;
    }
	public void setIdUserGroup(Integer idUserGroup) {
    	this.idUserGroup = idUserGroup;
    } 
}
