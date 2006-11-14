package com.affymetrix.genometry.servlets;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.SimpleDateFormat;


// import com.affymetrix.igb.test.SearchSymTest;
import com.affymetrix.genoviz.util.Memer;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;

import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.parsers.*;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.das2.Das2Coords;


/**
 *  experimental genometry-based DAS2 server
 *    (started with GenometryDasServlet (pseudo-DAS1 servlet), modifying for DAS2
 *
 *  Servlet-based, object-oriented, in-memory "database" of genomic
 *     annotations for efficient retrieval of annotations that overlap a query range
 *     on a sequence.
 *  Search strategy: "interval array"
 *                   binary search followed by constrained location-tuned bidirectional scan
 *
 */
public class GenometryDas2Servlet extends HttpServlet  {
  static boolean DEBUG = false;
  static boolean MAKE_LANDSCAPES = false;
  static boolean TIME_RESPONSES = true;
  static boolean ADD_VERSION_TO_CONTENT_TYPE = false;
  static boolean USE_CREATED_ATT = true;

  static Map genomeid2coord;
  static {
    // GAH 11-2006
    // for now hardwiring URIs for agreed upon genome assembly coordinates, based on 
    //    http://www.open-bio.org/wiki/DAS:GlobalSeqIDs
    // Plan to replace this with a smarter system once coordinates and reference URIs are specified in XML 
    //     rather than an HTML page (hopefully will be served up as DAS/2 sources & segments XML)
    genomeid2coord = new HashMap();
    genomeid2coord.put("H_sapiens_Mar_2006",
		     new Das2Coords("http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B36.1/",
				    "NCBI", "9606", "36", "Chromosome", null));
    genomeid2coord.put("H_sapiens_May_2004",
		     new Das2Coords("http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B35.1/",
				    "NCBI", "9606", "35", "Chromosome", null));
    genomeid2coord.put("D_melanogaster_Apr_2004",
		     new Das2Coords("http://www.flybase.org/genome/D_melanogaster/R3.1/",
				    "BDGP", "7227", "4", "Chromosome", null));
  }

  static String DAS2_VERSION = "2.0";
  public static String DAS2_NAMESPACE = Das2FeatureSaxParser.DAS2_NAMESPACE;
  static String SOURCES_CONTENT_TYPE = "application/x-das-sources+xml";
  static String SEGMENTS_CONTENT_TYPE = "application/x-das-segments+xml";
  static String TYPES_CONTENT_TYPE = "application/x-das-types+xml";
  //    FEATURES_CONTENT_TYPE is set in the Das2FeatureSaxParser
  //  static String FEATURES_CONTENT_TYPE = "application/x-das-features+xml";

  // For now server doesn't really understand seqeunce ontology, so just
  //    using the topmost term for annotations with sequence locations:
  //    SO:0000110, "located_sequence_feature";
  static String default_onto_num = "0000110";
  static String default_onto_term = "SO:" + default_onto_num;
  static String default_onto_uri =
    "http://das.biopackages.net/das/ontology/obo/1/ontology/SO/" + default_onto_num;
  //  static String default_onto_uri = default_onto_term;

  static String URID = "uri";
  static String NAME = "title";
  static String ONTOLOGY = "ontology";
  static String SO_ACCESSION = "so_accession";

  /*
   *  DAS commands recognized by GenometryDas2Servlet
   *  (additional commands may be recognized by command plugins)
   */
  //  static String sources_query = "sequence"; // sources query is same as root URL (xml_base) minus trailing slash
  static String sources_query_with_slash = "";  // set in setXmlBase()
  static String sources_query_no_slash= ""; // set in setXmlBase();
  static String segments_query = "segments";
  static String types_query = "types";
  static String features_query = "features";
  static String query_att = "query_uri";
  //  static String add_command = "add_features";

  static String default_feature_format = "das2feature";

  // see data_root for explanation
  static String default_data_root = "c:/data/genometry_server_data/das2_2005-02/";
  static String genometry_server_dir = System.getProperty("das2_genometry_server_dir");
  static String maintainer_email = System.getProperty("das2_maintainer_email");

  // static default_data_root = System.getProperty("user.dir") + "/das2_server/test_2005-02/" :

  /** The root directory of the data to be served-up.
   *  Defaults to system property "user.dir" + "/query_server_smaller/".
   *  The user can change this by setting a property for "genometry_server_dir"
   *  on the command line.  For example "java -Dgenometry_server_dir=/home/me/mydir/ ...".
   */
  public static final String data_root =
    (genometry_server_dir == null || genometry_server_dir.length()==0) ?
        default_data_root :
        (genometry_server_dir + "/");
  // Note I'm adding an extra "/" at the end of genometry_server_dir just to be certain
  // there is one there.  If it ends up with two "/" characters, that hurts nothing.

  static String synonym_file = data_root + "synonyms.txt";

  /**
   *  Map of commands to plugins, for extending DAS server to
   *     recognize additional commands.
   */
  Map command2plugin = new HashMap();

  static Pattern format_splitter = Pattern.compile(";");
  static final Pattern query_splitter = Pattern.compile(";");
  static final Pattern tagval_splitter = Pattern.compile("=");
  //  static final Pattern range_splitter = Pattern.compile("/");
  //  static final Pattern interval_splitter = Pattern.compile(":");

  /**
   *  Top level data structure that holds all the genome models
   */
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  /**
   *  Top level data structure that holds all the genome models in source/version hierarchy
   *  maps organism names to list of genome versions for that organism
   */
  static Map organisms = new LinkedHashMap();

  // specifying a template for chromosome seqs constructed in lift and chromInfo parsers
  //  MutableAnnotatedBioSeq template_seq = new NibbleBioSeq();
  MutableAnnotatedBioSeq template_seq = new SmartAnnotBioSeq();
  LiftParser lift_parser = new LiftParser(template_seq);
  ChromInfoParser chrom_parser = new ChromInfoParser(template_seq);

  ArrayList log = new ArrayList(100);
  //  HashMap directory_filter = new HashMap();
  Memer mem;
  Map output_registry = new HashMap();
  //  DateFormat date_formatter = DateFormat.getDateTimeInstance();
  SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd");
  long date_initialized = 0;
  String date_init_string = null;
  Map graph_name2file = new LinkedHashMap();
  ArrayList graph_formats = new ArrayList();
  String xml_base = null;
  String xml_base_trimmed = null;

