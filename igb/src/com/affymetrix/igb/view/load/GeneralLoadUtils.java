package com.affymetrix.igb.view.load;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.MutableSeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.MutableDoubleSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.util.LoadUtils.LoadStatus;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.LoadUtils.ServerType;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.das.DasFeatureLoader;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das.DasSource;
import com.affymetrix.igb.das2.Das2FeatureRequestSym;
import com.affymetrix.igb.das2.Das2Region;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.das2.Das2Source;
import com.affymetrix.igb.das2.Das2Type;
import com.affymetrix.igb.das2.Das2VersionedSource;
import com.affymetrix.igb.general.FeatureLoading;
import com.affymetrix.igb.general.ResidueLoading;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.igb.view.SeqMapView;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @version $Id$
 */
public final class GeneralLoadUtils {
	private static final boolean DEBUG = false;
	private static final boolean DEBUG_VIRTUAL_GENOME = false;
	/**
	 *  using negative start coord for virtual genome chrom because (at least for human genome)
	 *     whole genome start/end/length can't be represented with positive 4-byte ints (limit is +/- 2.1 billion)
	 */
//    final double default_genome_min = -2100200300;
	private static final double default_genome_min = 0;

	private static final SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	private final SeqMapView gviewer;

	//public static final String PREF_QUICKLOAD_CACHE_RESIDUES = "quickload_cache_residues";
	//public static final String PREF_QUICKLOAD_CACHE_ANNOTS = "quickload_cache_annots";
	//final static String ENCODE_FILE_NAME = "encodeRegions.bed";
	//final static String ENCODE_FILE_NAME2 = "encode.bed";
	//static boolean CACHE_RESIDUES_DEFAULT = false;
	//static boolean CACHE_ANNOTS_DEFAULT = true;

	private final Map<String, Boolean> version2init;

	// List of servers.
	private final List<GenericServer> discoveredServers;

	// versions associated with a given genome.
	final Map<String, List<GenericVersion>> species2genericVersionList;	// the list of versions associated with the species
	final Map<String, String> versionName2species;	// the species associated with the given version.
	final Map<String, Set<GenericVersion>> versionName2versionSet;
	// the list of GenericVersion objects associated with the version name.  This is to avoid synonym stuff.

	/**
	 * Private copy of the default Synonym lookup
	 * @see SynonymLookup#getDefaultLookup()
	 */
	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	
	/** Private synonym lookup for correlating versions to species. */
	private static final SynonymLookup SPECIES_LOOKUP = new SynonymLookup();
	
	/**
	 * Location of synonym file for correlating versions to species.
	 * The file lookup is done using {@link Class#getResourceAsStream(String)}.
	 * The default file is {@value}.
	 * 
	 * @see #SPECIES_LOOKUP
	 */
	private static final String SPECIES_SYNONYM_FILE = "/species.txt";
	private static final String CHROM_SYNONYM_FILE = "/chromosomes.txt";
	/*
	 * This is done in a static context vs the constructor to ensure that
	 * static functions have access to the synonyms
	 */
	static {
		try {
			SPECIES_LOOKUP.loadSynonyms(GeneralLoadUtils.class.getResourceAsStream(SPECIES_SYNONYM_FILE));

			// Eventually this will need its own synonyms object.
			SPECIES_LOOKUP.loadSynonyms(GeneralLoadUtils.class.getResourceAsStream(CHROM_SYNONYM_FILE));
		} catch (IOException e) { 
			e.printStackTrace();
		}
	}

	//public boolean allow_reinitialization = true;
	/*public void clear() {
		version2init.clear();
		discoveredServers.clear();
		species2genericVersionList.clear();
		versionName2species.clear();
		versionName2versionSet.clear();
	}*/

	public GeneralLoadUtils() {
		this.gviewer = Application.getSingleton().getMapView();

		version2init = new HashMap<String, Boolean>();
		discoveredServers = new ArrayList<GenericServer>();
		species2genericVersionList = new LinkedHashMap<String, List<GenericVersion>>();
		versionName2species = new HashMap<String, String>();
		versionName2versionSet = new HashMap<String, Set<GenericVersion>>();
	}

	private static GenericServer serverExists(List <GenericServer> servers, String serverName, ServerType serverType) {
		for (GenericServer gServer : servers) {
			if (gServer.serverName.equals(serverName) && gServer.serverType == serverType) {
				return gServer;
			}
		}
		return null;
	}

