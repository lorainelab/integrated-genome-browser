/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

package com.affymetrix.igb.tiers;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.awt.AdjustableJSlider;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.swing.SixWaySplitPane;
import java.awt.geom.Rectangle2D;

/**
 *  Wraps a AffyTieredMap and another map that has tier labels which
 *    track changes in tiers (size, placement) of AffyTieredMap.
 *
 * @version $Id$
 */
public final class AffyTieredMultiMap extends AffyLabelledTierMap {

  /* The extra map is where the rotated view appears on the right. */
  AffyTieredMap extramap;
  AffyTieredMap northEastMap;
  AffyTieredMap northMap;
  AffyTieredMap northWestMap = null;

  JPanel nwpan, nepan, wpan, cpan, epan;
  List<TierGlyph> extra_glyphs = new ArrayList<TierGlyph>();
  double extramap_inset = 5;

  private SixWaySplitPane windowPane;

  /**
   * Construct a map with default scroll bars.
   */
  public AffyTieredMultiMap() {
    this( true, true );
  }

  /**
   * Construct a map optionally with scroll bars.
   * @param hscroll_show makes an internal horizontal scroll bar visible.
   * @param vscroll_show makes an internal vertical scroll bar visible.
   */
  public AffyTieredMultiMap(boolean hscroll_show, boolean vscroll_show ) {
    super(hscroll_show, vscroll_show);
  }

  /**
   *  Overriding method from NeoMap. Called in NeoMap constructor.
   */
	@Override
  public void initComponentLayout() {

    this.northEastMap = new AffyTieredMap( false, false );
    this.northEastMap.name = "NorthEast";
    this.northMap = new AffyTieredMap( false, false );
    this.northMap.name = "North";
    int[] mainRange = this.getMapRange();
    this.northMap.setMapRange( mainRange[0], mainRange[1] );
    //this.northWestMap = new AffyTieredMap( false, false );
    this.northMap.setMapOffset( 0, 100 );
    this.northEastMap.setMapColor( Color.orange );
    this.northEastMap.setMapOffset( 0, 100 );
    //this.northWestMap.setMapOffset( 0, 100 );

    labelmap = new AffyTieredMap(false, false);
    labelmap.setRubberBandBehavior(false);
    this.setBackground(Color.blue);
    labelmap.setBackground(Color.lightGray);
    labelmap.name = "LabelMap";

    this.nwpan = new JPanel( new BorderLayout() );
    this.nepan = new JPanel( new BorderLayout() );
    this.nepan.add( northEastMap, BorderLayout.CENTER );
    this.wpan = new JPanel( new BorderLayout() );
    this.wpan.add( labelmap, BorderLayout.CENTER );

    NeoCanvas ncan = this.getNeoCanvas();
    this.cpan = new JPanel( new BorderLayout() );
    this.cpan.add( ncan, BorderLayout.CENTER );

    extramap = new AffyTieredMap(false, false);
    extramap.setBackground(Color.green);
    extramap.name = "ExtraMap";
    this.epan = new JPanel( new BorderLayout() );
    this.epan.add( extramap, BorderLayout.CENTER );
    this.epan.setPreferredSize( new Dimension( 100, 200 ) );

    if ( this.hscroll_show && this.scroller[X] instanceof JScrollBar )  {
      this.wpan.add( new MotionPanel( this.scroller[X].getOrientation(), -1 ), BorderLayout.SOUTH );
      MotionPanel mp = new MotionPanel( this.scroller[X].getOrientation(), X, X );
      setZoomer( X, mp.getZoomer( X ) );
      this.cpan.add( mp, BorderLayout.SOUTH );
      this.scroller[X] = mp.getPanner( X );
      this.epan.add( new MotionPanel( this.scroller[X].getOrientation(), -1, -1 ), BorderLayout.SOUTH );
    }
    if ( this.vscroll_show && this.scroller[Y] instanceof JScrollBar )  {
      MotionPanel mp = new MotionPanel( this.scroller[Y].getOrientation(), Y );
      setZoomer( Y, mp.getZoomer( Y ) );
      this.epan.add( mp, BorderLayout.EAST );
      this.scroller[Y] = mp.getPanner( Y );
      this.nepan.add( new MotionPanel( this.scroller[Y].getOrientation(), -1 ), BorderLayout.EAST );
    }

    this.windowPane = new SixWaySplitPane();
    this.windowPane.addCenter( this.cpan );
    this.windowPane.addWest( this.wpan );
    this.windowPane.addEast( this.epan );
    this.windowPane.addNorth( northMap );
    //this.windowPane.addNorthWest( northWestMap );
    this.windowPane.addNorthWest( nwpan );
    this.windowPane.addNorthEast( this.nepan );
    this.windowPane.setDividerLocations(50, 50, 150);

    this.setLayout(new BorderLayout());
    add( this.windowPane, BorderLayout.CENTER );

    // Try to make everything visible upon startup.
    this.northMap.setSize( 100, 20 );
    this.northEastMap.setSize( 40, 20 );
    //this.northWestMap.setSize( 60, 20 );
    this.wpan.setPreferredSize( new Dimension( 60, 60 ) );

    ZoomLine l = new ZoomLine();
    l.addMap( this );
    
    this.name = "AffyTieredMultiMap";
  }

