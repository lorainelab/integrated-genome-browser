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

package com.affymetrix.igb.genometry;

import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.util.SynonymLookup;

public class AnnotatedSeqGroup {
  String id;
  String version;  // not currently used
  String organism;  // not currently used
  Date version_date;  // not currently used
  String description;

  Map id2seq = new LinkedHashMap();
  ArrayList seqlist = new ArrayList();

  public AnnotatedSeqGroup(String gid) {
    id = gid;
  }

  public String getID() { return id; }

  public Map getSeqs() { return id2seq; }

  public MutableAnnotatedBioSeq getSeq(int index) {
    if (index < seqlist.size()) {
      return (MutableAnnotatedBioSeq)seqlist.get(index);
    }
    else { return null; }
  }

  public MutableAnnotatedBioSeq getSeq(String synonym) {
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)id2seq.get(synonym);
    if (aseq == null) {
      // try and find a synonym
      Iterator iter = id2seq.values().iterator();
      while (iter.hasNext()) {
        MutableAnnotatedBioSeq synseq = (MutableAnnotatedBioSeq)iter.next();
        if (synseq instanceof SmartAnnotBioSeq)  {
	  if (((SmartAnnotBioSeq)synseq).isSynonymous(synonym)) {
	    aseq = synseq;
	    break;
	  }
	}
	else {
	  SynonymLookup lookup = SynonymLookup.getDefaultLookup();
	  if (lookup.isSynonym(synseq.getID(), synonym)) {
	    aseq = synseq;
	    break;
	  }
	}
      }
    }
    return aseq;
  }

  /**
   *  For the given symmetry, tries to find in the group a sequence
   *    that is pointed to by that symmetry.
   *  @return the first sequence it finds (by iterating through sym's spans),
   *    or null if none is found.
   */
  public MutableAnnotatedBioSeq getSeq(SeqSymmetry sym) {
    MutableAnnotatedBioSeq result = null;
    if (sym != null) {
      int spancount = sym.getSpanCount();
      for (int i=0; i<spancount; i++) {
	SeqSpan span = sym.getSpan(i);
	BioSeq seq1 = span.getBioSeq();
	String id = seq1.getID();
	MutableAnnotatedBioSeq seq2 = (MutableAnnotatedBioSeq)id2seq.get(id);
	if ((seq2 != null) && (seq1 == seq2)) {
	  result = seq2;
	  break;
	}
      }
    }
    return result;
  }



  public boolean isSynonymous(String synonym) {
    if (id.equals(synonym)) { return true; }
    else {
      SynonymLookup lookup = SynonymLookup.getDefaultLookup();
      return (lookup.isSynonym(id, synonym));
    }
  }

  public MutableAnnotatedBioSeq addSeq(String seqid, int length) {
    MutableAnnotatedBioSeq aseq = new SmartAnnotBioSeq(seqid, this.getID(), length);
    this.addSeq(aseq);
    return aseq;
  }

  public void addSeq(MutableAnnotatedBioSeq seq) {
    MutableAnnotatedBioSeq oldseq = (MutableAnnotatedBioSeq)id2seq.get(seq.getID());
    if (oldseq == null) {
      id2seq.put(seq.getID(), seq);
      seqlist.add(seq);
      if (seq instanceof SmartAnnotBioSeq) {
	((SmartAnnotBioSeq)seq).setSeqGroup(this);
      }
    }
    else {
      throw new RuntimeException("ERROR! tried to add seq: " + seq.getID() + " to AnnotatedSeqGroup: " +
				 this.getID() + ", but seq with same id is already in group");
    }
  }

  public void setDescription(String str) { description = str; }
  public String getDescription() { return description; }

  /** Not currently used, may want to move getVersion() to an AnnotatedGenome subclass */
  public String getVersion() { return version; }
  /** Not currently used, may want to move getOrganism() to an AnnotatedGenome subclass */
  public String getOrganism() { return organism; }
  /** Not currently used, may want to move getVersionDate() to an AnnotatedGenome subclass */
  public Date getVersionDate() { return version_date; }

}
