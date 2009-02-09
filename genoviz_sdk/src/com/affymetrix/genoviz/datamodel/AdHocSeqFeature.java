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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * represents a feature of a biological sequence.
 *
 * <p> We anticipate developing specialized classes
 * for many types of features.
 * This class is for other features
 * so that we can handle new, unknown features
 * on an ad hoc basis.
 *
 * <p> The "type" of feature is specified in the constructor
 * and no where else.
 * So the type is immutable
 * over the life of each object of this class.
 * There is only one constructor
 * so that the assignment of a type is enforced.
 */
public class AdHocSeqFeature implements SeqFeatureI {

  protected String type;
  /** each a Range. */
  protected Vector<Range> pieces;
  /** each a name/value pair of Strings. */
  protected Hashtable<String,Object> attributes;

  /**
   * constructs a feature with a type,
   * but no pieces or attributes
   * (so far).
   */
  public AdHocSeqFeature(String theType) {
    if (null == theType) {
      throw new IllegalArgumentException("Need a type.");
    }
    int i = theType.length();
    if (i < 1) {
      throw new IllegalArgumentException("Type can not be empty.");
    }
    if (128 < i) {
      throw new IllegalArgumentException("Type is too long.");
    }
    if (theType.charAt(0) <= ' ' || theType.charAt(i-1) <= ' ') {
      throw new IllegalArgumentException(
        "Type must not begin or end with white space.");
    }
    this.setType(theType);
    pieces = new Vector<Range>();
    attributes = new Hashtable<String,Object>();
  }

  /**
   * @return a string representing the feature and all it's attributes.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer(type);
    Enumeration it;
    it = pieces.elements();
    while (it.hasMoreElements()) {
      Object o = it.nextElement();
      if (o instanceof Range) {
        Range r = (Range)o;
        sb.append("\n" + r);
      }
      else {
        sb.append(o.toString());
      }
    }
    it = attributes.keys();
    while (it.hasMoreElements()) {
      Object o = it.nextElement();
      Object value = attributes.get(o);
      if (value instanceof Vector) {
        Enumeration values = ((Vector)value).elements();
        while (values.hasMoreElements()) {
          Object v = values.nextElement();
          sb.append("\n" + o + ": " + v);
        }
      }
      else {
        sb.append("\n" + o + ": " + value);
      }
    }
    return sb.toString();
  }

  /**
   * sets the type.
   */
  public void setType(String theType) {
    this.type = theType;
  }
  /**
   * gets the type.
   *
   * @return the type of feature.
   */
  public String getType() {
    return this.type;
  }

  public void addPiece(Range thePiece) {
    pieces.addElement(thePiece);
  }

  /**
   * can be used to iterate over all the pieces of this feature.
   *
   * @return an enumeration of the pieces.
   */
  public Enumeration pieces() {
    return this.pieces.elements();
  }

  /**
   * associates a named attribute with this feature.
   *
   * @param theAttribute an array of two strings.
   *                     theAttribute[0] is the name.
   *                     theAttribute[1] is the value.
   */
  public void addAttribute(String[] theAttribute) {
    if (2 != theAttribute.length) {
      throw new IllegalArgumentException(
        "attribute must be a name/value pair");
    }
    addAttribute(theAttribute[0], theAttribute[1]);
  }

  /**
   * associates a named attribute with this feature.
   *
   * @param theName a name for the attribute.
   * @param theValue a value for the attribute.
   */
  public void addAttribute(String theName, String theValue) {
    Object oldValue = attributes.get(theName);
    if (null == oldValue) {
      attributes.put(theName, theValue);
      return;
    }
    if (oldValue instanceof Vector) {
      ((Vector)oldValue).addElement(theValue);
      return;
    }
    Vector v = new Vector();
    v.addElement(oldValue);
    v.addElement(theValue);
    attributes.put(theName, v);
  }
  /**
   * replaces one named attribute with another.
   *
   * @param theName names the attribute being replaced.
   * @param theValue gives the new value.
   */
  public void replaceAttribute(String theName, String theValue) {
    attributes.put(theName, theValue);
  }

  /**
   * gets a named attribute of this feature.
   *
   * @param theName names the attribute requested.
   * @return the value of the named attribute.
   */
  public Object getAttribute(String theName) {
    return attributes.get(theName);
  }

  /**
   * can be used to iterate over all the attributes of this feature.
   * It returns the keys (names) of the attributes.
   * You would then call getAttribute with each key enumerated.
   *
   * @return an enumeration of the names of the attributes
   *         of this feature.
   */
  public Enumeration attributes() {
    return attributes.keys();
  }

}