  public void init() throws ServletException  {
    System.out.println("called GenometryDas2Servlet.init()");
    try {
      super.init();

      if (! (new File(data_root)).isDirectory()) {
        throw new ServletException("Aborting: Specified directory does not exist: '"+data_root+"'");
      }
      System.out.println("Starting GenometryDas2Servlet in directory: '"+data_root+"'");

      // Alternatives: (for now trying option B)
      //   A. hashing to AnnotationWriter object:
      //        output_registry.put("bps", new BpsParser());
      //        output_registry.put("psl", new PSLParser())
      //   B. hashing to AnnotationWriter Class object rather than instance of a writer object:
      output_registry.put("bps", BpsParser.class);
      //      id2mime.put("bps", "binary/
      output_registry.put("psl", PSLParser.class);
      output_registry.put("dasgff", Das1FeatureSaxParser.class);
      output_registry.put("dasxml", Das1FeatureSaxParser.class);
      output_registry.put("bed", BedParser.class);
      output_registry.put("simplebed", SimpleBedParser.class);
      output_registry.put("bgn", BgnParser.class);
      output_registry.put("brs", BrsParser.class);
      // GFFParser.
      output_registry.put("gff", GFFParser.class);
      //      output_registry.put("link.psl", ProbeSetDisplayPlugin.class);
      output_registry.put("das2feature", Das2FeatureSaxParser.class);
      output_registry.put("das2xml", Das2FeatureSaxParser.class);
      output_registry.put("bar", BarParser.class);
      output_registry.put(Das2FeatureSaxParser.FEATURES_CONTENT_TYPE, Das2FeatureSaxParser.class);
      output_registry.put(Das2FeatureSaxParser.FEATURES_CONTENT_SUBTYPE, Das2FeatureSaxParser.class);
      output_registry.put("bp2", Bprobe1Parser.class);

      graph_formats.add("bar");

      loadSynonyms();
      loadGenomes();
      Map genomes = gmodel.getSeqGroups();
      Iterator giter = genomes.keySet().iterator();
      while (giter.hasNext()) {
	String key = (String)giter.next();
	System.out.println("key: " + key);
	AnnotatedSeqGroup group = (AnnotatedSeqGroup)genomes.get(key);
	System.out.println("Genome: " + group.getID() + ", organism: " + group.getOrganism() +
			   ", version: " + group.getVersion() + ", seq count: " + group.getSeqCount());
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    System.out.println("finished with GenometryDas2Servlet.init()");
    date_initialized = System.currentTimeMillis();
    date_init_string = date_formatter.format(new Date(date_initialized));
  }

  public void loadGenomes() throws IOException {
    // get list of all directories in data root
    // each directory corresponds to a different organism
    //    organism_name = directory name
    //    subdirectory for each genome_version
    //    for each genome version:
    //       genome_version_name = directory name
    //       parse liftAll or chromInfo file to create new AnnotatedSeqGroup for genome
    //           in SingletonGenometryModel
    //       for each file in genome_version directory
    //           if directory, recurse in
    //           else try to parse and annotate seqs based on file suffix (.xyz)
    mem = new Memer();
    mem.printMemory();

    File top_level = new File(data_root);
    if (! top_level.exists()) {
      throw new IOException("File does not exist: '"+top_level+"'");
    }
    File[] orgs = top_level.listFiles();
    if (orgs == null || orgs.length == 0) {
      throw new IOException("Directory has no contents: '"+top_level+"'");
    }
    for (int i=0; i<orgs.length; i++) {
      File org = orgs[i];
      if (org.isDirectory()) {  // assuming all directories at this level represent organisms
	File[] versions = org.listFiles();
	for (int k=0; k<versions.length; k++) {
	  File version = versions[k];
	  if (version.isDirectory()) {
	    loadGenome(version, org.getName());
	  }
	}
      }
    }
    System.gc();
    mem.printMemory();
  }


  void loadSynonyms() {
    File synfile = new File(synonym_file);
    if (synfile.exists()) {
      System.out.println("DAS server synonym file found, loading synonyms");
      SynonymLookup lookup = SynonymLookup.getDefaultLookup();
      try {
	lookup.loadSynonyms(new FileInputStream(synfile));
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    }
    else {
      System.out.println("DAS server synonym file not found, therefore not using synonyms");
    }
  }

  public void loadGenome(File genome_directory, String organism) throws IOException {
    //    Map seqhash = new LinkedHashMap();
    /** first, create MutableAnnotatedSeqs for each chromosome via ChromInfoParser */
    String genome_version = genome_directory.getName();
    System.out.println("loading data for genome: " + genome_version);
    String genome_path = genome_directory.getAbsolutePath();
    File chrom_info_file = new File(genome_path + "/mod_chromInfo.txt");
    if (chrom_info_file.exists()) {
      System.out.println("parsing in chromosome data from mod_chromInfo file for genome: " + genome_version);
      InputStream chromstream = new FileInputStream(chrom_info_file);
      //      seqhash = chrom_parser.parse(chromstream, genome_version);
      chrom_parser.parse(chromstream, genome_version);
    }
    else {
      System.out.println("couldn't find mod_chromInfo file for genome: " + genome_version);
      System.out.println("looking for lift file instead");
      File lift_file = new File(genome_path + "/liftAll.lft");
      if (lift_file.exists()) {
	System.out.println("parsing in chromosome data from liftAll file for genome: " + genome_version);
	InputStream liftstream = new FileInputStream(lift_file);
	//	seqhash = lift_parser.parse(liftstream, genome_version);
	lift_parser.parse(liftstream, genome_version);
      }
    }

    AnnotatedSeqGroup genome = gmodel.getSeqGroup(genome_version);
    genome.setOrganism(organism);
    List versions = (List)organisms.get(organism);
    if (versions == null) {
      versions = new ArrayList();
      organisms.put(organism, versions);
    }
    versions.add(genome);

    /**
     *   second, search genome directory for annotation files to load
     *   (and recursively descend through subdirectories doing same)
     */
    //    loadAnnotsFromFile(genome_directory, seqhash);
    loadAnnotsFromFile(genome_directory, genome);

    /**
     *  Third optimize genome by replacing second-level syms with IntervalSearchSyms
     */
    //    optimizeGenome(seqhash);
    optimizeGenome(genome);
  }


  //  public void optimizeGenome(Map seqhash) {
  public void optimizeGenome(AnnotatedSeqGroup genome) {
    System.out.println("******** optimizing genome:  " + genome.getID() + "  ********");
    /** third, replace top-level annotation SeqSymmetries with IntervalSearchSyms */
    //    Iterator iter = seqhash.keySet().iterator();
    Iterator iter = genome.getSeqList().iterator();

    // iterate through all annotated sequences in this genome version
    while (iter.hasNext()) {
      MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq) iter.next();
      optimizeSeq(aseq);
    }
    //    System.out.println("******** leaving optimizeGenome() ********");
  }


  /*
   *  After optimization, should end up (assuming seq is a SmartAnnotBioSeq) with
   *     a TypeContainerAnnot attached directly to the seq for each method/type of annotation
   *     and a (probably single) child for each container which is an IntervalSearchSym that
   *     holds annotations of the given type, and which has been optimized for range-based
   *     queries to return it's children
   *    Another way to think of this is that there are two levels of annotation hierarchy
   *       above what one would otherwise consider the "top level" annotations.
   *       For example for transcript predictions there would be a four-level hiearchy:
   *            single TypeContainerAnnot object A attached as annotation to seq
   *            single IntervalSearchSym object B, child of A
   *            multiple transcripts, children of B
   *            multiple exons, children of transcripts
   *
   *
   *  To Do:
   *
   *     check for multiple top-level annotations of the same type, and combine
   *       if found so there is only one top-level annotation per type for each seq
   *
   *     subclass AnnotatedBioSeq to add retrieval of top-level annotation by type
   */
  public void optimizeSeq(MutableAnnotatedBioSeq aseq) {
    if (DEBUG)  { System.out.println("optimizing seq = " + aseq.getID()); }
    int annot_count = aseq.getAnnotationCount();
    for (int i=annot_count-1; i>=0; i--) {
      // annot should be a TypeContainerAnnot (if seq is a SmartAnnotBioSeq)
      SeqSymmetry annot = aseq.getAnnotation(i);
      if (annot instanceof TypeContainerAnnot) {
	TypeContainerAnnot container = (TypeContainerAnnot)annot;
	optimizeTypeContainer(container, aseq);
      }
      else {
	System.out.println("problem in optimizeSeq(), found top-level sym that is not a TypeContainerAnnot: " +
			   annot);
      }
    }
  }


  public void optimizeTypeContainer(TypeContainerAnnot container, MutableAnnotatedBioSeq aseq) {
    if (DEBUG)  {
      System.out.println("optimizing type container: " + container.getProperty("method") +
		       ", depth = " + SeqUtils.getDepth(container));
    }
    String annot_type = container.getType();
    int child_count = container.getChildCount();
    ArrayList temp_annots = new ArrayList(child_count);

    // more efficient to remove from end of annotations...
    for (int i=child_count-1; i>=0; i--) {
      SeqSymmetry child = container.getChild(i);
      // if child is not IntervalSearchSym, copy to temp list in preparation for
      //    converting children to IntervalSearchSyms
      if (child instanceof IntervalSearchSym) {
	IntervalSearchSym search_sym = (IntervalSearchSym)child;
	if (! search_sym.getOptimizedForSearch()) {
	  search_sym.initForSearching(aseq);
	}
      }
      else {
	temp_annots.add(child);
        // really want to do container.removeChild(i) here, but
        //   currently there is no removeChild(int) method for MutableSeqSymmetry and descendants
	container.removeChild(child);
      }
    }

    int temp_count = temp_annots.size();
    // iterate through all annotations on this sequence that are not IntervalSearchSyms,
    //    convert them to IntervalSearchSyms.
    for (int i=temp_count-1; i>=0; i--) {
      SeqSymmetry annot_sym = (SeqSymmetry)temp_annots.get(i);
      IntervalSearchSym search_sym = new IntervalSearchSym(aseq, annot_sym);
      search_sym.setProperty("method", annot_type);
      search_sym.initForSearching(aseq);
      container.addChild(search_sym);
    }
    //    if (MAKE_LANDSCAPES) { makeLandscapes(aseq); }
    if (DEBUG)  {
      System.out.println("finished optimizing container: " + container.getProperty("method") +
			 ", depth = " + SeqUtils.getDepth(container));
    }
  }


  public void loadAnnotsFromUrl(String url, String annot_name, AnnotatedSeqGroup seq_group) {
    try {
      URL annot_url = new URL(url);
      InputStream istr = new BufferedInputStream(annot_url.openStream());
      // may need to trim down url_name here, but how much?
      loadAnnotsFromStream(istr, annot_name, seq_group);
    }
    catch (Exception ex) { ex.printStackTrace(); }
  }

  public void loadAnnotsFromStream(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group) {
    ParserController.parse(istr, stream_name, seq_group);
  }

  /**
   *   If current_file is directory, recursively call on each child files;
   *   if not directory, see if can parse as annotation file.
   */
  public void loadAnnotsFromFile(File current_file, AnnotatedSeqGroup seq_group) {
    String file_name = current_file.getName();
    // if current file is directory, then descend down into child files
    if (current_file.isDirectory()) {
      //      if (directory_filter.get(file_name) != null) {
      //	System.out.println("filtering out directory: " + current_file);
      //	return;  // screening out anything in filtered directories
      //      }
      System.out.println("checking for annotations in directory: " + current_file);
      File[] child_files = current_file.listFiles();
      for (int i=0; i<child_files.length; i++) {
	loadAnnotsFromFile(child_files[i], seq_group);
      }
    }
    else if (file_name.endsWith(".bar"))  {
      String file_path = current_file.getPath();
      // special casing so bar files are seen in types request, but not parsed in on startup
      //    (because using graph slicing so don't have to pull all bar file graphs into memory)
      System.out.println("@@@ adding graph file to types: " + file_name + ", path: " + file_path);
      graph_name2file.put(file_name, file_path);
    }
    else {  // current file is not a directory, so try and recognize as annotation file
      InputStream istr = null;
      try {
	istr = new BufferedInputStream(new FileInputStream(current_file));
	loadAnnotsFromStream(istr, file_name, seq_group);
      }
      catch (Exception ex)  {
	ex.printStackTrace();
      }
      finally {
        if (istr != null) try {istr.close();} catch (IOException ioe) {}
      }
    }
    System.gc();
    mem.printMemory();
  }

  public List getLog()  { return log; }
  //  public Map getGenomesModel() { return name2genome; }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    String path_info = request.getPathInfo();
    String query = request.getQueryString();
    System.out.println("GenometryDas2Servlet received POST request: ");
    System.out.println("   path: " + path_info);
    System.out.println("   query: " + query);
  }

  public void doPut(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    System.out.println("GenometryDas2Servlet received PUT request: ");
  }

  public long getLastModified(HttpServletRequest request) {
    System.out.println("getLastModified() called");
    String path_info = request.getPathInfo();
    String query = request.getQueryString();
    System.out.println("   path: " + path_info);
    System.out.println("   query: " + query);
    return date_initialized;
  }

  //  public void service(HttpServletRequest request, HttpServletResponse response)
  //    throws ServletException, IOException {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

    com.affymetrix.genoviz.util.Timer timecheck = null;
    log.clear();
    if (TIME_RESPONSES)  {
      timecheck = new com.affymetrix.genoviz.util.Timer();
      timecheck.start();
    }
    log.add("*************** Genometry Das Servlet ***************");
    log.add(date_formatter.format(new Date(System.currentTimeMillis())));
    //    Memer mem = new Memer();
    //    mem.printMemory();
    String path_info = request.getPathInfo();
    String query = request.getQueryString();
    String request_url = request.getRequestURL().toString();
    log.add("HttpServletResponse buffer size: " + response.getBufferSize());
    log.add("path_info: " + path_info);
    log.add("url: " + request_url);
    log.add("query: " + query);
    log.add("path translated = " + request.getPathTranslated());
    log.add("context path = " + request.getContextPath());
    log.add("request uri = " + request.getRequestURI());
    log.add("servlet path = " + request.getServletPath());

    //    PrintWriter pw = response.getWriter();
    addDasHeaders(response);

    if (path_info.endsWith(sources_query_no_slash) || path_info.endsWith(sources_query_with_slash))  {
      handleSourcesRequest(request, response);
    }
    else if (path_info == null || path_info.trim().length() == 0) {
      log.add("Unknown or missing DAS command");
      response.setStatus(response.SC_BAD_REQUEST);
    }
    else {
      AnnotatedSeqGroup genome = getGenome(request);
      // log.add("Genome version: '"+ genome.getID() + "'");
      if (genome == null) {
        log.add("Unknown genome version");
        response.setStatus(response.SC_BAD_REQUEST);
      }
      else {
        String das_command = path_info.substring(path_info.lastIndexOf("/")+1);
        log.add("das command: " + das_command);
	//        DasCommandPlugin plugin = (DasCommandPlugin)command2plugin.get(das_command);
	if (das_command.equals(segments_query)) {
	  handleSegmentsRequest(request, response);
	}
	else if (das_command.equals(types_query))  {
	  handleTypesRequest(request, response);
	}
	else if (das_command.equals(features_query)) {
	  handleFeaturesRequest(request, response);
	}
	/*
	else if (das_command.equals(add_command)) {
	  handleAddFeatures(request, response);
	}
	else if (plugin != null) {
	  plugin.handleRequest(this, request, response);
	}
	*/
	else {
	  log.add("DAS request not recognized, setting HTTP status header to 400, BAD_REQUEST");
	  response.setStatus(response.SC_BAD_REQUEST);
	}
      }
    }
    if (TIME_RESPONSES) {
      long tim = timecheck.read();
      log.add("---------- response time: " + tim/1000f + "----------");
    }
    for (int i=0; i<log.size(); i++) { System.out.println(log.get(i)); }
    log.clear();
  }



