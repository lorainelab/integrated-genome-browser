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

package tutorial.genoviz;

import com.affymetrix.genoviz.event.NeoRubberBandListener;
import com.affymetrix.genoviz.event.NeoRubberBandEvent;
import com.affymetrix.genoviz.bioviews.Rectangle2D;
import com.affymetrix.genoviz.bioviews.SceneI;

import java.applet.Applet;
import java.awt.CheckboxMenuItem;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

public class NeoDraw extends SimpleMap3
  implements NeoRubberBandListener, ActionListener, ItemListener {

  protected Menu glyphMenu = new Menu("Glyph", true);
  protected CheckboxMenuItem arrowMenuItem = new CheckboxMenuItem("ArrowGlyph");
  protected CheckboxMenuItem boundedPointMenuItem = new CheckboxMenuItem("BoundedPointGlyph");
  protected CheckboxMenuItem centeredCircleMenuItem = new CheckboxMenuItem("CenteredCircleGlyph");
  protected CheckboxMenuItem fillRectMenuItem = new CheckboxMenuItem("FillRectGlyph");
  protected CheckboxMenuItem fillOvalMenuItem = new CheckboxMenuItem("FillOvalGlyph");
  protected CheckboxMenuItem lineContainerMenuItem = new CheckboxMenuItem("LineContainerGlyph");
  protected CheckboxMenuItem outlineRectMenuItem = new CheckboxMenuItem("OutlineRectGlyph");
  protected CheckboxMenuItem squiggleMenuItem = new CheckboxMenuItem("SquiggleGlyph");
  protected CheckboxMenuItem triBarGlyphMenuItem = new CheckboxMenuItem("TriBarGlyph");
  protected CheckboxMenuItem triangleMenuItem = new CheckboxMenuItem("TriangleGlyph");

  protected Menu colorMenu = new Menu("Color", true);
  protected CheckboxMenuItem blackMenuItem = new CheckboxMenuItem("black");
  protected CheckboxMenuItem blueMenuItem = new CheckboxMenuItem("blue");
  protected CheckboxMenuItem redMenuItem = new CheckboxMenuItem("red");
  protected CheckboxMenuItem grayMenuItem = new CheckboxMenuItem("gray");
  protected CheckboxMenuItem magentaMenuItem = new CheckboxMenuItem("magenta");

  protected Menu editMenu = new Menu("Edit");
  protected MenuItem clearMenuItem = new MenuItem("Clear");
  protected MenuItem repackMenuItem = new MenuItem("Repack");

  private PopupMenu popup;

  public NeoDraw() {
    super();

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
  }

  private java.awt.Component cmpnt = this;
  public void init() {

    /* Some browsers and the appletviewer put applets in a Frame.
     * However, the implementations are uneven.
     * We would prefer to use a popup in applets.
     * But, worse still, some browsers freeze the X display when popups are used.
     * Netscape 4.07 did this on Solaris.
     * We cannot have that.
     * Some browsers ignore popups all together.
     * So we try to use the menu bar in a frame.
     * If there is no frame, we resort to trying a popup.
     */
    Container parent;
    parent = this.getParent();
    while (null != parent && ! (parent instanceof Frame)) {
      parent = parent.getParent();
    }
    if (null != parent && parent instanceof Frame) {
      Frame parentFrame = (Frame) parent;
      MenuBar bar = parentFrame.getMenuBar();
      if (null == bar) {
        bar = new MenuBar();
        parentFrame.setMenuBar(bar);
      }
      addMenus(bar);
    }
    else { // We're not in a frame.

      this.popup = new PopupMenu();
      addMenus(popup);
      this.add(popup);
      this.map.addMouseListener( new MouseAdapter() {
        public void mousePressed(MouseEvent evt) {
          Object evtSource = evt.getSource();
          if( evtSource == map && evt.isPopupTrigger() ) {
            popup.show( cmpnt, evt.getX(), evt.getY() );
          }
        }
      });

    }
  }

  public String getAppletInfo() {
    return "Simple Map Drawing Program - genoviz Software, Inc.";
  }

  public void addMenus(MenuBar bar) {
    bar.add(this.editMenu);
    bar.add(this.glyphMenu);
    bar.add(this.colorMenu);
  }
  public void addMenus(Menu m) {
    m.add(this.editMenu);
    m.add(this.glyphMenu);
    m.add(this.colorMenu);
  }

  public void removeMenus(MenuBar bar) {
    bar.remove(this.editMenu);
    bar.remove(this.glyphMenu);
    bar.remove(this.colorMenu);
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

  public void itemStateChanged(ItemEvent theEvent) {

    // Glyphs
    if (theEvent.getSource() == arrowMenuItem) {
      deselectGlyphMenu();
      arrowMenuItem.setState(true);
      this.map.configure("-glyphtype com.affymetrix.genoviz.glyph.ArrowGlyph");
    }
    else if (theEvent.getSource() == boundedPointMenuItem) {
      deselectGlyphMenu();
      boundedPointMenuItem.setState(true);
      this.map.configure("-glyphtype com.affymetrix.genoviz.glyph.BoundedPointGlyph");
    }
    else if (theEvent.getSource() == centeredCircleMenuItem) {
      deselectGlyphMenu();
      centeredCircleMenuItem.setState(true);
      this.map.configure("-glyphtype com.affymetrix.genoviz.glyph.CenteredCircleGlyph");
    }
    else if (theEvent.getSource() == fillOvalMenuItem) {
      deselectGlyphMenu();
      fillOvalMenuItem.setState(true);
      this.map.configure("-glyphtype com.affymetrix.genoviz.glyph.FillOvalGlyph");
    }
    else if (theEvent.getSource() == fillRectMenuItem) {
      deselectGlyphMenu();
      fillRectMenuItem.setState(true);
      this.map.configure("-glyphtype com.affymetrix.genoviz.glyph.FillRectGlyph");
    }
    else if (theEvent.getSource() == lineContainerMenuItem) {
      deselectGlyphMenu();
      lineContainerMenuItem.setState(true);
      this.map.configure("-glyphtype com.affymetrix.genoviz.glyph.LineContainerGlyph");
    }
    else if (theEvent.getSource() == outlineRectMenuItem) {
      deselectGlyphMenu();
      outlineRectMenuItem.setState(true);
      this.map.configure("-glyphtype com.affymetrix.genoviz.glyph.OutlineRectGlyph");
    }
    else if (theEvent.getSource() == squiggleMenuItem) {
      deselectGlyphMenu();
      squiggleMenuItem.setState(true);
      this.map.configure("-glyphtype com.affymetrix.genoviz.glyph.SquiggleGlyph");
    }
    else if (theEvent.getSource() == triBarGlyphMenuItem) {
      deselectGlyphMenu();
      triBarGlyphMenuItem.setState(true);
      this.map.configure("-glyphtype com.affymetrix.genoviz.glyph.TriBarGlyph");
    }
    else if (theEvent.getSource() == triangleMenuItem) {
      deselectGlyphMenu();
      triangleMenuItem.setState(true);
      this.map.configure("-glyphtype com.affymetrix.genoviz.glyph.TriangleGlyph");
    }

    // Colors
    else if (theEvent.getSource() == blackMenuItem) {
      deselectColorMenu();
      blackMenuItem.setState(true);
      this.map.configure("-color black");
    }
    else if (theEvent.getSource() == blueMenuItem) {
      deselectColorMenu();
      blueMenuItem.setState(true);
      this.map.configure("-color blue");
    }
    else if (theEvent.getSource() == redMenuItem) {
      deselectColorMenu();
      redMenuItem.setState(true);
      this.map.configure("-color red");
    }
    else if (theEvent.getSource() == grayMenuItem) {
      deselectColorMenu();
      grayMenuItem.setState(true);
      this.map.configure("-color gray");
    }
    else if (theEvent.getSource() == magentaMenuItem) {
      deselectColorMenu();
      magentaMenuItem.setState(true);
      this.map.configure("-color magenta");
    }

  }

  public void actionPerformed(ActionEvent theEvent) {

    if (theEvent.getSource() == clearMenuItem) {
      this.map.clearWidget();
      this.map.addAxis(0);
      this.map.updateWidget();
    }
    else if (theEvent.getSource() == repackMenuItem) {
      this.map.repack();
      this.map.updateWidget();
    }
    else super.actionPerformed(theEvent);
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
        this.map.configure("-width " + (int)coordBox.height
          + " -offset " + (int)coordBox.y);
        Object o = this.map.addItem((int)coordBox.x, (int)(coordBox.x + coordBox.width));

        map.updateWidget();
      }
    }
  }


  public static void main (String argv[]) {
    NeoDraw me = new NeoDraw();
    Frame f = new Frame("GenoViz");
    f.add("Center", me);

    me.addFileMenuItems(f);
    MenuBar bar = f.getMenuBar();
    if (null == bar) {
      bar = new MenuBar();
      f.setMenuBar(bar);
    }
    me.addMenus(bar);

    f.addWindowListener( new WindowAdapter() {
      public void windowClosing( WindowEvent e ) {
      Window w = (Window) e.getSource();
      w.dispose();
      }
      public void windowClosed( WindowEvent e ) {
      System.exit( 0 );
      }
    } );

    f.pack();
    f.setBounds(20, 40, 400, 300);
    f.show();
    me.start();
  }

}
