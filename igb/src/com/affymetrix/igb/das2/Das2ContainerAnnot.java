/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

package com.affymetrix.igb.das2;

import com.affymetrix.igb.genometry.TypeContainerAnnot;

public class Das2ContainerAnnot extends TypeContainerAnnot {
  /**
   *  there is also a type field and getType() method inherited from TypeContainerAnnot
   *    setting up in constructor so that inherited type field is set to String representation of Das2Type
   */
  Das2Type das2_type;
  Das2Region region;

  public Das2ContainerAnnot(Das2Type dtype) {
    super(dtype.toString());
    das2_type = dtype;
  }

  public Das2Type getDas2Type() {
    return das2_type;
  }

  public Das2Region getRegion() { return region; }
}