  /**
   * Extracts name of (versioned?) genome from servlet request,
   *    and uses to retrieve AnnotatedSeqGroup (genome) from SingletonGenometryModel
   */
  public AnnotatedSeqGroup getGenome(HttpServletRequest request) {
    String path_info = request.getPathInfo();
    if (path_info == null) {return null;}
    int last_slash = path_info.lastIndexOf('/');
    int prev_slash = path_info.lastIndexOf('/', last_slash-1);
    //    log.add("last_slash: " + last_slash + ",  prev_slash: " + prev_slash);
    String genome_name = path_info.substring(prev_slash+1, last_slash);
    AnnotatedSeqGroup genome = gmodel.getSeqGroup(genome_name);
    if (genome == null)  { log.add("unknown genome version: '" + genome_name + "'"); }
    return genome;
  }

  /**
   *  All DAS-specific headers have been eliminated in DAS/2 spec (as of 2006-02-02)
   *  Therefore, this call currently does nothing
   */
  protected static void addDasHeaders(HttpServletResponse response) {
    //    response.setHeader("X-Das-Version", "DAS_Affy_experimental/2.0");
    //    response.setHeader("X-Das-Status", DAS_STATUS_OK);
    //    response.setHeader("X-Das-Capabilities",
    //		       "dsn/1.0; types/1.0; entry_points/1.0; bps_features/2.0; minmin_maxmax/2.0");
    //		       "dsn/1.0; types/1.0; entry_points/1.0; bps_features/2.0");
  }

