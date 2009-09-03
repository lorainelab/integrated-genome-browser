package com.affymetrix.genometry.servlets.das2manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;


import org.hibernate.Session;

public class DictionaryHelper {
    
    private static DictionaryHelper              theDictionaryHelper   = null;
	
    private HashMap<Integer, AnalysisType>   analysisTypeMap  = new HashMap<Integer, AnalysisType>();
	private List<AnalysisType>               analysisTypeList = new ArrayList<AnalysisType>();
	
    private HashMap<Integer, ExperimentMethod>    experimentMethodMap  = new HashMap<Integer, ExperimentMethod>();
	private List<ExperimentMethod>                experimentMethodList = new ArrayList<ExperimentMethod>();

    private HashMap<Integer, ExperimentPlatform>  experimentPlatformMap  = new HashMap<Integer, ExperimentPlatform>();
	private List<ExperimentPlatform>              experimentPlatformList = new ArrayList<ExperimentPlatform>();

	private HashMap<Integer, Organism>            organismMap  = new HashMap<Integer, Organism>();
	private List<Organism>                        organismList = new ArrayList<Organism>();

	private HashMap<Integer, GenomeVersion>       genomeVersionMap  = new HashMap<Integer, GenomeVersion>();
	private List<GenomeVersion>                   genomeVersionList = new ArrayList<GenomeVersion>();
	private HashMap<Integer, List<GenomeVersion>> organismToGenomeVersionMap = new HashMap<Integer, List<GenomeVersion>>();

	private HashMap<Integer, SecurityGroup>       groupMap  = new HashMap<Integer, SecurityGroup>();
	private List<SecurityGroup>                   groupList = new ArrayList<SecurityGroup>();

	private HashMap<Integer, User>                userMap  = new HashMap<Integer, User>();
	private List<User>                            userList = new ArrayList<User>();

	private List<Visibility>                      visibilityList = new ArrayList<Visibility>();

	public static DictionaryHelper getInstance(Session sess) {
		if (theDictionaryHelper == null) {
			theDictionaryHelper = new DictionaryHelper();
			theDictionaryHelper.load(sess);
		}
		return theDictionaryHelper;
	}
	
	public static DictionaryHelper reload(Session sess) {
		theDictionaryHelper = new DictionaryHelper();
		theDictionaryHelper.load(sess);
		return theDictionaryHelper;
	}
	@SuppressWarnings("unchecked")
	private void load(Session sess) {
		List<AnalysisType> entries = (List<AnalysisType>) sess
		        .createQuery(
		                "SELECT d from AnalysisType d order by d.name")
		        .list();
		for (AnalysisType d : entries) {
			analysisTypeMap.put(d.getIdAnalysisType(), d);
			analysisTypeList.add(d);
		}
		
		List<ExperimentMethod> experimentMethods = (List<ExperimentMethod>) sess
		        .createQuery("SELECT d from ExperimentMethod d order by d.name")
		        .list();
		for (ExperimentMethod d : experimentMethods) {
			experimentMethodMap.put(d.getIdExperimentMethod(), d);
			experimentMethodList.add(d);
		}
		
		List<ExperimentPlatform> experimentPlatforms = (List<ExperimentPlatform>) sess
		        .createQuery("SELECT d from ExperimentPlatform d order by d.name")
		        .list();
		for (ExperimentPlatform d : experimentPlatforms) {
			experimentPlatformMap.put(d.getIdExperimentPlatform(), d);
			experimentPlatformList.add(d);
		}
		
		
		List<Visibility> visibilities = (List<Visibility>) sess
		        .createQuery("SELECT d from Visibility d order by d.name")
		        .list();
		for (Visibility d : visibilities) {
			visibilityList.add(d);
		}
		
		List<Organism> organisms = (List<Organism>) sess
        	.createQuery(
                "SELECT d from Organism d order by d.binomialName")
                .list();
		for (Organism d : organisms) {
			organismMap.put(d.getIdOrganism(), d);
			organismList.add(d);
		}
		
		List<GenomeVersion> genomeVersions = (List<GenomeVersion>) sess
        	.createQuery(
                "SELECT d from GenomeVersion d order by d.name")
                .list();
		for (GenomeVersion d : genomeVersions) {
			genomeVersionMap.put(d.getIdGenomeVersion(), d);
			genomeVersionList.add(d);
			
			List<GenomeVersion> versions = organismToGenomeVersionMap.get(d.getIdOrganism());
			if (versions == null) {
				versions = new ArrayList<GenomeVersion>();
				organismToGenomeVersionMap.put(d.getIdOrganism(), versions);
			}
			versions.add(d);
		}
				
		List<SecurityGroup> groups = (List<SecurityGroup>) sess
        	.createQuery(
                "SELECT d from SecurityGroup d order by d.name")
                .list();
		for (SecurityGroup d : groups) {
			groupMap.put(d.getIdSecurityGroup(), d);
			groupList.add(d);
		}
		
		List<User> users = (List<User>) sess
    	.createQuery(
            "SELECT d from User d order by d.lastName, d.firstName, d.middleName")
            .list();
		for (User d : users) {
			userMap.put(d.getIdUser(), d);
			userList.add(d);
		}		
		

	}

