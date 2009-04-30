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
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.awt.AdjustableJSlider;

public class TestLabelledTier implements WindowListener, MouseListener  {
  AffyLabelledTierMap map;
  AdjustableJSlider xzoomer;
  AdjustableJSlider yzoomer;

  public static void main(String[] args) {
    TestLabelledTier test = new TestLabelledTier();
    test.doTest();
  }

  public void doTest() {
    JFrame frm;
    frm = new JFrame("labelled tier test");
    frm.setSize(600, 400);
    frm.addWindowListener(this);
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    map = new AffyLabelledTierMap();
    //    map.setBackground(Color.red);

    xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
    map.setZoomer(NeoMap.X, xzoomer);
    cpane.add("South", xzoomer);

    yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
    map.setZoomer(NeoMap.Y, yzoomer);
    cpane.add("East", yzoomer);

    map.setMapRange(0, 1000);
    map.setMapOffset(0, 1000);
    map.addMouseListener(this);

    TierLabelManager tier_manager = new TierLabelManager(map);

    // currently need to call setState() _after_ mucking with packer to 
    //   get tier to set packer to a different packer ????
    
    TierGlyph tier;
    TransformTierGlyph transform_tier;
    LinearTransform trans;
    ExpandPacker epacker;

    tier = new TierGlyph();
    tier.setLabel("Tier1");
    tier.setFillColor(Color.blue);
    ((ExpandPacker)tier.getExpandedPacker()).setMoveType(ExpandPacker.UP);
    tier.setState(TierGlyph.EXPANDED);
    addGlyphs(tier);
    map.addTier(tier);

    tier = new TierGlyph();
    tier.setLabel("Tier1");
    tier.setFillColor(Color.yellow);
    ((ExpandPacker)tier.getExpandedPacker()).setMoveType(ExpandPacker.UP);
    tier.setState(TierGlyph.EXPANDED);
    addGlyphs(tier);
    map.addTier(tier);

    transform_tier = new TransformTierGlyph();
    transform_tier.setLabel("Tier2 (80 pixels)");
    transform_tier.setFixedPixelHeight(true);
    transform_tier.setFixedPixHeight(80);
    transform_tier.setFillColor(Color.cyan);
    addGlyphs(transform_tier);
    map.addTier(transform_tier);

    TransformTierGlyph axis_tier = new TransformTierGlyph();
    axis_tier.setLabel("Coordinates");
    axis_tier.setFixedPixelHeight(true);
    axis_tier.setFixedPixHeight(30);
    axis_tier.setFillColor(Color.white);
    map.addTier(axis_tier);
    GlyphI axis_glyph = map.addAxis(0);
    axis_tier.addChild(axis_glyph);

    transform_tier = new TransformTierGlyph();
    transform_tier.setLabel("Tier3 (40 pixels)");
    transform_tier.setFixedPixelHeight(true);
    transform_tier.setFixedPixHeight(40);
    transform_tier.setFillColor(Color.darkGray);
    addGlyphs(transform_tier);
    map.addTier(transform_tier);

    tier = new TierGlyph();
    tier.setLabel("Tier4");
    tier.setFillColor(Color.gray);
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
    //    System.out.println(src);
    if (src == map) {
      NeoMouseEvent nme = (NeoMouseEvent)evt;
      map.clearSelected();
      Vector hitGlyphs = map.getItems(nme.getCoordX(), nme.getCoordY());
      map.select(hitGlyphs);
      map.updateWidget();
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