  /**
   * Add a scroller to a section.
   * So far,
   * the only valid combinations are <code>HORIZONTAL</code> with <code>EAST</code>
   * and <code>VERTICAL</code> with <code>NORTH</code>.
   * @param theOrientation must be {@link NeoConstants#HORIZONTAL} or {@link NeoConstants#VERTICAL}.
   * @param theSection must be {@link NeoConstants#NORTH}, {@link NeoConstants#EAST}, {@link NeoConstants#CENTER}, or {@link NeoConstants#WEST}.
   */
  public void addScroller( int theOrientation, int theSection ) {
    if ( NeoConstants.HORIZONTAL == theOrientation && NeoConstants.EAST == theSection ) {
      MotionPanel mp = new MotionPanel( theOrientation, X );
      JScrollBar sb = new JScrollBar( JScrollBar.HORIZONTAL );
      this.epan.add( mp, BorderLayout.SOUTH );
      this.extramap.setScroller( X, mp.getPanner( X ) );
      this.extramap.setZoomer(X, mp.getZoomer(X));
      this.northEastMap.setScroller( X, mp.getPanner( X ) );
      this.northEastMap.setZoomer( X, mp.getZoomer( X ) );
    }
    else if ( NeoConstants.VERTICAL == theOrientation && NeoConstants.NORTH == theSection ) {
      JScrollBar sb = new JScrollBar( JScrollBar.VERTICAL );
      this.nepan.add( sb, BorderLayout.EAST );
      if ( null != this.northWestMap ) this.northWestMap.setScroller( Y, sb );
      this.northMap.setScroller( Y, sb );
      this.northEastMap.setScroller( Y, sb );
    }
    else {
      throw new IllegalArgumentException( "Only suport HORIZONTAL with EAST and VERTICAL with NORTH." );
    }
  }

  /**
   * Add a zoomer to a section.
   * So far,
   * the only valid combinations are <code>X</code> with <code>EAST</code>
   * and <code>Y</code> with <code>NORTH</code>.
   * @param theAxis must be {@link #X} or {@link #Y}.
   * @param theSection must be {@link NeoConstants#NORTH}, {@link NeoConstants#EAST}, {@link NeoConstants#CENTER}, or {@link NeoConstants#WEST}.
   * @param theControl
   */
  public void addZoomer( int theAxis, int theSection, Adjustable theControl ) {
    if ( X == theAxis && NeoConstants.EAST == theSection ) {
      this.extramap.setZoomer( theAxis, theControl );
      this.northEastMap.setZoomer( theAxis, theControl );
    }
    else if ( Y == theAxis && NeoConstants.NORTH == theSection ) {
      if ( null != this.northWestMap ) this.northWestMap.setZoomer( theAxis, theControl );
      this.northMap.setZoomer( theAxis, theControl );
      this.northEastMap.setZoomer( theAxis, theControl );
    }
    else {
      throw new IllegalArgumentException( "Only support X with EAST and Y with NORTH." );
    }
  }

