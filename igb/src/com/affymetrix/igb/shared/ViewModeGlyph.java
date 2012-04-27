package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;

/**
 *  !!! the TierGlyph and ViewModeGlyph and all its subordinate ViewModeGlyphs should
 *  all contain the same instance of CoordBox. !!!
 *  This is the glyph that displays the contents of a Tier/Track. Each TierGlyph
 *  contains a ViewModeGlyph and delegates all calls to the ViewModeGlyph.
 */
public abstract class ViewModeGlyph extends SolidGlyph {
	private TierGlyph tierGlyph;
	public abstract void setPreferredHeight(double height, ViewI view);
	public abstract Color getFillColor();
	public abstract void setFillColor(Color col);
	public abstract int getActualSlots();
	public abstract ITrackStyleExtended getAnnotStyle();
	public abstract void setStyle(ITrackStyleExtended style);
	public abstract String getLabel();
	public abstract void setLabel(String str);
	public abstract Direction getDirection();
	public abstract void setDirection(Direction d);
	public abstract void drawMiddle(ViewI view);
	public abstract Map<String,Class<?>> getPreferences();
	public abstract void setPreferences(Map<String,Object> preferences);
	public abstract void copyChildren(ViewModeGlyph temp);
	public abstract void addMiddleGlyph(GlyphI gl);	
	public abstract List<SeqSymmetry> getSelected();
	public abstract void initUnloaded();
	public abstract boolean toolBarHit(Rectangle2D.Double coord_hitbox, ViewI view);
	protected abstract boolean shouldDrawToolBar();
	public boolean isGarbage() {
		return getChildCount() == 0;
	}
	public boolean isCombo() {
		return false;
	}
	public boolean isPreLoaded() {
		return false;
	}
	public void addSym(SeqSymmetry sym) {}
	public TierGlyph getTierGlyph() {
		return tierGlyph;
	}
	public void setTierGlyph(TierGlyph tierGlyph) {
		this.tierGlyph = tierGlyph;
	}
	public void processParentCoordBox(Rectangle2D.Double parentCoordBox) {
		setCoordBox(parentCoordBox); // so all use the same coordbox
	}
	public int getSlotsNeeded(ViewI theView) {
		return 1;
	}
	public boolean isManuallyResizable() {
		if (this.getPacker() instanceof CollapsePacker) {
			return false;
		}
		return true;
	}
	public final void setMinimumPixelBounds(Graphics g){
		java.awt.FontMetrics fm = g.getFontMetrics();
		int h = fm.getHeight();
		h += 2 * 2; // border height
		h += 4; // padding top
		int w = fm.stringWidth("A Moderate Label");
		w += 2; // border left
		w += 4; // padding left
		java.awt.Dimension minTierSizeInPixels = new java.awt.Dimension(w, h);
		setMinimumPixelBounds(minTierSizeInPixels);
	}
}
