/**
 *   Copyright (c) 2001-2008 Affymetrix, Inc.
 *    
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genoviz.samples;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.List;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.util.NeoConstants.Direction;
import com.affymetrix.genoviz.tiers.AffyLabelledTierMap;
import com.affymetrix.genoviz.tiers.AffyTieredMap;
import com.affymetrix.genoviz.pack.ExpandPacker;
import com.affymetrix.genoviz.tiers.TestGlyph;
import com.affymetrix.genoviz.tiers.TierGlyph;
import com.affymetrix.genoviz.tiers.TransformTierGlyph;

public class LabelledTierTest {

  AffyTieredMap map;
  JSlider xzoomer;
  JSlider yzoomer;

  public static void main(String[] args) {
    LabelledTierTest test = new LabelledTierTest();
    test.doTest();
  }

  public void doTest() {
    JFrame frame;
    frame = new JFrame("labelled tier test");
    frame.setSize(600, 400);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    Container cpane = frame.getContentPane();
    cpane.setLayout(new BorderLayout());
    map = new AffyLabelledTierMap(true, true);
    map.setBackground(Color.CYAN);

    xzoomer = new JSlider(Adjustable.HORIZONTAL);
    map.setZoomer(WidgetAxis.Range, xzoomer);
    cpane.add("North", xzoomer);

    yzoomer = new JSlider(Adjustable.VERTICAL);
    //    yzoomer = new NeoScrollbar(Adjustable.VERTICAL);
    map.setZoomer(WidgetAxis.Offset, yzoomer);
    cpane.add("West", yzoomer);

    map.setMapRange(0, 1000);
    map.setMapOffset(0, 1000);
    map.addMouseListener(mouseListener);

    //TierLabelManager tier_manager = new TierLabelManager(map);

    // currently need to call setState() _after_ mucking with packer to 
    //   get tier to set packer to a different packer ????
    
    TierGlyph tier;
    TransformTierGlyph transform_tier;

    tier = new TierGlyph();
    tier.setLabel("Tier1");
    tier.setFillColor(Color.blue);
    ((ExpandPacker)tier.getExpandedPacker()).setMoveType(Direction.UP);
    tier.setState(TierGlyph.EXPANDED);
    addGlyphs(tier);
    map.addTier(tier);

    tier = new TierGlyph();
    tier.setLabel("Tier1");
    tier.setFillColor(Color.yellow);
    ((ExpandPacker)tier.getExpandedPacker()).setMoveType(Direction.UP);
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
    frame.setVisible(true);

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

  private final MouseListener mouseListener = new MouseAdapter() {

    @Override
    public void mousePressed(MouseEvent evt) {
      NeoMouseEvent nme = (NeoMouseEvent) evt;
      map.clearSelected();
      List<GlyphI> hitGlyphs = map.getItems(nme.getCoordX(), nme.getCoordY());
      map.select(hitGlyphs);
      map.updateWidget();
    }
  };
}
