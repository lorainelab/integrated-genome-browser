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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/** Holds ASCII data in a CharSequence that takes less memory than a String. */
public class AsciiCharSequence implements CharSequence {
  
  byte[] bytes;
  static final Charset charset = Charset.forName("ascii");
  
  /** Creates a new instance of AsciiCharSequence */
  public AsciiCharSequence(byte[] bytes) {
    this.bytes = bytes;
  }
  
  public AsciiCharSequence(String s) {
    this(getAsciiBytes(s));
  }

  static byte[] getAsciiBytes(String s) {
    byte[] bytes;
    try {
      bytes = s.getBytes(charset.name());
    } catch (UnsupportedEncodingException ex) {
      ex.printStackTrace();
      bytes = s.getBytes();
    }
    return bytes;
  }
  
  public int length() {
    return bytes.length;
  }

  public char charAt(int index) {
    byte b = bytes[index];
    return (char) b;
  }

  public CharSequence subSequence(int from, int to) {
    int newLength = to - from;
    if (newLength < 0) {
      throw new IllegalArgumentException(from + " > " + to);
    }
    byte[] copy = new byte[newLength];
    System.arraycopy(bytes, from, copy, 0,
                     Math.min(bytes.length - from, newLength));
    return new AsciiCharSequence(copy);
  }
  
  @Override
  public String toString() {
    return new String(bytes); // should be new String(bytes,charset) for jdk1.6
  }
}
