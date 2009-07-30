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

package com.affymetrix.igb.glyph;

import java.awt.event.*;
import java.util.*;
import java.util.List;

import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.bioviews.*;

/**
 *  A subclass of com.affymetrix.genoviz.bioviews.RubberBand that knows about NeoMaps, and 
 *     can be set up so that rubberbanding will only occur if the mouse drag is started 
 *     where no glyphs are hit.
 */
public final class SmartRubberBand extends com.affymetrix.genoviz.bioviews.RubberBand {
  NeoMap nmap;

  public SmartRubberBand(NeoMap nmap) {
    super(nmap.getNeoCanvas());
    this.nmap = nmap;
  }

  /**
   * Overrides to only pass event to 
   *   RubberBand.heardEvent() if mouse press did not occur over a hitable glyph.
   */
  public void mousePressed(MouseEvent e) { 
    List hit_glyphs = nmap.getItemsByPixel(e.getX(), e.getY());
    if (hit_glyphs == null || hit_glyphs.isEmpty()) {
      heardEvent(e); 
    }
  }

}
