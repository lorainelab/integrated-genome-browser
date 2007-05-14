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

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.tieredmap.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * implements something like an AWT List
 * using a TieredNeoMap.
 * For now implement just single selection.
 * Why use a NeoMap?
 * The idea is to eventually be able to add other glyphs
 * like graphics to create a histogram for example.
 */
public class NeoListMap extends TieredNeoMap implements ItemSelectable {

  public NeoListMap() {
    super( false, false ); // No scrollbars please.
    setSubSelectionAllowed(true); // allow selection of tiers
    SceneI s = getScene();
    s.setSelectionColor( Color.magenta );
    s.setSelectionAppearance( com.affymetrix.genoviz.bioviews.Scene.SELECT_FILL );
  }

  private int getTier( NeoMouseEvent e ) {
    Vector v = e.getItems();
    if ( null != v ) {
      Enumeration glyphs = v.elements();
      while ( glyphs.hasMoreElements() ) {
        Object o = glyphs.nextElement();
        if ( o instanceof MapTierGlyph ) {
          MapTierGlyph g = ( MapTierGlyph ) o;
          return indexOf( g );
        }
      }
    }
    return -1;
  }

  private Vector<MapTierGlyph> items = new Vector<MapTierGlyph>();
  private int selectedIndex = -1;
  private Integer itemIndex = new Integer( selectedIndex );
  private MouseListener switcher = new MouseAdapter() {
    public void mousePressed( MouseEvent e ) {
      if ( e instanceof NeoMouseEvent ) {
        select( getTier( ( NeoMouseEvent ) e ) );
      }
    }
  };

  public int getSelectedIndex() {
    return this.selectedIndex;
  }

  private int lastItem = -1;
  private static double itemHeight = 14f;
  /**
   * Similar to java.awt.List#add(String)
   */
  public MapTierGlyph add( String s ) {
    MapTierGlyph g = new MapTierGlyph();
    g.setLabel( s );
    this.items.addElement( g );
    g.setHitable( true );
    g.setSelectable( true );
    int x[] = getMapRange();
    double fx, fy, fw;
    fx = x[0];
    fw = x[1] - x[0];
    fy = this.itemHeight * ++this.lastItem;
    g.setCoords( fx, fy, fw, this.itemHeight );
    int height = (int) ( fy + this.itemHeight );
    int y[] = getMapOffset();
    if ( y[1] <= height ) {
      y[1] = height + 1;
      setMapOffset( y[0], y[1] );
    }
    addMouseListener( this.switcher );
    addTier( g );
    return g;
  }

  public void clearWidget(){
    super.clearWidget();
    this.lastItem = -1;
    items = new Vector<MapTierGlyph>();
    selectedIndex = -1;
  }

  public void select(GlyphI gl) {
    super.select(gl);
    if (this.getSelectedIndex() == -1 ||
        gl != items.elementAt(getSelectedIndex())) {
      int index = -1;
      for (int i=0; i<items.size(); i++) {
        if (gl == items.elementAt(i)) {
          select(i);
          return;
        }
      }
    }
  }

  /**
   * Similar to java.awt.List#select.
   */
  public void select( int index ) {
    if ( this.getSelectedIndex() == index ) return;
    MapTierGlyph g;
    if ( -1 < this.selectedIndex ) {
      deselect( this.selectedIndex );
    }
    this.selectedIndex = index;
    if ( -1 < index ) {
      g = ( MapTierGlyph ) this.items.elementAt( index );
      select(g);  // This makes sure that g.getSelected() == true, _and_ adds g to map selection vec.
      fireItemEvent( new ItemEvent( this,
                                    ItemEvent.ITEM_STATE_CHANGED,
                                    new Integer( this.selectedIndex ),
                                    ItemEvent.SELECTED ) );
      updateWidget();
    }
  }

  /**
   * Similar to java.awt.List#deselect.
   */
  public void deselect( int index ) {
    MapTierGlyph g = ( MapTierGlyph ) this.items.elementAt( index );
    deselect(g);  // This makes sure that g.getSelected() == false, _and_ removes g from map selection vec.
    Integer i = new Integer( this.selectedIndex );
    this.selectedIndex = -1;
    fireItemEvent( new ItemEvent( this,
                                  ItemEvent.ITEM_STATE_CHANGED,
                                  i,
                                  ItemEvent.DESELECTED ) );
    updateWidget();
  }

  // ItemSelectable Implementation

  private ItemListener listener = null;
  private Object[] selection = new Object[1];
  private Object[] emptySelection = new Object[0];

  public void addItemListener( ItemListener l ) {
    this.listener = AWTEventMulticaster.add( this.listener, l );
  }

  public void removeItemListener( ItemListener l ) {
    this.listener = AWTEventMulticaster.remove( this.listener, l );
  }

  public Object[] getSelectedObjects() {
    if ( this.selectedIndex < 0 ) {
      return this.emptySelection;
    }
    this.selection[0] = this.items.elementAt( this.selectedIndex );
    return this.selection;
  }

  protected void fireItemEvent( ItemEvent e ) {
    if ( null != this.listener ) {
      this.listener.itemStateChanged( e );
    }
  }

  /**
   * intercepts tier movements to keep the selected index up to date.
   */
  public void moveTier( int from, int to ) {
    super.moveTier( from, to );
    int at = this.getSelectedIndex();
    int low = Math.min( from, to );
    int high = Math.max( from, to );
    MapTierGlyph o = this.items.elementAt( from );
    this.items.removeElementAt( from );
    this.items.insertElementAt( o, to );
    if ( low <= at && at <= high ) {
      if ( at == from ) {
        this.selectedIndex = to;
      }
      else if ( from == low ) {
        this.selectedIndex--;
      }
      else { // from == high
        this.selectedIndex++;
      }
    }
  }

}
