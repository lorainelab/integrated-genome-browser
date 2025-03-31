package com.affymetrix.igb.view.load;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.LocalDataProvider;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderComparator;
import com.affymetrix.genometry.data.assembly.AssemblyProvider;
import com.affymetrix.genometry.data.sequence.ReferenceSequenceDataSetProvider;
import com.affymetrix.genometry.data.sequence.ReferenceSequenceProvider;
import com.affymetrix.genometry.data.sequence.ReferenceSequenceResource;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import static com.affymetrix.genometry.general.DataSet.detemineFriendlyName;
import com.affymetrix.genometry.parsers.Bprobe1Parser;
import com.affymetrix.genometry.parsers.graph.BarParser;
import com.affymetrix.genometry.parsers.useq.ArchiveInfo;
import com.affymetrix.genometry.parsers.useq.USeqGraphParser;
import com.affymetrix.genometry.quickload.QuickLoadSymLoader;
import com.affymetrix.genometry.span.MutableDoubleSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symloader.SymLoaderInst;
import com.affymetrix.genometry.symloader.SymLoaderInstNC;
import com.affymetrix.genometry.symloader.TwoBitNew;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.BioSeqUtils;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.LoadUtils.ResourceStatus;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometry.util.ServerUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import static com.affymetrix.igb.view.load.FileExtensionContants.BAM_EXT;
import static com.affymetrix.igb.view.load.FileExtensionContants.BAR_EXT;
import static com.affymetrix.igb.view.load.FileExtensionContants.BP1_EXT;
import static com.affymetrix.igb.view.load.FileExtensionContants.BP2_EXT;
import static com.affymetrix.igb.view.load.FileExtensionContants.USEQ_EXT;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import javax.swing.JLabel;
import org.lorainelab.igb.synonymlookup.services.ChromosomeSynonymLookup;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @version $Id: GeneralLoadUtils.java 11492 2012-05-10 18:17:28Z hiralv $
 */
public final class GeneralLoadUtils {

    private static final Logger LOG = LoggerFactory.getLogger(GeneralLoadUtils.class);
    private static final int MAX_INTERNAL_THREAD = Runtime.getRuntime().availableProcessors() + 1;
    private static final Pattern tab_regex = Pattern.compile("\t");
    /**
     * using negative start coord for virtual genome chrom because (at least for human genome) whole genome start/end/length can't be represented with positive 4-byte ints (limit is +/- 2.1 billion)
     */
//    final double default_genome_min = -2100200300;
    private static final double default_genome_min = -2100200300;
    private static final GenometryModel gmodel = GenometryModel.getInstance();
    // File name storing directory name associated with server on a cached server.
    public static final String SERVER_MAPPING = "/serverMapping.txt";
    private static final String LOADING_FEATURE_MESSAGE = IGBConstants.BUNDLE.getString("loadFeature");
    /**
     * Location of synonym file for correlating versions to species. The file lookup is done using {@link Class#getResourceAsStream(String)}. The default file is {@value}.
     */

    private static final double MAGIC_SPACER_NUMBER = 10.0;	// spacer factor used to keep genome spacing reasonable
    private final static SeqMapView gviewer = IGB.getInstance().getMapView();
    // versions associated with a given genome.
    private static final SetMultimap<String, DataContainer> speciesDataContainerReference
            = Multimaps.synchronizedSetMultimap(LinkedHashMultimap.create());// the list of versions associated with the species

    static final Map<String, String> versionName2species
            = new HashMap<>();	// the species associated with the given version.
    static Map<String, Integer> assemblyInfo = new HashMap<>();

    final static Comparator<DataContainer> DATA_CONTAINER_PRIORITY_COMPARATOR = (DataContainer o1, DataContainer o2) -> {
        DataProviderComparator dataProviderComparator = new DataProviderComparator();
        return dataProviderComparator.compare(o1.getDataProvider(), o2.getDataProvider());
    };

    final static Supplier<TreeSet<DataContainer>> DATA_CONTAINER_SORTED_SUPPLIER = () -> new TreeSet<>(DATA_CONTAINER_PRIORITY_COMPARATOR);

    public static Map<String, String> getVersionName2Species() {
        return versionName2species;
    }

    public static Map<String, Integer> getAssemblyInfo() {
        return assemblyInfo;
    }

    /**
     * Private copy of the default Synonym lookup
     *
     * @see SynonymLookup#getDefaultLookup()
     */
//    private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
    public static final String LOADING_MESSAGE_PREFIX = "Loading data set ";

    public static DataContainer getLocalFileDataContainer(GenomeVersion genomeVersion, String speciesName) {
        String versionName = genomeVersion.getName();
        if (speciesName == null) {
            speciesName = "-- Unknown -- " + versionName;	// make it distinct, but also make it appear at the top of the species list
        }

        for (DataContainer dataContainer : genomeVersion.getAvailableDataContainers()) {
            if (dataContainer.getDataProvider() instanceof LocalDataProvider) {
                return dataContainer;
            }
        }

        return retrieveDataContainer(genomeVersion.getLocalDataSetProvider(), speciesName, versionName, true, genomeVersion.getGenomeVersionSynonymLookup());
    }

    public static synchronized DataContainer retrieveDataContainer(DataProvider dataProvider, String speciesName, String versionName, boolean immediatelyRefresh, GenomeVersionSynonymLookup genomeVersionSynonymLookup) {
        // Make sure we use the preferred synonym for the genome version.
        String preferredVersionName = genomeVersionSynonymLookup.getPreferredName(versionName);
        GenomeVersion genomeVersion = gmodel.addGenomeVersion(preferredVersionName); // returns existing genomeVersion if found, otherwise creates a new genomeVersion
        DataContainer dataContainer = new DataContainer(genomeVersion, dataProvider);
        boolean isNewSpecies = !getLoadedSpeciesNames().contains(speciesName);
        Set<DataContainer> dataContainers = speciesDataContainerReference.get(speciesName);
        versionName2species.put(preferredVersionName, speciesName);
        dataContainer.getGenomeVersion().setSpeciesName(speciesName);
        if (!dataContainers.contains(dataContainer)) {
            dataContainers.add(dataContainer);
        }
        if (isNewSpecies && immediatelyRefresh) {
            if (SeqGroupView.getInstance() != null) { //ugly
                SeqGroupView.getInstance().refreshSpeciesCB();
            }
        }
        genomeVersion.addDataContainer(dataContainer);
        return dataContainer;
    }

