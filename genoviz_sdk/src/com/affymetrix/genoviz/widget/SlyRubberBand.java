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

package com.affymetrix.genoviz.widget;

import com.affymetrix.genoviz.bioviews.RubberBand;
import java.awt.Component;
import java.awt.Rectangle;

/**
 * This rubberband will not exceed the visible bounds of it's component.
 */
public class SlyRubberBand extends RubberBand {

  public SlyRubberBand() {
    super();
  }

  public SlyRubberBand( Component c ) {
    super( c );
  }

  /**
   * adjust the rubberband
   * so that the moving corner is as close to the mouse pointer as it can be
   * without leaving the component.
   *
   * @param x horizontal position of the mouse pointer.
   * @param y vertical position of the mouse pointer.
   */
  public void stretch( int x, int y ) {
    drawXOR();
    Rectangle b = comp.getBounds();
    if ( xorigin <= x ) {
      pixelbox.x = xorigin;
      pixelbox.width = x - xorigin;
      if ( b.width <= ( pixelbox.x + pixelbox.width ) ) {
        pixelbox.width = b.width - pixelbox.x - 1;
      }
      forward = true;
    }
    else {
      pixelbox.x = Math.max( 0, x );
      pixelbox.width = xorigin - pixelbox.x;
      forward = false;
    }
    if ( yorigin <= y ) {
      pixelbox.y = yorigin;
      pixelbox.height = y - yorigin;
      if ( b.height <= ( pixelbox.y + pixelbox.height ) ) {
        pixelbox.height = b.height - pixelbox.y - 1;
      }
    }
    else {
      pixelbox.y = Math.max( 0, y );
      pixelbox.height = yorigin - pixelbox.y;
    }
    drawXOR();
  }

}
