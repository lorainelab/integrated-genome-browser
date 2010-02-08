package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.MutableDoubleSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStatus;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
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
import java.util.Collections;
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

	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();


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
		}
	}
	
	/**
	 * Add specified server, finding species and versions associated with it.
	 * @param serverName
	 * @param serverURL
	 * @param serverType
	 * @return success of server add.
	 */
	public static GenericServer addServer(ServerType serverType, String serverName, String serverURL) {
		/* should never happen */
		if (serverType == ServerType.Unknown) { return null; }
		
		GenericServer gServer = ServerList.addServer(serverType, serverName, serverURL);
		if (gServer == null || !discoverServer(gServer)) {
			return null;
		}

		return gServer;
	}

	/**
	 * Remove specified server.
	 * @param serverName
	 * @param serverURL
	 * @param serverType
	 * @return success if server removed
	 */
	public static boolean removeServer(String serverName, String serverURL, ServerType serverType) {
		GenericServer gServer = ServerList.getServer(serverURL);
		if (gServer == null) {
			System.out.println("Server " + serverName +" does not exist");
		} else {
			ServerList.removeServer(serverURL);
		}
		
		return true;
	}

	
	
	/**
	 * Discover all of the servers and genomes and versions.
	 */
	static void discoverServersAndSpeciesAndVersions() {
		for (GenericServer gServer : ServerList.getEnabledServers()) {
			discoverServer(gServer);
		}
	}


	static boolean discoverServer(GenericServer gServer) {
		try {
			if (gServer == null || gServer.serverType == ServerType.Unknown) {
				// should never happen
				return false;
			}
			if (gServer.serverType == ServerType.QuickLoad) {
				if (!getQuickLoadSpeciesAndVersions(gServer)) {
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
	private synchronized static boolean getDAS1SpeciesAndVersions(GenericServer gServer) {
		DasServerInfo server = (DasServerInfo) gServer.serverObj;
		if (server.getDataSources() == null || server.getDataSources().values() == null || server.getDataSources().values().isEmpty()) {
			System.out.println("WARNING: Couldn't find species for server: " + gServer);
			return false;
		}
		for (DasSource source : server.getDataSources().values()) {
			if (DEBUG) {
				System.out.println("source, version:" + source.getName() + "..." + source.getVersion() + "..." + source.getDescription() + "..." + source.getInfoUrl() + "..." + source.getID());
			}
			String speciesName = SPECIES_LOOKUP.getSpeciesName(source.getID());
			String versionName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), source.getID());
			String versionID = source.getID();
			GenericVersion gVersion = new GenericVersion(versionID, versionName, gServer, source);
			discoverVersion(versionName, gVersion, speciesName);
		}
		return true;
	}


	/**
	 * Discover genomes from DAS/2
	 * @param gServer
	 * @return false if there's an obvious problem
	 */
	private synchronized static boolean getDAS2SpeciesAndVersions(GenericServer gServer) {
		Das2ServerInfo server = (Das2ServerInfo) gServer.serverObj;
		if (server.getSources() == null || server.getSources().values() == null || server.getSources().values().isEmpty()) {
			System.out.println("WARNING: Couldn't find species for server: " + gServer);
			return false;
		}
		for (Das2Source source : server.getSources().values()) {
			String speciesName = SPECIES_LOOKUP.getSpeciesName(source.getName());
			
			// Das/2 has versioned sources.  Get each version.
			for (Das2VersionedSource versionSource : source.getVersions().values()) {
				String versionName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), versionSource.getName());
				String versionID = versionSource.getName();
				GenericVersion gVersion = new GenericVersion(versionID, versionName, gServer, versionSource);
				discoverVersion(versionName, gVersion, speciesName);
			}
		}
		return true;
	}

	/**
	 * Discover genomes from Quickload
	 * @param gServer
	 * @return false if there's an obvious failure.
	 */
	private synchronized static boolean getQuickLoadSpeciesAndVersions(GenericServer gServer) {
		URL quickloadURL = null;
		try {
			quickloadURL = new URL((String) gServer.serverObj);
		} catch (MalformedURLException ex) {
			Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(gmodel, quickloadURL);
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
			Set<GenericVersion> gVersions = gmodel.addSeqGroup(genomeName).getVersions();
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
			GenericVersion gVersion = new GenericVersion(genomeID, versionName, gServer, quickloadServer);
			discoverVersion(versionName, gVersion, speciesName);
		}
		return true;
	}

	/**
	 * An AnnotatedSeqGroup was added independently of the GeneralLoadUtils.
	 * Update GeneralLoadUtils state.
	 * @param aseq
	 * @return genome version
	 */
	GenericVersion getUnknownVersion(AnnotatedSeqGroup aseq) {
		String versionName = aseq.getID();
		String speciesName = "-- Unknown -- " + versionName;	// make it distinct, but also make it appear at the top of the species list.

		GenericServer gServer = new  GenericServer(null, null, ServerType.Unknown, null);
		GenericVersion gVersion = new GenericVersion(versionName, versionName, gServer, null);

		discoverVersion(versionName, gVersion, speciesName);

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
	private static void discoverVersion(String versionName, GenericVersion gVersion, String speciesName) {
		List<GenericVersion> gVersionList = getSpeciesVersionList(speciesName);
		if (!gVersionList.contains(gVersion)) {
			gVersionList.add(gVersion);
		}
		versionName2species.put(versionName, speciesName);
		AnnotatedSeqGroup group = gmodel.addSeqGroup(versionName); // returns existing group if found, otherwise creates a new group
		group.addVersion(gVersion);
		if (DEBUG) {
			System.out.println("Added " + gVersion.gServer.serverType + "genome: " + speciesName + " version: " + versionName);
		}
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
	static List<GenericFeature> getFeatures(final String versionName) {
		// There may be more than one server with the same versionName.  Merge all the version names.
		List<GenericFeature> featureList = new ArrayList<GenericFeature>();
		AnnotatedSeqGroup group = gmodel.getSeqGroup(versionName);
		if (group != null) {
			Set<GenericVersion> versions = group.getVersions();
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
	static void initVersionAndSeq(final String versionName) {
		if (versionName == null) {
			return;
		}
		AnnotatedSeqGroup group = gmodel.getSeqGroup(versionName);
		for (GenericVersion gVersion : group.getVersions()) {
			if (!gVersion.isInitialized()) {
				FeatureLoading.loadFeatureNames(gVersion);
				if (group.getSeqCount() == 0) {
					loadChromInfo(gVersion);
					addGenomeVirtualSeq(group, default_genome_min, DEBUG_VIRTUAL_GENOME);
				}
				gVersion.setInitialized();
			}
		}
	}



	/**
	 * Load the sequence info for the given genome version.
	 */
	private static AnnotatedSeqGroup loadChromInfo(GenericVersion gVersion) {
		AnnotatedSeqGroup group = gmodel.addSeqGroup(gVersion.versionName);

		if (DEBUG) {
			System.out.println("Discovering " + gVersion.gServer.serverType + " chromosomes");
		}
		if (gVersion.gServer.serverType == ServerType.DAS2) {

			// Discover chromosomes from DAS/2
			Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;

			version.getGenome();  // adds genome to singleton genometry model if not already present
			// Calling version.getSegments() to ensure that Das2VersionedSource is populated with Das2Region segments,
			//    which in turn ensures that AnnotatedSeqGroup is populated with SmartAnnotBioSeqs
			version.getSegments();
		}
		if (gVersion.gServer.serverType == ServerType.DAS) {
			// Discover chromosomes from DAS
			DasSource version = (DasSource) gVersion.versionSourceObj;

			version.getGenome();
			version.getEntryPoints();
		}

		return group;
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
			BioSeq chrom_seq = group.getSeq(i);
			if (chrom_seq == genome_seq) {
				continue;
			}
			addSeqToVirtualGenome(genome_seq, chrom_seq, default_genome_min, DEBUG_VIRTUAL_GENOME);
		}
	}

	/**
	 * Make sure virtual genome doesn't overflow int bounds.
	 * @param group
	 * @return true or false
	 */
	private static boolean isVirtualGenomeSmallEnough(AnnotatedSeqGroup group, int chrom_count) {
		double seq_bounds = 0.0;

		for (int i = 0; i < chrom_count; i++) {
			BioSeq chrom_seq = group.getSeq(i);
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

	private static void addSeqToVirtualGenome(BioSeq genome_seq, BioSeq chrom, double default_genome_min, boolean DEBUG_VIRTUAL_GENOME) {
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
	 * @return true or false
	 */
	static boolean loadAndDisplayAnnotations(GenericFeature gFeature, BioSeq cur_seq, FeaturesTableModel model) {
		BioSeq selected_seq = gmodel.getSelectedSeq();
		BioSeq visible_seq = gviewer.getViewSeq();
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
		} else if (gFeature.loadStrategy == LoadStrategy.GENOME || gFeature.loadStrategy == LoadStrategy.CHROMOSOME) {
			overlap = new SimpleSeqSpan(0, selected_seq.getLength(), selected_seq);
		} else {
			ErrorHandler.errorPanel("ERROR", "Requested load strategy not recognized: " + gFeature.loadStrategy);
			return false;
		}


		ServerType serverType = gFeature.gVersion.gServer.serverType;

		

		boolean result = false;

		Application.getSingleton().addNotLockedUpMsg("Loading feature " + gFeature.featureName);

		if (serverType == ServerType.DAS2) {
			result = loadDAS2Annotations(
							selected_seq,
							gFeature.featureName,
							(Das2VersionedSource) gFeature.gVersion.versionSourceObj,
							gviewer,
							visible_seq,
							overlap);
		} else if (serverType == ServerType.DAS) {
			result = DasFeatureLoader.loadFeatures(gFeature, overlap);
			Application.getSingleton().removeNotLockedUpMsg("Loading feature " + gFeature.featureName);
		} else if (serverType == ServerType.QuickLoad) {
			result = FeatureLoading.loadQuickLoadAnnotations(gFeature);
		} else {
			System.out.println("class " + serverType + " is not implemented.");
			Application.getSingleton().removeNotLockedUpMsg("Loading feature " + gFeature.featureName);
		}

		return result;
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
	 * @return true or false
	 */
	private static boolean loadDAS2Annotations(
					BioSeq selected_seq, final String feature_name, Das2VersionedSource version, SeqMapView gviewer, BioSeq visible_seq, SeqSpan overlap) {
		if (selected_seq == null) {
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

		FeatureLoading.processDas2FeatureRequests(requests, feature_name, true, gmodel, gviewer);
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


	static String getPreferredVersionName(Set<GenericVersion> gVersions) {
		return LOOKUP.getPreferredName(gVersions.iterator().next().versionName);
	}


}
