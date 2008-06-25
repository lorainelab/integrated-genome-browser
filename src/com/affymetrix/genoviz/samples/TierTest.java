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

import com.affymetrix.genoviz.tiers.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.affymetrix.genoviz.widget.tieredmap.ExpandedTierPacker;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.util.NeoConstants.Direction;
import java.util.List;

public class TierTest {
  AffyTieredMap map;
  JSlider xzoomer;
  JSlider yzoomer;

  public static void main(String[] args) {
    TierTest test = new TierTest();
    test.doTest();
  }

  public void doTest() {
    JFrame frame;
    frame = new JFrame("tier test");
    frame.setSize(600, 400);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Container cpane = frame.getContentPane();
    cpane.setLayout(new BorderLayout());
    map = new AffyTieredMap(true, true);
    //    map.setBackground(Color.red);

    //    xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
    xzoomer = new JSlider(Adjustable.HORIZONTAL);
    map.setZoomer(TransformI.Dimension.X, xzoomer);
    cpane.add("North", xzoomer);

    //    yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
    yzoomer = new JSlider(Adjustable.VERTICAL);
    map.setZoomer(TransformI.Dimension.Y, yzoomer);
    cpane.add("West", yzoomer);

    map.setMapRange(0, 1000);
    map.setMapOffset(-100, 100);
    map.addMouseListener(mouseListener);
    
    TierGlyph tier;
    ExpandPacker epacker;

    tier = new TierGlyph();
    tier.setFillColor(Color.RED);
    tier.setCoords(0, 0, 1000, 0);
    tier.setExpandedPacker(new ExpandedTierPacker());
    tier.setState(TierGlyph.EXPANDED);
    ((ExpandedTierPacker)tier.getExpandedPacker()).setMoveType(Direction.UP);
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
    epacker.setMoveType(Direction.UP);
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
    epacker.setMoveType(Direction.DOWN);
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
    ((ExpandPacker)tier.getExpandedPacker()).setMoveType(Direction.DOWN);
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
