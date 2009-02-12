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

/**
 * all the things that can be done with something
 * that identifies something else.
 *
 * <p> The interface is much like that of java.lang.String
 * with the addition of TypedI.
 */
public interface IdentifyingI extends TypedI {

	public char charAt(int index);

	public int compareTo(String anotherString);

	public boolean endsWith(String suffix);

	public boolean equals(Object anObject);

	public boolean equalsIgnoreCase(String anotherString);

	public boolean equalsIgnoreCase(IdentifyingI anotherId);

	public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin);

	public int indexOf(int ch);

	public int indexOf(int ch, int fromIndex);

	public int indexOf(String str);

	public int indexOf(String str, int fromIndex);

	public String intern();

	public int lastIndexOf(int ch);

	public int lastIndexOf(int ch, int fromIndex);

	public int lastIndexOf(String str);

	public int lastIndexOf(String str, int fromIndex);

	public int length();

	public boolean regionMatches(int toffset, String other, int ooffset, int len);

	public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len);

	public String replace(char oldChar, char newChar);

	public boolean startsWith(String prefix, int toffset);

	public String substring(int beginIndex);

	public String substring(int beginIndex, int endIndex) throws StringIndexOutOfBoundsException;

	public char[] toCharArray();

	public String toLowerCase();

	public String toString();

	public String toUpperCase();

	public String trim();

}
