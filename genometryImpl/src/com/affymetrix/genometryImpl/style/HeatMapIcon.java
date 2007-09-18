/**
*   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.style;

import java.awt.*;
import javax.swing.*;

/** An Icon to represent a {@link HeatMap}. */
public class HeatMapIcon implements Icon {

    private int width;
    private int height;
    private HeatMap heatmap;

    public HeatMapIcon(int width, int height, HeatMap hm) {
      this.width = width;
      this.height = height;
      this.heatmap = hm;
    }

    public int getIconHeight() {
        return height;
    }

    public int getIconWidth() {
        return width;
    }
    
    public HeatMap getHeatMap() {
      return heatmap;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.translate(x, y);
        if (c.isEnabled()) {
            g.setColor(c.getForeground());
        } else {
            g.setColor(Color.gray);
        }
        g.fillRect(0, 0, width, height);
        double scale = 255.0/width;
        for (int i=1; i<width-1; i++) {
          Color color = this.heatmap.getColor((int) (i*scale));
          g.setColor(color);
          g.drawLine(i, 1, i, height-2);
        }
        g.translate(-x, -y);   //Restore graphics object
    }
}
