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

package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.util.SearchableCharIterator;
import com.affymetrix.igb.util.*;

/**
 * This is a replacement for org.apache.regexp.StringCharacterIterator, 
 * which is buggy (at least in Jakarta regex package version 1.1).
 *
 * StringCharacterIterator has a bug in substring(start, length) method
 * It passes through to substring(start, end) method in wrapped String, but 
 *    it _doesn't_ perform the necessary conversion from length to end as second argument
 *    (end = start + length)
 */
public final class ImprovedStringCharIter implements SearchableCharIterator {
  private final String src;

  public ImprovedStringCharIter(String src) {
    this.src = src;
  }

  public String substring(int offset, int length)  {
    // here is where the crucial bug is in StringCharacterIterator:
    // return src.substring(offset, length);
    return src.substring(offset, offset + length); 
  }

  public String substring(int offset) {
    return src.substring(offset);
  }

  public char charAt(int pos) {
    return src.charAt(pos);
  }

  public boolean isEnd(int pos) {
    return (pos >= src.length());
  }

  public int indexOf(String searchString, int offset) {
    return src.indexOf(searchString, offset);
  }
  
  public int getLength() {
    return src.length();
  }
}

