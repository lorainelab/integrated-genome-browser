/**
*   Copyright (c) 2001-2008 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.genoviz.glyph3;

import com.affymetrix.genoviz.glyph.LabelledGlyph2;
import java.awt.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.util.NeoConstants.Placement;
import java.awt.geom.Rectangle2D;

//TODO: Delete this glyph.  It is not an efficient glyph
public class LabelledLineContGlyph2 extends Glyph 
     implements LabelledGlyph2  {

  static boolean OUTLINE_PIXELBOX = false;
  static boolean DEBUG_OPTIMIZED_FILL = false;

  static Rectangle2D.Double scratch_cbox = new Rectangle2D.Double();
  //  static fnt = new Font("Monospaced", Font.PLAIN, 1);
  static int min_point_font = 6;
  static int max_point_font = 24;
  static Font[] font_point_array = new Font[max_point_font+1];
  //    currently basing font choice on point size of font, but would rather 
  //        base this on _pixel_ size of font -- started on this, 
  //        but commented out for now
  static int min_pixel_font = 6;
  static int max_pixel_font = 48;
  static Font[] font_pixel_array = new Font[max_pixel_font+1];
  //  static Font[] font_width_pix; // for each index i in font_width_pix, 
  static int pixel_separation = 1;
  static double pixels_per_inch = (double)Toolkit.getDefaultToolkit().getScreenResolution();
  static double points_per_inch = 72;
  static double points_per_pixel = points_per_inch / pixels_per_inch;
  
  static boolean optimize_child_draw = true;
  boolean show_label = true;
  boolean toggle_by_width = true;
  boolean toggle_by_height = true;

  CharSequence label;
  Placement label_loc = Placement.ABOVE;
  boolean move_children = true;

  static {
    //    Font base_fnt = new Font("Serif", Font.PLAIN, 1);
    Font base_fnt = new Font("Monospaced", Font.PLAIN, 1);
    //Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    /*
    System.out.println("screen size: " + dim.width + " x " + dim.height);
    System.out.println("pixels per inch: " + pixels_per_inch);
    System.out.println("points per inch: " + points_per_inch);
    System.out.println("points per pixel: " + points_per_pixel);
    */

    for (int i=min_point_font; i<=max_point_font; i++) {
      font_point_array[i] = base_fnt.deriveFont((float)i);
    }
    for (int i=min_pixel_font; i<=max_pixel_font; i++) {
      //      font_pixel_array[i] = base_fnt.deriveFont((float)(i * points_per_pixel));
      //      System.out.println("pixel height: " + i + ", point height = " + (i * points_per_pixel));
      font_pixel_array[i] = base_fnt.deriveFont((float)(i+1));
    }
    
  }

  @Override
  public void drawTraversal(ViewI view)  {
    if (optimize_child_draw) {
      view.transformToPixels(coordbox, pixelbox);
      if (withinView(view) && isVisible) {
	if ((pixelbox.width <=3) || 
	    (pixelbox.height <=3))  {
	    
	  // still ends up drawing children for selected, but in general 
	  //    only a few glyphs are ever selected at the same time, so should be fine
	  if (selected) { drawSelected(view); }  
	  else  { fillDraw(view); }
	}
	else {
	  super.drawTraversal(view);  // big enough to draw normal self and children
	}
      }
    }
    else {
      super.drawTraversal(view);  // no optimization, so draw normal self and children
    }
  }

  public void fillDraw(ViewI view) {
    super.draw(view);
    Graphics g = view.getGraphics();
    if (DEBUG_OPTIMIZED_FILL) { g.setColor(Color.white); }
    else { g.setColor(getBackgroundColor()); }

    if (show_label) {
      scratch_cbox.x = coordbox.x;
      scratch_cbox.width = coordbox.width;
      if (Placement.ABOVE.equals(label_loc)) {
	scratch_cbox.y = coordbox.y + coordbox.height/2;
	scratch_cbox.height = coordbox.height/2;
      }
      else if (Placement.BELOW.equals(label_loc)) {
	scratch_cbox.y = coordbox.y;
	scratch_cbox.height = coordbox.height/2;
      }
      view.transformToPixels(scratch_cbox, pixelbox);
    }
    else {
      view.transformToPixels(coordbox, pixelbox);
    }
    
    if (pixelbox.width < 1) { pixelbox.width = 1; }
    if (pixelbox.height < 1) { pixelbox.height = 1; }
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
  }


    @Override
  public void draw(ViewI view) {
    super.draw(view); 
    Graphics g = view.getGraphics();
    view.transformToPixels(coordbox, pixelbox);
    if (pixelbox.width == 0) { pixelbox.width = 1; }
    if (pixelbox.height == 0) { pixelbox.height = 1; }

    if (OUTLINE_PIXELBOX) {
      g.setColor(Color.yellow);
      g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    }
    // We use fillRect instead of drawLine, because it may be faster.
    g.setColor(getBackgroundColor());
    if (show_label) {
      if (getChildCount() <= 0) {
	g.fillRect(pixelbox.x, pixelbox.y+(pixelbox.height/2), 
		   pixelbox.width, Math.max(1, pixelbox.height/2));
      }
      else {
	if (Placement.ABOVE.equals(label_loc)) { // label occupies upper half, so center line in lower half
	  g.fillRect(pixelbox.x, pixelbox.y+((3*pixelbox.height)/4), pixelbox.width, 1);
	}
	else if (Placement.BELOW.equals(label_loc))  {  // label occupies lower half, so center line in upper half
	  g.fillRect(pixelbox.x, pixelbox.y+(pixelbox.height/4), pixelbox.width, 1);
	}
      }
      if (label != null) {
	int ypix_for_font = (pixelbox.height/2 - pixel_separation);
	if (ypix_for_font >= min_pixel_font)  {
	  Graphics g2 = g;
	  int font_index = Math.max(min_pixel_font, 
				    Math.min(max_pixel_font, ypix_for_font));
	  g2.setFont(font_pixel_array[font_index]);
	
	  FontMetrics fm = g2.getFontMetrics();
	  int text_width = fm.stringWidth(label.toString());
	  int text_height = fm.getAscent() - 4; // trying to fudge a little (since ascent isn't quite what I want)
	  /*
	  System.out.println("ypix for font: " + ypix_for_font);
	  System.out.println("font pixel size: " + font_index);
	  System.out.println("text ascent: " + fm.getAscent());
	  System.out.println("text height: " + fm.getHeight());
	  */
	  if (((! toggle_by_width) || 
	       (text_width <= pixelbox.width))  && 
	      ((! toggle_by_height) || 
	       (text_height <= (pixel_separation + pixelbox.height/2))) )  {
	    int xpos = pixelbox.x + (pixelbox.width/2) - (text_width/2);
	    if (Placement.ABOVE.equals(label_loc)) {
	      g2.drawString(label.toString(), xpos, 
			    //		       pixelbox.y + text_height);
			    pixelbox.y + pixelbox.height/2 - pixel_separation - 2);
	    }
	    else if (Placement.BELOW.equals(label_loc)) {
	      g2.drawString(label.toString(), xpos, 
			    pixelbox.y + pixelbox.height/2 + text_height + pixel_separation - 1);
	    }
	  }
	}
      }
    }
    else { // show_label = false, so center line within entire pixelbox
      if (getChildCount() <= 0) {
	g.fillRect(pixelbox.x, pixelbox.y+(pixelbox.height/2), 
		   pixelbox.width, Math.max(1, pixelbox.height/2));
      }
      else {
	g.fillRect(pixelbox.x, pixelbox.y+(pixelbox.height/2), pixelbox.width, 1);
      }
    }
    
  }

  /**
   *  overriding to force children to center on line
   */
    @Override
  public void addChild(GlyphI glyph) {
    // child.cbox.y is modified, but not child.cbox.height)
    // center the children of the LineContainerGlyph on the line
    super.addChild(glyph);
    adjustChild(glyph);
  }

  
  protected void adjustChild(GlyphI child) {
    if (! isMoveChildren()) {
      return;
    }
    Rectangle2D.Double cbox = child.getCoordBox();
    if (show_label) {
      if (Placement.ABOVE.equals(label_loc)) {
	double ycenter = this.coordbox.y + (0.75 * this.coordbox.height);
	cbox.y = ycenter - (0.5 * cbox.height);
      }
      else {
	double ycenter = this.coordbox.y + (0.25 * this.coordbox.height);
	cbox.y = ycenter - (0.5 * cbox.height);
      }
    }
    else {
      double ycenter = this.coordbox.y + this.coordbox.height/2;
      cbox.y = ycenter - cbox.height/2;
    }
  }

  protected void adjustChildren() {
    if (! isMoveChildren()) {
      return;
    }
    java.util.List childlist = this.getChildren();
    if (childlist != null) {
      int child_count = this.getChildCount();
      for (int i=0; i<child_count; i++) {
	GlyphI child = (GlyphI)childlist.get(i);
	adjustChild(child);
      }
    }
  }

    @Override
  public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
    calcPixels(view);
    return  isVisible?pixel_hitbox.intersects(pixelbox):false;
  }

  public boolean hit(Rectangle2D coord_hitbox, ViewI view)  {
    return isVisible?coord_hitbox.intersects(coordbox):false;
  }



  public void setLabelLocation(Placement loc) { 
    if (loc != label_loc) {
      label_loc = loc; 
      adjustChildren();
    }
  }

  public void setShowLabel(boolean b) { 
    if (b != show_label) {
      show_label = b; 
      adjustChildren();
    }
  }

  @Override
  public Placement getLabelLocation() { return label_loc; }
  @Override
  public boolean getShowLabel() { return show_label; }

  @Override
  public void setLabel(CharSequence str) { this.label = str; }
  @Override
  public CharSequence getLabel() { return label; }

    
  /**
   * If true, {@link #addChild(GlyphI)} will automatically center the child vertically.
   */
  public boolean isMoveChildren() {
    return this.move_children;
  }  

  /**
   * Set whether {@link #addChild(GlyphI)} will automatically center the child vertically.
   */
  public void setMoveChildren(boolean move_children) {
    this.move_children = move_children;
  }

}
