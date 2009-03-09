package com.affymetrix.igb.view.load;

import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.MutableSeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.MutableDoubleSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.das.DasDiscovery;
import com.affymetrix.igb.das.DasFeatureLoader;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das.DasSource;
import com.affymetrix.igb.das2.Das2Discovery;
import com.affymetrix.igb.das2.Das2FeatureRequestSym;
import com.affymetrix.igb.das2.Das2Region;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.das2.Das2Source;
import com.affymetrix.igb.das2.Das2Type;
import com.affymetrix.igb.das2.Das2VersionedSource;
import com.affymetrix.igb.general.FeatureLoading;
import com.affymetrix.igb.general.GenericFeature;
import com.affymetrix.igb.general.GenericVersion;
import com.affymetrix.igb.general.ResidueLoading;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.general.GenericServer;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.menuitem.OpenGraphAction;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.igb.view.SeqMapView;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @version $Id$
 */
final public class GeneralLoadUtils {

	public static enum LoadStrategy {

		NO_LOAD, VISIBLE, WHOLE
	};

	public static enum LoadStatus {

		UNLOADED, LOADING, LOADED
	};
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
	final static String ENCODE_FILE_NAME = "encodeRegions.bed";
	final static String ENCODE_FILE_NAME2 = "encode.bed";
	public final static String GENOME_SEQ_ID = "genome";                // user here and in SeqMapView
	public final static String ENCODE_REGIONS_ID = "encode_regions";    // used in SeqMapView
	//static boolean CACHE_RESIDUES_DEFAULT = false;
	//static boolean CACHE_ANNOTS_DEFAULT = true;

	/* TODO: can one group be represented by multiple versions? */
	final Map<AnnotatedSeqGroup, GenericVersion> group2version;
	private final Map<String, Boolean> version2init;

	// server name-> GenericServer class.
	final Map<String, GenericServer> discoveredServers;

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
	public void clear() {
		group2version.clear();
		version2init.clear();
		discoveredServers.clear();
		species2genericVersionList.clear();
		versionName2species.clear();
		versionName2versionSet.clear();
	}

	public GeneralLoadUtils() {
		this.gviewer = Application.getSingleton().getMapView();

		group2version = new HashMap<AnnotatedSeqGroup, GenericVersion>();
		version2init = new HashMap<String, Boolean>();
		discoveredServers = new LinkedHashMap<String, GenericServer>();
		species2genericVersionList = new LinkedHashMap<String, List<GenericVersion>>();
		versionName2species = new HashMap<String, String>();
		versionName2versionSet = new HashMap<String, Set<GenericVersion>>();
	}

	/**
	 * Discover all of the servers and genomes and versions.
	 */
	void discoverServersAndSpeciesAndVersions() {
		// it's assumed that if we're here, we need to refresh this information.
		discoveredServers.clear();

		// We use a thread to get the servers.  (Otherwise the user may see a lockup of their UI.)
		/*try {
			Runnable r = new Runnable() {

				public void run() {*/
					discoverServersInternal(discoveredServers);
					discoverSpeciesAndVersionsInternal();
/*				}
			};
			Thread thr1 = new Thread(r);
			thr1.start();
			while (thr1.isAlive()) {
				Thread.sleep(200);
			}
		} catch (InterruptedException ie) {
			System.out.println("Interruption while getting server list.");
		}*/
	}

	/**
	 * Discover the list of servers.
	 */
	public static synchronized void discoverServersInternal(final Map<String, GenericServer> discoveredServers) {
		for (Map.Entry<String, Das2ServerInfo> entry : Das2Discovery.getDas2Servers().entrySet()) {
			Das2ServerInfo server = entry.getValue();
			String serverName = entry.getKey();
			if (server != null && serverName != null) {
				if (!discoveredServers.containsKey(serverName)) {
					GenericServer g = new GenericServer(serverName, server.getURI().toString(), server.getClass(), server);
					discoveredServers.put(serverName, g);
				}
			}
		}

		// Discover DAS servers
		// TODO -- strip out descriptions and make synonyms with DAS/2
		// TODO -- get chromosome info
		for (Map.Entry<String, DasServerInfo> entry : DasDiscovery.getDasServers().entrySet()) {
			DasServerInfo server = entry.getValue();
			String serverName = entry.getKey();
			if (server != null && serverName != null) {
				if (!discoveredServers.containsKey(serverName)) {
					GenericServer g = new GenericServer(serverName, server.getRootUrl(), server.getClass(), server);
					discoveredServers.put(serverName, g);
				}
			}
		}

		// Discover Quickload servers
		// This is based on new preferences, which allow arbitrarily many quickload servers.
		for (GenericServer gServer : ServerList.getServers().values()) {
			if (gServer.serverType == GenericServer.ServerType.QuickLoad) {
				discoveredServers.put(gServer.serverName, gServer);
			}
		}
	}