  public void handleSourcesRequest(HttpServletRequest request, HttpServletResponse response)
    throws IOException  {
    log.add("received data source query");
    setContentType(response, SOURCES_CONTENT_TYPE);
    addDasHeaders(response);
    PrintWriter pw = response.getWriter();
    //    String xbase = request.getRequestURL().toString();
    //    static String xbase = request.getRequestUri();
    String xbase = getXmlBase(request);
    printXmlDeclaration(pw);
    //    pw.println("<!DOCTYPE DAS2XML SYSTEM \"http://www.biodas.org/dtd/das2xml.dtd\">");
    pw.println("<SOURCES");
    pw.println("    xmlns=\"" + DAS2_NAMESPACE + "\"");
    pw.println("    xml:base=\"" + xbase + "\" >");
    if (maintainer_email != null && maintainer_email.length() > 0) {
      pw.println("  <MAINTAINER email=\"" + maintainer_email + "\" />");
    }
    // other elements to add:

    //    Map genomes = gmodel.getSeqGroups();
    Iterator oiter = organisms.entrySet().iterator();
    while (oiter.hasNext()) {
      Map.Entry oentry = (Map.Entry)oiter.next();
      String org = (String)oentry.getKey();
      List versions = (List)oentry.getValue();
      //Iterator giter = genomes.entrySet().iterator();
      //      pw.println("  <SOURCE id=\"" + org + "\" >" );
      pw.println("  <SOURCE uri=\"" + org + "\" title=\"" + org + "\" >" );

      Iterator giter = versions.iterator();
      while (giter.hasNext()) {
	AnnotatedSeqGroup genome = (AnnotatedSeqGroup)giter.next();
	Das2Coords coords = (Das2Coords)genomeid2coord.get(genome.getID());
	System.out.println("Genome: " + genome.getID() + ", organism: " + genome.getOrganism() +
			   ", version: " + genome.getVersion() + ", seq count: " + genome.getSeqCount());
	//      pw.println("      <VERSION id=\"" + genome.getID() + "\" />" );
	if (USE_CREATED_ATT) {
	  pw.println("      <VERSION uri=\"" + genome.getID() + "\" title=\"" + genome.getID() +
		     "\" created=\"" + date_init_string + "\" >" );
	}
	else {
	  pw.println("      <VERSION uri=\"" + genome.getID() + "\" title=\"" + genome.getID() + "\" >" );
	}
	if (coords != null) {
	  pw.println("           <COORDINATES uri=\"" + coords.getURI() +
		     "\" authority=\"" + coords.getAuthority() +  
		     "\" taxid=\"" + coords.getTaxid() + 
		     "\" version=\"" + coords.getVersion() + 
		     "\" source=\"" + coords.getSource() + "\" />");
	}
	pw.println("           <CAPABILITY type=\"" + segments_query + "\" " + query_att + "=\"" +
		   genome.getID() + "/" + segments_query + "\" />");
	pw.println("           <CAPABILITY type=\"" + types_query + "\" " + query_att + "=\"" +
		   genome.getID() + "/" + types_query + "\" />");
	pw.println("           <CAPABILITY type=\"" + features_query + "\" " + query_att + "=\"" +
		   genome.getID() + "/" + features_query + "\" />");
	// other attributes to add:
	//   title, created, modified, writeable
	//   also doc_href -- need to change sources.rnc file for this!
	// other elements to add:
	//   <ASSEMBLY>
	//   <COORDINATES>
	//   <PROP>
	pw.println("      </VERSION>");
      }
      pw.println("  </SOURCE>");
    }
    pw.println("</SOURCES>");
  }

