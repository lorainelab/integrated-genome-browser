/**
*   Copyright (c) 1998-2007 Affymetrix, Inc.
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

package com.affymetrix.genoviz.event;

import com.affymetrix.genoviz.util.NeoConstants;
import java.util.EventObject;

public class NeoDragEvent extends EventObject {
  static final long serialVersionUID = 1L;

  protected NeoConstants.Direction direction;

  /**
   * @param source - thing being dragged.
   * @param direction - can also be NONE
   */
  public NeoDragEvent(Object source, NeoConstants.Direction direction) {
    super(source);
    this.direction = direction;
  }

  public NeoConstants.Direction getDirection() {
    return direction;
  }

}
