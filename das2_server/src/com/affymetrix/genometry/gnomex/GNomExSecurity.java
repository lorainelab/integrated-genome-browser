package com.affymetrix.genometry.gnomex;

import hci.gnomex.model.DataTrack;
import hci.gnomex.model.GenomeBuild;
import hci.gnomex.model.Organism;
import hci.gnomex.security.SecurityAdvisor;
import hci.gnomex.utility.DataTrackQuery;
import hci.gnomex.utility.PropertyDictionaryHelper;
import hci.gnomex.utility.QualifiedDataTrack;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Session;

import com.affymetrix.genometry.genopub.Constants;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometry.AnnotSecurity;
import java.util.logging.Level;


public class GNomExSecurity implements AnnotSecurity, Serializable {
	
	public static final String    SESSION_KEY = "GNomExSecurity";

	private  boolean              scrutinizeAccess = false;
	
	
	private SecurityAdvisor        secAdvisor;


	
	private final HashMap<String, HashMap<Integer, QualifiedDataTrack>> buildToAuthorizedDataTrackMap = new HashMap<String, HashMap<Integer, QualifiedDataTrack>>();
	private  Map<String, GenomeBuild> buildNameToVersionMap = new HashMap<String, GenomeBuild>();
	
	private String                  dataTrackInfoURL;
	
	private String                  analysis_root_dir;
	
	public void setDataTrackInfoURL(String serverName, String portNumber) {
	  if (portNumber == null) {
      portNumber = "";
    } else if (portNumber.length() != 0) {
      portNumber = ":" + portNumber;           
    }
	  String gnomexFlexApp = this.secAdvisor.isGuest() ? Constants.GNOMEX_GUEST_FLEX_APP : Constants.GNOMEX_FLEX_APP;
	  dataTrackInfoURL =  "https"+  "://"  + serverName + portNumber + "/gnomex/" + gnomexFlexApp;
	}
	
	@SuppressWarnings("unchecked")
	public GNomExSecurity(Session sess,  String serverName, SecurityAdvisor secAdvisor, boolean scrutinizeAccess) throws Exception {
    
    this.secAdvisor = secAdvisor;


    // Are the dataTracks loaded from the db?  If so, security
		// logic is driven from info in db, otherwise, access to all resources
		// is granted.
		this.scrutinizeAccess = scrutinizeAccess;
		
		
		if (this.scrutinizeAccess) {
			this.loadAuthorizedResources(sess);	
		}
		
		analysis_root_dir = PropertyDictionaryHelper.getInstance(sess).getAnalysisDirectory(serverName);
	}
	
	public Document getXML() {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("GenoPubSecurity");
		root.addAttribute("userName",        secAdvisor.getUID() != null ?  secAdvisor.getUID() : "");
		root.addAttribute("userDisplayName", secAdvisor.getAppUser() != null ? secAdvisor.getAppUser().getDisplayName() : "");
		root.addAttribute("name",            secAdvisor.getAppUser() != null ? secAdvisor.getAppUser().getDisplayName() : "");
		root.addAttribute("isAdmin",         secAdvisor.hasPermission(SecurityAdvisor.CAN_ADMINISTER_ALL_CORE_FACILITIES) ? "Y" : "N");
		root.addAttribute("isGuest",         secAdvisor.getIsGuest());
		root.addAttribute("canManageUsers",  secAdvisor.hasPermission(SecurityAdvisor.CAN_ADMINISTER_ALL_CORE_FACILITIES) || (secAdvisor.getAppUser() != null && secAdvisor.getGroupsIManage().size() > 0) ? "Y" : "N");
		
		
		return doc;		
	}
	

	

	
	public void loadAuthorizedResources(Session sess) throws Exception {
		if (!scrutinizeAccess) {
			return;
		}
		
		// Start over if we have already loaded the resources
		if (!buildToAuthorizedDataTrackMap.isEmpty()) {
			buildToAuthorizedDataTrackMap.clear();
		}
		
		// Cache the authorized data track ids of each genome build for this user
		DataTrackQuery dataTrackQuery = new DataTrackQuery();
		dataTrackQuery.runDataTrackQuery(sess, this.secAdvisor, false);
		
		// Cache the genome builds
		this.buildNameToVersionMap = dataTrackQuery.getGenomeBuildNameMap();
		
		// Cache the authorized dataTracks
		for (Organism organism : dataTrackQuery.getOrganisms()) {
			for (String genomeVersionName : dataTrackQuery.getGenomeBuildNames(organism)) {

				HashMap<Integer, QualifiedDataTrack> dataTrackMap = this.buildToAuthorizedDataTrackMap.get(genomeVersionName);
				if (dataTrackMap == null) {
					dataTrackMap = new HashMap<Integer, QualifiedDataTrack>();
					this.buildToAuthorizedDataTrackMap.put(genomeVersionName, dataTrackMap);
				}
				for (QualifiedDataTrack qa : dataTrackQuery.getQualifiedDataTracks(organism, genomeVersionName)) {
					dataTrackMap.put(qa.getDataTrack().getIdDataTrack(), qa);
				}
			}
		}

	}
	
	
	
	//
	//
	// AnnotSecurity methods
	//
	//

	public SecurityAdvisor getSecAdvisor() {
    return secAdvisor;
  }

