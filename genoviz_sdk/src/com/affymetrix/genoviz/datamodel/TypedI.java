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
 * used to allow ad hoc typing of some objects.
 * <p>
 * It can also be used for those who prefer
 * <pre>
 *     String type = o.getType();
 * </pre>
 * to
 * <pre>
 *     String type = o.getClass().getName();
 * </pre>
 * <p>
 * A class implementing this interface will typically have a constructor
 * which takes a type encoded in a <code>String</code>.
 * It would be this type string that is returned by <code>getType()</code>.
 * If there is no other constructor
 * and no other method for setting the type,
 * then this makes for a more rigorous new ad hoc subclass
 * of some other class.
 * It would probably be better to create a new class for each type.
 * However, this would require vigilence in finding out new types
 * (of whatever object is being modeled)
 * being used publicly and adding them as they appear.
 */
public interface TypedI {

  /**
   * gets the "type" of an object.
   */
  public String getType();

}