  public void handleSegmentsRequest(HttpServletRequest request, HttpServletResponse response)
     throws IOException  {
    log.add("received region query");
    AnnotatedSeqGroup genome = getGenome(request);
    Das2Coords coords = (Das2Coords)genomeid2coord.get(genome.getID());

    if (genome == null) {
      log.add("genome could not be found: " + genome.getID());
      // add error headers?
      return;
    }
    //    response.setContentType(SEGMENTS_CONTENT_TYPE);
    setContentType(response, SEGMENTS_CONTENT_TYPE);
    addDasHeaders(response);
    PrintWriter pw = response.getWriter();
    printXmlDeclaration(pw);
    //    String xbase = request.getRequestURL().toString();
    String xbase = getXmlBase(request) + genome.getID() + "/";
    String segments_uri = xbase + segments_query;
    //    pw.println("<!DOCTYPE DAS2XML SYSTEM \"http://www.biodas.org/dtd/das2xml.dtd\">");
    pw.println("<SEGMENTS ");
    pw.println("    xmlns=\"" + DAS2_NAMESPACE + "\"");
    //    pw.println("    xml:base=\"" + xbase + "\" >");
    pw.println("    xml:base=\"" + xbase + "\" ");
    // uri attribute is added purely to satisfy DAS 2.0 RelaxNG schema, it points back to this same document
    pw.println("    " + URID + "=\"" + segments_uri + "\" >");

    List seq_list = genome.getSeqList();
    Iterator siter = seq_list.iterator();
    while (siter.hasNext()) {
      //      Map.Entry keyval = (Map.Entry)siter.next();
      //      AnnotatedBioSeq aseq = (AnnotatedBioSeq)keyval.getValue();
      AnnotatedBioSeq aseq = (AnnotatedBioSeq)siter.next();
      String refatt = "";
      if (coords != null) {
	// GAH 11-2006
	// for now guessing at the reference URI, based on assembly URI and typical syntax used 
	//    at http://www.open-bio.org/wiki/DAS:GlobalSeqIDs for these URIs
	// Plan to replace this with a smarter system once reference URIs are specified in XML 
	//     rather than an HTML page (hopefully will be served up as DAS/2 sources & segments XML)
	String ref = coords.getURI() + "dna/" + aseq.getID();
	refatt = "reference=\"" + ref + "\"";
      }
      pw.println("   <SEGMENT " + URID + "=\"" + aseq.getID() + "\" " + NAME + "=\"" + aseq.getID() + "\"" +
      		 " length=\"" + aseq.getLength() + "\" " + refatt + " />");
      //      pw.println("<REGION id=\"" + aseq.getID() +
      //		 "\" start=\"0\" end=\"" + aseq.getLength() + "\" />");
    }
    pw.println("</SEGMENTS>");
  }

  /**
   *  Handles a request for "entry_types", building a "<DASTYPES>" response
   *  into the HttpServletRequest.
   */
  public void handleTypesRequest(HttpServletRequest request, HttpServletResponse response)
    throws IOException  {
    log.add("received types request");
    AnnotatedSeqGroup genome = getGenome(request);

    if (genome == null) {
      log.add("Unknown genome version");
      response.setStatus(response.SC_BAD_REQUEST);
      return;
    }

    //    response.setContentType(TYPES_CONTENT_TYPE);
    setContentType(response, TYPES_CONTENT_TYPE);
    addDasHeaders(response);
    PrintWriter pw = response.getWriter();

    printXmlDeclaration(pw);
    //    String xbase = request.getRequestURL().toString();
    //    String xbase = getXmlBase(request);
    String xbase = getXmlBase(request) + genome.getID() + "/";
    //    String types_uri = xbase + types_query;
    //    pw.println("<!DOCTYPE DAS2XML SYSTEM \"http://www.biodas.org/dtd/das2xml.dtd\">");
    //    pw.println("<!DOCTYPE DAS2TYPES SYSTEM \"http://www.biodas.org/dtd/das2types.dtd\" >");
    pw.println("<TYPES ");
    pw.println("    xmlns=\"" + DAS2_NAMESPACE + "\"");
    pw.println("    xml:base=\"" + xbase + "\" >");
    //    pw.println("    xml:base=\"" + xbase + "\" ");
    //    pw.println("    " + URID + "=\"" + types_uri + "\" >");

    Map types_hash = getTypes(genome);
    //    SortedSet types = new TreeSet(types_hash.keySet());  // this sorts the types alphabetically
    //    Iterator types_iter = types.iterator();
    Iterator types_iter = types_hash.keySet().iterator();
    while (types_iter.hasNext()) {
      String feat_type = (String) types_iter.next();
      java.util.List formats = (java.util.List) types_hash.get(feat_type);

      if (DEBUG)  { log.add("feat_type: " + feat_type + ", formats: " + formats); }

      pw.println("   <TYPE " + URID + "=\"" + feat_type + "\" " + NAME + "=\"" + feat_type +
		 "\" " + SO_ACCESSION + "=\"" + default_onto_term + "\" " + ONTOLOGY + "=\"" + default_onto_uri + "\" >");
      if (! formats.isEmpty()) {
        for (int k=0; k<formats.size(); k++) {
          String format = (String)formats.get(k);
          // pw.println("       <FORMAT id=\"" + format + "\" />");
	  pw.println("       <FORMAT name=\"" + format + "\" />");
        }
      }
      pw.println("   </TYPE>");
      /*
      if (! formats.isEmpty()) {
        pw.print("   <TYPE id=\"" + feat_type + "\" preferred_format=\"");
        for (int k=0; k<formats.size(); k++) {
          String format = (String)formats.get(k);
          pw.print(format + ";");
        }
        pw.println("\" />");
      }
      else {
        pw.println("   <TYPE id=\"" + feat_type + "\" />");
      }
      */

      // types_hash.put(feat_type, feat_type);
    }
    pw.println("</TYPES>");
  }


