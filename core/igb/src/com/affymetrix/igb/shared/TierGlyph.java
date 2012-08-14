 package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
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
		
	public String getLabel();

	public void resizeHeight(double d, double height);

	public List<SeqSymmetry> getSelected();
		
	public void setStyle(ITrackStyleExtended annotStyle);

	public void setLabel(String trackName);

	public void setDirection(Direction direction);

	public void addMiddleGlyph(GlyphI mglyph);

	public boolean inside(int x, int y);

	public int getActualSlots();
		
	public int getSlotsNeeded(ViewI ourView);

	public ITrackStyleExtended getAnnotStyle();

	public Direction getDirection();

	public boolean isManuallyResizable();

	public void setPreferredHeight(double maxHeight, ViewI view);
		
	AbstractViewModeGlyph getViewModeGlyph();

	boolean initUnloaded();

	boolean isGarbage();

	void makeGarbage();

	void setUnloadedOK(boolean unloadedOK);
	
	
	/***** ViewModeGlyph Methods ********/
	
//	public TierGlyph getTierGlyph();

//	public void setTierGlyph(TierGlyph aThis);
//	
//	public void drawMiddle(ViewI view);
//
//	public List<GlyphI> getMiddleGlyphs();
//	
//	public void copyChildren(ViewModeGlyph vmg);
//
//	public void processParentCoordBox(Rectangle2D.Double coordBox);
//
//	public double getChildHeight();
//	
//	public void setMinimumPixelBounds(Graphics graphics);
//
//	public boolean toolBarHit(Rectangle2D.Double hitrect, ViewI view);
	
	/***** ViewModeGlyph Methods ********/
    
}