	/**
	 * Add specified server, finding species and versions associated with it.
	 * @param serverName
	 * @param serverURL
	 * @param serverType
	 * @return success of server add.
	 */
	boolean addServer(String serverName, String serverURL, ServerType serverType) {
		GenericServer gServer = serverExists(discoveredServers, serverName, serverType);
		if (gServer != null) {
			System.out.println("Server " + gServer.toString() +" already exists at " + gServer.URL);
			return false;
		}
		
		try {
		if (serverType == ServerType.QuickLoad) {
			gServer = ServerList.addServer(serverType, serverName, serverURL);
			if (gServer == null) {
				return false;
			}
			if (!getQuickLoadSpeciesAndVersions(gServer)) {
				return false;
			}
			discoveredServers.add(gServer);
		} else if (serverType == ServerType.DAS) {
			/*DasServerInfo server = DasDiscovery.addDasServer(serverName, serverURL);
			if (server == null) {
				return false;
			}
			gServer = new GenericServer(serverName, server.getRootUrl(), serverType, server);*/
			gServer = ServerList.addServer(serverType, serverName, serverURL);
			if (gServer == null) {
				return false;
			}
			getDAS1SpeciesAndVersions(gServer);
			discoveredServers.add(gServer);

		} else if (serverType == ServerType.DAS2) {
			//Das2ServerInfo server = Das2Discovery.addDas2Server(serverName, serverURL);
			gServer = ServerList.addServer(serverType, serverName, serverURL);
			if (gServer == null) {
				return false;
			}
			//gServer = new GenericServer(serverName, server.getURI().toString(), serverType, server);
			getDAS2Species(gServer);
			getDAS2Versions(gServer);
			discoveredServers.add(gServer);
		}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Discover all of the servers and genomes and versions.
	 */
	void discoverServersAndSpeciesAndVersions() {
		// it's assumed that if we're here, we need to refresh this information.
		discoveredServers.clear();
		discoverServersInternal(discoveredServers);
		discoverSpeciesAndVersionsInternal();
	}

	/**
	 * Discover the list of servers.
	 */
	public static synchronized void discoverServersInternal(final List<GenericServer> discoveredServers) {
		/*for (Map.Entry<String, Das2ServerInfo> entry : Das2Discovery.getDas2Servers().entrySet()) {
			Das2ServerInfo server = entry.getValue();
			String serverName = entry.getKey();
			if (server != null && serverName != null) {
				if (serverExists(discoveredServers, serverName, ServerType.DAS2) == null) {
					GenericServer g = new GenericServer(serverName, server.getURI().toString(), ServerType.DAS2, server);
					discoveredServers.add(g);
				}
			}
		}*/
		/*for (GenericServer gServer : ServerList.getEnabledServers()) {
			System.out.println("Discovering server... " + gServer);
		}*/

		for (GenericServer gServer : ServerList.getEnabledServers()) {
			if (gServer.serverType == ServerType.DAS2) {
				if (serverExists(discoveredServers, gServer.serverName, gServer.serverType) == null) {
					discoveredServers.add(gServer);
				}
			}
		}

		// Discover DAS servers
		// TODO -- strip out descriptions and make synonyms with DAS/2
		// TODO -- get chromosome info
		for (GenericServer gServer : ServerList.getEnabledServers()) {
			if (gServer.serverType == ServerType.DAS) {
				if (serverExists(discoveredServers, gServer.serverName, gServer.serverType) == null) {
					discoveredServers.add(gServer);
				}
			}
		}
		/*for (Map.Entry<String, DasServerInfo> entry : DasDiscovery.getDasServers().entrySet()) {
			DasServerInfo server = entry.getValue();
			String serverName = entry.getKey();
			if (server != null && serverName != null) {
				if (serverExists(discoveredServers, serverName, ServerType.DAS) == null) {
					GenericServer g = new GenericServer(serverName, server.getRootUrl(), ServerType.DAS, server);
					discoveredServers.add(g);
				}
			}
		}*/

		// Discover Quickload servers
		// This is based on new preferences, which allow arbitrarily many quickload servers.
		for (GenericServer gServer : ServerList.getEnabledServers()) {
			if (gServer.serverType == ServerType.QuickLoad) {
				if (serverExists(discoveredServers, gServer.serverName, gServer.serverType) == null) {
					discoveredServers.add(gServer);
				}
			}
		}
	}

	/**
	 * Discover the species and genome versions.
	 */
	private synchronized void discoverSpeciesAndVersionsInternal() {
		for (GenericServer gServer : discoveredServers) {
			if (gServer.serverType == ServerType.DAS2) {
				getDAS2Species(gServer);
				getDAS2Versions(gServer);
				continue;
			}
			if (gServer.serverType == ServerType.DAS) {
				getDAS1SpeciesAndVersions(gServer);
				continue;
			}
			if (gServer.serverType == ServerType.QuickLoad) {
				getQuickLoadSpeciesAndVersions(gServer);
				continue;
			}
			if (gServer.serverType == ServerType.Unknown) {
				System.out.println("WARNING: Discovered server class " + gServer.serverType);
				continue;
			}
			System.out.println("WARNING: Unknown server class " + gServer.serverType);
		}
	}

	/**
	 * Discover species from DAS
	 * @param gServer
	 * @return false if there's an obvious problem
	 */
	private synchronized boolean getDAS1SpeciesAndVersions(GenericServer gServer) {
		DasServerInfo server = (DasServerInfo) gServer.serverObj;
		if (server.getDataSources() == null || server.getDataSources().values() == null || server.getDataSources().values().isEmpty()) {
			System.out.println("WARNING: Couldn't find species for server: " + gServer.serverName);
			return false;
		}
		for (DasSource source : server.getDataSources().values()) {
			if (DEBUG) {
				System.out.println("source, version:" + source.getName() + "..." + source.getVersion() + "..." + source.getDescription() + "..." + source.getInfoUrl() + "..." + source.getID());
			}
			/* TODO: speciesName needs its own SynonymLookup or equivalent
			 * using normalizeVersion allows us to use previously know names
			 */
			/* String speciesName = source.getDescription(); */
			String speciesName = SPECIES_LOOKUP.getPreferredName(source.getID());
			/* TODO: GenericVersion should be able to store source's name and ID */
			/* String versionName = source.getName(); */
			String versionName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), source.getID());
			String versionID = source.getID();
			List<GenericVersion> gVersionList = getSpeciesVersionList(speciesName);
			GenericVersion gVersion = new GenericVersion(versionID, versionName, gServer, source);
			discoverVersion(versionName, gServer, gVersion, gVersionList, speciesName);
		}
		return true;
	}


