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
 * is a String with some constraints on it's construction.
 * Since the java.lang.String object is final,
 * this class contains a String rather than extends String.
 * Unlike String, this class can be extended
 * to provide other specialized classes of Identifiers.
 */
public class Identifier implements IdentifyingI {

  protected String id = null;
  protected String type = null;

  /**
   * constructs an Identifier.
   *
   * @param theId must be at least one and fewer than 129 characters long.
   *              It must not start or end with white space.
   */
  public Identifier(String theId) {
    this.id = theId;
    // Or should this be done in getType?
    this.type = this.getClass().getName();
  }

  public char charAt(int index) {
    return this.id.charAt(index);
  }

  public int compareTo(String anotherString) {
    return this.id.compareTo(anotherString);
  }

  public boolean endsWith(String suffix) {
    return this.id.endsWith(suffix);
  }

  public boolean equals(Object anObject) {
    return this.id.equals(anObject);
  }

  public boolean equalsIgnoreCase(String anotherString) {
    return this.id.equalsIgnoreCase(anotherString);
  }

  public boolean equalsIgnoreCase(IdentifyingI anotherId) {
    return this.id.equals(anotherId);
  }

  public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
    this.id.getChars(srcBegin, srcEnd, dst, dstBegin);
  }

  public int indexOf(int ch) {
    return this.id.indexOf(ch);
  }

  public int indexOf(int ch, int fromIndex) {
    return this.id.indexOf(ch, fromIndex);
  }

  public int indexOf(String str) {
    return this.id.indexOf(str);
  }

  public int indexOf(String str, int fromIndex) {
    return this.id.indexOf(str, fromIndex);
  }

  public String intern() {
    return this.id.intern();
  }

  public int lastIndexOf(int ch) {
    return this.id.lastIndexOf(ch);
  }

  public int lastIndexOf(int ch, int fromIndex) {
    return this.id.lastIndexOf(ch, fromIndex);
  }

  public int lastIndexOf(String str) {
    return this.id.lastIndexOf(str);
  }

  public int lastIndexOf(String str, int fromIndex) {
    return this.id.lastIndexOf(str, fromIndex);
  }

  public int length() {
    return this.id.length();
  }

  public boolean regionMatches(int toffset, String other, int ooffset, int len) {
    return this.id.regionMatches(toffset, other, ooffset, len);
  }

  public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) {
    return this.id.regionMatches(ignoreCase, toffset, other, ooffset, len);
  }

  public String replace(char oldChar, char newChar) {
    return this.id.replace(oldChar, newChar);
  }

  public boolean startsWith(String prefix, int toffset) {
    return this.id.startsWith(prefix, toffset);
  }

  public String substring(int beginIndex) {
    return this.id.substring(beginIndex);
  }

  public String substring(int beginIndex, int endIndex) throws StringIndexOutOfBoundsException {
    return this.id.substring(beginIndex, endIndex);
  }

  public char[] toCharArray() {
    return this.toCharArray();
  }

  public String toLowerCase() {
    return this.id.toLowerCase();
  }

  public String toString() {
    return this.id.toString();
  }

  public String toUpperCase() {
    return this.id.toUpperCase();
  }

  public String trim() {
    return this.id.trim();
  }

  public String getType() {
    return this.type;
  }

}
