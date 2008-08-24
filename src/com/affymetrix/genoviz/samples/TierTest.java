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
import com.affymetrix.genoviz.bioviews.SceneI.SelectType;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.HorizontalAxisGlyph;
import com.affymetrix.genoviz.glyph.LineStretchContainerGlyph;
import com.affymetrix.genoviz.util.NeoConstants.Direction;
import com.affymetrix.genoviz.util.NeoConstants.Placement;
import com.affymetrix.genoviz.widget.NeoMap;
import java.util.List;

public class TierTest {
  final JFrame frame;
  static AffyTieredMap map;
  JSlider xzoomer;
  JSlider yzoomer;
  
  TierTest() {
    super();
    frame = new JFrame(this.getClass().getSimpleName());
    frame.setSize(600, 400);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  public static void main(String[] args) {
    final TierTest test = new TierTest();
    
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        test.doTest();
        test.frame.setVisible(true);
//        test.frame.setSize(600, 401);
        map.stretchToFit(true, true);
        map.updateWidget(true);
      }
    });
  }

  public void doTest() {

    Container cpane = frame.getContentPane();
    cpane.setLayout(new BorderLayout());
    map = new AffyTieredMap(true, true);
    map.setBackground(Color.MAGENTA);

    map.setSelectionAppearance(SelectType.SELECT_OUTLINE);
    
    map.setReshapeBehavior(NeoMap.Xint, NeoMap.FITWIDGET - 3);
    map.setReshapeBehavior(NeoMap.Yint, 0);
    
    //    xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
    xzoomer = new JSlider(Adjustable.HORIZONTAL);
    map.setZoomer(WidgetAxis.Primary, xzoomer);
    cpane.add("North", xzoomer);

    //    yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
    yzoomer = new JSlider(Adjustable.VERTICAL);
    map.setZoomer(WidgetAxis.Secondary, yzoomer);
    cpane.add("West", yzoomer);

    int mapWidth = 10000;
    map.setMapRange(0, mapWidth);
    map.setMapOffset(-100, 100); // mostly irrelvant
    map.addMouseListener(mouseListener);
    
    TierGlyph tier;
    ExpandPacker epacker;

    IAnnotStyle style = new DefaultIAnnotStyle();
    style.setHumanName("My name is BOB!");
    
    tier = new TierGlyph(style);
    tier.setFillColor(Color.LIGHT_GRAY);
    tier.setCoords(0, 0, mapWidth, 0);
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
    transform_tier.setCoords(0, 0, mapWidth, 0);
    epacker = new ExpandPacker();
    transform_tier.setExpandedPacker(epacker);
    epacker.setMoveType(Direction.UP);
    transform_tier.setState(TierGlyph.EXPANDED);
    addGlyphs(transform_tier);
    map.addTier(transform_tier);


    TransformTierGlyph axis_tier = new TransformTierGlyph();

    HorizontalAxisGlyph axis = map.addHorizontalAxis(30); //TODO: number 30 is ignored?
    axis.setForegroundColor(Color.GREEN.darker());
    axis.setLabelPlacement(Placement.BELOW);
    
    axis_tier.setFixedPixelHeight(true);
    axis_tier.setFixedPixHeight(40);
    axis_tier.setFillColor(Color.WHITE);
    axis_tier.setCoords(0, 0, mapWidth, 0);

    axis_tier.setSpacer(100); //Spacer ignored because no packer.
    axis_tier.addChild(axis);
    map.addTier(axis_tier);

    tier = new TierGlyph();
    tier.setFillColor(Color.gray);
    tier.setCoords(0, 0, mapWidth, 0);
    tier.setState(TierGlyph.EXPANDED);
    ((ExpandPacker)tier.getExpandedPacker()).setMoveType(Direction.DOWN);
//    addGlyphs(tier);
    fillWithComposites(tier, Color.MAGENTA.darker().darker(), 30);
    map.addTier(tier);

    map.repack();
    

    cpane.add("Center", map);    
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

  public void fillWithComposites(TierGlyph tier, Color col, int total) {
    GlyphI container;
    GlyphI child1, child2;
    int height = 5;
    int start, width;
    for (int i=0; i<total; i++) {
      start = (int)(Math.random() * tier.getCoordBox().width*3.0/4.0);
      width = (int)(Math.random() * tier.getCoordBox().width/4.0);
      //	    container = new LineContainerGlyph();
      container = new LineStretchContainerGlyph();
      container.setColor(col);
      //	    container.setCoords(start, 0, width, height);

      tier.addChild(container);
      child1 = new FillRectGlyph();
      child1.setCoords(start, 10, width/4, height);
      child1.setColor(col);
      container.addChild(child1);
      //	    mtg.addChild(child1);

      child2 = new FillRectGlyph();
      child2.setCoords(start+3*width/4, 10, width/4, height);
      child2.setColor(col);
      container.addChild(child2);
      //	    mtg.addChild(child2);

    }
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
