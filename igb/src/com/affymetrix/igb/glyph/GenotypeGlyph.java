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

import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.util.GeometryUtils;

import com.affymetrix.igb.util.IntList;

public class GenotypeGlyph extends SolidGlyph {

  static String ASTRING = "A";
  static String CSTRING = "C";
  static String GSTRING = "G";
  static String TSTRING = "T";
  static String NSTRING = "N";

  Color colorA = Color.blue;
  Color colorC = Color.green;
  Color colorG = Color.red;
  Color colorT = Color.yellow;
  Color colorN = new Color(255, 0, 255);

  protected Rectangle2D snp_cbox = new Rectangle2D();
  protected Rectangle snp_pbox = new Rectangle();

  protected Rectangle2D block_cbox = new Rectangle2D();
  protected Rectangle block_pbox = new Rectangle();

  protected Rectangle2D vis_cbox = new Rectangle2D();
  protected Rectangle vis_pbox = new Rectangle();

  protected IntList snp_locs = null;
  protected String snp_residues = null;
  protected boolean[] snp_variants = null;

  double snp_height = 5.0;
  double block_height = 10.0;
  Font fnt = new Font("Courier", Font.BOLD, 12);

  public void draw(ViewI view) {

    view.transformToPixels(coordbox, pixelbox);

    Graphics g = view.getGraphics();
    g.setColor(Color.green);
    g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    g.setFont(fnt);
    FontMetrics fm = g.getFontMetrics();
    int char_width = fm.charWidth('C');

    Rectangle2D viewbox = view.getCoordBox();
    double viewmin = viewbox.x;
    double viewmax = viewbox.x + viewbox.width;
    
    if (snp_locs != null) {
      g.setColor(this.getBackgroundColor());
      int snpcount = snp_locs.size();
      int visible_count = 0;
      for (int i=0; i<snpcount; i++) {
	double snploc = snp_locs.get(i);
	if (snploc >= viewmin && snploc <= viewmax) {
	  visible_count++;
	}
      }
      if (visible_count > 0) {
      // reshape vis_cbox to be intersection of viewbox and coordbox
	GeometryUtils.intersection(viewbox, coordbox, vis_cbox);
	double block_cwidth = vis_cbox.width / visible_count;

	block_cbox.reshape(0, 0, block_cwidth, block_height);
	view.transformToPixels(block_cbox, block_pbox);
	int block_pwidth = block_pbox.width;

        double offset = vis_cbox.x;
	for (int i=0; i<snpcount; i++) {
	  double snploc = snp_locs.get(i);
	  if (snploc >= viewmin && snploc <= viewmax) {
	    char base = snp_residues.charAt(i);
	    boolean is_variant = snp_variants[i];
	    g.setColor(this.getColor());

	    // draw SNP at it's genomic location
	    snp_cbox.reshape(snploc, coordbox.y, 1.0, snp_height);
	    view.transformToPixels(snp_cbox, snp_pbox);
	    if (snp_pbox.width < 1) { snp_pbox.width = 1; }
	    g.fillRect(snp_pbox.x, snp_pbox.y, snp_pbox.width, snp_pbox.height);

	    // draw SNP as a fitted block
	    block_cbox.reshape(offset, coordbox.y + 20, block_cwidth, block_height);
	    view.transformToPixels(block_cbox, block_pbox);
	    if (block_pbox.width < 1) { block_pbox.width = 1; }

            if (is_variant) {
	      g.setColor(Color.red);
	    }
	    else {
	      g.setColor(Color.lightGray);
	    }
	    g.fillRect(block_pbox.x, block_pbox.y, block_pbox.width, block_pbox.height);
	    g.setColor(this.getColor());
	    g.drawRect(block_pbox.x, block_pbox.y, block_pbox.width, block_pbox.height);

	    // draw lines connecting genomic location to fitted block
	    g.setColor(Color.gray);
	    g.drawLine(snp_pbox.x, snp_pbox.y+snp_pbox.height, 
		       block_pbox.x, block_pbox.y);
	    g.drawLine(snp_pbox.x+snp_pbox.width, snp_pbox.y+snp_pbox.height, 
		       block_pbox.x+block_pbox.width, block_pbox.y);

	    // draw SNP base
	    if (char_width < block_pwidth) {
	      g.setColor(Color.black);
	      String basestring = NSTRING;
	      if (base == 'A') { basestring = ASTRING; }
	      else if (base == 'C') { basestring = CSTRING; }
	      else if (base == 'G') { basestring = GSTRING; }
	      else if (base == 'T') { basestring = TSTRING; }
	      g.drawString(basestring, 
			   block_pbox.x + block_pbox.width/2 - char_width/2, 
			   block_pbox.y + block_pbox.height/2);
	    }
	    offset += block_cwidth;
	  }
	}
      }
    }
  }

  public void setSnps(IntList locs, String residues, boolean[] variants) {
    snp_locs = locs;
    snp_residues = residues;
    snp_variants = variants;
  }

  public IntList getSnpLocations() { return snp_locs; }
  
  public String getSnpResidues() { return snp_residues; }
  
  public boolean[] getSnpVariants() { return snp_variants; }

}
