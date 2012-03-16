package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Map;

public abstract class ViewModeGlyph extends SolidGlyph {
	private TierGlyph tierGlyph;
	public abstract void setPreferredHeight(double height, ViewI view);
	public abstract Color getFillColor();
	public abstract void setFillColor(Color col);
	public abstract int getActualSlots();
	@Override
	public abstract ITrackStyleExtended getAnnotStyle();
	public abstract void setStyle(ITrackStyleExtended style);
	public abstract String getLabel();
	public abstract void setLabel(String str);
	public abstract Direction getDirection();
	public abstract void setDirection(Direction d);
	public abstract Map<String,Class<?>> getPreferences();
	public abstract void setPreferences(Map<String,Object> preferences);
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
	// TODO remove this method
	public abstract void addMiddleGlyph(GlyphI gl);
}
