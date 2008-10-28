/**
*   Copyright (c) 1998-2008 Affymetrix, Inc.
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

import com.affymetrix.genoviz.bioviews.GlyphI;
import java.awt.geom.Point2D;
import java.util.EventObject;
import java.util.List;

/**
 * An interface implemented by some events to incorporate widget info
 * into the event. This includes widget coordinates, 
 * and the glyphs positioned under the event.
 */
//TODO: Get rid of this interface.  Only ever used by NeoMouseEvent
// (In fact, we could get rid of NeoMouseEvent as well.)
public interface NeoCoordEventI {

  /**
   * get the <code>x</code> coordinate of the event, in
   * widget coordinate units <b>(not pixels)</b>.
   */
  public double getCoordX();

  /**
   * Get the <code>y</code> coordinate of the event, in
   * widget coordinate units <b>(not pixels)</b>.
   */
  public double getCoordY();

  /**
   * Get the coordinates of the event as a Point2D, in
   * widget coordinate units <b>(not pixels)</b>.
   */
  public Point2D.Double getPoint2D();

  /**
   * Get the original event that this NeoCoordEventI is based
   * on (usually a standard AWTEvent).
   */
  public EventObject getOriginalEvent();

  /**
   * @return the event type
   */
  public int getID();

  /**
   * @return a List of GlyphI's whose coord bounds contain the
   * coord location of the event.
   */
  public List<GlyphI> getItems();
}
