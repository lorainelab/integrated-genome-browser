package com.affymetrix.genometry.servlets.das2manager;

import java.sql.Date;
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
    private String             createdBy;
    private Date               createDate;

    
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
	
	public String getQualifiedName() {
		String qualifiedName = "/";
		AnnotationGrouping parent = this.getParentAnnotationGrouping();
		if (parent != null) {
			qualifiedName = getName();
			qualifiedName = recurseGetParentName(qualifiedName);			
		}
		return qualifiedName;
	}
	
	private String recurseGetParentName(String qualifiedName) {
		AnnotationGrouping parent = this.getParentAnnotationGrouping();
		
		// Stop before the root annotation grouping
		if (parent != null && parent.getParentAnnotationGrouping() != null) {
			if (parent != null && parent.getName() != null) {
				qualifiedName = parent.getName() + "/" + qualifiedName;
			
				qualifiedName = parent.recurseGetParentName(qualifiedName);
			}
		}
		return qualifiedName;
	}
	public String getCreatedBy() {
    	return createdBy;
    }
	public void setCreatedBy(String createdBy) {
    	this.createdBy = createdBy;
    }
	public Date getCreateDate() {
    	return createDate;
    }
	public void setCreateDate(Date createDate) {
    	this.createDate = createDate;
    }
}