	/**
	 * Discover the species and genome versions.
	 */
	private synchronized void discoverSpeciesAndVersionsInternal() {
		for (GenericServer gServer : discoveredServers.values()) {
			if (gServer.serverType == GenericServer.ServerType.DAS2) {
				getDAS2Species(gServer);
				getDAS2Versions(gServer);
				continue;
			}
			if (gServer.serverType == GenericServer.ServerType.DAS) {
				getDAS1SpeciesAndVersions(gServer);
				continue;
			}
			if (gServer.serverType == GenericServer.ServerType.QuickLoad) {
				getQuickLoadSpeciesAndVersions(gServer);
				continue;
			}
			if (gServer.serverType == GenericServer.ServerType.Unknown) {
				System.out.println("WARNING: Discovered server class " + gServer.serverType);
				continue;
			}
			System.out.println("WARNING: Unknown server class " + gServer.serverType);
		}
	}

	/**
	 * Discover species from DAS
	 * @param gServer
	 */
	private synchronized void getDAS1SpeciesAndVersions(GenericServer gServer) {
		DasServerInfo server = (DasServerInfo) gServer.serverObj;
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
	}


	/**
	 * Discover genomes from DAS/2
	 * @param gServer
	 */
	private synchronized void getDAS2Species(GenericServer gServer) {
		Das2ServerInfo server = (Das2ServerInfo) gServer.serverObj;
		for (Das2Source source : server.getSources().values()) {
			String speciesName = SPECIES_LOOKUP.getPreferredName(source.getName());
			List<GenericVersion> gVersionList;
			if (!this.species2genericVersionList.containsKey(speciesName)) {
				gVersionList = new ArrayList<GenericVersion>();
				this.species2genericVersionList.put(speciesName, gVersionList);
			}
		}
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
	 */
	private synchronized void getQuickLoadSpeciesAndVersions(GenericServer gServer) {
		URL quickloadURL = null;
		try {
			quickloadURL = new URL((String) gServer.serverObj);
		} catch (MalformedURLException ex) {
			Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.SEVERE, null, ex);
		}
		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(gmodel, quickloadURL);
		List<String> genomeList = quickloadServer.getGenomeNames();

		for (String genomeID : genomeList) {
			String genomeName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), genomeID);
			// Retrieve group identity, since this has already been added in QuickLoadServerModel.

			AnnotatedSeqGroup group = gmodel.addSeqGroup(genomeName);
			GenericVersion gVersion = this.group2version.get(group);
			if (gVersion != null) {
				// We've found a corresponding version object that was initialized earlier.
				String speciesName = this.versionName2species.get(gVersion.versionName);
				GenericVersion quickLoadVersion = new GenericVersion(genomeID, gVersion.versionName, gServer, quickloadServer);
				List<GenericVersion> gVersionList = this.species2genericVersionList.get(speciesName);
				discoverVersion(gVersion.versionName, gServer, quickLoadVersion, gVersionList, speciesName);
				continue;
			}
			String species = SPECIES_LOOKUP.getPreferredName(genomeName);

			// Unknown genome.  We'll add the name as if it's a species and a version.
			if (DEBUG) {
				System.out.println("Unknown quickload genome:" + genomeName);
			}

			List<GenericVersion> gVersionList = this.getSpeciesVersionList(species);

