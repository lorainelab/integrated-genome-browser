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

package com.affymetrix.genoviz.awt.command;

import java.util.Properties;
import java.awt.*;

public class PrintCommand implements Runnable {

    private Frame compFrame;
    private Component componentToPrint;
    private String bannerTitle;
    private boolean needsRect;

    /**
     * creates the command.
     *
     * @param c the component to be printed.
     * @param title The title of the banner page for printing
     * @param haveRect Should the printed component come out with a rectangle around it?
     */
    public PrintCommand (Frame f, Component c, String title, boolean haveRect) {
      this.compFrame = f;
      this.componentToPrint = c;
      this.bannerTitle  = title;
      this.needsRect    = haveRect;
    }

    /**
     * commands the component to print.
     */
    public void run() {

      // Obtain a PrintJob and a Graphics object to use with it

      Toolkit toolkit = componentToPrint.getToolkit();
      PrintJob job    = toolkit.getPrintJob (compFrame, bannerTitle,
          new Properties());

      if (job == null)
        return;  // i.e. the user clicked Cancel.

      Graphics g = job.getGraphics();

      // Give the output some margins (avoid scrunching in upper
      // left corner)

      g.translate (50,50);

      Dimension size = componentToPrint.getSize();

      // Draw a border around the output, if appropriate.

      if (needsRect) {
        g.drawRect (-1, -1, size.width+1, size.height+1);
      }

      // Set a clipping region

      g.setClip (0, 0, size.width, size.height);

      // Print the componentToPrint and the components it contains

      componentToPrint.printAll (g);

      // Finish up.

      g.dispose();
      job.end();
    }

}