  /**
   *  Gets the list of types of annotations for a given genome version.
   *  Assuming top-level annotations hold type info in property "method" or "meth".
   *  @returns a Map where keys are feature type String's and values
   *    are non-null List's of preferred format String's
   *
   *  may want to cache this info (per versioned source) at some point...
   */
  Map getTypes(AnnotatedSeqGroup genome) {
    Map genome_types = new LinkedHashMap();
    Iterator seqiter = genome.getSeqList().iterator();
    // iterate over seqs to collect annotation types
    while (seqiter.hasNext()) {
      MutableAnnotatedBioSeq mseq = (MutableAnnotatedBioSeq)seqiter.next();
      if (mseq instanceof SmartAnnotBioSeq) {
        SmartAnnotBioSeq aseq = (SmartAnnotBioSeq)mseq;
        Map seq_types = aseq.getTypes();
        if (seq_types != null) {
          Iterator titer = seq_types.keySet().iterator();
          while (titer.hasNext()) {
            String type = (String)titer.next();
            java.util.List flist = Collections.EMPTY_LIST;
            if (genome_types.get(type) == null) {
              SymWithProps tannot = aseq.getAnnotation(type);
              //	    System.out.println("type: " + type + ", format info: " +
              //			       tannot.getProperty("preferred_formats"));
              SymWithProps first_child = (SymWithProps)tannot.getChild(0);
              if (first_child != null) {
                java.util.List formats = (java.util.List)first_child.getProperty("preferred_formats");
                //	      System.out.println("   child count: " + tannot.getChildCount() +
                //				 ", format info: " + formats);
                if (formats != null) { flist = formats; }
              }
              genome_types.put(type, flist);
            }
          }
        }
      }
      else {
	System.out.println("in DAS2 servlet getTypes(), found a seq that is _not_ a SmartAnnotSeq: " + mseq);
      }
    }
    // adding in any graph files as additional types (with type id = file name)
    // this is temporary, need a better solution soon -- should probably add empty graphs to seqs to have graphs
    //    show up in seq.getTypes(), but without actually being loaded??
    Iterator giter = graph_name2file.keySet().iterator();
    while (giter.hasNext()) {
      String gname = (String)giter.next();
      genome_types.put(gname, graph_formats);  // should probably get formats instead from "preferred_formats"?
    }

    return genome_types;
  }



  /**
   *  precedence for feature out format:
   *     specified in query string > specified in header > default
   *
   */
  public void handleFeaturesRequest(HttpServletRequest request, HttpServletResponse response) {
    log.add("received features request");
    AnnotatedSeqGroup genome = getGenome(request);
    addDasHeaders(response);
    String path_info = request.getPathInfo();
    String query = request.getQueryString();

    String output_format = default_feature_format;
    String query_type = null;
    SeqSpan overlap_span = null;
    SeqSpan inside_span = null;
    SeqSpan contain_span = null;
    SeqSpan identical_span = null;

    query = URLDecoder.decode(query);
    if (query == null || query.length() == 0) {

    }
    else {
      /**
       *  Should really update this to use ServletRequest getParameterValues(tag) instead
       *  This would have the added benefit of guaranteeing the values are URL-decoded, so don't
       *    need to decode query above...
       */
      // split query tagval list into format and filters
      // any tagval where tag = "format" determines format -- should only be one
      // all other tagvals should be filters
      //
      // GAH 4-18-2005 for now only trying to handle region filters and types filters
      //   currently assumes the following:
      //                    one "overlap" region filter, no ORing of multiple overlap filters
      //                    zero or one "inside" region filter, no ORing of multiple inside filters
      //                    one "type" filter (by typeid), no ORing of multiple type filters
      /**
       *  New logic for DAS/2 feature request filters:
       *  If there are multiple occurences of the same filter name in the request, take the union
       *      of the results of each of these filters individually
       *  Then take intersection of results of each different filter name
       *  (OR similar filters, AND different filters)
       */

      String[] segments = request.getParameterValues("segment");
      String[] types = request.getParameterValues("type");
      String[] overlaps = request.getParameterValues("overlaps");
      String[] insides = request.getParameterValues("inside");
      String[] formats = request.getParameterValues("format");

      String[] xids = request.getParameterValues("xid");
      String[] contains = request.getParameterValues("contains");
      String[] identicals = request.getParameterValues("identical");
      String[] names = request.getParameterValues("name");
      // property-based filters use a hybrid filter name of ("prop-" + prop_key)
      //  so for example if the property to filter by is "curator" then the parameter name
      //  will be "prop-curator".  So above approach won't work -- will need to go through
      //  entire list of parameter names and extract any that start with "prop-"...

      com.affymetrix.genoviz.util.Timer timecheck = new com.affymetrix.genoviz.util.Timer();
      long tim;
      List result = null;
      BioSeq outseq = null;

      if (names != null && names.length >= 1) {
	String name = names[0];
	result = genome.findSyms(name);
      }
      else {  // not a name query
	String[] query_array = query_splitter.split(query);
	boolean has_segment = false;
	String seqid = null;
	for (int i=0; i< query_array.length; i++) {
	  String tagval = query_array[i];
	  String[] tagval_array = tagval_splitter.split(tagval);
	  String tag = tagval_array[0];
	  String val = tagval_array[1];
	  log.add("tag = " + tag + ", val = " + val);
	  if (tag.equals("format")) {
	    output_format = val;
	  }
	  else if (tag.equalsIgnoreCase("segment")) {
	    has_segment = true;
	    seqid = val;
	    // hack to extract last part if segment is given as full URI (as it should according to DAS/2 spec v.300)...
	    int sindex = seqid.lastIndexOf("/");
	    if (sindex >= 0) { seqid = seqid.substring(sindex+1); }
	  }
	  else if (tag.equalsIgnoreCase("type")) {
	    // only track the last "type" value for now...
	    query_type = val;
	    // hack to extract last part if type is given as full URI
	    //    (as it should according to DAS/2 spec v.300)...
	    //    special-case exception is when giving "file:" URI for graph
	    //	    if (! (query_type.startsWith("file:") && query_type.endsWith(".bar"))) {
	    if (!(query_type.endsWith(".bar"))) {
	      int sindex = query_type.lastIndexOf("/");
	      if (sindex >= 0) { query_type = query_type.substring(sindex+1); }
	    }
	  }
	  else if (tag.equalsIgnoreCase("overlaps")) {
	    if (has_segment) { overlap_span = Das2FeatureSaxParser.getLocationSpan(seqid, val, genome); }
	    else  { overlap_span = Das2FeatureSaxParser.getLocationSpan(val, genome); }
	  }
	  else if (tag.equalsIgnoreCase("inside")) {
	    if (has_segment) { inside_span = Das2FeatureSaxParser.getLocationSpan(seqid, val, genome); }
	    else  { inside_span = Das2FeatureSaxParser.getLocationSpan(val, genome); }
	  }
	  else if (tag.equalsIgnoreCase("contains")) {
	    contain_span = Das2FeatureSaxParser.getLocationSpan(val, genome);
	  }
	  else if (tag.equalsIgnoreCase("identical")) {
	    identical_span = Das2FeatureSaxParser.getLocationSpan(val, genome);
	  }
	  //	else if (tag.equals("depth")) {
	  //	  System.out.println("NOT YET IMPLEMENTED, depth = " + val);
	  //	}
	  else {
	    log.add("query tagval not recognized: tag = " + tag + ", value = " + val);
	  }
	}
	if (query_type != null) { log.add("   query type: " + query_type); }
	if (overlap_span != null) { log.add("   overlap_span: " + SeqUtils.spanToString(overlap_span)); }
	if (inside_span != null) { log.add("   inside_span: " + SeqUtils.spanToString(inside_span)); }
	if (contain_span != null) { log.add("  contain_span: " + SeqUtils.spanToString(contain_span)); }
	if (identical_span != null) { log.add("   identical_span: " + SeqUtils.spanToString(identical_span)); }
	//	if (query_type != null && query_type.startsWith("file:") && query_type.endsWith(".bar")) {
	if (query_type != null && query_type.endsWith(".bar")) {
	  handleGraphRequest(request, response, query_type, overlap_span);
	  return;
	}

	BioSeq oseq = overlap_span.getBioSeq();
	outseq = oseq;
	timecheck.start();
	result = this.getIntersectedSymmetries(overlap_span, query_type);
	tim = timecheck.read();
	log.add("  overlapping annotations of type " + query_type + ": " + result.size());
	log.add("  time for range query: " + tim/1000f);

	// if an inside_span specified, then filter out intersected symmetries based on this:
	//    don't return symmetries with a min < inside_span.min()  (even if they overlap query interval)
	//    don't return symmetries with a max > inside_span.max()  (even if they overlap query interval)
	//    if (hard_min > 0 || hard_max < seqlength) {
	if (inside_span != null) {
	  int inside_min = inside_span.getMin();
	  int inside_max = inside_span.getMax();
	  BioSeq iseq = inside_span.getBioSeq();
	  log.add("*** trying to apply inside_span constraints ***");
	  if (iseq != oseq) {
	    log.add("Problem with applying inside_span constraint, different seqs: iseq = " +
		    iseq.getID() + ", oseq = " + oseq.getID());
	    // if different seqs, then no feature can pass constraint...
	    //   hmm, this might not strictly be true based on genometry...
	    result = null;
	  }
	  else {
	    timecheck.start();
	    MutableSeqSpan testspan = new SimpleMutableSeqSpan();
	    List orig_result = result;
	    int rcount = orig_result.size();
	    result = new ArrayList(rcount);
	    for (int i=0; i<rcount; i++) {
	      SeqSymmetry sym = (SeqSymmetry)orig_result.get(i);
	      // fill in testspan with span values for sym (on aseq)
	      sym.getSpan(iseq, testspan);
	      //	Ssytem.out.println("testing: " + testspan.getMin() + ", " + testspan.getMax()
	      if ((testspan.getMin() >= inside_min) &&
		  (testspan.getMax() <= inside_max)) {
		result.add(sym);
	      }
	    }
	    tim = timecheck.read();
	    log.add("  overlapping annotations of type " + query_type + " that passed inside_span constraints: " + result.size());
	    log.add("  time for inside_span filtering: " + tim/1000f);
	  }
	}
      }
      timecheck.start();

      log.add("return format: " + output_format);

      try {
	if (DEBUG) {
	  response.setContentType("text/html");
	  PrintWriter pw = response.getWriter();
	  pw.println("overlapping annotations found: " + result.size());
	}
	else {
	  outputAnnotations(result, outseq, query_type, request, response, output_format);
	  tim = timecheck.read();
	  log.add("  time for buffered output of results: " + tim/1000f);
	  timecheck.start();
	  tim = timecheck.read();
	  log.add("  time for closing output: " + tim/1000f);
	}
      }
      catch (Exception ex) {
	ex.printStackTrace();
      }

    }  // end (query != null) conditional
  }

