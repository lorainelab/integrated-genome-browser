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
import com.affymetrix.genometryImpl.parsers.LiftParser;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.das.DasDiscovery;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das.DasSource;
import com.affymetrix.igb.das.DasType;
import com.affymetrix.igb.das2.Das2Discovery;
import com.affymetrix.igb.das2.Das2FeatureRequestSym;
import com.affymetrix.igb.das2.Das2Region;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.das2.Das2Source;
import com.affymetrix.igb.das2.Das2Type;
import com.affymetrix.igb.das2.Das2VersionedSource;
//import com.affymetrix.igb.menuitem.LoadFileAction;
//import com.affymetrix.igb.menuitem.OpenGraphAction;
//import com.affymetrix.igb.util.GraphSymUtils;
import com.affymetrix.igb.util.LocalUrlCacher;
//import java.io.BufferedInputStream;
//import java.io.BufferedReader;
import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.URL;
import java.util.ArrayList;
//import java.util.Collections;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class GeneralLoadUtils {
    private static final boolean DEBUG=true;

    /**
     *  using negative start coord for virtual genome chrom because (at least for human genome)
     *     whole genome start/end/length can't be represented with positive 4-byte ints (limit is +/- 2.1 billion)
     */
    double default_genome_min = -2100200300;
    boolean DEBUG_VIRTUAL_GENOME = true;

    Class server_type;  // Is the server Das (DasServerInfo), Das/2 (Das2ServerInfo), or Quickload?

    //public static final String PREF_QUICKLOAD_CACHE_RESIDUES = "quickload_cache_residues";
    //public static final String PREF_QUICKLOAD_CACHE_ANNOTS = "quickload_cache_annots";
    static String ENCODE_FILE_NAME = "encodeRegions.bed";
    static String ENCODE_FILE_NAME2 = "encode.bed";
    //static boolean CACHE_RESIDUES_DEFAULT = false;
    //static boolean CACHE_ANNOTS_DEFAULT = true;
    SingletonGenometryModel gmodel;
    static Pattern tab_regex = Pattern.compile("\t");
    String root_url;
    SortedSet<String> genome_names;   // Genome names are unique even across multiple servers; thus we use a set instead of a list.
    Map<AnnotatedSeqGroup,String> group2name;
    Map<String,Boolean> version2init;

    // A map from String genome name to a List of features on the server for that group
    Map<String,List<String>> version2features;
    /**
     *  Map of AnnotatedSeqGroup to a load state map.
     *  Each load state map is a map of an annotation type name to Boolean for
     *  whether it has already been loaded or not
     */
    static Map<AnnotatedSeqGroup,Map<String,Integer>> group2states;

    // server name-> genericServer class.
    Map<String,genericServer> discoveredServers;

    // [server + "." + genome] -> the server source object
    //Map<String,Object> genomeAndServer2VersionSource;

    // [server + "." + genome] ->
    //Map<String,Integer> genomeAndServer2LoadStatus;
    //Map<String,Integer> genomeAndServer2LoadRequest;

    // versions associated with a given genome.
    Map<String,List<genericVersion>> genome2genericVersionList;
    Map<String,String> version2genome;

    // server associated with a given version Class.
    //Map<genericVersion,List<genericServer>> version2genericServers;
        //static Map url2quickload = new HashMap();
    //public boolean allow_reinitialization = true;

    // friendly name associated with version.
    //Map<String,genericVersion> versionName2genericVersion;

    public void clear() {
        group2states.clear();
        genome_names.clear();
        group2name.clear();
        version2init.clear();
        version2features.clear();
        //url2quickload.clear();
        group2states.clear();
        discoveredServers.clear();
        //genomeAndServer2VersionSource.clear();
        //genomeAndServer2LoadStatus.clear();
        genome2genericVersionList.clear();
        //version2genericServers.clear();
        //versionName2genericVersion.clear();
        version2genome.clear();
    }

    public GeneralLoadUtils(SingletonGenometryModel gmodel) {
            //SingletonGenometryModel gmodel, String url) {
        this.gmodel = gmodel;
        /*root_url = url;
        if (!root_url.endsWith("/")) {
            root_url = root_url + "/";
        }*/
        //loadGenomeNames();

        genome_names = new TreeSet<String>();
        group2name = new HashMap<AnnotatedSeqGroup, String>();
        version2init = new HashMap<String, Boolean>();
        version2features = new HashMap<String, List<String>>();
        group2states = new HashMap<AnnotatedSeqGroup, Map<String, Integer>>();
        discoveredServers = new LinkedHashMap<String, genericServer>();
        genome2genericVersionList = new LinkedHashMap<String,List<genericVersion>>();
        version2genome = new HashMap<String,String>();

    }

    /**
     * Discover all of the servers and genomes and versions.
     */
    void discoverServersAndGenomesAndVersions() {
        /*if (discoveredServers.size() > 0) {
            // servers have already been discovered and are cached.
            return discoveredServers.keySet();
        }*/

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
    public void discoverServersAndGenomesAndVersionsInternal() {
        // it's assumed that if we're here, we need to refresh this information.
        discoveredServers.clear();
        genome_names.clear();


        for (Map.Entry<String,Das2ServerInfo> entry : Das2Discovery.getDas2Servers().entrySet()) {
            Das2ServerInfo server = entry.getValue();
            String serverName = entry.getKey();
            if (server != null && serverName != null) {
                if (!discoveredServers.containsKey(serverName)) {
                    genericServer g = new genericServer(serverName, server.getClass(),server);
                    discoveredServers.put(serverName, g);
                }
                this.getGenomesAndVersionsInternal(serverName);
            }
        }

        // Discover DAS servers
        /*for (Map.Entry<String,DasServerInfo> entry : DasDiscovery.getDasServers().entrySet()) {
            DasServerInfo server = entry.getValue();
            String serverName = entry.getKey();
            if (server != null && serverName != null) {
                discoveredServers.put(serverName, server);
                discoveredServerClasses.put(serverName, server.getClass());
                this.getGenomesAndVersionsInternal(serverName);
            }
        }*/

        // Discover Quickload servers
    }


    public static boolean getCacheResidues() {
        return false;
    }

    public static boolean getCacheAnnots() {
        return false;
    }

    public String getRootUrl() {
        return root_url;
    }

    // Does the work of getting the genome names.
    private void getGenomesAndVersionsInternal(final String serverName) {
        // discover genomes from server
        genericServer gServer = discoveredServers.get(serverName);
        if (gServer.serverClass == Das2ServerInfo.class) {
            // Discover genomes from DAS/2
            Das2ServerInfo server = (Das2ServerInfo) gServer.serverObj;
            for (Das2Source source : server.getSources().values()) {
                String genomeName = source.getName();
                genome_names.add(genomeName);
                List<genericVersion> gVersionList = new ArrayList<genericVersion>(source.getVersions().size());
                this.genome2genericVersionList.put(genomeName, gVersionList);
                //genome2genericVersionList.put(genomeName, versionList);
                // Das/2 has versioned sources.  Get each version.
                for (Das2VersionedSource versionSource : source.getVersions().values()) {
                    String versionName = versionSource.getName();
                    genericVersion gVersion = new genericVersion(versionName,gServer,versionSource);
                    gVersionList.add(gVersion);
                    version2genome.put(versionName,genomeName);
                    AnnotatedSeqGroup group = gmodel.addSeqGroup(versionName);  // returns existing group if found, otherwise creates a new group
                    //genomeAndServer2VersionSource.put(uniqueServerGenomeVersion(serverName, genomeName, versionName), versionSource);
                    group2name.put(group, versionName);
                    //versionName2genericVersion.put(versionName, versionSource);
                    if (DEBUG) {
                    System.out.println("adding unique: " + uniqueServerGenomeVersion(serverName, genomeName, versionName));
                    System.out.println("Added version: " + this.genome2genericVersionList.get(genomeName).get(0).versionName);
                    }
                }
            }
        } else if (gServer.serverClass == DasServerInfo.class) {
            // Discover genomes from DAS
            DasServerInfo server = (DasServerInfo) gServer.serverObj;
            for (DasSource source : server.getDataSources().values()) {
                List<genericVersion> gVersionList = new ArrayList<genericVersion>(1);
                String genomeName = source.getName();
                this.genome2genericVersionList.put(genomeName, gVersionList);
                genericVersion gVersion = new genericVersion(genomeName,gServer,source);
                gVersionList.add(gVersion);
                version2genome.put(genomeName,genomeName);
                //genome2genericVersionList.put(genomeName, versionList);
                //if (genome_names.contains(genome_name))
                //        continue;   // there is already a server with this genome.
                AnnotatedSeqGroup group = gmodel.addSeqGroup(genomeName);  // returns existing group if found, otherwise creates a new group
                genome_names.add(genomeName);
                //versionName2genericVersion.put(genomeName, source);
                //genomeAndServer2VersionSource.put(uniqueServerGenomeVersion(serverName, genomeName, genomeName), source);
                group2name.put(group, genomeName);
                if (DEBUG) {
                    System.out.println("adding unique: " + uniqueServerGenomeVersion(serverName, genomeName, genomeName));
                    }
            }
        } else {
            System.out.println("WARNING: Unknown server class " + gServer.serverClass);
        }
    }
    //public Map getSeqGroups() { return group2name; }

    public AnnotatedSeqGroup getSeqGroup(final String genome_name) {
        return gmodel.addSeqGroup(genome_name);
    }

    /** Returns the name that this server uses to refer to the given AnnotatedSeqGroup.
     *  Because of synonyms, different servers may use different names to
     *  refer to the same genome.
     */
    public String getGenomeName(AnnotatedSeqGroup group) {
        return group2name.get(group);
    }

    public static String stripFilenameExtensions(final String name) {
        String new_name = name;
        if (name.indexOf('.') > 0) {
            new_name = name.substring(0, name.lastIndexOf('.'));
        }
        return new_name;
    }


     /**
     *  Returns the list of features that this server has
     *  for the genome with the given version name.
     *  The list may (rarely) be empty, but never null.
     */
    public List<String> getFeatures(final String versionName) {
        initVersion(versionName);
        loadAnnotationNames(versionName);
        List<String> features = version2features.get(versionName);
        if (features == null) {
            return new ArrayList<String>();
        } else {
            return features;
        }
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
        return load_states.get(stripFilenameExtensions(file_name));
    }

    public static void setLoadState(AnnotatedSeqGroup group, final String file_name, int loaded) {
        Map<String,Integer> load_states = group2states.get(group);
        if (load_states == null) {
            load_states = new LinkedHashMap<String,Integer>();
            group2states.put(group, load_states);
        }
        load_states.put(stripFilenameExtensions(file_name), loaded);
    }

    public static void setLoadStatus(AnnotatedSeqGroup group, final String server, final String genome, final String feature, int loaded) {
        String unique_name = "";
//        LoadStatus ls = new LoadStatus();

  //      this.genomeAndServer2LoadStatus.put(unique_name,)
    }


    /*public void initGenome(final String server_name, final String genome_name) {
        if (genome_name == null) {
            return;
        }
        Boolean init = version2init.get(genome_name);
        if (init == null || !init.booleanValue()) {
            System.out.println("initializing data for genome: " + genome_name);
            Application.getApplicationLogger().fine("initializing data for genome: " + genome_name);
            boolean seq_init = loadSeqInfo(server_name, genome_name);
            boolean annot_init = loadAnnotationNames(server_name, genome_name);
            if (seq_init && annot_init) {
                version2init.put(genome_name, Boolean.TRUE);
            }
        }
    }*/

     public void initVersion(final String versionName) {
        if (versionName == null) {
            return;
        }
        Boolean init = version2init.get(versionName);
        if (init == null || !init.booleanValue()) {
            System.out.println("initializing data for version: " + versionName);
            Application.getApplicationLogger().fine("initializing data for version: " + versionName);
            boolean seq_init = loadSeqInfo(versionName);
            //boolean annot_init = loadAnnotationNames(versionName);
            loadAnnotationNames(versionName);
            
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


    
    public boolean loadAnnotationNames(final String versionName) {
        String genomeName = this.version2genome.get(versionName);
        for (genericVersion gVersion : this.genome2genericVersionList.get(genomeName)) {
            if (gVersion.versionName.equals(versionName)) {
                loadAnnotationNames(gVersion);
            }
        }
           
        return true;
    }


    /**
     * Load the annotations for the given version.  Combine them across all servers.
     * @param gVersion
     */
    private void loadAnnotationNames(final genericVersion gVersion) {
        if (gVersion.gServer.serverClass == Das2ServerInfo.class) {
            System.out.println("Discovering DAS2 features for " + gVersion.versionName);
            // Discover features from DAS/2
            Das2VersionedSource version = (Das2VersionedSource)gVersion.versionObj;

            List<String>features = new ArrayList<String>();
            for (Das2Type type : version.getTypes().values()) {
                String type_name = type.getName();
                features.add(type_name);
            }
            version2features.put(gVersion.versionName, features);

        } else if (gVersion.gServer.serverClass == DasServerInfo.class) {
            // Discover features from DAS
            DasSource version = (DasSource)gVersion.versionObj;
            List<String>features = new ArrayList<String>();
            for (DasType type : version.getTypes().values()) {
                String type_name = type.getID();
                features.add(type_name);
            }
            version2features.put(gVersion.versionName, features);

        } else {
            System.out.println("WARNING: Unknown server class " + gVersion.gServer.serverClass);
            //return false;
        }
    }



    public void addGenomeVirtualSeq(AnnotatedSeqGroup group) {
        int chrom_count = group.getSeqCount();
        if (chrom_count <= 1) {
            // no need to make a virtual "genome" chrom if there is only a single chromosome
            return;
        }

        Application.getApplicationLogger().fine("$$$$$ adding virtual genome seq to seq group");
        if (!QuickLoadView2.build_virtual_genome ||
                (group.getSeq(QuickLoadView2.GENOME_SEQ_ID) != null)) {
            return;
        }

        SmartAnnotBioSeq genome_seq = group.addSeq(QuickLoadView2.GENOME_SEQ_ID, 0);
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
            Application.getApplicationLogger().fine("added seq: " + chrom.getID() + ", new genome bounds: min = " + genome_seq.getMin() + ", max = " + genome_seq.getMax() + ", length = " + genome_seq.getLengthDouble());
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
    public void addEncodeVirtualSeq(AnnotatedSeqGroup seq_group, final String urlpath) {
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
    }

    // Load the sequence info for the given genome (version, unimportant).
    // Then send it to the genometry model.
    public boolean loadSeqInfo(final String versionName) {
        System.out.println("loading seqinfo : Version " + versionName);
        String genomeName = this.version2genome.get(versionName);
        List<genericVersion> gVersionList = this.genome2genericVersionList.get(genomeName);
        genericVersion gVersion = gVersionList.get(0); // Get first server as default for chromosome data.

        AnnotatedSeqGroup group = loadChromInfo(gVersion);
        if (group == null)
            return false;

        /*
        if (gmodel.getSelectedSeqGroup() != group) {
            gmodel.setSelectedSeqGroup(group);
        }

        if (QuickLoadView2.build_virtual_genome && group != null) {
            addGenomeVirtualSeq(group);
        }*/
       
        return true;
    }

        // Load the sequence info for the given genome versionr.
    public AnnotatedSeqGroup loadChromInfo(genericVersion gVersion) {
        AnnotatedSeqGroup group = null;
        System.out.println("loading list of chromosomes for genome version: " + gVersion.versionName);
        //System.out.println("group: " + (group == null ? null : group.getID()) + ", " + group);
        Application.getApplicationLogger().fine("loading list of chromosomes for genome: " + gVersion.versionName);
        //Application.getApplicationLogger().fine("group: " + (group == null ? null : group.getID()) + ", " + group);

        // discover genomes from server
        if (gVersion.gServer == null) {
            return null;
        }
        if (gVersion.gServer.serverClass == Das2ServerInfo.class) {
            System.out.println("Discovering DAS2 chromosomes");
            // Discover chromosomes from DAS/2
            Das2VersionedSource version = (Das2VersionedSource)gVersion.versionObj;

            group = version.getGenome();  // adds genome to singleton genometry model if not already present
            // Calling version.getSegments() to ensure that Das2VersionedSource is populated with Das2Region segments,
            //    which in turn ensures that AnnotatedSeqGroup is populated with SmartAnnotBioSeqs
            group.setSource(gVersion.gServer.serverName);
            version.getSegments();
        } else if (gVersion.gServer.serverClass == DasServerInfo.class) {
            // Discover chromosomes from DAS
           DasSource version = (DasSource)gVersion.versionObj;

           group = version.getGenome();
           group.setSource(gVersion.gServer.serverName);

           version.getEntryPoints();
        } else {
            System.out.println("WARNING: Unknown server class " + gVersion.gServer.serverClass);
            return null;
        }


        return group;
    }

    public boolean loadAnnotations(SeqMapView gviewer, AnnotatedSeqGroup current_group, final String feature_name) {
        int loaded = getLoadState(current_group, feature_name);
        if (loaded > 0) {
            Application.getApplicationLogger().fine("already loaded: " + feature_name);
            System.out.println("already loaded: " + feature_name);
            return true;
        }

        MutableAnnotatedBioSeq selected_seq = gmodel.getSelectedSeq();
		MutableAnnotatedBioSeq visible_seq = (MutableAnnotatedBioSeq)gviewer.getViewSeq();
		if (selected_seq == null || visible_seq == null) {
			//      ErrorHandler.errorPanel("ERROR", "You must first choose a sequence to display.");
			      System.out.println("@@@@@ selected chrom: " + selected_seq);
			      System.out.println("@@@@@ visible chrom: " + visible_seq);
			return false;
		}
        if (visible_seq != selected_seq) {
			System.out.println("ERROR, VISIBLE SPAN DOES NOT MATCH GMODEL'S SELECTED SEQ!!!");
			System.out.println("   selected seq: " + selected_seq.getID());
			System.out.println("   visible seq: " + visible_seq.getID());
			return false;
		}

        String serverName = current_group.getSource();
        String genome_name = current_group.getID();
        // discover genomes from server

        System.out.println("server,genome: " + serverName + " " + genome_name);
        // Later, DAS here
        genericServer g = discoveredServers.get(serverName);
//        Object versionSource = this.genomeAndServer2VersionSource.get(uniqueServerGenome(serverName,genome_name));
        Object versionSource = null;
        if (versionSource == null) {
            return false;
        }

        /*TODO: if (load_strategy == Das2TypeState.VISIBLE_RANGE)  {
        overlap = gviewer.getVisibleSpan();
        }
        else if (load_strategy == Das2TypeState.WHOLE_SEQUENCE)  {
        overlap = new SimpleSeqSpan(0, selected_seq.getLength(), selected_seq);
        }
        else {
        ErrorHandler.errorPanel("ERROR", "Requested load strategy not recognized: " + load_strategy);
        return;
        }*/
        SeqSpan overlap = gviewer.getVisibleSpan();

        if (g.serverClass == Das2ServerInfo.class) {
            if (loadDAS2Annotations(
                    selected_seq,
                    feature_name,
                    (Das2VersionedSource)versionSource,
                    gviewer,
                    visible_seq,
                    overlap)) {
                // TODO: Set load strategy to 1 or 2??
                setLoadState(current_group, feature_name, 1);
                return true;
            }
            return false;
        }
        if (g.serverClass == DasServerInfo.class) {
            return false;
        }

        System.out.println("class " + g.serverClass + " is not implemented.");
        return false;

        // Quickload
        /*
            String annot_url = root_url + this.getGenomeName(current_group) + "/" + feature_name;
            Application.getApplicationLogger().fine("need to load: " + annot_url);
            InputStream istr = null;
            BufferedInputStream bis = null;

            try {
                istr = LocalUrlCacher.askAndGetInputStream(annot_url, getCacheAnnots());
                if (istr != null) {
                    bis = new BufferedInputStream(istr);

                    if (GraphSymUtils.isAGraphFilename(feature_name)) {
                        URL url = new URL(annot_url);
                        List graphs = OpenGraphAction.loadGraphFile(url, current_group, gmodel.getSelectedSeq());
                        if (graphs != null) {
                            // Reset the selected Seq Group to make sure that the DataLoadView knows
                            // about any new chromosomes that were added.
                            gmodel.setSelectedSeqGroup(gmodel.getSelectedSeqGroup());
                        }
                    } else {
                        LoadFileAction.load(Application.getSingleton().getFrame(), bis, feature_name, gmodel, gmodel.getSelectedSeq());
                    }

                    setLoadState(current_group, feature_name, true);
                }
            } catch (Exception ex) {
                ErrorHandler.errorPanel("ERROR", "Problem loading requested url:\n" + annot_url, ex);
                // keep load state false so we can load this annotation from a different server
                setLoadState(current_group, feature_name, false);
            } finally {
                GeneralUtils.safeClose(bis);
                GeneralUtils.safeClose(istr);
            }
        */
    }

    private boolean loadDAS2Annotations(MutableAnnotatedBioSeq selected_seq, final String feature_name, Das2VersionedSource version,SeqMapView gviewer, MutableAnnotatedBioSeq visible_seq, SeqSpan overlap) {
        if (!(selected_seq instanceof SmartAnnotBioSeq)) {
            ErrorHandler.errorPanel("ERROR", "selected seq is not appropriate for loading DAS2 data");
            return false;
        }
        
        System.out.println("seq = " + visible_seq.getID() + ", min = " + overlap.getMin() + ", max = " + overlap.getMax());
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
            Das2LoadView3.processFeatureRequests(requests, true);
        }
        return true;
    }

    private static String uniqueServerGenome(final String serverName, final String genomeName) {
        return serverName + "." + genomeName;
    }

    private static String uniqueServerGenomeVersion(final String serverName, final String genomeName, final String versionName) {
        return serverName + "." + genomeName + "." + versionName;
    }

    private static String uniqueServerGenomeFeature(final String serverName, final String genomeName, final String featureName) {
        return serverName + "." + genomeName + "." + featureName;
    }
    public static String uniqueServerGenomeChromFeature(final String server, final String genome, final String chrom, final String feature) {
        return server + "." + genome + "." + chrom + "." + feature;
    }

    // A class that's useful for visualizing a generic server.
    public final class genericServer {
        public String serverName;   // name of the server.
        public final Class serverClass;   // Das2ServerInfo, DasServerInfo, ..., QuickLoad?
        public final Object serverObj;    // Das2ServerInfo, DasServerInfo, ..., QuickLoad?

        /**
         * @param serverName
         * @param serverClass
         * @param serverObj
         */
        genericServer(String serverName, Class serverClass, Object serverObj) {
            this.serverName = serverName;
            this.serverClass = serverClass;
            this.serverObj = serverObj;
        }   
    }

     // A class that's useful for visualizing a generic version.
    public final class genericVersion {
        public String versionName;          // name of the version.
        public final genericServer gServer; // generic Server object.
        public final Object versionObj;     // Das2ServerInfo, DasServerInfo, ..., QuickLoad?
        public List <String> featureNames;

        /**
         * @param versionName
         * @param gServer
         * @param versionObj
         */
        genericVersion(String versionName, genericServer gServer, Object versionObj) {
            this.versionName = versionName;
            this.gServer = gServer;
            this.versionObj = versionObj;
            this.featureNames = new ArrayList<String>();
        }
    }

  }