  /**
   * Add a zoomer component to a section.
   * The axis is inferred from the orientation of the zoomer.
   * @param theSection must be {@link NeoConstants#NORTH}, {@link NeoConstants#EAST}, {@link NeoConstants#CENTER}, or {@link NeoConstants#WEST}.
   * @param theControl for zooming
   */
  public void addZoomer( int theSection, AdjustableJSlider theControl ) {
    int orientation = theControl.getOrientation();
    switch ( orientation ) {
      case Adjustable.HORIZONTAL:
        addZoomer( X, theSection, theControl ); break;
      case Adjustable.VERTICAL:
        addZoomer( Y, theSection, theControl ); break;
    }
  }

  public void clearWidget() {
  /*
   * Somehow, without overriding clearWidget() to take care of lable stuff,
   * map tiers end up staying around.
   * Maybe some hash in map is not being cleared out
   * and so label manipulations in packTiers() ends up bringing them back?
   */
    super.clearWidget();
    if (extramap != null) { extramap.clearWidget(); }
    if (northMap != null) { northMap.clearWidget(); }
    if (northEastMap != null) { northEastMap.clearWidget(); }
    if (northWestMap != null)  { northWestMap.clearWidget(); }
    if (labelmap != null)  { labelmap.clearWidget(); } // or is this handled in the superclass?
    extra_glyphs = new ArrayList<TierGlyph>();
  }

  public AffyTieredMap getExtraMap() {
    return extramap;
  }

  public List getExtraMapTiers() {
    return extra_glyphs;
  }

  /**
   * Pack tiers and line up tiers of the extra map.
   */
  public void packTiers(boolean full_repack, boolean stretch_map, boolean extra_for_now) {
    super.packTiers(full_repack, stretch_map, extra_for_now);
    Rectangle2D.Double lbox = extramap.getCoordBounds();
		for (TierGlyph extra_glyph : extra_glyphs) {
      TierGlyph tier_glyph = (TierGlyph)extra_glyph.getInfo();
      Rectangle2D.Double tbox = tier_glyph.getCoordBox();
      extra_glyph.setCoords(0, 0, lbox.width, tbox.height);
      extra_glyph.moveAbsolute(lbox.x, tbox.y);
      for (int k=0; k<extra_glyph.getChildCount(); k++) {
        GlyphI child = extra_glyph.getChild(k);
        child.setCoords(lbox.x, tbox.y + extramap_inset,
                        lbox.width, tbox.height-(2*extramap_inset));
      }
      
      // The "extra" tier glyph does not share the same AnnotStyle as the tier glyph,
      // because it may be drawn as a HeatMap while the other tier is drawn as
      // an mRNA glyph, but it does need to have the same property for "show".
      extra_glyph.getAnnotStyle().setShow(tier_glyph.isVisible());
      extra_glyph.setVisibility(tier_glyph.isVisible());
    }
    // The extra map's tiers need to be sorted by y position (top to bottom)
    // So that the tiers will not get rearanged by the extra map's packTiers method.
    Collections.sort( this.extramap.getAllTiers(), extraMapSorter );
  }

  public void setExtraMapInset(double inset) {
    extramap_inset = inset;
  }

  public void setExtraMapRange( int x, int y ) {
    this.extramap.setMapRange( x, y );
    this.northEastMap.setMapRange( x, y );
  }

  public double getExtraMapInset() { return extramap_inset; }


  public AffyTieredMap getNorthMap() {
    return this.northMap;
  }


  public AffyTieredMap getNorthEastMap() {
    return this.northEastMap;
  }

//  public void addNorthEastGlyph( SolidGlyph theGlyph ) {
//    int[] offset = this.northEastMap.getMapOffset();
//    int[] range = this.northEastMap.getMapRange();
//    Rectangle2D.Double r = new Rectangle2D( range[0], offset[0], range[1], offset[1] );
//    theGlyph.setCoordBox( r );
//    theGlyph.setCoords(0, 0, 100, 100);
//    this.northEastMap.addItem( theGlyph );
//    this.northEastMap.updateWidget();
//  }


