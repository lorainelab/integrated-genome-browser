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

import com.affymetrix.genometry.seq.*;

import com.affymetrix.igb.util.SearchableCharIterator;

import com.affymetrix.igb.util.NibbleIterator;
import com.affymetrix.igb.util.RevCompNibbleIterator;

/**
 *  BioSeq that maintains its residues as "nibbles" in a byte array.
 *  <pre>
 *  A nibble is 4 bits --> therefore 2 bases / byte
 *     4-bit residue encoding maps to 16 possible IUPAC codes:
 *     [ A, C, G, T, N, M, R, W, S, Y, K, V, H, D, B, X ]
 *
 *  Also implements SearchableCharacterIterator to allow for regex matching
 *    without having to build a String out of the byte array (which would kind of
 *    defeat the purpose of saving memory...)
 *
 *  SearchableCharIterator is an interface that inherits from CharacterIterator and
 *    adds methods:
 *      getLength()
 *      indexOf(String, offset)
 *
 *  Nibbles are now maintained by an internal SearchableCharIterator (for now
 *     always either a NibbleIterator or a RevCompNibbleIterator), so NibbleBioSeq can
 *     just make pass-through calls for most SearchableCharIterator methods...
 * </pre>
 */
public class NibbleBioSeq extends SimpleCompAnnotBioSeq
  implements SearchableCharIterator, Versioned {

  String version;
  // length field inherited from SimpleCompositeBioseq (by way of SimpleCompAnnotBioSeq)
  SearchableCharIterator residues_provider;

  public NibbleBioSeq() { }

  public NibbleBioSeq(String seqid, String seqversion, int length) {
    super(seqid, length);
    this.version = seqversion;
  }

  public String getVersion() { return version; }
  public void setVersion(String str) { this.version = str; }

  public NibbleBioSeq getReverseComplement() {
    NibbleBioSeq revcomp = null;
    SearchableCharIterator iter = this.getResiduesProvider();
    if (iter instanceof NibbleIterator) {
      revcomp = new NibbleBioSeq(this.getID() + ":revcomp",
				 this.getVersion(), this.getLength());
      NibbleIterator forward_iter = (NibbleIterator)iter;
      RevCompNibbleIterator revcomp_iter = new RevCompNibbleIterator(forward_iter);
      revcomp.setResiduesProvider(revcomp_iter);
    }
    else {
      System.out.println("ABORTED: can't currently reverse complement a " +
			 "reverse-complemented NibbleBioSeq...");
    }
    return revcomp;
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


  /** Gets residues.
   *  @param fillchar  Character to use for missing residues;
   *     warning: this parameter is used only if {@link #getResiduesProvider()} is null.
   */
  public String getResidues(int start, int end, char fillchar) {
    // ACKKK!  CharacterIterator.substring() takes start, _length_ argument (as opposed to String.substring(),
    //    which takes, start, end (-1) argument
    //    return residues_provider.substring(start, end);
    String result = null;
    if (residues_provider == null)  {
      // fall back on SimpleCompAnnotSeq (which will try both residues var and composition to provide residues)
      //      result = super.getResidues(start, end, fillchar);
      result = super.getResidues(start, end, '-');
    }
    else {
      result = residues_provider.substring(start, end-start);
    }
    return result;
  }


  public boolean isComplete(int start, int end) {
    if (residues_provider != null) { return true; }
    else { return super.isComplete(start, end); }
  }

  /*
   *  BEGIN
   *  SearchableCharIterator implementation
   */
  public char charAt(int pos) {
    if (residues_provider == null) {
      return super.getResidues(pos, pos+1, '-').charAt(0);
    }
    else {
      return residues_provider.charAt(pos);
    }
  }

  public boolean isEnd(int pos) {
    return (pos >= length);
  }

  public String substring(int offset) {
    System.out.println("called NibbleBioSeq.substring(offset)");
    return substring(offset, getLength());
  }

  public String substring(int offset, int length) {
    if (residues_provider == null) {
      return super.getResidues(offset, offset+length+1);
    }
    else {
      return residues_provider.substring(offset, length);
    }
  }

  public int indexOf(String str, int fromIndex) {
    // TODO: this will fail if residues_provider is null, so may need to call inside try/catch clause
    return residues_provider.indexOf(str, fromIndex);
  }

  /*
   *  END
   *  CharacterIterator implementation
   */

  public static void main(String[] args) {
    String test_string = "ACTGAAACCCTTTGGGNNNATATGCGC";
    System.out.println("in:  " + test_string);
    System.out.println("length: " + test_string.length());
    byte[] test_array = NibbleIterator.stringToNibbles(test_string, 0, test_string.length());
    NibbleBioSeq nibseq = new NibbleBioSeq(null, null, test_string.length());
    NibbleIterator nibber = new NibbleIterator(test_array, test_string.length());
    nibseq.setResiduesProvider(nibber);
    String result_string = NibbleIterator.nibblesToString(test_array, 0, test_string.length());
    System.out.println("out: " + result_string);
    for (int i=0; i<test_string.length(); i++) {
      System.out.println("nib " + i + ": " + nibseq.charAt(i));
    }

  }

}