			gVersion = new GenericVersion(genomeID, genomeName, gServer, quickloadServer);
			discoverVersion(gVersion.versionName, gServer, gVersion, gVersionList, species);
		}
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

		GenericServer gServer = new  GenericServer(null, null, GenericServer.ServerType.Unknown, null);
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
		group2version.put(group, gVersion);
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
	/*public static String stripFilenameExtensions(final String name) {
		String new_name = name;
		if (name.indexOf('.') > 0) {
			new_name = name.substring(0, name.lastIndexOf('.'));
		}
		return new_name;
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

	/** Returns true if the given genome has already been initialized via initGenome(String). */
	/*public boolean isInitialized(final String genome_name) {
		Boolean b = version2init.get(genome_name);
		return (Boolean.TRUE.equals(b));
	}*/

	

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

		addGenomeVirtualSeq(group, default_genome_min, DEBUG_VIRTUAL_GENOME);

		for (GenericVersion gVersion : gVersions) {
			// Initialize all the servers with unloaded status of the feature/chromosome combinations.
			for (GenericFeature gFeature : gVersion.features) {
				for (SmartAnnotBioSeq sabq : group.getSeqList()) {
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
				System.out.println("loading list of chromosomes for genome version: " + gVersion.versionName + " from server " + gVersion.gServer.serverName);
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
		
		//System.out.println("group: " + (group == null ? null : group.getID()) + ", " + group);
		Application.getApplicationLogger().fine("loading list of chromosomes for genome: " + gVersion.versionName);
		//Application.getApplicationLogger().fine("group: " + (group == null ? null : group.getID()) + ", " + group);

		// discover genomes from server
		if (gVersion.gServer == null) {
			return null;
		}
		if (DEBUG) {
			System.out.println("Discovering " + gVersion.gServer.serverType + " chromosomes");
		}
		if (gVersion.gServer.serverType == GenericServer.ServerType.DAS2) {

			// Discover chromosomes from DAS/2
			Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;

			group = version.getGenome();  // adds genome to singleton genometry model if not already present
			// Calling version.getSegments() to ensure that Das2VersionedSource is populated with Das2Region segments,
			//    which in turn ensures that AnnotatedSeqGroup is populated with SmartAnnotBioSeqs
			group.setSource(gVersion.gServer.serverName);
			version.getSegments();
			return group;
		}
		if (gVersion.gServer.serverType == GenericServer.ServerType.DAS) {
			// Discover chromosomes from DAS
			DasSource version = (DasSource) gVersion.versionSourceObj;

			group = version.getGenome();
			group.setSource(gVersion.gServer.serverName);
			version.getEntryPoints();

			return group;
		}
		if (gVersion.gServer.serverType == GenericServer.ServerType.QuickLoad) {
			// Discover chromosomes from QuickLoad
			group = gmodel.addSeqGroup(gVersion.versionName);
			group.setSource(gVersion.gServer.serverName);
			return group;
		}
		if (gVersion.gServer.serverType == GenericServer.ServerType.Unknown) {
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
		if (group.getSeq(GENOME_SEQ_ID) != null) {
			return; // return if we've already created the virtual genome
		}

		SmartAnnotBioSeq genome_seq = group.addSeq(GENOME_SEQ_ID, 0);
		for (int i = 0; i < chrom_count; i++) {
			BioSeq chrom_seq = group.getSeq(i);
			if (chrom_seq == genome_seq) {
				continue;
			}
			addSeqToVirtualGenome(genome_seq, chrom_seq, default_genome_min, DEBUG_VIRTUAL_GENOME);
		}
	}

	private static void addSeqToVirtualGenome(SmartAnnotBioSeq genome_seq, BioSeq chrom, double default_genome_min, boolean DEBUG_VIRTUAL_GENOME) {
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
	boolean loadAndDisplayAnnotations(GenericFeature gFeature, AnnotatedBioSeq cur_seq, FeaturesTableModel model) {

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


		GenericServer.ServerType serverType = gFeature.gVersion.gServer.serverType;
		if (serverType == GenericServer.ServerType.DAS2) {
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
		if (serverType == GenericServer.ServerType.DAS) {
			if (DasFeatureLoader.loadFeatures(gFeature, overlap)) {
				SetLoadStatus(gFeature, cur_seq, model, LoadStatus.LOADED);
				return true;
			}
			SetLoadStatus(gFeature, cur_seq, model, LoadStatus.UNLOADED);
			return false;
		}
		if (serverType == GenericServer.ServerType.QuickLoad) {
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

	private static void SetLoadStatus(GenericFeature gFeature, AnnotatedBioSeq aseq, FeaturesTableModel model, LoadStatus ls) {
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
		if (!(selected_seq instanceof SmartAnnotBioSeq)) {
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

		if (requests.size() > 0) {
			FeatureLoading.processDas2FeatureRequests(requests, true, gmodel, gviewer);
		}
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
	boolean loadResidues(String genomeVersionName, SmartAnnotBioSeq aseq, int min, int max, SeqSpan span) {
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