	/**
	 * Discover genomes from DAS/2
	 * @param gServer
	 * @return false if there's an obvious problem
	 */
	private synchronized boolean getDAS2Species(GenericServer gServer) {
		Das2ServerInfo server = (Das2ServerInfo) gServer.serverObj;
		if (server.getSources() == null || server.getSources().values() == null || server.getSources().values().isEmpty()) {
			System.out.println("WARNING: Couldn't find species for server: " + gServer.serverName);
			return false;
		}
		for (Das2Source source : server.getSources().values()) {
			String speciesName = SPECIES_LOOKUP.getPreferredName(source.getName());
			List<GenericVersion> gVersionList;
			if (!this.species2genericVersionList.containsKey(speciesName)) {
				gVersionList = new ArrayList<GenericVersion>();
				this.species2genericVersionList.put(speciesName, gVersionList);
			}
		}
		return true;
	}

	/**
	 * Discover genomes from DAS/2
	 * @param gServer
	 */
	private synchronized void getDAS2Versions(GenericServer gServer) {
		Das2ServerInfo server = (Das2ServerInfo) gServer.serverObj;
		for (Das2Source source : server.getSources().values()) {
			String speciesName = SPECIES_LOOKUP.getPreferredName(source.getName());
			List<GenericVersion> gVersionList = this.species2genericVersionList.get(speciesName);

			// Das/2 has versioned sources.  Get each version.
			for (Das2VersionedSource versionSource : source.getVersions().values()) {
				String versionName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), versionSource.getName());
				String versionID = versionSource.getName();
				GenericVersion gVersion = new GenericVersion(versionID, versionName, gServer, versionSource);
				discoverVersion(versionName, gServer, gVersion, gVersionList, speciesName);
			}
		}
	}

	/**
	 * Discover genomes from Quickload
	 * @param gServer
	 * @return false if there's an obvious failure.
	 */
	private synchronized boolean getQuickLoadSpeciesAndVersions(GenericServer gServer) {
		URL quickloadURL = null;
		try {
			quickloadURL = new URL((String) gServer.serverObj);
		} catch (MalformedURLException ex) {
			Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(gmodel, quickloadURL);
		if (quickloadServer == null) {
			System.out.println("ERROR: No quickload server model found for server: " + gServer.serverName);
			return false;
		}
		List<String> genomeList = quickloadServer.getGenomeNames();
		if (genomeList == null || genomeList.isEmpty()) {
			System.out.println("WARNING: No species found in server: " + gServer.serverName);
			return false;
		}

		for (String genomeID : genomeList) {
			String genomeName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), genomeID);
			// Retrieve group identity, since this has already been added in QuickLoadServerModel.

			AnnotatedSeqGroup group = gmodel.addSeqGroup(genomeName);
			List<GenericVersion> gVersions = group.getVersions();
			if (!gVersions.isEmpty()) {
				// We've found a corresponding version object that was initialized earlier.
				String versionName = gVersions.get(0).versionName;
				String speciesName = this.versionName2species.get(versionName);
				GenericVersion quickLoadVersion = new GenericVersion(genomeID, versionName, gServer, quickloadServer);
				List<GenericVersion> gVersionList = this.species2genericVersionList.get(speciesName);
				discoverVersion(versionName, gServer, quickLoadVersion, gVersionList, speciesName);
				continue;
			}
			String species = SPECIES_LOOKUP.getPreferredName(genomeName);

			// Unknown genome.  We'll add the name as if it's a species and a version.
			if (DEBUG) {
				System.out.println("Unknown quickload genome:" + genomeName);
			}

			List<GenericVersion> gVersionList = this.getSpeciesVersionList(species);

			GenericVersion gVersion = new GenericVersion(genomeID, genomeName, gServer, quickloadServer);
			discoverVersion(gVersion.versionName, gServer, gVersion, gVersionList, species);
		}
		return true;
	}

	/**
	 * An AnnotatedSeqGroup was added independently of the GeneralLoadUtils.
	 * Update GeneralLoadUtils state.
	 * @param aseq
	 * @return
	 */
	GenericVersion getUnknownVersion(AnnotatedSeqGroup aseq) {
		String versionName = aseq.getID();
		String speciesName = "-- Unknown -- " + versionName;	// make it distinct, but also make it appear at the top of the species list.

		List<GenericVersion> gVersionList = this.getSpeciesVersionList(speciesName);

		GenericServer gServer = new  GenericServer(null, null, ServerType.Unknown, null);
		GenericVersion gVersion = new GenericVersion(versionName, versionName, gServer, null);

		discoverVersion(versionName, gServer, gVersion, gVersionList, speciesName);

		return gVersion;
	}

	/**
	 *
	 * @param versionName not null or empty.
	 * @param gServer only used by debug statement.
	 * @param gVersion not null.
	 * @param gVersionList not null.
	 * @param speciesName not null or empty.
	 */
	private void discoverVersion(String versionName, GenericServer gServer, GenericVersion gVersion, List<GenericVersion> gVersionList, String speciesName) {
		if (!gVersionList.contains(gVersion)) {
			gVersionList.add(gVersion);
		}
		versionName2species.put(versionName, speciesName);
		Set<GenericVersion> versionSet;
		if (this.versionName2versionSet.containsKey(versionName)) {
			versionSet = this.versionName2versionSet.get(versionName);
		} else {
			versionSet = new HashSet<GenericVersion>();
			this.versionName2versionSet.put(versionName, versionSet);
		}
		versionSet.add(gVersion);
		AnnotatedSeqGroup group = gmodel.addSeqGroup(versionName); // returns existing group if found, otherwise creates a new group
		group.addVersion(gVersion);
		if (DEBUG) {
			System.out.println("Added " + gServer.serverType + "genome: " + speciesName + " version: " + versionName);
		}
	}


	/**
	 * Get list of versions for given species.  Create it if it doesn't exist.
	 * @param speciesName
	 * @return
	 */
	private List<GenericVersion> getSpeciesVersionList(String speciesName) {
		List<GenericVersion> gVersionList;
		if (!this.species2genericVersionList.containsKey(speciesName)) {
			gVersionList = new ArrayList<GenericVersion>();
			this.species2genericVersionList.put(speciesName, gVersionList);
		} else {
			gVersionList = this.species2genericVersionList.get(speciesName);
		}
		return gVersionList;
	}

	/** Returns the name that this server uses to refer to the given AnnotatedSeqGroup.
	 *  Because of synonyms, different servers may use different names to
	 *  refer to the same genome.
	 */
	/*public String getGenomeName(AnnotatedSeqGroup group) {
	return group2version.get(group);
	}*/


	/**
	 *  Returns the list of features for the genome with the given version name.
	 *  The list may (rarely) be empty, but never null.
	 */
	List<GenericFeature> getFeatures(final String versionName) {
		// There may be more than one server with the same versionName.  Merge all the version names.
		List<GenericFeature> featureList = new ArrayList<GenericFeature>();
		for (GenericVersion gVersion : this.versionName2versionSet.get(versionName)) {
			featureList.addAll(gVersion.features);
		}
		return featureList;
	}

	/**
	 * Returns the list of servers associated with the given versions.
	 * @param features -- assumed to be non-null.
	 * @return
	 */
	public static List<GenericServer> getServersWithAssociatedFeatures(List<GenericFeature> features) {
		List<GenericServer> serverList = new ArrayList<GenericServer>();
		for (GenericFeature gFeature : features) {
			if (!serverList.contains(gFeature.gVersion.gServer)) {
				serverList.add(gFeature.gVersion.gServer);
			}
		}
		return serverList;
	}

	
	/**
	 * Make sure this genome version has been initialized.
	 * @param versionName
	 */
	void initVersion(final String versionName) {
		if (versionName == null) {
			return;
		}
		Boolean init = version2init.get(versionName);
		if (init == null || !init.booleanValue()) {
			if (DEBUG) {
				System.out.println("initializing feature names for version: " + versionName);
			}
			FeatureLoading.loadFeatureNames(this.versionName2versionSet.get(versionName));
		}
	}

	/**
	 * Make sure this genome version has been initialized.
	 * @param versionName
	 */
	void initSeq(final String versionName) {
		if (versionName == null) {
			return;
		}
		Boolean init = version2init.get(versionName);
		if (init == null || !init.booleanValue()) {
			boolean seq_init = loadSeqInfo(versionName);

			if (seq_init) {
				version2init.put(versionName, Boolean.TRUE);
			}
		}
	}
	

	/**
	 * Load the sequence info for the given genome version.
	 * If there's more than one server with this genome version, just pick the first one.
	 * @param versionName
	 * @return
	 */
	private boolean loadSeqInfo(final String versionName) {
		if (DEBUG) {
			System.out.println("loading seqinfo : Version " + versionName);
		}
		Set<GenericVersion> gVersionSet = this.versionName2versionSet.get(versionName);
		List<GenericVersion> gVersions = new ArrayList<GenericVersion>(gVersionSet);
		
		AnnotatedSeqGroup group = loadChromInfo(gVersions);
		if (group == null) {
			return false;
		}
		if (DEBUG) {
			System.out.println("Group has :" + group.getSeqCount() + " chromosomes");
		}

		addGenomeVirtualSeq(group, default_genome_min, DEBUG_VIRTUAL_GENOME);

		for (GenericVersion gVersion : gVersions) {
			// Initialize all the servers with unloaded status of the feature/chromosome combinations.
			for (GenericFeature gFeature : gVersion.features) {
				for (BioSeq sabq : group.getSeqList()) {
					// Add chromosome sequences to feature
					if (!gFeature.LoadStatusMap.containsKey(sabq)) {
						gFeature.LoadStatusMap.put(sabq, LoadStatus.UNLOADED);
					}
				}
			}
		}

		return true;
	}


	/**
	 * Load the sequence info for the given genome version.
	 * Try all versions listed until one succeeds.
	 * @param gVersion
	 * @return
	 */
	private static AnnotatedSeqGroup loadChromInfo(List<GenericVersion> gVersions) {
		for (GenericVersion gVersion : gVersions) {
			if (DEBUG) {
				System.out.println("loading list of chromosomes for genome version: " + gVersion.versionName + " from server " + gVersion.gServer);
			}
			AnnotatedSeqGroup group = loadChromInfo(gVersion);
			if (group != null) {
				return group;
			}
		}
		return null;
	}

	/**
	 * Load the sequence info for the given genome version.
	 */
	private static AnnotatedSeqGroup loadChromInfo(GenericVersion gVersion) {
		AnnotatedSeqGroup group = null;

		// discover genomes from server
		if (gVersion.gServer == null) {
			return null;
		}
		if (DEBUG) {
			System.out.println("Discovering " + gVersion.gServer.serverType + " chromosomes");
		}
		if (gVersion.gServer.serverType == ServerType.DAS2) {

			// Discover chromosomes from DAS/2
			Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;

			group = version.getGenome();  // adds genome to singleton genometry model if not already present
			// Calling version.getSegments() to ensure that Das2VersionedSource is populated with Das2Region segments,
			//    which in turn ensures that AnnotatedSeqGroup is populated with SmartAnnotBioSeqs
			group.setSource(gVersion.gServer.serverName);
			version.getSegments();
			return group;
		}
		if (gVersion.gServer.serverType == ServerType.DAS) {
			// Discover chromosomes from DAS
			DasSource version = (DasSource) gVersion.versionSourceObj;

			group = version.getGenome();
			group.setSource(gVersion.gServer.serverName);
			version.getEntryPoints();

			return group;
		}
		if (gVersion.gServer.serverType == ServerType.QuickLoad) {
			// Discover chromosomes from QuickLoad
			group = gmodel.addSeqGroup(gVersion.versionName);
			group.setSource(gVersion.gServer.serverName);
			return group;
		}
		if (gVersion.gServer.serverType == ServerType.Unknown) {
				group = gmodel.addSeqGroup(gVersion.versionName);
				return group;
			}

		System.out.println("WARNING: Unknown server class " + gVersion.gServer.serverType);

		return null;
	}

	private static void addGenomeVirtualSeq(AnnotatedSeqGroup group, double default_genome_min, boolean DEBUG_VIRTUAL_GENOME) {
		int chrom_count = group.getSeqCount();
		if (chrom_count <= 1) {
			// no need to make a virtual "genome" chrom if there is only a single chromosome
			return;
		}

		if (DEBUG) {
			System.out.println("$$$$$ adding virtual genome seq to seq group");
		}
		if (group.getSeq(IGBConstants.GENOME_SEQ_ID) != null) {
			return; // return if we've already created the virtual genome
		}

		if (!isVirtualGenomeSmallEnough(group, chrom_count)) {
			return;
		}

		BioSeq genome_seq = group.addSeq(IGBConstants.GENOME_SEQ_ID, 0);
		for (int i = 0; i < chrom_count; i++) {
			MutableAnnotatedBioSeq chrom_seq = group.getSeq(i);
			if (chrom_seq == genome_seq) {
				continue;
			}
			addSeqToVirtualGenome(genome_seq, chrom_seq, default_genome_min, DEBUG_VIRTUAL_GENOME);
		}
	}

	/**
	 * Make sure virtual genome doesn't overflow int bounds.
	 * @param group
	 * @return
	 */
	private static boolean isVirtualGenomeSmallEnough(AnnotatedSeqGroup group, int chrom_count) {
		double seq_bounds = 0.0;

		for (int i = 0; i < chrom_count; i++) {
			MutableAnnotatedBioSeq chrom_seq = group.getSeq(i);
			int clength = chrom_seq.getLength();
			int spacer = (clength > 5000000) ? 5000000 : 100000;
			seq_bounds += clength + spacer;
			if (DEBUG_VIRTUAL_GENOME) {
				System.out.println("seq_bounds:" + seq_bounds);
			}
			if (seq_bounds > Integer.MAX_VALUE) {
				if (DEBUG_VIRTUAL_GENOME) {
					System.out.println("Virtual genome too large for " + group.getID());
				}
				return false;
			}
		}
		return true;
	}

	private static void addSeqToVirtualGenome(BioSeq genome_seq, MutableAnnotatedBioSeq chrom, double default_genome_min, boolean DEBUG_VIRTUAL_GENOME) {
		double glength = genome_seq.getLengthDouble();
		int clength = chrom.getLength();
		int spacer = (clength > 5000000) ? 5000000 : 100000;
		double new_glength = glength + clength + spacer;
		//	genome_seq.setLength(new_glength);
		genome_seq.setBoundsDouble(default_genome_min, default_genome_min + new_glength);
		if (DEBUG_VIRTUAL_GENOME) {
			System.out.println("added seq: " + chrom.getID() + ", new genome bounds: min = " + genome_seq.getMin() + ", max = " + genome_seq.getMax() + ", length = " + genome_seq.getLengthDouble());
		}
		MutableSeqSymmetry child = new SimpleMutableSeqSymmetry();
		MutableSeqSymmetry mapping = (MutableSeqSymmetry) genome_seq.getComposition();
		if (mapping == null) {
			mapping = new SimpleMutableSeqSymmetry();
			mapping.addSpan(new MutableDoubleSeqSpan(default_genome_min, default_genome_min + clength, genome_seq));
			genome_seq.setComposition(mapping);
		} else {
			MutableDoubleSeqSpan mspan = (MutableDoubleSeqSpan) mapping.getSpan(genome_seq);
			mspan.setDouble(default_genome_min, default_genome_min + new_glength, genome_seq);
		}
		// using doubles for coords, because may end up with coords > MAX_INT
		child.addSpan(new MutableDoubleSeqSpan(glength + default_genome_min, glength + clength + default_genome_min, genome_seq));
		child.addSpan(new MutableDoubleSeqSpan(0, clength, chrom));
		if (DEBUG_VIRTUAL_GENOME) {
			SeqUtils.printSpan(child.getSpan(0));
			SeqUtils.printSpan(child.getSpan(1));
		}
		mapping.addChild(child);
	}

	/**
	 * Load and display annotations (requested for the specific feature).
	 * Adjust the load status accordingly.
	 * @param gFeature
	 * @return
	 */
	boolean loadAndDisplayAnnotations(GenericFeature gFeature, MutableAnnotatedBioSeq cur_seq, FeaturesTableModel model) {

		// We don't validate previous load status.  It's assumed that we want to reload the feature.

		SetLoadStatus(gFeature, cur_seq, model, LoadStatus.UNLOADED);

		MutableAnnotatedBioSeq selected_seq = gmodel.getSelectedSeq();
		MutableAnnotatedBioSeq visible_seq = (MutableAnnotatedBioSeq) gviewer.getViewSeq();
		if (selected_seq == null || visible_seq == null) {
			//      ErrorHandler.errorPanel("ERROR", "You must first choose a sequence to display.");
			//System.out.println("@@@@@ selected chrom: " + selected_seq);
			//System.out.println("@@@@@ visible chrom: " + visible_seq);
			return false;
		}
		if (visible_seq != selected_seq) {
			System.out.println("ERROR, VISIBLE SPAN DOES NOT MATCH GMODEL'S SELECTED SEQ!!!");
			System.out.println("   selected seq: " + selected_seq.getID());
			System.out.println("   visible seq: " + visible_seq.getID());
			return false;
		}

		SeqSpan overlap;
		if (gFeature.loadStrategy == LoadStrategy.VISIBLE) {
			overlap = gviewer.getVisibleSpan();
		} else if (gFeature.loadStrategy == LoadStrategy.WHOLE) {
			overlap = new SimpleSeqSpan(0, selected_seq.getLength(), selected_seq);
		} else {
			ErrorHandler.errorPanel("ERROR", "Requested load strategy not recognized: " + gFeature.loadStrategy);
			return false;
		}


		ServerType serverType = gFeature.gVersion.gServer.serverType;
		Application.getSingleton().setNotLockedUpStatus();

		if (serverType == ServerType.DAS2) {
			SetLoadStatus(gFeature, cur_seq, model, LoadStatus.LOADING);
			if (loadDAS2Annotations(
							selected_seq,
							gFeature.featureName,
							(Das2VersionedSource) gFeature.gVersion.versionSourceObj,
							gviewer,
							visible_seq,
							overlap)) {
				SetLoadStatus(gFeature, cur_seq, model, LoadStatus.LOADED);
				return true;
			}
			SetLoadStatus(gFeature, cur_seq, model, LoadStatus.UNLOADED);
			return false;
		}
		if (serverType == ServerType.DAS) {
			if (DasFeatureLoader.loadFeatures(gFeature, overlap)) {
				SetLoadStatus(gFeature, cur_seq, model, LoadStatus.LOADED);
				return true;
			}
			SetLoadStatus(gFeature, cur_seq, model, LoadStatus.UNLOADED);
			return false;
		}
		if (serverType == ServerType.QuickLoad) {
			SetLoadStatus(gFeature, cur_seq, model, LoadStatus.LOADING);
			if (FeatureLoading.loadQuickLoadAnnotations(gFeature)) {
				SetLoadStatus(gFeature, cur_seq, model, LoadStatus.LOADED);
				return true;
			}
			SetLoadStatus(gFeature, cur_seq, model, LoadStatus.UNLOADED);
			return false;
		}

		System.out.println("class " + serverType + " is not implemented.");
		return false;
	}

	private static void SetLoadStatus(GenericFeature gFeature, MutableAnnotatedBioSeq aseq, FeaturesTableModel model, LoadStatus ls) {
		gFeature.LoadStatusMap.put(aseq, ls);
		model.fireTableDataChanged();

	}



	

	/**
	 * Loads (and displays) DAS/2 annotations.
	 * This is done in a multithreaded fashion so that the UI doesn't lock up.
	 * @param selected_seq
	 * @param feature_name
	 * @param version
	 * @param gviewer
	 * @param visible_seq
	 * @param overlap
	 * @return
	 */
	private boolean loadDAS2Annotations(
					MutableAnnotatedBioSeq selected_seq, final String feature_name, Das2VersionedSource version, SeqMapView gviewer, MutableAnnotatedBioSeq visible_seq, SeqSpan overlap) {
		if (!(selected_seq instanceof BioSeq)) {
			ErrorHandler.errorPanel("ERROR", "selected seq is not appropriate for loading DAS2 data");
			return false;
		}
		if (DEBUG) {
			System.out.println("seq = " + visible_seq.getID() + ", min = " + overlap.getMin() + ", max = " + overlap.getMax());
		}
		ArrayList<Das2FeatureRequestSym> requests = new ArrayList<Das2FeatureRequestSym>();
		/*for (Das2TypeState tstate : types_table_model.getTypeStates()) {
		Das2Type dtype = tstate.getDas2Type();
		Das2VersionedSource version = dtype.getVersionedSource();
		// if restricting to types from "current" version, then skip if verion != current_version
		//      if (restrict_to_current_version && (version != current_version)) { continue; }
		 */

		List<Das2Type> type_list = version.getTypesByName(feature_name);

		Das2Region region = version.getSegment(selected_seq);
		for (Das2Type dtype : type_list) {
			if (dtype != null && region != null) {
				//&& (tstate.getLoad())) {
				// maybe add a fully_loaded flag so know which ones to skip because they're done?
				Das2FeatureRequestSym request_sym = new Das2FeatureRequestSym(dtype, region, overlap, null);
				requests.add(request_sym);
			}
		}

		FeatureLoading.processDas2FeatureRequests(requests, true, gmodel, gviewer);
		return true;
	}

	/**
	 * Load residues on span.
	 * First, attempt to load them with DAS/2 servers.
	 * Second, attempt to load them with Quickload servers.
	 * Third, attempt to load them with DAS/1 servers.
	 * @param aseq
	 * @param span	-- may be null, if the entire sequence is requested.
	 * @return true if succeeded.
	 */
	boolean loadResidues(String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span) {
		String seq_name = aseq.getID();
		if (DEBUG) {
			System.out.println("processing request to load residues for sequence: " + seq_name);
		}

		/*
		 * This test does not work properly, so it's being commented out for now.
		 *
		if (aseq.isComplete()) {
			if (DEBUG) {
				System.out.println("already have residues for " + seq_name);
			}
			return false;
		}*/

		// TODO: Synonyms will be an issue soon!
		// We'll need to know what the appropriate synonym is, for the given server.

		// Determine list of servers that might have this chromosome sequence.
		List<GenericFeature> features = this.getFeatures(genomeVersionName);
		Set<GenericServer> serversWithChrom = new HashSet<GenericServer>();
		for (GenericFeature feature : features) {
			serversWithChrom.add(feature.gVersion.gServer);
		}

		if ((min <= 0) && (max >= aseq.getLength())) {
			if (DEBUG) {
				System.out.println("loading all residues");
			}
			min = 0;
			max = aseq.getLength();
		}

		return ResidueLoading.getResidues(serversWithChrom, genomeVersionName, seq_name, min, max, aseq, span);
	}

}
