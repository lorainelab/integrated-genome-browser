package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.BgnParser;
import com.affymetrix.genometryImpl.parsers.Bprobe1Parser;
import com.affymetrix.genometryImpl.parsers.BpsParser;
import com.affymetrix.genometryImpl.parsers.BrptParser;
import com.affymetrix.genometryImpl.parsers.BrsParser;
import com.affymetrix.genometryImpl.parsers.BsnpParser;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.parsers.Das2FeatureSaxParser;
import com.affymetrix.genometryImpl.parsers.ExonArrayDesignParser;
import com.affymetrix.genometryImpl.parsers.FishClonesParser;
import com.affymetrix.genometryImpl.parsers.GFF3Parser;
import com.affymetrix.genometryImpl.parsers.GFFParser;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.parsers.VarParser;
import com.affymetrix.genometryImpl.parsers.das.DASFeatureParser;
import com.affymetrix.genometryImpl.parsers.gchp.AffyCnChpParser;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.parsers.graph.CntParser;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.parsers.useq.ArchiveInfo;
import com.affymetrix.genometryImpl.parsers.useq.USeqGraphParser;
import com.affymetrix.genometryImpl.parsers.useq.USeqRegionParser;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.ParserController;
import java.io.InputStream;
import com.affymetrix.genometryImpl.util.SortTabFile;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import org.xml.sax.InputSource;

/**
 *
 * @author jnicol
 * Could be improved with iterators.  But for now this should be fine.
 */
