/**
*   Copyright (c) 2006 Affymetrix, Inc.
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
package com.affymetrix.igb.util;

import com.affymetrix.genoviz.awt.NeoBufferedComponent;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.menuitem.FileTracker;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import org.freehep.graphicsio.ps.PSGraphics2D;
import org.freehep.util.export.ExportDialog;


/**
 * Prints a component to an eps (encapsulated postscript) file.
 *
 * Turns off/on double-buffering in Swing and NGSDK components.
 *
 */
public class EPSWriter {
  static boolean DEBUG = false;
  static boolean DISABLE_SWING_BUFFERING = true;
  static boolean DISABLE_NEO_BUFFERING = true;

  public static void outputToFile(Component comp) throws IOException {
    JFileChooser chooser = getJFileChooser();
    int option = chooser.showSaveDialog(comp);
    if (option == JFileChooser.APPROVE_OPTION) {
      if (file_tracker != null) {file_tracker.setFile(chooser.getCurrentDirectory());}
      File outfile = chooser.getSelectedFile();
      outputToFile(comp, outfile);
    }
  }

  public static void outputToFile(Component comp, File outfile) throws IOException {
    // get the bounds of the component
    Dimension dim = comp.getSize();
    int cHeight = (int) (dim.getHeight());
    int cWidth = (int) (dim.getWidth());

    Vector neo_comps = new Vector();
    Vector buf_states = new Vector();
    if (DISABLE_NEO_BUFFERING) {
      // turning double buffering off in NeoBufferedComponents
      turnNeoBufferingOff(comp, neo_comps, buf_states);
    }

    RepaintManager currentManager = RepaintManager.currentManager(comp);
    if (DISABLE_SWING_BUFFERING) {
    // turning double buffering off in Swing components
      currentManager.setDoubleBufferingEnabled(false);
    }

    Properties p = new Properties();
    p.setProperty("PageSize", "Letter");
    Dimension dd = new Dimension(cWidth, cHeight);
    PSGraphics2D g = new PSGraphics2D(outfile, dd);
    g.setCreator(Application.getSingleton().getApplicationName() + "  Version: " + Application.getSingleton().getVersion());
    g.setProperties(p);
    g.startExport();
    comp.print(g);
    g.endExport();

    
    if (DISABLE_SWING_BUFFERING) {
      // turning double buffering back on in Swing components
      currentManager.setDoubleBufferingEnabled(true);
    }

    if (DISABLE_NEO_BUFFERING) {
      // turning double buffering back on in NeoBufferedComponents
      restoreNeoBuffering(neo_comps, buf_states);
    }
  }

  // recursively descend into children, searching for NeoBufferedComponents,
  //    recording their double-buffering status (in neo_comps and buf_states vectors),
  //    then turning double buffering off
  public static void turnNeoBufferingOff(Component com, Vector neo_comps, Vector buf_states) {
    if (com instanceof NeoBufferedComponent) {
      NeoBufferedComponent nbc = (NeoBufferedComponent)com;
      boolean buffered = nbc.isDoubleBuffered();

      if (DEBUG)  {
	System.out.println("turning off buffering in " + nbc +
			   ", previous buffered: " + buffered);
      }

      neo_comps.add(nbc);
      buf_states.add(new Boolean(buffered));
      nbc.setDoubleBuffered(false);
    }
    if (com instanceof Container) {
      Container parent = (Container)com;
      for (int i=0; i<parent.getComponentCount(); i++) {
	Component child = parent.getComponent(i);
	turnNeoBufferingOff(child, neo_comps, buf_states);
      }
    }
    return;
  }

  // restore buffer state of NeoBufferedComponents,
  //   based on neo_comps and buf_states Vectors
  public static void restoreNeoBuffering(Vector neo_comps, Vector buf_states) {
    for (int i=0; i<neo_comps.size(); i++) {
      NeoBufferedComponent nbc = (NeoBufferedComponent)neo_comps.elementAt(i);
      boolean buffered = ((Boolean)buf_states.elementAt(i)).booleanValue();
      if (DEBUG)  {
	System.out.println("restoring buffering in " + nbc +
			   ", buffer state: " + buffered);
      }
      nbc.setDoubleBuffered(buffered);
    }
  }

  static FileTracker file_tracker = FileTracker.OUTPUT_DIR_TRACKER;
  static JFileChooser static_chooser = null;

  /** Gets a static re-usable file chooser for writing EPS files
   *  in FileTracker.OUTPUT_DIR_TRACKER.
   */
  public static JFileChooser getJFileChooser() {
    if (static_chooser == null) {
      static_chooser = new UniFileChooser("Eps Files", "eps");
      if (file_tracker != null) {static_chooser.setCurrentDirectory(file_tracker.getFile());}
    }
    static_chooser.rescanCurrentDirectory();
    return static_chooser;
  }
  
  /** Show the export dialog that allows exporting in a variety of graphics 
   *  formats using the FreeHep libraries.  Some formats seem to be buggy, so
   *  I don't recommend using this.
   */
  public static void showExportDialog(Component c) {
    ExportDialog export = new ExportDialog();
    export.showExportDialog(c, "Export view as ...", c, "export");
  }

}
