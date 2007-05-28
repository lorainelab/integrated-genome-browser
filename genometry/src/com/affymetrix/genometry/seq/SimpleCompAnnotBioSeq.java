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

package com.affymetrix.genometry.seq;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.DNAUtils;

import java.util.ArrayList;
import java.util.List;

public class SimpleCompAnnotBioSeq
  //  extends SimpleCompositeBioSeq
  extends CompositeNegSeq
  implements CompositeBioSeq, MutableAnnotatedBioSeq, MutableBioSeq {

  boolean DEBUG = false;
  // GAH 8-14-2002: need a residues field in case residues need to be cached
  // (rather than derived from composition), or if we choose to store residues here
  // instead of in composition seqs in case we actually want to compose/cache
  // all residues...
  protected String residues;

  protected List<SeqSymmetry> annots;

  public String getResidues(int start, int end, char fillchar) {
    String result = null;
    if (start < 0 || end > getLength()) { result = null; }
    else if (residues == null) {
      result = super.getResidues(start, end, fillchar);
    }
    else  {
      if (start == 0 && end == getLength()) {
	result = residues; }
      else if (start == getLength() && end == 0) {
	result = DNAUtils.reverseComplement(residues);  }
      else if (start <= end) {
	result = residues.substring(start, end);
      }
      else {
	result = DNAUtils.reverseComplement(residues.substring(start, end));
      }
    }
    return result;
  }

  public boolean isComplete(int start, int end) {
    if (residues != null) { return true; }
    else  { return super.isComplete(start, end); }
  }

  //-----------------------------------------------------
  // BEGIN Methods copied from SimpleAnnotatedBioSeq
  //-----------------------------------------------------
  public SimpleCompAnnotBioSeq(String id, int length)  {
    super(id, length);
  }

  public SimpleCompAnnotBioSeq(String id)  {
    super(id);
  }

  public SimpleCompAnnotBioSeq()  { }

  public void addAnnotation(SeqSymmetry annot) {
    if (null == annots) { annots = new ArrayList<SeqSymmetry>(); }
    annots.add(annot);
  }

  public void removeAnnotation(SeqSymmetry annot) {
    if (null != annots) {
      annots.remove(annot);
    }
  }

  public void removeAnnotation(int index) {
    if (null != annots) {
      annots.remove(index);
    }
  }

  public int getAnnotationCount() {
    if (null != annots) return annots.size();
    else return 0;
  }

  public SeqSymmetry getAnnotation(int index) {
    if (null != annots && index < annots.size())
      return annots.get(index);
    else
      return null;
  }

  /** NOT YET IMPLEMENTED */
  public SeqSymmetry getAnnotationByID(String id) { return null; }
  /** NOT YET IMPLEMENTED */
  public List getIntersectedAnnotations(SeqSpan span) { return null; }
  /** NOT YET IMPLEMENTED */
  public List getContainedAnnotations(SeqSpan span) { return null; }
  //-----------------------------------------------------
  // END Methods copied from SimpleAnnotatedBioSeq
  //-----------------------------------------------------


  //-----------------------------------------------------
  // BEGIN methods copied from SimpleBioSeq
  //-----------------------------------------------------
  public void setID(String id) { this.id = id; }
  public void setLength(int length) {
    //    this.length = length;
    setBounds(0, length);  // sets start, end, bounds

    // if length does not agree with length of residues, null out residues
    if ((residues != null) && (residues.length() != length)) {
      System.out.println("*** WARNING!!! lengths disagree: residues = " + residues.length() +
			 ", seq = " + this.length + ", nulling out residues ****");
      residues = null;
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
  //-----------------------------------------------------
  // END methods copied from SimpleBioSeq
  //-----------------------------------------------------

}


