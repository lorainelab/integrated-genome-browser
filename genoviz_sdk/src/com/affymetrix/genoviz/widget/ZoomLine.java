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

import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.widget.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * The hairline is positioned with a mouse click.
 * Zooming is focused on the hairline.
 * To use this, create a ZoomLine,
 * Then add however many maps on which you would like it to appear.
 * @author Eric Blossom
 */
public class ZoomLine implements NeoWidgetListener {

  private VisibleRange zoomPoint = new VisibleRange();
  private Hashtable maps = new Hashtable();

  private MouseListener zoomPointAdjuster = new MouseAdapter() {
    public void mouseReleased( MouseEvent e ) {
      double focus = ((NeoMouseEvent)e).getCoordX();
      ZoomLine.this.zoomPoint.setSpot(focus);
    }
  };

  private KeyListener zoomPointNudger = new KeyAdapter() {
    public void keyPressed( KeyEvent ke ) {
      double at = ZoomLine.this.zoomPoint.getBeginning();
      int c = ke.getKeyCode();
      if ( KeyEvent.VK_RIGHT == c ) {
        ZoomLine.this.zoomPoint.setSpot( at + 1 );
      }
      else if ( KeyEvent.VK_LEFT == c ) {
        ZoomLine.this.zoomPoint.setSpot( at - 1 );
      }
    }
  };

  public ZoomLine() {

    super();

    NeoRangeListener zoomAdjuster = new NeoRangeListener() {
      public void rangeChanged( NeoRangeEvent e ) {
        double midPoint = ( e.getVisibleEnd() + e.getVisibleStart() ) / 2.0f;
        Enumeration clients = ZoomLine.this.maps.keys();
        while ( clients.hasMoreElements() ) {
          Object o = clients.nextElement();
          NeoWidgetI w = ( NeoWidgetI ) o;
          w.setZoomBehavior( NeoMap.X, NeoMap.CONSTRAIN_COORD, midPoint );
          w.updateWidget();
        }
      }
    };

    this.zoomPoint.addListener( zoomAdjuster );

  }

  public void addMap( NeoMap m ) {
    m.addMouseListener( this.zoomPointAdjuster );
    m.addKeyListener( this.zoomPointNudger );
    Shadow hairline =
      new Shadow( m, com.affymetrix.genoviz.util.NeoConstants.HORIZONTAL, Color.blue );
    hairline.setSelectable( false );
    this.zoomPoint.addListener( hairline );
    this.maps.put( m, hairline );
    m.addWidgetListener( this );
  }

  public void removeMap( NeoMap m ) {
    this.maps.remove( m );
  }

  public void widgetCleared( NeoWidgetEvent e ) {
    Object o = e.getSource();
    NeoMap m = ( NeoMap ) o;
    clearWidget( m );
  }

  /**
   * override clearWidget to notify listeners.
   * Used in order to keep the hairline.
   * <em>This should be listener code.</em>
   */
  public void clearWidget( NeoMap m ) {
    Object o = this.maps.get( m );
    if ( null != o ) {
      Shadow hairline = ( Shadow ) o;
      this.zoomPoint.removeListener( hairline );
      this.maps.remove( m );
      hairline = new Shadow( m, com.affymetrix.genoviz.util.NeoConstants.HORIZONTAL, Color.blue );
      this.maps.put( m, hairline );
      hairline.setSelectable( false );
      this.zoomPoint.addListener( hairline );
    }
  }

  public void setSpot( double theSpot ) {
    this.zoomPoint.setSpot( theSpot );
  }

  public double getSpot() {
    return this.zoomPoint.getBeginning();
  }

}
