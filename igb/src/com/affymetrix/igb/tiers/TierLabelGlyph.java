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

package com.affymetrix.igb.tiers;

import java.awt.*;
import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.IGB;

/**
 * A glyph used to display a label for a TierGlyph.
 */
public class TierLabelGlyph extends SolidGlyph implements NeoConstants  {

  private static final boolean DEBUG_PIXELBOX = false;
  private Rectangle debug_rect;

  private String str;
  private Font fnt;
  private int placement;
  private boolean show_background = false;
  private boolean show_outline = false;

  static final boolean THICK_OUTLINE = true;

  public String toString() {
    return ("TierLabelGlyph: string: \""+str+"\"  +coordbox: "+coordbox);
  }

  public TierLabelGlyph (String str) {
    this();
    this.str = str;
  }

  public TierLabelGlyph () {
    placement = CENTER;
    if (DEBUG_PIXELBOX) {
      debug_rect = new Rectangle();
    }
  }

  public void setString (String str) {
    this.str = str;
  }
  public String getString () {
    return str;
  }

  public void setShowBackground(boolean show) {
    show_background = show;
  }
  public boolean getShowBackground() {
    return show_background;
  }

  public void setShowOutline(boolean show) {
    show_outline = show;
  }

  public boolean getShowOutline() {
    return show_outline;
  }

  public void draw(ViewI view) {
    Color bgcolor = null;
    Color fgcolor = null;
    if (this.getInfo() instanceof TierGlyph) {
      TierGlyph reftier = (TierGlyph)this.getInfo();
      //      setBackgroundColor(reftier.getFillColor());
      //      setForegroundColor(reftier.getForegroundColor());
      bgcolor = reftier.getFillColor();
      fgcolor = reftier.getForegroundColor();
    }
    Graphics g = view.getGraphics();
    g.setPaintMode();
    if ( null != fnt ) {
      g.setFont(fnt);
    }
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

    if( getShowBackground() ) { // show background
      //      Color bgc = getBackgroundColor();
      //      if ( null != bgc ) {
	//	g.setColor( getBackgroundColor() );
      if (bgcolor != null) {
	g.setColor(bgcolor);
	g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
      }
    }
    if (getShowOutline()) {
      //      g.setColor(this.getForegroundColor());
      g.setColor(fgcolor);
      g.drawRect(pixelbox.x,   pixelbox.y,   pixelbox.width-1, pixelbox.height-1);
      if (THICK_OUTLINE) {
        g.drawRect(pixelbox.x+1, pixelbox.y+1, pixelbox.width-3, pixelbox.height-3);
      }
    }

    // only show text if it will fit in pixelbox (and it's not null)...
    if ((text_height <= pixelbox.height)  && (str != null)) {

      if (placement == LEFT ) {
	pixelbox.x = pixelbox.x;
      }
      else if (placement == RIGHT) {
	pixelbox.x = pixelbox.x + pixelbox.width + blank_width;
      }
      else {
	if (text_width > pixelbox.width) {
	  // if text wider than glyph's pixelbox, then show beginning of text
	  pixelbox.x = pixelbox.x+1;
	}
	else {
	  // if text wider than glyph's pixelbox, then center text
	  pixelbox.x = pixelbox.x + pixelbox.width/2 - text_width/2;
	}
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

      // display string
      g.setColor( getForegroundColor() );
      // define adjust such that: ascent-adjust = descent+adjust
      // (But see comment above about the extra -1 pixel)
      int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0) -1;
      g.drawString (str, pixelbox.x, pixelbox.y -pixelbox.height/2+adjust);
    }
    if (DEBUG_PIXELBOX) {
      // testing pixbox...
      g.setColor(Color.red);
      g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
      g.setColor(Color.yellow);
      g.drawRect(debug_rect.x, debug_rect.y,
		 debug_rect.width, debug_rect.height);
    }
    super.draw(view);
  }

  /** Draws the outline in a way that looks good for tiers.  With other glyphs,
   *  the outline is usually drawn a pixel or two larger than the glyph.
   *  With TierGlyphs, it is better to draw the outline inside of or contiguous
   *  with the glyphs borders.
   **/
  protected void drawSelectedOutline(ViewI view) {
    draw(view);

    Graphics g = view.getGraphics();
    g.setColor(view.getScene().getSelectionColor());
    view.transformToPixels(getPositiveCoordBox(), pixelbox);
    g.drawRect(pixelbox.x, pixelbox.y,
               pixelbox.width-1, pixelbox.height-1);
    if (THICK_OUTLINE) {
      g.drawRect(pixelbox.x+1, pixelbox.y+1,
               pixelbox.width-3, pixelbox.height-3);
    }
  }

  public void setFont(Font f) {
    this.fnt = f;
  }

  public Font getFont() {
    return this.fnt;
  }

  public void setPlacement(int placement) {
    this.placement = placement;
  }

  public int getPlacement() {
    return placement;
  }

  /**
   * @deprecated use {@link #setForegroundColor}.
   * Also see {@link #setBackgroundColor}.
   */
  public void setColor( Color c ) {
    setForegroundColor( c );
  }

  /**
   * @deprecated use {@link #getForegroundColor}.
   * Also see {@link #setBackgroundColor}.
   */
  public Color getColor() {
    return getForegroundColor();
  }

}
