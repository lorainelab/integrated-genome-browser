package com.affymetrix.genometry.servlets;

import com.affymetrix.genometry.parsers.ProbeSetDisplayPlugin;
import com.affymetrix.genometry.Das2AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;


import hci.gnomex.model.GenomeBuild;
import hci.gnomex.model.UnloadDataTrack;
import hci.gnomex.security.SecurityAdvisor;
import hci.gnomex.utility.DataTrackQuery;
import hci.gnomex.utility.PropertyDictionaryHelper;
import hci.gnomex.utility.QualifiedDataTrack;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;


import java.text.SimpleDateFormat;

import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import com.affymetrix.genometryImpl.SeqSpan;

import com.affymetrix.genometry.AnnotSecurity;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.das2.SimpleDas2Type;
import com.affymetrix.genometryImpl.parsers.*;
import com.affymetrix.genometryImpl.util.DirectoryFilter;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.HiddenFileFilter;
import com.affymetrix.genometry.util.Optimize;

import org.hibernate.Session;
import org.hibernate.Transaction;


import com.affymetrix.genometry.genopub.*;
import com.affymetrix.genometry.gnomex.GNomExSecurity;
import com.affymetrix.genometry.util.Das2ServerUtils;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.parsers.useq.USeqArchive;
import com.affymetrix.genometryImpl.parsers.useq.USeqUtilities;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometry.util.IndexingUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 *  genometry-based DAS2 server
 *
 *  Servlet-based, object-oriented, in-memory "database" of genomic
 *     annotations for efficient retrieval of annotations that overlap a query range
 *     on a sequence.
 *  Search strategy: "interval array"
 *                   binary search followed by constrained location-tuned bidirectional scan
 *

// Server looks for data in directory specified by system property "das2_genometry_server_dir"
// Within data directory should be subdirectories, each corresponding to a different organism
//    organism_name = directory name
//    subdirectory under organsism name for each genome_version
//    for each genome version:
//       genome_version_name = directory name
//       parse liftAll or chromInfo file to create new AnnotatedSeqGroup for genome
//           in GenometryModel
//       for each file in genome_version directory
//           if directory:
//                if ends with "..seqs" then indicates a subdirectory with same annotation type,
//                      but split up into one annotation file per seq (for example .bar graph files)
//                recurse in
//           else if non-loading filetype (for example .bar):
//                add as annot type without loading
//           else try to parse and annotate seqs based on file suffix (.xyz)
//
 */
public final class GenometryDas2Servlet extends HttpServlet {
	static final boolean DEBUG = true;
	private static final String RELEASE_VERSION = "2.6";
	private static final boolean USE_CREATED_ATT = true;
	private static final String SERVER_SYNTAX_EXPLANATION =
		"See http://netaffxdas.affymetrix.com/das2 for proper query syntax.";
	private static final String LIMITED_FEATURE_QUERIES_EXPLANATION =
		"See http://netaffxdas.affymetrix.com/das2 for supported feature queries.";

	/* The long versions of these error messages appears to cause problems for Apache.
	Specifically, they cause Apache to display an uninformative 502 Bad Gateway error
	page because it deems the http header 'syntactically invalid'.
	I suspect that the presence of multiple lines and/or blank lines is specically to blame.
	To be safe, we're now using very succinct, single-line messages containing a pointer to
	a web page which contains additional information to help guide the user about
	acceptable queries.  - SteveC 15 Apr 2008
	 */
	/*private static String SERVER_SYNTAX_EXPLANATION_LONG =
	"The Genometry DAS/2 server always uses a standard URI syntax for DAS/2 query URIs,\n\n" +
	" and enforces this by specifying URIs in the SOURCES doc according to this standard:\n" +
	"    das_server_root/genome_name/capability_name[?query_parameters]";*/
	/*private static String LIMITED_FEATURE_QUERIES_EXPLANATION_LONG =
	"The Genometry DAS/2 server currently does not support the full range of \n" +
	"DAS/2 feature queries and feature filters required by the DAS/2 specification. \n" +
	"To allow the Genometry DAS/2 server to still comply with the specification, \n" +
	"the server considers responses to any feature query it does not support as \n" +
	"being 'too large'.  Therefore it responds with an error message with HTTP \n" +
	"status code 413 'Request Entity Too Large', which is allowed by the DAS/2 spec \n" +
	"when the server considers the response too large.\n\n" +
	"Currently for the Genometry server to send a useful response containing features, \n" +
	"  the feature query string must contain: \n" +
	"     1 type filter \n" +
	"     1 segment filter \n" +
	"     1 overlaps filter \n" +
	"     0 or 1 inside filter \n" +
	"     0 or 1 format parameter \n" +
	"     0 other filters/parameters \n";*/
	private final static Map<String, Das2Coords> genomeid2coord;

	static {
		// GAH 11-2006
		// for now hardwiring URIs for agreed upon genome assembly coordinates, based on
		//    http://www.open-bio.org/wiki/DAS:GlobalSeqIDs
		// Plan to replace this with a smarter system once coordinates and reference URIs are specified in XML
		//     rather than an HTML page (hopefully will be served up as DAS/2 sources & segments XML)
		// covering the two naming schemes currently in use with this server, for example
		//     "H_sapiens_date" and "Human_date"
		genomeid2coord = new HashMap<String, Das2Coords>();
		genomeid2coord.put("H_sapiens_Mar_2006",
				new Das2Coords("http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B36.1/",
						"NCBI", "9606", "36", "Chromosome", null));
		genomeid2coord.put("Human_Mar_2006",
				new Das2Coords("http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B36.1/",
						"NCBI", "9606", "36", "Chromosome", null));
		genomeid2coord.put("H_sapiens_May_2004",
				new Das2Coords("http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B35.1/",
						"NCBI", "9606", "35", "Chromosome", null));
		genomeid2coord.put("Human_May_2004",
				new Das2Coords("http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B35.1/",
						"NCBI", "9606", "35", "Chromosome", null));
		genomeid2coord.put("D_melanogaster_Apr_2004",
				new Das2Coords("http://www.flybase.org/genome/D_melanogaster/R3.1/",
						"BDGP", "7227", "4", "Chromosome", null));
		genomeid2coord.put("Drosophila_Apr_2004",
				new Das2Coords("http://www.flybase.org/genome/D_melanogaster/R3.1/",
						"BDGP", "7227", "4", "Chromosome", null));
		//Zebrafish
		genomeid2coord.put("D_rerio_Jul_2007",
				new Das2Coords(
						//uri,rather odd, none of these actually point to anything
						"http://zfin.org/genome/D_rerio/Zv7/",
						//authority
						"ZFISH_7",
						//taxid
						"7955",
						//version
						"Zv7",
						//source
						"Chromosome",
						null));

		//C_elegans
		genomeid2coord.put("C_elegans_Jan_2007",
				new Das2Coords(
						//uri
						"http://www.wormbase.org/genome/C_elegans/WS180/",
						//authority
						"WS",
						//taxid
						"6239",
						//version
						"180",
						//source
						"Chromosome",
						null));

		//S_pombe
		genomeid2coord.put("S_pombe_Apr_2007",
				new Das2Coords(
						//uri
						"http://www.sanger.ac.uk/Projects/S_pombe/Apr_2007",
						//authority
						"Sanger",
						//taxid
						"4896",
						//version
						"Apr_2007",
						//source
						"Chromosome",
						null));
		//S_glossinidius
		genomeid2coord.put("S_glossinidius_Jan_2006",
				new Das2Coords(
						//uri
						"ftp://ftp.ncbi.nih.gov/genomes/Bacteria/Sodalis_glossinidius_morsitans/Jan_2006",
						//authority
						"NCBI",
						//taxid
						"343509",
						//version
						"Jan_2006",
						//source
						"Chromosome",
						null));
	}
	private static final String DAS2_NAMESPACE = Das2FeatureSaxParser.DAS2_NAMESPACE;
	private static final String SOURCES_CONTENT_TYPE = "application/x-das-sources+xml";
	private static final String SEGMENTS_CONTENT_TYPE = "application/x-das-segments+xml";
	private static final String TYPES_CONTENT_TYPE = "application/x-das-types+xml";
	private static final String LOGIN_CONTENT_TYPE = "application/x-das-login+xml";
	private static final String REFRESH_CONTENT_TYPE = "application/x-das-refresh+xml";
	//    FEATURES_CONTENT_TYPE is set in the Das2FeatureSaxParser
	//  static String FEATURES_CONTENT_TYPE = "application/x-das-features+xml";
	private static final String URID = "uri";
	private static final String NAME = "title";

	/*
	 *  DAS commands recognized by GenometryDas2Servlet
	 *  (additional commands may be recognized by command plugins)
	 */
	// sources query is same as root URL (xml_base) minus trailing slash
	private static String sources_query_with_slash = "";  // set in setXmlBase()
	private static String sources_query_no_slash = ""; // set in setXmlBase();
	private static final String segments_query = "segments";
	private static final String types_query = "types";
	private static final String features_query = "features";
	private static final String query_att = "query_uri";
	private static final String login_query = "login";
	private static final String refresh_query = "refresh";
	private static final String default_feature_format = "das2feature";
	// Determines if DAS2 uses file system, genopub db, or gnomex db to load data track information
	private static String genometry_mode			= Constants.GENOMETRY_MODE;
	private static final String MAINTAINER_EMAIL	= "maintainer_email";
	private static final String XML_BASE			= "xml_base";
	// static String that indicates where annotation files are served from
	// when data track info comes from db
	private static String genometry_server_dir;
	// static String that indicates where analysis files are served 
	// when data track info comes from gnomex db
	private static String gnomex_analysis_root_dir;
	private static String maintainer_email;
	private static String xml_base;
	/** The root directory of the data to be served-up.
	 *  Defaults to system property "user.dir" + "/query_server_smaller/".
	 *  The user can change this by setting a property for "genometry_server_dir"
	 *  on the command line.  For example "java -Dgenometry_server_dir=/home/me/mydir/ ...".
	 */
	private static String data_root;
	private static String types_xslt_file;
	private static final Pattern query_splitter = Pattern.compile("[;\\&]");
	private static final Pattern tagval_splitter = Pattern.compile("=");
	/**
	 *  Top level data structure that holds all the genome models
	 */
	private static GenometryModel gmodel = GenometryModel.getGenometryModel();
	/**
	 *  Top level data structure that holds all the genome models in source/version hierarchy
	 *  maps organism names to list of genome versions for that organism
	 */
	private static Map<String, List<AnnotatedSeqGroup>> organisms = new LinkedHashMap<String, List<AnnotatedSeqGroup>>();
	private final Map<String, Class<? extends AnnotationWriter>> output_registry =
			new HashMap<String, Class<? extends AnnotationWriter>>();
	private final SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	private long date_initialized = 0;
	private String date_init_string = null;
	private static Map<AnnotatedSeqGroup,List<AnnotMapElt>> annots_map = new HashMap<AnnotatedSeqGroup,List<AnnotMapElt>>();    // hash of filenames to annot properties.

