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
 *   Serves the same purpose as org.apache.regex.CharacterIterator,
 *   but we use our own implementations, since the apache ones were buggy.
 *   Also similar to the CharSequence interface, but returns Strings instead
 *   of generic CharSequence's.
 */
public interface CharIterator {

	public char charAt(int pos);

	public boolean isEnd(int pos);

	public String substring(int offset);

	public String substring(int offset, int length);
}
