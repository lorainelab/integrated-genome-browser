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
 * represents a feature on a sequence of amino acids.
 * <br>So far, this is exactly the same as an <code>AdHocSeqFeature</code>.
 */
public class AASeqFeature extends AdHocSeqFeature {

  public AASeqFeature(String theType) {
    super(theType);
  }

}