  //  public void handleGraphRequest(HttpServletRequest request, HttpServletResponse response)  {
  public void handleGraphRequest(HttpServletRequest request, HttpServletResponse response,
				 String type, SeqSpan span) {
    log.add("#### handling graph request");
    SmartAnnotBioSeq seq = (SmartAnnotBioSeq)span.getBioSeq();
    String seqid = seq.getID();
    AnnotatedSeqGroup genome = seq.getSeqGroup();
    log.add("#### genome: " + genome.getID() + ", span: " + SeqUtils.spanToString(span));
    // use bar parser to extract just the overlap slice from the graph
    String file_name = type;   // for now using file_name as graph type
    String file_path = (String)graph_name2file.get(file_name);
    if (file_path == null) { file_path = file_name; }
    log.add("####    file: " + file_path);
    GraphSym graf = null;
    try  {
      graf = BarParser.getSlice(file_path, span);
    }
    catch (Exception ex)  { ex.printStackTrace(); }
    if (graf != null) {
      ArrayList gsyms = new ArrayList();
      gsyms.add(graf);
      log.add("#### returning graph slice in bar format");
      outputAnnotations(gsyms, span.getBioSeq(), type, request, response, "bar");
    }
    else {
      // couldn't generate a GraphSym, so return an error?
      log.add("####### problem with retrieving graph slice ########");
      response.setStatus(response.SC_NOT_FOUND);
      try {
        PrintWriter pw = response.getWriter();
        pw.println("DAS/2 server could not find graph to return for type: " +
                   type);
      }
      catch (Exception ex) { ex.printStackTrace(); }
      log.add("set status to 404 not found");
    }
  }


  public boolean outputAnnotations(java.util.List syms, BioSeq seq,
				   String annot_type,
				   HttpServletRequest request, HttpServletResponse response,
				   String format) {
    boolean success = true;
    try {
      //    AnnotationWriter writer = (AnnotationWriter)output_registry.get(format);
      // or should this be done by class:
      Class writerclass = (Class)output_registry.get(format);
      if (writerclass == null) {
	log.add("no AnnotationWriter found for format: " + format);
        response.setStatus(response.SC_BAD_REQUEST);
	success = false;
      }
      else {
	AnnotationWriter writer = (AnnotationWriter)writerclass.newInstance();
	String mime_type = writer.getMimeType();
	//	String xbase = request.getRequestURL().toString();
	String xbase = getXmlBase(request);
	if (writer instanceof Das2FeatureSaxParser) {
	  ((Das2FeatureSaxParser)writer).setBaseURI(new URI(xbase));
	  setContentType(response, mime_type);
	}
	else {
	  response.setContentType(mime_type);
	}
	log.add("return mime type: " + mime_type);
	OutputStream outstream = response.getOutputStream();
	// need to test and see if creating a new BufferedOutputStream in the
	//   AnnotationWriter.writeAnnotations implementations is necessary
	//    because it may incur a performance hit.  Though os is _not_ an instance of
	//    BufferedOutputStream (at least using jetty server), may still provide it's
	//    own buffering...
	success = writer.writeAnnotations(syms, seq, annot_type, outstream);
	outstream.flush();
	outstream.close();
      }
    }
    catch (Exception ex) {
	System.err.println("problem in GenometryDas2Servlet.outputAnnotations():");
	ex.printStackTrace();
	success = false;
    }
    return success;
  }


