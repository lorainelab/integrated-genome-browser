 package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.List;

public interface TierGlyph extends GlyphI {

	public static enum Direction {
		FORWARD(" (+)"), NONE(""), REVERSE(" (-)"), BOTH(" (+/-)"), AXIS("");
		private final String display;
		private Direction(String display) {
			this.display = display;
		}
		public String getDisplay() {
			return display;
		}
	};
	
	public ITrackStyleExtended getAnnotStyle();
	
	public void setStyle(ITrackStyleExtended annotStyle);
	
	public Direction getDirection();
	
	public void setDirection(Direction direction);
	
	public void addMiddleGlyph(GlyphI mglyph);
	
	public List<SeqSymmetry> getSelected();

	public int getActualSlots();
		
	public int getSlotsNeeded(ViewI ourView);

	public boolean isManuallyResizable();

	public void resizeHeight(double d, double height);
		
	public double getChildHeight();
	
	public void setPreferredHeight(double maxHeight, ViewI view);
	
	public void setMinimumPixelBounds(Graphics graphics);
	
	public boolean toolBarHit(Rectangle2D.Double hitrect, ViewI view);
	
}
