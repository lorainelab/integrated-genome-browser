package com.affymetrix.igb.view.load;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.comparator.StringVersionDateComparator;
import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.Bprobe1Parser;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.parsers.useq.ArchiveInfo;
import com.affymetrix.genometryImpl.parsers.useq.USeqGraphParser;
import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoader;
import com.affymetrix.genometryImpl.span.MutableDoubleSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symloader.BAM.BamIndexNotFoundException;
import com.affymetrix.genometryImpl.symloader.ResidueTrackSymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderInst;
import com.affymetrix.genometryImpl.symloader.SymLoaderInstNC;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleSymWithResidues;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.BioSeqUtils;
import static com.affymetrix.genometryImpl.util.Constants.SERVER_MAPPING;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.RefreshStatus;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.SynonymLookupServiceI;
import com.affymetrix.genometryImpl.util.Timer;
import com.affymetrix.genometryImpl.util.VersionDiscoverer;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.parsers.QuickLoadSymLoaderChp;
import com.affymetrix.igb.parsers.XmlPrefsParser;
import com.affymetrix.igb.prefs.TierPrefsView;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.util.IGBAuthenticator;
import com.affymetrix.igb.view.SeqGroupViewI;
import com.affymetrix.igb.view.SeqMapViewI;
import com.affymetrix.igb.view.TrackView;
import static com.affymetrix.igb.view.load.FileExtensionContants.BAM_EXT;
import static com.affymetrix.igb.view.load.FileExtensionContants.BAR_EXT;
import static com.affymetrix.igb.view.load.FileExtensionContants.BP1_EXT;
import static com.affymetrix.igb.view.load.FileExtensionContants.BP2_EXT;
import static com.affymetrix.igb.view.load.FileExtensionContants.USEQ_EXT;
import com.google.common.collect.Ordering;
import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @version $Id: GeneralLoadUtils.java 11492 2012-05-10 18:17:28Z hiralv $
 */
@Component(name = GeneralLoadUtils.COMPONENT_NAME, provide = GeneralLoadUtilsService.class)
public final class GeneralLoadUtils implements GeneralLoadUtilsService {

    public static final String COMPONENT_NAME = "GeneralLoadUtils";
    private static final Logger logger = LoggerFactory.getLogger(GeneralLoadUtils.class);
    private static final int MAX_INTERNAL_THREAD = Runtime.getRuntime().availableProcessors() + 1;
    private static final Pattern tab_regex = Pattern.compile("\t");
    /**
     * using negative start coord for virtual genome chrom because (at least for
     * human genome) whole genome start/end/length can't be represented with
     * positive 4-byte ints (limit is +/- 2.1 billion)
     */
//    final double default_genome_min = -2100200300;
    private static final double default_genome_min = -2100200300;

    private static final String LOADING_FEATURE_MESSAGE = IGBConstants.BUNDLE.getString("loadFeature");
    private static final String LOADING_MESSAGE_PREFIX = "Loading data set ";
    private static final String SPECIES_SYNONYM_FILE = "/species.txt";
    private static final double MAGIC_SPACER_NUMBER = 10.0;	// spacer factor used to keep genome spacing reasonable

    private GenometryModel gmodel;
    private SeqGroupViewI seqGroupView;
    private SeqMapViewI gviewer;
    private VersionDiscoverer versionDiscoverer;
    private SynonymLookupServiceI synonymLookupService;
    private GeneralLoadView generalLoadView;
    private IGBService igbService;
    /**
     * Map to store directory name associated with the server on a cached
     * server.
     */
    private Map<String, String> servermapping;

    /**
     *
     */
//	private static RegionFinder regionFinder = new DefaultRegionFinder();
    private final GenericServerInitListener genericServerInitListener = new GenericServerInitListener() {

        @Override
        public void genericServerInit(GenericServerInitEvent evt) {
            GenericServer server = (GenericServer) evt.getSource();
            if (server.getServerStatus() == ServerStatus.NotResponding) {
                removeServer(server);
            }
        }

    };

    @Activate
    public void activate() {
        gmodel = GenometryModel.getInstance();
        synonymLookupService = SynonymLookup.getDefaultLookup();
        servermapping = new HashMap<>();
        try {
            SpeciesLookup.load(GeneralLoadUtils.class.getResourceAsStream(SPECIES_SYNONYM_FILE));
        } catch (IOException ex) {
            logger.error("Error retrieving Species synonym file", ex);
        } finally {
            GeneralUtils.safeClose(GeneralLoadUtils.class.getResourceAsStream(SPECIES_SYNONYM_FILE));
        }
        ServerList.getServerInstance().addServerInitListener(genericServerInitListener);
    }

    /**
     * Add specified server, finding species and versions associated with it.
     *
     * @param serverName
     * @param serverURL
     * @param serverType
     * @return success of server add.
     */
    @Override
    public GenericServer addServer(ServerList serverList, ServerTypeI serverType,
            String serverName, String serverURL, int order, boolean isDefault, String mirrorURL) {
        /*
         * should never happen
         */
        if (serverType == ServerTypeI.LocalFiles) {
            return null;
        }

        GenericServer gServer = serverList.addServer(serverType, serverName,
                serverURL, true, false, order, isDefault, mirrorURL);

        if (gServer == null) {
            return null;
        }

        if (!discoverServer(gServer)) {
            return null;
        }
        return gServer;
    }

    private void removeServer(GenericServer server) {
        Iterator<Map.Entry<String, GenericVersion>> i = versionDiscoverer.getSpecies2Generic().entries().iterator();
        while (i.hasNext()) {
            GenericVersion version = i.next().getValue();
            if (version.gServer == server) {
                removeAllFeautres(version.getFeatures());
                version.clear();
                i.remove();
            }
        }

        server.setEnabled(false);
        if (server.serverType == null) {
            IGBServiceImpl.getInstance().getRepositoryChangerHolder().repositoryRemoved(server.URL);
        }
    }

