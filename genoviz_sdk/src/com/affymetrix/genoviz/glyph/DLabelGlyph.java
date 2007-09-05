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

package com.affymetrix.genoviz.glyph;

import java.awt.*;
import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.util.NeoConstants;

/**
 * A glyph used to label other glyphs -- draws a text string positioned relative to
 * the other glyph.
 * <p>
 * There are still known problems with DLabelGlyph hit detection, which can affect
 * operations dependent on hit detection such as packing and selection.
 * <p>
 * The new version of this named LabelGlyph solves the hit detection problems.
 * This class was renamed and deprecated,
 * but is still included in the GenoViz.
 * There may be someone who still wants the old behavior
 * for some reason.
 *
 * @deprecated use LabelGlyph.
 */
  @Deprecated
public class DLabelGlyph extends Glyph implements NeoConstants  {
  private static final boolean DEBUG_PIXELBOX = false;
  private static final boolean DEBUG_HIT = false;

  protected static Font DEFAULT_FONT = new Font("Courier", Font.PLAIN, 12);

  protected int placement = ABOVE;
  protected String text;
  protected Font fnt;
  protected String font_name;
  protected int font_size;
  protected int font_style;

  protected boolean show_outline = false;
  protected boolean show_background = false;

  protected Color background_color = Color.lightGray;
  protected Color outline_color = Color.red;

  // NOTE the coordbox of the label is actually the coordbox of the
  //      glyph being labeled
  // BUT  the pixelbox of the label is the pixel bounds of the
  //      label's drawn aspect (not including any connector to the
  //      labeled glyph)
  // ALSO need to keep track of several other
  //      coord and pixel boxes:
  //      enclosing_coords, enclosing_pix, label_coords, labeled_pix

  // enclosing coord box contains both the label glyph's coord box and the
  //    coord box of the glyph it is labeling
  public Rectangle2D enclosing_coords;

  // enclosing pixel box contains both the label glyph's pixel box and the
  //    pixel box of the glyph it is labeling
  public Rectangle enclosing_pix;

  // label_coords is the coord box for just the label's drawn aspect
  //   (not including any connector the the labeled glyph...)
  public Rectangle2D label_coords;

  // labeled_pix is the pixel box for the glyph that DLabelGlyph is
  //   labeling
  public Rectangle labeled_pix;

  // the Glyph that is being labeled -- null means label is "on its own"
  protected GlyphI labeled;

  // keeping track of previous view to make sure previous coord/pixel
  //    calculations are relevant to current view
  //    (really needs to keep track of Transform -- maybe make a
  //     view.equivalent(view2) or view.unchanged(view2) method???)
  protected ViewI prev_view = null;

  // distance away from glyph being labeled that glyph should be placed
  // (currently ignored for placement of CENTER or NONE)
  int pixel_separation;

  // y position of text baseline (for drawString(str, x, y) since y argument
  //   needs to be text baseline rather than top of text
  int text_baseline;

  public DLabelGlyph (String str) {
    this();
    text = str;
  }

  public DLabelGlyph () {
    fnt = DEFAULT_FONT;
    setFontExtras();
    placement = ABOVE;
    labeled_pix = new Rectangle();
    enclosing_pix = new Rectangle();
    enclosing_coords = new Rectangle2D();
    label_coords = new Rectangle2D();
    pixel_separation = 3;

  }

  public void setText(String str) {
    text = str;
  }

  public String getText() {
    return text;
  }

  public void draw(ViewI view) {
    if (fnt == null || text == null) { return; }
    Graphics g = view.getGraphics();
    g.setFont(fnt);
    FontMetrics font = g.getFontMetrics();
    int text_width = font.stringWidth(text);
    int text_height = font.getAscent();

    Rectangle2D view_box = view.getCoordBox();
    //    Rectangle2D parent_box = parent.getCoordBox();
    view.transformToPixels(coordbox, pixelbox);
    labeled_pix.setBounds(pixelbox.x, pixelbox.y,
        pixelbox.width, pixelbox.height);
    if (placement == LEFT) {
      pixelbox.x = pixelbox.x - text_width - pixel_separation;
    }
    else if (placement == RIGHT) {
      pixelbox.x = pixelbox.x + pixelbox.width + pixel_separation;
    }
    else {
      pixelbox.x = pixelbox.x + pixelbox.width/2 - text_width/2;
    }
    if (placement == ABOVE) {
      //      pixelbox.y = pixelbox.y - pixel_separation;
      text_baseline = pixelbox.y - pixel_separation;
    }
    else if (placement == BELOW) {
      text_baseline = pixelbox.y + pixelbox.height +
        text_height + pixel_separation;
    }
    else {
      text_baseline = pixelbox.y + pixelbox.height/2 + text_height/2;
    }
    pixelbox.width = text_width;
    pixelbox.height = text_height;
    pixelbox.y = text_baseline - text_height;

    // -2/+4 for offsetting outline/background from text position
    pixelbox.x -= 2;
    pixelbox.width += 4;

    if (show_background) {
     g.setColor(background_color);
     g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    }

    g.setColor(getBackgroundColor());

    // +2 for offsetting text from outline/background
    g.drawString (text, pixelbox.x+2, text_baseline);

    // note that union creates a new Rectangle -- may want to try
    // doing union calculations here instead to avoid object creation...
    enclosing_pix = pixelbox.union(labeled_pix);
    enclosing_coords = view.transformToCoords(enclosing_pix, enclosing_coords);
    label_coords = view.transformToCoords(pixelbox, label_coords);

    if (DEBUG_PIXELBOX) {
      g.setColor(Color.red);
      g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    }
    if (show_outline) {
      g.setColor(outline_color);
      g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    }
    super.draw(view);
    prev_view = view;
  }

