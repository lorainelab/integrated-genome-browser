/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.datamodel;

import com.affymetrix.genoviz.util.Debug;

import java.lang.String;
import java.lang.StringBuffer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * a data model for an alignment of multiple sequences.
 * It may or may not have a consensus sequence.
 * Sequences (including the consensus) are named.
 * Sequences (including the consensus) are represented
 * by a string of characters,
 * one per residue.
 */
public class MultiSeqAlign {

  /**
   * represents a gap in a sequence.
   */
  public static final char GAP_CHAR = ' ';

  String consensus = null;
  String consensusName = null;
  Hashtable<String,AlignSequence> seqs = new Hashtable<String,AlignSequence>();

  /**
   * sets a consensus sequence
   *
   * @param theSequence a String representing the residues
   *        in the consensus sequence.
   *        gaps are represented by GAP_CHAR.
   */
  public void setConsensus(String theSequence) {
    this.consensus = new String(theSequence);
  }

  /**
   * @return a String of residues representing the consensus sequence.
   */
  public String getConsensus() {
    return new String(this.consensus);
  }

  /**
   * names the consensus sequence.
   *
   * @param theName is the name given to the consensus.
   */
  public void setConsensusName(String theName) {
    this.consensusName = new String(theName);
  }

  /**
   * @return name of the consensus sequence
   */
  public String getConsensusName() {
    return new String(this.consensusName);
  }

  /**
   * adds a consensus sequence to the alignment.
   *
   * @param theName names the consensus.
   * @param theSequence specifies the residues in the consensus.
   */
  public void addConsensus(String theName, String theSequence) {
    setConsensusName(theName);
    setConsensus(theSequence);
  }


  /**
   * adds a sequence to the alignment.
   *
   * @param theName names the sequence.
   * @param theSequence specifies the residues in the sequence.
   */
  public void addSequence(String theName, String theSequence) {
    AlignSequence s = new AlignSequence();
    s.setName(theName);
    s.setResidues(theSequence);
    this.seqs.put(theName, s);
  }

  public Sequence getSequence(String theName) {
    AlignSequence s = this.seqs.get(theName);
    return s;
  }

  /**
   * retrieves a named sequence
   *
   * @param theName the name of the sequence to retrieve.
   * @return the residues in the sequence.
   */
  public String getResidues(String theName) {
    AlignSequence s = this.seqs.get(theName);
    if (null != s) {
      StringBuffer sb = new StringBuffer();
      int i = 0;
      int start = s.getStart();
      while (i++ < start) {
        sb.append(' ');
      }
      sb.append(s.getResidues());

      i = s.getAlignEnd() - sb.length();
      while (i-- > 0) {
        sb.append(' ');
      }

      return new String(sb);
    } else {
      return null;
    }
  }

  /**
   * Enumerates the sequences in the alignment
   * not including the consensus.
   *
   * @return <code>java.lang.String</code>s
   *         representing the residues in each sequence.
   */
  public Enumeration<AlignSequence> sequences() {
    return seqs.elements();
  }

  /**
   * Enumerates the names of the sequences in the alignment
   * not including the consensus.
   *
   * @return the names of the sequences.
   * Each name is a <code>java.lang.String</code>.
   */
  public Enumeration<String> sequenceNames() {
    return seqs.keys();
  }

  /**
   * builds a consensus of the alignment using a very
   * simple scoring matrix, 1 for match, 0 for mismatch.
   *
   * @return a string representing a consenus of the
   * MultiSeqAlign.
   */
  public String buildAssembly() {

    Enumeration<AlignSequence> e = this.sequences();
    Vector<AlignSequence> v = new Vector<AlignSequence>();
    StringBuffer con = new StringBuffer();

    while (e.hasMoreElements()) {
      AlignSequence s = e.nextElement();
      v.addElement(s);
    }

    int length = v.elementAt(0).getAlignEnd();

    for (int i=0; i < length; i++) {
      char c = ' ';
      String possibles = new String();
      int best_score = 0;

      for (int j=0; j < v.size(); j++) {
        AlignSequence s = v.elementAt(j);
        char a = s.getResidue(i);
        if (i >= s.getStart() && i < (s.getStart() + s.getLength()) ) {
          if (possibles.indexOf(a) < 0) {
            possibles += a;
          }
        }
      }

      for (int j=0; j < possibles.length(); j++) {
        char a = possibles.charAt(j);
        int score  = 0;
        for (int k=0; k < v.size(); k++) {
          AlignSequence t = v.elementAt(k);
          if (i >= t.getStart() && i < (t.getStart() + t.getLength()) ) {
            char b = t.getResidue(i);
            score += score(a, b);
          }
        }

        if ( score > best_score ) {
          best_score = score;
          c = a;
        }
      }
      con.append(c);
    }

    return new String(con);
  }


  /**
   * builds a consensus of the alignment using a very
   * simple scoring matrix, 1 for match, 0 for mismatch.
   *
   * @return a string representing a consenus of the
   * MultiSeqAlign.
   */
  public String buildConsensus() {

    Enumeration<AlignSequence> e = this.sequences();
    Vector<AlignSequence> v = new Vector<AlignSequence>();
    StringBuffer con = new StringBuffer();

    while (e.hasMoreElements()) {
      AlignSequence s = e.nextElement();
      v.addElement(s);
    }

    int length = v.elementAt(0).getAlignEnd();

    for (int i=0; i < length; i++) {
      char c = ' ';
      String possibles = new String();
      int best_score = 0;

      for (int j=0; j < v.size(); j++) {
        AlignSequence s = v.elementAt(j);
        char a = s.getResidue(i);
        if (possibles.indexOf(a) < 0) {
          possibles += a;
        }
      }

      for (int j=0; j < possibles.length(); j++) {
        char a = possibles.charAt(j);
        int score  = 0;
        for (int k=0; k < v.size(); k++) {
          AlignSequence t = v.elementAt(k);
          char b = t.getResidue(i);
          score += score(a, b);
        }

        if ( score > best_score ) {
          best_score = score;
          c = a;
        }
      }
      con.append(c);
    }

    return new String(con);
  }


  protected static int score(char a, char b) {
    return ( Character.toUpperCase(a) == Character.toUpperCase(b) ) ? 1 : 0 ;
  }

  /**
   * @see java.lang.Object#toString
   */
  public String toString() {

    StringBuffer sb = new StringBuffer();
    if (null != this.consensusName) {
      sb.append(this.consensusName);
      sb.append(" (consensus)");
    }
    if (null != this.consensus) {
      if (null == this.consensusName) {
        sb.append("(consensus)");
      }
      sb.append(": ");
      sb.append(this.consensus);
    }
    Enumeration it = seqs.keys();
    if (null != it)
    while (it.hasMoreElements()) {
      Object o = it.nextElement();
      if (null != o)
      sb.append("\n");
      sb.append(o.toString());
      sb.append(": ");
      sb.append(seqs.get(o));
    }
    return sb.toString();
  }

}
