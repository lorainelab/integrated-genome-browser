/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometryImpl;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.MutableSeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *   Extends GeneralBioSeq to add "retrieve top-level feature by 'method'/'type'".
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
public final class SmartAnnotBioSeq extends GeneralBioSeq implements MutableAnnotatedBioSeq  {
	private static final boolean DEBUG = false;

	private Map<String, SymWithProps> type_id2sym = null;   // lazy instantiation of type ids to container annotations

	private AnnotatedSeqGroup seq_group;

	private List<SeqSymmetry> annots;

	public SmartAnnotBioSeq(String seqid, String seqversion, int length) {
		super(seqid, seqversion, length);
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

	public String getVersion() { return version; }

	public void setVersion(String str) { this.version = str; }

	/**
	 * Returns the number of residues in the sequence as a double.
	 *
	 * @return the number of residues in the sequence as a double
	 */
	public double getLengthDouble() {
		return length;
	}

	/**
	 *  Returns a top-level symmetry or null.
	 *  Used to return a TypeContainerAnnot, but now returns a SymWithProps which is
	 *     either a TypeContainerAnnot or a GraphSym, or a ScoredContainerSym,
	 *     so GraphSyms can be retrieved with graph id given as type
	 */
	public SymWithProps getAnnotation(String type) {
		if (type_id2sym == null) { return null; }	
		return type_id2sym.get(type);
	}

	public List<SymWithProps> getAnnotations(Pattern regex) {
		List<SymWithProps> results = new ArrayList<SymWithProps>();
		if (type_id2sym != null)  {
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
		if (type_id2sym == null) { 
			type_id2sym = new LinkedHashMap<String,SymWithProps>(); 
		}
		TypeContainerAnnot container = new TypeContainerAnnot(type);
		container.setProperty("method", type);
		SeqSpan span = new SimpleSeqSpan(0, this.getLength(), this);
		container.addSpan(span);
		type_id2sym.put(type, container);
		if (annots == null) {
			annots = new ArrayList<SeqSymmetry>();
		}
		annots.add(container);

		return container;
	}

	/**
	 *  Adds an annotation as a child of the top-level container sym
	 *     for the given type.  Creates new top-level container
	 *     if doesn't yet exist.
	 */
	public synchronized void addAnnotation(SeqSymmetry sym, String type) {
		if (type_id2sym == null) { 
			type_id2sym = new LinkedHashMap<String,SymWithProps>(); 
		}
		MutableSeqSymmetry container = (MutableSeqSymmetry) type_id2sym.get(type);
		if (container == null) {
			container = addAnnotation(type);
		}
		container.addChild(sym);
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
	 *       1. Top-level container annot per seq
	 *       2. 2nd-level container annot per DAS call (actually probably special DasFeatureRequestSym
	 *       3. Transcript syms
	 *       4. Exon syms
	 *
	 *  GraphSym's and ScoredContainerSym's are added directly, not in containers.
	 *  </pre>
	 */
	public synchronized void addAnnotation(SeqSymmetry sym) {
		if (! needsContainer(sym)) {
			if (type_id2sym == null) { 
				type_id2sym = new LinkedHashMap<String,SymWithProps>(); 
			}
			String id = sym.getID();
			if (id == null) {
				System.out.println("WARNING: ID is null!!!  sym: " + sym);
				throw new RuntimeException("in SmartAnnotBioSeq.addAnnotation, sym.getID() == null && (! needsContainer(sym)), this should never happen!");
			}
			if (sym instanceof SymWithProps) {
				type_id2sym.put(id, (SymWithProps) sym);
			} else {
				throw new RuntimeException("in SmartAnnotBioSeq.addAnnotation: sym must be a SymWithProps");
			}
			if (annots == null) {
				annots = new ArrayList<SeqSymmetry>();
			}
			annots.add(sym);
			return;
		}
		String type = determineMethod(sym);
		if (type != null)  {
			// add as child to the top-level container
			addAnnotation(sym, type); // side-effect calls notifyModified()
			return;
		}
		//    else { super.addAnnotation(sym); }  // this includes GraphSyms
		else  {
			throw new RuntimeException(
					"SmartAnnotBioSeq.addAnnotation(sym) will only accept " +
					" SeqSymmetries that are also SymWithProps and " +
					" have a _method_ property");
		}
	}

	public synchronized void removeAnnotation(SeqSymmetry annot) {
		if (! needsContainer(annot)) {
			if (null != annots) {
				annots.remove(annot);
			}
		} else {
			String type = determineMethod(annot);
			if ((type != null) && (getAnnotation(type) != null)) {
				MutableSeqSymmetry container = (MutableSeqSymmetry) getAnnotation(type);
				if (container == annot) {
					type_id2sym.remove(type);
					if (null != annots) {
						annots.remove(annot);
					}
				} else {
					container.removeChild(annot);
				}
			}
		}
	}


	/**
	 * Returns true if the sym is of a type needs to be wrapped in a {@link TypeContainerAnnot}.
	 * GraphSym's and ScoredContainerSym's are added directly, not in containers.
	 */
	private static boolean needsContainer(SeqSymmetry sym) {
		if (sym instanceof GraphSym || sym instanceof ScoredContainerSym || sym instanceof TypeContainerAnnot) {
			return false;
		} else {
			return true;
		}
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
		if (sym instanceof SymWithProps)  {
			SymWithProps psym = (SymWithProps)sym;
			meth = (String)psym.getProperty("method");
			if (meth == null) { meth = (String) psym.getProperty("meth"); }
			if (meth == null) { meth = (String) psym.getProperty("type"); }
		}
		if (meth == null) {
			if (sym instanceof TypedSym) {
				meth = ((TypedSym)sym).getType();
			}
		}
		return meth;
	}

	public int getAnnotationCount() {
		if (null != annots) return annots.size();
		return 0;
	}

	public /*@Nullable*/ SeqSymmetry getAnnotation(int index) {
		if (null != annots && index < annots.size())
			return annots.get(index);
		return null;
	}

	public void setLength(int length) {
		setBounds(0, length);  // sets start, end, bounds

		// if length does not agree with length of residues, null out residues
		if ((residues != null) && (residues.length() != length)) {
			System.out.println("*** WARNING!!! lengths disagree: residues = " + residues.length() +
					", seq = " + this.length);
			//residues = null;
		}
	}

	public void setResidues(String residues) {
		if (DEBUG)  { System.out.println("**** called SimpleCompAnnotBioSeq.setResidues()"); }
		if (residues.length() != this.length) {
			System.out.println("********************************");
			System.out.println("*** WARNING!!! lengths disagree: residues = " + residues.length() +
					", seq = " + this.length + " ****");
			System.out.println("********************************");
		}
		this.residues = residues;
		this.length = residues.length();
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
	 * Returns the integer index of the first residue of the sequence.  Negative
	 * values are acceptable.  The value returned is undefined if the minimum
	 * value is set using setBoundsDouble(double, double) to something outside
	 * of Integer.MIN_VALUE and Integer.MAX_VALUE.
	 *
	 * @return the integer index of the first residue of the sequence.
	 */
	public int getMin() { return start; }

	/**
	 * Returns the integer index of the last residue of the sequence.  The
	 * maximum value must always be greater than the minimum value.  The value
	 * returned is undefined if the maximum value is set using
	 * setBoundsDouble(double, double) to something outside of Integer.MIN_VALUE
	 * and Integer.MAX_VALUE.
	 *
	 * @return the integer index of the last residue of the sequence.
	 */
	public int getMax() { return end; }

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
		if (min < Integer.MIN_VALUE) { start = Integer.MIN_VALUE + 1; }
		else { start = (int)min; }
		if (max > Integer.MAX_VALUE) { end = Integer.MAX_VALUE - 1; }
		else { end = (int)max; }
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
		length = (double)end - (double)start;
	}

	@Override
		public String toString() {
			return this.getID();
		}

}
