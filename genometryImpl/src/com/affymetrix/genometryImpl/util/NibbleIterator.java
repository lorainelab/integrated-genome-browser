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

package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometry.util.DNAUtils;

public class NibbleIterator implements SearchableCharIterator {
  int length;
  byte[] nibble_array;

  public NibbleIterator(byte[] nibs, int len) {
    this.length = len;
    this.nibble_array = nibs;
  }

  // really should do this more elegantly, so each bit in nibble represents boolean
  //   for whether a particular base is present --
  //       base:   A C G T
  //        bit:   1 2 3 4
  //   so for example, nibble 0101 binary (= 5 decimal) would be (A or T)...
  /*
 static final char[] nibble2char = {
                                      '-', // 0000 ==> no bases (gap?)
                                      'A', // 0001 ==> A
                                      'C', // 0010 ==> C
                                      // 0011 ==> C|A
                                      'G', // 0100 ==> G
                                      // 0101 ==> G|A
                                      // 0110 ==> G|C
                                      // 0111 ==> G|C|A
                                      'T', // 1000 ==> T
                                      // 1001 ==> T|A
                                      // 1010 ==> T|C
                                      // 1011 ==> T|C|A
                                      // 1100 ==> T|G
                                      // 1101 ==> T|G|A
                                      // 1110 ==> T|G|C
                                      'N', // 1111 ==> T|G|C|A
  */
  // or possibly rig it to reverse complements of a single base is just the
  //    bitwise NOT of the base ( revcomp(nibble) = ^ nibble ) :
  /*
  static final char[] nibble2char = {
                                      // 0000
                                      'A', // 0001 ==> A
                                      // 0010 ==> C
                                      // 0011 ==>
                                      // 0100 ==>
                                      // 0101 ==>
                                      // 0110 ==>
                                      // 0111 ==>
                                      // 1000 ==>
                                      // 1001 ==>
                                      // 1010 ==>
                                      // 1011 ==>
                                      // 1100 ==>
                                      // 1101 ==> G
                                      // 1110 ==> T
                                      // 1111 ==>
   */

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

    char2nibble['a'] = 0;
    char2nibble['c'] = 1;
    char2nibble['g'] = 2;
    char2nibble['t'] = 3;
    char2nibble['n'] = 4;
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
  static final int offsets[] = {4, 0};
  //  static final byte filters[] = { hifilter, lofilter };
  static final byte filters[] = { lofilter, hifilter };
  static final int one_mask = 1;


  /**
   *  BEGIN
   *  CharacterIterator implementation
   */
  public char charAt(int pos) {
    //TODO
    /*
     *  BLECH!!!
     *  Very strange problem with _one_ position, pos = 30927828
     *  MUST FIX SOON
     */
    //    if (pos == 30927828 || pos == 13698664) {
    if (pos == 30927828)  {
      // System.out.println("hit weird NibbleIterator problem with 30927828");
      // return 'N';
    }

    int index = pos & one_mask;  // either 0 or 1, index into offsets and filters arrays
    int offset = offsets[index];
    byte filter = filters[index];
    byte by = nibble_array[pos >> 1];
    int n2c_index = ((by & filter) >> offset);

    char nib;
    try  {
      //    char nib = nibble2char[(by & filter) >> offset];
      nib = nibble2char[(by & filter) >> offset];
    }
    catch (Exception ex)  {
      System.out.println("!!!! problem: base pos = " + pos + ", nibble2char index = " + n2c_index);
      ex.printStackTrace();
      nib = 'N';
    }
    return nib;
  }

  public boolean isEnd(int pos) {
    //    return (pos >= nibble_array.length);
    return (pos >= length);
  }

  public String substring(int offset) {
    System.out.println("called NibbleIterator.substring(offset)");
    return substring(offset, this.length);
  }

  public String substring(int offset, int length) {
    //    System.out.println("called NibbleIterator.substring(offset = " + offset + ", length = " + length + ")");
    return NibbleIterator.nibblesToString(nibble_array, offset, offset+length);
  }

  public int getLength() {
    return length;
  }

