/**
 * Copyright (c) 2001-2004 Affymetrix, Inc.
 * 
* Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 * 
* The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.util.NeoConstants;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
/**
 * A convenient base class for glyphs that are "solid", meaning any event within
 * the coordinate bounds of the glyph is considered to hit the glyph.
 *
 * Mainly a convenience so other Glyphs don't have to implement hit methods if
 * they are willing to stick with simple hits.
 */
public class EfficientSolidGlyph extends Glyph {

	public boolean is_Compulsary = false;
	protected int direction = NeoConstants.NONE;
	static final protected int minHeight = 24;
	public static final BasicStroke dashStroke0 = new BasicStroke(1f, BasicStroke.CAP_SQUARE,
			BasicStroke.JOIN_MITER, 10.0f, new float[]{1, 2, 5, 3}, 0);
	public static final BasicStroke dashStroke1 = new BasicStroke(1f, BasicStroke.CAP_SQUARE,
			BasicStroke.JOIN_MITER, 10, new float[]{1, 10}, 1);
	public static final BasicStroke dashStroke2 = new BasicStroke(1f, BasicStroke.CAP_SQUARE,
			BasicStroke.JOIN_MITER, 10, new float[]{1, 10}, 2);
	public static final BasicStroke dashStrokeNeg0 = new BasicStroke(1f, BasicStroke.CAP_SQUARE,
			BasicStroke.JOIN_MITER, 10.0f, new float[]{1, 3, 5, 2}, 11);
	public static final BasicStroke dashStrokeNeg1 = new BasicStroke(1f, BasicStroke.CAP_SQUARE,
			BasicStroke.JOIN_MITER, 10, new float[]{1, 10}, 10);
	public static final BasicStroke dashStrokeNeg2 = new BasicStroke(1f, BasicStroke.CAP_SQUARE,
			BasicStroke.JOIN_MITER, 10, new float[]{1, 10}, 9);

	public EfficientSolidGlyph(){
		this.setHitable(true);
	}
	
	/**
	 * Draws a line with little arrows to indicate the direction.
	 *
	 * @param direction should be {@link NeoConstants#RIGHT},
	 *  {@link NeoConstants#LEFT}, or {@link NeoConstants#NONE}.
	 */
	static void drawDirectedLine(Graphics g, final int x, final int y, final int width, final int direction) {
		switch (direction) {
			case NeoConstants.RIGHT:
				Graphics2D g2R = (Graphics2D) g;
				Stroke old_strokeR = g2R.getStroke();
				g2R.setStroke(dashStroke0);
				g2R.drawLine(x, y, x + width, y);
				g2R.setStroke(dashStroke1);
				g2R.drawLine(x, y + 1, x + width, y + 1);
				g2R.drawLine(x, y - 1, x + width, y - 1);
				g2R.setStroke(dashStroke2);
				g2R.drawLine(x, y + 2, x + width, y + 2);
				g2R.drawLine(x, y - 2, x + width, y - 2);
				g2R.setStroke(old_strokeR);
				break;
			case NeoConstants.LEFT:
				Graphics2D g2L = (Graphics2D) g;
				Stroke old_strokeL = g2L.getStroke();
				g2L.setStroke(dashStrokeNeg0);
				g2L.drawLine(x, y, x + width, y);
				g2L.setStroke(dashStrokeNeg1);
				g2L.drawLine(x, y + 1, x + width, y + 1);
				g2L.drawLine(x, y - 1, x + width, y - 1);
				g2L.setStroke(dashStrokeNeg2);
				g2L.drawLine(x, y + 2, x + width, y + 2);
				g2L.drawLine(x, y - 2, x + width, y - 2);
				g2L.setStroke(old_strokeL);
				break;
			default:
				g.fillRect(x, y, width, 1);
		}
	}

	public void setDirection(int dir){
		direction = dir;
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
		return isHitable() && isVisible() && coord_hitbox.intersects(this.getCoordBox());
	}

	/**
	 * @return whether or not this glyph show useful information.
	 */
	public boolean isCompulsary() {
		return is_Compulsary;
	}

	public void setCompulsary(boolean isComp) {
		is_Compulsary = isComp;
	}
}