    public static Set<DataContainer> getDataContainersForSpecies(String speciesName) {
        return ImmutableSet.copyOf(speciesDataContainerReference.get(speciesName));
    }

    public static Set<String> getLoadedSpeciesNames() {
        return ImmutableSet.copyOf(speciesDataContainerReference.keySet());
    }

    public static Optional<DataSet> findFeatureFromUri(URI featurePath) {
        checkNotNull(featurePath);
        return GeneralLoadUtils.getAllDataSets().stream()
                .filter(feature -> feature.getURI().equals(featurePath)).findFirst();
    }

    /**
     * Returns the list of features for the genome with the given version name. The list may (rarely) be empty, but never null.
     */
    public static List<DataSet> getDataSets(GenomeVersion genomeVersion) {
        // There may be more than one server with the same versionName.  Merge all the version names.
        List<DataSet> featureList = new ArrayList<>();
        if (genomeVersion != null) {
            Optional.ofNullable(genomeVersion.getDataContainers()).ifPresent(versions -> {
                versions.stream()
                        .flatMap(version -> version.getDataSets()
                        .stream()).forEach(featureList::add);
            });
        }
        return featureList;
    }

    public static Set<DataSet> getAllDataSets() {
        Set<DataSet> featureList = Sets.newHashSet();
        GenometryModel.getInstance().getSeqGroups().values().stream()
                .flatMap(genomeVersion -> GeneralLoadUtils.getDataSets(genomeVersion).stream()).forEach(featureList::add);
        return featureList;
    }

    /**
     * Only want to display features with visible attribute set to true.
     *
     * @return list of visible features
     */
    public static List<DataSet> getVisibleFeatures() {
        List<DataSet> visibleFeatures = new ArrayList<>();
        GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
        for (DataSet dataSet : getDataSets(genomeVersion)) {
            if (dataSet.isVisible() && !dataSet.isReferenceSequence()) {
                visibleFeatures.add(dataSet);
            }
        }
        return visibleFeatures;
    }

    /*
     * Returns the list of features for currently selected genomeVersion.
     */
    public static List<DataSet> getGenomeVersionDataSets() {
        GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
        return getDataSets(genomeVersion);
    }

    /**
     * Returns the list of servers associated with the given versions.
     *
     * @param dataSets -- assumed to be non-null.
     * @return A list of servers associated with the given versions.
     */
    public static List<DataProvider> getServersWithAssociatedFeatures(List<DataSet> dataSets) {
        List<DataProvider> serverList = new ArrayList<>();
        dataSets.stream().filter(dataSet -> !serverList.contains(dataSet.getDataContainer().getDataProvider())).forEach(gFeature -> {
            serverList.add(gFeature.getDataContainer().getDataProvider());
        });
        // make sure these servers always have the same order

        Collections.sort(serverList, new DataProviderComparator());
        return serverList;
    }

    private static boolean loadGeneomeVersionAssemblyInfo(DataContainer dataContainer, ChromosomeSynonymLookup chromosomeLookup) {
        if (dataContainer.getGenomeVersion().getSeqList().isEmpty()) {
            DataProvider dataProvider = dataContainer.getDataProvider();
            if (dataProvider instanceof AssemblyProvider) {
                AssemblyProvider assemblyProvider = (AssemblyProvider) dataProvider;
                assemblyProvider.getChromosomeSynonyms(dataContainer).ifPresent(chromosomeSynonyms -> {
                    chromosomeSynonyms.keySet().stream().forEach(key -> {
                        chromosomeLookup.getPreferredNames().add(key);
                        chromosomeLookup.addSynonyms(Sets.newConcurrentHashSet(chromosomeSynonyms.get(key)));
                    });
                });
                assemblyInfo = assemblyProvider.getAssemblyInfo(dataContainer.getGenomeVersion());
                GenomeVersion genomeVersion = dataContainer.getGenomeVersion();
                assemblyInfo.entrySet().stream().forEach(entry -> {
                    genomeVersion.addSeq(entry.getKey(), entry.getValue());
                });
                return !assemblyInfo.isEmpty();
            }
        }
        return false;
    }

    /**
     * Load the annotations for the given version. This is specific to one server.
     *
     * @param dataContainer
     */
    private static void initializeDataContainer(final DataContainer dataContainer) {
        if (!dataContainer.getDataSets().isEmpty()) {
            return;
        }
        DataProvider dataProvider = dataContainer.getDataProvider();
        Set<DataSet> availableDataSets = dataProvider.getAvailableDataSets(dataContainer);
        availableDataSets.stream().forEach(dataSet -> {
            dataContainer.addDataSet(dataSet);
        });
    }

    /**
     * Make sure this genome version has been initialized.
     *
     * @param versionName
     */
    public static void initVersionAndSeq(String versionName) {
        GenomeVersion genomeVersion = gmodel.getSeqGroup(versionName);
        final Set<DataContainer> availableDataContainers = genomeVersion.getAvailableDataContainers();
        List<DataContainer> sortedContainers = Lists.newArrayList();
        sortedContainers.addAll(availableDataContainers);
        Collections.sort(sortedContainers, DATA_CONTAINER_PRIORITY_COMPARATOR);
        sortedContainers.stream()
                .filter(gv -> gv.getName().equals(versionName))
                .filter(dataContainer -> !dataContainer.isInitialized())
                .forEach(dataContainer -> {
                    loadGeneomeVersionAssemblyInfo(dataContainer, genomeVersion.getChrSynLookup());
                    initializeDataContainer(dataContainer);
                    dataContainer.setInitialized();
                });

        addGenomeVirtualSeq(genomeVersion);	// okay to run this multiple times
    }

