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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.util.NeoConstants;

public class VerticalStringGlyph extends SolidGlyph implements NeoConstants  {
  static final boolean DEBUG_PIXELBOX = true;
  static Font default_font = new Font("Courier", Font.PLAIN, 12);
  Rectangle debug_rect;
  String str;
  Font fnt;
  int placement;
  boolean show_background = false;

  public VerticalStringGlyph(String str) {
    this();
    this.str = str;
  }

  public VerticalStringGlyph() {
    placement = CENTER;
    fnt = default_font;
    if (DEBUG_PIXELBOX) {
      debug_rect = new Rectangle();
    }
  }

  public void draw(ViewI view) {
    Graphics g = view.getGraphics();
    Graphics2D g2 = (Graphics2D)g;
    AffineTransform oldtrans = g2.getTransform();
    g.setFont(fnt);
    FontMetrics fm = g.getFontMetrics();
    int text_width = 0;
    if ( null != str ) {
      text_width = fm.stringWidth(str);
    }
    int text_height = fm.getAscent() + fm.getDescent();
    int blank_width = fm.charWidth ('z')*2;

    Rectangle2D view_box = view.getCoordBox();
    view.transformToPixels(coordbox, pixelbox);
    if (DEBUG_PIXELBOX) {
      debug_rect.setBounds(pixelbox.x, pixelbox.y,
			 pixelbox.width, pixelbox.height);
    }
    if (placement == LEFT) {
      pixelbox.x = pixelbox.x;
    }
    else if (placement == RIGHT) {
      pixelbox.x = pixelbox.x + pixelbox.width + blank_width;
    }
    else {
      pixelbox.x = pixelbox.x + pixelbox.width/2 - text_width/2;
    }
    if (placement == ABOVE) {
      pixelbox.y = pixelbox.y;
    }
    else if (placement == BELOW) {
      pixelbox.y = pixelbox.y + pixelbox.height;
    }
    else {
      pixelbox.y = pixelbox.y + pixelbox.height/2 + text_height/2;
    }
    pixelbox.width = text_width;
    pixelbox.height = text_height+1; // +1 for an extra pixel below the text
                                     // so letters like 'g' still have at
                                     // least one pixel below them
    
    if( getShowBackground() ) { // show background
      Color bgc = getBackgroundColor();
      if ( null != bgc ) {
	g.setColor( getBackgroundColor() );
	g.fillRect( pixelbox.x, pixelbox.y - pixelbox.height, 
		    pixelbox.width, pixelbox.height);
      }
    }


    if ( null != str ) {
      // display string
      g.setColor( getForegroundColor() );
      // define adjust such that: ascent-adjust = descent+adjust
      // (But see comment above about the extra -1 pixel)
      int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0) -1;
      int ypos = pixelbox.y -pixelbox.height/2+adjust;
      g2.transform(AffineTransform.getRotateInstance(-Math.PI/2.0, pixelbox.x, ypos));
      g2.drawString (str, pixelbox.x, ypos);
      //      g.drawString (str, pixelbox.x, pixelbox.y -pixelbox.height/2+adjust);
    }
    
    if (DEBUG_PIXELBOX) {
      // testing pixbox...
      g.setColor(Color.red);
      g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
      g.setColor(Color.yellow);
      g.drawRect(debug_rect.x, debug_rect.y, 
		 debug_rect.width, debug_rect.height);
    }
    g2.setTransform(oldtrans);
    super.draw(view);
  }

  public void setString (String str) { this.str = str; }
  public String getString () { return str; }
  public void setShowBackground(boolean show) { show_background = show; }
  public boolean getShowBackground() { return show_background; }
  public void setFont(Font f) { this.fnt = f; }
  public Font getFont() { return this.fnt; }
  public void setPlacement(int placement) { this.placement = placement; }
  public int getPlacement() { return placement; }
  public void setColor( Color c ) { setForegroundColor( c ); }
  public Color getColor() { return getForegroundColor(); }

  public String toString() {
    return ("VerticalStringGlyph: string: \""+str+"\"  +coordbox: "+coordbox);
  }

}
