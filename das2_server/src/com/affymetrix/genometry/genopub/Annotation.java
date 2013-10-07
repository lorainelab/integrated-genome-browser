package com.affymetrix.genometry.genopub;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.affymetrix.genometry.genopub.AnnotationGrouping;
import com.affymetrix.genometry.genopub.DictionaryHelper;
import com.affymetrix.genometry.genopub.GenoPubSecurity;
import com.affymetrix.genometry.genopub.GenomeVersion;
import com.affymetrix.genometry.genopub.Institute;
import com.affymetrix.genometry.genopub.Owned;
import com.affymetrix.genometry.genopub.User;
import com.affymetrix.genometry.genopub.UserGroup;
import com.affymetrix.genometry.genopub.Util;
import com.affymetrix.genometry.genopub.Visibility;
import com.affymetrix.genometryImpl.parsers.useq.USeqUtilities;

public class Annotation implements Serializable, Owned {

	public static final String PROP_NAME                = "name";
	public static final String PROP_SUMMARY             = "summary";
	public static final String PROP_DESCRIPTION         = "description";
	public static final String PROP_OWNER               = "owner";
	public static final String PROP_OWNER_EMAIL         = "owner_institute";
	public static final String PROP_OWNER_INSTITUTE     = "owner_email";
	public static final String PROP_GROUP               = "group";
	public static final String PROP_GROUP_CONTACT       = "group_contact";
	public static final String PROP_GROUP_EMAIL         = "group_email";
	public static final String PROP_GROUP_INSTITUTE     = "group_institute";
	public static final String PROP_VISIBILITY          = "visibility";
	public static final String PROP_INSTITUTE           = "institute";
	public static final String PROP_ANALYSIS_TYPE       = "analysis_type";
	public static final String PROP_EXPERIMENT_METHOD   = "experiment_method";
	public static final String PROP_EXPERIMENT_PLATFORM = "experiment_platform";
	public static final String PROP_URL                 = "url";


