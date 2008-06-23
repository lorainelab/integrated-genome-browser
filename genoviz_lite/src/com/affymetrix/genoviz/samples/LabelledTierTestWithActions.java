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

import java.applet.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*; 
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.widget.tieredmap.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;

public class LabelledTierTestWithActions extends Applet 
  implements ActionListener, ItemListener  {
  JFrame mainframe;
  Container mainpane;

  TieredNeoMap tmap;
  TieredLabelMap lmap;

  MapTierGlyph tier1, tier2, tier3, tier4;
  MapTierGlyph selected = null;
  CheckboxMenuItem tier1_select, tier2_select, 
    tier3_select, tier4_select;
  MenuItem expandMI, collapseMI, hideMI, moveMI, removeMI;

  int tierCount = 0;


  public static void main(String[] args) {
    LabelledTierTestWithActions test = new LabelledTierTestWithActions();
    test.init();
  }

  @Override
  public void init() {
    mainframe = new JFrame("TieredNeoMap Test");
    mainpane = mainframe.getContentPane();

    tmap = new TieredNeoMap(false, true);
    tmap.setMapRange(0, 20000);
    tmap.setMapOffset(0, 100);

    lmap = new TieredLabelMap(false, true);
    lmap.setMapRange(0, 20000);
    lmap.setMapOffset(0, 100);

    // Set the regular & label maps to listen to one another

    lmap.addTierEventListener (tmap);
    tmap.addTierEventListener (lmap);

    tier1 = makeTier(20, Color.lightGray);
    tier2 = makeTier(40, null);
    tier3 = makeTier(20, Color.black);
    tier4 = makeTier(20, null);
    selected = tier1;
	
    fillWithComposites(tier1, Color.black, 70);
    fillWithOutlines(tier2, Color.blue, 50);
    fillWithSolids(tier3, Color.green, 30);
    //	fillWithComposites(null, Color.red, 2);

    GlyphI axis = tmap.addAxis(0);
    tier4.setSpacer(10);
    tier4.addChild(axis);

    tmap.addTier(tier1);
    tmap.addTier(tier2);
    tmap.addTier(tier3);
    tmap.addTier(tier4);

    tier1.setState(MapTierGlyph.EXPANDED);
    tier2.setState(MapTierGlyph.COLLAPSED);
    tier3.setState(MapTierGlyph.EXPANDED);
    tier4.setState(MapTierGlyph.EXPANDED);

    tmap.setScrollingOptimized(true);

    // Seems to not pack properly 'til it's showing...

    tmap.repack();

    setUpMenus();
    mainpane.setLayout(new GridLayout(1,2));
    mainpane.add(lmap);
    mainpane.add(tmap);
    //	mainpane.add("East", yscroller);

    
    mainframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainframe.setSize(600, 300);
    mainframe.setVisible(true);

  }

  public MapTierGlyph makeTier(int height, Color col) {
    MapTierGlyph tmg = new MapTierGlyph();
    tmg.setLabel ("Tier " + (++tierCount));
    tmg.setShowLabel(false);
    tmg.setFillColor(col);
    tmg.setCoords(0, 0, 0, height);
    tmg.setSpacer(5);
    return tmg;
  }

  public void fillWithComposites(MapTierGlyph mtg, Color col, int total) {
    GlyphI container;
    GlyphI child1, child2;
    int height = 5;
    int start, width;
    for (int i=0; i<total; i++) {
      start = (int)(Math.random() * 20000);
      width = (int)(500 + Math.random()*1000);
      //	    container = new LineContainerGlyph();
      container = new LineStretchContainerGlyph();
      container.setColor(col);
      //	    container.setCoords(start, 0, width, height);
      if (mtg == null) {
	tmap.addItem(container);
      }
      else {
	mtg.addChild(container);
      }
      child1 = new FillRectGlyph();
      child1.setCoords(start, 10, 200, height);
      child1.setColor(col);
      container.addChild(child1);
      //	    mtg.addChild(child1);

      child2 = new FillRectGlyph();
      child2.setCoords(start+width-200, 10, 200, height);
      child2.setColor(col);
      container.addChild(child2);
      //	    mtg.addChild(child2);

    }

  }

  public void fillWithSolids(MapTierGlyph mtg, Color col, int total) {
    GlyphI gl;
    int height = 5;
    int start;
    int width;
    for (int i=0; i<total; i++) {
      start = (int)(Math.random() * 20000);
      width = (int)(100 + Math.random()*1000);
      gl = new FillRectGlyph();
      gl.setCoords(start, 0, width, height);
      gl.setColor(col);
      mtg.addChild(gl);
    }
  }

  public void fillWithOutlines(MapTierGlyph mtg, Color col, int total) {
    GlyphI gl;
    int height;
    int start;
    int width;
    for (int i=0; i<total; i++) {
      start = (int)(Math.random() * 20000);
      width = (int)(100 + Math.random()*1000);
      height = (int)(3 + Math.random()*15);
      gl = new OutlineRectGlyph();
      gl.setCoords(start, 0, width, height);
      gl.setColor(col);
      mtg.addChild(gl);
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == expandMI) {
      selected.setState(MapTierGlyph.EXPANDED);
    }
    else if (src == collapseMI) {
      selected.setState(MapTierGlyph.COLLAPSED);
    }
    else if (src == hideMI) {
      selected.setState(MapTierGlyph.HIDDEN);
    }
    else if (src == moveMI) {
    }
    else if (src == removeMI) {

      // Remove just the tier from the tmap; it should tell all
      // listeners to do likewise.

      tmap.removeTier (selected);
      tmap.repack();
      tmap.updateWidget();
    }
  }

  public void itemStateChanged(ItemEvent evt) {
    Object src = evt.getSource();
    if (src == tier1_select)  {
      selected = tier1;
      fixSelectionMenu((CheckboxMenuItem)src);
    }
    else if (src == tier2_select) {
      selected = tier2;
      fixSelectionMenu((CheckboxMenuItem)src);
    }
    else if (src == tier3_select) {
      selected = tier3;
      fixSelectionMenu((CheckboxMenuItem)src);
    }
    else if (src == tier4_select) {
      selected = tier4;
      fixSelectionMenu((CheckboxMenuItem)src);
    }
  }

  protected void fixSelectionMenu(CheckboxMenuItem currentItem) {
    tier1_select.setState(false);
    tier2_select.setState(false);
    tier3_select.setState(false);
    tier4_select.setState(false);
    currentItem.setState(true);
  }

  public void setUpMenus() {
    MenuBar mbar = new MenuBar();
    mainframe.setMenuBar(mbar);
	
    Menu selectMenu = new Menu("Selection");
    Menu actionMenu = new Menu("Action");
    tier1_select = new CheckboxMenuItem("tier1");
    tier2_select = new CheckboxMenuItem("tier2");
    tier3_select = new CheckboxMenuItem("tier3");
    tier4_select = new CheckboxMenuItem("tier4");

    tier1_select.setState(true);	
    tier1_select.addItemListener(this);
    tier2_select.addItemListener(this);
    tier3_select.addItemListener(this);
    tier4_select.addItemListener(this);


    selectMenu.add(tier1_select);
    selectMenu.add(tier2_select);
    selectMenu.add(tier3_select);
    selectMenu.add(tier4_select);

    expandMI = new MenuItem("Expand");
    collapseMI = new MenuItem("Collapse");
    hideMI = new MenuItem("Hide");
    moveMI = new MenuItem("Move");
    removeMI = new MenuItem("Remove");

    expandMI.addActionListener(this);
    collapseMI.addActionListener(this);
    hideMI.addActionListener(this);
    moveMI.addActionListener(this);
    removeMI.addActionListener(this);
       
    actionMenu.add(expandMI);
    actionMenu.add(collapseMI);
    actionMenu.add(hideMI);
    actionMenu.add(moveMI);
    actionMenu.add(removeMI);

    mbar.add(selectMenu);
    mbar.add(actionMenu);
  }

    
}

