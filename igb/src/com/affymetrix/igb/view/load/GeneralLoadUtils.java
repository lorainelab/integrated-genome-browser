package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.MutableDoubleSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.comparator.StringVersionDateComparator;
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
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.genometryImpl.das.DasServerInfo;
import com.affymetrix.genometryImpl.das.DasSource;
import com.affymetrix.genometryImpl.das2.Das2ServerInfo;
import com.affymetrix.genometryImpl.das2.Das2Source;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.general.FeatureLoading;
import com.affymetrix.igb.general.ResidueLoading;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.featureloader.QuickLoad;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import com.affymetrix.genometryImpl.symloader.SymLoaderInst;
import com.affymetrix.igb.featureloader.Das;
import com.affymetrix.igb.featureloader.Das2;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.igb.util.ThreadHandler;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;

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
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @version $Id$
 */
public final class GeneralLoadUtils {

	private static final Pattern tab_regex = Pattern.compile("\t");
	/**
	 *  using negative start coord for virtual genome chrom because (at least for human genome)
	 *     whole genome start/end/length can't be represented with positive 4-byte ints (limit is +/- 2.1 billion)
	 */
//    final double default_genome_min = -2100200300;
	private static final double default_genome_min = -2100200300;

	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();

	// File name storing directory name associated with server on a cached server.
	public static final String SERVER_MAPPING = "/serverMapping.txt";

	/**
	 * Location of synonym file for correlating versions to species.
	 * The file lookup is done using {@link Class#getResourceAsStream(String)}.
	 * The default file is {@value}.
	 *
	 * @see #SPECIES_LOOKUP
	 */
	private static final String SPECIES_SYNONYM_FILE = "/species.txt";

	private static final double MAGIC_SPACER_NUMBER = 10.0;	// spacer factor used to keep genome spacing reasonable
	
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
	
	static {
		try {
			SpeciesLookup.load(GeneralLoadUtils.class.getResourceAsStream(SPECIES_SYNONYM_FILE));
		} catch (IOException ex) {
			Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(GeneralLoadUtils.class.getResourceAsStream(SPECIES_SYNONYM_FILE));
		}
	}