  /**
   *
   *  Currently assumes:
   *    query_span's seq is a SmartAnnotBioSeq (which implies top-level annots are TypeContainerAnnots)
   *    only one IntervalSearchSym child for each TypeContainerAnnot
   *  Should expand soon so results can be returned from multiple IntervalSearchSyms children
   *      of the TypeContainerAnnot
   */
  public List getIntersectedSymmetries(SeqSpan query_span, String annot_type) {
    SmartAnnotBioSeq seq = (SmartAnnotBioSeq)query_span.getBioSeq();
    SymWithProps container = seq.getAnnotation(annot_type);
    if (container != null) {
      int annot_count = container.getChildCount();
      for (int i=0; i<annot_count; i++) {
	SeqSymmetry sym = container.getChild(i);
	if (sym instanceof SearchableSeqSymmetry)  {
	  SearchableSeqSymmetry target_sym = (SearchableSeqSymmetry)sym;
	  return target_sym.getOverlappingChildren(query_span);
	}
      }
    }
    return Collections.EMPTY_LIST;
  }

  public static void printXmlDeclaration(PrintWriter pw) {
    // medium declaration (version, encoding)
    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    // long declaration (version, encoding, standalone)
    //  pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
    //    pw.println("<!DOCTYPE DAS2XML SYSTEM \"http://www.biodas.org/dtd/das2xml.dtd\">");
  }

  public static void setContentType(HttpServletResponse response, String ctype) {
    if (ADD_VERSION_TO_CONTENT_TYPE)  {
      response.setContentType(ctype + "; version=" + DAS2_VERSION);
    }
    else {
      response.setContentType(ctype);
    }
  }

  public void setXmlBase(String xbase) {
    xml_base = xbase;
    String trimmed_xml_base;
    if (xml_base.endsWith("/")) {
      trimmed_xml_base = xml_base.substring(0, xml_base.length()-1);
    }
    else {
      trimmed_xml_base = xml_base;
      xml_base += "/";
    }
    sources_query_no_slash = trimmed_xml_base.substring(trimmed_xml_base.lastIndexOf("/"));
    sources_query_with_slash = sources_query_no_slash + "/";
    System.out.println("*** xml_base: " + xml_base);
  }

  /** getXmlBase() should no longer depend on request, should always be set via setXmlBase()
      when servlet starts up -- need to remove request arg soon
  */
  public String getXmlBase(HttpServletRequest request) {
    if (xml_base != null) { return xml_base; }
    else { return request.getRequestURL().toString(); }
    //    else { return request.getRequestURI(); }
  }

  /**
   *  Start of attempt to add dynamic feature submission / addition to DAS server
   */
  /*
  public void handleAddFeatures(HttpServletRequest request, HttpServletResponse response) {
    System.out.println("attempting a dynamic data load into GenometryDas2Servlet");
    String genome_version = getGenomeVersionName(request);
    Map seqhash = (Map)name2genome.get(genome_version);
    addDasHeaders(response);
    String path_info = request.getPathInfo();
    String query = request.getQueryString();
    String annot_url = request.getParameter("feature_url");
    String annot_name = request.getParameter("feature_name");
    if (annot_name == null) {
      annot_name = annot_url;
    }
    System.out.println("url to load annots: " + annot_url);
    loadAnnotsFromUrl(annot_url, annot_name, seqhash);
    //    storeAnnotsFromUrl(annot_url, annot_name, genome_version);
    storeAnnotsFromUrl(annot_url, genome_version);
    optimizeGenome(seqhash);
  }
  */

  /*
  public void addCommandPlugin(String das_command, String plugin_class) {
    try {
      DasCommandPlugin plugin = (DasCommandPlugin) ObjectUtils.classForName(plugin_class).newInstance();
      addCommandPlugin(das_command, plugin);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  */

  /*
  public void addCommandPlugin(String das_command, DasCommandPlugin plugin) {
    System.out.println("adding das command plugin:  command = " + das_command + ", plugin = " + plugin);
    command2plugin.put(das_command, plugin);
  }
  */

  /**
   *  trying to add in autogeneration of landscape & projection annotations
   *
   *  need to figure out recalc of projections and landscapes -- when is it needed?
   *  need to filter out (or recalc) types ending with "::projection" or "::landscape"
   *
   */
  /*
  public void makeLandscapes(MutableAnnotatedBioSeq aseq) {
    System.out.println("BEGIN adding landscapes and projections for annotations on seq: " + aseq.getID());
    annot_count = aseq.getAnnotationCount();  // recheck annot count in case any dropped out or got combined by this point
    ArrayList landscapes = new ArrayList();
    ArrayList projections = new ArrayList();
    for (int i=0; i<annot_count; i++) {
      SymWithProps asym = (SymWithProps)aseq.getAnnotation(i);
      String meth = (String)asym.getProperty("method");
      GraphSym landscape = createLandscape(asym, aseq);
      //      SymWithProps projection = projectLandscape(landscape);
      SymWithProps projection = SeqSymSummarizer.projectLandscape(landscape);

      IntervalSearchSym searchproj = new IntervalSearchSym(aseq, projection);
      searchproj.initForSearching(aseq);

      landscape.setProperty("method", (meth + "::landscape"));
      searchproj.setProperty("method", (meth + "::projection"));
      landscapes.add(landscape);
      projections.add(searchproj);
      //      projection.setProperty("method", (meth + "::projection"));
      // projections.add(projection
    }
    int lcount = landscapes.size();
    for (int i=0; i<lcount; i++) {
      aseq.addAnnotation((SeqSymmetry)landscapes.get(i));
      aseq.addAnnotation((SeqSymmetry)projections.get(i));
    }
    System.out.println("DONE adding landscapes and projections for annotations on seq: " + aseq.getID());
  }
  */

  /*
  public GraphSym createLandscape(SymWithProps sym, BioSeq seq) {
    ArrayList symlist = new ArrayList();
    symlist.add(sym);
    // should probably move SeqSymSummarizer code into com.affymetrix.genometry.SeqUtils...
    GraphSym landscape = SeqSymSummarizer.getSymmetrySummary(symlist, seq);
    return landscape;
  }
  */

  //  public SymWithProps projectLandscape(GraphSym landscape) {
  //    return SeqSymSummarizer.projectLandscape(landscape);
  //  }

  //  public void storeAnnotsFromUrl(String url, String annot_name, String genome_version) {
  /*
  public void storeAnnotsFromUrl(String url, String genome_version) {
    try  {
      URL annot_url = new URL(url);
      URLConnection conn = annot_url.openConnection();
      InputStream bis = new BufferedInputStream(annot_url.openStream());

      int blength = conn.getContentLength();
      byte[] bytebuf = new byte[(int)blength];
      bis.read(bytebuf);
      bis.close();

      String outfile_name = data_root + genome_version + "/" + URLEncoder.encode(url, "UTF-8");
      File outfile = new File(outfile_name);
      OutputStream bos = new BufferedOutputStream(new FileOutputStream(outfile));
      bos.write(bytebuf);
      bos.close();
    }
    catch (Exception ex) {
      System.err.println("Error encountered in GenometryDas2Servlet.storeAnnotsFromUrl()");
      ex.printStackTrace();
    }
  }
  */


}
