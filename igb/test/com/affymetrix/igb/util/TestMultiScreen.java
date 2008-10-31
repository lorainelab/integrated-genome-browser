package com.affymetrix.igb.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import com.affymetrix.genoviz.awt.AdjustableJSlider;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.util.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.glyph.*;


/**
 *  Compares performance of some Glyph classes.
 *  comparing performance of standard implementation of GlyphI
 *    (root of implementation ==> com.affymetrix.genoviz.bioviews.GlyphI)
 *    with new efficient implementation of GlyphI
 *    (root of implementation ==> com.affymetrix.igb.glyph.EfficientGlyph
 */
public class TestMultiScreen implements ActionListener {
  int xwindows_per_screen = 1;
  int ywindows_per_screen = 1;
  int map_coord_width = 10000;
  int map_coord_height = 200;
  int max_glyph_width = 300;
  int max_glyph_height = 5;
  int glyph_count = 200;
  Adjustable xzoomer;
  //  NeoMap map;
  AffyTieredMap map;
  JButton memB = new JButton("Print Memory");
  JButton gcB = new JButton("Force GC");
  JButton glyphTestB = new JButton("Test Glyphs");
  java.util.List extra_maps = new ArrayList();
  com.affymetrix.genoviz.util.Timer tim = new com.affymetrix.genoviz.util.Timer();
  com.affymetrix.genoviz.util.Memer mem = new com.affymetrix.genoviz.util.Memer();
  public static void main(String[] args) {
    TestMultiScreen tester = new TestMultiScreen();
    tester.runTest();
  }

  public void runTest() {
    mem.printMemory();
    JFrame frm = new JFrame("Glyph Performance Test");

    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    //    map = new NeoMap(true, true);
    map = new AffyTieredMap(true, true);
    map.getNeoCanvas().setDoubleBuffered(false);
    map.addAxis(50);
    map.setMapRange(0, map_coord_width);
    map.setMapOffset(0, map_coord_height);
    cpane.add("Center", map);

    JPanel butP = new JPanel();
    butP.setLayout(new GridLayout(1, 3));
    butP.add(glyphTestB);
    butP.add(memB);
    butP.add(gcB);
    //    cpane.add("South", butP);
    xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
    map.setZoomer(NeoMap.X, xzoomer);
    cpane.add("South", (Component)xzoomer);

    memB.addActionListener(this);
    gcB.addActionListener(this);
    glyphTestB.addActionListener(this);

    frm.setSize(800, 600);
    testGlyphs();

    mem.printMemory();
    replicateWindows();
    frm.show();
    frm.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) { System.exit(0);}
    });
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == memB) {
      mem.printMemory();
    }
    else if (src == gcB) {
      System.gc();
    }
    else if (src == glyphTestB) {
      testGlyphs();
    }
  }

  public void testGlyphs() {
    //    clearWidget();
    //    map.addAxis(50);

    map.clearWidget();

    for (int i=0; i<glyph_count; i++) {
      //      GlyphI gl = new OutlineRectGlyph();
      GlyphI gl = new FillRectGlyph();
      gl.setColor(Color.red);
      int xstart = (int)(Math.random() * (map_coord_width - max_glyph_width));
      int width = (int)(Math.random() * max_glyph_width);
      int ystart = (int)(Math.random() * (map_coord_height - max_glyph_height));
      //      int height = (int)(Math.random() * max_glyph_height);
      int height = max_glyph_height;
      gl.setCoords(xstart, ystart, width, height);
      //      System.out.println(gl.getCoordBox());
      map.addItem(gl);
    }

    map.updateWidget();

    //    updateWidget();
    /*
    System.out.println("glyphs to create: " + glyph_count);
    System.out.println("time using FillRectGlyph: " + (old_time/1000f));
    System.out.println("time using EfficientFillRectGlyph: " + (new_time/1000f));
    System.out.println("initial memory: " + mem1);
    System.out.println("memory after FillRectGlyph: " + mem2);
    System.out.println("memory after EfficientFillRectGlyph: " + mem3);
    */
    System.out.println("updated");
  }

  // for each RASTER_SCREEN graphics device, make a grid of
  //   xwindows_per_screen X ywindows_per_screen of
  //   windows (either JWindow or JFrame) that each contain a new map
  //   with its root set to the original map
  //
  // currently just setting each new map's xzoomer to the original map's
  //   xzoomer, to test performance when zooming all maps
  public void replicateWindows() {
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
	  for (int y=0; y < ywindows_per_screen; y++) {
	    // JFrame newfrm = new JFrame(gconfig);
	    JWindow newfrm = new JWindow(gconfig);
	    int xpixels_per_window = config_bounds.width / xwindows_per_screen;
;	    int ypixels_per_window = config_bounds.height / ywindows_per_screen;
	    //	    int ypixels_per_window = (config_bounds.height - 30) / ywindows_per_screen
	    // -30 so Windows control bar will be visible at bottom

	    //	    newfrm.setLocation(x*400, yoffset + y*300);
	    newfrm.setLocation(config_bounds.x + (x * xpixels_per_window),
			       config_bounds.y + (y * ypixels_per_window));
	    newfrm.setSize(xpixels_per_window, ypixels_per_window);

	    //	    NeoMap newmap = new NeoMap(map);  // make a new map with original
	    //	    NeoMap newmap = new NeoMap(false, false);
	    NeoMap newmap = new NeoMap(true, true);
	    newmap.setRoot(map);
	    Container cpane = newfrm.getContentPane();
	    cpane.setLayout(new BorderLayout());
	    newmap.getNeoCanvas().setDoubleBuffered(false);
	    newmap.setZoomer(NeoMap.X, xzoomer);
	    cpane.add("Center", newmap);
	    newfrm.show();
	  }
	}
      }
    }
    System.out.println("*******************************************");
  }


}


