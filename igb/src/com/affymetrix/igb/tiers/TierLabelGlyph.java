/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.util.NeoConstants;

/**
 * A glyph used to display a label for a TierGlyph.
 */
public class TierLabelGlyph extends SolidGlyph implements NeoConstants  {

  private static final boolean DEBUG_PIXELBOX = false;
  private Rectangle debug_rect;

  private Font fnt;
  private int placement;
  private boolean show_background = false;
  private boolean show_outline = false;
  private boolean show_label = true;
  
  static final boolean THICK_OUTLINE = true;
  static final boolean DRAW_COLLAPSED_EXPANDED_ICONS = false;

  public String toString() {
    return ("TierLabelGlyph: label: \""+getLabelString()+"\"  +coordbox: "+coordbox);
  }

  /**
   *  Constructor.  
   *  @param reference_tier the tier in the main part of the AffyLabelledTierMap, 
   *    must not be null
   */
  public TierLabelGlyph(TierGlyph reference_tier) {
    this();
    //str = reference_tier.getLabel();
    //if (str==null) {str = "......."; }
    this.setInfo(reference_tier);
  }
  
  private TierLabelGlyph () {
    placement = CENTER;
    if (DEBUG_PIXELBOX) {
      debug_rect = new Rectangle();
    }
  }

  /** Overridden such that the info must be of type TierGlyph.  It is used
   *  to store the reference tier that will be returned by getReferenceTier().
   */
  public void setInfo(Object o) {
    if (! (o instanceof TierGlyph)) {
      throw new IllegalArgumentException();
    }
    super.setInfo(o);
  }
  
  /** Returns the reference tier from the main map in AffyLabelledTierMap. 
   *  Equivalent to value returned by getInfo().  Will not be null.
   */
  public TierGlyph getReferenceTier() {
    return (TierGlyph) getInfo();
  }
    
 String getDirectionString(TierGlyph tg) {
    switch (tg.direction) {
      case TierGlyph.DIRECTION_FORWARD:
        return "(+)";
      case TierGlyph.DIRECTION_REVERSE:
        return "(-)";
      case TierGlyph.DIRECTION_BOTH:
        return "(+/-)";
      default: // DIRECTION_NONE
        return "";
    }
  }
      
  /** Returns the label of the reference tier, or some default string if there isn't one. */
  String getLabelString() {
    TierGlyph reference_tier = getReferenceTier();
    if (reference_tier == null || reference_tier.getLabel() == null) {
      return ".......";
    } else {
      String direction_str = getDirectionString(reference_tier);
      if ("".equals(direction_str)) {
        return reference_tier.getLabel();
      } else {
        return reference_tier.getLabel() + " " + direction_str;
      }
    }
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

  /** Sets whether or not to draw the label string. Default is true. */
  public void setShowLabel(boolean show) {
    show_label = show;
  }

  /** Whether or not to draw the label string. Default is true. */
  public boolean getShowLabel() {
    return show_label;
  }
  
  boolean useRefTierColors = true;
  boolean useInvertedRefTierColors = false;
  
  /** Whether to use the same color as the reference TierGlyph,
   *  and thus ignore any setting of the colors on this Glyph.
   *  Default is TRUE.
   *  @see getReferenceTier()
   */
  public void setUseRefTierColors(boolean b) {
    this.useRefTierColors = b;
  }
  
  /** Whether to use the same color as the reference TierGlyph,
   *  and thus ignore any setting of the colors on this Glyph.
   *  Default is TRUE.
   *  @see getReferenceTier()
   */
  public boolean getUseRefTierColors() {
    return this.useRefTierColors;
  }
  
  public void setInvertRefTierColors(boolean b) {
    useInvertedRefTierColors = b;
  }

  public boolean getInvertRefTierColors() {
    return useInvertedRefTierColors;
  }

  public void draw(ViewI view) {    
    Color bgcolor = null;
    Color fgcolor = null;

    TierGlyph reftier = this.getReferenceTier();
    if (useRefTierColors) {
      if (useInvertedRefTierColors) {
        fgcolor = reftier.getFillColor();
        bgcolor = reftier.getForegroundColor();
      } else {
        bgcolor = reftier.getFillColor();
        fgcolor = reftier.getForegroundColor();
      }
    } else {
      bgcolor = getBackgroundColor();
      fgcolor = getForegroundColor();
    }
    boolean collapsed = reftier.getAnnotStyle().getCollapsed();
    
    Graphics g = view.getGraphics();
    g.setPaintMode();
    
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

    if (DRAW_COLLAPSED_EXPANDED_ICONS && reftier.getAnnotStyle().getExpandable()) {
      if (collapsed) {
        g.setColor(fgcolor);
        g.drawLine(pixelbox.x + pixelbox.width - 13, pixelbox.y + 8, pixelbox.x + pixelbox.width - 5, pixelbox.y + 8);
        g.drawLine(pixelbox.x + pixelbox.width - 9, pixelbox.y + 4, pixelbox.x + pixelbox.width - 9, pixelbox.y + 12);
      } else {
        g.setColor(fgcolor);
        g.drawLine(pixelbox.x + pixelbox.width - 13, pixelbox.y + 8, pixelbox.x + pixelbox.width - 5, pixelbox.y + 8);
      }
    }

    if (show_label) {
      g.setColor( fgcolor );
      drawLabel(g);
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

  void drawLabel(Graphics g) {
    // assumes that pixelbox coordinates are already computed
    
    if ( null != fnt ) {
      g.setFont(fnt);
    }
    FontMetrics fm = g.getFontMetrics();
    int text_width = 0;
    String label = getLabelString();
    if ( null != label ) {
      text_width = fm.stringWidth(label);
    }
    int text_height = fm.getAscent() + fm.getDescent();
    int blank_width = fm.charWidth ('z')*2;

    // only show text if it will fit in pixelbox (and it's not null)...
    if ((text_height <= pixelbox.height)  && (label != null)) {

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
      //g.setColor( fgcolor );
      //g.setColor( getForegroundColor() );
      // define adjust such that: ascent-adjust = descent+adjust
      // (But see comment above about the extra -1 pixel)
      int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0) -1;
      g.drawString (label, pixelbox.x, pixelbox.y -pixelbox.height/2+adjust);
    }
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
