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

package com.affymetrix.igb.view;

import java.awt.event.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.awt.Font;

public class UnibrowHairline {

  public static String PREF_KEEP_HAIRLINE_IN_VIEW = "Keep hairline in view";
  public static boolean default_keep_hairline_in_view = true;
    
  static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

  // It is common practice to use a VisibleRange with the Shadow as
  // a NeoRangeListener on it, but that seems overly complex for this case.
  //private VisibleRange visible_range;

  private Shadow hairline;
  private NeoMap map;
  //private MouseListener mouse_listener;

  // The NeoViewBoxListener makes sure that the hairline will
  // stay inside the borders of the map, no matter where the user
  // scrolls or zooms the map.
  // (We could use a NeoRangeListener on the NeoMap instead of
  // a pre-draw NeoViewBoxListener on the View.)
  private NeoViewBoxListener pre_draw_listener;

  // the current location of the hairline
  private double focus = 1;

  private boolean keep_hairline_in_view = true;

  public UnibrowHairline(NeoMap the_map) {
    if (the_map == null) throw new IllegalArgumentException();

    map = the_map;
    //visible_range = new VisibleRange();

    // Move the hairline when the user clicks with mouse button #1
/*
    mouse_listener = new MouseAdapter() {
      public void mouseReleased( MouseEvent e ) {
        if (0 != (e.getModifiers() & MouseEvent.BUTTON1_MASK)) {
          setSpot(((NeoMouseEvent)e).getCoordX());
        }
      }
    };
*/

    pre_draw_listener = new NeoViewBoxListener() {
      public void viewBoxChanged( NeoViewBoxChangeEvent e ) {
        if (keep_hairline_in_view == true) {
          double start = e.getCoordBox().x;
          double end = e.getCoordBox().width + start;
          if (focus < start) {setSpot(start);}
          else if (focus > end) {setSpot(end);}
        }
      }
    };

    hairline = new Shadow(map);
    hairline.setUseXOR(true);
    hairline.setSelectable(false);
    hairline.setLabeled(false);
    // hairline.setFont(DEFAULT_FONT);
    setKeepHairlineInView(UnibrowPrefsUtil.getBooleanParam(PREF_KEEP_HAIRLINE_IN_VIEW, default_keep_hairline_in_view));

    //map.addMouseListener( mouse_listener );
    //visible_range.addListener( hairline );
    map.getView().addPreDrawViewListener(pre_draw_listener);
  }

  /** Sets the flag determining whether the hairline is constrained
   *  to remain inside the visible map boundaries.
   *  If b is null, the current value of the flag is not changed.
   */
  public void setKeepHairlineInView(boolean b) {
    keep_hairline_in_view = b;
  }

  /** Sets the location of the hairline.  This is the only supported
   *  way to move the hairline.  Does *NOT* call map.updateWidget() and
   *  but you will probably want to do that after calling this method.
   */
  public void setSpot(double spot) {
    focus = spot;
    //visible_range.setSpot(focus);
    // instead of using the visible_range, directly call hairline.setRange()
    hairline.setRange((int) focus, (int) focus + 1);
    map.setZoomBehavior(map.X, map.CONSTRAIN_COORD, focus);
    //map.updateWidget();
  }

  /** Returns the current location of the hairline. */
  public double getSpot() {
    return focus;
  }

  /** Returns the actual shadow glyph. */
  public Shadow getShadow() {
    return hairline;
  }

  /** Call this method to get rid of circular references, to make
   *  garbage collection easier.
   */
  public void destroy() {
    if (map != null && pre_draw_listener != null) {
      map.getView().removePreDrawViewListener(pre_draw_listener);
    }
    //if (visible_range != null && hairline != null) {
    //  visible_range.removeListener(hairline);
    //}
    //if (map != null && mouse_listener != null) {
    //  map.removeMouseListener(mouse_listener);
    //}
    if (hairline != null) { hairline.destroy(); }
    //visible_range = null;
    //mouse_listener = null;
    hairline = null;
    map = null;
  }
}
