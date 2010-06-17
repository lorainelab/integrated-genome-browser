package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.MutableDoubleSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.das.DasFeatureLoader;
import com.affymetrix.genometryImpl.das.DasServerInfo;
import com.affymetrix.genometryImpl.das.DasSource;
import com.affymetrix.genometryImpl.das2.Das2FeatureRequestSym;
import com.affymetrix.genometryImpl.das2.Das2Region;
import com.affymetrix.genometryImpl.das2.Das2ServerInfo;
import com.affymetrix.genometryImpl.das2.Das2Source;
import com.affymetrix.genometryImpl.das2.Das2Type;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.general.FeatureLoading;
import com.affymetrix.igb.general.ResidueLoading;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.symloader.QuickLoad;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.igb.view.SeqMapView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @version $Id$
 */
public final class GeneralLoadUtils {
	private static final boolean DEBUG = false;
	private static final boolean DEBUG_VIRTUAL_GENOME = false;

	private static final Pattern tab_regex = Pattern.compile("\t");
	/**
	 *  using negative start coord for virtual genome chrom because (at least for human genome)
	 *     whole genome start/end/length can't be represented with positive 4-byte ints (limit is +/- 2.1 billion)
	 */
//    final double default_genome_min = -2100200300;
	private static final double default_genome_min = -2100200300;

	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();

	public static final String SERVER_MAPPING = "/serverMapping.txt";

	/**
	 * Location of synonym file for correlating versions to species.
	 * The file lookup is done using {@link Class#getResourceAsStream(String)}.
	 * The default file is {@value}.
	 *
	 * @see #SPECIES_LOOKUP
	 */
	private static final String SPECIES_SYNONYM_FILE = "/species.txt";

	/** Unused list of chromosomes that may be used in a future chromosome lookup */
	private static final String CHROM_SYNONYM_FILE = "/chromosomes.txt";
	
	private final static SeqMapView gviewer = Application.getSingleton().getMapView();

	// versions associated with a given genome.
	static final Map<String, List<GenericVersion>> species2genericVersionList =
			new LinkedHashMap<String, List<GenericVersion>>();	// the list of versions associated with the species
	static final Map<String, String> versionName2species =
			new HashMap<String, String>();	// the species associated with the given version.

	/**
	 * Private copy of the default Synonym lookup
	 * @see SynonymLookup#getDefaultLookup()
	 */
	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	
	/** Private synonym lookup for correlating versions to species. */
	private static final SpeciesLookup SPECIES_LOOKUP = new SpeciesLookup();
	
	static {
		try {
			SPECIES_LOOKUP.load(GeneralLoadUtils.class.getResourceAsStream(SPECIES_SYNONYM_FILE));
		} catch (IOException ex) {
			Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(GeneralLoadUtils.class.getResourceAsStream(SPECIES_SYNONYM_FILE));
		}
	}
	
	private static Map<URL,URL> servermapping = new HashMap<URL,URL>();
	/**
	 * Add specified server, finding species and versions associated with it.
	 * @param serverName
	 * @param serverURL
	 * @param serverType
	 * @return success of server add.
	 */
	public static GenericServer addServer(ServerType serverType, String serverName, String serverURL) {
		/* should never happen */
		if (serverType == ServerType.LocalFiles) { return null; }
		
		GenericServer gServer = ServerList.addServer(serverType, serverName, serverURL, true);
		if (gServer == null) {
			return null;
		}
		if (!discoverServer(gServer)) {
			ServerList.removeServer(serverURL);
			return null;
		}

		return gServer;
	}

	public static void removeServer(GenericServer server) {
		Iterator<Map.Entry<String, List<GenericVersion>>> entryIterator = species2genericVersionList.entrySet().iterator();
		Map.Entry<String, List<GenericVersion>> entry;
		Iterator<GenericVersion> versionIterator;
		GenericVersion version;

		while (entryIterator.hasNext()) {
			entry = entryIterator.next();
			versionIterator = entry.getValue().iterator();

			while (versionIterator.hasNext()) {
				version = versionIterator.next();

				if (version.gServer == server) {
					versionIterator.remove();
				}
			}
			if (entry.getValue().isEmpty()) {
				entryIterator.remove();
			}
		}
	}

	public static boolean discoverServer(GenericServer gServer) {
		return discoverServer(gServer, true);
	}