	/**
	 * Map to store directory name associated with the server on a cached server.
	 */
	private static Map<String, String> servermapping = new HashMap<String, String>();
	/**
	 * Add specified server, finding species and versions associated with it.
	 * @param serverName
	 * @param serverURL
	 * @param serverType
	 * @return success of server add.
	 */
	public static GenericServer addServer(ServerList serverList, ServerType serverType, String serverName, String serverURL) {
		/* should never happen */
		if (serverType == ServerType.LocalFiles) { return null; }
		
		GenericServer gServer = serverList.addServer(serverType, serverName, serverURL, true);
		if (gServer == null) {
			return null;
		}
		if (!discoverServer(gServer)) {
			gServer.setEnabled(false);
			//ServerList.removeServer(serverURL);
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
		server.setEnabled(false);
		if (server.serverType == null) {
			IGBServiceImpl.getInstance().repositoryRemoved(server.URL);
		}
	}

	
	public static boolean discoverServer(GenericServer gServer) {
		if (gServer.serverType == null) {
			Application.getSingleton().addNotLockedUpMsg("Loading repository " + gServer);
		} else {
			Application.getSingleton().addNotLockedUpMsg("Loading server " + gServer + " (" + gServer.serverType.toString() + ")");
		}

		if (gServer.serverType == null) {
			return IGBServiceImpl.getInstance().repositoryAdded(gServer.URL);
		}
		try {
			if (gServer == null || gServer.serverType == ServerType.LocalFiles) {
				// should never happen
				return false;
			}
			if (gServer.serverType == ServerType.QuickLoad) {
				if (!getQuickLoadSpeciesAndVersions(gServer)) {
					ServerList.getServerInstance().fireServerInitEvent(gServer, ServerStatus.NotResponding, false);
					return false;
				}
			} else if (gServer.serverType == ServerType.DAS) {
				if (!getDAS1SpeciesAndVersions(gServer)) {
					ServerList.getServerInstance().fireServerInitEvent(gServer, ServerStatus.NotResponding, false);
					return false;
				}
			} else if (gServer.serverType == ServerType.DAS2) {
				if (!getDAS2SpeciesAndVersions(gServer)) {
					ServerList.getServerInstance().fireServerInitEvent(gServer, ServerStatus.NotResponding, false);
					return false;
				}
			}
			ServerList.getServerInstance().fireServerInitEvent(gServer, ServerStatus.Initialized);
			if(gServer.serverType == ServerType.QuickLoad){
				loadWholeRangeFeatures(ServerType.QuickLoad);
			}
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
		GenericServer primaryServer = ServerList.getServerInstance().getPrimaryServer();
		URL primaryURL = getServerDirectory(gServer.URL);
		Map<String,DasSource> sources = server.getDataSources(primaryURL,primaryServer);
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			System.out.println("WARNING: Couldn't find species for server: " + gServer);
			return false;
		}
		for (DasSource source : sources.values()) {
			String speciesName = SpeciesLookup.getSpeciesName(source.getID());
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
		URL primaryURL = getServerDirectory(gServer.URL);
		GenericServer primaryServer = ServerList.getServerInstance().getPrimaryServer();
		Map<String,Das2Source> sources = server.getSources(primaryURL, primaryServer);
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			System.out.println("WARNING: Couldn't find species for server: " + gServer);
			return false;
		}
		for (Das2Source source : sources.values()) {
			String speciesName = SpeciesLookup.getSpeciesName(source.getName());
			
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
	private static boolean getQuickLoadSpeciesAndVersions(GenericServer gServer) {
		if(gServer.isPrimary())
			return true;

		URL quickloadURL = null;
		try {
			quickloadURL = new URL((String) gServer.serverObj);
		} catch (MalformedURLException ex) {
			Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		GenericServer primaryServer = ServerList.getServerInstance().getPrimaryServer();
		URL primaryURL = getServerDirectory(gServer.URL);
		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL, primaryURL, primaryServer);
		
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
				versionName = genomeName;
				speciesName = SpeciesLookup.getSpeciesName(genomeName);
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

		GenericServer server = ServerList.getServerInstance().getLocalFilesServer();
		
		return discoverVersion(versionName, versionName, server, null, speciesName);
	}

	/**
	 * An AnnotatedSeqGroup was added independently of the GeneralLoadUtils.
	 * Update GeneralLoadUtils state.
	 * @param seqgroup
	 * @return genome version
	 */
	public static GenericVersion getLocalFilesVersion(AnnotatedSeqGroup group, String speciesName) {
		String versionName = group.getID();
		if (speciesName == null) {
			 speciesName = "-- Unknown -- " + versionName;	// make it distinct, but also make it appear at the top of the species list
		}
		GenericServer server = ServerList.getServerInstance().getLocalFilesServer();

		for(GenericVersion gVersion : group.getEnabledVersions()){
			if(gVersion.gServer.serverType == ServerType.LocalFiles){
				return gVersion;
			}
		}
		
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
	public static List<GenericFeature> getFeatures(AnnotatedSeqGroup group) {
		// There may be more than one server with the same versionName.  Merge all the version names.
		List<GenericFeature> featureList = new ArrayList<GenericFeature>();
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

	/*
	 * Returns the list of features for currently selected group.
	 */
	public static List<GenericFeature> getSelectedVersionFeatures() {
		AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		return getFeatures(group);
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

		int spacer = determineSpacer(group, chrom_count);
		double seqBounds = determineSeqBounds(group, spacer, chrom_count);
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
			Logger.getLogger(GeneralLoadUtils.class.getName()).fine("Ignoring multithreading illegal state exception.");
			return;
		}

		for (int i=0;i<chrom_count;i++) {
			BioSeq chrom_seq = group.getSeq(i);
			if (chrom_seq == genome_seq) {
				continue;
			}

			// Add seq to virtual genome.  Keep values above 0 if possible.
			addSeqToVirtualGenome(seqBounds < 0 ? 0.0 : default_genome_min, spacer, genome_seq, chrom_seq);
		}
	}

	/**
	 * Determine size of spacer between chromosomes in whole genome view.
	 * @param group
	 * @param chrom_count
	 * @return
	 */
	private static int determineSpacer(AnnotatedSeqGroup group, int chrom_count) {
		double spacer = 0;
		for (BioSeq chrom_seq : group.getSeqList()) {
			spacer += (chrom_seq.getLengthDouble()) / chrom_count;
		}
		return (int)(spacer / MAGIC_SPACER_NUMBER);
	}


	/**
	 * Make sure virtual genome doesn't overflow integer bounds.
	 * @param group
	 * @return true or false
	 */
	private static double determineSeqBounds(AnnotatedSeqGroup group, int spacer, int chrom_count) {
		double seq_bounds = default_genome_min;

		for (int i = 0; i < chrom_count; i++) {
			BioSeq chrom_seq = group.getSeq(i);
			int clength = chrom_seq.getLength();
			seq_bounds += clength + spacer;
		}
		return seq_bounds;
	}

	private static void addSeqToVirtualGenome(double genome_min, int spacer, BioSeq genome_seq, BioSeq chrom) {
		double glength = genome_seq.getLengthDouble();
		int clength = chrom.getLength();
		double new_glength = glength + clength + spacer;

		genome_seq.setBoundsDouble(genome_min, genome_min + new_glength);
		
		MutableSeqSymmetry mapping = (MutableSeqSymmetry) genome_seq.getComposition();
		if (mapping == null) {
			mapping = new SimpleMutableSeqSymmetry();
			mapping.addSpan(new MutableDoubleSeqSpan(genome_min, genome_min + clength, genome_seq));
			genome_seq.setComposition(mapping);
		} else {
			MutableDoubleSeqSpan mspan = (MutableDoubleSeqSpan) mapping.getSpan(genome_seq);
			mspan.setDouble(genome_min, genome_min + new_glength, genome_seq);
		}

		MutableSeqSymmetry child = new SimpleMutableSeqSymmetry();
		// using doubles for coords, because may end up with coords > MAX_INT
		child.addSpan(new MutableDoubleSeqSpan(glength + genome_min, glength + genome_min + clength, genome_seq));
		child.addSpan(new MutableDoubleSeqSpan(0, clength, chrom));

		mapping.addChild(child);
	}

	protected static void bufferDataForAutoload(){
		SeqSpan visible = gviewer.getVisibleSpan();
		BioSeq seq = gmodel.getSelectedSeq();
		
		if(visible == null || seq == null)
			return;
		
		int length = visible.getLength();
		int min = visible.getMin();
		int max = visible.getMax();
		SeqSpan leftSpan = new SimpleSeqSpan(Math.max(0, min - length), min, seq);
		SeqSpan rightSpan = new SimpleSeqSpan(max, Math.min(seq.getLength(), max + length), seq);

		for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
			if (gFeature.getLoadStrategy() != LoadStrategy.AUTOLOAD) {
				continue;
			}

			if(checkBeforeLoading(gFeature)){
				loadAndDisplaySpan(leftSpan, gFeature);
				loadAndDisplaySpan(rightSpan, gFeature);
			}
		}
	}

	/**
	 * Load any features that have a whole strategy and haven't already been loaded.
	 * @param versionName
	 */
	static void loadWholeRangeFeatures(ServerType serverType) {
		List<LoadStrategy> loadStrategies = new ArrayList<LoadStrategy>();
		loadStrategies.add(LoadStrategy.GENOME);
		loadFeatures(loadStrategies, serverType);
	}

	static void loadFeatures(List<LoadStrategy> loadStrategies, ServerType serverType){
		for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
			if (!loadStrategies.contains(gFeature.getLoadStrategy())) {
				continue;
			}
			if(serverType != null && gFeature.gVersion.gServer.serverType != serverType){
				continue;
			}

			GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
		}
	}

	private static boolean checkBeforeLoading(GenericFeature gFeature){
		if (gFeature.getLoadStrategy() == LoadStrategy.NO_LOAD) {
			return false;	// should never happen
		}

//		Thread may have been cancelled. So removing test for now.
//		//Already loaded the data.
//		if((gFeature.gVersion.gServer.serverType == ServerType.LocalFiles)
//				&& ((QuickLoad)gFeature.symL).getSymLoader() instanceof SymLoaderInstNC){
//			return false;
//		}

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

		return true;
	}
	
	/**
	 * Load and display annotations (requested for the specific feature).
	 * Adjust the load status accordingly.
	 * @param gFeature
	 * @return true or false
	 */
	static public void loadAndDisplayAnnotations(GenericFeature gFeature) {
		if(!checkBeforeLoading(gFeature))
			return;

		BioSeq selected_seq = gmodel.getSelectedSeq();
		SeqSpan overlap = null;
		if(gFeature.getLoadStrategy() == LoadStrategy.AUTOLOAD && !gviewer.getAutoLoad().shouldAutoLoad()){
			return;
		}
		if (gFeature.getLoadStrategy() == LoadStrategy.VISIBLE || gFeature.getLoadStrategy() == LoadStrategy.AUTOLOAD) {
			overlap = gviewer.getVisibleSpan();
		} else if (gFeature.getLoadStrategy() == LoadStrategy.GENOME || gFeature.getLoadStrategy() == LoadStrategy.CHROMOSOME) {
			// TODO: Investigate edge case at max
			overlap = new SimpleSeqSpan(selected_seq.getMin(), selected_seq.getMax()-1, selected_seq);
		}
		
		loadAndDisplaySpan(overlap, gFeature);
	}

	public static void loadAndDisplaySpan(final SeqSpan span, final GenericFeature feature) {
		SeqSymmetry optimized_sym = null;
		// special-case chp files, due to their LazyChpSym DAS/2 loading
		if ((feature.gVersion.gServer.serverType == ServerType.QuickLoad || feature.gVersion.gServer.serverType == ServerType.LocalFiles) && ((QuickLoad) feature.symL).extension.endsWith(".chp")) {
			feature.setLoadStrategy(LoadStrategy.GENOME);	// it should be set to this already.  But just in case...
			optimized_sym = new SimpleMutableSeqSymmetry();
			((SimpleMutableSeqSymmetry)optimized_sym).addSpan(span);
			loadFeaturesForSym(optimized_sym, feature);
			return;
		}

		optimized_sym = feature.optimizeRequest(span);

		if (feature.getLoadStrategy() != LoadStrategy.GENOME || feature.gVersion.gServer.serverType == ServerType.DAS2) {
			// Don't iterate for DAS/2.  "Genome" there is used for autoloading.

			loadFeaturesForSym(optimized_sym, feature);
			return;
		}
		
		//If Loading whole genome for unoptimized file then load everything at once.
		if ((feature.gVersion.gServer.serverType == ServerType.QuickLoad || feature.gVersion.gServer.serverType == ServerType.LocalFiles)
				&& ((QuickLoad) feature.symL).getSymLoader() instanceof SymLoaderInst) {

			if (optimized_sym != null) {
				((QuickLoad) feature.symL).loadAllSymmetriesThread(feature);
			}
			return;
		}

		iterateSeqList(feature);

	}

	private static void iterateSeqList(final GenericFeature feature) {
		final BioSeq current_seq = gmodel.getSelectedSeq();
		CThreadWorker worker = new CThreadWorker("Loading whole feature " + feature.featureName) {

			@Override
			protected Object runInBackground() {
				Thread thread = Thread.currentThread();

				BioSeq seq = null;
				if (current_seq != null) {
					loadOnSequence(current_seq);
				}

				AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
				for (int i = 0; i < group.getSeqCount(); i++) {
					seq = group.getSeq(i);
					
					if(seq == current_seq){
						continue;
					}
					
					if (thread.isInterrupted()) {
						break;
					}
					loadOnSequence(seq);
				}
				return null;
			}

			@Override
			protected void finished() {
				if(isCancelled()){
					feature.setLoadStrategy(LoadStrategy.NO_LOAD);
					GeneralLoadView.feature_model.fireTableDataChanged();
				}
				
				BioSeq seq = gmodel.getSelectedSeq();
				if (seq != null) {
					gviewer.setAnnotatedSeq(seq, true, true);
				} else if (gmodel.getSelectedSeqGroup().getSeqCount() > 0) {
					// This can happen when loading a brand-new genome
					gmodel.setSelectedSeq(gmodel.getSelectedSeqGroup().getSeq(0));
				}
			}
			
			private void loadOnSequence(BioSeq seq) {
				if (IGBConstants.GENOME_SEQ_ID.equals(seq.getID())) {
					return; // don't load into Whole Genome
				}
				
				try {
					SeqSymmetry optimized_sym = feature.optimizeRequest(new SimpleSeqSpan(seq.getMin(), seq.getMax() - 1, seq));
					if (optimized_sym != null) {
						loadFeaturesForSym(feature, optimized_sym);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};

		ThreadHandler.getThreadHandler().execute(feature, worker);
	}
	
	private static void loadFeaturesForSym(final SeqSymmetry optimized_sym, final GenericFeature feature) throws OutOfMemoryError {
		if (optimized_sym == null) {
			Logger.getLogger(GeneralLoadUtils.class.getName()).log(
					Level.INFO, "All of new query covered by previous queries for feature {0}", feature.featureName);
			return;
		}
		
		final int seq_count = gmodel.getSelectedSeqGroup().getSeqCount();
		final CThreadWorker worker = new CThreadWorker("Loading feature " + feature.featureName) {

			@Override
			protected Object runInBackground() {
				try{
					loadFeaturesForSym(feature, optimized_sym);
					TrackView.updateDependentData();
				}catch(Exception ex){
					ex.printStackTrace();
				}
				return null;
			}

			@Override
			protected void finished() {
				BioSeq aseq = gmodel.getSelectedSeq();

				if (aseq != null) {
					gviewer.setAnnotatedSeq(aseq, true, true);
				} else if(gmodel.getSelectedSeqGroup().getSeqCount() > 0){
					// This can happen when loading a brand-new genome
					gmodel.setSelectedSeq(gmodel.getSelectedSeqGroup().getSeq(0));
				}

				//Since sequence are never removed so if no. of sequence increases then refresh sequence table.
				if (gmodel.getSelectedSeqGroup().getSeqCount() > seq_count) {
					SeqGroupView.getInstance().refreshTable();
				}
			}
		};

		ThreadHandler.getThreadHandler().execute(feature, worker);
	}
	
	private static void loadFeaturesForSym(GenericFeature feature, SeqSymmetry optimized_sym) throws OutOfMemoryError, IOException {
		List<SeqSpan> optimized_spans = new ArrayList<SeqSpan>();
		List<SeqSpan> spans = new ArrayList<SeqSpan>();
		convertSymToSpanList(optimized_sym, spans);
		optimized_spans.addAll(spans);
		Thread thread = Thread.currentThread();
		
		switch (feature.gVersion.gServer.serverType) {
			case DAS2:
				for (SeqSpan optimized_span : optimized_spans) {
					feature.addLoadingSpanRequest(optimized_span);	// this span is requested to be loaded.
					Das2.loadFeatures(optimized_span, feature);
					if(thread.isInterrupted()){
						break;
					}
				}
				break;

			case DAS:
				for (SeqSpan optimized_span : optimized_spans) {
					feature.addLoadingSpanRequest(optimized_span);	// this span is requested to be loaded.
					Das.loadFeatures(optimized_span, feature);
					if(thread.isInterrupted()){
						break;
					}
				}
				break;

			case QuickLoad:
			case LocalFiles:
				for (SeqSpan optimized_span : optimized_spans) {
					feature.addLoadingSpanRequest(optimized_span);	// this span is requested to be loaded.
					((QuickLoad) feature.symL).loadFeatures(optimized_span, feature);
					if(thread.isInterrupted()){
						break;
					}
				}
				break;
		}
	}

	private static boolean checkBamLoading(GenericFeature feature, SeqSymmetry optimized_sym) {
		//start max
		boolean bamCheck = GeneralLoadView.getLoadView().isLoadingConfirm();
		GeneralLoadView.getLoadView().setShowLoadingConfirm(false);
		if (optimized_sym!= null && feature.getExtension() != null && feature.getExtension().endsWith("bam") && bamCheck) {
			String message = "Region in view is big (> 100k), do you want to continue?";
			int childrenCount = optimized_sym.getChildCount();
			int spanWidth = 0;
			for (int childIndex = 0; childIndex < childrenCount; childIndex++) {
				SeqSymmetry child = optimized_sym.getChild(childIndex);
				for (int spanIndex = 0; spanIndex < child.getSpanCount(); spanIndex++) {
					spanWidth = spanWidth + (child.getSpan(spanIndex).getMax() - child.getSpan(spanIndex).getMin());
				}
			}
			if (spanWidth > 100000){
//				return !(Application.confirmPanel(message, PreferenceUtils.getTopNode(),
//					PreferenceUtils.CONFIRM_BEFORE_LOAD, PreferenceUtils.default_confirm_before_load));
			}
		
		}
		return false;
		//end max
	}

	/**
	 * Walk the SeqSymmetry, converting all of its children into spans.
	 * @param sym the SeqSymmetry to walk.
	 */
	private static void convertSymToSpanList(SeqSymmetry sym, List<SeqSpan> spans) {
		int childCount = sym.getChildCount();
		if (childCount > 0) {
			for (int i = 0; i < childCount; i++) {
				convertSymToSpanList(sym.getChild(i), spans);
			}
		} else {
			int spanCount = sym.getSpanCount();
			for (int i = 0; i < spanCount; i++) {
				spans.add(sym.getSpan(i));
			}
		}
	}


	/**
	 * Load residues on span.
	 * First, attempt to load them with DAS/2 servers.
	 * Second, attempt to load them with QuickLoad servers.
	 * Third, attempt to load them with DAS/1 servers.
	 * @param aseq
	 * @param span	-- may be null, if the entire sequence is requested.
	 * @return true if succeeded.
	 */
	static boolean loadResidues(String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span) {
	
		/*
		 * This test does not work properly, so it's being commented out for now.
		 *
		if (aseq.isComplete()) {
			if (DEBUG) {
				System.out.println("already have residues for " + seq_name);
			}
			return false;
		}*/

		// Determine list of servers that might have this chromosome sequence.
		Set<GenericVersion> versionsWithChrom = new HashSet<GenericVersion>();
		versionsWithChrom.addAll(aseq.getSeqGroup().getEnabledVersions());

		if ((min <= 0) && (max >= aseq.getLength())) {
			min = 0;
			max = aseq.getLength();
		}

		if(aseq.isAvailable(min, max)){
			Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.INFO,
					"All residues in range are already loaded on sequence {0}",new Object[]{aseq});
			return true;
		}

		Application.getSingleton().addNotLockedUpMsg("Loading residues for "+aseq.getID());
		
		return ResidueLoading.getResidues(versionsWithChrom, genomeVersionName, aseq, min, max, span);
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
		synonymBuilder.append("<html>").append(IGBConstants.BUNDLE.getString("synonymList"));
		Set<String> synonymSet = LOOKUP.getSynonyms(versionName);
		for (String synonym : synonymSet) {
			if (synonym.equalsIgnoreCase(versionName)) {
				continue;
			}
			synonymBuilder.append("<p>").append(synonym).append("</p>");
		}
		if (synonymSet.size() <= 1) {
			synonymBuilder.append(IGBConstants.BUNDLE.getString("noSynonyms"));
		}
		synonymBuilder.append("</html>");
		return synonymBuilder.toString();
	}
	
	/**
	 * Method to load server directory mapping.
	 */
	public static void loadServerMapping() {
		GenericServer primaryServer = ServerList.getServerInstance().getPrimaryServer();
		if (primaryServer == null) {
			return;
		}
		InputStream istr = null;
		InputStreamReader ireader = null;
		BufferedReader br = null;

		try {
			try {
				istr = LocalUrlCacher.getInputStream(primaryServer.friendlyURL.toExternalForm() + SERVER_MAPPING);
			} catch (Exception e) {
				Logger.getLogger(GeneralLoadUtils.class.getName()).log(
						Level.SEVERE,"Couldn''t open ''{0}" + SERVER_MAPPING + "\n:  {1}", new Object[]{primaryServer.friendlyURL.toExternalForm(), e.toString()});
				istr = null; // dealt with below
			}
			if (istr == null) {
				Logger.getLogger(GeneralLoadUtils.class.getName()).log(
						Level.INFO,"Could not load server mapping contents from\n{0}" + SERVER_MAPPING, primaryServer.friendlyURL.toExternalForm());
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
				if (fields.length >= 2) {
					String serverURL = fields[0];
					String dirURL = primaryServer.URL + fields[1];
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

	/**
	 * Get directory url on cached server from servermapping map.
	 * @param url	URL of the server.
	 * @return	Returns a directory if exists else null.
	 */
	public static URL getServerDirectory(String url){
		if (ServerList.getServerInstance().getPrimaryServer() == null) {
			return null;
		}
		
		for(Entry<String, String> primary : servermapping.entrySet()){
			if(url.equals(primary.getKey())){
				try {
					return new URL(primary.getValue());
				} catch (MalformedURLException ex) {
					Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.SEVERE, null, ex);
					return null;
				}
			}
		}
		
		return null;
	}

	/**
	 * Set autoload variable in features.
	 * @param autoload	
	 */
	public static void setFeatureAutoLoad(boolean autoload){
		for(List<GenericVersion> genericVersions : species2genericVersionList.values()){
			for(GenericVersion genericVersion : genericVersions){
				for(GenericFeature genericFeature : genericVersion.getFeatures()){
					genericFeature.setAutoload(autoload);
				}
			}
		}

		//It autoload data is selected then load.
		if(autoload){
			gmodel.setSelectedSeq(gmodel.getSelectedSeq());
		}
	}

	public static List<String> getSpeciesList(){
		final List<String> speciesList = new ArrayList<String>();
		speciesList.addAll(species2genericVersionList.keySet());
		Collections.sort(speciesList);
		return speciesList;
	}

	public static List<String> getGenericVersions(final String speciesName){
		final List<GenericVersion> versionList = species2genericVersionList.get(speciesName);
		final List<String> versionNames = new ArrayList<String>();
		if (versionList != null) {
			for (GenericVersion gVersion : versionList) {
				// the same versionName name may occur on multiple servers
				String versionName = gVersion.versionName;
				if (!versionNames.contains(versionName)) {
					versionNames.add(versionName);
				}
			}
			Collections.sort(versionNames, new StringVersionDateComparator());
		}
		return versionNames;
	}
}