	private final Map<AnnotatedSeqGroup, Map<String, String>> genome2graphfiles = new LinkedHashMap<AnnotatedSeqGroup, Map<String, String>>();
	private final Map<AnnotatedSeqGroup, Map<String, String>> genome2graphdirs = new LinkedHashMap<AnnotatedSeqGroup, Map<String, String>>();
	private final HashMap<String, USeqArchive> file2USeqArchive = new HashMap<String, USeqArchive>();
	private Transformer types_transformer;
	private static final boolean DEFAULT_USE_TYPES_XSLT = true;
	private boolean use_types_xslt;

	private static File synonym_file;
	private static File chr_synonym_file;
	private static String org_order_filename;

	@Override
	public void init() throws ServletException {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException ex) {
			Logger.getLogger(GenometryDas2Servlet.class.getName()).log(Level.SEVERE, null, ex);
		}
		//attempt to load fields from System.properties or file
		if (!loadAndSetFields()) {
			throw new ServletException("FAILED to init() GenometryDas2Servlet, aborting!");
		}

		try {
			super.init();
			use_types_xslt = DEFAULT_USE_TYPES_XSLT && (new File(types_xslt_file)).exists();
			if (use_types_xslt) {
				Source type_xslt = new StreamSource(types_xslt_file);
				TransformerFactory transFact = TransformerFactory.newInstance();
				types_transformer = transFact.newTransformer(type_xslt);
			}

			if (!(new File(data_root)).isDirectory()) {
				throw new ServletException("Aborting: Specified directory does not exist: '" + data_root + "'");
			}

			initFormats(output_registry);

			Das2ServerUtils.loadSynonyms(synonym_file, SynonymLookup.getDefaultLookup());
			Das2ServerUtils.loadSynonyms(chr_synonym_file, SynonymLookup.getChromosomeLookup());

			if (genometry_mode.equals(Constants.GENOMETRY_MODE_GNOMEX)) {
        Logger.getLogger(GenometryDas2Servlet.class.getName()).info("Loading genomes from gnomex database....");
        loadGenomesFromGNomEx(null, false);          
      } else if (genometry_mode.equals(Constants.GENOMETRY_MODE_GENOPUB)) {
				Logger.getLogger(GenometryDas2Servlet.class.getName()).info("Loading genomes from relational database....");
				loadGenomesFromGenoPub(null, false);				  
			} else {
				Logger.getLogger(GenometryDas2Servlet.class.getName()).info("Loading genomes from file system....");
				loadGenomesFromFileSystem(data_root, organisms, org_order_filename);
			}

			Das2ServerUtils.printGenomes(organisms);

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new ServletException(ex);
			// this is a major deal... kill the servlet.
			// (don't use System.exit(), as that may kill other processes as well.)
		}