  public boolean isAuthorized(String genomeVersionName, String dataTrackName, Object dataTrackId) {
		// When dataTrack is loaded directly from file system, all dataTracks 
		// are shown.
		if (!scrutinizeAccess) { 
			return true;
		}

		// If the dataTrack id is not provided, block access
		if (dataTrackId == null) {
			Logger.getLogger(GNomExSecurity.class.getName()).log(Level.WARNING, "Unable to find dataTrack id for {0}.  Blocking access.", dataTrackName);
		}

		// Get the hash map of dataTrack ids this user is authorized to view
		Map dataTrackMap = (Map)buildToAuthorizedDataTrackMap.get(genomeVersionName);

		// Returns true if dataTrack id is in hash map; otherwise, returns false
		if (dataTrackMap == null) {
		  return false;
		} else {
	    return dataTrackMap.containsKey(dataTrackId);		  
		}

	}
	
	public Map<String, Object> getProperties(String genomeVersionName, String dataTrackName, Object dataTrackId) {
		// When dataTrack is loaded directly from file system, dataTracks
		// don't have any additional properties
		if (!scrutinizeAccess) { 
			return null;
		}

		// If the dataTrack access is blocked, don't show
		// properties.
		if (!isAuthorized(genomeVersionName, dataTrackName, dataTrackId)) {
			return null;
		}

		// Get the hash map of dataTrack ids this user is authorized to view
		Map<Integer, QualifiedDataTrack> dataTrackMap = buildToAuthorizedDataTrackMap.get(genomeVersionName);
		QualifiedDataTrack qa = dataTrackMap.get(dataTrackId);
		Map<String, Object> props =  qa.getDataTrack().getProperties();
		
		// Add the 'info' URL as a property.  
		if (!props.containsKey(DataTrack.PROP_URL)) {
      String gnomexDataTrackURL = this.dataTrackInfoURL + "?dataTrackNumber=" + qa.getDataTrack().getNumber();
	    props.put(DataTrack.PROP_URL, gnomexDataTrackURL);
		}
		return props;
		
	}
	
	
	
	public boolean isBarGraphData(String data_root, String genomeVersionName, String dataTrackName, Object dataTrackId) {
		// When dataTrack is loaded directly from file system, just return true
		if (!scrutinizeAccess) { 
			return true;
		}

		// If the dataTrack access is blocked, return false
		if (!isAuthorized(genomeVersionName, dataTrackName, dataTrackId)) {
			return false;
		}

		// Get the hash map of dataTrack ids this user is authorized to view
		Map<Integer, QualifiedDataTrack> dataTrackMap = buildToAuthorizedDataTrackMap.get(genomeVersionName);
		QualifiedDataTrack qa = dataTrackMap.get(dataTrackId);
		try {
			return qa.getDataTrack().isBarGraphData(data_root, analysis_root_dir);
		} catch (Exception e) {
			return false;
		}
		
		
	}
	
	public boolean isUseqGraphData(String data_root, String genomeVersionName, String dataTrackName, Object dataTrackId) {
		// When dataTrack is loaded directly from file system, just return true
		if (!scrutinizeAccess) { 
			return true;
		}

		// If the dataTrack access is blocked, return false
		if (!isAuthorized(genomeVersionName, dataTrackName, dataTrackId)) {
			return false;
		}

		// Get the hash map of dataTrack ids this user is authorized to view
		Map<Integer, QualifiedDataTrack> dataTrackMap = buildToAuthorizedDataTrackMap.get(genomeVersionName);
		QualifiedDataTrack qa = dataTrackMap.get(dataTrackId);
		try {
			return qa.getDataTrack().isUseqGraphData(data_root, analysis_root_dir);
		} catch (Exception e) {
			return false;
		}
		
		
	}
	
	public boolean isBamData(String data_root, String genomeVersionName, String dataTrackName, Object dataTrackId) {
		// When dataTrack is loaded directly from file system, just return true
		if (!scrutinizeAccess) { 
			return true;
		}

		// If the dataTrack access is blocked, return false
		if (!isAuthorized(genomeVersionName, dataTrackName, dataTrackId)) {
			return false;
		}

		// Get the hash map of dataTrack ids this user is authorized to view
		Map<Integer, QualifiedDataTrack> dataTrackMap = buildToAuthorizedDataTrackMap.get(genomeVersionName);
		QualifiedDataTrack qa = dataTrackMap.get(dataTrackId);
		try {
			return qa.getDataTrack().isBamData(data_root, analysis_root_dir);
		} catch (Exception e) {
			return false;
		}
		
		
	}
	

	
	public String getSequenceDirectory(String data_root, AnnotatedSeqGroup genome) throws Exception {
		if (scrutinizeAccess) {
			GenomeBuild genomeVersion = buildNameToVersionMap.get(genome.getID());
			if (genomeVersion == null) {
				throw new Exception("Cannot find genome build " + genome.getID() + " in genome build map");
			}
			return data_root + com.affymetrix.genometry.genopub.Constants.SEQUENCE_DIR_PREFIX + genomeVersion.getIdGenomeBuild().toString() + "/";
		} else {
			return data_root + genome.getOrganism() + "/" + genome.getID() + "/dna/";
		}
	}

	public boolean isGuestRole() {
	  return this.secAdvisor.isGuest();
	}


}
 