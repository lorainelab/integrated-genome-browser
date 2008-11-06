/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.glyph;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;

public class VerticalGraphLabels extends SolidGlyph {
  static final boolean DEBUG_PIXELBOX = true;
  static Font default_font = new Font("Courier", Font.PLAIN, 12);

  List labels = new ArrayList();
  Font fnt;
  Rectangle debug_rect;
  //  int xcoord_offset = 5;

  public VerticalGraphLabels() {
    fnt = default_font;
    if (DEBUG_PIXELBOX) {
      debug_rect = new Rectangle();
    }
  }

  public void addLabel(String label) {
    labels.add(label);
  }

  public void draw(ViewI view) {
    view.transformToPixels(coordbox, pixelbox);

    Graphics g = view.getGraphics();
    Graphics2D g2 = (Graphics2D)g;
    g.setColor(getForegroundColor());
    g.setFont(fnt);
    FontMetrics fm = g.getFontMetrics();

    int text_height = fm.getAscent() + fm.getDescent();
    //    int text_height = fm.getAscent();
    int label_count = labels.size();
    //    float spacing = (float)pixelbox.width/(float)label_count;
    //    float spacing = (float)pixelbox.width/(float)(label_count-1);
    float spacing = (float)pixelbox.width/(float)(label_count);
    float half_spacing = spacing/2;
    float ypos = pixelbox.y + pixelbox.height;
    //    float xpos = 0.0f + text_height + half_spacing;
    //    float xpos = text_height + half_spacing;
    float xpos = pixelbox.x + half_spacing;
    //    float xpos = half_spacing + text_height/2;
    for (int i=0; i<label_count; i++) {
      String label = (String)labels.get(i);
      int text_width = fm.stringWidth(label);
      AffineTransform oldtrans = g2.getTransform();
      g2.transform(AffineTransform.getRotateInstance(-Math.PI/2.0, xpos, ypos));
      //      g2.setColor(Color.green);
      //      g2.drawRect((int)xpos, (int)ypos, text_width, text_height);
      g2.setColor(getForegroundColor());
      //      g2.drawString (label, xpos, ypos + text_height);
      //      g2.drawString (label, xpos, ypos + text_height/3);
      //      g2.drawString (label, xpos, ypos + fm.getAscent());
      g2.drawString (label, xpos, ypos);

      g2.setTransform(oldtrans);
      xpos += spacing;      
    }
    super.draw(view);
  }

  public void setFont(Font f) { this.fnt = f; }
  public Font getFont() { return this.fnt; }
  public void setColor( Color c ) { setForegroundColor( c ); }
  public Color getColor() { return getForegroundColor(); }

  public String toString() {
    return ("VerticalStringGlyph, coordbox: " + coordbox);
  }

}
