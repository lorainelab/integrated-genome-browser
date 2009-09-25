package com.affymetrix.genometry.servlets.das2manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.affymetrix.genometryImpl.Propertied;

public class Annotation implements Owned, Propertied {

	public static final String PROP_NAME                = "name";
    public static final String PROP_SUMMARY             = "summary";
    public static final String PROP_DESCRIPTION         = "description";
    public static final String PROP_OWNER               = "owner";
    public static final String PROP_GROUP               = "group";
    public static final String PROP_VISIBILITY          = "visibility";
    public static final String PROP_INSTITUTE           = "institute";
    public static final String PROP_ANALYSIS_TYPE       = "analysis_type";
    public static final String PROP_EXPERIMENT_METHOD   = "experiment_method";
    public static final String PROP_EXPERIMENT_PLATFORM = "experiment_platform";
    
    private Integer             idAnnotation;
    private String              name;
    private String              summary;
    private String              description;
    private String              codeVisibility;
    private String              fileName;
    private Integer             idGenomeVersion;
    private Integer             idAnalysisType;
    private Integer             idExperimentMethod;
    private Integer             idExperimentPlatform;
    private Set                 annotationGroupings;
    private Integer             idUser;
    private Integer             idUserGroup;
    private String              createdBy;
    private java.sql.Date       createDate;
    
    private Map<String, Object> props;  // tag/value representation of annotation properties
    
    
    
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
	public Integer getIdUserGroup() {
    	return idUserGroup;
    }
	public void setIdUserGroup(Integer idUserGroup) {
    	this.idUserGroup = idUserGroup;
    }
	
	public boolean isOwner(Integer idUser) {
		if (this.getIdUser() != null && this.getIdUser().equals(idUser)) {
			return true;
		} else {
			return false;
		}
	}
	public boolean isUserGroup(Integer idUserGroup) {
		if (this.getIdUserGroup() != null && this.getIdUserGroup().equals(idUserGroup)) {
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
		root.addAttribute("idUserGroup", this.getIdUserGroup() != null ? this.getIdUserGroup().toString() : "");
		root.addAttribute("owner", dh.getUserFullName(this.getIdUser()));
		root.addAttribute("genomeVersion", genomeVersion.getName());
		root.addAttribute("organism", dh.getOrganismName(genomeVersion.getIdOrganism()));
		root.addAttribute("securityGroup", dh.getUserGroupName(this.getIdUserGroup()));
		root.addAttribute("createdBy", this.getCreatedBy() != null ? this.getCreatedBy() : "");
		root.addAttribute("createDate", this.getCreateDate() != null ? Util.formatDate(this.getCreateDate()) : "");
		Element agsNode = root.addElement("AnnotationGroupings");
		for(AnnotationGrouping ag : (Set<AnnotationGrouping>)this.getAnnotationGroupings()) {
			Element agNode = agsNode.addElement("AnnotationGrouping");
			agNode.addAttribute("name", ag.getQualifiedName());
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

					long kb = Util.getKilobytes(f1.length());
					String kilobytes = kb + " kb";
					
					fileNode.addAttribute("name", displayName);
					fileNode.addAttribute("url", fileName);
					fileNode.addAttribute("size", kilobytes);
					fileNode.addAttribute("lastModified", Util.formatDate(new java.sql.Date(f1.lastModified())));

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
	
	public Map<String, Object> loadProps(DictionaryHelper dictionaryHelper) {
		props = new TreeMap<String, Object>();
		props.put(PROP_NAME, this.getName());
		props.put(PROP_DESCRIPTION, this.getDescription());
		props.put(PROP_SUMMARY, this.getSummary());
		props.put(PROP_VISIBILITY,  Visibility.getDisplay(this.getCodeVisibility()));
		props.put(PROP_OWNER, this.getIdUser() != null ? dictionaryHelper.getUserFullName(this.getIdUser()) : "");
		props.put(PROP_GROUP, this.getIdUserGroup() != null ? dictionaryHelper.getUserGroupName(this.getIdUserGroup()) : "");
		props.put(PROP_ANALYSIS_TYPE, dictionaryHelper.getAnalysisType(this.getIdAnalysisType()));
		props.put(PROP_EXPERIMENT_METHOD, dictionaryHelper.getExperimentMethod(this.getIdExperimentMethod()));
		props.put(PROP_EXPERIMENT_PLATFORM, dictionaryHelper.getExperimentPlatform(this.getIdExperimentPlatform()));
		props.put(PROP_EXPERIMENT_PLATFORM, dictionaryHelper.getExperimentPlatform(this.getIdExperimentPlatform()));
		return props;
    }

	public Map<String,Object> getProperties() {
		return props;
	}
	public Map<String,Object> cloneProperties() {
		return props;
	}
	
	public Object getProperty(String key) {
		if (props != null) {
			return props.get(key);
		} else {
			return null;
		}
	}
	public boolean setProperty(String key, Object val) {
		if (props != null) {
			props.put(key, val);
			return true;
		} else {
			return false;
		}
	}
	public String getCreatedBy() {
    	return createdBy;
    }
	public void setCreatedBy(String createdBy) {
    	this.createdBy = createdBy;
    }
	public java.sql.Date getCreateDate() {
    	return createDate;
    }
	public void setCreateDate(java.sql.Date createDate) {
    	this.createDate = createDate;
    }
   
}
