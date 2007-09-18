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

/**
 *  Extends CharIterator to include indexOf function.
 *
 *  to clean up code in SeqSearchView, OrfAnalyzer, etc., need an interface that 
 *  extends CharacterIterator (so regex stuff, SeqCharGlyph, etc. can use it), but 
 *  that also includes an indexOf() method (so SeqSearchView, etc. code can just 
 *  wrap a String with a SearchableCharIterator and not have to have lots of 
 *  conditionals in code... (and can cast objects to SearchableCharIterator rather 
 *  than more specific class [such as NibbleBioSeq])
 */
public interface SearchableCharIterator extends CharIterator {
  public int indexOf(String searchstring, int offset);
  public int getLength();
}

