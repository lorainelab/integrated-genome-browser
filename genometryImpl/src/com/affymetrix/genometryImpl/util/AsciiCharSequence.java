/**
*   Copyright (c) 2007 Affymetrix, Inc.
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

import java.nio.charset.Charset;
import java.util.Arrays;

/** Holds ASCII data in a CharSequence that takes less memory than a String. */
public class AsciiCharSequence implements CharSequence {
  
  byte[] bytes;
  static final Charset charset = Charset.forName("ascii");
  
  /** Creates a new instance of AsciiCharSequence */
  public AsciiCharSequence(byte[] bytes) {
    this.bytes = bytes;
  }
  
  public AsciiCharSequence(String s) {
    this(s.getBytes(charset));
  }

  public int length() {
    return bytes.length;
  }

  public char charAt(int index) {
    byte b = bytes[index];
    return (char) b;
  }

  public CharSequence subSequence(int start, int end) {
    return new AsciiCharSequence(Arrays.copyOfRange(bytes, start, end));
  }
  
  public String toString() {
    return new String(bytes, charset);
  }
  
  /**
   * This will test converting a simple string containing a non-ascii character
   * into an ascii string.  The bad character will be replaced by '?'.
   */
  public static void main(String[] args) {
    
    String test = "The grand cañon is big!";
        
    AsciiCharSequence as = new AsciiCharSequence(test);
    System.out.println("Ascii String: " + as.toString());
    System.out.println("AsciiCharSequence length: " + as.length());
    System.out.println("AsciiCharSequence byte length: " + as.bytes.length);
  }
}
