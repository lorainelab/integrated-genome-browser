package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.symloader.*;
import com.affymetrix.genometryImpl.symmetry.SearchableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.IndexingUtils.IndexedSyms;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utils for DAS/2 and other servers.
 */
public abstract class DasServerUtils {
	static final Pattern interval_splitter = Pattern.compile(":");
	
	/**
	 *  Differs from Das2FeatureSaxParser.getLocationSpan():
	 *     Won't add unrecognized seqids or null groups
	 *     If rng is null or "", will set to span to [0, originalSeq.getLength()]
	 */
	public static SeqSpan getLocationSpan(String seqid, String rng, AnnotatedSeqGroup group) {
		if (seqid == null || group == null) {
			return null;
		}
		BioSeq seq = group.getSeq(seqid);
		if (seq == null) {
			return null;
		}
		int min;
		int max;
		boolean forward = true;
		if (rng == null) {
			min = 0;
			max = seq.getLength();
		} else {
			try {
				String[] subfields = interval_splitter.split(rng);
				min = Integer.parseInt(subfields[0]);
				max = Integer.parseInt(subfields[1]);
				if (subfields.length >= 3) {  // in DAS/2 strandedness is not allowed for range query params, but accepting it here
					if (subfields[2].equals("-1")) {
						forward = false;
					}
				}
			} catch (Exception ex) {
				Logger.getLogger(DasServerUtils.class.getName()).log(Level.INFO,
						"Problem parsing a query parameter range filter: {0}", rng);
				return null;
			}
		}
		SeqSpan span;
		if (forward) {
			span = new SimpleSeqSpan(min, max, seq);
		} else {
			span = new SimpleSeqSpan(max, min, seq);
		}
		return span;
	}

	/**
	 * Retrieve symmetries meeting query constraints.
	 * @param overlap_span
	 * @param query_type - annotation "type", which is feature name.
	 * @param inside_span
	 */
	public static List<SeqSymmetry> getIntersectedSymmetries(SeqSpan overlap_span, String query_type, SeqSpan inside_span) {
		List<SeqSymmetry> result =
				DasServerUtils.getOverlappedSymmetries(overlap_span, query_type);
		if (result == null) {
			result = Collections.<SeqSymmetry>emptyList();
		}
		if (inside_span != null) {
			result = DasServerUtils.specifiedInsideSpan(inside_span, result);
		}
		return result;
	}

