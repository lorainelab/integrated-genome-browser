/**
*   Copyright (c) 2008 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.genoviz.bioviews;

/**
 * Interface to transform a single value
 * in a one-dimensional space
 * to another two-dimensional space.
 */
public interface OneDimTransform extends Cloneable  {

  /**
   * Transforms a single value.
   *
   * @param in the coordinate value to be transformed
   *
   * @return the transformed value
   */
  public double transform(double in);

  /**
   * Inverts the transform of a single value.
   *
   * @param in the coordinate value to be transformed
   *
   * @return the transformed value
   */
  public double inverseTransform(double in);

  /**
   * Creates a clone of the TransormI.
   *
   * @return the new clone.
   */
  public OneDimTransform clone() throws CloneNotSupportedException;

}
