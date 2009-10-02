package com.affymetrix.genometry.genopub;

import java.util.Set;

public class Organism {
    
    private Integer idOrganism;
    private String  name;
    private String  commonName;
    private String  binomialName;
    private String  NCBITaxID;
    private Integer sortOrder;
    private Set     genomeVersions;
    
    public Integer getIdOrganism() {
        return idOrganism;
    }
    public void setIdOrganism(Integer idOrganism) {
        this.idOrganism = idOrganism;
    }

    public Set getGenomeVersions() {
        return genomeVersions;
    }
    public void setGenomeVersions(Set genomeVersions) {
        this.genomeVersions = genomeVersions;
    }
    public String getCommonName() {
        return commonName;
    }
    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }
    public Integer getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    public String getBinomialName() {
        return binomialName;
    }
    public void setBinomialName(String binomialName) {
        this.binomialName = binomialName;
    }
    public String getNCBITaxID() {
        return NCBITaxID;
    }
    public void setNCBITaxID(String taxID) {
        NCBITaxID = taxID;
    }
	public String getName() {
    	return name;
    }
	public void setName(String name) {
    	this.name = name;
    }
    
    

}
