package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.parsers.BAMParser;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.parsers.Parser;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

		FileTypeHandler fileTypeHandler = FileTypeHolder.getInstance().getFileTypeHandler(extension);
		if (fileTypeHandler == null) {
			Logger.getLogger(SymLoader.class.getName()).log(
				Level.WARNING, "ABORTING FEATURE LOADING, FORMAT NOT RECOGNIZED: {0}", extension);
			return null;
		}
		else {
			Parser parser = fileTypeHandler.getParser();
			if (parser instanceof BAMParser) {
				return ((BAMParser)parser).parse(uri, istr, group, featureName, overlap_span);
			}
			else {
				return parser.parse(bis, group, featureName, uri.toString(), false);
			}
		}
	}
}