	@SuppressWarnings("unchecked")
	public Document getXML(Das2ManagerSecurity das2Security) {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Dictionaries");

		//
		// AnalysisType
		//
		// Create two hierarchies - one for dropdowns 
		// and another used for dictionary editing.
		//
		Element dictEdit = root.addElement("Dictionary");
		dictEdit.addAttribute("dictionaryName", "AnalysisType");
		dictEdit.addAttribute("dictionaryDisplayName", "Analysis Type");
		dictEdit .addAttribute("label", "Analysis Types");
		
		Element dict = root.addElement("AnalysisTypes");
		this.makeBlankNode(dict, "AnalysisType");
		
		for (AnalysisType d : analysisTypeList) {
			Element dictEntry = dictEdit.addElement("DictionaryEntry");
			dictEntry.addAttribute("dictionaryName", "AnalysisType");
			dictEntry.addAttribute("dictionaryDisplayName", "Analysis Type");
			dictEntry.addAttribute("id",       d.getIdAnalysisType().toString());
			dictEntry.addAttribute("name",     d.getName());
			dictEntry.addAttribute("label",    d.getName());
			dictEntry.addAttribute("isActive", d.getIsActive());
			dictEntry.addAttribute("type",     "DictionaryEntry");
			dictEntry.addAttribute("canWrite", das2Security.canWrite(d) ? "Y" : "N");
			dictEntry.addAttribute("idUser",   d.getIdUser() != null ? d.getIdUser().toString() : "");
			dictEntry.addAttribute("owner",    this.getUserFullName(d.getIdUser()));
			
			Element de = (Element)dictEntry.clone();
			de.setName("AnalysisType");
			dict.add(de);
		}
		

		//
		// ExperimentMethods
		//
		// Create two hierarchies - one for dropdowns 
		// and another used for dictionary editing.
		//
		dictEdit = root.addElement("Dictionary");
		dictEdit.addAttribute("dictionaryName", "ExperimentMethod");
		dictEdit.addAttribute("dictionaryDisplayName", "Experiment Method");
		dictEdit.addAttribute("label", "Experiment Methods");

		dict = root.addElement("ExperimentMethods");
		this.makeBlankNode(dict, "ExperimentMethod");
		
		for (ExperimentMethod d : experimentMethodList) {
			Element dictEntry = dictEdit.addElement("DictionaryEntry");
			dictEntry.addAttribute("dictionaryName", "ExperimentMethod");
			dictEntry.addAttribute("dictionaryDisplayName", "Experiment Method");
			dictEntry.addAttribute("id",       d.getIdExperimentMethod().toString());
			dictEntry.addAttribute("name",     d.getName());
			dictEntry.addAttribute("label",    d.getName());
			dictEntry.addAttribute("isActive", d.getIsActive());
			dictEntry.addAttribute("type",     "DictionaryEntry");
			dictEntry.addAttribute("canWrite", das2Security.canWrite(d) ? "Y" : "N");
			dictEntry.addAttribute("idUser",   d.getIdUser() != null ? d.getIdUser().toString() : "");
			dictEntry.addAttribute("owner",    this.getUserFullName(d.getIdUser()));
			
			Element de = (Element)dictEntry.clone();
			de.setName("ExperimentMethod");
			dict.add(de);
		}
		

		//
		// ExperimentPlatforms
		//
		// Create two hierarchies - one for dropdowns 
		// and another used for dictionary editing.
		//

		dictEdit = root.addElement("Dictionary");
		dictEdit.addAttribute("dictionaryName", "ExperimentPlatform");
		dictEdit.addAttribute("dictionaryDisplayName", "Experiment Platform");
		dictEdit.addAttribute("label", "Experiment Platforms");

		dict = root.addElement("ExperimentPlatforms");
		this.makeBlankNode(dict, "ExperimentPlatform");
		
		for (ExperimentPlatform d : experimentPlatformList) {
			Element dictEntry = dictEdit.addElement("DictionaryEntry");
			dictEntry.addAttribute("dictionaryName", "ExperimentPlatform");
			dictEntry.addAttribute("dictionaryDisplayName", "Experiment Platform");
			dictEntry.addAttribute("id",       d.getIdExperimentPlatform().toString());
			dictEntry.addAttribute("name",     d.getName());
			dictEntry.addAttribute("label",    d.getName());
			dictEntry.addAttribute("isActive", d.getIsActive());
			dictEntry.addAttribute("type",     "DictionaryEntry");
			dictEntry.addAttribute("canWrite", das2Security.canWrite(d) ? "Y" : "N");
			dictEntry.addAttribute("idUser",   d.getIdUser() != null ? d.getIdUser().toString() : "");
			dictEntry.addAttribute("owner",    this.getUserFullName(d.getIdUser()));
			
			Element de = (Element)dictEntry.clone();
			de.setName("ExperimentPlatform");
			dict.add(de);
		}
		
		
		//
		// Visibility
		//
		dict = root.addElement("Visibilities");
		makeBlankNode(dict, "Visibility");
		for (Visibility d : visibilityList) {
			Element dictEntry = dict.addElement("Visibility");
			dictEntry.addAttribute("id",       d.getCodeVisibility());
			dictEntry.addAttribute("name",     d.getName());
		}

		//
		// Genome versions
		//
		dict = root.addElement("GenomeVersions");
		makeBlankNode(dict, "GenomeVersion");
		for (GenomeVersion d : genomeVersionList) {
			Element dictEntry = dict.addElement("GenomeVersion");
			dictEntry.addAttribute("id",         d.getIdGenomeVersion().toString());
			dictEntry.addAttribute("name",       d.getName());
			dictEntry.addAttribute("idOrganism", d.getIdOrganism().toString());
		}

		dict = root.addElement("Organisms");
		makeBlankNode(dict, "Organism");
		for (Organism d : organismList) {
			Element dictEntry = dict.addElement("Organism");
			dictEntry.addAttribute("id",   d.getIdOrganism().toString());
			dictEntry.addAttribute("name", d.getName());
			
			makeBlankNode(dictEntry, "GenomeVersion");
			if (this.getGenomeVersions(d.getIdOrganism()) != null) {
				for (GenomeVersion gv : this.getGenomeVersions(d.getIdOrganism())) {
					Element de = dictEntry.addElement("GenomeVersion");
					de.addAttribute("id",         gv.getIdGenomeVersion().toString());
					de.addAttribute("name",       gv.getName());
					de.addAttribute("idOrganism", gv.getIdOrganism().toString());
				}
				
			}
			
		}
		
		
		//
		// Security groups
		//
		dict = root.addElement("SecurityGroups");
		Element blank = makeBlankNode(dict, "SecurityGroup");
		blank.addAttribute("isPartOf", "Y");
		for (SecurityGroup d : groupList) {
			Element dictEntry = dict.addElement("SecurityGroup");
			dictEntry.addAttribute("id",         d.getIdSecurityGroup().toString());
			dictEntry.addAttribute("name",       d.getName());
			
			dictEntry.addAttribute("isPartOf",         das2Security.isAdminRole() || das2Security.belongsToGroup(d) ? "Y" : "N");
			dictEntry.addAttribute("isMemberOf",       das2Security.isAdminRole() || das2Security.isMember(d) ? "Y" : "N");
			dictEntry.addAttribute("isManagerOf",      das2Security.isAdminRole() || das2Security.isManager(d) ? "Y" : "N");
			dictEntry.addAttribute("isCollaboratorOf", das2Security.isAdminRole() || das2Security.isCollaborator(d) ? "Y" : "N");

			Element membersNode = dictEntry.addElement("Members");
			makeBlankNode(membersNode, "User");
			for (User member : (Set<User>)d.getMembers()) {
				Element memberNode = membersNode.addElement("User");
				memberNode.addAttribute("id",         member.getIdUser().toString());
				memberNode.addAttribute("name",       member.getName());
			}

			Element collaboratorsNode = dictEntry.addElement("Collaborators");
			makeBlankNode(collaboratorsNode, "User");
			for (User member : (Set<User>)d.getCollaborators()) {
				Element memberNode = collaboratorsNode.addElement("User");
				memberNode.addAttribute("id",         member.getIdUser().toString());
				memberNode.addAttribute("name",       member.getName());
			}

			Element managersNode = dictEntry.addElement("Managers");
			makeBlankNode(managersNode, "User");
			for (User member : (Set<User>)d.getManagers()) {
				Element memberNode = managersNode.addElement("User");
				memberNode.addAttribute("id",         member.getIdUser().toString());
				memberNode.addAttribute("name",       member.getName());
			}

		}

		// 
		// Users
		//
		dict = root.addElement("Users");
		makeBlankNode(dict, "User");
		for (User d : userList) {
			Element dictEntry = dict.addElement("User");
			dictEntry.addAttribute("id",         d.getIdUser().toString());
			dictEntry.addAttribute("name",       d.getName());
		}
		
		return doc;
	}
	
