package javax.swing;
// package com.affymetrix.swing.test;

// import javax.swing.*;
import java.awt.*;

import com.affymetrix.igb.Application;

public class GraphicsConfigChecker {

  public GraphicsConfigChecker() {
    reportGraphicsConfig();
  }

  public Rectangle reportGraphicsConfig() {
    System.out.println("*********  Graphics Configuration *********");
    Toolkit kit = Toolkit.getDefaultToolkit();
    Dimension dim = kit.getScreenSize();
    System.out.println("Screen dimensions: " + dim);
    RepaintManager rmanager = RepaintManager.currentManager(Application.getSingleton().getFrame());
    System.out.println("max double buffer size: " + rmanager.getDoubleBufferMaximumSize());
    //    rmanager.setDoubleBufferMaximumSize(new Dimension(4096, 768));
    //    System.out.println("new max double buffer size: " + rmanager.getDoubleBufferMaximumSize());
    //    System.out.println(".... ");

    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice default_device = genv.getDefaultScreenDevice();
    Rectangle maxbounds = genv.getMaximumWindowBounds();
    Point center = genv.getCenterPoint();
    GraphicsDevice[] devices = genv.getScreenDevices();

    System.out.println("Graphics Environment: " + genv);
    System.out.println("Screen Devices: " + devices.length);
    System.out.println("Default Screen device: " + default_device);
    System.out.println("max bounds: " + maxbounds);
    System.out.println("center point: " + center);
    Rectangle fullBounds = new Rectangle();
    for (int i=0; i<devices.length; i++) {
      GraphicsDevice dev = devices[i];
      String id = dev.getIDstring();
      int type = dev.getType();
      GraphicsConfiguration gconfig = dev.getDefaultConfiguration();
//      DisplayMode dmode = dev.getDisplayMode();
      boolean fullscreen = dev.isFullScreenSupported();
      boolean change = dev.isDisplayChangeSupported();
      Rectangle config_bounds = gconfig.getBounds();
      fullBounds = fullBounds.union(config_bounds);
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
    System.out.println(".......");
    System.out.println("full screen bounds: " + fullBounds);
    System.out.println("*******************************************");
    return fullBounds;
  }

}
