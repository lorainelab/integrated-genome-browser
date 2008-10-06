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

package com.affymetrix.igb.tiers;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.widget.tieredmap.ExpandedTierPacker;
import com.affymetrix.genoviz.awt.NeoScrollbar;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.glyph.*;

public class TierTest implements WindowListener, MouseListener  {
  AffyTieredMap map;
  NeoScrollbar xzoomer;
  NeoScrollbar yzoomer;

  public static void main(String[] args) {
    TierTest test = new TierTest();
    test.doTest();
  }

  public void doTest() {
    JFrame frm;
    frm = new JFrame("tier test");
    frm.setSize(600, 400);
    frm.addWindowListener(this);
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    map = new AffyTieredMap(true, true);
    //    map.setBackground(Color.red);

    //    xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
    xzoomer = new NeoScrollbar(Adjustable.HORIZONTAL);
    map.setZoomer(map.X, xzoomer);
    cpane.add("South", xzoomer);

    //    yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
    yzoomer = new NeoScrollbar(Adjustable.VERTICAL);
    map.setZoomer(map.Y, yzoomer);
    cpane.add("East", yzoomer);

    map.setMapRange(0, 1000);
    map.setMapOffset(-100, 100);
    map.addMouseListener(this);
    
    TierGlyph tier;
    LinearTransform trans;
    ExpandPacker epacker;

    tier = new TierGlyph();
    tier.setFillColor(Color.blue);
    tier.setCoords(0, 0, 1000, 0);
    tier.setExpandedPacker(new ExpandedTierPacker());
    tier.setState(TierGlyph.EXPANDED);
    ((ExpandedTierPacker)tier.getExpandedPacker()).setMoveType(ExpandedTierPacker.UP);
    addGlyphs(tier);
    map.addTier(tier);

    
    TransformTierGlyph transform_tier;
    transform_tier = new TransformTierGlyph();
    //       trans = new LinearTransform();
    //       trans.setScaleY(0.7f);
    //       transform_tier.setTransform(trans);
    transform_tier.setFixedPixelHeight(true);
    transform_tier.setFixedPixHeight(80);
    transform_tier.setFillColor(Color.cyan);
    transform_tier.setCoords(0, 0, 1000, 0);
    epacker = new ExpandPacker();
    transform_tier.setExpandedPacker(epacker);
    epacker.setMoveType(ExpandPacker.UP);
    transform_tier.setState(TierGlyph.EXPANDED);
    addGlyphs(transform_tier);
    map.addTier(transform_tier);

    transform_tier = new TransformTierGlyph();
    //    trans = new LinearTransform();
    //    trans.setScaleY(0.5f);
    //    transform_tier.setTransform(trans);
    transform_tier.setFixedPixelHeight(true);
    transform_tier.setFixedPixHeight(40);
    transform_tier.setFillColor(Color.darkGray);
    transform_tier.setCoords(0, 0, 1000, 0);
    epacker = new ExpandPacker();
    transform_tier.setExpandedPacker(epacker);
    epacker.setMoveType(ExpandPacker.DOWN);
    // currently need to call setState() _after_ mucking with packer to 
    //   get tier to set packer to epacker
    transform_tier.setState(TierGlyph.EXPANDED);
    //    addGlyphs2(transform_tier);
    addGlyphs(transform_tier);
    map.addTier(transform_tier);

    tier = new TierGlyph();
    tier.setFillColor(Color.gray);
    tier.setCoords(0, 0, 1000, 0);
    tier.setState(TierGlyph.EXPANDED);
    ((ExpandPacker)tier.getExpandedPacker()).setMoveType(ExpandPacker.DOWN);
    addGlyphs(tier);
    map.addTier(tier);

    map.repack();

    cpane.add("Center", map);
    frm.show();
  }

  public void addGlyphs2(TierGlyph tier) {
    GlyphI gl;
    gl = new TestGlyph("1");
    //    gl = new OutlineRectGlyph();
    gl.setColor(Color.black);
    gl.setCoords(20, 0, 520, 15);
    tier.addChild(gl);

  }

  public void addGlyphs(TierGlyph tier) {
    GlyphI gl;
    gl = new TestGlyph("1");
    //    gl = new OutlineRectGlyph();
    gl.setColor(Color.red);
    gl.setCoords(100, 0, 400, 10);
    tier.addChild(gl);

    gl = new TestGlyph("2");
    //    gl = new OutlineRectGlyph();
    gl.setColor(Color.pink);
    gl.setCoords(200, 0, 400, 10);
    tier.addChild(gl);

    gl = new TestGlyph("3");
    //    gl = new OutlineRectGlyph();
    gl.setColor(Color.yellow);
    gl.setCoords(300, 0, 400, 10);
    tier.addChild(gl);

    //    gl = new OutlineRectGlyph();
    gl = new TestGlyph("4");
    gl.setColor(Color.white);
    gl.setCoords(400, 0, 400, 10);
    tier.addChild(gl);

  }

  public void mouseClicked(MouseEvent e) { }
  public void mouseEntered(MouseEvent e) { }
  public void mouseExited(MouseEvent e) { }
  public void mouseReleased(MouseEvent e) { }
  public void mousePressed(MouseEvent evt) { 
    Object src = evt.getSource();
    if (src == map) {
      NeoMouseEvent nme = (NeoMouseEvent)evt;
      map.clearSelected();
      Vector hitGlyphs = map.getItems(nme.getCoordX(), nme.getCoordY());
      map.select(hitGlyphs);
      map.updateWidget();
      System.out.println("canvas event: " + nme.getX() + ", " + nme.getY() + 
			 ",   " + map.getNeoCanvas());
    }
  }

  public void windowActivated(WindowEvent evt) {}
  public void windowDeactivated(WindowEvent evt) {}
  public void windowDeiconified(WindowEvent evt) {}
  public void windowIconified(WindowEvent evt) {}
  public void windowOpened(WindowEvent evt) {}
  public void windowClosed(WindowEvent evt) {}
  public void windowClosing(WindowEvent evt) {
    System.exit(0);
  }

}
