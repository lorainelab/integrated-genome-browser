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

package com.affymetrix.igb.util;

import com.affymetrix.igb.util.NibbleIterator;

public class RevCompNibbleIterator 
  implements SearchableCharIterator {
  int length;
  byte[] nibble_array;
  // shift indicates whether first residue of reverse complement maps to:
  //   false  ==> "last" nibble (low 4 bits of last byte in nibble_array) 
  //   true   ==> "next to last" nibble (hi 4 bits of last byte in nibble_array)
  //  int shift;
  boolean shift;

  public RevCompNibbleIterator(NibbleIterator forward_iter) {
    this(forward_iter.nibble_array, forward_iter.length);
  }

  public RevCompNibbleIterator(byte[] nibs, int len) {
    this.length = len;
    this.nibble_array = nibs;
    //    shift = length % 2;
    shift = ((length % 2) != 0);
    System.out.println("constructring RevCompNibbleIterator, length = " + length + 
		       ", array length = " + nibble_array.length + ", shift = " + shift);
  }

  // maps complementary
  static final char[] char_comp = new char[256];
  static {
    char_comp['A'] = 'T';
    char_comp['C'] = 'G';
    char_comp['G'] = 'C';
    char_comp['T'] = 'A';
    char_comp['N'] = 'N';
  }

  static final char[] nibble2char = { 'A', 'C', 'G', 'T', 
				      'N', 'M', 'R', 'W',  
				      'S', 'Y', 'K', 'V', 
				      'H', 'D', 'B', 'U' };

  static final byte[] char2nibble= new byte[256];

  static {

    char2nibble['A'] = 0;
    char2nibble['C'] = 1;
    char2nibble['G'] = 2;
    char2nibble['T'] = 3;
    char2nibble['N'] = 4;
    char2nibble['M'] = 5;
    char2nibble['R'] = 6;
    char2nibble['W'] = 7;
    char2nibble['S'] = 8;
    char2nibble['Y'] = 9;
    char2nibble['K'] = 10;
    char2nibble['V'] = 11;
    char2nibble['H'] = 12;
    char2nibble['D'] = 13;
    char2nibble['B'] = 14;
    char2nibble['U'] = 15;

    char2nibble['a'] = 1;
    char2nibble['c'] = 2;
    char2nibble['g'] = 3;
    char2nibble['t'] = 4;
    char2nibble['n'] = 5;
    char2nibble['m'] = 5;
    char2nibble['r'] = 6;
    char2nibble['w'] = 7;
    char2nibble['s'] = 8;
    char2nibble['y'] = 9;
    char2nibble['k'] = 10;
    char2nibble['v'] = 11;
    char2nibble['h'] = 12;
    char2nibble['d'] = 13;
    char2nibble['b'] = 14;
    char2nibble['u'] = 15;
  }

  
  // 127 -->   0111 1111
  // -128 -->  1111 1111
  // 15   -->  0000 1111
  // -16  -->  1111 0000   --> (-128) + 64 + 32 + 16

  // & with hifilter to filter out 4 hi bits (only lo bits retained)
  static final byte hifilter = 15;  
  // & with lofilter to filter out 4 lo bits (only hi bits retained)
  static final byte lofilter = -16; 

  // number of bits to shift when converting hinibble and lonibble
  //   (4 bits for hinibble, 0 bits for lonibble)
  //  static final int offsets[] = {4, 0};
  //  static final byte filters[] = { hifilter, lofilter };
  //  static final byte filters[] = { lofilter, hifilter };
  static final int one_mask = 1;

  // finds char at this position in _reverse complement_ of sequence 
  //    corresponding to nibble_array
  //
  // so if conceptually:
  //   SeqA = forward BioSeq 
  //   byte[] nibs = SeqA residues converted to a nibble array
  //   SeqB = BioSeq that is reverse complement of SeqA
  //   RevCompNibbleIterator rcnib_iter = RevCompNibbleIterator(nibs, SeqB.length())
  // 
  // then SeqB.getResidues().charAt(i) == rcnib_iter.charAt(i)
  // 
  public char charAt(int pos) { 
    if (pos == 13698664) {
      System.out.println("hit weird RevCompNibbleIterator problem with pos == 13698664");
      return 'N';
    }
    int rev_base_pos = length - pos - 1;  // need -1 so revpos(pos=0) ==> length - 1..

    int byte_array_pos = rev_base_pos >> 1;
    byte by = nibble_array[byte_array_pos];
    
    int nibble;
    try  {
      if (shift) {
	boolean get_lobits = ((pos & one_mask) == 1);
	if (get_lobits) { nibble = (by & hifilter); }
	else { nibble = (by & lofilter) >> 4; }
	char rev = nibble2char[nibble];
	char revcomp = char_comp[rev];
	return revcomp;
      }
      else {
	boolean get_lobits = ((pos & one_mask) == 0);
	if (get_lobits) { nibble = (by & hifilter); }
	else { nibble = (by & lofilter) >> 4; }
	char rev = nibble2char[nibble];
	char revcomp = char_comp[rev];
	return revcomp;
      }
    }

    catch (Exception ex)  {
	System.out.println("!!!! problem: base pos = " + pos);
	ex.printStackTrace();
	char revcomp = 'N';
	return revcomp;
    } 
  }

  public char compCharAt(int pos) {
    //    int rev_base_pos = length - pos - 1;  // need -1 so revpos(pos=0) ==> length - 1..
    //    int byte_array_pos = rev_base_pos >> 1;
    int byte_array_pos = pos >> 1;
    byte by = nibble_array[byte_array_pos];
    boolean get_lobits = ((pos & one_mask) == 1);
    int nibble;
    if (get_lobits) { nibble = (by & hifilter); }
    else { nibble = (by & lofilter) >> 4; }
    char ch = nibble2char[nibble];
    char comp = char_comp[ch];
    return comp;
  }

  public char revCharAt(int pos) {
    int rev_base_pos = length - pos - 1;  // need -1 so revpos(pos=0) ==> length - 1..
    int byte_array_pos = rev_base_pos >> 1;
    byte by = nibble_array[byte_array_pos];
    
    int nibble;
    if (shift) {
      boolean get_lobits = ((pos & one_mask) == 1);
      if (get_lobits) { nibble = (by & hifilter); }
      else { nibble = (by & lofilter) >> 4; }
      char rev = nibble2char[nibble];
      //      char revcomp = char_comp[rev];
      //      return revcomp;
      return rev;
    }
    else {
      boolean get_lobits = ((pos & one_mask) == 0);
      if (get_lobits) { nibble = (by & hifilter); }
      else { nibble = (by & lofilter) >> 4; }
      char rev = nibble2char[nibble];
      //      char revcomp = char_comp[rev];
      //      return revcomp;
      return rev;
    } 
  }


  /**
   *  return substring for _reverse complement_ of nibble_array residues...
   */
  public String substring(int start, int len) { 
    int end = start + len;
    StringBuffer buf = new StringBuffer(len);
    for (int i=start; i<end; i++)  {
      //      System.out.println("getting char at: " + i);
      char ch = this.charAt(i);
      buf.append(ch);
    }
    return buf.toString();
  }

  public String substring(int start) {
    return substring(start, this.length);
  }

  /*
   * identical to NibbleIterator.indexOf() 
   * (revcomp conversions all happen in charAt() calls)
   */
  public int indexOf(String str, int fromIndex) {
    
    //    	char v1[] = value;
    //    	char v2[] = str.value;
    char querychars[] = str.toCharArray();
    //    	int max = offset + (count - str.count);
    int max = length - str.length();
    if (fromIndex >= length) {
      if (length == 0 && fromIndex == 0 && str.length() == 0) {
	/* There is an empty string at index 0 in an empty string. */
	return 0;
      }
      /* Note: fromIndex might be near -1>>>1 */
      return -1;
    }
    if (fromIndex < 0) {
      fromIndex = 0;
    }
    if (str.length() == 0) {
      return fromIndex;
    }

    //    	int strOffset = str.offset;
    int strOffset = 0;
    char first  = querychars[strOffset];
    //        int i = offset + fromIndex;
    int i = fromIndex;

  startSearchForFirstChar:
    while (true) {

      /* Look for first character. */
      //	    while (i <= max && v1[i] != first) {
      while (i <= max && this.charAt(i) != first) {
	i++;
      }
      if (i > max) {
	return -1;
      }

      /* Found first character, now look at the rest of querychars */
      int j = i + 1;
      //	    int end = j + str.count - 1;
      int end = j + str.length() - 1;
      int k = strOffset + 1;
      while (j < end) {
	//		if (v1[j++] != querychars[k++]) {
	if (this.charAt(j++) != querychars[k++]) {
	  i++;
	  /* Look for str's first char again. */
	  continue startSearchForFirstChar;
	}
      }
      //	    return i - offset;	/* Found whole string. */
      return i;
    }
  }

  public boolean isEnd(int pos) {
    return (pos >= length);
  }

  public int getLength() {
    return length;
  }

  public static void main(String[] args) {
    String test_string = "ACTGAAACCCTTTGGGNNNATATGC";
    byte[] test_array = NibbleIterator.stringToNibbles(test_string, 0, test_string.length());
    NibbleIterator nibs = new NibbleIterator(test_array, test_string.length());
    RevCompNibbleIterator rcnibs = new RevCompNibbleIterator(test_array, test_string.length());

    System.out.println("length: " + test_string.length());
    System.out.println("in:   " + test_string);
    System.out.println("nib:  " + nibs.substring(0, test_string.length()));

    /*
    StringBuffer compbuf = new StringBuffer();
    for (int i=0; i<test_string.length(); i++) {
      compbuf.append(rcnibs.compCharAt(i));
    }
    System.out.println("comp: " + compbuf.toString());

    StringBuffer revbuf = new StringBuffer();
    for (int i=0; i<test_string.length(); i++) {
      revbuf.append(rcnibs.revCharAt(i));
    }
    System.out.println("rev:  " + revbuf.toString());
    */

    /*
    StringBuffer rcbuf = new StringBuffer();
    for (int i=0; i<test_string.length(); i++) {
      rcbuf.append(rcnibs.charAt(i));
    }
    */
    String rcstr = rcnibs.substring(0, rcnibs.getLength());
    System.out.println("rc:   " + rcstr);
    System.out.println("index of TTT in forward: " + nibs.indexOf("TTT", 0));
    System.out.println("index of TTT in revcomp: " + rcnibs.indexOf("TTT", 0));
  }


}


