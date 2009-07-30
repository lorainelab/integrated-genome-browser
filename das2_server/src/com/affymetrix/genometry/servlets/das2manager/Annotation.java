package com.affymetrix.genometry.servlets.das2manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class Annotation implements Owned {
    
    private Integer   idAnnotation;
    private String    name;
    private String    summary;
    private String    description;
    private String    codeVisibility;
    private String    fileName;
    private Integer   idGenomeVersion;
    private Integer   idAnalysisType;
    private Integer   idExperimentMethod;
    private Integer   idExperimentPlatform;
    private Set       annotationGroupings;
    private Integer   idUser;
    private Integer   idSecurityGroup;
    
    
    public Integer getIdAnnotation() {
        return idAnnotation;
    }
    public void setIdAnnotation(Integer idAnnotation) {
        this.idAnnotation = idAnnotation;
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
    public String getCodeVisibility() {
        return codeVisibility;
    }
    public void setCodeVisibility(String codeVisibility) {
        this.codeVisibility = codeVisibility;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public Integer getIdGenomeVersion() {
        return idGenomeVersion;
    }
    public void setIdGenomeVersion(Integer idGenomeVersion) {
        this.idGenomeVersion = idGenomeVersion;
    }
    public Integer getIdAnalysisType() {
        return idAnalysisType;
    }
    public void setIdAnalysisType(Integer idAnalysisType) {
        this.idAnalysisType = idAnalysisType;
    }
    public Integer getIdExperimentMethod() {
        return idExperimentMethod;
    }
    public void setIdExperimentMethod(Integer idExperimentMethod) {
        this.idExperimentMethod = idExperimentMethod;
    }
    public Integer getIdExperimentPlatform() {
        return idExperimentPlatform;
    }
    public void setIdExperimentPlatform(Integer idExperimentPlatform) {
        this.idExperimentPlatform = idExperimentPlatform;
    }
	public Set getAnnotationGroupings() {
    	return annotationGroupings;
    }
	public void setAnnotationGroupings(Set annotationGroupings) {
    	this.annotationGroupings = annotationGroupings;
    }
	public Integer getIdUser() {
    	return idUser;
    }
	public void setIdUser(Integer idUser) {
    	this.idUser = idUser;
    }
	public Integer getIdSecurityGroup() {
    	return idSecurityGroup;
    }
	public void setIdSecurityGroup(Integer idSecurityGroup) {
    	this.idSecurityGroup = idSecurityGroup;
    }
	
	public boolean isOwner(Integer idUser) {
		if (this.getIdUser() != null && this.getIdUser().equals(idUser)) {
			return true;
		} else {
			return false;
		}
	}
	public boolean isSecurityGroup(Integer idSecurityGroup) {
		if (this.getIdSecurityGroup() != null && this.getIdSecurityGroup().equals(idSecurityGroup)) {
			return true;
		} else {
			return false;
		}
	}    
	public String getSummary() {
    	return summary;
    }
	public void setSummary(String summary) {
    	this.summary = summary;
    }

	
	@SuppressWarnings("unchecked")
	public Document getXML(Das2ManagerSecurity das2Security, DictionaryHelper dh, String genometry_manager_data_dir) {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Annotation");
		
		GenomeVersion genomeVersion = dh.getGenomeVersion(this.getIdGenomeVersion());
		
		root.addAttribute("idAnnotation", this.getIdAnnotation().toString());
		root.addAttribute("name", this.getName());
		root.addAttribute("summary", this.getSummary());
		root.addAttribute("description", this.getDescription());
		root.addAttribute("codeVisibility", this.getCodeVisibility());
		root.addAttribute("idGenomeVersion", this.getIdGenomeVersion() != null ? this.getIdGenomeVersion().toString() : "");
		root.addAttribute("idAnalysisType", this.getIdAnalysisType() != null ? this.getIdAnalysisType().toString() : "");
		root.addAttribute("idExperimentMethod", this.getIdExperimentMethod() != null ? this.getIdExperimentMethod().toString() : "");
		root.addAttribute("idExperimentPlatform", this.getIdExperimentPlatform() != null ? this.getIdExperimentPlatform().toString() : "");
		root.addAttribute("idUser", this.getIdUser() != null ? this.getIdUser().toString() : "");
		root.addAttribute("idSecurityGroup", this.getIdSecurityGroup() != null ? this.getIdSecurityGroup().toString() : "");
		root.addAttribute("owner", dh.getUserFullName(this.getIdUser()));
		root.addAttribute("genomeVersion", genomeVersion.getName());
		root.addAttribute("organism", dh.getOrganismName(genomeVersion.getIdOrganism()));
		root.addAttribute("securityGroup", dh.getSecurityGroupName(this.getIdSecurityGroup()));
		Element agsNode = root.addElement("AnnotationGroupings");
		for(AnnotationGrouping ag : (Set<AnnotationGrouping>)this.getAnnotationGroupings()) {
			Element agNode = agsNode.addElement("AnnotationGrouping");
			agNode.addAttribute("name", ag.getName());
		}
		Element filesNode = root.addElement("Files");
		
		String filePath = getDirectory(genometry_manager_data_dir);
	    File fd = new File(filePath);
	    if (fd.exists()) {
		    Element fileNode = filesNode.addElement("Dir");
			fileNode.addAttribute("name", this.getFileName());
			fileNode.addAttribute("url", filePath);
		    appendFileXML(filePath, fileNode, null);	    	
	    }
		
		root.addAttribute("canRead", das2Security.canRead(this) ? "Y" : "N");
		root.addAttribute("canWrite", das2Security.canWrite(this) ? "Y" : "N");
			
		return doc;
	}
	
	public static void appendFileXML(String filePath, Element parentNode, String subDirName) {
		File fd = new File(filePath);

		if (fd.isDirectory()) {
			String[] fileList = fd.list();
			for (int x = 0; x < fileList.length; x++) {
				String fileName = filePath + "/" + fileList[x];
				File f1 = new File(fileName);

				// Show the subdirectory in the name if we are not at the main folder level
				String displayName = "";
				if (subDirName != null) {
					displayName = subDirName + "/" + fileList[x];
				} else {
					displayName = f1.getName();
				}

				if (f1.isDirectory()) {
					Element fileNode = parentNode.addElement("Dir");
					fileNode.addAttribute("name", displayName);
					fileNode.addAttribute("url", fileName);
					appendFileXML(fileName, fileNode,
					        subDirName != null ? subDirName + "/"
					                + f1.getName() : f1.getName());
				} else {
					Element fileNode = parentNode.addElement("File");
					fileNode.addAttribute("name", displayName);
					fileNode.addAttribute("url", fileName);
				}
			}
		}
	}
	
	public void removeFiles(String genometry_manager_data_dir) throws IOException {
		
		String filePath = getDirectory(genometry_manager_data_dir);
	    File dir = new File(filePath);
	    
	    if (dir.exists()) {
		    // Delete the files in the directory
		    String[] childFileNames = dir.list();
		    if (childFileNames != null) {
				for (int x = 0; x < childFileNames.length; x++) {
					String fileName = filePath + "/" + childFileNames[x];
					File f = new File(fileName);
					f.delete();
				}
		    	
		    }
			
			// Delete the annotation directory
			dir.delete();	    	
	    }
	}
	
	public String getQualifiedFileName(String genometry_manager_data_dir) {
		if (this.getFileName() == null || this.getFileName().equals("")) {
			return "";
		}
		String filePath =  genometry_manager_data_dir + this.getFileName();
		File file = new File(filePath);
		
		// If there is only one annotation file in the directory, append the file name to the file path.
		if (file != null && file.list() != null && file.list().length == 1) {
			String[] childFileNames = file.list();
			filePath += "/" + childFileNames[0];
		}
		
		return filePath;
		
	}
	
	public String getDirectory(String genometry_manager_data_dir) {
		return genometry_manager_data_dir  + this.getFileName();
	}

   
}