    private static void addGenomeVirtualSeq(GenomeVersion genomeVersion) {
        int chrom_count = genomeVersion.getSeqCount();
        if (chrom_count <= 1) {
            // no need to make a virtual "genome" chrom if there is only a single chromosome
            return;
        }

        int spacer = determineSpacer(genomeVersion, chrom_count);
        double seqBounds = determineSeqBounds(genomeVersion, spacer, chrom_count);
        if (seqBounds > Integer.MAX_VALUE) {
            return;
        }
        if (genomeVersion.getSeq(IGBConstants.GENOME_SEQ_ID) != null) {
            return; // return if we've already created the virtual genome
        }

        BioSeq genome_seq = null;
        try {
            genome_seq = genomeVersion.addSeq(IGBConstants.GENOME_SEQ_ID, 0);
        } catch (IllegalStateException ex) {
            // due to multithreading, it's possible that this sequence has been created by another thread while doing this test.
            // we can safely return in this case.
            LOG.trace("Ignoring multithreading illegal state exception.");
            return;
        }

        for (int i = 0; i < chrom_count; i++) {
            BioSeq chrom_seq = genomeVersion.getSeq(i);
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
    private static int determineSpacer(GenomeVersion genomeVersion, int chrom_count) {
        double spacer = 0;
        for (BioSeq chrom_seq : genomeVersion.getSeqList()) {
            spacer += (chrom_seq.getLengthDouble()) / chrom_count;
        }
        return (int) (spacer / MAGIC_SPACER_NUMBER);
    }

    /**
     * Make sure virtual genome doesn't overflow integer bounds.
     *
     * @param genomeVersion
     * @return true or false
     */
    private static double determineSeqBounds(GenomeVersion genomeVersion, int spacer, int chrom_count) {
        double seq_bounds = default_genome_min;

        for (int i = 0; i < chrom_count; i++) {
            BioSeq chrom_seq = genomeVersion.getSeq(i);
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

    protected static void bufferDataForAutoload() {
        SeqSpan visible = gviewer.getVisibleSpan();
        Optional<BioSeq> selectedSeq = gmodel.getSelectedSeq();

        if (visible == null || !selectedSeq.isPresent()) {
            return;
        }
        BioSeq seq = selectedSeq.get();

        int length = visible.getLength();
        int min = visible.getMin();
        int max = visible.getMax();
        SeqSpan leftSpan = new SimpleSeqSpan(Math.max(0, min - length), min, seq);
        SeqSpan rightSpan = new SimpleSeqSpan(max, Math.min(seq.getLength(), max + length), seq);

        for (DataSet gFeature : GeneralLoadUtils.getGenomeVersionDataSets()) {
            if (gFeature.getLoadStrategy() != LoadStrategy.AUTOLOAD) {
                continue;
            }

            if (checkBeforeLoading(gFeature)) {
                loadAndDisplaySpan(leftSpan, gFeature);
                loadAndDisplaySpan(rightSpan, gFeature);
            }
        }
    }

    private static boolean checkBeforeLoading(DataSet gFeature) {
        if (gFeature.getLoadStrategy() == LoadStrategy.NO_LOAD) {
            return false;	// should never happen
        }

//		Thread may have been cancelled. So removing test for now.
//		//Already loaded the data.
//		if((gFeature.dataContainer.gServer.serverType == ServerType.LocalFiles)
//				&& ((QuickLoad)gFeature.symL).getSymLoader() instanceof SymLoaderInstNC){
//			return false;
//		}
        BioSeq selected_seq = gmodel.getSelectedSeq().orElse(null);
        BioSeq visible_seq = gviewer.getViewSeq();
        if ((selected_seq == null || visible_seq == null)) {
            //      ErrorHandler.errorPanel("ERROR", "You must first choose a sequence to display.");
            //System.out.println("@@@@@ selected chrom: " + selected_seq);
            //System.out.println("@@@@@ visible chrom: " + visible_seq);
            return false;
        }
        if (visible_seq != selected_seq) {
            System.out.println("ERROR, VISIBLE SPAN DOES NOT MATCH GMODEL'S SELECTED SEQ!!!");
            System.out.println("   selected seq: " + selected_seq.getId());
            System.out.println("   visible seq: " + visible_seq.getId());
            return false;
        }

        return true;
    }

    /**
     * Load and display annotations (requested for the specific feature). Adjust the load status accordingly.
     */
    static public void loadAndDisplayAnnotations(DataSet gFeature) {
        if (!checkBeforeLoading(gFeature)) {
            return;
        }

        BioSeq selected_seq = gmodel.getSelectedSeq().orElse(null);
        if (selected_seq == null) {
            ErrorHandler.errorPanel("Couldn't find genome data on server for file, genome = " + gFeature.getDataContainer().getGenomeVersion().getName());
            return;
        }
        SeqSpan overlap = null;
        if (gFeature.getLoadStrategy() == LoadStrategy.VISIBLE || gFeature.getLoadStrategy() == LoadStrategy.AUTOLOAD) {
            overlap = gviewer.getVisibleSpan();
            // TODO: Investigate edge case at max
            if (overlap.getMin() == selected_seq.getMin() && overlap.getMax() == selected_seq.getMax()) {
                overlap = new SimpleSeqSpan(selected_seq.getMin(), selected_seq.getMax() - 1, selected_seq);
            }
        } else if (gFeature.getLoadStrategy() == LoadStrategy.GENOME /*
                 * || gFeature.getLoadStrategy() == LoadStrategy.CHROMOSOME
                 */) {
            // TODO: Investigate edge case at max
            overlap = new SimpleSeqSpan(selected_seq.getMin(), selected_seq.getMax() - 1, selected_seq);
        }

        loadAndDisplaySpan(overlap, gFeature);
    }

    public static void loadAndDisplaySpan(final SeqSpan span, final DataSet dataSource) {
        SeqSymmetry optimized_sym = null;
//        // special-case chp files, due to their LazyChpSym DAS/2 loading
//        if (dataSource.getDataContainer().getDataProvider() instanceof DataSetProvider
//                && ((QuickLoadSymLoader) dataSource.getSymL()).extension.endsWith("chp")) {
//            dataSource.setLoadStrategy(LoadStrategy.GENOME);	// it should be set to this already.  But just in case...
//            optimized_sym = new SimpleMutableSeqSymmetry();
//            ((SimpleMutableSeqSymmetry) optimized_sym).addSpan(span);
//            loadFeaturesForSym(optimized_sym, dataSource);
//            return;
//        }

        optimized_sym = dataSource.optimizeRequest(span);

        if (dataSource.getLoadStrategy() != LoadStrategy.GENOME) {//|| dataSource.getDataContainer().getDataProvider().getServerType() == DasServerType.getInstance()) {
            // Don't iterate for DAS/2.  "Genome" there is used for autoloading.

            if (checkBamAndSamLoading(dataSource, optimized_sym)) {
                return;
            }

            loadFeaturesForSym(optimized_sym, dataSource);
            return;
        }
        //Since Das1 does not have whole genome return if it is not Quickload or LocalFile
//        if (dataSource.getDataContainer().getDataProvider().getServerType() != QuickloadServerType.getInstance() && dataSource.getDataContainer().getDataProvider().getServerType() != LocalFilesServerType.getInstance()) {
//            return;
//        }
        //If Loading whole genome for unoptimized file then load everything at once.
        if (dataSource.getSymL() instanceof SymLoaderInst) {
            if (optimized_sym != null) {
                loadAllSymmetriesThread(dataSource);
            }
            return;
        }
        iterateSeqList(dataSource, false);
    }

    static void iterateSeqList(final DataSet feature, boolean isUCSCRestRefAutoload) {

        CThreadWorker<Void, BioSeq> worker = new CThreadWorker<Void, BioSeq>(
                MessageFormat.format(LOADING_FEATURE_MESSAGE, feature.getDataSetName())) {

            @Override
            protected Void runInBackground() {
                final Stopwatch stopwatch = Stopwatch.createStarted();
                try {
                    List<BioSeq> chrList = gmodel.getSelectedGenomeVersion().getSeqList();
//                            Collections.sort(chrList,
//                                    new Comparator<BioSeq>() {
//                                        @Override
//                                        public int compare(BioSeq s1, BioSeq s2) {
//                                            return s1.getName().compareToIgnoreCase(s2.getName());
//                                        }
//                                    });
                    if (!isUCSCRestRefAutoload && feature.getSymL().isMultiThreadOK()) {
                        return multiThreadedLoad(chrList);
                    }
                    return singleThreadedLoad(chrList, isUCSCRestRefAutoload);
                } catch (Throwable ex) {
                    LOG.error(
                            "Error while loading feature", ex);
                    return null;
                } finally {
                    stopwatch.stop();
                    LOG.info("Loaded {} in {}", feature.getDataSetName(), stopwatch);
                }
            }

            protected Void singleThreadedLoad(List<BioSeq> chrList, boolean isUCSCRestRefAutoload) throws Exception {
                BioSeq current_seq = gmodel.getSelectedSeq().orElse(null);

                if(current_seq == null && isUCSCRestRefAutoload && !chrList.isEmpty())
                    current_seq = chrList.get(0);

                if (current_seq != null) {
                    loadOnSequence(current_seq);
                    publish(current_seq);
                }

                if (!isUCSCRestRefAutoload){
                    for (final BioSeq seq : chrList) {
                        if (seq == current_seq) {
                            continue;
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        loadOnSequence(seq);
                    }
                }
                return null;
            }

            ExecutorService internalExecutor;

            protected Void multiThreadedLoad(List<BioSeq> chrList) throws Exception {
                internalExecutor = Executors.newFixedThreadPool(MAX_INTERNAL_THREAD);

                final BioSeq current_seq = gmodel.getSelectedSeq().orElse(null);

                if (current_seq != null) {
                    internalExecutor.submit(() -> {
                        loadOnSequence(current_seq);
                        publish(current_seq);
                    });
                }

                for (final BioSeq seq : chrList) {
                    if (seq == current_seq) {
                        continue;
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    internalExecutor.submit(() -> loadOnSequence(seq));
                }
                internalExecutor.shutdown();
                try {
                    internalExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    LOG.warn("Internal executor exception", ex);
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
                BioSeq selectedSeq = gmodel.getSelectedSeq().orElse(null);
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

                BioSeq seq = gmodel.getSelectedSeq().orElse(null);
                if (seq != null) {
                    gviewer.setAnnotatedSeq(seq, true, true);
                } else if (gmodel.getSelectedGenomeVersion() != null) {
                    if (gmodel.getSelectedGenomeVersion().getSeqCount() > 0) {
                        // This can happen when loading a brand-new genome
                        gmodel.setSelectedSeq(gmodel.getSelectedGenomeVersion().getSeq(0));
                    }
                }
                GeneralLoadView.getLoadView().refreshDataManagementView();
            }

            private void loadOnSequence(BioSeq seq) {
                if (IGBConstants.GENOME_SEQ_ID.equals(seq.getId())) {
                    return; // don't load into Whole Genome
                }

                try {
                    SeqSymmetry optimized_sym = feature.optimizeRequest(new SimpleSeqSpan(seq.getMin(), seq.getMax() - 1, seq));
                    if (optimized_sym != null) {
                        loadFeaturesForSym(feature, optimized_sym);
                    }
                } catch (Exception ex) {
                    LOG.error("Error in loadOnSequence", ex);
                    if (ex instanceof FileNotFoundException) {
                        ErrorHandler.errorPanel(feature.getDataSetName() + " not Found", "The server is no longer available. Please refresh the server from Preferences > Data Sources or try again later.", Level.SEVERE);
                    }
                }
            }
        };

        CThreadHolder.getInstance().execute(feature, worker);
    }

    private static void loadFeaturesForSym(final SeqSymmetry optimized_sym, final DataSet dataSet) throws OutOfMemoryError {
        if (optimized_sym == null) {
            LOG.debug("All of new query covered by previous queries for feature {}", dataSet.getDataSetName());
            return;
        }

        final int seq_count = gmodel.getSelectedGenomeVersion().getSeqCount();
        final CThreadWorker<Map<String, List<? extends SeqSymmetry>>, Object> worker
                = new CThreadWorker<Map<String, List<? extends SeqSymmetry>>, Object>(LOADING_MESSAGE_PREFIX + dataSet.getDataSetName(), Thread.MIN_PRIORITY) {

            @Override
            protected Map<String, List<? extends SeqSymmetry>> runInBackground() {
                try {
                    return loadFeaturesForSym(dataSet, optimized_sym);
                } catch (RuntimeException ex) {
                    LOG.error(ex.getMessage(), ex);
                } catch (Exception ex) {
                    if (ex instanceof FileNotFoundException) {
                        ErrorHandler.errorPanel(dataSet.getDataSetName() + " not Found", "The server is no longer available. Please refresh the server from Preferences > Data Sources or try again later.", Level.SEVERE);
                    }
                } catch (Throwable ex) {
                    LOG.error(ex.getMessage(), ex);
                }
                return Collections.<String, List<? extends SeqSymmetry>>emptyMap();
            }

            @Override
            protected void finished() {

                BioSeq aseq = gmodel.getSelectedSeq().orElse(null);

                if (aseq != null) {
                    gviewer.setAnnotatedSeq(aseq, true, true);
                } else if (gmodel.getSelectedGenomeVersion() != null && gmodel.getSelectedGenomeVersion().getSeqCount() > 0) {
                    // This can happen when loading a brand-new genome
                    aseq = gmodel.getSelectedGenomeVersion().getSeq(0);
                    gmodel.setSelectedSeq(aseq);
                }

                //Since sequence are never removed so if no. of sequence increases then refresh sequence table.
                if (gmodel.getSelectedGenomeVersion() != null && gmodel.getSelectedGenomeVersion().getSeqCount() > seq_count) {
                    SeqGroupView.getInstance().refreshTable();
                }

                GeneralLoadView.getLoadView().refreshDataManagementView();
            }
        };

        CThreadHolder.getInstance().execute(dataSet, worker);
    }

    //TO DO: Make this private again.
    public static Map<String, List<? extends SeqSymmetry>> loadFeaturesForSym(
            DataSet dataSet, SeqSymmetry optimized_sym) throws OutOfMemoryError, Exception {
        if (dataSet.getDataContainer().getDataProvider() == null) {
            return Collections.<String, List<? extends SeqSymmetry>>emptyMap();
        }

        List<SeqSpan> optimized_spans = new ArrayList<>();
        SeqUtils.convertSymToSpanList(optimized_sym, optimized_spans);
        Map<String, List<? extends SeqSymmetry>> loaded = new HashMap<>();

        for (SeqSpan optimized_span : optimized_spans) {

            //TODO - remove quickloadsymloader and make general utils class of some sort
            QuickLoadSymLoader symLoader;
            if (!(dataSet.getSymL() instanceof QuickLoadSymLoader)) {
                symLoader = new QuickLoadSymLoader(dataSet.getURI(), dataSet.getIndex(), dataSet.getDataSetName(), dataSet.getSymL(), dataSet.getDataContainer().getGenomeVersion());
            } else {
                symLoader = (QuickLoadSymLoader) dataSet.getSymL();
            }
            Map<String, List<? extends SeqSymmetry>> results = symLoader.loadFeatures(optimized_span, dataSet);

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

    private static boolean checkBamAndSamLoading(DataSet feature, SeqSymmetry optimized_sym) {
        //start max
        boolean check = GeneralLoadView.getLoadView().isLoadingConfirm();
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

                GeneralLoadView.getLoadView().setShowLoadingConfirm(!check);
                return !(ModalUtils.confirmPanel(message,
                        PreferenceUtils.CONFIRM_BEFORE_LOAD, PreferenceUtils.default_confirm_before_load));
            }
        }
        return false;
    }

    /**
     * Get residues from servers: DAS/2, Quickload, or DAS/1. Also gets partial residues.
     *
     * @param genomeVersionName -- name of the genome.
     * @param span	-- May be null. If not, then it's used for partial loading.
     */
    // Most confusing thing here -- certain parsers update the composition, and certain ones do not.
    // DAS/1 and partial loading in DAS/2 do not update the composition, so it's done separately.
    private static boolean getResidues(Set<DataContainer> versionsWithChrom, SeqSpan span) {
        BioSeq bioSeq = span.getBioSeq();
        String bioSeqId = bioSeq.getId();
        boolean residuesLoaded = false;

        List<DataContainer> sortedContainers = Lists.newArrayList();
        sortedContainers.addAll(versionsWithChrom);
        Collections.sort(sortedContainers, DATA_CONTAINER_PRIORITY_COMPARATOR);
        for (DataContainer dataContainer : sortedContainers) {
            final DataProvider dataProvider = dataContainer.getDataProvider();
            if (dataProvider.getStatus() == ResourceStatus.Disabled
                    || dataProvider.getStatus() == ResourceStatus.NotResponding) {
                continue;
            }
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }

            String serverDescription = dataProvider.getName() + " " + dataProvider.getUrl();

            try {
                if (dataProvider instanceof LocalDataProvider) {
                    LocalDataProvider dataSetProvider = (LocalDataProvider) dataProvider;
                    if (dataSetProvider.isContainsReferenceSequenceData()) {
                        Optional<DataSet> refSeqDataSet = dataContainer.getDataSets().stream()
                                .filter(ds -> ds.isReferenceSequence())
                                .findFirst();
                        if (refSeqDataSet.isPresent()) {
                            residuesLoaded = loadReferenceSequenceFromUri(refSeqDataSet.get().getURI(), refSeqDataSet.get().getIndex(), dataContainer, span, residuesLoaded);

                        }
                    }
                }
                if (!residuesLoaded && dataProvider instanceof ReferenceSequenceResource) {
                    if (dataProvider instanceof ReferenceSequenceDataSetProvider) {
                        ReferenceSequenceDataSetProvider sequenceDataSetProvider = ReferenceSequenceDataSetProvider.class.cast(dataProvider);
                        Optional<URI> sequenceFileUri = sequenceDataSetProvider.getSequenceFileUri(dataContainer.getGenomeVersion());
                        if (sequenceFileUri.isPresent()) {
                            final URI uri = sequenceFileUri.get();
                            residuesLoaded = loadReferenceSequenceFromUri(uri, Optional.empty(), dataContainer, span, residuesLoaded);
                        }
                    }
                    if (dataProvider instanceof ReferenceSequenceProvider) {
                        ReferenceSequenceProvider rsp = ReferenceSequenceProvider.class.cast(dataProvider);
                        String sequence = rsp.getSequence(dataContainer, span);
                        if (!Strings.isNullOrEmpty(sequence)) {
                            BioSeqUtils.addResiduesToComposition(span.getBioSeq(), sequence, span);
                            residuesLoaded = true;
                        }
                    }
                }
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
                residuesLoaded = false;
            }

            if (residuesLoaded) {
                IGB.getInstance().setStatus(MessageFormat.format(IGBConstants.BUNDLE.getString("completedLoadingSequence"), bioSeqId, span.getMin(), span.getMax(), serverDescription));
                return true;
            }
        }
        IGB.getInstance().setStatus("");
        return false;
    }

    private static boolean loadReferenceSequenceFromUri(final URI uri, final Optional<URI> indexUri, DataContainer dataContainer, SeqSpan span, boolean residuesLoaded) throws Exception {
        SymLoader syml = null;
        if (SymLoader.getExtension(uri).equalsIgnoreCase("2bit")) {
            syml = new TwoBitNew(uri, indexUri, "", dataContainer.getGenomeVersion());
        } else {
            syml = ServerUtils.determineLoader(SymLoader.getExtension(uri), uri, indexUri, detemineFriendlyName(uri), dataContainer.getGenomeVersion());
        }
        String regionResidues = syml.getRegionResidues(span);
        if (!Strings.isNullOrEmpty(regionResidues)) {
            BioSeqUtils.addResiduesToComposition(span.getBioSeq(), regionResidues, span);
            residuesLoaded = true;
        }
        return residuesLoaded;
    }

    static boolean loadResidues(SeqSpan span) {
        BioSeq aseq = span.getBioSeq();
        int min = span.getMin();
        int max = span.getMax();
        // Determine list of servers that might have this chromosome sequence.
        Set<DataContainer> versionsWithChrom = new HashSet<>();

        versionsWithChrom.addAll(aseq.getGenomeVersion().getAvailableDataContainers());

        if ((min <= 0) && (max >= aseq.getLength())) {
            min = 0;
            max = aseq.getLength();
        }

        if (aseq.isAvailable(min, max)) {
            LOG.info(
                    "All residues in range are already loaded on sequence {}", new Object[]{aseq});
            return true;
        }

        return getResidues(versionsWithChrom, span);
    }
    public static boolean loadResidues(BioSeq seq, int start, int end){
        Set<DataContainer> versionsWithChrom = new HashSet<>();
        versionsWithChrom.addAll(seq.getGenomeVersion().getAvailableDataContainers());
        final SimpleSeqSpan simpleSeqSpan = new SimpleSeqSpan(start, end, seq);
        return getResidues(versionsWithChrom, simpleSeqSpan);
    }

    /**
     * Get synonyms of version.
     *
     * @param versionName - version name
     * @return a friendly HTML string of version synonyms (not including versionName).
     */
    public static String listSynonyms(String versionName, GenomeVersionSynonymLookup genomeVersionSynonymLookup) {
        StringBuilder synonymBuilder = new StringBuilder(100);
        synonymBuilder.append("<html>").append(IGBConstants.BUNDLE.getString("synonymList"));
        Set<String> synonymSet = genomeVersionSynonymLookup.getSynonyms(versionName);
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
     * Set autoload variable in features.
     *
     * @param autoload
     */
    public static void setFeatureAutoLoad(boolean autoload) {
        for (DataContainer genericVersion : speciesDataContainerReference.values()) {
            genericVersion.getDataSets().stream().filter(genericFeature -> autoload).forEach(genericFeature -> {
                genericFeature.setAutoload(autoload);
            });
        }

        //It autoload data is selected then load.
        if (autoload) {
            GeneralLoadView.loadWholeRangeFeatures();
            GeneralLoadView.getLoadView().refreshTreeView();
            GeneralLoadView.getLoadView().refreshDataManagementView();
        }
    }

    public static List<String> getSpeciesList() {
        return Ordering.from(String.CASE_INSENSITIVE_ORDER).immutableSortedCopy(speciesDataContainerReference.keySet());
    }

    public static void openURI(URI uri, Optional<URI> indexUri, String fileName, GenomeVersion genomeVersion, String speciesName, boolean isReferenceSequence) {
        // If server requires authentication then.
        // If it cannot be authenticated then don't add the feature.
        if (!LocalUrlCacher.isValidURI(uri)) {
            ErrorHandler.errorPanel("UNABLE TO FIND URL", uri + "\n URL provided not found or times out: ", Level.WARNING);
            return;
        }

        getDataSet(uri, indexUri, fileName, speciesName, genomeVersion, isReferenceSequence).ifPresent(dataSet -> {
            addDataSet(dataSet);
        });

    }

    public static void addDataSet(DataSet dataSet) {
        if (dataSet.getSymL() != null) {
            addChromosomesForUnknownGroup(dataSet);
        }

        // force a refresh of this server
        SeqGroupView.getInstance().setSelectedGenomeVersion(dataSet.getDataContainer().getGenomeVersion());
        GeneralLoadView.getLoadView().refreshTreeView();
        GeneralLoadView.getLoadView().refreshDataManagementView();
    }

    private static void addChromosomesForUnknownGroup(final DataSet dataSet) {
        if (dataSet.getSymL() instanceof QuickLoadSymLoader) {
            if (((QuickLoadSymLoader) dataSet.getSymL()).getSymLoader() instanceof SymLoaderInstNC) {
                loadAllSymmetriesThread(dataSet);
                // force a refresh of this server. This forces creation of 'genome' sequence.
//            DataProviderManager.getInstance().fireServerInitEvent(DataProviderManager.getInstance().getLocalFilesServer(), ResourceStatus.Initialized, true);
                return;
            }
        }
        final GenomeVersion loadGroup = dataSet.getDataContainer().getGenomeVersion();
        final String message = MessageFormat.format(IGBConstants.BUNDLE.getString("retrieveChr"), dataSet.getDataSetName());
        final CThreadWorker<Boolean, Object> worker = new CThreadWorker<Boolean, Object>(message) {
            boolean featureRemoved = false;

            @Override
            protected Boolean runInBackground() {
                String message = "IGB is unable to load the data in your file.<br>Error message: ";
                String helpMessage = "<br>More information about what went wrong may be available in the Console. <br>To get help, visit the ";
                String linkName = "IGB Help Page";
                String link = "https://bioviz.org/help.html";
                try {
                    for (BioSeq seq : dataSet.getSymL().getChromosomeList()) {
                        loadGroup.addSeq(seq.getId(), seq.getLength(), dataSet.getSymL().uri.toString());
                    }
                    return true;
                } catch (NumberFormatException nfe) {
                    ((QuickLoadSymLoader) dataSet.getSymL()).logException(nfe);

                    featureRemoved = removeFeatureAndRefresh(dataSet, message + "The input string " + nfe.getMessage().split(":")[1] + " should be numeric." + helpMessage, linkName, link);
                    return featureRemoved;

                } catch (Exception ex) {
                    ((QuickLoadSymLoader) dataSet.getSymL()).logException(ex);

                    featureRemoved = removeFeatureAndRefresh(dataSet, message + ex.getMessage() + helpMessage, linkName, link);
                    return featureRemoved;
                }

            }

            @Override
            protected boolean showCancelConfirmation() {
                return removeFeature("Cancel chromosome retrieval and remove " + dataSet.getDataSetName() + "?");
            }

            private boolean removeFeature(String msg) {
                if (ModalUtils.confirmPanel(msg)) {
                    if (dataSet.getDataContainer().removeDataSet(dataSet)) {
                        SeqGroupView.getInstance().refreshTable();
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
                    LOG.error(null, ex);
                }
                if (result) {
                    GeneralLoadView.addFeatureTier(dataSet);
                    SeqGroupView.getInstance().refreshTable();
                    if (loadGroup.getSeqCount() > 0 && gmodel.getSelectedSeq().orElse(null) == null) {
                        // select a chromosomes
                        gmodel.setSelectedSeq(loadGroup.getSeq(0));
                    }
                } else {
                    gmodel.setSelectedSeq(gmodel.getSelectedSeq().orElse(null));
                }
//                DataProviderManager.getInstance().fireServerInitEvent(DataProviderManager.getInstance().getLocalFilesServer(), ResourceStatus.Initialized, true);
                if (dataSet.getLoadStrategy() == LoadStrategy.VISIBLE && !featureRemoved) {
                    if (dataSet.isReferenceSequence()) {
                        JLabel label = new JLabel(DataSet.REFERENCE_SEQUENCE_LOAD_MESSAGE);
                        ModalUtils.infoPanel(label);
                    } else {
                        ModalUtils.infoPanel(DataSet.LOAD_WARNING_MESSAGE,
                                DataSet.show_how_to_load, DataSet.default_show_how_to_load);
                    }
                }
            }
        };
        CThreadHolder.getInstance().execute(dataSet, worker);
    }

    private static boolean removeFeatureAndRefresh(DataSet gFeature, String msg, String linkName, String link) {
        if (ModalUtils.confirmPanel(msg, linkName, link)) {
            GeneralLoadView.getLoadView().removeDataSet(gFeature, true);
            return true;
        }
        return false;
    }

    private static boolean removeFeatureAndRefresh(DataSet gFeature, String msg) {
        if (ModalUtils.confirmPanel(msg)) {
            GeneralLoadView.getLoadView().removeDataSet(gFeature, true);
            return true;
        }
        return false;
    }

    private static Optional<DataSet> getDataSet(URI uri, Optional<URI> indexUri, String fileName, String speciesName, GenomeVersion genomeVersion, boolean isReferenceSequence) {
        List<DataSet> visibleFeatures = GeneralLoadUtils.getVisibleFeatures();
        Optional<DataSet> loadedDataSet = GeneralLoadUtils.getLoadedDataSet(uri, visibleFeatures);

        // Test to determine if a feature with this uri is contained in the load mode table
        if (!loadedDataSet.isPresent()) {
            DataContainer dataContainer = GeneralLoadUtils.getLocalFileDataContainer(genomeVersion, speciesName);
            dataContainer = setVersion(uri, genomeVersion, dataContainer);
            if (isReferenceSequence) {
                genomeVersion.getLocalDataSetProvider().setContainsReferenceSequenceData(true);
            }
            // In case of BAM
            if (dataContainer == null) {
                return null;
            }

            // handle URL case.
            String uriString = uri.toString();
            int httpIndex = uriString.toLowerCase().indexOf(HTTP_PROTOCOL);
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
            SymLoader symL = ServerUtils.determineLoader(SymLoader.getExtension(uri), uri, indexUri, QuickLoadSymLoader.detemineFriendlyName(uri), dataContainer.getGenomeVersion());
            if (symL != null && symL.isResidueLoader() && !isReferenceSequence) {
                featureProps = new HashMap<>();
                featureProps.put("collapsed", "true");
                featureProps.put("show2tracks", "false");
            }
            
            // Updated dataSet initialization
            DataSet dataSet = new DataSet(uri, fileName, featureProps, dataContainer, autoload, isReferenceSequence);
            dataContainer.addDataSet(dataSet);

            dataSet.setVisible(); // this should be automatically checked in the feature tree
            loadedDataSet = Optional.of(dataSet);

        } else {
            ErrorHandler.errorPanel("Cannot add same feature",
                    "The feature " + uri + " has already been added.", Level.WARNING);
        }

        return loadedDataSet;
    }

    /**
     * Handle file formats that has SeqGroup info.
     */
    private static DataContainer setVersion(URI uri, GenomeVersion loadGroup, DataContainer version) {
        String unzippedStreamName = GeneralUtils.stripEndings(uri.toString());
        String extension = GeneralUtils.getExtension(unzippedStreamName);
        boolean getNewVersion = false;

        switch (extension) {
            case BAM_EXT:
                try {
                    handleBam(uri);
                } catch (IOException ex) {
                    String errorMessage = MessageFormat.format(IGBConstants.BUNDLE.getString("bamIndexNotFound"), uri);
                    ErrorHandler.errorPanel("Cannot open file", errorMessage, Level.WARNING);
                    version = null;
                }
                break;
            case USEQ_EXT:
                loadGroup = handleUseq(uri, loadGroup);
                getNewVersion = true;
                break;
            case BAR_EXT:
                loadGroup = handleBar(uri, loadGroup);
                getNewVersion = true;
                break;
            case BP1_EXT:
            case BP2_EXT:
                loadGroup = handleBp(uri, loadGroup);
                getNewVersion = true;
                break;
        }

        if (getNewVersion) {
            DataContainer newVersion = getLocalFileDataContainer(loadGroup, loadGroup.getSpeciesName());
            if (GenometryModel.getInstance().getSelectedGenomeVersion() == null
                    || version == newVersion
                    || ModalUtils.confirmPanel(MessageFormat.format(IGBConstants.BUNDLE.getString("confirmGroupChange"),
                            version.getGenomeVersion().getSpeciesName(), version, newVersion.getGenomeVersion().getSpeciesName(), newVersion),
                            PreferenceUtils.CONFIRM_BEFORE_GROUP_CHANGE,
                            PreferenceUtils.default_confirm_before_group_change)) {
                version = newVersion;
            }
        }

        return version;
    }

    private static boolean handleBam(URI uri) throws IOException {
        return BamIndexValidator.bamFileHasIndex(uri);
    }

    /**
     * Get GenomeVersion for BAR file format.
     */
    private static GenomeVersion handleBar(URI uri, GenomeVersion genomeVersion) {
        InputStream istr = null;
        try {
            istr = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
            List<GenomeVersion> groups = BarParser.getSeqGroups(uri.toString(), istr, genomeVersion, gmodel);
            if (groups.isEmpty()) {
                return genomeVersion;
            }

            //TODO: What if there are more than one seq genomeVersion ?
            if (groups.size() > 1) {
                LOG.warn("File {} has more than one genomeVersion. Looking for the closest match to existing", new Object[]{uri.toString()});
                //First look for the selected genomeVersion in the groups
                for (GenomeVersion gr : groups) {
                    if (gr == genomeVersion) {
                        return gr;
                    }
                }

                //If it does not match any exiting genomeVersion the return the one that matches organism
                if (genomeVersion.getSpeciesName() != null && genomeVersion.getSpeciesName().length() != 0) {
                    for (GenomeVersion gr : groups) {
                        if (genomeVersion.getSpeciesName().equalsIgnoreCase(gr.getSpeciesName())) {
                            return gr;
                        }
                    }
                }

                //If it does not match organism then return the genomeVersion with most version
                GenomeVersion grp = groups.get(0);
                for (GenomeVersion gr : groups) {
                    if (gr.getDataContainers().size() > grp.getDataContainers().size()) {
                        grp = gr;
                    }
                }
                return grp;
            }

            //Return the first one
            return groups.get(0);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            GeneralUtils.safeClose(istr);
        }

        return genomeVersion;
    }

    /**
     * Get GenomeVersion for USEQ file format.
     */
    private static GenomeVersion handleUseq(URI uri, GenomeVersion genomeVersion) {
        InputStream istr = null;
        ZipInputStream zis = null;
        try {
            istr = LocalUrlCacher.getInputStream(uri.toURL());
            zis = new ZipInputStream(istr);
            zis.getNextEntry();
            ArchiveInfo archiveInfo = new ArchiveInfo(zis, false);
            GenomeVersion gr = USeqGraphParser.getSeqGroup(archiveInfo.getVersionedGenome(), gmodel);
            if (gr != null) {
                return gr;
            }
        } catch (Exception ex) {
            LOG.error("Exception in handleUseq method", ex);
        } finally {
            GeneralUtils.safeClose(istr);
            GeneralUtils.safeClose(zis);
        }

        return genomeVersion;
    }

    private static GenomeVersion handleBp(URI uri, GenomeVersion genomeVersion) {
        InputStream istr = null;
        try {
            istr = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
            GenomeVersion gr = Bprobe1Parser.getSeqGroup(istr, genomeVersion, gmodel);
            if (gr != null) {
                return gr;
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            GeneralUtils.safeClose(istr);
        }

        return genomeVersion;
    }

    /**
     * For unoptimized file formats load symmetries and add them.
     *
     * @param dataSet
     */
    public static void loadAllSymmetriesThread(final DataSet dataSet) {
        SymLoader symL = dataSet.getSymL();
        QuickLoadSymLoader loadSymLoader = new QuickLoadSymLoader(dataSet.getURI(), dataSet.getIndex(), dataSet.getDataSetName(), symL, dataSet.getDataContainer().getGenomeVersion());
        final SeqMapView gviewer = IGB.getInstance().getMapView();

        CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>(LOADING_MESSAGE_PREFIX + dataSet.getDataSetName()) {

            @Override
            protected Object runInBackground() {
                try {
                    loadSymLoader.loadAndAddAllSymmetries(dataSet);
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    String message = "IGB is unable to load the data in your file. One problem might be that the file format does not match IGB expectations. <br>More information about what went wrong may be available in the Console. To get help, visit the ";
                    String linkName = "IGB Help Page";
                    String link = "https://bioviz.org/help.html";
                    removeFeatureAndRefresh(dataSet, message, linkName, link);
                    //removeFeatureAndRefresh(dataSet, "Unable to load data set for this file. \nWould you like to remove this file from the list?");
                }
                return null;
            }

            @Override
            protected void finished() {
                try {
                    BioSeq aseq = GenometryModel.getInstance().getSelectedSeq().orElse(null);
                    if (aseq != null) {
                        gviewer.setAnnotatedSeq(aseq, true, true);
                    } else if (GenometryModel.getInstance().getSelectedSeq() == null && loadSymLoader.getAnnotatedSeqGroup() != null) {
                        // This can happen when loading a brand-new genome
                        GenometryModel.getInstance().setSelectedSeq(loadSymLoader.getAnnotatedSeqGroup().getSeq(0));
                    }

                    SeqGroupView.getInstance().refreshTable();
                    GeneralLoadView.getLoadView().refreshDataManagementView();
                } catch (Exception ex) {
                    LOG.error("Exception occurred while loading symmetries", ex);
                }
            }
        };

        CThreadHolder.getInstance().execute(dataSet, worker);
    }

    public static boolean isLoaded(DataSet dataSet, List<DataSet> visibleFeatures) {
        Optional<DataSet> loadedFeature = getLoadedDataSet(dataSet.getURI(), visibleFeatures);
        if (loadedFeature.isPresent()) {
            if (loadedFeature.get().getRequestSym().getChildCount() > 0) {
                return true;
            }
        }
        return false;
    }

    //This method is added as a more performant version of previous getLoadedFeature method, 
    //but its unclear why this even needs to exist and why the DataSet object doesn't just contain a 
    //equals/hashcode override to allow collections api method to be used (e.g. contains)
    public static Optional<DataSet> getLoadedDataSet(URI uri, List<DataSet> visibleFeatures) {
        return visibleFeatures.stream()
                .filter(dataSet -> dataSet.getURI().equals(uri)).findFirst();
    }

}
