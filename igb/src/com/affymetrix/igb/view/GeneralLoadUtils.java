package com.affymetrix.igb.view;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.MutableSeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.span.MutableDoubleSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.das.DasClientOptimizer;
import com.affymetrix.igb.das.DasDiscovery;
import com.affymetrix.igb.das.DasEntryPoint;
import com.affymetrix.igb.das.DasLoader;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das.DasSource;
import com.affymetrix.igb.das.DasType;
import com.affymetrix.igb.das2.Das2Discovery;
import com.affymetrix.igb.das2.Das2FeatureRequestSym;
import com.affymetrix.igb.das2.Das2Region;
import com.affymetrix.igb.das2.Das2SeqGroup;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.das2.Das2Source;
import com.affymetrix.igb.das2.Das2Type;
import com.affymetrix.igb.das2.Das2VersionedSource;
import com.affymetrix.igb.general.FeatureLoading;
import com.affymetrix.igb.general.GenericFeature;
import com.affymetrix.igb.general.GenericVersion;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.general.genericServer;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.menuitem.OpenGraphAction;
import com.affymetrix.igb.util.LocalUrlCacher;
import java.io.BufferedInputStream;
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
import java.util.regex.Pattern;
import javax.swing.text.Document;

public class GeneralLoadUtils {
    public static enum LoadStrategy { NO_LOAD, VISIBLE, WHOLE };    
    public static enum LoadStatus { UNLOADED, LOADING, LOADED };

    private static final boolean DEBUG=true;

    /**
     *  using negative start coord for virtual genome chrom because (at least for human genome)
     *     whole genome start/end/length can't be represented with positive 4-byte ints (limit is +/- 2.1 billion)
     */
//    final double default_genome_min = -2100200300;
    final double default_genome_min = 0;
    final boolean DEBUG_VIRTUAL_GENOME = true;

    Class server_type;  // Is the server Das (DasServerInfo), Das/2 (Das2ServerInfo), or Quickload?

    //public static final String PREF_QUICKLOAD_CACHE_RESIDUES = "quickload_cache_residues";
    //public static final String PREF_QUICKLOAD_CACHE_ANNOTS = "quickload_cache_annots";
    final static String ENCODE_FILE_NAME = "encodeRegions.bed";
    final static String ENCODE_FILE_NAME2 = "encode.bed";
    //static boolean CACHE_RESIDUES_DEFAULT = false;
    //static boolean CACHE_ANNOTS_DEFAULT = true;
    final SingletonGenometryModel gmodel;
    final SeqMapView gviewer;
    final static Pattern tab_regex = Pattern.compile("\t");
    String root_url;
    final SortedSet<String> genome_names;   // Genome names are unique even across multiple servers; thus we use a set instead of a list.
    final Map<AnnotatedSeqGroup,GenericVersion> group2version;
    private final Map<String,Boolean> version2init;

    /**
     *  Map of AnnotatedSeqGroup to a load state map.
     *  Each load state map is a map of an annotation type name to Boolean for
     *  whether it has already been loaded or not
     */
    static Map<AnnotatedSeqGroup,Map<String,Integer>> group2states;

    // server name-> genericServer class.
    final Map<String,genericServer> discoveredServers;

    // versions associated with a given genome.
    final Map<String,List<GenericVersion>> genome2genericVersionList;
    final Map<String,String> versionName2genome;
    final Map<String,Set<GenericVersion>> versionName2versionSet;

    //public boolean allow_reinitialization = true;


    public void clear() {
        group2states.clear();
        genome_names.clear();
        group2version.clear();
        version2init.clear();
        group2states.clear();
        discoveredServers.clear();
        genome2genericVersionList.clear();
        versionName2genome.clear();
        versionName2versionSet.clear();
    }

