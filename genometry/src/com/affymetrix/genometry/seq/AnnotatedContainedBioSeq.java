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

package com.affymetrix.genometry.seq;

import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.IOException;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.MutableBioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;

public class AnnotatedContainedBioSeq
  extends SimpleAnnotatedBioSeq {
  
  protected BioSeq bsq;

  public AnnotatedContainedBioSeq(String id) {
    super(id, 0);
    annots = new ArrayList();
    this.bsq = new SimpleBioSeq(id, 0);
  }

  public AnnotatedContainedBioSeq(String id, BioSeq bsq, List annotlist) {
    super(id, 0, annotlist);
    this.bsq = bsq;
  }

  public AnnotatedContainedBioSeq(String id, BioSeq bsq)  {
    super(id, 0);
    annots = new ArrayList();
    this.bsq = bsq;
  }

  public void setContainedBioSeq(BioSeq bsq) {
    this.bsq = bsq;
  }
  
  public BioSeq getContainedBioSeq() {
    return bsq;
  }
  
  public String getID() {
    if (null != bsq) { return bsq.getID(); }
    else { return super.getID(); }
  }
  
  public int getLength() {
    if (null != bsq) { return bsq.getLength(); }
    else { return super.getLength(); }
  }
  
  public void setLength(int length) {
    if (null != bsq) {
      if (bsq instanceof MutableBioSeq) {
        ((MutableBioSeq) bsq).setLength(length);
      }
    }
    super.setLength(length);
  }
  
  public String getResidues() {
    if (null != bsq) { return bsq.getResidues(); }
    else { return super.getResidues(); }
  }
  
  public String getResidues(int start, int end) {
    if (null != bsq) { return bsq.getResidues(start, end); }
    else { return super.getResidues(start, end); }
  }
  
  public boolean isComplete() { 
    if (null != bsq) { return bsq.isComplete(); }
    else { return super.isComplete(); }
  }
  
  public boolean isComplete(int start, int end) {
    if (null != bsq) { return bsq.isComplete(start, end); }
    else { return super.isComplete(start, end); }
  }

}


