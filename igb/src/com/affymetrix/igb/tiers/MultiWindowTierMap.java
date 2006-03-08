package com.affymetrix.igb.tiers;

import java.awt.*;
import javax.swing.*;
import java.util.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.bioviews.*;

/**
 *  trying to split a tiered map across multiple windows on multiple screens
 *  maybe have one master map and others use it as root?
 */
public class MultiWindowTierMap extends AffyTieredMap {
  //  Frame, Window, JFrame, JWindow
  boolean USE_SWING = true;
  boolean USE_FRAME = false;

  int tile_width = 800;
  int tile_height = 600;
  int tile_columns = 4;
  int tile_rows = 2;
  int total_width = tile_width * tile_columns;
  int total_height = tile_height * tile_rows;
  NeoMap[][] child_maps = new NeoMap[tile_columns][tile_rows];
  // java.util.List child_maps = new ArrayList();
  //  LinearTransform temp_trans = new LinearTransform();

  public MultiWindowTierMap(boolean hscroll, boolean vscroll) {
    super(hscroll, vscroll);
    this.setSize(total_width, total_height);
    this.getNeoCanvas().setSize(total_width, total_height);
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
        LinearTransform ctrans = (LinearTransform)cmap.getView().getTransform();
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
    int xwindows_per_screen = tile_columns / screen_count;
    int ywindows_per_screen = tile_rows;
    int col = 0;

    for (int i=0; i<devices.length; i++) {
      GraphicsDevice dev = devices[i];
      String id = dev.getIDstring();
      int type = dev.getType();
      GraphicsConfiguration gconfig = dev.getDefaultConfiguration();
      DisplayMode dmode = dev.getDisplayMode();
      boolean fullscreen = dev.isFullScreenSupported();
      boolean change = dev.isDisplayChangeSupported();
      Rectangle config_bounds = gconfig.getBounds();
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
      int yoffset = 600;
      if (type == GraphicsDevice.TYPE_RASTER_SCREEN) {
	for (int x=0; x < xwindows_per_screen; x++) {
	  int row = 0;
	  for (int y=0; y < ywindows_per_screen; y++) {
	    //	    JFrame win = new JFrame(gconfig);
	    Container win;
	    if (USE_SWING) {
	      if (USE_FRAME) { win = new JFrame(gconfig); }
	      else { win = new JWindow(gconfig); }
	    }
	    else {
	      if (USE_FRAME) { win = new Frame(gconfig); }
	      else { win = new Window(IGB.getSingletonIGB().getFrame(), gconfig); }
	    }

	    //	    int xpixels_per_window = config_bounds.width / xwindows_per_screen;
	    //	    int ypixels_per_window = config_bounds.height / ywindows_per_screen;
	    int xpixels_per_window = tile_width;
	    int ypixels_per_window = tile_height;
	    //	    int ypixels_per_window = (config_bounds.height - 30) / ywindows_per_screen
	    // -30 so Windows control bar will be visible at bottom

	    //	    win.setLocation(x*400, yoffset + y*300);
	    win.setLocation(config_bounds.x + (x * xpixels_per_window),
			       config_bounds.y + (y * ypixels_per_window));
	    win.setSize(xpixels_per_window, ypixels_per_window);

	    //	    AffyTieredMap newmap = new AffyTieredMap(main_map);
	    //	    NeoMap newmap = new NeoMap(map);  // make a new map with original
	    //	    NeoMap newmap = new NeoMap(true, true);
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
	      newmap.getNeoCanvas().setDoubleBuffered(false);
	    }
	    else {  // win is a Window or Frame
	      win.setLayout(new BorderLayout());
	      newmap.getNeoCanvas().setDoubleBuffered(false);
	      win.add("Center", newmap);
	    }

	    //	    newmap.setScrollIncrementBehavior(newmap.X, newmap.AUTO_SCROLL_HALF_PAGE);
	    //	    newmap.getNeoCanvas().setDoubleBuffered(false);
	    //	    child_maps[x][y] = newmap;
	    child_maps[col][row] = newmap;
	    win.show();
	    row++;
	  }
	  col++;
	}
      }
    }
    System.out.println("*******************************************");
  }


}