	public static boolean discoverServer(GenericServer gServer, boolean loadGenome) {
		try {
			if (gServer == null || gServer.serverType == ServerType.LocalFiles) {
				// should never happen
				return false;
			}
			if (gServer.serverType == ServerType.QuickLoad) {
				if (!getQuickLoadSpeciesAndVersions(gServer, loadGenome)) {
					ServerList.fireServerInitEvent(gServer, ServerStatus.NotResponding);
					return false;
				}
			} else if (gServer.serverType == ServerType.DAS) {
				if (!getDAS1SpeciesAndVersions(gServer)) {
					ServerList.fireServerInitEvent(gServer, ServerStatus.NotResponding);
					return false;
				}
			} else if (gServer.serverType == ServerType.DAS2) {
				if (!getDAS2SpeciesAndVersions(gServer)) {
					ServerList.fireServerInitEvent(gServer, ServerStatus.NotResponding);
					return false;
				}
			}
			ServerList.fireServerInitEvent(gServer, ServerStatus.Initialized);
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}


	/**
	 * Discover species from DAS
	 * @param gServer
	 * @return false if there's an obvious problem
	 */
	private static boolean getDAS1SpeciesAndVersions(GenericServer gServer) {
		DasServerInfo server = (DasServerInfo) gServer.serverObj;
		Map<String,DasSource> sources = server.getDataSources();
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			System.out.println("WARNING: Couldn't find species for server: " + gServer);
			return false;
		}
		for (DasSource source : sources.values()) {
			String speciesName = SPECIES_LOOKUP.getSpeciesName(source.getID());
			String versionName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), source.getID());
			String versionID = source.getID();
			discoverVersion(versionID, versionName, gServer, source, speciesName);
		}
		return true;
	}


	/**
	 * Discover genomes from DAS/2
	 * @param gServer
	 * @return false if there's an obvious problem
	 */
	private static boolean getDAS2SpeciesAndVersions(GenericServer gServer) {
		Das2ServerInfo server = (Das2ServerInfo) gServer.serverObj;
		Map<String,Das2Source> sources = server.getSources();
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			System.out.println("WARNING: Couldn't find species for server: " + gServer);
			return false;
		}
		for (Das2Source source : sources.values()) {
			String speciesName = SPECIES_LOOKUP.getSpeciesName(source.getName());
			
			// Das/2 has versioned sources.  Get each version.
			for (Das2VersionedSource versionSource : source.getVersions().values()) {
				String versionName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), versionSource.getName());
				String versionID = versionSource.getName();
				discoverVersion(versionID, versionName, gServer, versionSource, speciesName);
			}
		}
		return true;
	}

	/**
	 * Discover genomes from Quickload
	 * @param gServer
	 * @param loadGenome boolean to check load genomes from server.
	 * @return false if there's an obvious failure.
	 */
	private static boolean getQuickLoadSpeciesAndVersions(GenericServer gServer, boolean loadGenome) {
		URL quickloadURL = null;
		try {
			quickloadURL = new URL((String) gServer.serverObj);
		} catch (MalformedURLException ex) {
			Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		GenericServer primaryServer = ServerList.getPrimaryServer();
		QuickLoadServerModel quickloadServer;

		if(loadGenome || primaryServer == null){
			quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
		}else{
			quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL, primaryServer.friendlyURL);
		}

		if (quickloadServer == null) {
			System.out.println("ERROR: No quickload server model found for server: " + gServer);
			return false;
		}
		List<String> genomeList = quickloadServer.getGenomeNames();
		if (genomeList == null || genomeList.isEmpty()) {
			System.out.println("WARNING: No species found in server: " + gServer);
			return false;
		}

		for (String genomeID : genomeList) {
			String genomeName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), genomeID);
			String versionName,speciesName;
			// Retrieve group identity, since this has already been added in QuickLoadServerModel.
			Set<GenericVersion> gVersions = gmodel.addSeqGroup(genomeName).getEnabledVersions();
			if (!gVersions.isEmpty()) {
				// We've found a corresponding version object that was initialized earlier.
				versionName = getPreferredVersionName(gVersions);
				speciesName = versionName2species.get(versionName);
			} else {
				// Unknown genome.  We'll add the name as if it's a species and a version.
				if (DEBUG) {
					System.out.println("Unknown quickload genome:" + genomeName);
				}
				versionName = genomeName;
				speciesName = SPECIES_LOOKUP.getSpeciesName(genomeName);
			}
			discoverVersion(genomeID, versionName, gServer, quickloadServer, speciesName);
		}
		return true;
	}


	/**
	 * An AnnotatedSeqGroup was added independently of the GeneralLoadUtils.
	 * Update GeneralLoadUtils state.
	 * @param aseq
	 * @return genome version
	 */
	static GenericVersion getUnknownVersion(AnnotatedSeqGroup aseq) {
		String versionName = aseq.getID();
		String speciesName = "-- Unknown -- " + versionName;	// make it distinct, but also make it appear at the top of the species list.

		GenericServer server = ServerList.getLocalFilesServer();
		
		return discoverVersion(versionName, versionName, server, null, speciesName);
	}

	/**
	 * An AnnotatedSeqGroup was added independently of the GeneralLoadUtils.
	 * Update GeneralLoadUtils state.
	 * @param aseq
	 * @return genome version
	 */
	public static GenericVersion getLocalFilesVersion(AnnotatedSeqGroup aseq) {
		String versionName = aseq.getID();
		String speciesName = GeneralLoadUtils.versionName2species.get(versionName);
		if (speciesName == null) {
			 speciesName = "-- Unknown -- " + versionName;	// make it distinct, but also make it appear at the top of the species list
		}
		GenericServer server = ServerList.getLocalFilesServer();

		return discoverVersion(versionName, versionName, server, null, speciesName);
	}

	private static synchronized GenericVersion discoverVersion(String versionID, String versionName, GenericServer gServer, Object versionSourceObj, String speciesName) {
		// Make sure we use the preferred synonym for the genome version.
		String preferredVersionName = LOOKUP.getPreferredName(versionName);
		AnnotatedSeqGroup group = gmodel.addSeqGroup(preferredVersionName); // returns existing group if found, otherwise creates a new group

		GenericVersion gVersion = new GenericVersion(group, versionID, preferredVersionName, gServer, versionSourceObj);
		List<GenericVersion> gVersionList = getSpeciesVersionList(speciesName);
		versionName2species.put(preferredVersionName, speciesName);
		if (!gVersionList.contains(gVersion)) {
			gVersionList.add(gVersion);
		}
		group.addVersion(gVersion);
		if (DEBUG) {
			System.out.println("Added " + gVersion.gServer.serverType + " genome: " + speciesName + " version: " + preferredVersionName);
		}
		return gVersion;
	}


	/**
	 * Get list of versions for given species.  Create it if it doesn't exist.
	 * @param speciesName
	 * @return list of versions for the given species.
	 */
	private static List<GenericVersion> getSpeciesVersionList(String speciesName) {
		List<GenericVersion> gVersionList;
		if (!species2genericVersionList.containsKey(speciesName)) {
			gVersionList = new ArrayList<GenericVersion>();
			species2genericVersionList.put(speciesName, gVersionList);
		} else {
			gVersionList = species2genericVersionList.get(speciesName);
		}
		return gVersionList;
	}
	
	/**
	 *  Returns the list of features for the genome with the given version name.
	 *  The list may (rarely) be empty, but never null.
	 */
	public static List<GenericFeature> getFeatures(final String versionName) {
		// There may be more than one server with the same versionName.  Merge all the version names.
		List<GenericFeature> featureList = new ArrayList<GenericFeature>();
		AnnotatedSeqGroup group = gmodel.getSeqGroup(versionName);
		if (group != null) {
			Set<GenericVersion> versions = group.getEnabledVersions();
			if (versions != null) {
				for (GenericVersion gVersion : versions) {
					featureList.addAll(gVersion.getFeatures());
				}
			}
		}
		return featureList;
	}

	/**
	 * Returns the list of servers associated with the given versions.
	 * @param features -- assumed to be non-null.
	 * @return A list of servers associated with the given versions.
	 */
	public static List<GenericServer> getServersWithAssociatedFeatures(List<GenericFeature> features) {
		List<GenericServer> serverList = new ArrayList<GenericServer>();
		for (GenericFeature gFeature : features) {
			if (!serverList.contains(gFeature.gVersion.gServer)) {
				serverList.add(gFeature.gVersion.gServer);
			}
		}
		// make sure these servers always have the same order
		Collections.sort(serverList);
		return serverList;
	}

	
	/**
	 * Make sure this genome version has been initialized.
	 * @param versionName
	 */
	public static void initVersionAndSeq(final String versionName) {
		if (versionName == null) {
			return;
		}
		AnnotatedSeqGroup group = gmodel.getSeqGroup(versionName);
		for (GenericVersion gVersion : group.getEnabledVersions()) {
			if (!gVersion.isInitialized()) {
				FeatureLoading.loadFeatureNames(gVersion);
				gVersion.setInitialized();
			}
		}
		if (DEBUG) {
			System.out.println("Seq count: " + group.getSeqCount());
		}
		if (group.getSeqCount() == 0) {
			loadChromInfo(group);
		}
		addGenomeVirtualSeq(group);	// okay to run this multiple times
	}



	/**
	 * Load the sequence info for the given group.
	 * Try loading from DAS/2 before loading from DAS; chances are DAS/2 will be faster, and that the chromosome
	 * names will be closer to what is expected.
	 */
	private static void loadChromInfo(AnnotatedSeqGroup group) {

		for (GenericVersion gVersion : group.getEnabledVersions()) {
			if (gVersion.gServer.serverType != ServerType.DAS2) {
				continue;
			}
			if (DEBUG) {
					System.out.println("Discovering " + gVersion.gServer.serverType + " chromosomes");
				}
			// Discover chromosomes from DAS/2
			Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;

			version.getGenome();  // adds genome to singleton genometry model if not already present
			// Calling version.getSegments() to ensure that Das2VersionedSource is populated with Das2Region segments,
			//    which in turn ensures that AnnotatedSeqGroup is populated with SmartAnnotBioSeqs
			version.getSegments();
			return;
		}

		for (GenericVersion gVersion : group.getEnabledVersions()) {
			if (gVersion.gServer.serverType != ServerType.DAS) {
				continue;
			}
			if (DEBUG) {
				System.out.println("Discovering " + gVersion.gServer.serverType + " chromosomes");
			}
			// Discover chromosomes from DAS
			DasSource version = (DasSource) gVersion.versionSourceObj;

			version.getGenome();
			version.getEntryPoints();
			return;
		}
	}

	private static void addGenomeVirtualSeq(AnnotatedSeqGroup group) {
		int chrom_count = group.getSeqCount();
		if (chrom_count <= 1) {
			// no need to make a virtual "genome" chrom if there is only a single chromosome
			return;
		}

		if (DEBUG) {
			System.out.println("$$$$$ adding virtual genome seq to seq group");
		}
		double seqBounds = determineSeqBounds(group, chrom_count);
		if (seqBounds > Integer.MAX_VALUE) {
			return;
		}
		if (group.getSeq(IGBConstants.GENOME_SEQ_ID) != null) {
			return; // return if we've already created the virtual genome
		}

		BioSeq genome_seq = null;
		try {
			genome_seq = group.addSeq(IGBConstants.GENOME_SEQ_ID, 0);
		} catch (IllegalStateException ex) {
			// due to multithreading, it's possible that this sequence has been created by another thread while doing this test.
			// we can safely return in this case.
			if (DEBUG) {
				System.out.println("Ignoring illegal state exception.");
			}
			return;
		}
		for (int i = 0; i < chrom_count; i++) {
			BioSeq chrom_seq = group.getSeq(i);
			if (chrom_seq == genome_seq) {
				continue;
			}
			// Add seq to virtual genome.  Keep values above 0 if possible.
			addSeqToVirtualGenome(seqBounds < 0 ? 0.0 : default_genome_min, genome_seq, chrom_seq);
		}
	}

	/**
	 * Make sure virtual genome doesn't overflow int bounds.
	 * @param group
	 * @return true or false
	 */
	private static double determineSeqBounds(AnnotatedSeqGroup group, int chrom_count) {
		double seq_bounds = default_genome_min;

		for (int i = 0; i < chrom_count; i++) {
			BioSeq chrom_seq = group.getSeq(i);
			int clength = chrom_seq.getLength();
			int spacer = (clength > 5000000) ? 5000000 : 100000;
			seq_bounds += clength + spacer;
			if (DEBUG_VIRTUAL_GENOME) {
				System.out.println("seq_bounds:" + seq_bounds);
			}
		}
		return seq_bounds;
	}

	private static void addSeqToVirtualGenome(double genome_min, BioSeq genome_seq, BioSeq chrom) {
		double glength = genome_seq.getLengthDouble();
		int clength = chrom.getLength();
		int spacer = (clength > 5000000) ? 5000000 : 100000;
		double new_glength = glength + clength + spacer;
		//	genome_seq.setLength(new_glength);
		genome_seq.setBoundsDouble(genome_min, genome_min + new_glength);
		if (DEBUG_VIRTUAL_GENOME) {
			System.out.println("added seq: " + chrom.getID() + ", new genome bounds: min = " + genome_seq.getMin() + ", max = " + genome_seq.getMax() + ", length = " + genome_seq.getLengthDouble());
		}
		MutableSeqSymmetry child = new SimpleMutableSeqSymmetry();
		MutableSeqSymmetry mapping = (MutableSeqSymmetry) genome_seq.getComposition();
		if (mapping == null) {
			mapping = new SimpleMutableSeqSymmetry();
			mapping.addSpan(new MutableDoubleSeqSpan(genome_min, genome_min + clength, genome_seq));
			genome_seq.setComposition(mapping);
		} else {
			MutableDoubleSeqSpan mspan = (MutableDoubleSeqSpan) mapping.getSpan(genome_seq);
			mspan.setDouble(genome_min, genome_min + new_glength, genome_seq);
		}
		// using doubles for coords, because may end up with coords > MAX_INT
		child.addSpan(new MutableDoubleSeqSpan(glength + genome_min, glength + clength + genome_min, genome_seq));
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
	 * @return true or false
	 */
	static boolean loadAndDisplayAnnotations(GenericFeature gFeature, FeaturesTableModel model) {
		BioSeq selected_seq = gmodel.getSelectedSeq();
		BioSeq visible_seq = gviewer.getViewSeq();
		if ((selected_seq == null || visible_seq == null) && (gFeature.gVersion.gServer.serverType != ServerType.LocalFiles)) {
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

		SeqSpan overlap = null;
		if (gFeature.loadStrategy == LoadStrategy.VISIBLE) {
			overlap = gviewer.getVisibleSpan();
		} else if (gFeature.loadStrategy == LoadStrategy.GENOME || gFeature.loadStrategy == LoadStrategy.CHROMOSOME) {
			if (selected_seq != null) {
				overlap = new SimpleSeqSpan(0, selected_seq.getLength(), selected_seq);
			}
		} else {
			ErrorHandler.errorPanel("ERROR", "Requested load strategy not recognized: " + gFeature.loadStrategy);
			return false;
		}


		ServerType serverType = gFeature.gVersion.gServer.serverType;

		if (serverType == ServerType.DAS2) {
			Application.getSingleton().addNotLockedUpMsg("Loading feature " + gFeature.featureName);
			return loadFeatures(overlap, gFeature);
		}
		if (serverType == ServerType.DAS) {
			Application.getSingleton().addNotLockedUpMsg("Loading feature " + gFeature.featureName);
			return DasFeatureLoader.loadFeatures(overlap, gFeature);
		}
		if (serverType == ServerType.QuickLoad) {
			QuickLoad symL = (QuickLoad) gFeature.symL;
			Application.getSingleton().addNotLockedUpMsg("Loading feature " + symL.featureName);
			return symL.loadFeatures(overlap, gFeature.loadStrategy);
		}
		if (serverType == ServerType.LocalFiles) {
			QuickLoad symL = (QuickLoad) gFeature.symL;
			Application.getSingleton().addNotLockedUpMsg("Loading feature " + symL.featureName);
			return symL.loadFeatures(overlap, gFeature.loadStrategy);
		}
		System.out.println("class " + serverType + " is not implemented.");
		return false;
	}

	

	/**
	 * Loads (and displays) DAS/2 annotations.
	 * This is done in a multithreaded fashion so that the UI doesn't lock up.
	 * @param selected_seq
	 * @param gFeature
	 * @param gviewer
	 * @param overlap
	 * @return true or false
	 */
	private static boolean loadFeatures(SeqSpan overlap, GenericFeature gFeature) {
		final String feature_name = gFeature.featureName;
		final BioSeq selected_seq = overlap.getBioSeq();
		if (selected_seq == null) {
			ErrorHandler.errorPanel("ERROR", "selected seq is not appropriate for loading DAS2 data");
			Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature_name);
			return false;
		}
		if (DEBUG) {
			System.out.println("seq = " + selected_seq.getID() + ", min = " + overlap.getMin() + ", max = " + overlap.getMax());
		}
		Das2VersionedSource version = (Das2VersionedSource)gFeature.gVersion.versionSourceObj;
		List<Das2FeatureRequestSym> requests = new ArrayList<Das2FeatureRequestSym>();

		List<Das2Type> type_list = version.getTypesByName(feature_name);

		Das2Region region = version.getSegment(selected_seq);
		for (Das2Type dtype : type_list) {
			if (dtype != null && region != null) {
				// maybe add a fully_loaded flag so know which ones to skip because they're done?
				Das2FeatureRequestSym request_sym = new Das2FeatureRequestSym(dtype, region, overlap, null);
				requests.add(request_sym);
			}
		}

		FeatureLoading.processDas2FeatureRequests(requests, feature_name, true, gmodel);
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
	static boolean loadResidues(String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span) {
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
		List<GenericFeature> features = getFeatures(genomeVersionName);
		Set<GenericVersion> versionsWithChrom = new HashSet<GenericVersion>();
		for (GenericFeature feature : features) {
			versionsWithChrom.add(feature.gVersion);
		}

		if ((min <= 0) && (max >= aseq.getLength())) {
			if (DEBUG) {
				System.out.println("loading all residues");
			}
			min = 0;
			max = aseq.getLength();
		}

		return ResidueLoading.getResidues(versionsWithChrom, genomeVersionName, seq_name, min, max, aseq, span);
	}


	static String getPreferredVersionName(Set<GenericVersion> gVersions) {
		return LOOKUP.getPreferredName(gVersions.iterator().next().versionName);
	}

	/**
	 * Get synonyms of version.
	 * @param versionName - version name
	 * @return a friendly HTML string of version synonyms (not including versionName).
	 */
	static String listSynonyms(String versionName) {
		StringBuilder synonymBuilder = new StringBuilder(100);
		synonymBuilder.append("<html>" + IGBConstants.BUNDLE.getString("synonymList"));
		Set<String> synonymSet = LOOKUP.getSynonyms(versionName);
		for (String synonym : synonymSet) {
			if (synonym.equalsIgnoreCase(versionName)) {
				continue;
			}
			synonymBuilder.append("<p>" + synonym + "</p>");
		}
		if (synonymSet.size() <= 1) {
			synonymBuilder.append(IGBConstants.BUNDLE.getString("noSynonyms"));
		}
		synonymBuilder.append("</html>");
		return synonymBuilder.toString();
	}

	static String getSpeciesCommonName(String speciesName) {
		return SPECIES_LOOKUP.getCommonSpeciesName(speciesName);
	}

	public static void loadServerMapping() {
		InputStream istr = null;
		InputStreamReader ireader = null;
		BufferedReader br = null;
		GenericServer primaryServer = ServerList.getPrimaryServer();

		if (primaryServer != null) {
			try {
				try {
					istr = LocalUrlCacher.getInputStream(primaryServer.friendlyURL.toExternalForm() + SERVER_MAPPING);
				} catch (Exception e) {
					System.out.println("ERROR: Couldn't open '" + primaryServer.friendlyURL.toExternalForm() + SERVER_MAPPING + "\n:  " + e.toString());
					istr = null; // dealt with below
				}
				if (istr == null) {
					System.out.println("Could not load server mapping contents from\n" + primaryServer.friendlyURL.toExternalForm() + SERVER_MAPPING);
					return;
				}
				ireader = new InputStreamReader(istr);
				br = new BufferedReader(ireader);
				String line;
				while ((line = br.readLine()) != null) {
					if ((line.length() == 0) || line.startsWith("#")) {
						continue;
					}

					String[] fields = tab_regex.split(line);
					if(fields.length >= 2){
						URL serverURL = new URL(fields[0]);
						URL dirURL = new URL(primaryServer.URL + fields[1]);
						servermapping.put(serverURL, dirURL);
					}
				}
			} catch (Exception ex) {
				ErrorHandler.errorPanel("ERROR", "Error loading server mapping", ex);
			} finally {
				GeneralUtils.safeClose(istr);
				GeneralUtils.safeClose(ireader);
				GeneralUtils.safeClose(br);
			}
		}
	}
}