  public boolean intersects (Rectangle rect) {
    if (DEBUG_HIT) {
      System.out.println("DLabelGlyph.intersects(Rect, View) called");
    }
    if (DEBUG_HIT) {
      System.out.println("same view as last draw -- using enclosing pix");
    }
    return isVisible?rect.intersects(enclosing_pix):false;
  }

  public boolean intersects(Rectangle2D rect, ViewI view) {
    if (DEBUG_HIT) {
      System.out.println("DLabelGlyph.intersects(Rect2D, View) called");
    }
    if (view == prev_view) {
      if (DEBUG_HIT) {
        System.out.println("same view as last draw -- using enclosing coords");
      }
      return isVisible?rect.intersects(enclosing_coords):false;
    }
    else return super.intersects(rect, view);
  }

  // NEEDS WORK TO RESTRICT HIT TO LABELLED GLYPH OR LABEL
  // RATHER THAN ENCLOSING_PIX
  public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
    if (DEBUG_HIT) {
      System.out.println("DLabelGlyph.hit(Rect, View) called");
    }
    if (view == prev_view) {
      if (DEBUG_HIT) {
        System.out.println("same view as last draw -- using enclosing pix");
      }
      return pixel_hitbox.intersects(enclosing_pix);
    }
    calcPixels(view);
    return  pixel_hitbox.intersects(pixelbox);
  }

  public boolean hit(Rectangle2D coord_hitbox, ViewI view)  {
    if (DEBUG_HIT) {
      System.out.println("DLabelGlyph.hit(Rect2D, View) called");
    }
    if (view == prev_view) {
      if (DEBUG_HIT) {
        System.out.println("same view as last draw -- using enclosing coords");
      }
      return (coord_hitbox.intersects(label_coords) ||
          coord_hitbox.intersects(coordbox));
    }
    return coord_hitbox.intersects(coordbox);
  }

  public void setFont(Font f) {
    fnt = f;
    setFontExtras();
  }

  public Font getFont() {
    return this.fnt;
  }

  //  since fnt is referenced in setFont(fnt), not copied, it could be
  //  changed from outside.  Therefore whenever font extras
  //  (name, size, style) are going to be referred to, it is a good idea to
  //  call setFontExtras() first;
  // NOTE that new font IS created in setFont() with no arguments though...
  //   might want to set up an external_font boolean so can have it both
  //   ways...
  protected void setFontExtras() {
    font_name = fnt.getFamily();
    font_size = fnt.getSize();
    font_style = fnt.getStyle();
  }

  public void setFontName(String name) {
    setFontExtras();
    font_name = name;
    setFont();
  }

  public void setFontSize(int size) {
    setFontExtras();
    font_size = size;
    setFont();
  }

  public void setFontStyle(int style) {
    setFontExtras();
    font_style = style;
    setFont();
  }

  protected void setFont() {
    fnt = new Font(font_name, font_style, font_size);
  }

  /**
   * places this label
   * relative to the labeled glyph.
   *
   * @param placement LEFT, RIGHT, ABOVE, or BELOW
   */
  public void setPlacement(int placement) {
    this.placement = placement;
  }

  /**
   * gets this label's placement
   * relative to the labeled glyph.
   *
   * @return LEFT, RIGHT, ABOVE, or BELOW
   */
  public int getPlacement() {
    return this.placement;
  }

  public void setPixelSeparation(int pix) {
    pixel_separation = pix;
  }

  public int getPixelSeparation() {
    return pixel_separation;
  }

  /**
   * associates this label with a labeled glyph.
   * Only one glyph can be labeled at a time.
   * Multiple labels can label the same glyph.
   *
   * @param lg the labeled glyph
   */
  public void setLabeledGlyph(GlyphI lg) {
    labeled = lg;
    // setting to lg's coordbox causes moving problems, since both lg and this
    // will try to do moveRelative() calls affecting the _same_ coordbox --
    // trying a reshape instead
    Rectangle2D lgbox = lg.getCoordBox();
    setCoords(lgbox.x, lgbox.y, lgbox.width, lgbox.height);
    prev_view = null;
  }

  public GlyphI getLabeledGlyph() {
    return(labeled);
  }

  // handle how label is moved with respect to moving / not moving labeled
  //   glyph here, in selection, or elsewhere???
  public void moveRelative(double diffx, double diffy) {
    super.moveRelative(diffx, diffy);
  }

  public void setShowOutline(boolean show) {
    show_outline = show;
  }

  public boolean getShowOutline() {
    return show_outline;
  }

  public void setShowBackground(boolean show) {
    show_background = show;
  }

  public boolean getShowBackground() {
    return show_background;
  }

  public void setBackgroundColor(Color col) {
    background_color = col;
  }

  public void setOutlineColor(Color col) {
    outline_color = col;
  }

  public void setTextColor(Color col) {
    setBackgroundColor(col);
  }

  public Color getBackgroundColor() {
    return background_color;
  }

  public Color getOutlineColor() {
    return outline_color;
  }

  public Color getTextColor() {
    return getBackgroundColor();
  }

}