    public GeneralLoadUtils(SingletonGenometryModel gmodel, SeqMapView gviewer) {
            //SingletonGenometryModel gmodel, String url) {
        this.gmodel = gmodel;
        this.gviewer = gviewer;
 
        genome_names = new TreeSet<String>();
        group2version = new HashMap<AnnotatedSeqGroup, GenericVersion>();
        version2init = new HashMap<String, Boolean>();
        group2states = new HashMap<AnnotatedSeqGroup, Map<String, Integer>>();
        discoveredServers = new LinkedHashMap<String, genericServer>();
        genome2genericVersionList = new LinkedHashMap<String,List<GenericVersion>>();
        versionName2genome = new HashMap<String,String>();
        versionName2versionSet = new HashMap<String,Set<GenericVersion>>();
    }

    /**
     * Discover all of the servers and genomes and versions.
     */
    void discoverServersAndGenomesAndVersions() {
        /*if (discoveredServers.size() > 0) {
            // servers have already been discovered and are cached.
            return discoveredServers.keySet();
        }*/

        // it's assumed that if we're here, we need to refresh this information.
        discoveredServers.clear();
        genome_names.clear();

        
        // We use a thread to get the servers.  (Otherwise the user may see a lockup of their UI.)
        try {
            Runnable r = new Runnable() {
                public void run() {
                    discoverServersAndGenomesAndVersionsInternal();
                }
            };
            Thread thr1 = new Thread(r);
            thr1.start();
            while (thr1.isAlive()) {
                Thread.sleep(200);
            }
        } catch (InterruptedException ie) {
            System.out.println("Interruption while getting server list.");
        }
    }
    public synchronized void discoverServersAndGenomesAndVersionsInternal() {
        for (Map.Entry<String,Das2ServerInfo> entry : Das2Discovery.getDas2Servers().entrySet()) {
            Das2ServerInfo server = entry.getValue();
            String serverName = entry.getKey();
            if (server != null && serverName != null) {
                if (!discoveredServers.containsKey(serverName)) {
                    genericServer g = new genericServer(serverName, server.getURI().toString(), server.getClass(),server);
                    discoveredServers.put(serverName, g);
                    this.getGenomesAndVersionsInternal(serverName);
                }
            }
        }

        // Discover DAS servers
        // TODO -- strip out descriptions and make synonyms with DAS/2
        // TODO -- get chromosome info
        for (Map.Entry<String,DasServerInfo> entry : DasDiscovery.getDasServers().entrySet()) {
            DasServerInfo server = entry.getValue();
            String serverName = entry.getKey();
            if (server != null && serverName != null) {
                if (!discoveredServers.containsKey(serverName)) {
                     genericServer g = new genericServer(serverName, server.getRootUrl(), server.getClass(),server);
                     discoveredServers.put(serverName, g);
                     this.getGenomesAndVersionsInternal(serverName);
                }
            }
        }

        // Discover Quickload servers
        // This is based on new preferences, which allow arbitrarily many quickload servers.
        for (genericServer gServer : ServerList.getServers().values()) {
            if (gServer.serverClass == QuickLoadServerModel.class) {
                discoveredServers.put(gServer.serverName, gServer);
                this.getGenomesAndVersionsInternal(gServer.serverName);
            }
        }
    }


    public static boolean getCacheResidues() {
        return false;
    }

    public static boolean getCacheAnnots() {
        return false;
    }

/*
    public String getRootUrl() {
        return root_url;
    }
*/
    // Does the work of getting the genome names.
    private void getGenomesAndVersionsInternal(final String serverName) {
        // discover genomes from server
        genericServer gServer = discoveredServers.get(serverName);
        if (gServer.serverClass == Das2ServerInfo.class) {
            getDAS2Genomes(gServer);
            return;
        }
        if (gServer.serverClass == DasServerInfo.class) {
            getDAS1Genomes(gServer);
            return;
        }
        if (gServer.serverClass == QuickLoadServerModel.class) {
            getQuickLoadGenomes(gServer);
            return;
        }

        System.out.println("WARNING: Unknown server class " + gServer.serverClass);
    }


