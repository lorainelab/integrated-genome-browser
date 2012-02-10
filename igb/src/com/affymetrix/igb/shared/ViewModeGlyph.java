package com.affymetrix.igb.shared;

import java.awt.Color;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;

public abstract class ViewModeGlyph extends SolidGlyph implements StyleGlyphI {
	public abstract String getViewMode();
	public abstract void setPreferredHeight(double height, ViewI view);
	public abstract Color getFillColor();
	public abstract void setFillColor(Color col);
	public abstract int getActualSlots();
	public abstract ITrackStyleExtended getAnnotStyle();
	public abstract String getLabel();
	public abstract void setLabel(String str);
	public abstract Direction getDirection();
	public abstract void setDirection(Direction d);
	// TODO remove this method
	public abstract void addMiddleGlyph(GlyphI gl);
}