  public int indexOf(String str, int fromIndex) {

    //            char v1[] = value;
    //            char v2[] = str.value;
    char querychars[] = str.toCharArray();
    //            int max = offset + (count - str.count);
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

    //            int strOffset = str.offset;
    int strOffset = 0;
    char first  = querychars[strOffset];
    //        int i = offset + fromIndex;
    int i = fromIndex;

  startSearchForFirstChar:
    while (true) {

      /* Look for first character. */
      //            while (i <= max && v1[i] != first) {
      while (i <= max && this.charAt(i) != first) {
        i++;
      }
      if (i > max) {
        return -1;
      }

      /* Found first character, now look at the rest of querychars */
      int j = i + 1;
      //            int end = j + str.count - 1;
      int end = j + str.length() - 1;
      int k = strOffset + 1;
      while (j < end) {
        //                if (v1[j++] != querychars[k++]) {
        if (this.charAt(j++) != querychars[k++]) {
          i++;
          /* Look for str's first char again. */
          continue startSearchForFirstChar;
        }
      }
      //            return i - offset;        /* Found whole string. */
      return i;
    }
  }

  public static byte[] stringToNibbles(String str, int start, int end) {
    if (start <= end) {
      int length = end - start;
      int extra_nibble = length & one_mask;
      //      System.out.println("extra nibble: " + extra_nibble);
      //      length = length / 2;
      byte[] nibbles = new byte[(length/2) + extra_nibble];

      //      for (int i=start; i<end; i++) {
      //      for (int i=0; i<length-1; i++) {
      //      for (int i=0; i<length-1; i++) {
      int max;
      if (extra_nibble > 0) { max = length - 1; }
      else { max = length; }
      for (int i=0; i<length-1; i++) {
        int byte_index = i >> 1;
        char ch1 = str.charAt(i + start);
        i++;
        char ch2 = str.charAt(i + start);

        byte hinib = char2nibble[ch1];
        byte lonib = char2nibble[ch2];
        byte two_nibbles = (byte)((hinib << 4) +  lonib);
        //        System.out.println("" + hinib + ", " + lonib + ", " + two_nibbles);
        //        byte two_nibbles = (byte)((hinib << 4) | lonib);
        //        byte two_nibbles = nibbles2bytes[hinib][lonib];
        nibbles[byte_index] = two_nibbles;
      }
      if (extra_nibble > 0) {
        int byte_index = (length-1) >> 1;
        char ch1 = str.charAt(length-1 + start);
        //        i++;
        //        char ch2 = str.charAt(i + start);

        byte hinib = char2nibble[ch1];
        //        byte lonib = char2nibble[ch2];
        //        byte two_nibbles = (byte)((hinib << 4) +  lonib);
        byte singlet_nibble = (byte)(hinib << 4);
        //        System.out.println("" + hinib + ", no lonib, " + singlet_nibble);
        //        byte two_nibbles = (byte)((hinib << 4) | lonib);
        //        byte two_nibbles = nibbles2bytes[hinib][lonib];
        nibbles[byte_index] = singlet_nibble;
      }
      return nibbles;
    }
    else {
      System.out.println("in NibbleIterator.stringToNibbles(), " +
                         "start < end NOT YET IMPLEMENTED");
    }
    return null;
  }

  public static String nibblesToString(byte[] nibbles, int start, int end) {
    String residues = null;
    boolean forward = (start <= end);
    int min = Math.min(start, end);
    int max = Math.max(start, end);
    StringBuffer buf = new StringBuffer(max - min);
    for (int i = min; i < max; i++) {
      int index = i & one_mask;  // either 0 or 1, index into offsets and filters arrays
      int offset = offsets[index];
      byte filter = filters[index];
      byte by = nibbles[i >> 1];
      char nib = nibble2char[(by & filter) >> offset];
      buf.append(nib);
    }
    residues = buf.toString();
    if (! forward) {
      residues = DNAUtils.reverseComplement(residues);
    }
    return residues;
  }

  public static void main(String[] args) {
    String test_string = "ACTGAAACCCTTTGGGNNNATATGCGCgatcattattcggcgg";
    System.out.println("in:  " + test_string);
    System.out.println("length: " + test_string.length());
    byte[] test_array = NibbleIterator.stringToNibbles(test_string, 0, test_string.length());
    //    NibbleIterator nibseq = new NibbleIterator(null, null, test_string.length());
    //    nibseq.setResidueNibbles(test_array);
    String result_string = NibbleIterator.nibblesToString(test_array, 0, test_string.length());
    System.out.println("out: " + result_string);
    for (int i=0; i<test_string.length(); i++) {
      //      System.out.println("nib " + i + ": " + nibseq.charAt(i));
      System.out.println("nib " + i + ": " + result_string.charAt(i));
    }
  }

}