public abstract class SymLoader {
	public final URI uri;
	public final String extension;	// used for ServerUtils call
	public boolean isResidueLoader = false;	// Let other classes know if this is just residues
	protected volatile boolean isInitialized = false;
	protected final Map<BioSeq,File> chrList = new HashMap<BioSeq,File>();
	private final Map<String,Boolean> chrSort = new HashMap<String,Boolean>();
	protected final AnnotatedSeqGroup group;
	public final String featureName;

	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}

	public SymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
        this.uri = uri;
		this.featureName = featureName;
		this.group = group;
		
		String uriString = uri.toASCIIString().toLowerCase();
		String unzippedStreamName = GeneralUtils.stripEndings(uriString);
		extension = ParserController.getExtension(unzippedStreamName);

    }

	protected void init() {
		this.isInitialized = true;
	}

	protected void buildIndex(){
		BufferedInputStream bis = null;
		Map<String, Integer> chrLength = new HashMap<String, Integer>();
		Map<String, File> chrFiles = new HashMap<String, File>();

		try {
			bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
			parseLines(bis, chrLength, chrFiles);
			createResults(chrLength, chrFiles);
		} catch (Exception ex) {
			Logger.getLogger(SymLoader.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(bis);
		}

	}

	protected void sortCreatedFiles(){
		//Now Sort all files
		for (Entry<BioSeq, File> file : chrList.entrySet()) {
			chrSort.put(file.getKey().getID(), SortTabFile.sort(file.getValue()));
		}
	}
	/**
	 * Return possible strategies to load this URI.
	 * @return
	 */
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}
	/**
	 * Get list of chromosomes used in the file/uri.
	 * Especially useful when loading a file into an "unknown" genome
	 * @return List of chromosomes
	 */
	public List<BioSeq> getChromosomeList() {
		return Collections.<BioSeq>emptyList();
	}
	
    /**
     * @return List of symmetries in genome
     */
    public List<? extends SeqSymmetry> getGenome() {

		if (GraphSymUtils.isAGraphFilename(this.extension)) {
			BufferedInputStream bis = null;
			try {
				GenometryModel gmodel = GenometryModel.getGenometryModel();
				bis = LocalUrlCacher.convertURIToBufferedStream(this.uri);
				List<GraphSym> graphs = GraphSymUtils.readGraphs(bis, this.uri.toString(), gmodel, gmodel.getSelectedSeqGroup(), null);
				GraphSymUtils.setName(graphs, GraphSymUtils.getGraphNameForURL(this.uri.toURL()));
				return graphs;
			} catch (Exception ex) {
				Logger.getLogger(SymLoader.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(bis);
			}
		}
		
		List<? extends SeqSymmetry> feats = null;
		try {
			BufferedInputStream bis = null;
			try {
				// This will also unzip the stream if necessary
				bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(this.uri);
				feats = Parse(this.extension, this.uri, bis, group, this.featureName, null);
				return feats;
			} catch (FileNotFoundException ex) {
				Logger.getLogger(SymLoader.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(bis);
			}
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Logger.getLogger(this.getClass().getName()).log(
					Level.SEVERE, "Retrieving genome is not defined");
        return null;
    }

    /**
     * @param seq - chromosome
     * @return List of symmetries in chromosome
     */
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) {
		Logger.getLogger(this.getClass().getName()).log(
					Level.FINE, "Retrieving chromosome is not optimized");
		List<? extends SeqSymmetry> genomeResults = this.getGenome();
		if (seq == null || genomeResults == null) {
			return genomeResults;
		}
		return filterResultsByChromosome(genomeResults, seq);
    }

	public List<String> getFormatPrefList(){
		return Collections.<String>emptyList();
	}
	
	/**
	 * Return the symmetries that match the given chromosome.
	 * @param genomeResults
	 * @param seq
	 * @return
	 */
	public static List<SeqSymmetry> filterResultsByChromosome(List<? extends SeqSymmetry> genomeResults, BioSeq seq) {
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		for (SeqSymmetry sym : genomeResults) {
			BioSeq seq2 = null;
			if (sym instanceof UcscPslSym) {
				seq2 = ((UcscPslSym) sym).getTargetSeq();
			} else {
				seq2 = sym.getSpanSeq(0);
			}
			if (seq.equals(seq2)) {
				results.add(sym);
			}
		}
		return results;
	}

    /**
     * Get a region of the chromosome.
     * @param seq - chromosome
     * @param overlapSpan - span of overlap
     * @return List of symmetries satisfying requirements
     */
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) {
		Logger.getLogger(this.getClass().getName()).log(
					Level.WARNING, "Retrieving region is not supported.  Returning entire chromosome.");
		List<? extends SeqSymmetry> chrResults = this.getChromosome(overlapSpan.getBioSeq());
		return chrResults;
    }

	/**
     * Get residues in the region of the chromosome.  This is generally only defined for some parsers
     * @param span - span of chromosome
     * @return String of residues
     */
    public String getRegionResidues(SeqSpan span) {
		Logger.getLogger(this.getClass().getName()).log(
					Level.WARNING, "Not supported.  Returning empty string.");
		return "";
    }

	protected void parseLines(InputStream istr, Map<String, Integer> chrLength, Map<String, File> chrFiles){
		Logger.getLogger(this.getClass().getName()).log(
					Level.SEVERE, "parseLines is not defined");
	}
	
	protected static void addToLists(
			Map<String, BufferedWriter> chrs, String current_seq_id, Map<String, File> chrFiles, Map<String,Integer> chrLength, String format) throws IOException {

		String fileName = current_seq_id;
		if (fileName.length() < 3) {
			fileName += "___";
		}
		format = !format.startsWith(".") ? "." + format : format;
		File tempFile = File.createTempFile(fileName, format);
		tempFile.deleteOnExit();
		chrs.put(current_seq_id, new BufferedWriter(new FileWriter(tempFile, true)));
		chrFiles.put(current_seq_id, tempFile);
		chrLength.put(current_seq_id, 0);
	}


	protected void createResults(Map<String, Integer> chrLength, Map<String, File> chrFiles){
		for(Entry<String, Integer> bioseq : chrLength.entrySet()){
			String key = bioseq.getKey();
			chrList.put(group.addSeq(key, bioseq.getValue()), chrFiles.get(key));
		}
	}

	/**
   * Split list of symmetries by track.
   * @param results - list of symmetries
   * @return - Map<String trackName,List<SeqSymmetry>>
   */
  public static Map<String, List<SeqSymmetry>> splitResultsByTracks(List<? extends SeqSymmetry> results) {
		Map<String, List<SeqSymmetry>> track2Results = new HashMap<String, List<SeqSymmetry>>();
		List<SeqSymmetry> resultList = null;
		String method = null;
		for (SeqSymmetry result : results) {
			method = (result instanceof SymWithProps) ? (String) ((SymWithProps) result).getProperty("method") : null;
			if (track2Results.containsKey(method)) {
				resultList = track2Results.get(method);
			} else {
				resultList = new ArrayList<SeqSymmetry>();
				track2Results.put(method, resultList);
			}
			resultList.add(result);
		}

	  return track2Results;
  }

	public static void addAnnotations(
			List<? extends SeqSymmetry> feats, SeqSpan span, URI uri) {
		if (feats == null || feats.isEmpty()) {
			return;
		}
		if (feats.get(0) instanceof GraphSym) {
			// We assume that if there are any GraphSyms, then we're dealing with a list of GraphSyms.
			List<GraphSym> grafs = new ArrayList<GraphSym>(feats.size());
			for(SeqSymmetry feat : feats) {
				grafs.add((GraphSym)feat);
			}
			GraphSymUtils.processGraphSyms(grafs, uri.toString());
			return;
		}

		BioSeq seq = span.getBioSeq();
		for (SeqSymmetry feat : feats) {
			seq.addAnnotation(feat);
		}
	}

	/**
	 * parse the input stream, with parser determined by extension.
	 * @param extension
	 * @param uri - the URI corresponding to the file/URL
	 * @param istr
	 * @param group
	 * @param featureName
	 * @return list of symmetries
	 * @throws Exception
	 */
	public static List<? extends SeqSymmetry> Parse(
			String extension, URI uri, InputStream istr, AnnotatedSeqGroup group, String featureName, SeqSpan overlap_span)
			throws Exception {
		BufferedInputStream bis = new BufferedInputStream(istr);
		extension = extension.substring(extension.indexOf('.') + 1);	// strip off first .

		// These extensions are overloaded by QuickLoad
		if (extension.equals("bar")) {
			return BarParser.parse(bis, GenometryModel.getGenometryModel(), group, null, 0, Integer.MAX_VALUE, featureName, false);
		}
		if (extension.equals("bed")) {
			BedParser parser = new BedParser();
			return parser.parse(bis, GenometryModel.getGenometryModel(), group, false, uri.toString(), false);
		}
		if (extension.equals("useq")) {
			//find out what kind of data it is, graph or region, from the ArchiveInfo object
			ZipInputStream zis = new ZipInputStream(bis);
			zis.getNextEntry();
			ArchiveInfo archiveInfo = new ArchiveInfo(zis, false);
			if (archiveInfo.getDataType().equals(ArchiveInfo.DATA_TYPE_VALUE_GRAPH)) {
				USeqGraphParser gp = new USeqGraphParser();
				return gp.parseGraphSyms(zis, GenometryModel.getGenometryModel(), featureName, archiveInfo);
			}
			USeqRegionParser rp = new USeqRegionParser();
			return rp.parse(zis, group, featureName, false, archiveInfo);
		}

		// Not overloaded extensions
		if (extension.equals("bgn")) {
			BgnParser parser = new BgnParser();
			return parser.parse(bis, featureName, group, false);
		}
		if (extension.equals("bps")) {
			DataInputStream dis = new DataInputStream(bis);
			return BpsParser.parse(dis, featureName, null, group, false, false);
		}
		if (extension.equals("bp1") || extension.equals("bp2")) {
			Bprobe1Parser bp1_reader = new Bprobe1Parser();
			// parsing probesets in bp2 format, also adding probeset ids
			return bp1_reader.parse(bis, group, false, featureName, false);
		}
		if (extension.equals("brpt")) {
			List<SeqSymmetry> alist = BrptParser.parse(bis, featureName, group, false);
			Logger.getLogger(SymLoader.class.getName()).log(
					Level.FINE, "total repeats loaded: {0}", alist.size());
			return alist;
		}
		if (extension.equals("brs")) {
			DataInputStream dis = new DataInputStream(bis);
			return BrsParser.parse(dis, featureName, group, false);
		}
		if (extension.equals("bsnp")) {
			List<SeqSymmetry> alist = BsnpParser.parse(bis, featureName, group, false);
			Logger.getLogger(SymLoader.class.getName()).log(
					Level.FINE, "total snps loaded: {0}", alist.size());
			return alist;
		}
		if (extension.equals("cnchp") || extension.equals("lohchp")) {
			AffyCnChpParser parser = new AffyCnChpParser();
			return parser.parse(null, bis, featureName, group, false);
		}
		if (extension.equals("cnt")) {
			CntParser parser = new CntParser();
			return parser.parse(bis, group, false);
		}
		if (extension.equals("cyt")) {
			CytobandParser parser = new CytobandParser();
			return parser.parse(bis, group, false);
		}
		if (extension.equals("das") || extension.equals("dasxml")) {
			DASFeatureParser parser = new DASFeatureParser();
			parser.setAnnotateSeq(false);
			return (List<? extends SeqSymmetry>) parser.parse(bis, group);
		}
		if (extension.equals(Das2FeatureSaxParser.FEATURES_CONTENT_SUBTYPE)
				|| extension.equals("das2feature")
				|| extension.equals("das2xml")
				|| extension.startsWith("x-das-feature")) {
			Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
			return parser.parse(new InputSource(bis), uri.toString(), group, false);
		}
		if (extension.equals("ead")) {
			ExonArrayDesignParser parser = new ExonArrayDesignParser();
			return parser.parse(bis, group, false, featureName);
		}
		if (extension.equals(FishClonesParser.FILE_EXT)) {
			FishClonesParser parser = new FishClonesParser(false);
			return parser.parse(bis, featureName, group);
		}
		if (extension.equals("gff") || extension.equals("gtf")) {
			GFFParser parser = new GFFParser();
			parser.parse(bis, featureName, group, false, true);
			return null;	// hack -- cannot currently annotate with FRS!
		}
		if (extension.equals("gff3")) {
			/* Force parsing as GFF3 */
			GFF3Parser parser = new GFF3Parser();
			parser.parse(bis, featureName, group, true);
			return null;	// cannot currently annotate with FRS!
		}
		if (extension.equals("link.psl")) {
			PSLParser parser = new PSLParser();
			parser.setIsLinkPsl(true);
			parser.enableSharedQueryTarget(true);
			// annotate _target_ (which is chromosome for consensus annots, and consensus seq for probeset annots
			// why is annotate_target parameter below set to false?
			return parser.parse(bis, featureName, null, group, null, false, false, false); // do not annotate.  This is done later
		}
		if (extension.equals("psl") || extension.equals("psl3")) {
			// reference to LoadFileAction.ParsePSL
			PSLParser parser = new PSLParser();
			parser.enableSharedQueryTarget(true);
			DataInputStream dis = new DataInputStream(bis);
			return parser.parse(dis, featureName, null, group, null, false, false, false); // do not annotate.  This is done later
		}
		if (extension.equals("sin") || extension.equals("egr") || extension.equals("txt")) {
			ScoredIntervalParser parser = new ScoredIntervalParser();
			return parser.parse(bis, featureName, group, false);
		}
		if (extension.equals("var")) {
			return VarParser.parse(bis, group);
		}

		if (extension.equalsIgnoreCase("bam")) {
			File bamfile = GeneralUtils.convertStreamToFile(istr, featureName);
			bamfile.deleteOnExit();
			BAM bam = new BAM(bamfile.toURI(),featureName,group);
			return bam.getRegion(overlap_span);
		}

		Logger.getLogger(SymLoader.class.getName()).log(
				Level.WARNING, "ABORTING FEATURE LOADING, FORMAT NOT RECOGNIZED: {0}", extension);
		return null;
	}

}
