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
	private Integer             idGenomeVersion;
	private Integer             idAnalysisType;
	private Integer             idExperimentMethod;
	private Integer             idExperimentPlatform;
	private Set                 annotationGroupings;
	private Integer             idUser;
	private Integer             idUserGroup;
	private UserGroup           userGroup;
  private Integer             idInstitute;
	private String              createdBy;
	private java.sql.Date       createDate;
	private String              isLoaded;
	private Set                 collaborators;


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
	public Document getXML(GenoPubSecurity genoPubSecurity, DictionaryHelper dh, String data_root) throws Exception {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Annotation");

		GenomeVersion genomeVersion = dh.getGenomeVersion(this.getIdGenomeVersion());
		if (genomeVersion == null) {
			Logger.getLogger(Annotation.class.getName()).log(Level.SEVERE,"Unable to find genome version " + this.getIdGenomeVersion() + " for annotation " + this.getName());
			throw new Exception("Unable to find genome version " + this.getIdGenomeVersion() + " for annotation " + this.getName());
		}

		root.addAttribute("idAnnotation", this.getIdAnnotation().toString());
		root.addAttribute("label", this.getName());
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
    root.addAttribute("idInstitute", this.getIdInstitute() != null ? this.getIdInstitute().toString() : "");
		root.addAttribute("owner", dh.getUserFullName(this.getIdUser()));
		root.addAttribute("genomeVersion", genomeVersion != null ? genomeVersion.getName() : "");
		root.addAttribute("organism", dh.getOrganismName(genomeVersion.getIdOrganism()));
		root.addAttribute("securityGroup", dh.getUserGroupName(this.getIdUserGroup()));
		root.addAttribute("createdBy", this.getCreatedBy() != null ? this.getCreatedBy() : "");
		root.addAttribute("createDate", this.getCreateDate() != null ? Util.formatDate(this.getCreateDate()) : "");
		root.addAttribute("annotationGroupingCount", Integer.valueOf(this.getAnnotationGroupings().size()).toString());

		// Only show annotation groupings and annotation files for detail
		// (when data_root is provided).
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
				appendFileXML(filePath, fileNode, null);	    	
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


		root.addAttribute("canRead", genoPubSecurity.canRead(this) ? "Y" : "N");
		root.addAttribute("canWrite", genoPubSecurity.canWrite(this) ? "Y" : "N");

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
			// Delete the files in the directory
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
			// Delete the files in the directory
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
		if (this.getFileName() == null || this.getFileName().equals("")) {
			return "";
		}
		String filePath =  data_root + this.getFileName();
		File file = new File(filePath);

		File[] files = file.listFiles();

		if (files != null) {
			//one file return file
			if (files.length == 1){
				String[] childFileNames = file.list();
				filePath += "/" + childFileNames[0];
			}
			//two file possibly bam
			else if (files.length == 2){
				for (int i=0; i< files.length; i++){
					if (files[i].getName().endsWith("bam")) {
						filePath += "/" + files[i].getName();
						break;
					}
				}
			}
		}
		else {
System.out.println("\nNix GetQualifiedFileName null files "+filePath + " "+this.idGenomeVersion);
		}
		return filePath;

	}

	public String getDirectory(String data_root) {
		return data_root  + this.getFileName();
	}

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
		//props.put(PROP_GROUP_INSTITUTE, this.getIdUserGroup() != null ? dictionaryHelper.getUserGroupInstitute(this.getIdUserGroup()) : "");
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

}
