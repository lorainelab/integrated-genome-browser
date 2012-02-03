package com.affymetrix.igb.shared;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;

public abstract class ViewModeGlyph extends SolidGlyph {
	public abstract String getViewMode();
	public abstract void setPreferredHeight(double height, ViewI view);
	public abstract void setFillColor(Color col);
	public abstract int getActualSlots();
	public abstract boolean toolBarHit(Rectangle2D.Double coord_hitbox, ViewI view);
}
