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
 * represents an ad hoc pseudosubclass of Identifier.
 *
 * <p> The "type" of identifier is specified in the constructor
 * and no where else.
 * So the type is immutable
 * over the life of each object of this class.
 * There is only one constructor
 * so that the assignment of a type is enforced.
 * The same goes for the value.
 */
public class AdHocIdentifier extends Identifier {

  protected String type;

  /**
   * @param theType must not be null,
   *                must contain at least one character,
   *                must not start or end with white space,
   *                and must be less than 129 characters long.
   *                no other checks are made.
   * @param theId a String value.
   */
  public AdHocIdentifier(String theType, String theId) {
    super(theId);
    if (null == theType) {
      throw new IllegalArgumentException("Need a type.");
    }
    int i = theType.length();
    if (i < 1) {
      throw new IllegalArgumentException("Type can not be empty.");
    }
    if (128 < i) {
      throw new IllegalArgumentException("Type must be less than 129 chars.");
    }
    if (theType.charAt(0) <= ' ' || theType.charAt(i-1) <= ' ') {
      throw new IllegalArgumentException(
        "Type must not begin or end with white space.");
    }
    this.type = theType;
  }

  public String getType() {
    return this.id;
  }

}