  public void addNorthEastTier( TierGlyph theTier ) {
    this.northEastMap.addTier( theTier );
    this.northEastMap.updateWidget();
  }

  /**
   * Adds a tier to the map, generates a label for it,
   * and an extra glyph for the extra map.
   * @see #getExtraMap()
   */
  public void addTier(TierGlyph mtg, int tier_index) {
    super.addTier(mtg, tier_index);
    TierGlyph extra_glyph = new TierGlyph();
    extra_glyph.setFillColor(mtg.getFillColor());
    extra_glyph.setForegroundColor(mtg.getForegroundColor());
    extramap.addTier(extra_glyph, tier_index);
    extramap.setDataModel(extra_glyph, mtg);
    extra_glyphs.add(extra_glyph);
  }

  public void removeTier(TierGlyph toRemove) {
    super.removeTier(toRemove);
    GlyphI extra_glyph = extramap.getItem(toRemove);
    if (extra_glyph != null) {
      extramap.removeItem(extra_glyph);
      extra_glyphs.remove(extra_glyph);
    }
  }

  public void setFloatBounds(int axis, double start, double end) {
    super.setFloatBounds(axis, start, end);
    //? should the following two lines refer to labelmap or extramap?
    if (axis == Y && labelmap != null) {
      extramap.setFloatBounds(axis, start, end);
    }
    if ( X == axis && null != this.northMap ) {
      this.northMap.setFloatBounds( axis, start, end );
    }
  }

  public void setBounds(int axis, int start, int end) {
    super.setBounds(axis, start, end);
    if (axis == Y && extramap != null) {
      extramap.setBounds(axis, start, end);
    }
    if ( X == axis && null != this.northMap ) {
      this.northMap.setBounds( axis, start, end );
    }
  }

  /**
   * Zoom sections of the multimap.
   * @param axisid along which to zoom
   * If horizontal zoom the central and northern sections.
   * If vertical zoom the western, central, and eastern sections.
   */
  public void zoom(int axisid, double zoom_scale) {
    super.zoom(axisid, zoom_scale);
    if ( Y == axisid && null != extramap ) {
      this.extramap.zoom( axisid, zoom_scale );
      this.extramap.updateWidget();
    }
    if ( axisid == X && null != this.northMap ) {
      this.northMap.zoom( axisid, zoom_scale );
      this.northMap.updateWidget();
    }
  }

  /**
   * Scroll sections of the multimap.
   * @param axisid along which to scroll.
   * If horizontal, scroll central and north sections.
   * If vertical, scroll western, central, and eastern sections.
   * @param value of magnatude to scroll.
   */
  public void scroll( int axisid, double value ) {
    super.scroll( axisid, value );
    if ( Y == axisid && null != extramap ) {
      this.extramap.scroll( axisid, value );
      this.extramap.updateWidget();
    }
    if ( X == axisid && null != this.northMap ) {
      this.northMap.scroll( axisid, value );
      this.northMap.updateWidget();
    }
  }

  /**
   * @param axisid along which to zoom
   * If horizontal set the zooming behavior if the central and northern sections.
   * If vertical set the zooming behavior of the western, central, and eastern sections.
   */
  public void setZoomBehavior(int axisid, int constraint, double coord) {
    super.setZoomBehavior(axisid, constraint, coord);
    if ( Y == axisid && null != this.extramap ) {
      this.extramap.setZoomBehavior( axisid, constraint, coord );
    }
    if ( X == axisid && null != this.northMap ) {
      this.northMap.setZoomBehavior( axisid, constraint, coord );
    }
  }

  public void updateWidget() {
    super.updateWidget();
    this.extramap.updateWidget();
    this.northMap.updateWidget();
    if (northEastMap != null) {this.northEastMap.updateWidget();}
    if (northWestMap != null) {this.northWestMap.updateWidget();}
    zoom( Y, this.getZoom( Y ) ); // This seems a bit artificial.
  }

  public void updateWidget(boolean b) {
    super.updateWidget(b);
    this.extramap.updateWidget(b);
    this.northMap.updateWidget(b);
    if (northEastMap != null) {this.northEastMap.updateWidget(b);}
    if (northWestMap != null) {this.northWestMap.updateWidget(b);}
    zoom( Y, this.getZoom( Y ) ); // This seems a bit artificial.
  }

