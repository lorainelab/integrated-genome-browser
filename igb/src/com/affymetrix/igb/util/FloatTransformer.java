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

package com.affymetrix.igb.util;

/**
 *
 *  A simple interface for arbitrary transformation of float values.
 *  Primarily intended for transformation of GraphSym y values.
 *  Wanted to include method for inverting transform so can have a
 *     GraphSym transformed "in-place" via transform() calls, without using more memory,
 *     then untransformed via inverseTransform() calls
 *
 */
public interface FloatTransformer  {
  public float transform(float x);

  /**
   *  inverseTranform() should be optional
   * if isInvertible() == false, inverseTransform() should throw runtime exception?
   */
  public float inverseTransform(float x);
  public boolean isInvertible();
}