	private Element makeBlankNode(Element parentNode, String name) {
		Element node = parentNode.addElement(name);
		node.addAttribute("id",   "");
		node.addAttribute("name", "");
		return node;
	}

	public String getAnalysisType(Integer id) {
		if (id == null) {
			return "";
		}
		AnalysisType d = analysisTypeMap.get(id);
		if (d != null) {
			return d.getName();
		} else {
			return "";
		}
	}
	
	public String getExperimentMethod(Integer id) {
		if (id == null) {
			return "";
		}
		ExperimentMethod d = experimentMethodMap.get(id);
		if (d != null) {
			return d.getName();
		} else {
			return "";
		}
	}
	
	public String getExperimentPlatform(Integer id) {
		if (id == null) {
			return "";
		}
		ExperimentPlatform d = experimentPlatformMap.get(id);
		if (d != null) {
			return d.getName();
		} else {
			return "";
		}
	}
	
	public List<Organism> getOrganisms() {
		return this.organismList;
	}
	
	public List<GenomeVersion> getGenomeVersions(Integer idOrganism) {
		return this.organismToGenomeVersionMap.get(idOrganism);
	}
	
	public String getUserFullName(Integer idUser) {
		User user = userMap.get(idUser);
		if (user != null) {
			return user.getName();
		} else {
			return "";
		}
	}
	
	public String getOrganismName(Integer idOrganism) {
		Organism organism = organismMap.get(idOrganism);
		if (organism != null) {
			return organism.getName();
		} else {
			return "";
		}
	}
	public String getOrganismName(GenomeVersion genomeVersion) {
		if (genomeVersion != null && genomeVersion.getIdOrganism() != null) {
			Organism organism = organismMap.get(genomeVersion.getIdOrganism());
			if (organism != null) {
				return organism.getName();
			} else {
				return "";
			}			
		} else {
			return "";
		}
	}	
	public String getGenomeVersionName(Integer idGenomeVersion) {
		GenomeVersion genomeVersion = genomeVersionMap.get(idGenomeVersion);
		if (genomeVersion != null) {
			return genomeVersion.getName();
		} else {
			return "";
		}
	}
	public GenomeVersion getGenomeVersion(Integer idGenomeVersion) {
		GenomeVersion genomeVersion = genomeVersionMap.get(idGenomeVersion);
		return genomeVersion;
	}	
	
	public String getSecurityGroupName(Integer idSecurityGroup) {
		SecurityGroup group = groupMap.get(idSecurityGroup);
		if (group != null) {
			return group.getName();
		} else {
			return "";
		}
	}

}
