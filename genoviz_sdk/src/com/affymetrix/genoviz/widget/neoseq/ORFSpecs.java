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

package com.affymetrix.genoviz.widget.neoseq;

public class ORFSpecs implements Cloneable {
  protected int startLoc;
  protected int endLoc;

  /**
   * Create a new ORFSpecs object,
   * with the ORF starting and stopping at the specified locations.
   */
  public ORFSpecs (int start, int end) {
    this.startLoc = start;
    this.endLoc   = end;
  }

  public int  getStart ()          { return startLoc; }
  public void setStart (int start) { startLoc = start; }
  public int  getEnd   ()          { return endLoc;  }
  public void setEnd   (int end)   { endLoc = end; }

}