	/**
	 *
	 *  Currently assumes:
	 *    query_span's originalSeq is a BioSeq (which implies top-level annots are TypeContainerAnnots)
	 *    only one IntervalSearchSym child for each TypeContainerAnnot
	 *  Should expand soon so loadedSyms can be returned from multiple IntervalSearchSyms children
	 *      of the TypeContainerAnnot
	 */
	public static List<SeqSymmetry> getOverlappedSymmetries(SeqSpan query_span, String annot_type) {
		BioSeq seq = query_span.getBioSeq();
		SymWithProps container = seq.getAnnotation(annot_type);
		if (container != null) {
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.FINE,
					"non-indexed request for {0}", annot_type);
			int annot_count = container.getChildCount();
			for (int i = 0; i < annot_count; i++) {
				SeqSymmetry sym = container.getChild(i);
				if (sym instanceof SearchableSeqSymmetry) {
					SearchableSeqSymmetry target_sym = (SearchableSeqSymmetry) sym;
					return target_sym.getOverlappingChildren(query_span);
				}
			}
		} else {
			// Couldn't find it.  See if it's been indexed.
			Logger.getLogger(DasServerUtils.class.getName()).log(Level.FINE,
					"indexed request for {0}", annot_type);
			IndexedSyms iSyms = seq.getIndexedSym(annot_type);
			if (iSyms != null) {
				return getIndexedOverlappedSymmetries(
						query_span,
						iSyms,
						annot_type,
						seq.getSeqGroup());
			}
		}
		return Collections.<SeqSymmetry>emptyList();
	}

	// if an inside_span specified, then filter out intersected symmetries based on this:
	//    don't return symmetries with a min < inside_span.min() or max > inside_span.max()  (even if they overlap query interval)
	public static <SymExtended extends SeqSymmetry> List<SymExtended> specifiedInsideSpan(
			SeqSpan inside_span, List<SymExtended> result) {
		int inside_min = inside_span.getMin();
		int inside_max = inside_span.getMax();
		BioSeq iseq = inside_span.getBioSeq();
		MutableSeqSpan testspan = new SimpleMutableSeqSpan();
		List<SymExtended> orig_result = result;
		result = new ArrayList<SymExtended>(orig_result.size());
		for (SymExtended sym : orig_result) {
			// fill in testspan with span values for sym (on aseq)
			sym.getSpan(iseq, testspan);
			if ((testspan.getMin() >= inside_min) && (testspan.getMax() <= inside_max)) {
				result.add(sym);
			}
		}
		Logger.getLogger(DasServerUtils.class.getName()).log(Level.FINE,
				"  overlapping annotations that passed inside_span constraints: {0}", result.size());
		return result;
	}

	
	/**
	 * Get the list of symmetries.
	 * @param overlap_span
	 * @param iSyms
	 * @param annot_type
	 * @param group
	 * @return list of indexed overlapped symmetries
	 */
	public static List<SeqSymmetry> getIndexedOverlappedSymmetries(
			SeqSpan overlap_span,
			IndexedSyms iSyms,
			String annot_type,
			AnnotatedSeqGroup group) {

		List<? extends SeqSymmetry> symList = getIndexedSymmetries(overlap_span,iSyms,annot_type,group);

		// We need to filter this list to only return overlaps.
		// Due to the way indexing is implemented, there may have been additional symmetries outside of the specified interval.
		// This violates the DAS/2 specification, but more importantly, IGB gets confused.
		return SeqUtils.filterForOverlappingSymmetries(overlap_span,symList);
	}

	/**
	 * Get the list of symmetries
	 * @param overlap_span
	 * @param iSyms
	 * @param annot_type
	 * @param group
	 * @return list of indexed seq symmetries
	 */
	private static List<? extends SeqSymmetry> getIndexedSymmetries(
			SeqSpan overlap_span,
			IndexedSyms iSyms,
			String annot_type,
			AnnotatedSeqGroup group) {

		InputStream newIstr = null;
		DataInputStream dis = null;
		try {
			int[] overlapRange = new int[2];
			int[] outputRange = new int[2];
			overlapRange[0] = overlap_span.getMin();
			overlapRange[1] = overlap_span.getMax();
			IndexingUtils.findMaxOverlap(overlapRange, outputRange, iSyms.min, iSyms.max);
			int minPos = outputRange[0];
			// We add 1 to the maxPos index.
			// Since filePos is recorded at the *beginning* of each line, this allows us to read the last element.
			int maxPos = outputRange[1] + 1;

			if (minPos >= maxPos) {
				// Nothing found, or invalid values passed in.
				return Collections.<SeqSymmetry>emptyList();
			}
			byte[] bytes = IndexingUtils.readBytesFromFile(
					iSyms.file, iSyms.filePos[minPos], (int) (iSyms.filePos[maxPos] - iSyms.filePos[minPos]));

			if ((iSyms.iWriter instanceof PSLParser || iSyms.iWriter instanceof PSL) && iSyms.ext.equalsIgnoreCase("link.psl")) {
				String indexesFileName = iSyms.file.getAbsolutePath();
				newIstr = IndexingUtils.readAdditionalLinkPSLIndex(indexesFileName, annot_type, bytes);
			} else {
				newIstr = new ByteArrayInputStream(bytes);
			}
			dis = new DataInputStream(newIstr);

			return iSyms.iWriter.parse(dis, annot_type, group);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		finally {
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(newIstr);
		}
	}

}
