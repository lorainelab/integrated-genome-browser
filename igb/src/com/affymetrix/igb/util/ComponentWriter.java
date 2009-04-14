package com.affymetrix.igb.util;

import java.awt.Component;
import org.freehep.util.export.ExportDialog;


/**
 * Prints a component to a file.
 * That file can be in any of the formats supported by the FreeHep VectorGraphics library: http://java.freehep.org/vectorgraphics
 * These include:  PostScript, PDF, EMF, SVF, Flash SWF, GIF, PNG, JPG and PPM.
 * Turns off/on double-buffering in Swing and NGSDK components.
 *
 */
public final class ComponentWriter {
  
  /** Show the export dialog that allows exporting in a variety of graphics 
   *  formats using the FreeHep libraries.  
   */
  public static void showExportDialog(Component c) {
    ExportDialog export = new ExportDialog();
    export.showExportDialog(c, "Export view as ...", c, "export");
  }

}