    @Override
    public boolean discoverServer(GenericServer gServer) {
        if (gServer.isPrimary()) {
            return true;
        }
        if (gServer.serverType == null) { // bundle repository
            ServerList.getRepositoryInstance().fireServerInitEvent(gServer, ServerStatus.Initialized, true);
            return true;
        }

        try {
            if (gServer == null || gServer.serverType == ServerTypeI.LocalFiles) {
                // should never happen
                return false;
            }
            if (gServer.serverType != null) {
                //tKanapar
                if (!LocalUrlCacher.isValidURL(gServer.URL)) {//Adding check on the request if authentication is required
                    if (IGBAuthenticator.authenticationRequestCancelled()) {//If the cancel dialog is clicked in the IGB Authenticator
                        IGBAuthenticator.resetAuthenticationRequestCancelled();//Reset the cancel for future use
                        ServerList.getServerInstance().removeServer(gServer.URL);//Remove the preference so that it wont add the server to list
                        ServerList.getServerInstance().removeServerFromPrefs(gServer.URL);
                        return false;
                    }
                }

                GenericServer primaryServer = ServerList.getServerInstance().getPrimaryServer();
                URL primaryURL = getServerDirectory(gServer.URL);

                if (!gServer.serverType.getSpeciesAndVersions(gServer, primaryServer, primaryURL, versionDiscoverer)) {

                    /**
                     * qlmirror - Quickload Mirror Server
                     *
                     * All related changes can be searched by 'qlmirror'
                     *
                     * The following code will try to use mirror server when
                     * server is being discovered e.g. IGB startup
                     *
                     * Mirror server address is specified in
                     * igb_defaults_prefs.xml by 'mirror' attribute
                     *
                     */
                    if (gServer.useMirrorSite()) {
//
                        // Change serverObj for Quickload to apply mirror site
                        // Currently only Quickload has mirror
                        if (gServer.serverType == ServerTypeI.QuickLoad) {
                            logger.info("Using mirror site: {}", gServer.mirrorURL);
                            gServer.serverObj = gServer.mirrorURL;
//							ServerList.getServerInstance().fireServerInitEvent(gServer, LoadUtils.ServerStatus.NotInitialized);
                            discoverServer(gServer);
                        } else {
                            ServerList.getServerInstance().fireServerInitEvent(gServer, ServerStatus.NotResponding, false);
                            gServer.setEnabled(false);
                            return false;
                        }
                    } else { // Disable server if no mirror or not used
                        ServerList.getServerInstance().fireServerInitEvent(gServer, ServerStatus.NotResponding, false);
                        gServer.setEnabled(false);
                        return false;
                    }
                } else {
                    Application.getSingleton().addNotLockedUpMsg("Loading server " + gServer + " (" + gServer.serverType.toString() + ")");
                }
                if (gServer.serverType == ServerTypeI.QuickLoad) {
                    XmlPrefsParser.parse(gServer.serverObj.toString() + "preferences.xml"); // Use server object for Quickload
                }
            }
            ServerList.getServerInstance().fireServerInitEvent(gServer, ServerStatus.Initialized, true);
        } catch (IllegalStateException ex) {
            ServerList.getServerInstance().fireServerInitEvent(gServer, ServerStatus.NotResponding, false);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    /**
     * An AnnotatedSeqGroup was added independently of the GeneralLoadUtils.
     * Update GeneralLoadUtils state.
     *
     * @param aseq
     * @return genome version
     */
    @Override
    public GenericVersion getUnknownVersion(AnnotatedSeqGroup aseq) {
        String versionName = aseq.getID();
        String speciesName = "-- Unknown -- " + versionName;	// make it distinct, but also make it appear at the top of the species list.

        GenericServer server = ServerList.getServerInstance().getLocalFilesServer();

        return versionDiscoverer.discoverVersion(versionName, versionName, server, null, speciesName);
    }

    @Override
    public GenericVersion getIGBFilesVersion(AnnotatedSeqGroup group, String speciesName) {
        return getXFilesVersion(ServerList.getServerInstance().getIGBFilesServer(), group, speciesName);
    }

    /**
     * An AnnotatedSeqGroup was added independently of the GeneralLoadUtils.
     * Update GeneralLoadUtils state.
     *
     * @return genome version
     */
    @Override
    public GenericVersion getLocalFilesVersion(AnnotatedSeqGroup group, String speciesName) {
        return getXFilesVersion(ServerList.getServerInstance().getLocalFilesServer(), group, speciesName);
    }

    private GenericVersion getXFilesVersion(GenericServer server, AnnotatedSeqGroup group, String speciesName) {
        String versionName = group.getID();
        if (speciesName == null) {
            speciesName = "-- Unknown -- " + versionName;	// make it distinct, but also make it appear at the top of the species list
        }

        for (GenericVersion gVersion : group.getEnabledVersions()) {
            if (gVersion.gServer == server) {
                return gVersion;
            }
        }

        return versionDiscoverer.discoverVersion(versionName, versionName, server, null, speciesName);
    }

    /**
     * Returns the list of features for the genome with the given version name.
     * The list may (rarely) be empty, but never null.
     */
    @Override
    public List<GenericFeature> getFeatures(AnnotatedSeqGroup group) {
        // There may be more than one server with the same versionName.  Merge all the version names.
        List<GenericFeature> featureList = new ArrayList<>();
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
     * Only want to display features with visible attribute set to true.
     *
     * @return list of visible features
     */
    @Override
    public List<GenericFeature> getVisibleFeatures() {
        List<GenericFeature> visibleFeatures = new ArrayList<>();
        AnnotatedSeqGroup group = GenometryModel.getInstance().getSelectedSeqGroup();

        for (GenericFeature gFeature : getFeatures(group)) {
            if (gFeature.isVisible() && !gFeature.isReferenceSequence()) {
                visibleFeatures.add(gFeature);
            }
        }

        return visibleFeatures;
    }

    /*
     * Returns the list of features for currently selected group.
     */
    @Override
    public List<GenericFeature> getSelectedVersionFeatures() {
        AnnotatedSeqGroup group = GenometryModel.getInstance().getSelectedSeqGroup();
        return getFeatures(group);
    }

    /**
     * Returns the list of servers associated with the given versions.
     *
     * @param features -- assumed to be non-null.
     * @return A list of servers associated with the given versions.
     */
    @Override
    public List<GenericServer> getServersWithAssociatedFeatures(List<GenericFeature> features) {
        List<GenericServer> serverList = new ArrayList<>();
        for (GenericFeature gFeature : features) {
            if (!serverList.contains(gFeature.gVersion.gServer)) {
                serverList.add(gFeature.gVersion.gServer);
            }
        }
        // make sure these servers always have the same order
        Collections.sort(serverList, ServerList.getServerInstance().getServerOrderComparator());
        return serverList;
    }

    /**
     * Load the annotations for the given version. This is specific to one
     * server.
     *
     * @param gVersion
     */
    @Override
    public void loadFeatureNames(final GenericVersion gVersion) {
        boolean autoload = PreferenceUtils.getBooleanParam(
                PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load);
        if (!gVersion.getFeatures().isEmpty()) {
            logger.debug("Feature names are already loaded.");
            return;
        }
        if (gVersion.gServer.serverType == null) {
            logger.warn("WARNING: Unknown server class " + gVersion.gServer.serverType);
        } else {
            gVersion.gServer.serverType.discoverFeatures(gVersion, autoload);
        }
    }

    /**
     * Make sure this genome version has been initialized.
     *
     * @param versionName
     */
    @Override
    public void initVersionAndSeq(final String versionName) {
        if (versionName == null) {
            return;
        }
        AnnotatedSeqGroup group = gmodel.getSeqGroup(versionName);
        for (GenericVersion gVersion : group.getEnabledVersions()) {
            if (!gVersion.isInitialized()) {
                loadFeatureNames(gVersion);
                gVersion.setInitialized();
            }
        }
        if (group.getSeqCount() == 0) {
            loadChromInfo(group);
        }
        addGenomeVirtualSeq(group);	// okay to run this multiple times
    }

    /**
     * Load the sequence info for the given group. Try loading from DAS/2 before
     * loading from DAS; chances are DAS/2 will be faster, and that the
     * chromosome names will be closer to what is expected.
     */
    @Override
    public void loadChromInfo(AnnotatedSeqGroup group) {
        for (ServerTypeI serverType : ServerUtils.getServerTypes()) {
            for (GenericVersion gVersion : group.getEnabledVersions()) {
                if (gVersion.gServer.serverType != serverType) {
                    continue;
                }
                serverType.discoverChromosomes(gVersion.versionSourceObj);
                return;
            }
        }
    }

    @Override
    public void addGenomeVirtualSeq(AnnotatedSeqGroup group) {
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
            logger.trace("Ignoring multithreading illegal state exception.");
            return;
        }

        for (int i = 0; i < chrom_count; i++) {
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
     */
    private int determineSpacer(AnnotatedSeqGroup group, int chrom_count) {
        double spacer = 0;
        for (BioSeq chrom_seq : group.getSeqList()) {
            spacer += (chrom_seq.getLengthDouble()) / chrom_count;
        }
        return (int) (spacer / MAGIC_SPACER_NUMBER);
    }

    /**
     * Make sure virtual genome doesn't overflow integer bounds.
     *
     * @param group
     * @return true or false
     */
    private double determineSeqBounds(AnnotatedSeqGroup group, int spacer, int chrom_count) {
        double seq_bounds = default_genome_min;

        for (int i = 0; i < chrom_count; i++) {
            BioSeq chrom_seq = group.getSeq(i);
            int clength = chrom_seq.getLength();
            seq_bounds += clength + spacer;
        }
        return seq_bounds;
    }

    private void addSeqToVirtualGenome(double genome_min, int spacer, BioSeq genome_seq, BioSeq chrom) {
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

    @Override
    public void bufferDataForAutoload() {
        SeqSpan visible = gviewer.getVisibleSpan();
        BioSeq seq = gmodel.getSelectedSeq();

        if (visible == null || seq == null) {
            return;
        }

        int length = visible.getLength();
        int min = visible.getMin();
        int max = visible.getMax();
        SeqSpan leftSpan = new SimpleSeqSpan(Math.max(0, min - length), min, seq);
        SeqSpan rightSpan = new SimpleSeqSpan(max, Math.min(seq.getLength(), max + length), seq);

        for (GenericFeature gFeature : getSelectedVersionFeatures()) {
            if (gFeature.getLoadStrategy() != LoadStrategy.AUTOLOAD) {
                continue;
            }

            if (checkBeforeLoading(gFeature)) {
                loadAndDisplaySpan(leftSpan, gFeature);
                loadAndDisplaySpan(rightSpan, gFeature);
            }
        }
    }

    private boolean checkBeforeLoading(GenericFeature gFeature) {
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
        if ((selected_seq == null || visible_seq == null) && (gFeature.gVersion.gServer.serverType != ServerTypeI.LocalFiles)) {
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
     * Load and display annotations (requested for the specific feature). Adjust
     * the load status accordingly.
     */
    @Override
    public void loadAndDisplayAnnotations(GenericFeature gFeature) {
        if (!checkBeforeLoading(gFeature)) {
            return;
        }

        BioSeq selected_seq = gmodel.getSelectedSeq();
        if (selected_seq == null) {
            ErrorHandler.errorPanel("Couldn't find genome data on server for file, genome = " + gFeature.gVersion.group.getID());
            return;
        }
        SeqSpan overlap = null;
        if (gFeature.getLoadStrategy() == LoadStrategy.VISIBLE || gFeature.getLoadStrategy() == LoadStrategy.AUTOLOAD) {
            overlap = gviewer.getVisibleSpan();
            // TODO: Investigate edge case at max
            if (overlap.getMin() == selected_seq.getMin() && overlap.getMax() == selected_seq.getMax()) {
                overlap = new SimpleSeqSpan(selected_seq.getMin(), selected_seq.getMax() - 1, selected_seq);
            }
        } else if (gFeature.getLoadStrategy() == LoadStrategy.GENOME /*|| gFeature.getLoadStrategy() == LoadStrategy.CHROMOSOME*/) {
            // TODO: Investigate edge case at max
            overlap = new SimpleSeqSpan(selected_seq.getMin(), selected_seq.getMax() - 1, selected_seq);
        }

        loadAndDisplaySpan(overlap, gFeature);
    }

    @Override
    public void loadAndDisplaySpan(final SeqSpan span, final GenericFeature feature) {
        SeqSymmetry optimized_sym = null;
        // special-case chp files, due to their LazyChpSym DAS/2 loading
        if ((feature.gVersion.gServer.serverType == ServerTypeI.QuickLoad || feature.gVersion.gServer.serverType == ServerTypeI.LocalFiles)
                && ((QuickLoadSymLoader) feature.symL).extension.endsWith("chp")) {
            feature.setLoadStrategy(LoadStrategy.GENOME);	// it should be set to this already.  But just in case...
            optimized_sym = new SimpleMutableSeqSymmetry();
            ((SimpleMutableSeqSymmetry) optimized_sym).addSpan(span);
            loadFeaturesForSym(optimized_sym, feature);
            return;
        }

        optimized_sym = feature.optimizeRequest(span);

        if (feature.getLoadStrategy() != LoadStrategy.GENOME
                || feature.gVersion.gServer.serverType == ServerTypeI.DAS2
                || feature.gVersion.gServer.serverType == ServerTypeI.DAS) {
            // Don't iterate for DAS/2.  "Genome" there is used for autoloading.

            if (checkBamAndSamLoading(feature, optimized_sym)) {
                return;
            }

            loadFeaturesForSym(optimized_sym, feature);
            return;
        }

        //Since Das1 does not have whole genome return if it is not Quickload or LocalFile
        if (feature.gVersion.gServer.serverType != ServerTypeI.QuickLoad && feature.gVersion.gServer.serverType != ServerTypeI.LocalFiles) {
            return;
        }

        //If Loading whole genome for unoptimized file then load everything at once.
        if (((QuickLoadSymLoader) feature.symL).getSymLoader() instanceof SymLoaderInst) {
            if (optimized_sym != null) {
                loadAllSymmetriesThread(feature);
            }
            return;
        }

        iterateSeqList(feature);
    }

    @Override
    public void iterateSeqList(final GenericFeature feature) {

        CThreadWorker<Void, BioSeq> worker = new CThreadWorker<Void, BioSeq>(
                MessageFormat.format(LOADING_FEATURE_MESSAGE, feature.featureName)) {

                    @Override
                    protected Void runInBackground() {
                        Timer timer = new Timer();
                        timer.start();
                        try {
                            List<BioSeq> chrList = feature.symL.getChromosomeList();
                            Collections.sort(chrList,
                                    new Comparator<BioSeq>() {
                                        @Override
                                        public int compare(BioSeq s1, BioSeq s2) {
                                            return s1.getID().compareToIgnoreCase(s2.getID());
                                        }
                                    });
                            if (feature.symL.isMultiThreadOK()) {
                                return multiThreadedLoad(chrList);
                            }
                            return singleThreadedLoad(chrList);
                        } catch (Throwable ex) {
                            logger.error(
                                    "Error while loading feature", ex);
                            return null;
                        } finally {
                            logger.info("Loaded {} in {} secs", new Object[]{feature.featureName, (double) timer.read() / 1000f});
                        }
                    }

                    protected Void singleThreadedLoad(List<BioSeq> chrList) throws Exception {
                        final BioSeq current_seq = gmodel.getSelectedSeq();

                        if (current_seq != null) {
                            loadOnSequence(current_seq);
                            publish(current_seq);
                        }

                        for (final BioSeq seq : chrList) {
                            if (seq == current_seq) {
                                continue;
                            }
                            if (Thread.currentThread().isInterrupted()) {
                                break;
                            }
                            loadOnSequence(seq);
                        }
                        return null;
                    }

                    ExecutorService internalExecutor;

                    protected Void multiThreadedLoad(List<BioSeq> chrList) throws Exception {
                        internalExecutor = Executors.newFixedThreadPool(MAX_INTERNAL_THREAD);

                        final BioSeq current_seq = gmodel.getSelectedSeq();

                        if (current_seq != null) {
                            internalExecutor.submit(new Runnable() {
                                @Override
                                public void run() {
                                    loadOnSequence(current_seq);
                                    publish(current_seq);
                                }
                            });
                        }

                        for (final BioSeq seq : chrList) {
                            if (seq == current_seq) {
                                continue;
                            }

                            if (Thread.currentThread().isInterrupted()) {
                                break;
                            }

                            internalExecutor.submit(new Runnable() {
                                @Override
                                public void run() {
                                    loadOnSequence(seq);
                                }
                            });
                        }
                        internalExecutor.shutdown();
                        try {
                            internalExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                        } catch (InterruptedException ex) {
                            logger.warn("Internal executor exception", ex);
                        }

                        return null;
                    }

                    @Override
                    public boolean cancelThread(boolean b) {
                        boolean confirm = super.cancelThread(b);
                        if (confirm && internalExecutor != null) {
                            internalExecutor.shutdownNow();
                        }
                        return confirm;
                    }

                    @Override
                    protected void process(List<BioSeq> seqs) {
                        BioSeq selectedSeq = gmodel.getSelectedSeq();
                        BioSeq seq = seqs.get(0);
                        // If user has switched sequence then do not process it
                        if (selectedSeq == seq) {
                            gviewer.setAnnotatedSeq(seq, true, true);
                        }
                    }

                    @Override
                    protected void finished() {
                        if (isCancelled()) {
                            feature.setLoadStrategy(LoadStrategy.NO_LOAD);
                        }

                        BioSeq seq = gmodel.getSelectedSeq();
                        if (seq != null) {
                            gviewer.setAnnotatedSeq(seq, true, true);
                        } else if (gmodel.getSelectedSeqGroup() != null) {
                            if (gmodel.getSelectedSeqGroup().getSeqCount() > 0) {
                                // This can happen when loading a brand-new genome
                                gmodel.setSelectedSeq(gmodel.getSelectedSeqGroup().getSeq(0));
                            }
                        }
                        generalLoadView.refreshDataManagementView();
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
                            logger.error("Error in loadOnSequence", ex);
                            if (ex instanceof FileNotFoundException) {
                                ErrorHandler.errorPanel(feature.featureName + " not Found", "The server is no longer available. Please refresh the server from Preferences > Data Sources or try again later.", Level.SEVERE);
                            }
                        }
                    }
                };

        CThreadHolder.getInstance().execute(feature, worker);
    }

    private void loadFeaturesForSym(final SeqSymmetry optimized_sym, final GenericFeature feature) throws OutOfMemoryError {
        if (optimized_sym == null) {
            logger.debug("All of new query covered by previous queries for feature {}", feature.featureName);
            return;
        }

        final int seq_count = gmodel.getSelectedSeqGroup().getSeqCount();
        final CThreadWorker<Map<String, List<? extends SeqSymmetry>>, Object> worker
                = new CThreadWorker<Map<String, List<? extends SeqSymmetry>>, Object>(LOADING_MESSAGE_PREFIX + feature.featureName, Thread.MIN_PRIORITY) {

                    @Override
                    protected Map<String, List<? extends SeqSymmetry>> runInBackground() {
                        try {
                            return loadFeaturesForSym(feature, optimized_sym);
                        } catch (RuntimeException re) {
                            re.printStackTrace();
                        } catch (Exception ex) {
                            if (ex instanceof FileNotFoundException) {
                                ErrorHandler.errorPanel(feature.featureName + " not Found", "The server is no longer available. Please refresh the server from Preferences > Data Sources or try again later.", Level.SEVERE);
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        return Collections.<String, List<? extends SeqSymmetry>>emptyMap();
                    }

                    @Override
                    protected void finished() {

                        BioSeq aseq = gmodel.getSelectedSeq();

                        if (aseq != null) {
                            gviewer.setAnnotatedSeq(aseq, true, true);
//					if (this.isCancelled()) {
//						return;
//					}
//					try {
//						Map<String, List<? extends SeqSymmetry>> results = get();
//						for (Entry<String, List<? extends SeqSymmetry>> entry : results.entrySet()) {
//							RootSeqSymmetry annotSym = aseq.getAnnotation(entry.getKey());
//							if (entry.getKey() != null && annotSym != null) {
//								MapTierGlyphFactoryI factory = MapTierTypeHolder.getInstance().getDefaultFactoryFor(annotSym.getCategory());
//								ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(entry.getKey());
//								factory.createGlyphs(annotSym, entry.getValue(), style, gviewer, aseq);
//							}
//						}
//						gviewer.getSeqMap().repackTheTiers(true, true, true);
//					} catch (Exception ex) {
//						Logger.getLogger(GeneralLoadUtils.class.getName()).log(
//								Level.SEVERE, "Unable to get refresh action result.", ex);
//					}
                        } else if (gmodel.getSelectedSeqGroup() != null && gmodel.getSelectedSeqGroup().getSeqCount() > 0) {
                            // This can happen when loading a brand-new genome
                            aseq = gmodel.getSelectedSeqGroup().getSeq(0);
                            gmodel.setSelectedSeq(aseq);
                        }

                        //Since sequence are never removed so if no. of sequence increases then refresh sequence table.
                        if (gmodel.getSelectedSeqGroup() != null && gmodel.getSelectedSeqGroup().getSeqCount() > seq_count) {
                            seqGroupView.refreshTable();
                        }

                        generalLoadView.refreshDataManagementView();
                    }
                };

        CThreadHolder.getInstance().execute(feature, worker);
    }

    //TO DO: Make this private again.
    @Override
    public Map<String, List<? extends SeqSymmetry>> loadFeaturesForSym(
            GenericFeature feature, SeqSymmetry optimized_sym) throws OutOfMemoryError, Exception {
        if (feature.gVersion.gServer.serverType == null) {
            return Collections.<String, List<? extends SeqSymmetry>>emptyMap();
        }

        List<SeqSpan> optimized_spans = new ArrayList<SeqSpan>();
        SeqUtils.convertSymToSpanList(optimized_sym, optimized_spans);
        Map<String, List<? extends SeqSymmetry>> loaded = new HashMap<String, List<? extends SeqSymmetry>>();

        for (SeqSpan optimized_span : optimized_spans) {
            Map<String, List<? extends SeqSymmetry>> results = feature.gVersion.gServer.serverType.loadFeatures(optimized_span, feature);

            // If thread was interruped then it might return null. 
            // So avoid null pointer exception, check it here.
            if (results != null) {
                for (Entry<String, List<? extends SeqSymmetry>> entry : results.entrySet()) {
                    if (!loaded.containsKey(entry.getKey())) {
                        loaded.put(entry.getKey(), entry.getValue());
                    } else {
                        @SuppressWarnings("unchecked")
                        List<SeqSymmetry> syms = (List<SeqSymmetry>) loaded.get(entry.getKey());
                        syms.addAll(entry.getValue());
                    }
                }
            }

            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }

        return loaded;
    }

    private boolean checkBamAndSamLoading(GenericFeature feature, SeqSymmetry optimized_sym) {
        //start max
        boolean check = generalLoadView.isLoadingConfirm();
        if (optimized_sym != null && feature.getExtension() != null
                && (feature.getExtension().endsWith("bam") || feature.getExtension().endsWith("sam"))) {
            String message = "Region in view is big (> 500k), do you want to continue?";
            int childrenCount = optimized_sym.getChildCount();
            int spanWidth = 0;
            for (int childIndex = 0; childIndex < childrenCount; childIndex++) {
                SeqSymmetry child = optimized_sym.getChild(childIndex);
                for (int spanIndex = 0; spanIndex < child.getSpanCount(); spanIndex++) {
                    spanWidth += (child.getSpan(spanIndex).getMax() - child.getSpan(spanIndex).getMin());
                }
            }

            if (spanWidth > 500000) {
                if (!check) {
                    return !check;
                }

                generalLoadView.setShowLoadingConfirm(!check);
                return !(Application.confirmPanel(message,
                        PreferenceUtils.CONFIRM_BEFORE_LOAD, PreferenceUtils.default_confirm_before_load));
            }
        }
//		if(!check )
//			return !check;
//
        return false;
        //end max
    }

    private void setLastRefreshStatus(GenericFeature feature, boolean result) {
        if (result) {
            feature.setLastRefreshStatus(RefreshStatus.DATA_LOADED);
        } else {
            if (feature.getMethods().isEmpty()) {
                feature.setLastRefreshStatus(RefreshStatus.NO_DATA_LOADED);
            } else {
                feature.setLastRefreshStatus(RefreshStatus.NO_NEW_DATA_LOADED);
            }
        }
        //LoadModeTable.updateVirtualFeatureList();
    }

    /**
     * Get residues from servers: DAS/2, Quickload, or DAS/1. Also gets partial
     * residues.
     *
     * @param genomeVersionName -- name of the genome.
     * @param span	-- May be null. If not, then it's used for partial loading.
     */
    // Most confusing thing here -- certain parsers update the composition, and certain ones do not.
    // DAS/1 and partial loading in DAS/2 do not update the composition, so it's done separately.
    @Override
    public boolean getResidues(Set<GenericVersion> versionsWithChrom, String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span) {
        if (span == null) {
            span = new SimpleSeqSpan(min, max, aseq);
        }
        List<GenericVersion> versions = new ArrayList<GenericVersion>(versionsWithChrom);
        String seq_name = aseq.getID();
        boolean residuesLoaded = false;
        for (GenericVersion version : versions) {
            if (!version.gServer.isEnabled()) {
                continue;
            }
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }
            String serverDescription = version.gServer.serverName + " " + version.gServer.serverType;
//			String msg = MessageFormat.format(IGBConstants.BUNDLE.getString("loadingSequence"), seq_name, serverDescription);
//			Application.getSingleton().addNotLockedUpMsg(msg);
            if (version.gServer.serverType != null && version.gServer.serverType.getResidues(version, genomeVersionName, aseq, min, max, span)) {
                residuesLoaded = true;
            }
//			Application.getSingleton().removeNotLockedUpMsg(msg);
            if (residuesLoaded) {
                Application.getSingleton().setStatus(MessageFormat.format(IGBConstants.BUNDLE.getString("completedLoadingSequence"),
                        seq_name, min, max, serverDescription));
                return true;
            }
        }
        Application.getSingleton().setStatus("");
        return false;
    }

    /**
     * Load residues on span. First, attempt to load them with DAS/2 servers.
     * Second, attempt to load them with QuickLoad servers. Third, attempt to
     * load them with DAS/1 servers.
     *
     * @param aseq
     * @param span	-- may be null, if the entire sequence is requested.
     * @return true if succeeded.
     */
    @Override
    public boolean loadResidues(String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span) {

        /*
         * This test does not work properly, so it's being commented out for
         * now.
         *
         * if (aseq.isComplete()) {  if (logger.isDebugEnabled()) { System.out.println("already
         * have residues for " + seq_name); } return false; }
         */
        // Determine list of servers that might have this chromosome sequence.
        Set<GenericVersion> versionsWithChrom = new HashSet<>();
        versionsWithChrom.addAll(aseq.getSeqGroup().getEnabledVersions());

        if ((min <= 0) && (max >= aseq.getLength())) {
            min = 0;
            max = aseq.getLength();
        }

        if (aseq.isAvailable(min, max)) {
            logger.info(
                    "All residues in range are already loaded on sequence {}", new Object[]{aseq});
            return true;
        }

//		Application.getSingleton().addNotLockedUpMsg("Loading residues for "+aseq.getID());
        return getResidues(versionsWithChrom, genomeVersionName, aseq, min, max, span);
    }

    /**
     * Get synonyms of version.
     *
     * @param versionName - version name
     * @return a friendly HTML string of version synonyms (not including
     * versionName).
     */
    @Override
    public String listSynonyms(String versionName) {
        StringBuilder synonymBuilder = new StringBuilder(100);
        synonymBuilder.append("<html>").append(IGBConstants.BUNDLE.getString("synonymList"));
        Set<String> synonymSet = synonymLookupService.getSynonyms(versionName);
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
    @Override
    public void loadServerMapping() {
        GenericServer primaryServer = ServerList.getServerInstance().getPrimaryServer();
        if (primaryServer == null) {
            return;
        }
        InputStream istr = null;
        InputStreamReader ireader = null;
        BufferedReader br = null;

        try {
            try {
                istr = LocalUrlCacher.getInputStream(primaryServer.getFriendlyURL() + SERVER_MAPPING);
            } catch (Exception e) {
                logger.error("Couldn''t open ''{}" + SERVER_MAPPING + "\n:  {}", new Object[]{primaryServer.getFriendlyURL(), e.toString()});
                istr = null; // dealt with below
            }
            if (istr == null) {
                logger.info("Could not load server mapping contents from\n{}" + SERVER_MAPPING, primaryServer.getFriendlyURL());
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
            ErrorHandler.errorPanel("Error loading server mapping", ex, Level.SEVERE);
        } finally {
            GeneralUtils.safeClose(istr);
            GeneralUtils.safeClose(ireader);
            GeneralUtils.safeClose(br);
        }
    }

    /**
     * Get directory url on cached server from servermapping map.
     *
     * @param url	URL of the server.
     * @return	Returns a directory if exists else null.
     */
    @Override
    public URL getServerDirectory(String url) {
        if (ServerList.getServerInstance().getPrimaryServer() == null) {
            return null;
        }

        for (Entry<String, String> primary : servermapping.entrySet()) {
            if (url.equals(primary.getKey())) {
                try {
                    return new URL(primary.getValue());
                } catch (MalformedURLException ex) {
                    logger.error(null, ex);
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Set autoload variable in features.
     *
     * @param autoload
     */
    @Override
    public void setFeatureAutoLoad(boolean autoload) {
        for (GenericVersion genericVersion : versionDiscoverer.getSpecies2Generic().values()) {
            for (GenericFeature genericFeature : genericVersion.getFeatures()) {
                if (autoload) {
                    genericFeature.setAutoload(autoload);
                }
            }
        }

        //It autoload data is selected then load.
        if (autoload) {
            loadWholeRangeFeatures(null);
            generalLoadView.refreshTreeView();
            generalLoadView.refreshDataManagementView();
        }
    }

    @Override
    public List<String> getSpeciesList() {
        return Ordering.from(String.CASE_INSENSITIVE_ORDER).immutableSortedCopy(versionDiscoverer.getSpecies2Generic().keySet());
    }

    @Override
    public List<String> getGenericVersions(final String speciesName) {
        final Set<GenericVersion> versionList = versionDiscoverer.getSpecies2Generic().get(speciesName);
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

    @Override
    public void openURI(URI uri, String fileName, AnnotatedSeqGroup loadGroup, String speciesName, boolean isReferenceSequence) {
        // If server requires authentication then.
        // If it cannot be authenticated then don't add the feature.
        if (!LocalUrlCacher.isValidURI(uri)) {
            ErrorHandler.errorPanel("UNABLE TO FIND URL", uri + "\n URL provided not found or times out: ", Level.WARNING);
            return;
        }

        GenericFeature gFeature = getFeature(uri, fileName, speciesName, loadGroup, isReferenceSequence);

        if (gFeature == null) {
            return;
        } else {
            addFeature(gFeature);
        }
    }

    @Override
    public void addFeature(GenericFeature gFeature) {
        if (gFeature.symL != null) {
            addChromosomesForUnknownGroup(gFeature);
        }

        // force a refresh of this server		
        ServerList.getServerInstance().fireServerInitEvent(ServerList.getServerInstance().getLocalFilesServer(), ServerStatus.Initialized, true);

//		SeqGroupView.getInstance().setSelectedGroup(gFeature.gVersion.group.getID());
        generalLoadView.refreshDataManagementView();
    }

    private void addChromosomesForUnknownGroup(final GenericFeature gFeature) {
        if (((QuickLoadSymLoader) gFeature.symL).getSymLoader() instanceof SymLoaderInstNC) {
            loadAllSymmetriesThread(gFeature);
            // force a refresh of this server. This forces creation of 'genome' sequence.
            ServerList.getServerInstance().fireServerInitEvent(ServerList.getServerInstance().getLocalFilesServer(), ServerStatus.Initialized, true);
            return;
        }

        final AnnotatedSeqGroup loadGroup = gFeature.gVersion.group;
        final String message = MessageFormat.format(IGBConstants.BUNDLE.getString("retrieveChr"), gFeature.featureName);
        final CThreadWorker<Boolean, Object> worker = new CThreadWorker<Boolean, Object>(message) {
            boolean featureRemoved = false;

            @Override
            protected Boolean runInBackground() {
                try {
                    for (BioSeq seq : gFeature.symL.getChromosomeList()) {
                        loadGroup.addSeq(seq.getID(), seq.getLength(), gFeature.symL.uri.toString());
                    }
                    return true;
                } catch (Exception ex) {
                    ((QuickLoadSymLoader) gFeature.symL).logException(ex);
                    featureRemoved = removeFeatureAndRefresh(gFeature, "Unable to load data set for this file. \nWould you like to remove this file from the list?");
                    return featureRemoved;
                }

            }

            @Override
            protected boolean showCancelConfirmation() {
                return removeFeature("Cancel chromosome retrieval and remove " + gFeature.featureName + "?");
            }

            private boolean removeFeature(String msg) {
                if (Application.confirmPanel(msg)) {
                    if (gFeature.gVersion.removeFeature(gFeature)) {
                        seqGroupView.refreshTable();
                    }
                    return true;
                }
                return false;
            }

            @Override
            protected void finished() {
                boolean result = true;
                try {
                    if (!isCancelled()) {
                        result = get();
                    } else {
                        result = false;
                    }
                } catch (Exception ex) {
                    logger.error(null, ex);
                }
                if (result) {
                    addFeatureTier(gFeature);
                    seqGroupView.refreshTable();
                    if (loadGroup.getSeqCount() > 0 && gmodel.getSelectedSeq() == null) {
                        // select a chromosomes
                        gmodel.setSelectedSeq(loadGroup.getSeq(0));
                    }
                } else {
                    gmodel.setSelectedSeq(gmodel.getSelectedSeq());
                }
                ServerList.getServerInstance().fireServerInitEvent(ServerList.getServerInstance().getLocalFilesServer(), ServerStatus.Initialized, true);
                if (gFeature.getLoadStrategy() == LoadStrategy.VISIBLE && !featureRemoved) {
                    if (gFeature.isReferenceSequence()) {
                        JLabel label = new JLabel(GenericFeature.REFERENCE_SEQUENCE_LOAD_MESSAGE);
                        Application.infoPanel(label);
                    } else {
                        Application.infoPanel(GenericFeature.LOAD_WARNING_MESSAGE,
                                GenericFeature.show_how_to_load, GenericFeature.default_show_how_to_load);
                    }
                }
            }
        };
        CThreadHolder.getInstance().execute(gFeature, worker);
    }

    private boolean removeFeatureAndRefresh(GenericFeature gFeature, String msg) {
        if (Application.confirmPanel(msg)) {
            removeFeature(gFeature, true);
            return true;
        }
        return false;
    }

    @Override
    public GenericFeature getFeature(URI uri, String fileName, String speciesName, AnnotatedSeqGroup loadGroup, boolean isReferenceSequence) {
        GenericFeature gFeature = getLoadedFeature(uri);
        // Test to determine if a feature with this uri is contained in the load mode table
        if (gFeature == null) {
            GenericVersion version = getLocalFilesVersion(loadGroup, speciesName);
            version = setVersion(uri, loadGroup, version);

            // In case of BAM
            if (version == null) {
                return null;
            }

            // handle URL case.
            String uriString = uri.toString();
            int httpIndex = uriString.toLowerCase().indexOf("http:");
            if (httpIndex > -1) {
                // Strip off initial characters up to and including http:
                // Sometimes this is necessary, as URLs can start with invalid "http:/"
                uriString = GeneralUtils.convertStreamNameToValidURLName(uriString);
                uri = URI.create(uriString);
            }
            boolean autoload = PreferenceUtils.getBooleanParam(PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load);

            Map<String, String> featureProps = null;
            if (!LocalUrlCacher.isValidURI(uri)) {
                return null;
            }
            SymLoader symL = ServerUtils.determineLoader(SymLoader.getExtension(uri), uri, QuickLoadSymLoader.detemineFriendlyName(uri), version.group);
            if (symL != null && symL.isResidueLoader() && !isReferenceSequence) {
                featureProps = new HashMap<>();
                featureProps.put("collapsed", "true");
                featureProps.put("show2tracks", "false");
            }
            String friendlyName = QuickLoadSymLoader.detemineFriendlyName(uri);
            QuickLoadSymLoader quickLoad = SymLoader.getExtension(uri).endsWith("chp")
                    ? new QuickLoadSymLoaderChp(uri, friendlyName, version.group)
                    : new QuickLoadSymLoader(uri, friendlyName, version.group, !isReferenceSequence);
            gFeature = new GenericFeature(fileName, featureProps, version, quickLoad, File.class, autoload, isReferenceSequence);

            version.addFeature(gFeature);

            gFeature.setVisible(); // this should be automatically checked in the feature tree

        } else {
            ErrorHandler.errorPanel("Cannot add same feature",
                    "The feature " + uri + " has already been added.", Level.WARNING);
        }

        return gFeature;
    }

    /**
     * Handle file formats that has SeqGroup info.
     */
    private GenericVersion setVersion(URI uri, AnnotatedSeqGroup loadGroup, GenericVersion version) {
        String unzippedStreamName = GeneralUtils.stripEndings(uri.toString());
        String extension = GeneralUtils.getExtension(unzippedStreamName);
        boolean getNewVersion = false;

        if (extension.equals(BAM_EXT)) {
            try {
                handleBam(uri);
            } catch (BamIndexNotFoundException ex) {
                String errorMessage = MessageFormat.format(IGBConstants.BUNDLE.getString("bamIndexNotFound"), uri);
                ErrorHandler.errorPanel("Cannot open file", errorMessage, Level.WARNING);
                version = null;

            }
        } else if (extension.equals(USEQ_EXT)) {
            loadGroup = handleUseq(uri, loadGroup);
            getNewVersion = true;
        } else if (extension.equals(BAR_EXT)) {
            loadGroup = handleBar(uri, loadGroup);
            getNewVersion = true;
        } else if (extension.equals(BP1_EXT) || extension.equals(BP2_EXT)) {
            loadGroup = handleBp(uri, loadGroup);
            getNewVersion = true;
        }

        if (getNewVersion) {
            GenericVersion newVersion = getLocalFilesVersion(loadGroup, loadGroup.getOrganism());
            if (GenometryModel.getInstance().getSelectedSeqGroup() == null
                    || version == newVersion
                    || Application.confirmPanel(MessageFormat.format(IGBConstants.BUNDLE.getString("confirmGroupChange"),
                                    version.group.getOrganism(), version, newVersion.group.getOrganism(), newVersion),
                            PreferenceUtils.CONFIRM_BEFORE_GROUP_CHANGE,
                            PreferenceUtils.default_confirm_before_group_change)) {
                version = newVersion;
            }
        }

        return version;
    }

    private boolean handleBam(URI uri) throws BamIndexNotFoundException {
        return BAM.hasIndex(uri);
    }

    /**
     * Get AnnotatedSeqGroup for BAR file format.
     */
    private AnnotatedSeqGroup handleBar(URI uri, AnnotatedSeqGroup group) {
        InputStream istr = null;
        try {
            istr = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
            List<AnnotatedSeqGroup> groups = BarParser.getSeqGroups(uri.toString(), istr, group, gmodel);
            if (groups.isEmpty()) {
                return group;
            }

            //TODO: What if there are more than one seq group ?
            if (groups.size() > 1) {
                logger.warn("File {} has more than one group. Looking for the closest match to existing", new Object[]{uri.toString()});
                //First look for the selected group in the groups
                for (AnnotatedSeqGroup gr : groups) {
                    if (gr == group) {
                        return gr;
                    }
                }

                //If it does not match any exiting group the return the one that matches organism
                if (group.getOrganism() != null && group.getOrganism().length() != 0) {
                    for (AnnotatedSeqGroup gr : groups) {
                        if (group.getOrganism().equalsIgnoreCase(gr.getOrganism())) {
                            return gr;
                        }
                    }
                }

                //If it does not match organism then return the group with most version
                AnnotatedSeqGroup grp = groups.get(0);
                for (AnnotatedSeqGroup gr : groups) {
                    if (gr.getAllVersions().size() > grp.getAllVersions().size()) {
                        grp = gr;
                    }
                }
                return grp;
            }

            //Return the first one
            return groups.get(0);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            GeneralUtils.safeClose(istr);
        }

        return group;
    }

    /**
     * Get AnnotatedSeqGroup for USEQ file format.
     */
    private AnnotatedSeqGroup handleUseq(URI uri, AnnotatedSeqGroup group) {
        InputStream istr = null;
        ZipInputStream zis = null;
        try {
            istr = LocalUrlCacher.getInputStream(uri.toURL());
            zis = new ZipInputStream(istr);
            zis.getNextEntry();
            ArchiveInfo archiveInfo = new ArchiveInfo(zis, false);
            AnnotatedSeqGroup gr = USeqGraphParser.getSeqGroup(archiveInfo.getVersionedGenome(), gmodel);
            if (gr != null) {
                return gr;
            }
        } catch (Exception ex) {
            logger.error("Exception in handleUseq method", ex);
        } finally {
            GeneralUtils.safeClose(istr);
            GeneralUtils.safeClose(zis);
        }

        return group;
    }

    private AnnotatedSeqGroup handleBp(URI uri, AnnotatedSeqGroup group) {
        InputStream istr = null;
        try {
            istr = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
            AnnotatedSeqGroup gr = Bprobe1Parser.getSeqGroup(istr, group, gmodel);
            if (gr != null) {
                return gr;
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            GeneralUtils.safeClose(istr);
        }

        return group;
    }

    /**
     * For unoptimized file formats load symmetries and add them.
     *
     * @param feature
     */
    @Override
    public void loadAllSymmetriesThread(final GenericFeature feature) {
        final QuickLoadSymLoader quickLoad = (QuickLoadSymLoader) feature.symL;

        CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>(LOADING_MESSAGE_PREFIX + feature.featureName) {

            @Override
            protected Object runInBackground() {
                try {
                    quickLoad.loadAndAddAllSymmetries(feature);
                } catch (Exception ex) {
                    quickLoad.logException(ex);
                    removeFeatureAndRefresh(feature, "Unable to load data set for this file. \nWould you like to remove this file from the list?");
                }
                return null;
            }

            @Override
            protected void finished() {
                try {
                    BioSeq aseq = GenometryModel.getInstance().getSelectedSeq();
                    if (aseq != null) {
                        gviewer.setAnnotatedSeq(aseq, true, true);
                    } else if (GenometryModel.getInstance().getSelectedSeq() == null && quickLoad.getAnnotatedSeqGroup() != null) {
                        // This can happen when loading a brand-new genome
                        GenometryModel.getInstance().setSelectedSeq(quickLoad.getAnnotatedSeqGroup().getSeq(0));
                    }

                    seqGroupView.refreshTable();
                    generalLoadView.refreshDataManagementView();
                } catch (Exception ex) {
                    logger.error("Exception occurred while loading symmetries", ex);
                }
            }
        };

        CThreadHolder.getInstance().execute(feature, worker);
    }

    @Override
    public boolean isLoaded(GenericFeature gFeature) {
        GenericFeature f = getLoadedFeature(gFeature.getURI());
        if (f != null && f != gFeature) {
            gFeature.clear();
            //TODO look into this refresh call
            generalLoadView.refreshTreeView();
            return true;
        }

        return false;
    }

    @Override
    public GenericFeature getLoadedFeature(URI uri) {
        for (GenericFeature gFeature : getVisibleFeatures()) {
            if (gFeature.getURI().equals(uri) && gFeature.isVisible()) {
                return gFeature;
            }
        }
        return null;
    }

    /**
     * Load any data that's marked for visible range.
     */
    public void loadVisibleFeatures() {
        if (logger.isDebugEnabled()) {
            SeqSpan request_span = gviewer.getVisibleSpan();
            logger.debug("Visible load request span: " + request_span.getBioSeq() + ":" + request_span.getStart() + "-" + request_span.getEnd());
        }
        List<LoadStrategy> loadStrategies = new ArrayList<>();
        loadStrategies.add(LoadStrategy.AUTOLOAD);
        loadStrategies.add(LoadStrategy.VISIBLE);
//		loadStrategies.add(LoadStrategy.CHROMOSOME);
        //TODO refactor code to not use serverType == null as a hack
        loadFeatures(loadStrategies, null);
    }

    /**
     * Load any features that have a autoload strategy and haven't already been
     * loaded.
     */
    public void loadAutoLoadFeatures() {
        List<LoadStrategy> loadStrategies = new ArrayList<>();
        loadStrategies.add(LoadStrategy.AUTOLOAD);
        loadFeatures(loadStrategies, null);
        bufferDataForAutoload();
    }

    /**
     * Load any features that have a whole strategy and haven't already been
     * loaded.
     */
    public void loadWholeRangeFeatures(ServerTypeI serverType) {
        List<LoadStrategy> loadStrategies = new ArrayList<>();
        loadStrategies.add(LoadStrategy.GENOME);
        loadFeatures(loadStrategies, serverType);
    }

    void loadFeatures(List<LoadStrategy> loadStrategies, ServerTypeI serverType) {
        for (GenericFeature gFeature : getSelectedVersionFeatures()) {
            if (isLoaded(gFeature)) {
                continue;
            }
            loadFeature(loadStrategies, gFeature, serverType);
        }
    }

    boolean loadFeature(List<LoadStrategy> loadStrategies, GenericFeature gFeature, ServerTypeI serverType) {
        if (!loadStrategies.contains(gFeature.getLoadStrategy())) {
            return false;
        }

        //TODO refactor code to not use serverType == null as a hack
        if (serverType != null && gFeature.gVersion.gServer.serverType != serverType) {
            return false;
        }

        loadAndDisplayAnnotations(gFeature);

        return true;
    }

    public synchronized void AutoloadQuickloadFeature() {
        for (GenericFeature gFeature : getSelectedVersionFeatures()) {
            if (gFeature.getLoadStrategy() != LoadStrategy.GENOME
                    || gFeature.gVersion.gServer.serverType != ServerTypeI.QuickLoad) {
                continue;
            }

            if (isLoaded(gFeature)) {
                continue;
            }

            //If Loading whole genome for unoptimized file then load everything at once.
            if (((QuickLoadSymLoader) gFeature.symL).getSymLoader() instanceof SymLoaderInst) {
                loadAllSymmetriesThread(gFeature);
            } else {
                iterateSeqList(gFeature);
            }
        }
    }

    public void initVersion(String versionName) {
        igbService.addNotLockedUpMsg(MessageFormat.format(BUNDLE.getString("loadingChr"), versionName));
        try {
            initVersionAndSeq(versionName); // Make sure this genome versionName's feature names are initialized.
        } finally {
            igbService.removeNotLockedUpMsg(MessageFormat.format(BUNDLE.getString("loadingChr"), versionName));
        }
    }

    /**
     * Handles clicking of partial residue, all residue, and refresh data
     * buttons.
     */
    public void loadResidues(final boolean partial) {
        final BioSeq seq = gmodel.getSelectedSeq();

        CThreadWorker<Boolean, Void> worker = new CThreadWorker<Boolean, Void>(MessageFormat.format(BUNDLE.getString(partial ? "loadPartialResidues" : "loadAllResidues"), seq.getID()), Thread.MIN_PRIORITY) {

            @Override
            public Boolean runInBackground() {
                return loadResidues(seq, gviewer.getVisibleSpan(), partial, false, true);
            }

            @Override
            public void finished() {
                try {
                    if (!isCancelled() && get()) {
                        gviewer.setAnnotatedSeq(seq, true, true, true);
                    }
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
//					igbService.removeNotLockedUpMsg("Loading residues for " + seq.getID());
                }
            }
        };

        // Use a SwingWorker to avoid locking up the GUI.
        CThreadHolder.getInstance().execute(this, worker);
    }

    public boolean loadResidues(SeqSpan span, boolean tryFull) {
        if (!span.isForward()) {
            span = new SimpleSeqSpan(span.getMin(), span.getMax(), span.getBioSeq());
        }
        return loadResidues(span.getBioSeq(), span, true, tryFull, false);
    }

    private boolean loadResidues(final BioSeq seq,
            final SeqSpan viewspan, final boolean partial, final boolean tryFull, final boolean show_error_panel) {
        final String genomeVersionName = (String) seqGroupView.getVersionCB().getSelectedItem();
        try {
            if (partial) {
                if (!loadResidues(genomeVersionName, seq, viewspan.getMin(), viewspan.getMax(), viewspan)
                        && !Thread.currentThread().isInterrupted()) {
                    if (!tryFull) {
                        if (show_error_panel) {
                            ErrorHandler.errorPanel("Couldn't load partial sequence", "Couldn't locate the partial sequence.  Try loading the full sequence.", Level.INFO);
                        }
                        java.util.logging.Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.WARNING, "Unable to load partial sequence");
                        return false;
                    } else {
                        if (!loadResidues(genomeVersionName, seq, 0, seq.getLength(), null)) {
                            if (show_error_panel) {
                                ErrorHandler.errorPanel("Couldn't load partial or full sequence", "Couldn't locate the sequence.", Level.SEVERE);
                            }
                            java.util.logging.Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.WARNING,
                                    "Couldn't load partial or full sequence. Couldn't locate the sequence.");
                            return false;
                        }
                    }
                }
            } else {
                if (!loadResidues(genomeVersionName, seq, 0, seq.getLength(), null)
                        && !Thread.currentThread().isInterrupted()) {
                    if (show_error_panel) {
                        ErrorHandler.errorPanel("Couldn't load full sequence", "Couldn't locate the sequence.", Level.SEVERE);
                    }
                    java.util.logging.Logger.getLogger(GeneralLoadViewGUI.class.getName()).log(Level.WARNING,
                            "Couldn't load full sequence. Couldn't locate the sequence.");
                    return false;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    public void useAsRefSequence(final GenericFeature feature) throws Exception {
        if (feature != null && feature.symL != null) {
            final QuickLoadSymLoader quickload = (QuickLoadSymLoader) feature.symL;
            if (quickload.getSymLoader() instanceof ResidueTrackSymLoader) {

                final CThreadWorker<Void, Void> worker = new CThreadWorker<Void, Void>(feature.featureName) {

                    @Override
                    protected Void runInBackground() {
                        try {
                            SymWithProps sym;
                            SeqSymmetry child;
                            SimpleSymWithResidues rchild;

                            for (BioSeq seq : feature.symL.getChromosomeList()) {
                                sym = seq.getAnnotation(feature.getURI().toString());
                                if (sym != null) {

                                    //Clear previous sequence
                                    seq.setComposition(null);

                                    for (int i = 0; i < sym.getChildCount(); i++) {
                                        child = sym.getChild(i);
                                        if (child instanceof SimpleSymWithResidues) {
                                            rchild = (SimpleSymWithResidues) child;
                                            BioSeqUtils.addResiduesToComposition(seq, rchild.getResidues(), rchild.getSpan(seq));
                                        }
                                    }
                                    seq.removeAnnotation(sym);
                                }
                            }

                            ((ResidueTrackSymLoader) quickload.getSymLoader()).loadAsReferenceSequence(true);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void finished() {
                        gviewer.updatePanel();
                    }
                };

                worker.execute();
            }
        }
    }

    public GenericFeature createFeature(String featureName, SymLoader loader) {
        GenericVersion version = getIGBFilesVersion(GenometryModel.getInstance().getSelectedSeqGroup(), getSelectedSpecies());
        GenericFeature feature = new GenericFeature(featureName, null, version, loader, null, false);
        version.addFeature(feature);
        feature.setVisible(); // this should be automatically checked in the feature tree
        ServerList.getServerInstance().fireServerInitEvent(ServerList.getServerInstance().getIGBFilesServer(), LoadUtils.ServerStatus.Initialized, true);
        generalLoadView.refreshDataManagementView();

        return feature;
    }

    public void addViewFeature(final GenericFeature feature) {
        feature.setVisible();

        List<LoadStrategy> loadStrategies = new java.util.ArrayList<>();
        loadStrategies.add(LoadStrategy.GENOME);

        if (!loadFeature(loadStrategies, feature, null)) {
            addFeatureTier(feature);
        }

        generalLoadView.refreshDataManagementView();
    }

    public void addFeatureTier(final GenericFeature feature) {

        CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>(LOADING_MESSAGE_PREFIX + feature.featureName, Thread.MIN_PRIORITY) {

            @Override
            protected Object runInBackground() {
                TrackView.getInstance().addEmptyTierFor(feature, gviewer);
                return null;
            }

            @Override
            protected void finished() {
                AbstractAction action = new AbstractAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        generalLoadView.refreshDataManagementTable(getVisibleFeatures());
                        gviewer.getSeqMap().packTiers(false, true, true);
                        gviewer.getSeqMap().stretchToFit(false, true);
                        gviewer.getSeqMap().updateWidget();
                        TierPrefsView.getSingleton().refreshList();
                    }
                };
                gviewer.preserveSelectionAndPerformAction(action);
            }
        };

        CThreadHolder.getInstance().execute(feature, worker);
    }

    void removeAllFeautres(Set<GenericFeature> features) {
        for (GenericFeature feature : features) {
            if (feature.isVisible()) {
                removeFeature(feature, true);
            }
        }
    }

    public void removeFeature(final GenericFeature feature, final boolean refresh) {
        removeFeature(feature, refresh, true);
    }

    public void clearTrack(final ITrackStyleExtended style) {
        final String method = style.getMethodName();
        if (method != null) {
            final BioSeq bioseq = GenometryModel.getInstance().getSelectedSeq();
            final GenericFeature feature = style.getFeature();

            // If genome is selected then delete all syms on the all seqs.
            if (IGBConstants.GENOME_SEQ_ID.equals(bioseq.getID())) {
                removeFeature(feature, true);
                return;
            }

            CThreadWorker<Void, Void> clear = new CThreadWorker<Void, Void>("Clearing track  " + style.getTrackName()) {

                @Override
                protected Void runInBackground() {
                    TrackView.getInstance().deleteSymsOnSeq(gviewer, method, bioseq, feature);
                    return null;
                }

                @Override
                protected void finished() {
                    TierGlyph tier = TrackView.getInstance().getTier(style, StyledGlyph.Direction.FORWARD);
                    if (tier != null) {
                        tier.removeAllChildren();
                        tier.setInfo(null);
                    }
                    tier = TrackView.getInstance().getTier(style, StyledGlyph.Direction.REVERSE);
                    if (tier != null) {
                        tier.removeAllChildren();
                        tier.setInfo(null);
                    }
                    TrackView.getInstance().addTierFor(style, gviewer);
                    gviewer.getSeqMap().repackTheTiers(true, true, true);
                }

            };

            CThreadHolder.getInstance().execute(feature, clear);
        }
    }

    void removeFeature(final GenericFeature feature, final boolean refresh, final boolean removeLocal) {
        if (feature == null) {
            return;
        }

        CThreadWorker<Void, Void> delete = new CThreadWorker<Void, Void>("Removing feature  " + feature.featureName) {

            @Override
            protected Void runInBackground() {
                if (!feature.getMethods().isEmpty()) {
                    for (String method : feature.getMethods()) {
                        for (BioSeq bioseq : feature.gVersion.group.getSeqList()) {
                            TrackView.getInstance().deleteSymsOnSeq(gviewer, method, bioseq, feature);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void finished() {
                boolean refSeq = feature.gVersion.gServer.serverType.equals(ServerTypeI.LocalFiles) && feature.symL.isResidueLoader();
                if (removeLocal || refSeq) {
                    // If feature is local then remove it from server.
                    GenericVersion version = feature.gVersion;
                    if (version.gServer.serverType.equals(ServerTypeI.LocalFiles)) {
                        if (version.removeFeature(feature)) {
                            seqGroupView.refreshTable();
                            if (gmodel.getSelectedSeqGroup().getSeqCount() > 0
                                    && !gmodel.getSelectedSeqGroup().getSeqList().contains(gmodel.getSelectedSeq())) {
                                gmodel.setSelectedSeq(gmodel.getSelectedSeqGroup().getSeqList().get(0));
                            } else {
                                gmodel.setSelectedSeq(null);
                            }
                        }
                    }
                }

                if (refresh) {
                    removeTier(feature.getURI().toString());
                    if (!feature.getMethods().isEmpty()) {
                        for (String method : feature.getMethods()) {
                            removeTier(method);
                        }
                    }
                    feature.clear();

                    // Refresh
                    generalLoadView.refreshTreeViewAndRestore();
                    generalLoadView.refreshDataManagementView();
                    //gviewer.dataRemoved();
                    gviewer.getSeqMap().repackTheTiers(true, true, true);
                }

                ((AffyLabelledTierMap) gviewer.getSeqMap()).fireTierOrderChanged();
            }

            private void removeTier(String method) {
                ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method);
                TierGlyph tier = TrackView.getInstance().getTier(style, StyledGlyph.Direction.FORWARD);
                if (tier != null) {
                    gviewer.getSeqMap().removeTier(tier);
                }
                tier = TrackView.getInstance().getTier(style, StyledGlyph.Direction.REVERSE);
                if (tier != null) {
                    gviewer.getSeqMap().removeTier(tier);
                }

                if (style.isGraphTier()) {
                    DefaultStateProvider.getGlobalStateProvider().removeGraphState(method);
                } else {
                    DefaultStateProvider.getGlobalStateProvider().removeAnnotStyle(method);
                }
            }
        };

        CThreadHolder.getInstance().execute(feature, delete);

    }

    @Reference
    @Override
    public void setSeqGroupView(SeqGroupViewI seqGroupView) {
        this.seqGroupView = seqGroupView;
    }

    @Reference
    @Override
    public void setVersionDiscoverer(VersionDiscoverer versionDiscoverer) {
        this.versionDiscoverer = versionDiscoverer;
    }

    @Reference
    public void setGeneralLoadView(GeneralLoadView generalLoadView) {
        this.generalLoadView = generalLoadView;
    }

    @Reference
    public void setIgbService(IGBService igbService) {
        this.igbService = igbService;
    }

}