	private Integer             idAnnotation;
	private String              name;
	private String              summary;
	private String              description;
	private String              codeVisibility;
	private String              fileName;
	private String              dataPath;
	private Integer             idGenomeVersion;
	private Set                 annotationGroupings;
	private Integer             idUser;
	private Integer             idUserGroup;
	private UserGroup           userGroup;
	private Integer             idInstitute;
	private String              createdBy;
	private java.sql.Date       createDate;
	private String              isLoaded;
	private Set                 collaborators;
	private Set                 annotationProperties;

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
	/**This is the name of the directory that contains the annotation files*/
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getDataPath() {
    return dataPath;
  }
  public void setDataPath(String dataPath) {
    this.dataPath = dataPath;
  }
  public Integer getIdGenomeVersion() {
		return idGenomeVersion;
	}
	public void setIdGenomeVersion(Integer idGenomeVersion) {
		this.idGenomeVersion = idGenomeVersion;
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

	public String getNumber() {
		if (this.getIdAnnotation() != null) {
			return "A" + this.getIdAnnotation();
		} else {
			return "";
		}
	}

	@SuppressWarnings("unchecked")
	public Document getXML(GenoPubSecurity genoPubSecurity, DictionaryHelper dh, String data_root) throws Exception {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Annotation");

		GenomeVersion genomeVersion = dh.getGenomeVersion(this.getIdGenomeVersion());
		if (genomeVersion == null) {
			Logger.getLogger(Annotation.class.getName()).log(Level.SEVERE,"Unable to find genome version " + this.getIdGenomeVersion() + " for annotation " + this.getName());
			throw new Exception("Unable to find genome version " + this.getIdGenomeVersion() + " for annotation " + this.getName());
		}

		root.addAttribute("idAnnotation", this.getIdAnnotation().toString());
		root.addAttribute("number", this.getNumber());
		root.addAttribute("label", this.getName());
		root.addAttribute("name", this.getName());
		root.addAttribute("summary", this.getSummary());
		root.addAttribute("description", this.getDescription());
		root.addAttribute("codeVisibility", this.getCodeVisibility());
		root.addAttribute("idGenomeVersion", this.getIdGenomeVersion() != null ? this.getIdGenomeVersion().toString() : "");
		root.addAttribute("idUser", this.getIdUser() != null ? this.getIdUser().toString() : "");
		root.addAttribute("idUserGroup", this.getIdUserGroup() != null ? this.getIdUserGroup().toString() : "");
		root.addAttribute("idInstitute", this.getIdInstitute() != null ? this.getIdInstitute().toString() : "");
		root.addAttribute("owner", dh.getUserFullName(this.getIdUser()));
		root.addAttribute("genomeVersion", genomeVersion != null ? genomeVersion.getName() : "");
		root.addAttribute("organism", dh.getOrganismName(genomeVersion.getIdOrganism()));
		root.addAttribute("securityGroup", dh.getUserGroupName(this.getIdUserGroup()));
		root.addAttribute("createdBy", this.getCreatedBy() != null ? this.getCreatedBy() : "");
		root.addAttribute("createDate", this.getCreateDate() != null ? Util.formatDate(this.getCreateDate()) : "");
		root.addAttribute("annotationGroupingCount", Integer.valueOf(this.getAnnotationGroupings().size()).toString());
		root.addAttribute("number", this.getNumber());

		// Only show annotation groupings and annotation files for detail
		// (when data_root is provided).
		// Also look for files that can be linked to the UCSC Genome Browser
		if (data_root != null) {
			Element agsNode = root.addElement("AnnotationGroupings");
			for(AnnotationGrouping ag : (Set<AnnotationGrouping>)this.getAnnotationGroupings()) {
				Element agNode = agsNode.addElement("AnnotationGrouping");
				agNode.addAttribute("name", ag.getQualifiedName());
			}
			Element filesNode = root.addElement("Files");

			String filePath = getDirectory(data_root);
			File fd = new File(filePath);
			if (fd.exists()) {
				Element fileNode = filesNode.addElement("Dir");
				fileNode.addAttribute("name", this.getFileName());
				fileNode.addAttribute("url", filePath);
				String ucscLinkFile = appendFileXML(filePath, fileNode, null);
				root.addAttribute("ucscLinkFile", ucscLinkFile);
			}			
		}

		// Show list of collaborators.  Only show for
		// annotation detail (when data_root is provided)
		if (data_root != null) {
			if (getCollaborators() != null) {
				Element collaboratorsNode = root.addElement("Collaborators");
				for(Iterator i = getCollaborators().iterator(); i.hasNext();) {
					User u = (User)i.next();
					Element userNode = collaboratorsNode.addElement("User");
					userNode.addAttribute("idUser", u.getIdUser().toString());  
					userNode.addAttribute("name", u.getName());
					userNode.addAttribute("userDisplayName", u.getUserDisplayName());
				}
			}
		}

		// Show list of possible collaborators.  Only show
		// for annotation detail (when data_root is provided).
		if (data_root != null) {
			if (getUserGroup() != null) {
				TreeMap<String, User> possibleCollaboratorMap = new TreeMap<String, User>();

				Element possibleCollaboratorsNode = root.addElement("PossibleCollaborators");

				for(Iterator i = getUserGroup().getMembers().iterator(); i.hasNext();) {
					User user = (User)i.next();
					possibleCollaboratorMap.put(user.getName(), user);
				}
				for(Iterator i = getUserGroup().getCollaborators().iterator(); i.hasNext();) {
					User user = (User)i.next();
					possibleCollaboratorMap.put(user.getName(), user);
				}
				for(Iterator i = getUserGroup().getManagers().iterator(); i.hasNext();) {
					User user = (User)i.next();
					possibleCollaboratorMap.put(user.getName(), user);
				}

				for(Iterator i = possibleCollaboratorMap.keySet().iterator(); i.hasNext();) {
					String name = (String)i.next();
					User user = possibleCollaboratorMap.get(name);
					Element userNode = possibleCollaboratorsNode.addElement("User");
					userNode.addAttribute("idUser", user.getIdUser().toString());  
					userNode.addAttribute("name", user.getName());
					userNode.addAttribute("userDisplayName", user.getUserDisplayName());
				}

			}

		}

		// Show list of possible institutes.  Only show for
		// annotation detail (when data_root is provided).
		if (data_root != null) {
			if (getUserGroup() != null) {
				Element institutesNode = root.addElement("PossibleInstitutes");

				Element emptyNode = institutesNode.addElement("Institute");
				emptyNode.addAttribute("idInstitute", "");  
				emptyNode.addAttribute("name", "");

				for(Iterator i = userGroup.getInstitutes().iterator(); i.hasNext();) {
					Institute institute = (Institute)i.next();
					Element userNode = institutesNode.addElement("Institute");
					userNode.addAttribute("idInstitute", institute.getIdInstitute().toString());  
					userNode.addAttribute("name", institute.getName());
				}

			}

		}

		// Show list annotation properties.
		// Only show for annotation detail (when data_root is provided).
		if (data_root != null) {
			Element propertiesNode = root.addElement("AnnotationProperties");
			for (Property property : dh.getPropertyList()) {

				Element propNode = propertiesNode.addElement("AnnotationProperty");

				AnnotationProperty ap = null;
				for(Iterator i = getAnnotationProperties().iterator(); i.hasNext();) {
					AnnotationProperty annotationProperty = (AnnotationProperty)i.next();
					if (annotationProperty.getProperty().getIdProperty().equals(property.getIdProperty())) {
						ap = annotationProperty;
						break;
					}
				}

				propNode.addAttribute("idAnnotationProperty", ap != null ? ap.getIdAnnotationProperty().toString() : "");  
				propNode.addAttribute("name", property.getName());
				propNode.addAttribute("value", ap != null && ap.getValue() != null ? ap.getValue() : "");
				propNode.addAttribute("codePropertyType", property.getCodePropertyType());
				propNode.addAttribute("idProperty", property.getIdProperty().toString());

				if (ap != null && ap.getValues() != null && ap.getValues().size() > 0) {
					for (Iterator i1 = ap.getValues().iterator(); i1.hasNext();) {
						AnnotationPropertyValue av = (AnnotationPropertyValue)i1.next();
						Element valueNode = propNode.addElement("AnnotationPropertyValue");
						valueNode.addAttribute("idAnnotationPropertyValue", av.getIdAnnotationPropertyValue().toString());
						valueNode.addAttribute("value", av.getValue() != null ? av.getValue() : "");
					}
				}
				if (property.getCodePropertyType().equals(PropertyType.URL)) {
					// Add an empty value for URL
					Element emptyNode = propNode.addElement("AnnotationPropertyValue");
					emptyNode.addAttribute("idAnnotationPropertyValue", "");
					emptyNode.addAttribute("value", "Enter URL here...");
				}

				if (property.getOptions() != null && property.getOptions().size() > 0) {
					for (Iterator i1 = property.getOptions().iterator(); i1.hasNext();) {
						PropertyOption option = (PropertyOption)i1.next();
						Element optionNode = propNode.addElement("PropertyOption");
						optionNode.addAttribute("idPropertyOption", option.getIdPropertyOption().toString());
						optionNode.addAttribute("name", option.getName());
						boolean isSelected = false;
						if (ap != null && ap.getOptions() != null) {
							for (Iterator i2 = ap.getOptions().iterator(); i2.hasNext();) {
								PropertyOption optionSelected = (PropertyOption)i2.next();
								if (optionSelected.getIdPropertyOption().equals(option.getIdPropertyOption())) {
									isSelected = true;
									break;
								}
							}
						}
						optionNode.addAttribute("selected", isSelected ? "Y" : "N");
					}
				}
			}      
		}


		root.addAttribute("canRead", genoPubSecurity.canRead(this) ? "Y" : "N");
		root.addAttribute("canWrite", genoPubSecurity.canWrite(this) ? "Y" : "N");

		return doc;
	}

	/**Returns 'none' if no files available for UCSC linking, 'convert' for files requiring conversion, or 'link' if they are ready to go.*/
	public static String appendFileXML(String filePath, Element parentNode, String subDirName) {
		File fd = new File(filePath);
		String ucscLinkFile = "none";
		if (fd.isDirectory()) {
			String[] fileList = fd.list();
			for (int x = 0; x < fileList.length; x++) {
				String fileName = filePath + "/" + fileList[x];
				File f1 = new File(fileName);
				
				//link file?
				if (fileList[x].endsWith(".useq") && ucscLinkFile.equals("none")) ucscLinkFile = "convert";
				else if (fileList[x].endsWith(".bb") || fileList[x].endsWith(".bw") || fileList[x].endsWith(".bam")) ucscLinkFile = "link";

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
		return ucscLinkFile;
	}

	public void removeFiles(String data_root) throws IOException {

		String filePath = getDirectory(data_root);
		File dir = new File(filePath);

		if (dir.exists()) {
			// Delete the files in the directory
			String[] childFileNames = dir.list();
			if (childFileNames != null) {
				for (int x = 0; x < childFileNames.length; x++) {
					String fileName = filePath + "/" + childFileNames[x];
					File f = new File(fileName);
					boolean success = f.delete();
					if (!success) {
						Logger.getLogger(Annotation.class.getName()).log(Level.WARNING, "Unable to delete file " + fileName);
					}
				}

			}

			// Delete the annotation directory
			boolean success = dir.delete();	    	
			if (!success) {
				Logger.getLogger(Annotation.class.getName()).log(Level.WARNING, "Unable to delete directory " + filePath);
			}
		}
	}


	public List<File> getFiles(String data_root) throws IOException {

		ArrayList<File> files = new ArrayList<File>();

		String filePath = getDirectory(data_root);
		File dir = new File(filePath);

		if (dir.exists()) {
			String[] childFileNames = dir.list();
			if (childFileNames != null) {
				for (int x = 0; x < childFileNames.length; x++) {
					String fileName = filePath + "/" + childFileNames[x];
					File f = new File(fileName);
					files.add(f);
				}

			}
		}

		return files;
	}
	public boolean isBarGraphData(String data_root) throws IOException {
		boolean isExtension = false;
		String filePath = getDirectory(data_root);
		File dir = new File(filePath);

		if (dir.exists()) {
			// Delete the files in the directory
			String[] childFileNames = dir.list();
			if (childFileNames != null) {
				for (int x = 0; x < childFileNames.length; x++) {
					if (childFileNames[x].endsWith("bar")) {
						isExtension = true;
						break;
					}
				}

			}
		}
		return isExtension;
	}

	public boolean isBamData(String data_root) throws IOException {
		boolean isExtension = false;
		String filePath = getDirectory(data_root);
		File dir = new File(filePath);

		if (dir.exists()) {
			String[] childFileNames = dir.list();
			if (childFileNames != null) {
				for (int x = 0; x < childFileNames.length; x++) {
					if (childFileNames[x].endsWith("bam")) {
						isExtension = true;
						break;
					}
				}

			}
		}
		return isExtension;
	}

	public boolean isUseqGraphData(String data_root) throws IOException {
		boolean isExtension = false;
		String filePath = getDirectory(data_root);
		File dir = new File(filePath);
		if (dir.exists()) {
			String[] childFileNames = dir.list();
			if (childFileNames != null) {
				for (int x = 0; x < childFileNames.length; x++) {
					if (USeqUtilities.USEQ_ARCHIVE.matcher(childFileNames[x]).matches() ) {
						isExtension = true;
						break;
					}
				}
			}
		}
		return isExtension;
	}

	public int getFileCount(String data_root) throws IOException {
		int fileCount = 0;
		String filePath = getDirectory(data_root);
		File dir = new File(filePath);

		if (dir.exists()) {
			// Delete the files in the directory
			String[] childFileNames = dir.list();
			if (childFileNames != null) {
				fileCount = childFileNames.length;
			}
		}
		return fileCount;
	}

	public String getQualifiedFileName(String data_root) {
		if (this.getFileName() == null || this.getFileName().length() == 0) {
			return "";
		}
		
		String filePath =  getDirectory(data_root);
		File file = new File(filePath);

		File[] files = file.listFiles();

		if (files != null) {
			//one file return file
			if (files.length == 1){
				String[] childFileNames = file.list();
				filePath += "/" + childFileNames[0];
			}
			//multiple files, might contain a useq file with URL link files (xxx.bw, xxx.bb) that should be skipped or bam and it's associated bai index file
			else {
				for (File f: files){
					String fileName = f.getName();
					//bam?
					if (fileName.endsWith("bam")) {
						filePath += "/" + fileName;
						break;
					}
					//useq?
					else if (fileName.endsWith(USeqUtilities.USEQ_EXTENSION_WITH_PERIOD)) {
						filePath += "/" + fileName;
						break;
					}
				}
			}
			//make sure it's not a ucsc big file xxx.bw, or xxx.bb
			if (filePath.endsWith(".bb") || filePath.endsWith(".bw")) filePath = "";
			
			//bar files should return the directory so don't do anything
			
		}
		return filePath;
	}

	public String getDirectory(String data_root) {
	  String dataPath = null;
	  if (this.getDataPath() != null && this.getDataPath().length() != 0) {
      dataPath = this.getDataPath();
    } else {
      dataPath = data_root;
    }
	  return dataPath + this.getFileName();
	}


	@SuppressWarnings("unchecked")
	public Map<String, Object> loadProps(DictionaryHelper dictionaryHelper) {
		props = new TreeMap<String, Object>();
		props.put(PROP_NAME, this.getName());
		props.put(PROP_DESCRIPTION, this.getDescription() != null ? Util.removeHTMLTags(this.getDescription()) : "");
		props.put(PROP_SUMMARY, this.getSummary() != null ? Util.removeHTMLTags(this.getSummary()) : "");
		props.put(PROP_VISIBILITY,  Visibility.getDisplay(this.getCodeVisibility()));
		props.put(PROP_OWNER, this.getIdUser() != null ? dictionaryHelper.getUserFullName(this.getIdUser()) : "");
		props.put(PROP_OWNER_EMAIL, this.getIdUser() != null ? dictionaryHelper.getUserEmail(this.getIdUser()) : "");
		props.put(PROP_OWNER_INSTITUTE, this.getIdUser() != null ? dictionaryHelper.getUserInstitute(this.getIdUser()) : "");
		props.put(PROP_GROUP, this.getIdUserGroup() != null ? dictionaryHelper.getUserGroupName(this.getIdUserGroup()) : "");
		props.put(PROP_GROUP_CONTACT, this.getIdUserGroup() != null ? dictionaryHelper.getUserGroupContact(this.getIdUserGroup()) : "");
		props.put(PROP_GROUP_EMAIL, this.getIdUserGroup() != null ? dictionaryHelper.getUserGroupEmail(this.getIdUserGroup()) : "");
		props.put(PROP_GROUP_INSTITUTE, this.getIdInstitute() != null ? dictionaryHelper.getInstituteName(this.getIdInstitute()) : "");

		for (AnnotationProperty ap : (Set<AnnotationProperty>)this.getAnnotationProperties()) {
			props.put(ap.getName(), ap.getValue() != null ? ap.getValue() : "");
		}
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

	public String getIsLoaded() {
		return isLoaded;
	}
	public void setIsLoaded(String isLoaded) {
		this.isLoaded = isLoaded;
	}
	public UserGroup getUserGroup() {
		return this.userGroup;
	}
	public void setUserGroup(UserGroup userGroup) {
		this.userGroup = userGroup;
	}
	public Set getCollaborators() {
		return collaborators;
	}
	public void setCollaborators(Set collaborators) {
		this.collaborators = collaborators;
	}
	public Integer getIdInstitute() {
		return idInstitute;
	}
	public void setIdInstitute(Integer idInstitute) {
		this.idInstitute = idInstitute;
	}
	public Set getAnnotationProperties() {
		return annotationProperties;
	}
	public void setAnnotationProperties(Set annotationProperties) {
		this.annotationProperties = annotationProperties;
	}

}