    private synchronized void getDAS1Genomes(genericServer gServer) {
        // Discover genomes from DAS
        DasServerInfo server = (DasServerInfo) gServer.serverObj;
        for (DasSource source : server.getDataSources().values()) {
            System.out.println("source, version:" + source.getName() + "..." + source.getVersion() + "..." + source.getDescription() + "..." + source.getInfoUrl() + "..." + source.getID());
            String genomeName = source.getDescription();
            String versionName = source.getName();
            genome_names.add(genomeName);
            List<GenericVersion> gVersionList;
            if (!this.genome2genericVersionList.containsKey(genomeName)) {
                gVersionList = new ArrayList<GenericVersion>(1);
                this.genome2genericVersionList.put(genomeName, gVersionList);
            } else {
                gVersionList = this.genome2genericVersionList.get(genomeName);
            }
            GenericVersion gVersion = new GenericVersion(versionName, gServer, source);
            discoverVersion(versionName, gServer, gVersion, gVersionList, genomeName);
        }
    }

    private synchronized void getDAS2Genomes(genericServer gServer) {
        // Discover genomes from DAS/2
        Das2ServerInfo server = (Das2ServerInfo) gServer.serverObj;
        for (Das2Source source : server.getSources().values()) {
            String genomeName = source.getName();
            genome_names.add(genomeName);
            List<GenericVersion> gVersionList;
            if (!this.genome2genericVersionList.containsKey(genomeName)) {
                gVersionList = new ArrayList<GenericVersion>(source.getVersions().size());
                this.genome2genericVersionList.put(genomeName, gVersionList);
            } else {
                gVersionList = this.genome2genericVersionList.get(genomeName);
            }
            // Das/2 has versioned sources.  Get each version.
            for (Das2VersionedSource versionSource : source.getVersions().values()) {
                String versionName = versionSource.getName();
                GenericVersion gVersion = new GenericVersion(versionName, gServer, versionSource);
                discoverVersion(versionName, gServer, gVersion, gVersionList, genomeName);
            }
        }
    }

