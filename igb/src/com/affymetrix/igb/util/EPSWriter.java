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

import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genoviz.awt.NeoBufferedComponent;
import com.affymetrix.genoviz.util.ComponentPagePrinter;
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

    ArrayList<NeoBufferedComponent> neo_comps = new ArrayList<NeoBufferedComponent>();
    ArrayList<Boolean> buf_states = new ArrayList<Boolean>();
    if (DISABLE_NEO_BUFFERING) {
      // turning double buffering off in NeoBufferedComponents
      ComponentPagePrinter.turnNeoBufferingOff(comp, neo_comps, buf_states);
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
    if (comp instanceof JComponent) {
        ((JComponent) comp).printComponents(g);
    } else {
        comp.print(g);
    }
    g.endExport();

    
    if (DISABLE_SWING_BUFFERING) {
      // turning double buffering back on in Swing components
      currentManager.setDoubleBufferingEnabled(true);
    }

    if (DISABLE_NEO_BUFFERING) {
      // turning double buffering back on in NeoBufferedComponents
      ComponentPagePrinter.restoreNeoBuffering(neo_comps, buf_states);
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
   *  @deprecate This will not work if the Freehep jars have been rolled into
   *  a single jar, as we have done.  It is supposed to use the openide-lookup
   *  jar as well, but we also don't include that in our distributions.
   */
  public static void showExportDialog(Component c) {
    ExportDialog export = new ExportDialog();
    export.showExportDialog(c, "Export view as ...", c, "export");
  }

}
