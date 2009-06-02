package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.util.SearchableCharIterator;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.util.DNAUtils;
import com.affymetrix.genometry.util.SeqUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *   Implements SearchableCharacterIterator to allow for regex matching
 *   without having to build a String out of the byte array (which would kind of
 *   defeat the purpose of saving memory...)
 *
 *   Also imposes structure in the top two levels of annotation hierarchy.
 *   First level for a given type is a container symmetry with that type;
 *   second level is still containers, broken down by location, and dependent on how
 *      the annotatations were loaded.
 *
 *   Also adds reference to AnnotatedSeqGroup (getSeqGroup()), and
 *     isSynonymous() method.
 *
 * @version: $Id$
 */
public final class SmartAnnotBioSeq
				implements SearchableCharIterator, MutableAnnotatedBioSeq, CompositeBioSeq {

	private Map<String, SymWithProps> type_id2sym = null;   // lazy instantiation of type ids to container annotations
	private AnnotatedSeqGroup seq_group;
	private int start;	//The index of the first residue of the sequence.
	private int end;	//The index of the last residue of the sequence.

	private SeqSymmetry compose;	//SeqSymmetry to store the sequence in.
	/**
	 * Length of the sequence, stored as a double.  The value is always an
	 * integer and much of the functionality of this class and its sub-classes
	 * is lost if the length is greater than Integer.INT_MAX.
	 */
	private double length = 0;

	private final String id;	// String identifier for the sequence.  This is not guaranteed to be unique.
	private static final boolean DEBUG = false;
	
	private List<SeqSymmetry> annots;
	String version;

	// GAH 8-14-2002: need a residues field in case residues need to be cached
	// (rather than derived from composition), or if we choose to store residues here
	// instead of in composition seqs in case we actually want to compose/cache
	// all residues...
	private String residues;
	SearchableCharIterator residues_provider;

	public SmartAnnotBioSeq(String seqid, String seqversion, int length) {
		this.id = seqid;
		this.length = length;
		start = 0;
		end = length;
		this.version = seqversion;
	}

	public AnnotatedSeqGroup getSeqGroup() {
		return seq_group;
	}

	public void setSeqGroup(AnnotatedSeqGroup group) {
		seq_group = group;
	}

	/**
	 *  returns Map of type id to container sym for annotations of that type
	 */
	public Map<String, SymWithProps> getTypeMap() {
		return type_id2sym;
	}

	@Override
	public String toString() {
		return this.getID();
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String str) {
		this.version = str;
	}

	public SeqSymmetry getComposition() {
		return compose;
	}

	public void setComposition(SeqSymmetry compose) {
		this.compose = compose;
	}

		public String getID() {
		return id;
	}

	/**
	 * Returns the integer index of the first residue of the sequence.  Negative
	 * values are acceptable.  The value returned is undefined if the minimum
	 * value is set using setBoundsDouble(double, double) to something outside
	 * of Integer.MIN_VALUE and Integer.MAX_VALUE.
	 *
	 * @return the integer index of the first residue of the sequence.
	 */
	public int getMin() {
		return start;
	}

	/**
	 * Returns the integer index of the last residue of the sequence.  The
	 * maximum value must always be greater than the minimum value.  The value
	 * returned is undefined if the maximum value is set using
	 * setBoundsDouble(double, double) to something outside of Integer.MIN_VALUE
	 * and Integer.MAX_VALUE.
	 *
	 * @return the integer index of the last residue of the sequence.
	 */
	public int getMax() {
		return end;
	}

	/**
	 *  Returns a top-level symmetry or null.
	 *  Used to return a TypeContainerAnnot, but now returns a SymWithProps which is
	 *     either a TypeContainerAnnot or a GraphSym, or a ScoredContainerSym,
	 *     so GraphSyms can be retrieved with graph id given as type
	 */
	public SymWithProps getAnnotation(String type) {
		if (type_id2sym == null) {
			return null;
		}
		return type_id2sym.get(type);
	}

	public List<SymWithProps> getAnnotations(Pattern regex) {
		List<SymWithProps> results = new ArrayList<SymWithProps>();
		if (type_id2sym != null) {
			Matcher match = regex.matcher("");
			for (Map.Entry<String, SymWithProps> entry : type_id2sym.entrySet()) {
				String type = entry.getKey();
				// System.out.println("  type: " + type);
				if (match.reset(type).matches()) {
					results.add(entry.getValue());
				}
			}
		}
		return results;
	}

	/**
	 *  Creates an empty top-level container sym.
	 *  @return an instance of {@link TypeContainerAnnot}
	 */
	public synchronized MutableSeqSymmetry addAnnotation(String type) {
		type = type;
		if (type_id2sym == null) {
			type_id2sym = new LinkedHashMap<String, SymWithProps>();
		}
		TypeContainerAnnot container = new TypeContainerAnnot(type);
		container.setProperty("method", type);
		SeqSpan span = new SimpleSeqSpan(0, this.getLength(), this);
		container.addSpan(span);
		type_id2sym.put(type, container);
		if (null == annots) {
			annots = new ArrayList<SeqSymmetry>();
		}
		annots.add(container);
		//notifyModified();
		return container;
	}

	/**
	 *  Adds an annotation as a child of the top-level container sym
	 *     for the given type.  Creates new top-level container
	 *     if doesn't yet exist.
	 */
	public synchronized void addAnnotation(SeqSymmetry sym, String type) {
		type = type;
		if (type_id2sym == null) {
			type_id2sym = new LinkedHashMap<String, SymWithProps>();
		}
		MutableSeqSymmetry container = (MutableSeqSymmetry) type_id2sym.get(type);
		if (container == null) {
			container = addAnnotation(type);
		}
		container.addChild(sym);
	//notifyModified();
	}

	/**
	 *  Overriding addAnnotation(sym) to try and extract a "method"/"type" property
	 *    from the sym.
	 *  <pre>
	 *    If can be found, then instead of adding annotation directly
	 *    to seq, use addAnnotation(sym, type).  Which ends up adding the annotation
	 *    as a child of a container annotation (generally means two levels of container,
	 *    since parsers call addAnnotation with a container already if indicated).
	 *    So for example for DAS transcript-exon annotation will get a four-level
	 *    hierarchy:
	 *       1. Top-level container container per seq
	 *       2. 2nd-level container container per DAS call (actually probably special DasFeatureRequestSym
	 *       3. Transcript syms
	 *       4. Exon syms
	 *
	 *  GraphSym's and ScoredContainerSym's are added directly, not in containers.
	 *  </pre>
	 */
	public synchronized void addAnnotation(SeqSymmetry sym) {
		if (!needsContainer(sym)) {
			if (type_id2sym == null) {
				type_id2sym = new LinkedHashMap<String, SymWithProps>();
			}
			String id = sym.getID();
			if (id == null) {
				System.out.println("WARNING: ID is null!!!  sym: " + sym);
				throw new RuntimeException("in SmartAnnotBioSeq.addAnnotation, sym.getID() == null && (! needsContainer(sym)), this should never happen!");
			}
			if (sym instanceof SymWithProps) {
				type_id2sym.put(id, (SymWithProps) sym);
			} else {
				throw new RuntimeException("n SmartAnnotBioSeq.addAnnotation: sym must be a SymWithProps");
			}

			if (null == annots) {
				annots = new ArrayList<SeqSymmetry>();
			}
			annots.add(sym);
			//notifyModified();
			return;
		}
		String type = determineMethod(sym);
		if (type != null) {
			// add as child to the top-level container
			addAnnotation(sym, type); // side-effect calls notifyModified()
			return;
		} //    else { super.addAnnotation(sym); }  // this includes GraphSyms
		else {
			throw new RuntimeException(
							"SmartAnnotBioSeq.addAnnotation(sym) will only accept " +
							" SeqSymmetries that are also SymWithProps and " +
							" have a _method_ property");
		}
	}

	public synchronized void removeAnnotation(SeqSymmetry annot) {
		if (!needsContainer(annot)) {
			if (null != annots) {
				annots.remove(annot);
			}
		//notifyModified();
		//      return;
		} else {
			String type = determineMethod(annot);
			if ((type != null) && (getAnnotation(type) != null)) {
				MutableSeqSymmetry container = (MutableSeqSymmetry) getAnnotation(type);
				if (container == annot) {
					type_id2sym.remove(type);
					if (null != annots) {
						annots.remove(annot);
					}
				//notifyModified();
				//	  return;
				} else {
					container.removeChild(annot);
				//notifyModified();
				}
			}
		}
	}

	public int getAnnotationCount() {
		if (null != annots) {
			return annots.size();
		} else {
			return 0;
		}
	}

	public SeqSymmetry getAnnotation(int index) {
		if (null != annots && index < annots.size()) {
			return annots.get(index);
		} else {
			return null;
		}
	}

	/**
	 * Returns true if the sym is of a type needs to be wrapped in a {@link TypeContainerAnnot}.
	 * GraphSym's and ScoredContainerSym's are added directly, not in containers.
	 */
	private static boolean needsContainer(SeqSymmetry sym) {
		return !(sym instanceof GraphSym || sym instanceof ScoredContainerSym || sym instanceof TypeContainerAnnot);
	}

	/**
	 *  Finds the "method" for a SeqSymmetry.
	 *  Looks for the "method" in four places, in order:
	 *   (1) the property "method", (2) the property "meth",
	 *   (3) the property "type", (4) TypedSym.getType().
	 *  If no method is found, returns null.
	 */
	public static String determineMethod(SeqSymmetry sym) {
		String meth = null;
		if (sym instanceof SymWithProps) {
			SymWithProps psym = (SymWithProps) sym;
			meth = (String) psym.getProperty("method");
			if (meth == null) {
				meth = (String) psym.getProperty("meth");
			}
			if (meth == null) {
				meth = (String) psym.getProperty("type");
			}
		}
		if (meth == null) {
			if (sym instanceof TypedSym) {
				meth = ((TypedSym) sym).getType();
			}
		}
		if (meth != null) {
			meth = meth;
		}
		return meth;
	}

	

	public void setResiduesProvider(SearchableCharIterator chariter) {
		if (chariter.getLength() != this.getLength()) {
			System.out.println("WARNING -- in setResidueProvider, lengths don't match");
		}
		residues_provider = chariter;
	}

	public SearchableCharIterator getResiduesProvider() {
		return residues_provider;
	}

	/**
	 * Returns true if all residues on the sequence are available.
	 *
	 * @return true if all residues on the sequence are available.
	 */
	public boolean isComplete() {
		return isComplete(start, end);
	}

	/**
	 * Returns true if all residues between start and end are available.  Unknown
	 * if implementations of this function are inclusive or exclusive on start
	 * and end.
	 * <p />
	 * <em>WARNING:</em> This implementation is flawed.  It only verifies that
	 * all SeqSymmetrys are complete, not that the SeqSymmetrys completely
	 * cover the range in question.
	 *
	 * @param  start the start index (inclusive?)
	 * @param  end   the end index (exclusive?)
	 * @return       true if all residues betwen start and end are available
	 */
	public boolean isComplete(int start, int end) {
		if (residues_provider != null) {
			return true;
		}

		if (residues != null) {
			return true;
		}

		// assuming that if all sequences the composite is composed of are
		//    complete, then composite is also complete
		//    [which is an invalid assumption! Because that further assumes that composed seq
		//     is fully covered by the sequences that it is composed from...]
		SeqSymmetry rootsym = this.getComposition();

		if (rootsym == null) {
			return false;
		}
		int comp_count = rootsym.getChildCount();
		if (comp_count == 0) {
			BioSeq other_seq = SeqUtils.getOtherSeq(rootsym, this);
			return other_seq.isComplete(start, end);
		}

		boolean comp_complete = true;
		for (int i = 0; i < comp_count; i++) {
			SeqSymmetry comp_sym = rootsym.getChild(i);
			BioSeq other_seq = SeqUtils.getOtherSeq(comp_sym, this);
			comp_complete = (comp_complete & other_seq.isComplete());
		}
		return comp_complete;
	}

	public char charAt(int pos) {
		if (residues_provider == null) {
			String str = getResidues(pos, pos + 1, '-');
			if (str == null) {
				return '-';
			} else {
				return str.charAt(0);
			}
		} else {
			return residues_provider.charAt(pos);
		}
	}

	public String substring(int start, int end) {
		if (residues_provider == null) {
			return getResidues(start, end);
		}
		return residues_provider.substring(start, end);
	}

	public int indexOf(String str, int fromIndex) {
		if (residues_provider != null) {
			return residues_provider.indexOf(str, fromIndex);
		}
		if (residues != null) {
			return residues.indexOf(str, fromIndex);
		}
		System.out.println("WARNING: Could not find residues for " + str);
		return -1;
	}



	/**
	 * Returns all residues on the sequence.
	 *
	 * @return a String containing all residues on the sequence.
	 */
	public String getResidues() {
		return getResidues(start, end);
	}

	/**
	 * Returns all residues on the sequence.
	 *
	 * @return a String containing all residues on the sequence
	 */
	public String getResidues(int start, int end) {
		return getResidues(start, end, ' ');
	}

	/**
	 * Returns the residues on the sequence between start and end using the
	 * fillchar to fill any gaps in the sequence.  Unknown if this implementation
	 * is inclusive or exclusive on start and end.
	 *
	 * @param  start    the start index (inclusive?)
	 * @param  end      the end index (exclusive?)
	 * @param  fillchar the character to fill empty residues in the sequence with.
	 * @return          a String containing residues between start and end.
	 */
	private String getResidues(int start, int end, char fillchar) {
		int residue_length = this.getLength();
		if (start < 0 || residue_length <= 0) {
			return null;
		}

		// Sanity checks on argument size.
		start = Math.min(start, residue_length);
		end = Math.min(end, residue_length);
		if (start <= end) {
			end = Math.min(end, start + residue_length);
		} else {
			start = Math.min(start, end + residue_length);
		}

		if (residues == null) {
			SeqSpan residue_span = new SimpleSeqSpan(start, end, this);
			int reslength = Math.abs(end - start);
			char[] char_array = new char[reslength];
			java.util.Arrays.fill(char_array, fillchar);
			SeqSymmetry rootsym = this.getComposition();
			if (rootsym == null) {
				return null;
			}
			// adjusting index into array to compensate for possible seq start < 0

			getResidues(residue_span, rootsym, char_array);
			// Note that new String(char[]) causes the allocation of a second char array
			String res = new String(char_array);
			return res;
		}

		if (start <= end) {
			return residues.substring(start, end);
		}

		// start > end -- that means reverse complement.
		return DNAUtils.reverseComplement(residues.substring(end, start));
	}

	/**
	 * Function for finding residues.  This function is a bit of a mess:
	 * the implementation is more confusing than it needs to be.
	 *
	 * @param this_residue_span the SeqSpan to find residues on
	 * @param sym               the SeqSymmetry to search for residues
	 * @param residues          the character array to be filled with residues
	 */
	private void getResidues(SeqSpan this_residue_span, SeqSymmetry sym, char[] residues) {
		int symCount = sym.getChildCount();
		if (symCount == 0) {
			SeqSpan this_comp_span = sym.getSpan(this);
			if (SeqUtils.intersects(this_comp_span, this_residue_span)) {
				BioSeq other_seq = SeqUtils.getOtherSeq(sym, this);
				SeqSpan other_comp_span = sym.getSpan(other_seq);
				MutableSeqSpan ispan = new SimpleMutableSeqSpan();
				boolean intersects = SeqUtils.intersection(this_comp_span, this_residue_span, ispan);
				MutableSeqSpan other_residue_span = new SimpleMutableSeqSpan();
				SeqUtils.transformSpan(ispan, other_residue_span, other_seq, sym);
				boolean opposite_strands = this_comp_span.isForward() ^ other_comp_span.isForward();
				boolean resultForward = opposite_strands ^ this_residue_span.isForward();
				String spanResidues;
				if (resultForward) {
					spanResidues = other_seq.getResidues(other_residue_span.getMin(), other_residue_span.getMax());
				} else {
					spanResidues = other_seq.getResidues(other_residue_span.getMax(), other_residue_span.getMin());
				}
				if (spanResidues != null) {
					if (DEBUG) {
						System.out.println(spanResidues.substring(0, 15) + "..." + spanResidues.substring(spanResidues.length() - 15));
						System.out.println("desired span: " + SeqUtils.spanToString(this_residue_span));
						System.out.println("child residue span: " + SeqUtils.spanToString(this_comp_span));
						System.out.println("intersect(child_span, desired_span): " + SeqUtils.spanToString(ispan));
						System.out.println("other seq span: " + SeqUtils.spanToString(other_residue_span));
						System.out.println("opposite strands: " + opposite_strands);
						System.out.println("result forward: " + resultForward);
						System.out.println("start < end: " + (other_residue_span.getStart() < other_residue_span.getEnd()));
						System.out.println("");
					}
					int offset = ispan.getMin() - this_residue_span.getMin();
					for (int j = 0; j < spanResidues.length(); j++) {
						residues[offset + j] = spanResidues.charAt(j);
					}
				}
			}
		} else {
			// recurse to children
			for (int i = 0; i < symCount; i++) {
				SeqSymmetry childSym = sym.getChild(i);
				getResidues(this_residue_span, childSym, residues);
			}
		}
	}



	
	public void setLength(int length) {
		setBounds(0, length);  // sets start, end, bounds

		// if length does not agree with length of residues, null out residues
		if ((residues != null) && (residues.length() != length)) {
			System.out.println("*** WARNING!!! lengths disagree: residues = " + residues.length() +
							", seq = " + this.length + ", nulling out residues ****");
			residues = null;
		}
	}

	public void setResidues(String residues) {
		if (DEBUG) {
			System.out.println("**** called SmartAnnotBioSeq.setResidues()");
		}
		if (residues.length() != this.length) {
			System.out.println("********************************");
			System.out.println("*** WARNING!!! lengths disagree: residues = " + residues.length() +
							", seq = " + this.length + " ****");
			System.out.println("********************************");
		}
		this.residues = residues;
		this.length = residues.length();
	}

	/**
	 * Sets the start and end of the sequence as double values.
	 * <p />
	 * <em>WARNING:</em> min and max are stored intenally using integers.  If
	 * min or max are outside of the range Integer.MIN_VALUE and
	 * Interger.MAX_VALUE, the values will not be stored properly.  The length
	 * (min - max) is computed and stored as a double before min and max are
	 * downcast to int.
	 *
	 * @param min the index of the first residue of the sequence, as a double.
	 * @param max the index of the last residue of the sequence, as a double.
	 */
	public void setBoundsDouble(double min, double max) {
		length = max - min;
		if (min < Integer.MIN_VALUE) {
			start = Integer.MIN_VALUE + 1;
		} else {
			start = (int) min;
		}
		if (max > Integer.MAX_VALUE) {
			end = Integer.MAX_VALUE - 1;
		} else {
			end = (int) max;
		}
	}

	/**
	 * Sets the start and end of the sequence
	 *
	 * @param min the index of the first residue of the sequence.
	 * @param max the index of the last residue of the sequence.
	 */
	public void setBounds(int min, int max) {
		start = min;
		end = max;
		//    length = end - start;
		length = (double) end - (double) start;
	}

	

	/**
	 * Returns the number of residues in the sequence as a double.
	 *
	 * @return the number of residues in the sequence as a double
	 */
	public double getLengthDouble() {
		return length;
	}

	public int getLength() {
		if (length > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE - 1;
		} else {
			return (int) length;
		}
	}
}
