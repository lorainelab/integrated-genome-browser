package com.affymetrix.genometry.genopub;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.affymetrix.genometry.genopub.AnnotationGrouping;

public class GenomeVersion {
    
    private Integer  idGenomeVersion;
    private String   name;
    private Date     buildDate;
    private String   coordURI;
    private String   coordAuthority;
    private String   coordVersion;
    private String   coordSource;
    private String   coordTestRange;
    private Set      segments;
    private Set      aliases;
    private Integer  idOrganism;
    private Set      annotationGroupings;
    private Set      annotations;
    
    public Integer getIdGenomeVersion() {
        return idGenomeVersion;
    }
    public void setIdGenomeVersion(Integer idGenomeVersion) {
        this.idGenomeVersion = idGenomeVersion;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Date getBuildDate() {
        return buildDate;
    }
    public void setBuildDate(Date buildDate) {
        this.buildDate = buildDate;
    }
    public String getCoordURI() {
        return coordURI;
    }
    public void setCoordURI(String coordURI) {
        this.coordURI = coordURI;
    }
    public String getCoordAuthority() {
        return coordAuthority;
    }
    public void setCoordAuthority(String coordAuthority) {
        this.coordAuthority = coordAuthority;
    }
    public String getCoordVersion() {
        return coordVersion;
    }
    public void setCoordVersion(String coordVersion) {
        this.coordVersion = coordVersion;
    }
    public String getCoordSource() {
        return coordSource;
    }
    public void setCoordSource(String coordSource) {
        this.coordSource = coordSource;
    }
    public String getCoordTestRange() {
        return coordTestRange;
    }
    public void setCoordTestRange(String coordTestRange) {
        this.coordTestRange = coordTestRange;
    }
    public Set getSegments() {
        return segments;
    }
    public void setSegments(Set segments) {
        this.segments = segments;
    }
    public Set getAliases() {
        return aliases;
    }
    public void setAliases(Set aliases) {
        this.aliases = aliases;
    }
    public Set getAnnotationGroupings() {
        return annotationGroupings;
    }
    public void setAnnotationGroupings(Set annotationGroupings) {
        this.annotationGroupings = annotationGroupings;
    }
    
    @SuppressWarnings("unchecked")
	private List getRootAnnotationGroupings() {
		ArrayList rootGroupings = new ArrayList();
		for (AnnotationGrouping annotationGrouping : (Set<AnnotationGrouping>) this
		        .getAnnotationGroupings()) {
			if (annotationGrouping.getIdParentAnnotationGrouping() == null) {
				rootGroupings.add(annotationGrouping);
			}
		}
		return rootGroupings;
	}
    
    public AnnotationGrouping getRootAnnotationGrouping() {
    	List rootGroupings = this.getRootAnnotationGroupings();
    	if (rootGroupings.size() > 0) {
    		return AnnotationGrouping.class.cast(rootGroupings.get(0));
    	} else {
    		return null;
    	}
    }
    
	public Integer getIdOrganism() {
    	return idOrganism;
    }
	public void setIdOrganism(Integer idOrganism) {
    	this.idOrganism = idOrganism;
    }
	public Set getAnnotations() {
    	return annotations;
    }
	public void setAnnotations(Set annotations) {
    	this.annotations = annotations;
    }
	

}
