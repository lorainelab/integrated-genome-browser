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

package demo.genoviz;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import demo.genoviz.datamodel.*;
import demo.genoviz.adapter.*;

import com.affymetrix.genoviz.awt.NeoScrollbar;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.MapGlyphFactory;
import com.affymetrix.genoviz.bioviews.NeoDataAdapterI;
import com.affymetrix.genoviz.bioviews.Rectangle2D;
import com.affymetrix.genoviz.bioviews.SceneI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRubberBandEvent;
import com.affymetrix.genoviz.event.NeoRubberBandListener;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.NeoMapI;
import com.affymetrix.genoviz.widget.NeoMapCustomizer;

public class NeoMapDemo extends Applet
implements WindowListener, MouseListener, ActionListener,
  ItemListener, KeyListener, NeoRubberBandListener
{
  NeoMap map;
  Adjustable xzoomer, yzoomer;
  Frame mapframe, zoomframe, propframe;

  Menu fileMenu, editMenu, optionsMenu, selectionMenu, reshapeMenu,
       zoomingMenu, fuzzinessMenu;

  MenuItem printMenuItem, exitMenuItem, clearMenuItem, deleteMenuItem,
      hideMenuItem, unhideMenuItem, optionsMenuItem;

  CheckboxMenuItem noSelection, highlightSelection, outlineSelection,
                   selectRed, selectOrange, selectYellow;
  CheckboxMenuItem fitHorizontallyMenuItem, fitVerticallyMenuItem;
  CheckboxMenuItem zoomTopMenuItem, zoomMiddleMenuItem, zoomBottomMenuItem,
                   zoomLeftMenuItem, zoomCenterMenuItem, zoomRightMenuItem;
  CheckboxMenuItem sharpPrecisionMenuItem, normalPrecisionMenuItem,
                   fuzzyPrecisionMenuItem;

  int pixel_width = 600;
  int pixel_height = 300;
  int seq_start = -200;
  int seq_end = 1050;
  int offset_start = -100;
  int offset_end = 100;

  Vector hidden = new Vector();
  Color selectionColor = Color.red;
  int selectionType = SceneI.SELECT_FILL;

  Image backgroundImage = null;
  boolean clicking = false;
  NeoMapCustomizer customizer;
  boolean framesShowing = true;
  boolean going = false;
  Color nicePaleBlue = new Color(180, 250, 250);
  boolean use_neozoomers = true;

  Label placeholder;

  public void init() {

    String param;

    param = getParameter("background");
    if (null != param) {
      backgroundImage = this.getImage(this.getDocumentBase(), param);
    }

    if (null == backgroundImage) {
      placeholder =
        new Label("Running genoviz NeoMap Demo", Label.CENTER);
      this.setLayout(new BorderLayout());
      this.add("Center", placeholder);
      placeholder.setBackground(nicePaleBlue);
    }

    param = getParameter("show");
    if (null != param) {
      if (param.equalsIgnoreCase("onclick")) {
        clicking = true;
        framesShowing = false;
      }
    }
  }


  protected void getGoing() {

    going = true;
    map = new NeoMap(true, true);
    map.setMapColor(nicePaleBlue);

    /**
     *  Use the NeoMap's built-in selection behavior.
     */
    map.setSelectionEvent(NeoMapI.ON_MOUSE_DOWN);

    /**
     *  Specify selection appearance and color (though map will use defaults if not specified)
     */
    map.setSelectionAppearance(selectionType);
    map.setSelectionColor(selectionColor);

    // setting the coordinates of the linear map (in this case the start and
    //     end of the sequence)
    map.setMapRange(seq_start, seq_end);

    // setting the range of possible offset values (coordinates
    //    perpendicular to the linear map)
    map.setMapOffset(offset_start, offset_end);

    /**
     *  The map widget can be assigned Adjustables to control zooming
     *  along the axis (range) and perpendicular to the axis (offset).
     *  Once these are set up, changes to the Adjustable change the
     *  scale and offset of the map, and calls to the map's zoom
     *  methods change the Adjustables.
     */
    if (use_neozoomers) {  // use NeoScrollbars for zoom controls
      xzoomer = new NeoScrollbar(NeoScrollbar.HORIZONTAL);
      yzoomer = new NeoScrollbar(NeoScrollbar.HORIZONTAL);
    }
    else {  // use standard java.awt.Scrollbars for zoom controls
      xzoomer = new Scrollbar(Scrollbar.HORIZONTAL);
      yzoomer = new Scrollbar(Scrollbar.HORIZONTAL);
    }

    map.setZoomer(map.X, xzoomer);
    map.setZoomer(map.Y, yzoomer);

    // Place an axis along the center of the map.
    map.addAxis(0);

    addItemsDirectly();  // examples of adding items directly to map
    addItemsWithFactory();  // examples of adding items using factories
    addItemsWithDataAdapter();  // examples of adding items using data adapters
    addSequence();  // manipulation of a more sophisticated glyph

    /**
     *  Set up a separate frame for the zoom bar,
     *  just to emphasize its independence from the rest of the widget.
     */
    setupZoomFrame();
    mapframe = new Frame("genoviz NeoMap Demo");

    mapframe.setLayout(new BorderLayout());
    setupMenus(mapframe);

    /**
     *  All NeoWidgets in this release are lightweight components.
     *  Placing a lightweight component inside a standard Panel often
     *  causes flicker in the repainting of the lightweight components.
     *  Therefore the GenoViz includes the NeoPanel, a special subclass
     *  of Panel, that is designed to support better repainting of
     *  NeoWidgets contained withing it.  Note however that if you are
     *  using the widgets within a lightweight component framework
     *  (such as Swing), you should _not_ wrap them with a NeoPanel
     *  (since the NeoPanel is a heavyweight component).
     *
     *  In order for the map to automatically respond to resize events
     *  by filling the available space, it is highly recommended a
     *  BorderLayout (or similarly flexible layout manager) be used for the
     *  Container that holds the NeoMap, and add the NeoMap in the center.
     */

    Panel map_pan;
    map_pan = new NeoPanel();
    map_pan.setLayout(new BorderLayout());
    map_pan.add("Center", map);
    mapframe.add("Center", map_pan);

    mapframe.setSize(pixel_width, pixel_height);

    mapframe.addWindowListener(this);
    map.addMouseListener(this);
    map.addRubberBandListener(this);
    map.addKeyListener(this);
  }

  public void addItemsDirectly() {

    /**
     *  --------------- Adding items directly to Map ---------------
     *  Configure the map via a "-tag value" string
     *  All of the options here configure the display of items that
     *  are later added to the map.  For all possible options, the
     *  map has built-in defaults.  The options in the configuration
     *  string override the defaults -- any options not listed in the
     *  configuration string remain unchanged.  The configuration options
     *  used here:
     *
     *    -offset   specifies the offset perpendicular to the axis
     *                (in offset coordinates, not pixels)
     *    -color    specifies a color by name, this name hashes to
     *                a specific color in the global color map,
     *                GeneralUtils.getColorMap()
     *    -glyph    specifies the class of the visual icon to use to
     *                represent map items
     *    -width    specifies the width of the item perpendicular to the
     *                axis (in offset coordinates, not pixels)
     */

    map.configure("-offset -30 -color blue " +
        "-glyphtype BoundedPointGlyph -width 5");

    /**
     *  Add an annotation to the map from base 30 to base 100,
     *  according to how the map is currently configured
     */
    map.addItem(30, 100);

    map.configure("-glyphtype FillOvalGlyph");
    map.addItem(400, 450);
    map.configure("-glyphtype CenteredCircleGlyph");
    map.addItem(400,450);

    map.configure("-glyphtype ArrowGlyph");
    map.configure("-offset -10 -color green -width 10");
    map.addItem(300, 500);
    map.addItem(700, 600);
    map.addItem(800, 900);
    map.addItem(800,900);

    // adding items to other items
    map.configure("-glyphtype LineContainerGlyph -offset 0 -width 10 -color green");
    GlyphI gene_item = map.addItem(100, 700);
    map.configure("-glyphtype FillRectGlyph -color blue");
    map.addItem(gene_item, map.addItem(200, 300));
    map.addItem(gene_item, map.addItem(350, 375));
    map.addItem(gene_item, map.addItem(600, 700));

    map.configure("-offset -60");
    map.addItem(480, 540);

    map.configure("-glyphtype FillRectGlyph -color lightGray -packer null");

  }

  /**
   *  ------------------- Factories -------------------
   *  Different configurations for adding items to maps can be stored
   *  in the map as factories.
   *  A map can have any number of factories, each of which
   *  can be configured to produce glyphs on the map with color,
   *  offset perpendicular to the axis, glyph class, etc. set by
   *  configuration methods similar to those of the map itself.
   *  Each factory has a String name, and can be referred to by name.
   */
   protected void addItemsWithFactory () {
    // add a factory named "factory1" to the map
    MapGlyphFactory fac1 = map.addFactory(
        "-color test1 -offset -50 " + "-width 5");
    MapGlyphFactory fac2 = map.addFactory(
        "-offset -80 -color magenta -glyphtype ArrowGlyph");
    // add an item to the map from 500 to 100 using factory fac1
    map.addItem(fac1, 500, 100);

    // Factories can also be called directly via makeItem()
    fac2.makeItem(500, 400);
    fac2.makeItem(300, 200, "-color white");
    fac2.configure("-offset 50 -width 5 -glyphtype OutlineRectGlyph");
    fac2.makeItem(700, 900);
  }

  /**
   *  ----------------- Data Adapters -----------------
   *  Adding items directly to the map or through the map's
   *  factories by specifying a start and end position works for simple
   *  annotations.  For more complex annotations, classes that implement the
   *  NeoDataAdapterI interface provide a more general mechanism to
   *  represent data models on the map.  However, making a data adapter
   *  class requires knowledge of both the data model one wants to represent
   *  and more of the internals of the map widget.
   *
   *  Data Adapters can be used to automate building a visual
   *  representation of datamodels.  Each data adapter is specific
   *  to a particular class of data model.  Once a data adapter has
   *  been constructed and added to the map, calling Map.addData(model)
   *  will represent the model in the map in a manner specific to the
   *  data adapter and its configuration.
   */
  protected void addItemsWithDataAdapter() {

    NeoDataAdapterI adapter;

    // Annotation constructor takes as arguments: start, end
    Annotation annots[] = {
      new Annotation(100, 300),
      new Annotation(400, 500)
    };

    // ScoredAnnotation constructor takes as arguments: start, end, score
    ScoredAnnotation scoredannots[] = {
      new ScoredAnnotation(500, 550, 0.2),
      new ScoredAnnotation(600, 650, 0.4),
      new ScoredAnnotation(700, 750, 0.6),
      new ScoredAnnotation(800, 850, 0.8),
      new ScoredAnnotation(900, 950, 1.0),
    };

    /**
     *  ScoredAnnotAdapter uses the score of an annotation to
     *  determine a grayscale value to color the glyph representing it
     *  Unless told otherwise, it assumes a scoring system that
     *  ranges from 0.0 to 1.0, and maps this along a linear color
     *  scale, from 0.0 = black to 1.0 = white [values outside this
     *  range will give unpredictable results]
     */
    adapter = new ScoredAnnotAdapter();
    adapter.configure("-offset -70");
    map.addDataAdapter(adapter);

    /**
     *  AnnotationAdapter is a very simple adapter for representing annotations.
     *  Annotations implement the interface AnnotationI,
     *  which has a getStart() and getEnd() method
     */
    adapter = new AnnotationAdapter();
    adapter.configure("-offset 20 -glyphtype SquiggleGlyph -width 10");
    map.addDataAdapter(adapter);

    // add an array of Annotations to the map
    for (int i=0; i<annots.length; i++) {
      map.addData(annots[i]);
    }

    // add an array of ScoredAnnotations to the map
    for (int i=0; i<scoredannots.length; i++) {
      map.addData(scoredannots[i]);
    }


  }

  /**
   *  Creating a sequence item, and adding visible annotations to it
   */
  protected void addSequence () {
    map.configure("-offset -88 -color yellow -glyphtype SequenceGlyph " +
                  "-width 16");
    String theSeq = "ACGTACGTACGTACTGACTGTTTTTAAAAAAATATATATATGAATTCGGG";
    SequenceGlyph sg =
      (SequenceGlyph)map.addItem(500, 500 + theSeq.length()-1);
    sg.setResidues(theSeq);

    // Add some boxes to the sequence glyph
    map.configure("-color green");
    map.addItem(sg, map.addItem(501, 505));
    map.configure("-color blue");
    map.addItem(sg, map.addItem(541, 546));

  }

  private void showFrames() {
    if (!going) {
      getGoing();
    }
    Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
    mapframe.setLocation((screen_size.width-pixel_width)/2,
        (screen_size.height-pixel_height)/2);
    Rectangle mapframesize = mapframe.getBounds();
    zoomframe.setLocation ( mapframesize.x + mapframesize.width -
        zoomframe.getBounds().width - 20,
        mapframesize.y + 35);
    mapframe.show();
    zoomframe.show();
  }

  private void hideFrames() {
    if (null != propframe)
      propframe.setVisible(false);
    if (null != zoomframe)
      zoomframe.setVisible(false);
    if (null != mapframe)
      mapframe.setVisible(false);
  }

  public void start() {
    if (framesShowing) {
      showFrames();
    }
  }

  public void stop() {
    hideFrames();
  }

  public void destroy() {
    if ( this.mapframe != null )  {
      this.mapframe.setVisible( false);
      this.mapframe.dispose();
      this.mapframe = null;
    }
    if ( this.zoomframe != null ) {
      this.zoomframe.setVisible( false );
      this.zoomframe.dispose();
      this.zoomframe = null;
    }
    if ( this.propframe != null ) {
      this.propframe.setVisible( false );
      this.propframe.dispose();
      this.propframe = null;
    }
    super.destroy();
  }

  public String getAppletInfo() {
    return ("Demonstration of genoviz Software's Map Widget");
  }


  /**
   * We set up a separate Frame for our zoom controls.
   * They are just a couple Scrollbars.
   */
  public void setupZoomFrame() {
    zoomframe = new Frame("Map Zoom Controls");
    zoomframe.setBackground(Color.white);
    zoomframe.setLayout(new BorderLayout());

    /**
     *  NeoScrollbars are lightweight components in this release.
     *  To avoid problems with lightweight component repainting,
     *  they are placed within a NeoPanel
     */
    Panel zoom_pan = new NeoPanel();
    zoom_pan.setLayout(new BorderLayout());
    zoom_pan.add("South", (Component)xzoomer);
    zoom_pan.add("North", (Component)yzoomer);
    zoomframe.add("Center", zoom_pan);

    zoomframe.pack();
    zoomframe.setSize(200, zoomframe.getSize().height);
    zoomframe.addWindowListener(this);
  }

  public void setupMenus(Frame dock) {

    fileMenu = new Menu("File");
    printMenuItem = new MenuItem("Print...");
    exitMenuItem   = new MenuItem("Exit");
    fileMenu.add(printMenuItem);
    fileMenu.addSeparator();
    fileMenu.add(exitMenuItem);
    printMenuItem.addActionListener(this);
    exitMenuItem.addActionListener(this);


    editMenu = new Menu("Edit");
    deleteMenuItem = new MenuItem("Delete");
    clearMenuItem = new MenuItem("Delete All");
    hideMenuItem = new MenuItem("Hide");
    unhideMenuItem = new MenuItem("Show Hidden");
    optionsMenuItem = new MenuItem("Properties...");
    editMenu.add(deleteMenuItem);
    editMenu.add(clearMenuItem);
    editMenu.add(hideMenuItem);
    editMenu.add(unhideMenuItem);
    editMenu.addSeparator();
    editMenu.add(optionsMenuItem);
    deleteMenuItem.addActionListener(this);
    clearMenuItem.addActionListener(this);
    hideMenuItem.addActionListener(this);
    unhideMenuItem.addActionListener(this);
    optionsMenuItem.addActionListener(this);

    selectionMenu = new Menu("Selection");
    noSelection = new CheckboxMenuItem("none");
    highlightSelection = new CheckboxMenuItem("highlighted");
    outlineSelection = new CheckboxMenuItem("outlined");
    selectRed = new CheckboxMenuItem("red");
    selectOrange = new CheckboxMenuItem("orange");
    selectYellow = new CheckboxMenuItem("yellow");
    highlightSelection.setState(true);
    selectRed.setState(true);
    selectionMenu.add(noSelection);
    selectionMenu.add(highlightSelection);
    selectionMenu.add(outlineSelection);
    selectionMenu.addSeparator();
    selectionMenu.add(selectYellow);
    selectionMenu.add(selectOrange);
    selectionMenu.add(selectRed);
    noSelection.addItemListener(this);
    highlightSelection.addItemListener(this);
    outlineSelection.addItemListener(this);
    selectYellow.addItemListener(this);
    selectOrange.addItemListener(this);
    selectRed.addItemListener(this);

    reshapeMenu = new Menu("Reshaping");
    fitHorizontallyMenuItem = new CheckboxMenuItem("Fit Horizontally");
    fitVerticallyMenuItem = new CheckboxMenuItem("Fit Vertically");
    fitHorizontallyMenuItem.setState(true);
    fitVerticallyMenuItem.setState(true);
    reshapeMenu.add(fitHorizontallyMenuItem);
    reshapeMenu.add(fitVerticallyMenuItem);
    fitHorizontallyMenuItem.addItemListener(this);
    fitVerticallyMenuItem.addItemListener(this);

    zoomingMenu = new Menu("Zoom from");
    zoomTopMenuItem = new CheckboxMenuItem("Top");
    zoomMiddleMenuItem = new CheckboxMenuItem("Middle");
    zoomBottomMenuItem = new CheckboxMenuItem("Bottom");
    zoomLeftMenuItem = new CheckboxMenuItem("Left");
    zoomCenterMenuItem = new CheckboxMenuItem("Center");
    zoomRightMenuItem = new CheckboxMenuItem("Right");
    zoomMiddleMenuItem.setState(true);
    zoomCenterMenuItem.setState(true);
    zoomingMenu.add(zoomTopMenuItem);
    zoomingMenu.add(zoomMiddleMenuItem);
    zoomingMenu.add(zoomBottomMenuItem);
    zoomingMenu.addSeparator();
    zoomingMenu.add(zoomLeftMenuItem);
    zoomingMenu.add(zoomCenterMenuItem);
    zoomingMenu.add(zoomRightMenuItem);
    zoomTopMenuItem.addItemListener(this);
    zoomMiddleMenuItem.addItemListener(this);
    zoomBottomMenuItem.addItemListener(this);
    zoomLeftMenuItem.addItemListener(this);
    zoomCenterMenuItem.addItemListener(this);
    zoomRightMenuItem.addItemListener(this);

    fuzzinessMenu = new Menu("Pointer Precision");
    sharpPrecisionMenuItem = new CheckboxMenuItem("Sharp");
    normalPrecisionMenuItem = new CheckboxMenuItem("Normal");
    fuzzyPrecisionMenuItem = new CheckboxMenuItem("Fuzzy");
    normalPrecisionMenuItem.setState(true);
    fuzzinessMenu.add(sharpPrecisionMenuItem);
    fuzzinessMenu.add(normalPrecisionMenuItem);
    fuzzinessMenu.add(fuzzyPrecisionMenuItem);
    sharpPrecisionMenuItem.addItemListener(this);
    normalPrecisionMenuItem.addItemListener(this);
    fuzzyPrecisionMenuItem.addItemListener(this);

    optionsMenu = new Menu("Options");
    optionsMenu.add(selectionMenu);
    optionsMenu.add(reshapeMenu);
    optionsMenu.add(zoomingMenu);
    optionsMenu.add(fuzzinessMenu);

    MenuBar bar = dock.getMenuBar();
    if (null == bar) {
      bar = new MenuBar();
      dock.setMenuBar(bar);
    }
    bar.add(fileMenu);
    bar.add(editMenu);
    bar.add(optionsMenu);
  }

  public void paint(Graphics g) {
    if (null == this.backgroundImage) {
      super.paint(g);
    }
    else {
      g.drawImage(this.backgroundImage, 0, 0, this.getSize().width, this.getSize().height, this);
    }
  }

  /** EventListener interface implementations: */

  /** MouseListener interface implementation */

  public void mouseClicked(MouseEvent e) { }
  public void mouseEntered(MouseEvent e) { }
  public void mouseExited(MouseEvent e) { }
  public void mouseReleased(MouseEvent e) {
    if (clicking) {
      if (framesShowing) {
        hideFrames();
      }
      else {
        showFrames();
      }
      framesShowing = !framesShowing;
    }
  }
  public void mousePressed(MouseEvent e) {
    Object source = e.getSource();
    if (!(e instanceof NeoMouseEvent)) { return; }
    NeoMouseEvent nme = (NeoMouseEvent)e;
    Object coord_source = nme.getSource();
    if (coord_source == map && e instanceof NeoMouseEvent) {
      // Make the selected item the center of zooming.
      map.setZoomBehavior(map.X, map.CONSTRAIN_COORD, nme.getCoordX());
    }
  }

  /** NeoRubberBandListener interface implementation */

  public void rubberBandChanged(NeoRubberBandEvent e) {
    int id = e.getID();
    if (id == e.BAND_END && map.getSelectionEvent() != map.NO_SELECTION) {
      // Here we add some selection by rubberband.
      Rectangle pixelBox = e.getPixelBox();
      pixelBox.setSize(pixelBox.width+1, pixelBox.height+1);
      int fuzziness = map.getPixelFuzziness();
      if (fuzziness <= pixelBox.height || fuzziness <= pixelBox.width) {
        // Rubberband is non-trivial.
        // Select items within it.
        Vector items = map.getItems(pixelBox);
        if (!e.isShiftDown()) {
          map.deselect(map.getSelected());
        }
        GlyphI gl;
        for (int i=0; i<items.size(); i++) {
          gl = (GlyphI)items.elementAt(i);
          if (gl.isSelectable()) {
            map.select(gl);
          }
        }
        map.updateWidget();
      }
    }
  }

  /** ActionListener interface implementation */

  public void actionPerformed(ActionEvent e) {
    Object theItem = e.getSource();
    if (theItem == printMenuItem) {
      printMap();
    }
    else if (theItem == exitMenuItem) {
      this.stop();
    }
    else if (theItem == deleteMenuItem) {
      map.removeItem(map.getSelected());
      map.updateWidget();
    }
    else if (theItem == clearMenuItem) {
      map.clearWidget();
      map.addAxis(0);
      map.updateWidget();
    }
    else if (theItem == hideMenuItem) {
      // should be able to just do setVisibility(selected, false);
      Enumeration enm = map.getSelected().elements();
      while (enm.hasMoreElements()) {
        GlyphI gl = (GlyphI)enm.nextElement();
        map.setVisibility(gl, false);
        hidden.addElement(gl);
      }
      map.updateWidget();
    }
    else if (theItem == unhideMenuItem) {
      // should be able to just do setVisibility(selected, true);
      Enumeration enm = hidden.elements();
      while (enm.hasMoreElements()) {
        GlyphI gl = (GlyphI)enm.nextElement();
        map.setVisibility(gl, true);
      }
      hidden.removeAllElements();
      map.updateWidget();
    }
    else if (theItem == optionsMenuItem) {
      if (propframe == null) {
        propframe = new Frame("NeoMap Properties");
        customizer = new NeoMapCustomizer();
        customizer.setObject(map);
        propframe.add("Center", customizer);
        propframe.pack();
        propframe.addWindowListener(this);
      }
      propframe.setBounds(200, 200, 500, 300);
      propframe.show();
    }

  }

  /* ItemListener interface implementation */

  public void itemStateChanged(ItemEvent e) {
    Object theItem = e.getSource();
    if (theItem == noSelection) {
      map.deselect(map.getSelected());
      map.updateWidget();
      map.setSelectionEvent(NeoMapI.NO_SELECTION);
      noSelection.setState(true);
      highlightSelection.setState(false);
      outlineSelection.setState(false);
    }
    else if (theItem == highlightSelection) {
      selectionType = SceneI.SELECT_FILL;
      map.setSelectionAppearance(selectionType);
      map.updateWidget();
      map.setSelectionEvent(NeoMapI.ON_MOUSE_DOWN);
      noSelection.setState(false);
      highlightSelection.setState(true);
      outlineSelection.setState(false);
    }
    else if (theItem == outlineSelection) {
      selectionType = SceneI.SELECT_OUTLINE;
      map.setSelectionAppearance(selectionType);
      map.updateWidget();
      map.setSelectionEvent(NeoMapI.ON_MOUSE_DOWN);
      noSelection.setState(false);
      highlightSelection.setState(false);
      outlineSelection.setState(true);
    }
    else if (theItem == selectRed || theItem == selectOrange ||
        theItem == selectYellow) {
      selectRed.setState(false);
      selectOrange.setState(false);
      selectYellow.setState(false);
      ((CheckboxMenuItem)theItem).setState(true);
      if (theItem == selectRed) { selectionColor = Color.red; }
      else if (theItem == selectOrange) { selectionColor = Color.orange; }
      else if (theItem == selectYellow) { selectionColor = Color.yellow; }
      map.setSelectionColor(selectionColor);
      map.updateWidget();
    }
    else if (theItem == fitHorizontallyMenuItem) {
      if (((CheckboxMenuItem)theItem).getState()) {
        map.setReshapeBehavior(NeoMapI.X, NeoMapI.FITWIDGET);
        map.setSize(map.getSize()); // use Component's set/getSize method
        map.updateWidget();
      }
      else {
        map.setReshapeBehavior(NeoMapI.X, NeoMapI.NONE);
      }
    }
    else if (theItem == fitVerticallyMenuItem) {
      if (((CheckboxMenuItem)theItem).getState()) {
        map.setReshapeBehavior(NeoMapI.Y, NeoMapI.FITWIDGET);
        map.setSize(map.getSize()); // use Component's set/getSize method
        map.updateWidget();
      }
      else {
        map.setReshapeBehavior(NeoMapI.Y, NeoMapI.NONE);
      }
    }
    else if (theItem == zoomTopMenuItem) {
      zoomTopMenuItem.setState(true);
      map.setZoomBehavior(NeoMapI.Y, NeoMapI.CONSTRAIN_START);
      zoomMiddleMenuItem.setState(false);
      zoomBottomMenuItem.setState(false);
    }
    else if (theItem == zoomMiddleMenuItem) {
      zoomTopMenuItem.setState(false);
      zoomMiddleMenuItem.setState(true);
      map.setZoomBehavior(NeoMapI.Y, NeoMapI.CONSTRAIN_MIDDLE);
      zoomBottomMenuItem.setState(false);
    }
    else if (theItem == zoomBottomMenuItem) {
      zoomTopMenuItem.setState(false);
      zoomMiddleMenuItem.setState(false);
      map.setZoomBehavior(NeoMapI.Y, NeoMapI.CONSTRAIN_END);
      zoomBottomMenuItem.setState(true);
    }
    else if (theItem == zoomLeftMenuItem) {
      zoomLeftMenuItem.setState(true);
      map.setZoomBehavior(NeoMapI.X, NeoMapI.CONSTRAIN_START);
      zoomCenterMenuItem.setState(false);
      zoomRightMenuItem.setState(false);
    }
    else if (theItem == zoomCenterMenuItem) {
      zoomLeftMenuItem.setState(false);
      map.setZoomBehavior(NeoMapI.X, NeoMapI.CONSTRAIN_MIDDLE);
      zoomCenterMenuItem.setState(true);
      zoomRightMenuItem.setState(false);
    }
    else if (theItem == zoomRightMenuItem) {
      zoomLeftMenuItem.setState(false);
      zoomCenterMenuItem.setState(false);
      map.setZoomBehavior(NeoMapI.X, NeoMapI.CONSTRAIN_END);
      zoomRightMenuItem.setState(true);
    }
    else if (theItem == sharpPrecisionMenuItem) {
      map.setPixelFuzziness(0);
      sharpPrecisionMenuItem.setState(true);
      normalPrecisionMenuItem.setState(false);
      fuzzyPrecisionMenuItem.setState(false);
    }
    else if (theItem == normalPrecisionMenuItem) {
      map.setPixelFuzziness(2);
      sharpPrecisionMenuItem.setState(false);
      normalPrecisionMenuItem.setState(true);
      fuzzyPrecisionMenuItem.setState(false);
    }
    else if (theItem == fuzzyPrecisionMenuItem) {
      map.setPixelFuzziness(5);
      sharpPrecisionMenuItem.setState(false);
      normalPrecisionMenuItem.setState(false);
      fuzzyPrecisionMenuItem.setState(true);
    }
  }

  /* WindowListener interface implementation */

  public void windowActivated(WindowEvent e) {}
  public void windowClosed(WindowEvent e) {}
  public void windowDeactivated(WindowEvent e) {}
  public void windowDeiconified(WindowEvent e) {}
  public void windowIconified(WindowEvent e) {}
  public void windowOpened(WindowEvent e) {}
  public void windowClosing(WindowEvent e) {
    if(e.getSource() == mapframe) {
      this.stop();
    }
    else {
      ((Window)e.getSource()).setVisible(false);
    }
  }

    public void keyPressed(KeyEvent e) { heardKeyEvent(e); }
    public void keyReleased(KeyEvent e) { heardKeyEvent(e); }
    public void keyTyped(KeyEvent e) { heardKeyEvent(e); }

    public void heardKeyEvent(KeyEvent e) {
      System.out.println("NeoMapDemo heard key press: " + e.getKeyChar());
    }


    /** Printing */

    private void printMap () {

      // Obtain a PrintJob and a Graphics object to use with it

      Toolkit toolkit = mapframe.getToolkit();
      PrintJob job    = toolkit.getPrintJob (mapframe, "Print Map",
          new Properties());

      if (job == null)
        return;  // i.e. the user clicked Cancel.

      Graphics g = job.getGraphics();

      // Give the output some margins (avoid scrunching in upper left corner)
      g.translate (50,50);

      Dimension size = mapframe.getSize();

      // Set a clipping region
      g.setClip (0, 0, size.width, size.height);

      // Print the mapframe and the components it contains
      mapframe.printAll (g);

      // Finish up.
      g.dispose();
      job.end();

    }

}
