package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
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
import com.affymetrix.genometryImpl.parsers.graph.BgrParser;
import com.affymetrix.genometryImpl.parsers.graph.CntParser;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.parsers.useq.ArchiveInfo;
import com.affymetrix.genometryImpl.parsers.useq.USeqGraphParser;
import com.affymetrix.genometryImpl.parsers.useq.USeqRegionParser;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import org.xml.sax.InputSource;

/**
 *
 * Singleton to perform all SymLoader static methods
 */
public class SymProcessor {
	private static final SymProcessor instance = new SymProcessor();

	public static SymProcessor getInstance() {
		return instance;
	}

	private SymProcessor() {
		super();
    }

	/**
	 * Return the symmetries that match the given chromosome.
	 * @param genomeResults
	 * @param seq
	 * @return
	 */
	public List<SeqSymmetry> filterResultsByChromosome(List<? extends SeqSymmetry> genomeResults, BioSeq seq) {
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
   * Split list of symmetries by track.
   * @param results - list of symmetries
   * @return - Map<String trackName,List<SeqSymmetry>>
   */
  public Map<String, List<SeqSymmetry>> splitResultsByTracks(List<? extends SeqSymmetry> results) {
		Map<String, List<SeqSymmetry>> track2Results = new HashMap<String, List<SeqSymmetry>>();
		List<SeqSymmetry> resultList = null;
		String method = null;
		for (SeqSymmetry result : results) {
			method = BioSeq.determineMethod(result);
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

  public Map<BioSeq, List<SeqSymmetry>> splitResultsBySeqs(List<? extends SeqSymmetry> results){
	  Map<BioSeq, List<SeqSymmetry>> seq2Results = new HashMap<BioSeq, List<SeqSymmetry>>();
	  List<SeqSymmetry> resultList = null;
	  BioSeq seq = null;
		for (SeqSymmetry result : results) {

			for(int i=0; i<result.getSpanCount(); i++){
				seq = result.getSpan(i).getBioSeq();

				if (seq2Results.containsKey(seq)) {
					resultList = seq2Results.get(seq);
				} else {
					resultList = new ArrayList<SeqSymmetry>();
					seq2Results.put(seq, resultList);
				}
				resultList.add(result);
			}

		}

	  return seq2Results;
  }

	public void filterAndAddAnnotations(
			List<? extends SeqSymmetry> feats, SeqSpan span, URI uri, GenericFeature feature) {
		if (feats == null || feats.isEmpty()) {
			return;
		}
		SeqSymmetry originalRequestSym = feature.getRequestSym();
		List<? extends SeqSymmetry> filteredFeats = filterOutExistingSymmetries(originalRequestSym, feats, span.getBioSeq());	
		if (filteredFeats.isEmpty()) {
			return;
		}
		if (filteredFeats.get(0) instanceof GraphSym) {
			// We assume that if there are any GraphSyms, then we're dealing with a list of GraphSyms.
			for(SeqSymmetry feat : filteredFeats) {
				//grafs.add((GraphSym)feat);
				if (feat instanceof GraphSym) {
					GraphSymUtils.addChildGraph((GraphSym) feat, ((GraphSym) feat).getID(), ((GraphSym) feat).getGraphName(), uri.toString(), span);
				}
			}

			return;
		}

		BioSeq seq = span.getBioSeq();
		for (SeqSymmetry feat : filteredFeats) {
			seq.addAnnotation(feat);
		}
	}


	private List<? extends SeqSymmetry> filterOutExistingSymmetries(SeqSymmetry original_sym, List<? extends SeqSymmetry> syms, BioSeq seq) {
		List<SeqSymmetry> newSyms = new ArrayList<SeqSymmetry>(syms.size());	// roughly this size
		MutableSeqSymmetry dummySym = new SimpleMutableSeqSymmetry();
		for (SeqSymmetry sym : syms) {

			/**
			 * Since GraphSym is only SeqSymmetry containing all points.
			 * The intersection may find some points intersecting and
			 * thus not add whole GraphSym at all. So if GraphSym is encountered
			 * the it's not checked if it is intersecting. 
			 */
			if (sym instanceof GraphSym) {
				// if graphs, then adding to annotation BioSeq is handled by addChildGraph() method
				return syms;
			}

			dummySym.clear();
			if (SeqUtils.intersection(sym, original_sym, dummySym, seq)) {
				// There is an intersection with previous requests.  Ignore this symmetry
				continue;
			}
			newSyms.add(sym);
		}
		return newSyms;
	}

	public List<BioSeq> getChromosomes(URI uri, String extension, String featureName){
		AnnotatedSeqGroup temp_group = new AnnotatedSeqGroup("temp_group");
		SymLoader temp = new SymLoader(uri, featureName, temp_group) {};
		List<? extends SeqSymmetry> syms = temp.getGenome();
		List<BioSeq> seqs = new ArrayList<BioSeq>();
		seqs.addAll(temp_group.getSeqList());
		
		// Force GC
		syms.clear();
		syms = null;
		temp = null;
		temp_group = null;

		return seqs;
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
	public List<? extends SeqSymmetry> parse(
			String extension, URI uri, InputStream istr, AnnotatedSeqGroup group, String featureName, SeqSpan overlap_span)
			throws Exception {
		BufferedInputStream bis = new BufferedInputStream(istr);
		extension = extension.substring(extension.indexOf('.') + 1);	// strip off first .

		// These extensions are overloaded by QuickLoad
		if (extension.equals("bar")) {
			return BarParser.parse(bis, GenometryModel.getGenometryModel(), group, null, 0, Integer.MAX_VALUE, uri.toString(), false);
		}
		if (extension.equals("bed")) {
			BedParser parser = new BedParser();
			return parser.parse(bis, GenometryModel.getGenometryModel(), group, false, featureName, false);
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
			return bp1_reader.parse(bis, group, false, featureName, true);
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
			return parser.parse(bis, featureName, group, false, false);
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
		if (extension.equals("psl") || extension.equals("psl3") || extension.equals("pslx")) {
			// reference to LoadFileAction.ParsePSL
			PSLParser parser = new PSLParser();
			parser.enableSharedQueryTarget(true);
			DataInputStream dis = new DataInputStream(bis);
			return parser.parse(dis, featureName, null, group, null, false, false, false); // do not annotate.  This is done later
		}
		if (extension.equals("sin") || extension.equals("egr") || extension.equals("txt")) {
			ScoredIntervalParser parser = new ScoredIntervalParser();
			return parser.parse(bis, uri.toString(), group, false);
		}
		if (extension.equals("var")) {
			return VarParser.parse(bis, group);
		}
		if (extension.equals("bgr")) {
			return BgrParser.parse(bis, uri.toString(), group);
		}
		if (extension.equalsIgnoreCase("bam")) {
			File bamfile = GeneralUtils.convertStreamToFile(istr, featureName);
			bamfile.deleteOnExit();
			BAM bam = new BAM(bamfile.toURI(),featureName,group);
			//for DAS/2 responses, the bam data is already trimmed so should just load it and not build an index, note bam files loaded from a url are not parsed here but elsewhere so the only http inputs are from DAS
			if (uri.getScheme().equals("http")) return bam.parseAll(overlap_span.getBioSeq());
			return bam.getRegion(overlap_span);
		}

		Logger.getLogger(SymLoader.class.getName()).log(
				Level.WARNING, "ABORTING FEATURE LOADING, FORMAT NOT RECOGNIZED: {0}", extension);
		return null;
	}
}