		date_initialized = System.currentTimeMillis();
		date_init_string = date_formatter.format(new Date(date_initialized));
		System.out.println("GenometryDas2Servlet " + RELEASE_VERSION + ", dir: '" + data_root + "', url: '" + xml_base + "'");
	}


	/**
	 * Attempts to load the genometry_server_dir, maintainer_email, and the
	 * xml_base from the servlet context, System.properties or from a
	 * genometryDas2ServletParameters.txt.  Lastly it will set several fields
	 * for the servlet.
	 *
	 * @return true if fields loaded or false if not.
	 */
	private boolean loadAndSetFields() {
		ServletContext context = getServletContext();


		// Indicates if the annotation info comes from the genopub or the file system
		if (context.getInitParameter(Constants.GENOMETRY_MODE) != null ) {
			genometry_mode = context.getInitParameter(Constants.GENOMETRY_MODE);
		}
			
		// When we are getting the datatracks from gnomex, use the gnomex db property to
		// get the genometry_server_dir.
		if (genometry_mode.equals(Constants.GENOMETRY_MODE_GNOMEX)) {
			Session sess = null;
			try {
				String gnomex_server_name = context.getInitParameter(Constants.GNOMEX_SERVER_NAME);
				sess = com.affymetrix.genometry.gnomex.HibernateUtil.getSessionFactory().openSession();
				genometry_server_dir = PropertyDictionaryHelper.getInstance(sess).getDataTrackDirectory(gnomex_server_name);
				gnomex_analysis_root_dir = PropertyDictionaryHelper.getInstance(sess).getAnalysisDirectory(gnomex_server_name);
			} catch (Exception e) {
				System.out.println("\nERROR: Cannot open hibernate session to obtain gnomex property " + e.toString());
			} finally {
				com.affymetrix.genometry.gnomex.HibernateUtil.getSessionFactory().close();
			}

		} else if (genometry_mode.equals(Constants.GENOMETRY_MODE_GENOPUB)) {
			genometry_server_dir = context.getInitParameter(Constants.GENOMETRY_SERVER_DIR_GENOPUB);
		} else {
			genometry_server_dir = context.getInitParameter(Constants.GENOMETRY_SERVER_DIR_CLASSIC);
		}

		maintainer_email = context.getInitParameter(MAINTAINER_EMAIL);
		xml_base = context.getInitParameter(XML_BASE);


		//attempt to get from System.properties
		if (genometry_server_dir == null || maintainer_email == null || xml_base == null) {
			if (genometry_mode.equals(Constants.GENOMETRY_MODE_GENOPUB)) {
				genometry_server_dir = System.getProperty("das2_" + Constants.GENOMETRY_SERVER_DIR_GENOPUB);				
			} else {
				genometry_server_dir = System.getProperty("das2_" + Constants.GENOMETRY_SERVER_DIR_CLASSIC);
			}
			maintainer_email = System.getProperty("das2_" + MAINTAINER_EMAIL);
			xml_base = System.getProperty("das2_" + XML_BASE);
		}

		//attempt to load from file?
		if (genometry_server_dir == null || maintainer_email == null || xml_base == null) {
			//look for file
			File p = new File("genometryDas2ServletParameters.txt");
			if (!p.exists()) {
				System.out.println("\tLooking for but couldn't find " + p);
				File dir = new File(System.getProperty("user.dir"));
				p = new File(dir, "genometryDas2ServletParameters.txt");
				//look for it in the users home
				if (!p.exists()) {
					System.out.println("\tLooking for but couldn't find " + p);
					dir = new File(System.getProperty("user.home"));
					p = new File(dir, "genometryDas2ServletParameters.txt");
					if (!p.exists()) {
						System.out.println("\tLooking for but couldn't find " + p);
						return false;
					}
				}
			}
			
			System.out.println("\tFound and loading " + p);

			//load file
			HashMap<String, String> prop = Das2ServerUtils.loadFileIntoHashMap(p);

			//load fields
			if (genometry_mode.equals(Constants.GENOMETRY_MODE_GENOPUB)) {
				if (genometry_server_dir == null && prop.containsKey(Constants.GENOMETRY_SERVER_DIR_GENOPUB)) {
					genometry_server_dir = prop.get(Constants.GENOMETRY_SERVER_DIR_GENOPUB);
				}

			} else if (genometry_mode.equals(Constants.GENOMETRY_MODE_CLASSIC)) {
				if (genometry_server_dir == null && prop.containsKey(Constants.GENOMETRY_SERVER_DIR_CLASSIC)) {
					genometry_server_dir = prop.get(Constants.GENOMETRY_SERVER_DIR_CLASSIC);
				}				
			}
			if (maintainer_email == null && prop.containsKey(MAINTAINER_EMAIL)) {
				maintainer_email = prop.get(MAINTAINER_EMAIL);
			}
			if (xml_base == null && prop.containsKey(XML_BASE)) {
				xml_base = prop.get(XML_BASE);
			}
			//check for data dir and xml base, email is apparently optional
			if (genometry_server_dir == null || xml_base == null) {
				System.out.println("\tERROR: could not set the following:\n\t\tgenometry_server_dir\t" + genometry_server_dir + "\n\t\txml_base\t" + xml_base);
				return false;
			}
		}

		if (genometry_server_dir != null  && !genometry_server_dir.endsWith("/")) {
			genometry_server_dir += "/";      
		}
		
		//set data root
		// We have two possible data roots:  if running in "db" mode, use the
		// db annotations dir as data root; if running in "filesystem" mode,
		// use the server dir as the data root.
		// Note adding an extra "/" at the end of the directory just to be certain
		// there is one there.  If it ends up with two "/" characters, that hurts nothing
		data_root = genometry_server_dir + "/";			

		//set various files as Strings
		synonym_file = new File(data_root + "synonyms.txt");
		chr_synonym_file = new File(data_root + "synonyms.txt");
		types_xslt_file = data_root + "types.xslt";
		org_order_filename = data_root + "organism_order.txt";

		setXmlBase(xml_base);

		return true;
	}

	private static void initFormats(Map<String, Class<? extends AnnotationWriter>> output_registry) {
		output_registry.put("link.psl", ProbeSetDisplayPlugin.class);
		output_registry.put("bps", BpsParser.class);
		output_registry.put("psl", PSLParser.class);
		output_registry.put("bed", BedParser.class);
		output_registry.put("simplebed", SimpleBedParser.class);
		output_registry.put("bgn", BgnParser.class);
		output_registry.put("brs", BrsParser.class);
		output_registry.put("gff", GFFParser.class);
		output_registry.put("das2feature", Das2FeatureSaxParser.class);
		output_registry.put("das2xml", Das2FeatureSaxParser.class);
		output_registry.put("bar", BarParser.class);
		output_registry.put(Das2FeatureSaxParser.FEATURES_CONTENT_TYPE, Das2FeatureSaxParser.class);
		output_registry.put(Das2FeatureSaxParser.FEATURES_CONTENT_SUBTYPE, Das2FeatureSaxParser.class);
		output_registry.put("bp2", Bprobe1Parser.class);
		output_registry.put("ead", ExonArrayDesignParser.class);
		output_registry.put("cyt", CytobandParser.class);
	}


	private boolean loadGenomesFromGenoPub(GenoPubSecurity genoPubSecurity, boolean isServerRefreshMode)  {
		Logger.getLogger(GenometryDas2Servlet.class.getName()).info("Loading Genomes from DB");
		Session sess  = null;
		Transaction tx = null;
		File file = null;
		try {
			sess  = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			AnnotationQuery annotationQuery = new AnnotationQuery();
			annotationQuery.runAnnotationQuery(sess, genoPubSecurity, isServerRefreshMode);
			for (Organism organism : annotationQuery.getOrganisms()) {
				Logger.getLogger(GenometryDas2Servlet.class.getName()).log(Level.FINE, "Organism = {0}", organism.getName());

				// Get genome versions for an organism.  
				for (String genomeVersionName : annotationQuery.getVersionNames(organism)) {

					GenomeVersion gv = annotationQuery.getGenomeVersion(genomeVersionName);
					
					// If this is a server refresh, unload annotations from DAS/2 that were deleted from GenoPub.  Otherwise,
					// if this is a full reload, just clear out all pending unloads.
					for (UnloadAnnotation unloadAnnotation : AnnotationQuery.getUnloadedAnnotations(sess, genoPubSecurity, gv)) {

						if (isServerRefreshMode) {
							AnnotatedSeqGroup genomeVersion = gmodel.getSeqGroup(genomeVersionName);
							if (genomeVersion != null) {
								Das2ServerUtils.unloadGenoPubAnnot(unloadAnnotation.getTypeName(), genomeVersion, genome2graphdirs.get(genomeVersion));																
							}
						}

						// Get rid of the pending unload entry
						sess.delete(unloadAnnotation);
					}
					
					// Obtain the list of annotations and segments for this genome version
					List<QualifiedAnnotation> qualifiedAnnotations = annotationQuery.getQualifiedAnnotations(organism, genomeVersionName);
					List<Segment> segments = annotationQuery.getSegments(organism, genomeVersionName);

					// Ignore genome version if there are not annotations nor sequence associated with it.
					if (!gv.hasSequence(data_root) && (qualifiedAnnotations == null || qualifiedAnnotations.isEmpty())) {
						Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
								Level.FINE, "Bypassing Genome version = {0}. No annotations nor sequence exists.", genomeVersionName);
						continue;
					}

					// Ignore genome version if no segment information is present.
					if (segments == null || segments.isEmpty()) {
						Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
								Level.WARNING, "Bypassing annotations/sequence for Genome version {0}.  No segments have been defined.", genomeVersionName);
						continue;
					}

					Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
							Level.FINE, "Genome version = {0}", genomeVersionName);

					// Instantiate an AnnotatedSeqGroup (the genome version).    
					AnnotatedSeqGroup genomeVersion = gmodel.getSeqGroup(genomeVersionName);
					if(genomeVersion == null){
						genomeVersion = new Das2AnnotatedSeqGroup(genomeVersionName);
						gmodel.addSeqGroup(genomeVersion);
					} 
					genomeVersion.setOrganism(organism.getName());


					// Hash the organism and genome version
					List<AnnotatedSeqGroup> versions = organisms.get(organism.getName());
					if (versions == null) {
						versions = new ArrayList<AnnotatedSeqGroup>();
						organisms.put(organism.getName(), versions);
					}
					versions.add(genomeVersion);


					// Create SmartAnnotBioSeqs (chromosomes) for the genome version
					if (segments != null) {
						for(Segment segment : segments) {
							BioSeq chrom = genomeVersion.addSeq(segment.getName(), segment.getLength().intValue());
							chrom.setVersion(genomeVersionName);
						}

					}

					// Get the hash maps for graph dirs and graph files for this genome version
					Map<String,String> graph_name2dir = genome2graphdirs.get(genomeVersion);
					if (graph_name2dir == null) {
						graph_name2dir = new LinkedHashMap<String, String>();
						genome2graphdirs.put(genomeVersion, graph_name2dir);
					}
					Map<String,String> graph_name2file = genome2graphfiles.get(genomeVersion);
					if (graph_name2file == null) {
						graph_name2file = new LinkedHashMap<String, String>();
						genome2graphfiles.put(genomeVersion, graph_name2file);
					}

					// Load annotations for the genome version
					for (QualifiedAnnotation qa : qualifiedAnnotations) {

						String fileName = qa.getAnnotation().getQualifiedFileName(genometry_server_dir);    
						String typePrefix = qa.getTypePrefix(); 
						
						file = new File(fileName);
						
						if (file.exists()) {
							Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
									Level.FINE, "Annotation type = {0}\t{1}", new Object[]{typePrefix != null ? typePrefix : "", fileName != null ? fileName : ""});
							
							if (file.isDirectory() ) {
								if (isMultiFileDataTrackType(file)) {
									Das2ServerUtils.loadGenoPubAnnotFromDir(typePrefix, 
											file.getPath(), 
											genomeVersion, 
											file, 
											qa.getAnnotation().getIdAnnotation(),
											graph_name2dir);                  

								} else if (!file.exists() || file.list() == null || file.list().length == 0) {
									Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
											Level.WARNING, "Bypassing annotation {0}.  No files associated with this annotation.", typePrefix);
								} else {
									Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
											Level.WARNING, "Bypassing annotation {0} for file {1}. Only the bar format permits multiple annotation files.", new Object[]{typePrefix, fileName});
								}


							} else {
								//watch out for single file bai indexes, just warn and skip
								if (file.getName().toLowerCase().endsWith(".bai")){
									Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
											Level.WARNING, "Bypassing annotation '{'0'}'.  No associated bam alignment file for {0}", file);
								}
								
								else {
									Das2ServerUtils.loadGenoPubAnnotsFromFile(genometry_server_dir,
										file, 
										genomeVersion, 
										annots_map,
										typePrefix, 
										qa.getAnnotation().getIdAnnotation(),
										graph_name2file); 
								}
							}
							
							// Update the flag indicating that the annotation has been loaded
							if (qa.getAnnotation().getIsLoaded() != null && qa.getAnnotation().getIsLoaded().equals("N")) {
								qa.getAnnotation().setIsLoaded("Y");								
							}
							
						} else {
							Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
									Level.WARNING, "Annotation not loaded. File does not exist or is not supported: {0}\t{1}", new Object[]{typePrefix != null ? typePrefix : "", fileName != null ? fileName : ""});
						}

					}
					
					Optimize.genome(genomeVersion);
				}

				
			}
			// Commit updates and deletes to the genopub database
			sess.flush();
			tx.commit();


		} catch (Exception e) {
			Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
					Level.SEVERE, "Problems reading annotations from file '"+file+"' in database {0}", e.toString());
			e.printStackTrace();
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			HibernateUtil.getSessionFactory().close();
		}
		return true;

	}

	 private boolean loadGenomesFromGNomEx(GNomExSecurity gnomexSecurity, boolean isServerRefreshMode)  {
	    Logger.getLogger(GenometryDas2Servlet.class.getName()).info("Loading Genomes from GNomEx DB");
	    Session sess  = null;
	    Transaction tx = null;
	    File file = null;
	    try {
	      sess  = com.affymetrix.genometry.gnomex.HibernateUtil.getSessionFactory().openSession();
	      tx = sess.beginTransaction();
	      
	      // Let's refresh the gnomex dictionaries since new genome builds and organisms
	      // could have been added since last refresh
	      if (isServerRefreshMode) {
	        hci.gnomex.utility.DictionaryHelper.reloadLimited(sess);
	      }

	      DataTrackQuery dataTrackQuery = new DataTrackQuery();
	      dataTrackQuery.runDataTrackQuery(sess, gnomexSecurity != null ? gnomexSecurity.getSecAdvisor() : null, isServerRefreshMode);
        
	      // If this is server refresh mode, the qualifiedDataTracks in the above query gets only
        // those data tracks not yet loaded.  We need the full list of data tracks for the genome
        // build to know if this genome build is "empty" and should be removed so that it doesn't
        // show up in the sources query.
	      DataTrackQuery dataTrackQueryAll = null;
        if (isServerRefreshMode) {
          dataTrackQueryAll= new DataTrackQuery();
          dataTrackQueryAll.runDataTrackQuery(sess, gnomexSecurity != null ? gnomexSecurity.getSecAdvisor() : null, false);
        } else {
          dataTrackQueryAll = dataTrackQuery;
        }
        
        // For server refresh mode, we need to check for genome versions that have been removed.  
        // Loop through the organism genome builds that are hashed and remove those
        // that don't have a corresponding entry from the data track query
        if (isServerRefreshMode) {
          for (Map.Entry<String, List<AnnotatedSeqGroup>> oentry : organisms.entrySet()) {
            String org = oentry.getKey();
            List<AnnotatedSeqGroup> versions = oentry.getValue();
            ArrayList<AnnotatedSeqGroup> staleVersions = new ArrayList<AnnotatedSeqGroup>();
            for (AnnotatedSeqGroup version : versions) {
              GenomeBuild gb = dataTrackQueryAll.getGenomeBuild(version.getID());
              if (gb == null) {
                staleVersions.add(version);
              }
            }
            for (AnnotatedSeqGroup staleVersion : staleVersions) {
              Logger.getLogger(GenometryDas2Servlet.class.getName()).log(Level.WARNING, "Removing stale genome version {0}", staleVersion.getID());
              versions.remove(staleVersion);
            }
          }
          
        }
        
	      for (hci.gnomex.model.Organism organism : dataTrackQueryAll.getOrganisms()) {
	        Logger.getLogger(GenometryDas2Servlet.class.getName()).log(Level.FINE, "Organism = {0}", organism.getDas2Name());

	        // Get genome versions for an organism.  
	        for (String genomeBuildName : dataTrackQueryAll.getGenomeBuildNames(organism)) {

	          GenomeBuild gb = dataTrackQueryAll.getGenomeBuild(genomeBuildName);
	          
	          // If this is a server refresh, unload data tracks from DAS/2 that were deleted from GNomEx.  Otherwise,
	          // if this is a full reload, just clear out all pending unloads.
	          for (UnloadDataTrack unloadDataTrack : DataTrackQuery.getUnloadedDataTracks(sess, gnomexSecurity != null ? gnomexSecurity.getSecAdvisor() : null, gb)) {

	            if (isServerRefreshMode) {
	              AnnotatedSeqGroup genomeVersion = gmodel.getSeqGroup(genomeBuildName);
	              if (genomeVersion != null) {
	                Das2ServerUtils.unloadGenoPubAnnot(unloadDataTrack.getTypeName(), genomeVersion, null);                               
	              }
	            }

	            // Get rid of the pending unload entry
	            sess.delete(unloadDataTrack);
	          }
	          
	          // Obtain the list of data tracks and segments for this genome build
	          List<QualifiedDataTrack> qualifiedDataTracks = dataTrackQuery.getQualifiedDataTracks(organism, genomeBuildName);	          
	          List<hci.gnomex.model.Segment> segments = dataTrackQuery.getSegments(organism, genomeBuildName);
	          
	          // In server refresh mode, we need the list of all data tracks, to figure out if the the
	          // genome build is empty and should be dropped from the cache.
	          List<QualifiedDataTrack> qualifiedDataTracksAll = null;
	          qualifiedDataTracksAll = dataTrackQueryAll.getQualifiedDataTracks(organism, genomeBuildName);
	          
	          System.out.println(genomeBuildName + " qualifiedDataTracksAll.size=" + (qualifiedDataTracksAll != null ? qualifiedDataTracksAll.size() : "") + " segments=" + (segments != null ? segments.size() : ""));
	            
	          if (!gb.hasSequence(data_root) && (qualifiedDataTracksAll == null || qualifiedDataTracksAll.isEmpty())) {
	            Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
	                Level.FINE, "Bypassing Genome version = {0}. No data tracks nor sequence exists.", genomeBuildName);
	            
	            // If this is refresh mode, remove "empty" genome build from organisms
	            if (isServerRefreshMode) {
	              List<AnnotatedSeqGroup> versions = organisms.get(organism.getDas2Name());
	              if (versions != null) {
	                for (AnnotatedSeqGroup gv : versions) {
	                  if (gv.getID().equals(genomeBuildName)) {
	                    Logger.getLogger(GenometryDas2Servlet.class.getName()).log(Level.WARNING, "Remove invalid genome version {0}", genomeBuildName);
	                    versions.remove(gv);
	                    break;
	                  }
	                }
	              }
	            }
	            
	            continue;
	          }

	          // Ignore genome build if no segment information is present.
	          if (segments == null || segments.isEmpty()) {
	            Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
	                Level.WARNING, "Bypassing data tracks/sequence for Genome version {0}.  No segments have been defined.", genomeBuildName);

	            // If this is refresh mode, remove invalid (no segments) genome build from organisms
              if (isServerRefreshMode) {
                List<AnnotatedSeqGroup> versions = organisms.get(organism.getDas2Name());
                if (versions != null) {
                  for (AnnotatedSeqGroup gv : versions) {
                    if (gv.getID().equals(genomeBuildName)) {
                      Logger.getLogger(GenometryDas2Servlet.class.getName()).log(Level.WARNING, "Remove invalid genome version {0}", genomeBuildName);
                      versions.remove(gv);
                      break;
                    }
                  }
                }
              }

              
	            continue;
	          }

	          Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
	              Level.FINE, "Genome version = {0}", genomeBuildName);

	          // Instantiate an AnnotatedSeqGroup (the genome version).         
	          AnnotatedSeqGroup genomeVersion = gmodel.addSeqGroup(genomeBuildName);
	          genomeVersion.setOrganism(organism.getDas2Name());


	          // Hash the organism and genome version
	          List<AnnotatedSeqGroup> versions = organisms.get(organism.getDas2Name());
	          if (versions == null) {
	            versions = new ArrayList<AnnotatedSeqGroup>();
	            organisms.put(organism.getDas2Name(), versions);
	          }
	          
	          // If this is server refresh mode, make sure we haven't already loaded this genome
	          // version.
	          boolean found = false;
	          if (isServerRefreshMode) {
	            for (AnnotatedSeqGroup gv : versions) {
	              if (gv.getID().equals(genomeBuildName)) {
	                found = true;
	                break;
	              }
	            }
            }
	          if (!found) {
	            versions.add(genomeVersion);
	          }


	          // Create SmartAnnotBioSeqs (chromosomes) for the genome version
	          if (segments != null) {
	            for(hci.gnomex.model.Segment segment : segments) {
	              BioSeq chrom = genomeVersion.addSeq(segment.getName(), segment.getLength().intValue());
	              chrom.setVersion(genomeBuildName);
	            }

	          }

	          // Get the hash maps for graph dirs and graph files for this genome version
	          Map<String,String> graph_name2dir = genome2graphdirs.get(genomeVersion);
	          if (graph_name2dir == null) {
	            graph_name2dir = new LinkedHashMap<String, String>();
	            genome2graphdirs.put(genomeVersion, graph_name2dir);
	          }
	          Map<String,String> graph_name2file = genome2graphfiles.get(genomeVersion);
	          if (graph_name2file == null) {
	            graph_name2file = new LinkedHashMap<String, String>();
	            genome2graphfiles.put(genomeVersion, graph_name2file);
	          }

	          // Load data track for the genome version
	          for (QualifiedDataTrack qdt : qualifiedDataTracks) {

	            String fileName = qdt.getDataTrack().getQualifiedFileName(genometry_server_dir, gnomex_analysis_root_dir);    
	            String typePrefix = qdt.getTypePrefix(); 
	            
	            file = new File(fileName);
	            
	            
	            if (file.exists()) {
	              Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
	                  Level.FINE, "Data track type = {0}\t{1}", new Object[]{typePrefix != null ? typePrefix : "", fileName != null ? fileName : ""});
	              
	              if (file.isDirectory() ) {
	                if (isMultiFileDataTrackType(file)) {
	                  Das2ServerUtils.loadGenoPubAnnotFromDir(typePrefix, 
	                      file.getPath(), 
	                      genomeVersion, 
	                      file, 
	                      qdt.getDataTrack().getIdDataTrack(),
	                      graph_name2dir);                  

	                } else if (!file.exists() || file.list() == null || file.list().length == 0) {
	                  Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
	                      Level.WARNING, "Bypassing data track {0}.  No files associated with this data track.", typePrefix);
	                } else {
	                  Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
	                      Level.WARNING, "Bypassing data track {0} for file {1}. Only the bar format permits multiple data track files.", new Object[]{typePrefix, fileName});
	                }


	              } else {
	                //watch out for single file bai indexes, just warn and skip
	                if (file.getName().toLowerCase().endsWith(".bai")){
	                  Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
	                      Level.WARNING, "Bypassing data track '{'0'}'.  No associated bam alignment file for {0}", file);
	                }
	                
	                else {
	                  Das2ServerUtils.loadGenoPubAnnotsFromFile(genometry_server_dir,
	                    file, 
	                    genomeVersion, 
	                    annots_map,
	                    typePrefix, 
	                    qdt.getDataTrack().getIdDataTrack(),
	                    graph_name2file); 
	                }
	              }
	              
	              // Update the flag indicating that the data track has been loaded
	              if (qdt.getDataTrack().getIsLoaded() != null && qdt.getDataTrack().getIsLoaded().equals("N")) {
	                qdt.getDataTrack().setIsLoaded("Y");                
	              }
	              
	            } else {
	              Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
	                  Level.WARNING, "Data track not loaded. File does not exist or is not supported: {0}\t{1}", new Object[]{typePrefix != null ? typePrefix : "", fileName != null ? fileName : ""});
	            }

	          }
	          
	          Optimize.genome(genomeVersion);
	        }

	        
	      }
	      // Commit updates and deletes to the genopub database
	      sess.flush();
	      tx.commit();


	    } catch (Exception e) {
	      Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
	          Level.SEVERE, "Problems reading data tracks from file '"+file+"' in gnomex database {0}", e.toString());
	      e.printStackTrace();
	      if (tx != null) {
	        tx.rollback();
	      }
	    } finally {
	      com.affymetrix.genometry.gnomex.HibernateUtil.getSessionFactory().close();
	    }
	    return true;

	  }

	private static boolean isMultiFileDataTrackType(File dir) {
		if (dir.exists() && dir.isDirectory()) {
			String[] childFileNames = dir.list();
			if (childFileNames != null) {
				for (int x = 0; x < childFileNames.length; x++) {
					if (childFileNames[x].endsWith("bar")) {
						return true;
					}
				}
			}
		}
		return false;
	}
	 
	

	private void loadGenomesFromFileSystem(String dataRoot,
			Map<String, List<AnnotatedSeqGroup>> organisms,
			String org_order_filename) throws IOException {
		// get list of all directories in data root
		// each directory corresponds to a different organism
		//    organism_name = directory name
		//    subdirectory for each genome_version
		//    for each genome version:
		//       genome_version_name = directory name
		//       parse liftAll or chromInfo file to create new AnnotatedSeqGroup for genome
		//           in GenometryModel
		//       for each file in genome_version directory
		//           if directory, recurse in
		//           else try to parse and annotate seqs based on file suffix (.xyz)

		File top_level = new File(dataRoot);
		if (!top_level.exists() && !top_level.isDirectory()) {
			throw new IOException("'" + top_level + "' does not exist or is not a directory");
		}

		FileFilter filter = new HiddenFileFilter(new DirectoryFilter());
		for (File org : top_level.listFiles(filter)) {
			for (File version : org.listFiles(filter)) {
				loadGenome(version, org.getName(), dataRoot);
			}
		}

		Das2ServerUtils.sortGenomes(organisms, org_order_filename);
	}

	private void loadGenome(File genome_directory, String organism, String dataRoot) throws IOException {
		String genome_version = genome_directory.getName();

		// Instantiate an AnnotatedSeqGroup (the genome version).    
		AnnotatedSeqGroup genome = gmodel.getSeqGroup(genome_version);
		if(genome == null){
			genome = new Das2AnnotatedSeqGroup(genome_version);
			gmodel.addSeqGroup(genome);
		} 
	
		// create MutableAnnotatedSeqs for each chromosome via ChromInfoParser
		Das2ServerUtils.parseChromosomeData(genome_directory, genome);
		
		genome2graphdirs.put(genome, new LinkedHashMap<String, String>());
		genome2graphfiles.put(genome, new LinkedHashMap<String, String>());
		genome.setOrganism(organism);
		List<AnnotatedSeqGroup> versions = organisms.get(organism);
		if (versions == null) {
			versions = new ArrayList<AnnotatedSeqGroup>();
			organisms.put(organism, versions);
		}
		versions.add(genome);

		// search genome directory for annotation files to load
		// (and recursively descend through subdirectories doing same)
		Map<String, String> graph_name2dir = genome2graphdirs.get(genome);
		Map<String, String> graph_name2file = genome2graphfiles.get(genome);
		Das2ServerUtils.loadAnnots(genome_directory, genome, annots_map, graph_name2dir, graph_name2file, dataRoot);

		// optimize genome by replacing second-level syms with IntervalSearchSyms
		Optimize.genome(genome);

		// Garbage collection after initialization
		// only needed for debugging purposes (to see how much memory is actually used in initialization)
		System.gc();
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		String path_info = request.getPathInfo();
		String query = request.getQueryString();
		System.out.println("GenometryDas2Servlet received POST request: ");
		System.out.println("   path: " + path_info);
		System.out.println("   query: " + query);
	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		System.out.println("GenometryDas2Servlet received PUT request: ");
	}

	@Override
	public long getLastModified(HttpServletRequest request) {
		return date_initialized;
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		handleDas2Request(response, request);
	}

	private void handleDas2Request(HttpServletResponse response, HttpServletRequest request) throws IOException {
		String path_info = request.getPathInfo();
		if (DEBUG) {
			String query = request.getQueryString();
			System.out.println("GenometryDas2Servlet received GET request: ");
			System.out.println("   path: " + path_info);
			System.out.println("   query: " + query);
			if (query != null) System.out.println("   decoded: "+GeneralUtils.URLDecode(query));
		}
		if (path_info == null || path_info.trim().length() == 0 || path_info.endsWith(sources_query_no_slash) || path_info.endsWith(sources_query_with_slash)) {
			handleSourcesRequest(request, response, date_init_string);
		} else if (path_info.endsWith(login_query)) {
			handleLoginRequest(request, response);
		} else if (path_info.endsWith(refresh_query)) {
			handleRefreshRequest(request, response);
		} else {		
			AnnotatedSeqGroup genome = getGenome(path_info);
			if (genome == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Query was not recognized, possibly the genome name is incorrect or missing from path? " + SERVER_SYNTAX_EXPLANATION);
				return;
			}
			String das_command = path_info.substring(path_info.lastIndexOf('/') + 1);
			if (das_command.equals(segments_query)) {
				handleSegmentsRequest(genome, request, response);
			} else if (das_command.equals(types_query)) {
				handleTypesRequest(genome, request, response);
			} else if (das_command.equals(features_query)) {
				handleFeaturesRequest(genome, request, response);
			} else if (genome.getSeq(das_command) != null) {
				handleSequenceRequest(genome, request, response);
			} else {
				System.out.println("DAS2 request " + path_info + " not recognized, setting HTTP status header to 400, BAD_REQUEST");
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Query was not recognized. " + SERVER_SYNTAX_EXPLANATION);
			}
		}
	}
	
	private AnnotSecurity getAnnotSecurity(HttpServletRequest request) {
    AnnotSecurity annotSecurity = null;
    if (genometry_mode.equals(Constants.GENOMETRY_MODE_GENOPUB)) {
      annotSecurity = this.getGenoPubSecurity(request);
    } else if (genometry_mode.equals(Constants.GENOMETRY_MODE_GNOMEX)){
      annotSecurity = this.getGNomExSecurity(request);
    }
    return annotSecurity;
	}

	private GenoPubSecurity getGenoPubSecurity(HttpServletRequest request) {
		if (!genometry_mode.equals(Constants.GENOMETRY_MODE_GENOPUB)) {
			return null;
		}

		GenoPubSecurity genoPubSecurity = null;

		// Get the GenoPubSecurity    
		try {
			genoPubSecurity = GenoPubSecurity.class.cast(request.getSession().getAttribute(this.getClass().getName() + GenoPubSecurity.SESSION_KEY));
			if (genoPubSecurity == null) {
				Session sess = sess = HibernateUtil.getSessionFactory().openSession();					

				genoPubSecurity = new GenoPubSecurity(sess, 
						request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null, 
						true,
						request.getUserPrincipal() != null ? request.isUserInRole(GenoPubSecurity.ADMIN_ROLE) : false,
						request.getUserPrincipal() != null ? request.isUserInRole(GenoPubSecurity.GUEST_ROLE) : true,
						false);
				genoPubSecurity.setBaseURL(request.getRequestURL().toString(), request.getServletPath(), request.getPathInfo());
				request.getSession().setAttribute(this.getClass().getName() + GenoPubSecurity.SESSION_KEY, genoPubSecurity);

			}
		} catch (Exception e ){     
			System.out.println(e.toString());
			e.printStackTrace();
		} finally {
		  HibernateUtil.getSessionFactory().close();
		}

		return genoPubSecurity;

	}
	
	private GNomExSecurity getGNomExSecurity(HttpServletRequest request) {
	  
	  if (!genometry_mode.equals(Constants.GENOMETRY_MODE_GNOMEX)) {
      return null;
    }

    GNomExSecurity gnomexSecurity = null;

    // Get the SecurityAdvisor    
    try {
      gnomexSecurity = GNomExSecurity.class.cast(request.getSession().getAttribute(this.getClass().getName() + GNomExSecurity.SESSION_KEY));
      if (gnomexSecurity == null) {
        Session sess = com.affymetrix.genometry.gnomex.HibernateUtil.getSessionFactory().openSession();         
        
        SecurityAdvisor secAdvisor = SecurityAdvisor.create(sess, request.getUserPrincipal().getName());
        
        gnomexSecurity = new GNomExSecurity(sess, request.getServerName(), secAdvisor, true);
        
        ServletContext context = getServletContext();
        String gnomex_server_name = context.getInitParameter(Constants.GNOMEX_SERVER_NAME);
        String gnomex_port = context.getInitParameter(Constants.GNOMEX_SERVER_PORT);

        gnomexSecurity.setDataTrackInfoURL(gnomex_server_name, gnomex_port);

        request.getSession().setAttribute(this.getClass().getName() + GNomExSecurity.SESSION_KEY, gnomexSecurity);

      }
    } catch (Exception e ){     
      System.out.println(e.toString());
      e.printStackTrace();
    } finally {
      com.affymetrix.genometry.gnomex.HibernateUtil.getSessionFactory().close();
    }

    return gnomexSecurity;
    
	}


	/**
	 * Extracts name of (versioned?) genome from servlet request,
	 *    and uses to retrieve AnnotatedSeqGroup (genome) from GenometryModel
	 */
	private static AnnotatedSeqGroup getGenome(String path_info) {
		int last_slash = path_info.lastIndexOf('/');
		int prev_slash = path_info.lastIndexOf('/', last_slash - 1);
		String genome_name = path_info.substring(prev_slash + 1, last_slash);
		AnnotatedSeqGroup genome = gmodel.getSeqGroup(genome_name);
		if (genome == null) {
			System.out.println("unknown genome version: '" + genome_name + "' with request: " + path_info);
		}
		return genome;
	}

	/**
	 * Handle a sequence request.
	 * @param request
	 * @param response
	 * @throws java.io.IOExceptionhandleSequenceRequest
	 */
	@SuppressWarnings("deprecation")
	private void handleSequenceRequest(AnnotatedSeqGroup genome, HttpServletRequest request, HttpServletResponse response)
	throws IOException {
		String queryString = request.getQueryString();
		if (queryString == null) {
			System.out.println("No query string, aborting");
			return;
		}

		List<String> formats = new ArrayList<String>();
		List<String> ranges = new ArrayList<String>();

		splitSequenceQuery(GeneralUtils.URLDecode(request.getQueryString()), formats, ranges);

		if (ranges.size() > 1) {
			System.out.println("too many range params, aborting");
			return;
		}
		if (formats.size() > 1) {
			System.out.println("too many format params, aborting");
			return;
		}

		String path_info = request.getPathInfo();
		String seqname = path_info.substring(path_info.lastIndexOf('/') + 1);

		SeqSpan span = null;
		if (ranges.size() == 1) {
			span = Das2ServerUtils.getLocationSpan(seqname, ranges.get(0), genome);
		}

		String format = "";
		if (formats.size() == 1) {
			format = formats.get(0);
		}

		String sequence_directory = data_root + genome.getOrganism() + "/" + genome.getID() + "/dna/";
		if (GenometryDas2Servlet.genometry_mode.equals(Constants.GENOMETRY_MODE_GENOPUB) || genometry_mode.equals(Constants.GENOMETRY_MODE_GNOMEX)) {
			try {
				// Get the  annotation security which will determine the sequence directory
				AnnotSecurity annotSecurity = this.getAnnotSecurity(request);
				sequence_directory = annotSecurity.getSequenceDirectory(data_root, genome);
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
		}

		// retrieval of partial chromosome in raw format
		if (format.equals("raw")) {
			ServletUtils.retrieveRAW(ranges, span, sequence_directory, seqname, response, request);
			return;
		}

		// retrieval of whole chromosome in bnib format
		if (format.equals("bnib")) {
			ServletUtils.retrieveBNIB(sequence_directory, seqname, response, request);
			return;
		}

		if (format.equals("fasta")) {
			ServletUtils.retrieveFASTA(ranges, span, sequence_directory, genome.getOrganism(), seqname, response, request);
			return;
		}

		PrintWriter pw = response.getWriter();
		pw.println("This DAS/2 server cannot currently handle request:    ");
		pw.println(request.getRequestURL().toString());
	}


	private static void handleSourcesRequest(HttpServletRequest request, HttpServletResponse response, String date_init_string)
	throws IOException {
		response.setContentType(SOURCES_CONTENT_TYPE);
		PrintWriter pw = response.getWriter();

		String xbase = getXmlBase(request);
		printXmlDeclaration(pw);
		pw.println("<SOURCES");
		pw.println("    xmlns=\"" + DAS2_NAMESPACE + "\"");
		pw.println("    xml:base=\"" + xbase + "\" >");
		if (maintainer_email != null && maintainer_email.length() > 0) {
			pw.println("  <MAINTAINER email=\"" + maintainer_email + "\" />");
		}
		// other elements to add:

		for (Map.Entry<String, List<AnnotatedSeqGroup>> oentry : organisms.entrySet()) {
			String org = oentry.getKey();
			List<AnnotatedSeqGroup> versions = oentry.getValue();
			pw.println("  <SOURCE uri=\"" + org + "\" title=\"" + org + "\" >");

			for (AnnotatedSeqGroup genome : versions) {
				Das2Coords coords = genomeid2coord.get(genome.getID());
				if (USE_CREATED_ATT) {
					pw.println("      <VERSION uri=\"" + genome.getID() + "\" title=\"" + genome.getID() +
							"\" created=\"" + date_init_string + "\" >");
				} else {
					pw.println("      <VERSION uri=\"" + genome.getID() + "\" title=\"" + genome.getID() + "\" >");
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

	private static void handleSegmentsRequest(AnnotatedSeqGroup genome, HttpServletRequest request, HttpServletResponse response)
	throws IOException {
		// genome null check already handled, so if it get this far the genome is non-null
		Das2Coords coords = genomeid2coord.get(genome.getID());

		response.setContentType(SEGMENTS_CONTENT_TYPE);

		PrintWriter pw = response.getWriter();
		printXmlDeclaration(pw);

		String xbase = getXmlBase(request) + genome.getID() + "/";
		String segments_uri = xbase + segments_query;
		pw.println("<SEGMENTS ");
		pw.println("    xmlns=\"" + DAS2_NAMESPACE + "\"");
		pw.println("    xml:base=\"" + xbase + "\" ");
		// uri attribute is added purely to satisfy DAS 2.0 RelaxNG schema, it points back to this same document
		pw.println("    " + URID + "=\"" + segments_uri + "\" >");


		for (BioSeq aseq : genome.getSeqList()) {
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
		}
		pw.println("</SEGMENTS>");
	}

	/**
	 *  Handles a request for "entry_types", building a "<DASTYPES>" response
	 *  into the HttpServletRequest. If this server instance is authorizing, some restricted types will be
	 *  filtered based on the users Session object. 
	 *  
	 */
	private void handleTypesRequest(AnnotatedSeqGroup genome, HttpServletRequest request, HttpServletResponse response)
	throws IOException {

		// Get the  genopub security which will determine which resources (annotations)
		// are authorized for this user.

		response.setContentType(TYPES_CONTENT_TYPE);
		
		Map<String, SimpleDas2Type> types_hash = Das2ServerUtils.getAnnotationTypes(data_root,genome,getAnnotSecurity(request));
		if(genome instanceof Das2AnnotatedSeqGroup){
			Das2ServerUtils.getSymloaderTypes((Das2AnnotatedSeqGroup)genome, this.getAnnotSecurity(request), types_hash);
		}
		Das2ServerUtils.getGraphTypes(data_root, genome, this.getAnnotSecurity(request), types_hash);

		ByteArrayOutputStream buf = null;
		ByteArrayInputStream bais = null;
		PrintWriter pw = null;
		try {
			if (use_types_xslt) {
				buf = new ByteArrayOutputStream(types_hash.size() * 1000);
				pw = new PrintWriter(buf);
			} else {
				pw = response.getWriter();
			}

			String xbase = getXmlBase(request) + genome.getID() + "/";
			List<AnnotMapElt> annotList = annots_map.get(genome);
			writeTypesXML(pw, xbase, types_hash, annotList);

			if (use_types_xslt) {
				pw.flush();
				byte[] buf_array = buf.toByteArray();

				bais = new ByteArrayInputStream(buf_array);
				Source types_doc = new StreamSource(bais);
				Result result = new StreamResult(response.getWriter());
				try {
					types_transformer.transform(types_doc, result);
				} catch (TransformerException ex) {
					ex.printStackTrace();
				}
			}
		} finally {
			GeneralUtils.safeClose(bais);
			GeneralUtils.safeClose(buf);
		}
	}

	private static void writeTypesXML(
			PrintWriter pw,
			String xbase,
			Map<String,SimpleDas2Type> types_hash,
			List<AnnotMapElt> annotList) {
		printXmlDeclaration(pw);
		pw.println("<TYPES ");
		pw.println("    xmlns=\"" + DAS2_NAMESPACE + "\"");
		pw.println("    xml:base=\"" + xbase + "\" >");

		List<String> sorted_types_list = new ArrayList<String>(types_hash.keySet());
		Collections.sort(sorted_types_list);
		for (String feat_type : sorted_types_list) {
			SimpleDas2Type das2Type = types_hash.get(feat_type);
			List<String> formats = das2Type.getFormats();
			Map<String, Object> props = das2Type.getProps();

			String feat_type_encoded = GeneralUtils.URLEncode(feat_type);
			// URLEncoding replaces slashes, want to keep those...
			feat_type_encoded = feat_type_encoded.replaceAll("%2F", "/");

			// Title may be stored in annots.xml file.
			String title = feat_type;

			pw.println("   <TYPE " + URID + "=\"" + feat_type_encoded + "\" " + NAME + "=\"" + title + "\" >");
			if (formats != null) {
				for (String format : formats) {
					pw.println("       <FORMAT name=\"" + format + "\" />");
				}
			}

			// For now, if props is empty from Types request, fill in properties
			// from annots.xml file.
			if (props == null && annotList != null) {
				AnnotMapElt ame = AnnotMapElt.findTitleElt(title, annotList);
				if (ame != null && ame.props != null) {
					props = new HashMap<String, Object>();
					for (Map.Entry<String, String> propEntry : ame.props.entrySet()) {
						if (propEntry.getValue().length() > 0) {
							props.put(propEntry.getKey(), propEntry.getValue());
						}
					}
				}
			}

			// Print properties of annotation as tag/value pairs
			if (props != null) {
				for (Map.Entry<String, Object> entry : props.entrySet()) {
					Object value = entry.getValue();
					if (value != null && !value.equals("")) {
						pw.println("       <PROP key=\"" + entry.getKey() + "\" value=\"" + value + "\" />");
					}
				}
			}

			pw.println("   </TYPE>");
		}
		pw.println("</TYPES>");
	}


	/**We are using basic authentication, so if we get to this point, that means the user has 
	 * passed authentication.  Just send back XML indicating we are authenticated. */
	private void handleLoginRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		boolean authorized = true;

		//send response
		response.setContentType(LOGIN_CONTENT_TYPE);
		PrintWriter pw = response.getWriter();
		String xbase = getXmlBase(request);
		printXmlDeclaration(pw);
		pw.println("<LOGIN");
		pw.println("    xmlns=\"" + DAS2_NAMESPACE + "\"");
		pw.println("    xml:base=\"" + xbase + "\" >");
		if (maintainer_email != null && maintainer_email.length() > 0) {
			pw.println("  <MAINTAINER email=\"" + maintainer_email + "\" />");
		}
		pw.println("\t<AUTHORIZED>" + authorized + "</AUTHORIZED>");
		if (request.getUserPrincipal() != null) {
			pw.println("\t<USERNAME>" + request.getUserPrincipal().getName()  + "</USERNAME>");			
		}
		pw.println("</LOGIN>");
	}
	

	/**Refresh the not-yet-loaded annotations from genopub */
	private void handleRefreshRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
	
		// Refresh is not supported in classic mode
		if (genometry_mode.equals(Constants.GENOMETRY_MODE_CLASSIC)) {
			PrintWriter pw = response.getWriter();
			pw.println("DAS/2 refresh is not supported in classic mode");
			return;
		}
		
		// Guest users cannot perform refresh.
		// Admins can perform refresh and so can normal genopub (non-guest) users.
		// (A non-admins will only reload those annotations he owns.)
    if (getAnnotSecurity(request).isGuestRole()) {
      PrintWriter pw = response.getWriter();
      pw.println("DAS/2 refresh cannot by performed by guest users.");
      return;
    }
		
		Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
				Level.INFO, "Refreshing DAS2 server.  User: {0}", request.getUserPrincipal().getName());
		try {

			// Reload the annotation files
			Logger.getLogger(GenometryDas2Servlet.class.getName()).info("Loading genomes from relational database....");
			
			// Refresh the authorized resources for this user
			Logger.getLogger(GenometryDas2Servlet.class.getName()).info("Refreshing authorized resources....");
			Session sess  = null;
			if (genometry_mode.equals(Constants.GENOMETRY_MODE_GENOPUB)) {
			    sess = com.affymetrix.genometry.genopub.HibernateUtil.getSessionFactory().openSession();
			    this.loadGenomesFromGenoPub(getGenoPubSecurity(request), true);
	        this.getGenoPubSecurity(request).loadAuthorizedResources(sess);
			} else if (genometry_mode.equals(Constants.GENOMETRY_MODE_GNOMEX)) {
			  sess = com.affymetrix.genometry.gnomex.HibernateUtil.getSessionFactory().openSession();
        this.loadGenomesFromGNomEx(getGNomExSecurity(request), true);
        this.getGNomExSecurity(request).loadAuthorizedResources(sess);
			}

		} catch (Exception e) {
			Logger.getLogger(GenometryDas2Servlet.class.getName()).log(
					Level.SEVERE, "ERROR - problems refreshing annotations {0}", e.toString());
			e.printStackTrace();
		} finally {
		  if (genometry_mode.equals(Constants.GENOMETRY_MODE_GENOPUB)) {
		    com.affymetrix.genometry.genopub.HibernateUtil.getSessionFactory().close();		    
		  } else if (genometry_mode.equals(Constants.GENOMETRY_MODE_GNOMEX)) {
		    com.affymetrix.genometry.gnomex.HibernateUtil.getSessionFactory().close();    
		  }
		}


		//send response
		response.setContentType(LOGIN_CONTENT_TYPE);
		response.setHeader("Cache-Control", "max-age=0, must-revalidate");
		// Set to expire far in the past.
		response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
		// Set standard HTTP/1.1 no-cache headers.
		response.setHeader("Cache-Control", "max-age=0, no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
		// Set standard HTTP/1.0 no-cache header.
		response.setHeader("Pragma", "no-cache");

		PrintWriter pw = response.getWriter();
		String xbase = getXmlBase(request);
		printXmlDeclaration(pw);
		pw.println("<REFRESH");
		pw.println("    xmlns=\"" + DAS2_NAMESPACE + "\"");
		pw.println("    xml:base=\"" + xbase + "\" >");
		if (maintainer_email != null && maintainer_email.length() > 0) {
			pw.println("  <MAINTAINER email=\"" + maintainer_email + "\" />");
		}
		pw.println("</REFRESH>");
		
		date_initialized = System.currentTimeMillis();
		date_init_string = date_formatter.format(new Date(date_initialized));
	}

	/**
	 *  precedence for feature out format:
	 *     specified in query string > specified in content-negotiation header > default
	 *
	 *  Would like to change this code to use ServletRequest getParameterValues(tag) instead,
	 *   but getParameterValues() is stricter about the query parameters, so that a non-url-encoded ":"
	 *   in a query parameter can lead to dropping any following parameters.  Which would be okay
	 *   if parameters were always url-encoded like they are supposed to be, but I want the genometry
	 *   server to handle cases where parameters were accidentally not url-encoded.  So this code
	 *   takes the raw query string and handles processing itself.
	// split query tagval list into format and filters
	// any tagval where tag = "format" determines format -- should only be one
	// all other tagvals should be filters
	//
	// GAH 4-18-2005 for now only trying to handle region filters and types filters
	//   currently assumes the following:
	//                    one "overlap" region filter, no ORing of multiple overlap filters
	//                    zero or one "inside" region filter, no ORing of multiple inside filters
	//                    one "type" filter (by typeid), no ORing of multiple type filters
	 *
	 *  New logic for DAS/2 feature request filters:
	 *  If there are multiple occurrences of the same filter name in the request, take the union
	 *      of the results of each of these filters individually
	 *  Then take intersection of results of each different filter name
	 *  (OR similar filters, AND different filters [ except excludes ] )
	 *
	 *  General query strategy:
	 *  [NOT SURE WHAT TO DO YET ABOUT COORDINATES FILTERS]
	 *    if any link, note, or prop filter, then return empty results
	 *    if format parameter:
	 *       if > 1 format parameter, then return "bad request" error
	 *       if > 1 type (or no type), then need to make sure that format can support multiple types
	 *         (for now, return "REQUEST TOO LARGE" error)
	 *       if 1 type, then make sure server supports returning that type in that format
	 *    if just name filter:
	 *        special case to search for names...
	 *    else:
	 *      if no type given, then return "REQUEST TOO LARGE" error
	 *      else for each type:
	 *       for each segment:
	 *          for each overlap:
	 *               collect (top-level) RESULTS syms with given type, segment, and overlap range
	 *    for each sym in RESULTS
	 *        filter by inside(s)
	 *        filter by exclude(s)
	 *        filter by name(s)
	 *
	 */
	private void handleFeaturesRequest(AnnotatedSeqGroup genome, HttpServletRequest request, HttpServletResponse response) {
		String query = request.getQueryString();
		String xbase = getXmlBase(request);
		String output_format = default_feature_format;
		String query_type = null;
		SeqSpan overlap_span = null;
		SeqSpan inside_span = null;

		List<SeqSymmetry> result = null;
		BioSeq outseq = null;
		Class<? extends AnnotationWriter> writerclass = null;

		if (query == null || query.length() == 0) {
			// no query string, so requesting _all_ features for a versioned source
			//    genometry server does not support this
			//    so leave result = null and null test below will trigger sending
			//    HTTP error message with status 413 "Request Entity Too Large"
		} else {  // request contains query string

			List<String> formats = new ArrayList<String>();
			List<String> types = new ArrayList<String>();
			List<String> segments = new ArrayList<String>();
			List<String> overlaps = new ArrayList<String>();
			List<String> insides = new ArrayList<String>();
			List<String> excludes = new ArrayList<String>();
			List<String> names = new ArrayList<String>();
			List<String> coordinates = new ArrayList<String>();
			List<String> links = new ArrayList<String>();
			List<String> notes = new ArrayList<String>();
			Map<String, ArrayList<String>> props = new HashMap<String, ArrayList<String>>();

			boolean known_query =
				splitFeaturesQuery(GeneralUtils.URLDecode(query), formats, types, segments, overlaps, insides, excludes, names, coordinates, links, notes, props);
			
			if (formats.size() == 1) {
				output_format = formats.get(0);
			}			
			writerclass = output_registry.get(output_format);			

			if (!known_query) {
				// at least one query parameter was not recognized, throw bad request error
				result = null;
			} else if (formats.size() > 1) {
				// can only be zero or one format, otherwise it's a bad request
				result = null;
			} // the Genometry DAS/2 server does not return features with LINK, NOTE, or PROP elements,
			//    so if any of these are queried for the server can return a response with zero features
			//    in the appropriate format
			else if (links.size() > 0 ||
					notes.size() > 0 ||
					props.size() > 0) {
				result = new ArrayList<SeqSymmetry>();
			} /* support for single name, single format, no other filters */
			else if (names != null && names.size() == 1) {
				BioSeq seq = null;
				if (segments.size() == 1) {
					String seqid = segments.get(0);
					System.out.println("seqid is " + seqid);
					// using end of URI for internal seqid if segment is given as full URI (as it should according to DAS/2 spec)
					int sindex = seqid.lastIndexOf('/');
					if (sindex >= 0) {
						seqid = seqid.substring(sindex + 1);
					}
					seq = genome.getSeq(seqid);
					System.out.println("Seq is " + seq == null ? null : seq.getID());
				}

				handleNameQuery(names, genome, seq, writerclass, response, xbase);
				return;
			}		

			// handling one type, one segment, one overlaps, optionally one inside
			if (types.size() == 1 && // one and only one type
					segments.size() == 1 && // one and only one segment
					overlaps.size() <= 1 && // one and only one overlaps
					insides.size() <= 1 && // zere or one inside
					excludes.isEmpty() && // zero excludes
					names.isEmpty()) {

				String seqid = segments.get(0);
				// using end of URI for internal seqid if segment is given as full URI (as it should according to DAS/2 spec)
				int sindex = seqid.lastIndexOf('/');
				if (sindex >= 0) {
					seqid = seqid.substring(sindex + 1);
				}
				String type_full_uri = types.get(0);
				query_type = getInternalType(type_full_uri, genome);				

				String overlap = null;
				if (overlaps.size() == 1) {
					overlap = overlaps.get(0);
				}
				// if overlap string is null (no overlap parameter), then no overlap filter --
				///   which is the equivalent of any annot on seq passing overlap filter --
				//    which is same as an overlap filter with range = [0, seq.length]
				//    (therefore any annotation on the seq passes overlap filter)
				//     then want all getLocationSpan will return bounds of seq as overlap
				
				overlap_span = Das2ServerUtils.getLocationSpan(seqid, overlap, genome);
				if (overlap_span != null) {
					Map<String, String> graph_name2dir = genome2graphdirs.get(genome);
					Map<String, String> graph_name2file = genome2graphfiles.get(genome);

					//useq format? this check must proceed the default graph handling below
					if (formats.contains(USeqUtilities.USEQ_EXTENSION_NO_PERIOD)){
						//does the file exist?
						if (graph_name2file.containsKey(query_type)){
							handleUSeqRequest(output_format, response, new File(graph_name2file.get(query_type)), overlap_span);				}
						else {
							result = null;
							System.out.println("  ***** Call for a useq file that doesn't exist? Aborting. *****  ");
						}						
						return;
					}
					
					if (insides.size() == 1) {
						String inside = insides.get(0);
						inside_span = Das2ServerUtils.getLocationSpan(seqid, inside, genome);						
					}
					outseq = overlap_span.getBioSeq();

					//bam files
					if(formats.contains("bam") && genome instanceof Das2AnnotatedSeqGroup){	
						handleBamRequest(query_type, (Das2AnnotatedSeqGroup)genome, outseq, overlap_span, inside_span, response);
						return;
					}
					
					//default graph formats, eg bar
					if ((graph_name2dir.get(query_type) != null) ||
							(graph_name2file.get(query_type) != null) ||
							(query_type.endsWith(".bar"))) {
						handleGraphRequest(output_registry, xbase, response, query_type, overlap_span);
						return;
					}				

					/** this is the main call to retrieve symmetries meeting query constraints */
					result = Das2ServerUtils.getIntersectedSymmetries(overlap_span, query_type, inside_span);
				}
			} else {
				// any query combination not recognized above may  be correct based on DAS/2 spec
				//    but is not currently supported, so leave result = null and and null test below will trigger sending
				//    HTTP error message with status 413 "Request Entity Too Large"
				result = null;
				System.out.println("  ***** query combination not supported, throwing an error");
			}
		}

		OutputTheDataTracks(writerclass, output_format, response, result, outseq, query_type, xbase);

	}

	/**Handles request for USeq data.*/
	private void handleUSeqRequest(String outputFormat, HttpServletResponse response, File useqArchiveFile, SeqSpan overlapSpan) {
		OutputStream outputStream = null;
		try {
			//get coordinates 
			String chromosome = overlapSpan.getBioSeq().getID();
			int start = overlapSpan.getStart();
			int end = overlapSpan.getEnd();
			
			//fetch USeqArchive from cache or make new
			USeqArchive useqArchive = file2USeqArchive.get(useqArchiveFile.toString());
			if (useqArchive == null){
				useqArchive = new USeqArchive(useqArchiveFile);
				file2USeqArchive.put(useqArchiveFile.toString(), useqArchive);
			}

			//set mime type for binary useq archives
			response.setContentType("binary/"+USeqUtilities.USEQ_EXTENSION_NO_PERIOD);

			//write to stream
			outputStream = response.getOutputStream();
			boolean wrote = useqArchive.writeSlicesToStream(outputStream, chromosome, start, end, true);
			if (wrote == false){
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				PrintWriter pw = response.getWriter();
				pw.println("DAS/2 server could not find useq data for " + chromosome+":"+start+"-"+end+" from "+useqArchiveFile.getName());
				pw.close();
			}
			else System.out.println("Wrote useq data to stream for "+ chromosome+":"+start+"-"+end+" from "+useqArchiveFile.getName());

		} catch (Exception ex) {
			ex.printStackTrace();
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			try {
				PrintWriter pw = response.getWriter();
				pw.println("The DAS/2 server encountered an error while attempting to fetch useq data from "+useqArchiveFile.getName());
				pw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//close stream
		GeneralUtils.safeClose(outputStream);
	}

	private static void splitSequenceQuery(String query, List<String> formats, List<String> ranges) {
		for (String tagval : query_splitter.split(query)) {
			String[] tagval_array = tagval_splitter.split(tagval);
			String tag = tagval_array[0];
			String val = tagval_array[1];
			if (tag.equals("format")) {
				formats.add(val);
			} else if (tag.equals("range")) {
				ranges.add(val);
			}
		}
	}


	private static boolean splitFeaturesQuery(
			String query, List<String> formats, List<String> types, List<String> segments, List<String> overlaps, List<String> insides, List<String> excludes, List<String> names, List<String> coordinates, List<String> links, List<String> notes, Map<String, ArrayList<String>> props) {
		// genometry server does not currently serve up features with PROPERTY, LINK, or NOTE element,
		//   so if any of these are encountered and the response is not an error for some other reason,
		//   the response should be a FEATURES doc with zero features.


		boolean known_query = true;
		for (String tagval : query_splitter.split(query)) {
			String[] tagval_array = tagval_splitter.split(tagval);
			String tag = tagval_array[0];
			String val = tagval_array[1];
			if (tag.equals("format")) {
				formats.add(val);
			} else if (tag.equals("type")) {
				types.add(val);
			} else if (tag.equals("segment")) {
				segments.add(val);
			} else if (tag.equals("overlaps")) {
				overlaps.add(val);
			} else if (tag.equals("inside")) {
				insides.add(val);
			} else if (tag.equals("excludes")) {
				excludes.add(val);
			} else if (tag.equals("name")) {
				names.add(val);
			} else if (tag.equals("coordinates")) {
				coordinates.add(val);
			} else if (tag.equals("link")) {
				links.add(val);
			} else if (tag.equals("note")) {
				notes.add(val);
			} else if (tag.startsWith("prop-")) {
				// extract prop's key from tag ('prop-key')
				// if already seen this key, get list from hash and add to it
				// if not already seen, create new list and add to hash with key prop-key
				String pkey = tag.substring(5); // strip off "prop-" to get key
				ArrayList<String> vlist = props.get(pkey);
				if (vlist == null) {
					vlist = new ArrayList<String>();
					props.put(pkey, vlist);
				}
				vlist.add(val);
			} else {
				known_query = false; // tag not recognized, so reject whole query
			}
		}
		return known_query;
	}


	private static void handleNameQuery(
			List<String> names, AnnotatedSeqGroup genome, BioSeq seq, Class<? extends AnnotationWriter> writerclass,
			HttpServletResponse response, String xbase) {
		String name = names.get(0);
		Set<SeqSymmetry> result = IndexingUtils.findNameInGenome(name, genome);
		OutputStream outstream = null;
		try {
			AnnotationWriter writer = (AnnotationWriter) writerclass.newInstance();
			String mime_type = writer.getMimeType();
			if (writer instanceof Das2FeatureSaxParser) {
				((Das2FeatureSaxParser) writer).setBaseURI(new URI(xbase));
			}
			response.setContentType(mime_type);
			outstream = response.getOutputStream();
			if (seq != null) {
				// a chromosome was specified
				writer.writeAnnotations(result, seq, null, outstream);
			} else {
				// Writing all of the chromosomes
				for (BioSeq tempSeq : genome.getSeqList()) {
					writer.writeAnnotations(result, tempSeq, null, outstream);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(outstream);
		}
	}


	/**
	 * 1) looks for graph files in graph seq grouping directories (".graphs.seqs")
	 * or, 2) looks for graph files as bar files sans seq grouping directories, but within data directory hierarchy
	 * or, 3) tries to directly access file
	 */
	private void handleGraphRequest(
			Map<String, Class<? extends AnnotationWriter>> output_registry, String xbase, HttpServletResponse response,
			String type, SeqSpan span) {
		BioSeq seq = span.getBioSeq();
		String seqid = seq.getID();
		AnnotatedSeqGroup genome = seq.getSeqGroup();
		// use bar parser to extract just the overlap slice from the graph
		String graph_name = type;   // for now using graph_name as graph type

		Map<String, String> graph_name2dir = genome2graphdirs.get(genome);
		Map<String, String> graph_name2file = genome2graphfiles.get(genome);

		String file_path = DetermineFilePath(graph_name2dir, graph_name2file, graph_name, seqid);
		OutputGraphSlice(output_registry.get("bar"), file_path, span, type, xbase, response);
	}

	/**
	 * Determine the file path of the graph, based upon name and seqid.
	 * General assumption: there is a directory called NAME.graphs.seqs/, containing chr1.bar, chr2.bar, etc.
	 * @param graph_name2dir
	 * @param graph_name2file
	 * @param graph_name
	 * @param seqid
	 * @return file path of the graph
	 */
	private static String DetermineFilePath(
			Map<String, String> graph_name2dir, Map<String, String> graph_name2file, String graph_name, String seqid) {
		// for now using graph_name as graph type
		String file_path = graph_name2dir.get(graph_name);
		if (file_path != null) {
			file_path += "/" + seqid + ".bar";
		} else {
			file_path = graph_name2file.get(graph_name);
			if (file_path == null) {
				file_path = graph_name;
			}
		}

		if (file_path.startsWith("file:")) {
			// if file_path is URI string, strip off "file:" prefix
			file_path = file_path.substring(5);
		}
		return file_path;
	}

	private static void OutputGraphSlice(
			Class<? extends AnnotationWriter> writerclass, String file_path, SeqSpan span, String type, String xbase, HttpServletResponse response) {
		GraphSym graf = null;
		try {
			graf = BarParser.getRegion(file_path, span);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (graf != null) {
			List<SeqSymmetry> gsyms = new ArrayList<SeqSymmetry>();
			gsyms.add(graf);
			System.out.println("#### returning graph slice in bar format");
			outputDataTracks(gsyms, span.getBioSeq(), type, xbase, response, writerclass, "bar");
		} else {
			// couldn't generate a GraphSym, so return an error?
			System.out.println("####### problem with retrieving graph slice ########");
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			try {
				PrintWriter pw = response.getWriter();
				pw.println("DAS/2 server could not find graph to return for type: " + type);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			System.out.println("set status to 404 not found");
		}
	}

	private static void OutputTheDataTracks(
			Class<? extends AnnotationWriter> writerclass,
			String output_format,
			HttpServletResponse response,
			List<SeqSymmetry> result,
			BioSeq outseq,
			String query_type,
			String xbase) {
		try {
			if (DEBUG) {
				System.out.println("overlapping annotations found: " + (result==null ? null : result.size()));
			}
			if (result == null) {
				response.sendError(
						HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
						"Query could not be handled. " + LIMITED_FEATURE_QUERIES_EXPLANATION);
			} else {
				outputDataTracks(result, outseq, query_type, xbase, response, writerclass, output_format);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static String getInternalType(String full_type_uri, AnnotatedSeqGroup genome) {
		String query_type = GeneralUtils.URLDecode(full_type_uri);
		// using end of URI for internal typeid if type is given as full URI
		//    (as it should according to DAS/2 spec)
		//    special-case exception is when need to know full URL for locating graph data,
		if (!(query_type.endsWith(".bar"))) {
			String gid = genome.getID();
			int gindex = query_type.indexOf(gid);
			if (gindex >= 0) {
				query_type = query_type.substring(gindex + gid.length() + 1);
			}
		}
		return query_type;
	}

	private static boolean outputDataTracks(List<SeqSymmetry> syms, BioSeq seq,
			String annot_type,
			String xbase, HttpServletResponse response,
			Class<? extends AnnotationWriter> writerclass,
			String format) {
		try {
			if (writerclass == null) {
				System.out.println("no AnnotationWriter found for format: " + format);
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return false;
			}
			AnnotationWriter writer = writerclass.newInstance();
			String mime_type = writer.getMimeType();

			if (writer instanceof Das2FeatureSaxParser) {
				((Das2FeatureSaxParser) writer).setBaseURI(new URI(xbase));
			}
			response.setContentType(mime_type);

			OutputStream outstream = response.getOutputStream();
			try {
				return writer.writeAnnotations(syms, seq, annot_type, outstream);
			} finally {
				GeneralUtils.safeClose(outstream);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	private static void printXmlDeclaration(PrintWriter pw) {
		// medium declaration (version, encoding)
		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		// long declaration (version, encoding, standalone)
		//  pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
		//    pw.println("<!DOCTYPE DAS2XML SYSTEM \"http://www.biodas.org/dtd/das2xml.dtd\">");
	}

	static void setXmlBase(String xbase) {
		xml_base = xbase;
		String trimmed_xml_base;
		if (xml_base.endsWith("/")) {
			trimmed_xml_base = xml_base.substring(0, xml_base.length() - 1);
		} else {
			trimmed_xml_base = xml_base;
			xml_base += "/";
		}
		sources_query_no_slash = trimmed_xml_base.substring(trimmed_xml_base.lastIndexOf('/'));
		sources_query_with_slash = sources_query_no_slash + "/";
	}

	/** getXmlBase() should no longer depend on request, should always be set via setXmlBase()
	when servlet starts up -- need to remove request arg soon
	 */
	private static String getXmlBase(HttpServletRequest request) {
		if (xml_base != null) {
			return xml_base;
		} else {
			return request.getRequestURL().toString();
		}
	}

	private void handleBamRequest(String query_type, Das2AnnotatedSeqGroup genome, BioSeq seq, SeqSpan overlap_span, SeqSpan inside_span, HttpServletResponse response) {
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try{
			BAM bamfile = (BAM) genome.getSymLoader(query_type);
			response.setContentType(bamfile.getMimeType());
			bos = new BufferedOutputStream(response.getOutputStream());
			dos = new DataOutputStream(bos);			
			bamfile.writeAnnotations(seq, overlap_span.getMin(), overlap_span.getMax(), dos, true);
			//BedParser bed = new BedParser();
			//response.setContentType(bamfile.getMimeType());
			//bed.writeAnnotations(bamfile.getRegion(overlap_span), seq, "test", dos);
		}catch(Exception ex){
			Logger.getLogger(GenometryDas2Servlet.class.getName()).log(Level.SEVERE, "Unable to load bam file", ex);
		}finally{
			GeneralUtils.safeClose(bos);
			GeneralUtils.safeClose(dos);
		}

	}
}
