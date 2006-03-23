package com.affymetrix.igb.tiers;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;

/**
 *  trying to split a tiered map across multiple windows on multiple screens
 *  maybe have one master map and others use it as root?
 */
public class MultiWindowTierMap extends AffyTieredMap implements MouseListener {
  //  Frame, Window, JFrame, JWindow
  boolean USE_SWING = true;
  boolean USE_FRAME = false;

  public static int tile_width = 1024;
  public static int tile_height = 768;
  public static int tile_columns = 4;
  public static int tile_rows = 2;
  public static int xbump = 0;
  public static int ybump = 0;

  int total_width = tile_width * tile_columns;
  int total_height = tile_height * tile_rows;
  NeoMap[][] child_maps = new NeoMap[tile_columns][tile_rows];
  MultiMapEventHandler child_event_handler;

  // shouldn't need this, but appear to be problems returning listeners normally when
  //   component isn't actually being displayed???
  java.util.List mlisteners = new ArrayList();
  // java.util.List child_maps = new ArrayList();
  //  LinearTransform temp_trans = new LinearTransform();

  public MultiWindowTierMap(boolean hscroll, boolean vscroll) {
    super(hscroll, vscroll);
    this.setSize(total_width, total_height);
    this.getNeoCanvas().setSize(total_width, total_height);
    child_event_handler = new MultiMapEventHandler(this);
    initMultiWindows();
  }

  public void updateWidget() { updateWidget(false); }
  public void updateWidget(boolean full_update) {
    //    super.updateWidget(full_update);
    // set views of child maps based on this map's view
    ViewI rootview = this.getView();

    // transform scale is pixels/coord, transform offset is in pixels
    LinearTransform trans = (LinearTransform)rootview.getTransform();
    double xscale = trans.getScaleX();
    double yscale = trans.getScaleY();
    double xoffset = trans.getOffsetX();
    double yoffset = trans.getOffsetY();
    //    temp_trans.copyTransform(trans);
    // figure out total coords in view -- should be able to just get coordbox?
    //    Rectangle2D view_cbox = rootview.getCoordBox();
    //    Rectangle view_pbox = rootview.getPixelBox();

    //int map_count = child_maps.size();
    int map_count = tile_columns * tile_rows;
    // update all the child maps
    for (int x=0; x<tile_columns; x++) {
      //      NeoMap cmap = (NeoMap)child_maps.get(i);
      for (int y=0; y<tile_rows; y++) {
	NeoMap cmap = child_maps[x][y];
        // System.out.println("map: " + cmap);
        ViewI cview = cmap.getView();
        // System.out.println("view: " + cview);
        LinearTransform ctrans = (LinearTransform)cview.getTransform();
	ctrans.setScaleX(xscale);
	ctrans.setScaleY(yscale);
	ctrans.setOffsetX(xoffset - (x * tile_width));
	ctrans.setOffsetY(yoffset - (y * tile_height));
	cmap.updateWidget();
      }
    }
  }


  public void initMultiWindows() {
    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice default_device = genv.getDefaultScreenDevice();
    Rectangle maxbounds = genv.getMaximumWindowBounds();
    Point center = genv.getCenterPoint();
    GraphicsDevice[] devices = genv.getScreenDevices();
    System.out.println("*********  Graphics Configuration *********");
    System.out.println("Graphics Environment: " + genv);
    System.out.println("Screen Devices: " + devices.length);
    System.out.println("Default Screen device: " + default_device);
    System.out.println("max bounds: " + maxbounds);
    System.out.println("center point: " + center);

    int screen_count = 0;
    for (int i=0; i<devices.length; i++)  {
      int type = devices[i].getType();
      if (type == GraphicsDevice.TYPE_RASTER_SCREEN) {  screen_count++; }
    }
//    int xwindows_per_screen = tile_columns / screen_count;
//    int ywindows_per_screen = tile_rows;
    int xwindows_per_screen = 1;
    int ywindows_per_screen = 1;

    Map bounds2config = new LinkedHashMap();
    for (int i=0; i<devices.length; i++)  {
      GraphicsDevice dev = devices[i];
      String id = dev.getIDstring();
      int type = dev.getType();
      if (type == GraphicsDevice.TYPE_RASTER_SCREEN) {
	GraphicsConfiguration gconfig = dev.getDefaultConfiguration();
	Rectangle config_bounds = gconfig.getBounds();
	bounds2config.put(config_bounds, gconfig);
	boolean fullscreen = dev.isFullScreenSupported();
	boolean change = dev.isDisplayChangeSupported();
	int avail_accelmem = dev.getAvailableAcceleratedMemory() / 1000000;

	System.out.print("Graphics Device " + i + ", id =  " + id + " : " );
	if (type == GraphicsDevice.TYPE_RASTER_SCREEN) { System.out.print("RASTER\n"); }
	else if (type == GraphicsDevice.TYPE_IMAGE_BUFFER) { System.out.print("BUFFER\n"); }
	else if (type == GraphicsDevice.TYPE_PRINTER) { System.out.print("PRINTER\n"); }

	System.out.println("   default config: " + gconfig);
	System.out.println("   bounds: " + config_bounds);
	System.out.println("   available VRAM: " + avail_accelmem + " MB");
	System.out.println("   full screen support: " + fullscreen);
	System.out.println("   display change support: " + change);
      }
    }
    for (int x=0; x<tile_columns; x++) {
      for (int y=0; y<tile_rows; y++) {
	Point topleft = new Point((x * tile_width) + xbump, (y * tile_height) + ybump);
	Rectangle win_bounds = new Rectangle((x * tile_width) + xbump, (y * tile_height) + ybump,
					 tile_width, tile_height);
	Iterator iterbounds = bounds2config.keySet().iterator();
	while (iterbounds.hasNext()) {
	  Rectangle screen_bounds = (Rectangle)iterbounds.next();
	  if (win_bounds.intersects(screen_bounds)) {
	    // found the right screen for this window
            GraphicsConfiguration gconfig = (GraphicsConfiguration)bounds2config.get(screen_bounds);
	    Container win;
	    if (USE_SWING) {
	      if (USE_FRAME) { win = new JFrame(gconfig); }
	      else { win = new JWindow(gconfig); }
	    }
	    else {
	      if (USE_FRAME) { win = new Frame(gconfig); }
	      else { win = new Window(IGB.getSingletonIGB().getFrame(), gconfig); }
	    }
	    win.setLocation(win_bounds.x, win_bounds.y);
	    win.setSize(win_bounds.width, win_bounds.height);
	    NeoMap newmap = new NeoMap(false, false);
	    newmap.setRoot(this);
	    if (USE_SWING) {  // win is a JWindow or JFrame
	      Container cpane;
	      if (USE_FRAME) {  // win is a JFrame
		JFrame frm = (JFrame)win;
		cpane = frm.getContentPane();
	      }
	      else {  // win is a JWindow
		JWindow jwin = (JWindow)win;
		cpane = jwin.getContentPane();
	      }
	      cpane.setLayout(new BorderLayout());
	      cpane.add("Center", newmap);
	    }
	    else {  // win is a Window or Frame
	      win.setLayout(new BorderLayout());
	      win.add("Center", newmap);
	    }
	    newmap.getNeoCanvas().setDoubleBuffered(false);
            newmap.addMouseListener(child_event_handler);
	    //	    newmap.setScrollIncrementBehavior(newmap.X, newmap.AUTO_SCROLL_HALF_PAGE);
            child_maps[x][y] = newmap;
            System.out.println("added map : " + child_maps[x][y]);
	    win.show();
	    break;
	  }
	}
      }
    }

    System.out.println("*******************************************");
  }

