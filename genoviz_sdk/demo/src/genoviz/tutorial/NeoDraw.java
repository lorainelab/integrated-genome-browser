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

package genoviz.tutorial;

import com.affymetrix.genoviz.bioviews.MapGlyphFactory;
import com.affymetrix.genoviz.event.NeoRubberBandListener;
import com.affymetrix.genoviz.event.NeoRubberBandEvent;
import com.affymetrix.genoviz.bioviews.Rectangle2D;
import com.affymetrix.genoviz.bioviews.SceneI;

import com.affymetrix.genoviz.widget.NeoMap;

import java.awt.Color;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JApplet;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.WindowConstants;

public class NeoDraw extends JApplet 
	implements NeoRubberBandListener, ActionListener, ItemListener {

  protected JMenu glyphMenu = new JMenu("Glyph", true);
  protected JCheckBoxMenuItem arrowMenuItem = new JCheckBoxMenuItem("ArrowGlyph");
  protected JCheckBoxMenuItem boundedPointMenuItem = new JCheckBoxMenuItem("BoundedPointGlyph");
  protected JCheckBoxMenuItem centeredCircleMenuItem = new JCheckBoxMenuItem("CenteredCircleGlyph");
  protected JCheckBoxMenuItem fillRectMenuItem = new JCheckBoxMenuItem("FillRectGlyph");
  protected JCheckBoxMenuItem fillOvalMenuItem = new JCheckBoxMenuItem("FillOvalGlyph");
  protected JCheckBoxMenuItem lineContainerMenuItem = new JCheckBoxMenuItem("LineContainerGlyph");
  protected JCheckBoxMenuItem outlineRectMenuItem = new JCheckBoxMenuItem("OutlineRectGlyph");
  protected JCheckBoxMenuItem squiggleMenuItem = new JCheckBoxMenuItem("SquiggleGlyph");
  protected JCheckBoxMenuItem triBarGlyphMenuItem = new JCheckBoxMenuItem("TriBarGlyph");
  protected JCheckBoxMenuItem triangleMenuItem = new JCheckBoxMenuItem("TriangleGlyph");

  protected JMenu colorMenu = new JMenu("Color", true);
  protected JCheckBoxMenuItem blackMenuItem = new JCheckBoxMenuItem("black");
  protected JCheckBoxMenuItem blueMenuItem = new JCheckBoxMenuItem("blue");
  protected JCheckBoxMenuItem redMenuItem = new JCheckBoxMenuItem("red");
  protected JCheckBoxMenuItem grayMenuItem = new JCheckBoxMenuItem("gray");
  protected JCheckBoxMenuItem magentaMenuItem = new JCheckBoxMenuItem("magenta");

  protected JMenu editMenu = new JMenu("Edit");
  protected JMenuItem clearMenuItem = new JMenuItem("Clear");
  protected JMenuItem repackMenuItem = new JMenuItem("Repack");

  NeoMap map;

  /**
   *  main method wrapper for NeoDraw (which is a JApplet), so can be used 
   *  as both an applet and a standalone program
   */
  public static void main (String argv[]) {
    NeoDraw me = new NeoDraw();
    JFrame frm = new JFrame("GenoViz Tutorial: NeoDraw");
    frm.add("Center", me);
    frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frm.pack();
    frm.setBounds( 20, 40, 900, 400 );
    frm.setVisible(true);
  }

  public NeoDraw() {
    map = new NeoMap();

    this.map.setSelectionEvent(this.map.ON_MOUSE_DOWN);
    this.map.setSelectionAppearance(SceneI.SELECT_OUTLINE);
    this.map.addRubberBandListener(this);

    fillRectMenuItem.setState(true);
    glyphMenu.add(arrowMenuItem);
    arrowMenuItem.addItemListener(this);
    glyphMenu.add(boundedPointMenuItem);
    boundedPointMenuItem.addItemListener(this);
    glyphMenu.add(centeredCircleMenuItem);
    centeredCircleMenuItem.addItemListener(this);
    glyphMenu.add(fillOvalMenuItem);
    fillOvalMenuItem.addItemListener(this);
    glyphMenu.add(fillRectMenuItem);
    fillRectMenuItem.addItemListener(this);
    glyphMenu.add(lineContainerMenuItem);
    lineContainerMenuItem.addItemListener(this);
    glyphMenu.add(outlineRectMenuItem);
    outlineRectMenuItem.addItemListener(this);
    glyphMenu.add(squiggleMenuItem);
    squiggleMenuItem.addItemListener(this);
    glyphMenu.add(triBarGlyphMenuItem);
    triBarGlyphMenuItem.addItemListener(this);
    glyphMenu.add(triangleMenuItem);
    triangleMenuItem.addItemListener(this);

    blackMenuItem.setState(true);
    colorMenu.add(blackMenuItem);
    blackMenuItem.addItemListener(this);
    colorMenu.add(blueMenuItem);
    blueMenuItem.addItemListener(this);
    colorMenu.add(redMenuItem);
    redMenuItem.addItemListener(this);
    colorMenu.add(grayMenuItem);
    grayMenuItem.addItemListener(this);
    colorMenu.add(magentaMenuItem);
    magentaMenuItem.addItemListener(this);

    editMenu.add(clearMenuItem);
    clearMenuItem.addActionListener(this);
    editMenu.add(repackMenuItem);
    repackMenuItem.addActionListener(this);

    Container cpane = this.getContentPane();
    cpane.add("Center", map);
    JMenuBar bar = new JMenuBar();
    this.setJMenuBar(bar);
    bar.add(this.editMenu);
    bar.add(this.glyphMenu);
    bar.add(this.colorMenu);
    clearMap();
  }

  public String getAppletInfo() {
    return "Simple Map Drawing Program - genoviz Software, Inc.";
  }

  public void deselectGlyphMenu() {
    arrowMenuItem.setState(false);
    boundedPointMenuItem.setState(false);
    centeredCircleMenuItem.setState(false);
    fillOvalMenuItem.setState(false);
    fillRectMenuItem.setState(false);
    lineContainerMenuItem.setState(false);
    outlineRectMenuItem.setState(false);
    squiggleMenuItem.setState(false);
    triBarGlyphMenuItem.setState(false);
    triangleMenuItem.setState(false);
  }

  public void deselectColorMenu() {
    blackMenuItem.setState(false);
    blueMenuItem.setState(false);
    redMenuItem.setState(false);
    grayMenuItem.setState(false);
    magentaMenuItem.setState(false);
  }

  public void itemStateChanged(ItemEvent evt) {
    Object src = evt.getSource();
    int state = evt.getStateChange();
    MapGlyphFactory fac = map.getFactory();
    if (state == ItemEvent.SELECTED)  {
      if (src == arrowMenuItem) {
	deselectGlyphMenu();
	fac.setGlyphtype(com.affymetrix.genoviz.glyph.ArrowGlyph.class);
      }
      else if (src == boundedPointMenuItem) {
	deselectGlyphMenu();
	fac.setGlyphtype(com.affymetrix.genoviz.glyph.BoundedPointGlyph.class);
      }
      else if (src == centeredCircleMenuItem) {
	deselectGlyphMenu();
	fac.setGlyphtype(com.affymetrix.genoviz.glyph.CenteredCircleGlyph.class);
      }
      else if (src == fillOvalMenuItem) {
	deselectGlyphMenu();
	fac.setGlyphtype(com.affymetrix.genoviz.glyph.FillOvalGlyph.class);
      }
      else if (src == fillRectMenuItem) {
	deselectGlyphMenu();
	fac.setGlyphtype(com.affymetrix.genoviz.glyph.FillRectGlyph.class);
      }
      else if (src == lineContainerMenuItem) {
	deselectGlyphMenu();
	fac.setGlyphtype(com.affymetrix.genoviz.glyph.LineContainerGlyph.class);
      }
      else if (src == outlineRectMenuItem) {
	deselectGlyphMenu();
	fac.setGlyphtype(com.affymetrix.genoviz.glyph.OutlineRectGlyph.class);
      }
      else if (src == squiggleMenuItem) {
	deselectGlyphMenu();
	fac.setGlyphtype(com.affymetrix.genoviz.glyph.SquiggleGlyph.class);
      }
      else if (src == triBarGlyphMenuItem) {
	deselectGlyphMenu();
	fac.setGlyphtype(com.affymetrix.genoviz.glyph.TriBarGlyph.class);
      }
      else if (src == triangleMenuItem) {
	deselectGlyphMenu();
	fac.setGlyphtype(com.affymetrix.genoviz.glyph.TriangleGlyph.class);
      }

      // Colors
      else if (src == blackMenuItem) {
	deselectColorMenu();
	fac.setBackgroundColor(Color.black);
      }
      else if (src == blueMenuItem) {
	deselectColorMenu();
	fac.setBackgroundColor(Color.blue);
      }
      else if (src == redMenuItem) {
	deselectColorMenu();
	fac.setBackgroundColor(Color.red);
      }
      else if (src == grayMenuItem) {
	deselectColorMenu();
	fac.setBackgroundColor(Color.gray);
      }
      else if (src == magentaMenuItem) {
	deselectColorMenu();
	fac.setBackgroundColor(Color.magenta);
      }
    }
  }

  public void clearMap()  {
    map.clearWidget();
    map.setMapRange(0, 1000);
    map.setMapOffset(-300, 300);
    map.addAxis(0);
    map.updateWidget();
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == clearMenuItem) {
      clearMap();
    }
    else if (src == repackMenuItem) {
      map.repack();
      map.updateWidget();
    }
  }

  public void rubberBandChanged(NeoRubberBandEvent theEvent) {
    // Here we react to a rubberband.
    if (theEvent.getID() == NeoRubberBandEvent.BAND_END
	&& map.NO_SELECTION != map.getSelectionEvent())
      {
	NeoRubberBandEvent bandevent = (NeoRubberBandEvent)theEvent;
	Rectangle pixelBox = bandevent.getPixelBox();
	pixelBox.setSize(pixelBox.width+1, pixelBox.height+1);
	int fuzziness = map.getPixelFuzziness();
	if (fuzziness <= pixelBox.height || fuzziness <= pixelBox.width) {
	  // Rubberband is non-trivial.

	  // Create a glyph to fit.
	  Rectangle2D coordBox = new Rectangle2D();
	  coordBox = this.map.getView().transformToCoords(pixelBox, coordBox);
	  MapGlyphFactory fac = map.getFactory();
	  fac.setOffset((int)coordBox.y);
	  fac.setWidth((int)coordBox.height);
	  map.addItem((int)coordBox.x, (int)(coordBox.x + coordBox.width));
	  map.updateWidget();
	}
      }
  }
 


}