     private synchronized void getQuickLoadGenomes(genericServer gServer) {
        // Discover genomes from Quickload
        URL quickloadURL = null;
        try {
            quickloadURL = new URL((String) gServer.serverObj);
        } catch (MalformedURLException ex) {
            Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(gmodel, quickloadURL);
        List<String> genomeList = quickloadServer.getGenomeNames();
        for (String genomeName : genomeList) {
            genome_names.add(genomeName);
            List<GenericVersion> gVersionList;
            if (!this.genome2genericVersionList.containsKey(genomeName)) {
                gVersionList = new ArrayList<GenericVersion>(1);
                this.genome2genericVersionList.put(genomeName, gVersionList);
            } else {
                gVersionList = this.genome2genericVersionList.get(genomeName);
            }
            GenericVersion gVersion = new GenericVersion(genomeName, gServer, quickloadServer);
            discoverVersion(gVersion.versionName, gServer, gVersion, gVersionList, genomeName);
        }
    }

    private void discoverVersion(String versionName, genericServer gServer, GenericVersion gVersion, List<GenericVersion> gVersionList, String genomeName) {
        gVersionList.add(gVersion);
        versionName2genome.put(versionName, genomeName);
        Set<GenericVersion> versionSet;
        if (this.versionName2versionSet.containsKey(versionName)) {
            versionSet = this.versionName2versionSet.get(versionName);
        } else {
            versionSet = new HashSet<GenericVersion>(1);
            this.versionName2versionSet.put(versionName, versionSet);
        }
        versionSet.add(gVersion);
        AnnotatedSeqGroup group = gmodel.addSeqGroup(versionName); // returns existing group if found, otherwise creates a new group
        group2version.put(group, gVersion);
        if (DEBUG) {
            System.out.println("Added " + gServer.serverClass + "genome: " + genomeName + " version: " + versionName + "--" + this.genome2genericVersionList.get(genomeName).get(0).versionName);
        }
    }



    public AnnotatedSeqGroup getSeqGroup(final String genome_name) {
        return gmodel.addSeqGroup(genome_name);
    }

    /** Returns the name that this server uses to refer to the given AnnotatedSeqGroup.
     *  Because of synonyms, different servers may use different names to
     *  refer to the same genome.
     */
    /*public String getGenomeName(AnnotatedSeqGroup group) {
        return group2version.get(group);
    }*/

    public static String stripFilenameExtensions(final String name) {
        String new_name = name;
        if (name.indexOf('.') > 0) {
            new_name = name.substring(0, name.lastIndexOf('.'));
        }
        return new_name;
    }



    /**
     *  Returns the list of features for the genome with the given version name.
     *  The list may (rarely) be empty, but never null.
     */
    public List<GenericFeature> getFeatures(final String versionName) {
        // There may be more than one server with the same versionName.  Merge all the version names.
        List<GenericFeature> featureList = new ArrayList<GenericFeature>();
        for (GenericVersion gVersion: this.versionName2versionSet.get(versionName)) {
            featureList.addAll(gVersion.features);
        }
        return featureList;
    }


    /** Returns Map of annotation type name to Integer, 0 if annotation type is not loaded */
    public static Map<String,Integer> getLoadStates(AnnotatedSeqGroup group) {
        return group2states.get(group);
    }

    public static int getLoadState(AnnotatedSeqGroup group, final String file_name) {
        Map<String,Integer> load_states = getLoadStates(group);
        if (load_states == null) {
            return 0; /* shouldn't happen */
        }
        String stripped_file = stripFilenameExtensions(file_name);
        return load_states.get(stripped_file);
    }

    public static void setLoadState(AnnotatedSeqGroup group, final String file_name, int loaded) {
        Map<String,Integer> load_states = group2states.get(group);
        if (load_states == null) {
            load_states = new LinkedHashMap<String,Integer>();
            group2states.put(group, load_states);
        }
        String stripped_file = stripFilenameExtensions(file_name);
        load_states.put(stripped_file, loaded);
    }

    public static void setLoadStatus(AnnotatedSeqGroup group, final String server, final String genome, final String feature, int loaded) {
        String unique_name = "";
//        LoadStatus ls = new LoadStatus();

  //      this.genomeAndServer2LoadStatus.put(unique_name,)
    }


     public void initVersion(final String versionName) {
        if (versionName == null) {
            return;
        }
        Boolean init = version2init.get(versionName);
        if (init == null || !init.booleanValue()) {
            System.out.println("initializing data for version: " + versionName);
            Application.getApplicationLogger().fine("initializing data for version: " + versionName);
            boolean seq_init = loadSeqInfo(versionName);
            loadFeatureNames(versionName);
            
            boolean annot_init = true;
            if (seq_init && annot_init) {
                version2init.put(versionName, Boolean.TRUE);
            }
        }
    }

    /** Returns true if the given genome has already been initialized via initGenome(String). */
    public boolean isInitialized(final String genome_name) {
        Boolean b = version2init.get(genome_name);
        return (Boolean.TRUE.equals(b));
    }

   


    /**
     * Load annotation names for the given version name (across multiple servers).
     * The internal call is threaded to keep from locking up the GUI.
     * @param versionName
     * @return
     */
    private boolean loadFeatureNames(final String versionName) {
        String genomeName = this.versionName2genome.get(versionName);
        for (final GenericVersion gVersion : this.genome2genericVersionList.get(genomeName)) {
            if (!gVersion.versionName.equals(versionName)) {
                continue;
            }

            // We use a thread to get the servers.  (Otherwise the user may see a lockup of their UI.)
            try {
                Runnable r = new Runnable() {

                    public void run() {
                        loadFeatureNames(gVersion);
                    }
                };
                Thread thr1 = new Thread(r);
                thr1.start();
                while (thr1.isAlive()) {
                    Thread.sleep(200);
                }
            } catch (InterruptedException ie) {
                System.out.println("Interruption while getting feature list.");
            }
        }

        return true;
    }


    /**
     * Load the annotations for the given version.  This is specific to one server.
     * @param gVersion
     */
    private void loadFeatureNames(final GenericVersion gVersion) {
        if (gVersion.features.size() > 0) {
            System.out.println("Feature names are already loaded.");
            return;
        }

        if (gVersion.features.size() > 0) {
            System.out.println("Feature names are already loaded.");
            return;
        }

        if (gVersion.gServer.serverClass == Das2ServerInfo.class) {
            System.out.println("Discovering DAS2 features for " + gVersion.versionName);
            // Discover features from DAS/2
            Das2VersionedSource version = (Das2VersionedSource)gVersion.versionSourceObj;
            for (Das2Type type : version.getTypes().values()) {
                String type_name = type.getName();
                gVersion.features.add(new GenericFeature(type_name,gVersion));
            }
            return;
        }
        if (gVersion.gServer.serverClass == DasServerInfo.class) {
            // Discover features from DAS
            DasSource version = (DasSource)gVersion.versionSourceObj;
            for (DasType type : version.getTypes().values()) {
                String type_name = type.getID();
                gVersion.features.add(new GenericFeature(type_name,gVersion));
            }
            return;
        }
        if (gVersion.gServer.serverClass == QuickLoadServerModel.class) {
            // Discover feature names from QuickLoad
             try {
                URL quickloadURL = new URL((String) gVersion.gServer.serverObj);
                QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(gmodel, quickloadURL);
                List<String> featureNames = quickloadServer.getFilenames(gVersion.versionName);
                for (String featureName : featureNames) {
                    gVersion.features.add(new GenericFeature(featureName,gVersion));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
             return;
        }

        System.out.println("WARNING: Unknown server class " + gVersion.gServer.serverClass);
    //return false;
    }


    // Load the sequence info for the given genome (version, unimportant).
    // Then send it to the genometry model.
    public boolean loadSeqInfo(final String versionName) {
        System.out.println("loading seqinfo : Version " + versionName);
        String genomeName = this.versionName2genome.get(versionName);
        List<GenericVersion> gVersionList = this.genome2genericVersionList.get(genomeName);
        GenericVersion gVersion = gVersionList.get(0); // Get first server as default for chromosome data.

        AnnotatedSeqGroup group = loadChromInfo(gVersion);
        if (group == null)
            return false;

        addGenomeVirtualSeq(group);

        if (gmodel.getSelectedSeqGroup() != group) {
            gmodel.setSelectedSeqGroup(group);
        }

        if (gmodel.getSelectedSeq() == null) {
            gmodel.setSelectedSeq(group.getSeq(0)); // default to the first chromosome
        }

        return true;
    }


        // Load the sequence info for the given genome versionr.
    private AnnotatedSeqGroup loadChromInfo(GenericVersion gVersion) {
        AnnotatedSeqGroup group = null;
        if (DEBUG) {
            System.out.println("loading list of chromosomes for genome version: " + gVersion.versionName);
        }
        //System.out.println("group: " + (group == null ? null : group.getID()) + ", " + group);
        Application.getApplicationLogger().fine("loading list of chromosomes for genome: " + gVersion.versionName);
        //Application.getApplicationLogger().fine("group: " + (group == null ? null : group.getID()) + ", " + group);

        // discover genomes from server
        if (gVersion.gServer == null) {
            return null;
        }
        if (DEBUG) {
            System.out.println("Discovering " + gVersion.gServer.serverClass + " chromosomes");
        }
        if (gVersion.gServer.serverClass == Das2ServerInfo.class) {
            
            // Discover chromosomes from DAS/2
            Das2VersionedSource version = (Das2VersionedSource)gVersion.versionSourceObj;

            group = version.getGenome();  // adds genome to singleton genometry model if not already present
            // Calling version.getSegments() to ensure that Das2VersionedSource is populated with Das2Region segments,
            //    which in turn ensures that AnnotatedSeqGroup is populated with SmartAnnotBioSeqs
            group.setSource(gVersion.gServer.serverName);
            version.getSegments();
            return group;
        }
        if (gVersion.gServer.serverClass == DasServerInfo.class) {
            // Discover chromosomes from DAS
           DasSource version = (DasSource)gVersion.versionSourceObj;

           group = version.getGenome();
           group.setSource(gVersion.gServer.serverName);
           Map<String,DasEntryPoint> chromMap = version.getEntryPoints();
           for (DasEntryPoint chrom : chromMap.values()) {
            // Something similar to running Das2Region.
               /*String lengthstr = reg.getAttribute("length");
                String region_name = reg.getAttribute(NAME);
                if (region_name.length() == 0) {
                    region_name = reg.getAttribute(TITLE);
                }
                String region_info_url = reg.getAttribute("doc_href");
                String description = null;
                int length = Integer.parseInt(lengthstr);
                Das2Region region = new Das2Region(this, region_uri, region_name, region_info_url, length);
                * */

               String name = chrom.getID(); // maybe
               int length = chrom.getSegment().getLength();

                AnnotatedSeqGroup genome = group;
                MutableAnnotatedBioSeq aseq = null;
                if (!(genome instanceof Das2SeqGroup)) {
                    aseq = genome.getSeq(name);
                    if (aseq == null) {
                        aseq = genome.getSeq(chrom.getID());    // maybe
                    }
                }
                // b) if can't find a previously seen genome for this DasSource, then
                //     create a new genome entry
                if (aseq == null) {
                    // using name instead of id for now
                    aseq = genome.addSeq(name, length);
                }
                SimpleSeqSpan segment_span = new SimpleSeqSpan(0, length, aseq);

            }
            //version.getEntryPoints();

           //Document doc = DasLoader.getDocument(request_con);
        //seqs = DasLoader.parseSegmentsFromEntryPoints(doc);

           return group;
        }
        if (gVersion.gServer.serverClass == QuickLoadServerModel.class) {
            // Discover chromosomes from QuickLoad

            URL quickloadURL;
            try {
                quickloadURL = new URL((String) gVersion.gServer.serverObj);
                QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(gmodel, quickloadURL);
                group = quickloadServer.getSeqGroup(gVersion.versionName);
                group.setSource(gVersion.gServer.serverName);

                //quickloadServer.
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
                //Logger.getLogger(GeneralLoadUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return group;
        }

        System.out.println("WARNING: Unknown server class " + gVersion.gServer.serverClass);

        return null;
    }

    private void addGenomeVirtualSeq(AnnotatedSeqGroup group) {
        int chrom_count = group.getSeqCount();
        if (chrom_count <= 1) {
            // no need to make a virtual "genome" chrom if there is only a single chromosome
            return;
        }

        Application.getApplicationLogger().fine("$$$$$ adding virtual genome seq to seq group");
        String GENOME_SEQ_ID = "genome";
        if (group.getSeq(GENOME_SEQ_ID) != null) {
            return; // return if we've already created the virtual genome
        }

        SmartAnnotBioSeq genome_seq = group.addSeq(GENOME_SEQ_ID, 0);
        for (int i = 0; i < chrom_count; i++) {
            BioSeq chrom_seq = group.getSeq(i);
            if (chrom_seq == genome_seq)
                continue;
            addSeqToVirtualGenome(genome_seq,chrom_seq,default_genome_min,DEBUG_VIRTUAL_GENOME);
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
     *  addEncodeVirtualSeq.
     *  adds virtual CompositeBioSeq which is composed from all the ENCODE regions.
     *  assumes urlpath resolves to bed file for ENCODE regions
     */
    /*public void addEncodeVirtualSeq(AnnotatedSeqGroup seq_group, final String urlpath) {
        InputStream istr = null;
        Application.getApplicationLogger().fine("$$$$$ adding virtual encode seq to seq group");
        // assume it's a bed file...
        BedParser parser = new BedParser();
        try {
            istr = LocalUrlCacher.getInputStream(urlpath, getCacheAnnots());
            //      BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(filepath)));
            List<SeqSymmetry> regions = parser.parse(istr, gmodel, seq_group, false, QuickLoadView2.ENCODE_REGIONS_ID, false);
            SmartAnnotBioSeq virtual_seq = seq_group.addSeq(QuickLoadView2.ENCODE_REGIONS_ID, 0);
            MutableSeqSymmetry mapping = new SimpleMutableSeqSymmetry();

            int min_base_pos = 0;
            int current_base = min_base_pos;
            int spacer = 20000;
            for (SeqSymmetry esym : regions) {
                SeqSpan espan = esym.getSpan(0);
                int elength = espan.getLength();

                SimpleSymWithProps child = new SimpleSymWithProps();
                String cid = esym.getID();
                if (cid != null) {
                    child.setID(cid);
                }
                child.addSpan(espan);
                child.addSpan(new SimpleSeqSpan(current_base, current_base + elength, virtual_seq));
                mapping.addChild(child);
                current_base = current_base + elength + spacer;
            }
            virtual_seq.setBounds(min_base_pos, current_base);
            mapping.addSpan(new SimpleSeqSpan(min_base_pos, current_base, virtual_seq));
            virtual_seq.setComposition(mapping);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            GeneralUtils.safeClose(istr);
        }
        return;
    }*/

    /**
     * Load and display annotations (requested for the specific feature).
     * @param gFeature
     * @return
     */
    public boolean loadAndDisplayAnnotations(GenericFeature gFeature) {
        MutableAnnotatedBioSeq selected_seq = gmodel.getSelectedSeq();
		MutableAnnotatedBioSeq visible_seq = (MutableAnnotatedBioSeq)gviewer.getViewSeq();
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
        }
        else {
            ErrorHandler.errorPanel("ERROR", "Requested load strategy not recognized: " + gFeature.loadStrategy);
            return false;
        }
        

        Class serverClass = gFeature.gVersion.gServer.serverClass;
        if (serverClass == Das2ServerInfo.class) {
            if (loadDAS2Annotations(
                    selected_seq,
                    gFeature.featureName,
                    (Das2VersionedSource)gFeature.gVersion.versionSourceObj,
                    gviewer,
                    visible_seq,
                    overlap)) {
                return true;
            }
            return false;
        }
        if (serverClass == DasServerInfo.class) {
            //TODO
            List<String> featureList = new ArrayList<String>(1);
            featureList.add(gFeature.featureName);
            if (DasClientOptimizer.loadAnnotations(
                    gFeature.gVersion.gServer.URL,
                    "",
                    overlap,
                    featureList)) {
                return true;
            }
            return false;
        }
        if (serverClass == QuickLoadServerModel.class) {
            //String annot_url = root_url + genome_version_name + "/" + feature_name;
             //String root_url = gFeature.gVersion.;
             
            String annot_url = gFeature.gVersion.gServer.URL + gFeature.gVersion.versionName + "/" + gFeature.featureName;
            System.out.println("need to load: " + annot_url);
            Application.getApplicationLogger().fine("need to load: " + annot_url);
            InputStream istr = null;
            BufferedInputStream bis = null;

            try {
                istr = LocalUrlCacher.getInputStream(annot_url, getCacheAnnots());
                if (istr != null) {
                    bis = new BufferedInputStream(istr);

                    if (GraphSymUtils.isAGraphFilename(gFeature.featureName)) {
                        URL url = new URL(annot_url);
                        List graphs = OpenGraphAction.loadGraphFile(url, gmodel.getSelectedSeqGroup(), gmodel.getSelectedSeq());
                        if (graphs != null) {
                            // Reset the selected Seq Group to make sure that the DataLoadView knows
                            // about any new chromosomes that were added.
                            gmodel.setSelectedSeqGroup(gmodel.getSelectedSeqGroup());
                        }
                    } else {
                        LoadFileAction.load(Application.getSingleton().getFrame(), bis, gFeature.featureName, gmodel, gmodel.getSelectedSeq());
                    }

                    //setLoadState(current_group, feature_name, true);
                    return true;
                }
            } catch (Exception ex) {
                ErrorHandler.errorPanel("ERROR", "Problem loading requested url:\n" + annot_url, ex);
                // keep load state false so we can load this annotation from a different server
                //setLoadState(current_group, feature_name, false);
            } finally {
                GeneralUtils.safeClose(bis);
                GeneralUtils.safeClose(istr);
            }


            return false;
        }

        System.out.println("class " + serverClass + " is not implemented.");
        return false;
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
            MutableAnnotatedBioSeq selected_seq, final String feature_name, Das2VersionedSource version,SeqMapView gviewer, MutableAnnotatedBioSeq visible_seq, SeqSpan overlap) {
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
            FeatureLoading.processDas2FeatureRequests(requests, true, true, gmodel, gviewer);
        }
        return true;
    }
  }