  public void transformMouseEvent(MouseEvent evt) {
    // if NeoMouseEvent on one of the child maps, coord position for event should be correct
    //   (since each child map has a proper view), though pixels position will be position in
    //   child canvas and therefore most likely wrong.
    // To fix pixels should be able to just do a reverse transform with view of parent (this) map?
    //    System.out.println("MultiWindowTierMap.transformMouseEvent() called");
    if (evt instanceof NeoMouseEvent) {
      NeoMouseEvent cevt = (NeoMouseEvent)evt;
      // make new event with this (parent) map as source
      NeoMouseEvent pevt =
	//	new NeoMouseEvent((MouseEvent)cevt.getOriginalEvent(), this, cevt.getCoordX(), cevt.getCoordY());
	new NeoMouseEvent(cevt, this, cevt.getCoordX(), cevt.getCoordY());
      // post new event to any listeners on this map
      //      System.out.println("posting new event: " + pevt);

      // First tried just calling processMouseEvent(), but apparently that doesn't actually end up
      //    calling the mouse listeners if this component is not actually being rendered itself
      // Therefore setting up extra list to keep track of listeners and notify them here
      //  this.processMouseEvent(pevt);
      for (int i=0; i<mlisteners.size(); i++) {
	MouseListener listener = (MouseListener)mlisteners.get(i);
	//	System.out.println("listener: " + listener);
        if (listener != null) {
	  int id = pevt.getID();
	  switch(id) {
	  case MouseEvent.MOUSE_PRESSED:
	    listener.mousePressed(pevt);
	    break;
	  case MouseEvent.MOUSE_RELEASED:
	    listener.mouseReleased(pevt);
	    break;
	  case MouseEvent.MOUSE_CLICKED:
	    listener.mouseClicked(pevt);
	    break;
	  case MouseEvent.MOUSE_EXITED:
	    listener.mouseExited(pevt);
	    break;
	  case MouseEvent.MOUSE_ENTERED:
	    listener.mouseEntered(pevt);
	    break;
	  }
	}
      }
    }
  }

  public void addMouseListener(MouseListener listener) {
    System.out.println("-------- adding mouse listener to MultiWindowTierMap: " + listener);
    super.addMouseListener(listener);
    mlisteners.add(listener);
  }

  public void removeMouseListener(MouseListener listener) {
    super.removeMouseListener(listener);
    mlisteners.remove(listener);
  }

}

class MultiMapEventHandler implements MouseListener {
  MultiWindowTierMap main_map;
  public MultiMapEventHandler(MultiWindowTierMap map) {
    main_map = map;
  }
  public void mouseEntered(MouseEvent evt) { main_map.transformMouseEvent(evt); }
  public void mouseExited(MouseEvent evt) { main_map.transformMouseEvent(evt); }
  public void mouseClicked(MouseEvent evt) { main_map.transformMouseEvent(evt); }
  public void mousePressed(MouseEvent evt) { main_map.transformMouseEvent(evt); }
  public void mouseReleased(MouseEvent evt) { main_map.transformMouseEvent(evt); }
    //    if (isOurPopupTrigger(evt)) {
    //      smv.showPopup((NeoMouseEvent) evt);
    //    }

}
