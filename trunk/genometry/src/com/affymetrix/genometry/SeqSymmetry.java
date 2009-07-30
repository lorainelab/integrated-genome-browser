/**
 *   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.genometry;

/**
 * Implementations model a collection of {@link SeqSpan}s.
 * SeqSymmetries are also a collection of SeqSymmetries.
 * Each one can have zero or more children.
 */
public interface SeqSymmetry {
	public String getID();
	public int getSpanCount();

	//  public int getSpanCount(MutableAnnotatedBioSeq seq);

	/**
	 *  Convenience method to get a SeqSpan whose MutableAnnotatedBioSeq is seq
	 *  If the SeqSymmetry has no spans with the specified seq, returns null
	 *  If the SeqSymmetry contains only one SeqSpan whose MutableAnnotatedBioSeq is seq, then that span is returned
	 *  If the SeqSymmetry contains multiple spans whose MutableAnnotatedBioSeq is seq, 
	 *      then only one these spans is returned -- which one is unspecified
	 *  
	 *  In many situations it will already be known by the calling code which SeqSymmetrys have 
	 *     only one span per seq, in which case getSpan(seq) is very convenient.  However, in 
	 *     general a SeqSymmetry can have multiple spans with the same seq, so unless the calling code 
	 *     already known a symmetry has only one span per seq, it should only use getSpan(seq) if 
	 *     sym.getSpanCount(seq) <= 1, otherwise it should iterate through all the spans 
	 *     via sym.getSpan(index) to find each span that points to the seq 
	 *
	 *  Other possibilities considered:
	 *    public List getSeqSpans(MutableAnnotatedBioSeq seq)  -- but too much potential extra List creation and/or storage
	 *    public void getSeqSpans(MutableAnnotatedBioSeq seq, List spans)  -- spans with MutableAnnotatedBioSeq seq are added to passed in list
	 *  If either of these methods is added could probably remove getSpanCount(seq) method.
	 *  If add getSeqSpans(seq, spans) could possibly also remove getSpan(seq)
	 */
	public SeqSpan getSpan(MutableAnnotatedBioSeq seq);
	public SeqSpan getSpan(int index);
	/** maybe this method should be called copySpan() ? */
	public boolean getSpan(MutableAnnotatedBioSeq seq, MutableSeqSpan span);
	/**  maybe this method should be called copySpan() ? */
	public boolean getSpan(int index, MutableSeqSpan span);
	/** 
	 * getSpanSeq(index) is not being used much, so might remove it soon (4-22-2005)
	 */
	public MutableAnnotatedBioSeq getSpanSeq(int index);
	public int getChildCount();
	public SeqSymmetry getChild(int index);

	// Removed getParent() to allow for possibility of same symmetry being used in multiple 
	//      hiearchies (having multiple parents)
	//   public SeqSymmetry getParent();

	// Methods to improve efficiency for "compressed" SeqSymmetry implementations
	//  public boolean getChildSpan(int child_index, MutableAnnotatedBioSeq seq, MutableSeqSpan span);
	//  public boolean getChildSpan(int child_index, int child_span_index, MutableSeqSpan span);


}