  public void stretchToFit(boolean fitx, boolean fity) {
    super.stretchToFit(fitx, fity);
    this.extramap.stretchToFit(fitx, fity);
    if ( null != this.northWestMap ) this.northWestMap.stretchToFit( fitx, fity );
    this.northMap.stretchToFit( fitx, fity );
    this.northEastMap.stretchToFit( fitx, fity );
  }

  public void repackTheTiers(boolean full_repack, boolean stretch_vertically) {
    super.repackTheTiers(full_repack, stretch_vertically);
    this.extramap.repackTheTiers(full_repack,stretch_vertically);
    if ( null != this.northWestMap ) this.northWestMap.repackTheTiers(full_repack, stretch_vertically);
    this.northMap.repackTheTiers(full_repack, stretch_vertically);
    this.northEastMap.repackTheTiers(full_repack, stretch_vertically);    
  }
  
  
  /**
   * Put the axis on the north map.
   */
  public AxisGlyph addHeaderAxis() {
    northMap.setMapOffset(0, 20);
    AxisGlyph ag = this.northMap.addAxis( 10 );    
    SeqMapView.setAxisFormatFromPrefs(ag);
    northMap.stretchToFit(true, true);
    northMap.updateWidget(true);
    return ag;
  }

  public void setMapRange( int theStart, int theEnd ) {
    super.setMapRange( theStart, theEnd );
    if ( null != this.northMap ) {
      this.northMap.setMapRange( theStart, theEnd );
    }
  }

  private final class VerticalTierComparator implements Comparator<GlyphI> {
    public int compare( GlyphI g1, GlyphI g2 ) {
      double y1 = g1.getCoordBox().y;
      double y2 = g2.getCoordBox().y;
      if ( y1 < y2 ) return -1;
      if ( y2 < y1 ) return 1;
      return 0;
    }
  }
  private VerticalTierComparator extraMapSorter = new VerticalTierComparator();

  public void setBackground(Color col) {
    super.setBackground(col);
    this.northEastMap.setBackground(col);
    this.northMap.setBackground(col);
    if (northWestMap != null) {this.northWestMap.setBackground(col);}
    if (extramap != null) {this.extramap.setBackground(col);}
  }
  
  /**
   *  Main for testing.
   */
  public static void main(String[] args) {
    AffyTieredMultiMap map = new AffyTieredMultiMap();

    map.setMapRange(0, 10000);
    map.setMapOffset(0, 1000);
    //map.addAxis(500);

    TierGlyph mtg = new TierGlyph();
    mtg.setCoords(0, 0, 1000, 200);
    mtg.setFillColor(Color.red);
    mtg.setLabel( "Red Tier" );
    map.addTier(mtg);
    mtg = new TierGlyph();
    mtg.setCoords(0, 0, 1000, 400);
    mtg.setFillColor(Color.orange);
    mtg.setLabel( "Orange Tier" );
    map.addTier(mtg);
    com.affymetrix.genoviz.bioviews.GlyphI spodt = new com.affymetrix.genoviz.glyph.FillOvalGlyph();
    spodt.setBackgroundColor( Color.blue );
    spodt.setCoords( 5000, 0,  500,  5 );
    mtg.addChild( spodt  );

    mtg = new TierGlyph();
    mtg.setFillColor( Color.white );
    mtg.setLabel( "header" );
    mtg.setCoords( 0, 0, 10000, 800 ); // Make the tier fill most of northern map (for now).
    AxisGlyph axis = new AxisGlyph();
    axis.setCoords(0, 0, 10001, 20);
    mtg.addChild( axis );

    map.addScroller( NeoConstants.HORIZONTAL, NeoConstants.EAST );
    //map.addScroller( VERTICAL, NORTH );

    map.repack();

    JFrame frm = new JFrame("AffyTieredMultiMap.main() test");
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", map);
    frm.setSize(600, 400);
    frm.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        System.exit(0);
      }
    } );
    frm.setVisible(true);
  }

}
