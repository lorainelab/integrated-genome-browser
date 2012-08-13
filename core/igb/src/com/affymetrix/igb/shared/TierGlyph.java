 package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.widget.NeoMap;
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
		
	/**
	 * make the glyph a regular (nonfloating) glyph
	 * @param floater the PixelFloaterGlyph
	 * @param floatingGlyph the glyph
	 */
	void defloat(Glyph floater, ViewModeGlyph floatingGlyph);

	/**
	 * Make the glyph a regular (nonjoined) glyph.
	 * @param comboGlyph the comboGlyph
	 * @param joinedGlyph the glyph
	 */
	void dejoin(ViewModeGlyph comboGlyph, ViewModeGlyph joinedGlyph);

	/**
	 * Make the glyph a floating glyph.
	 * Note - the viewModeGlyph will leave its tierGlyph pointing to this glyph.
	 * @param floater the PixelFloaterGlyph
	 */
	void enfloat(Glyph floater, NeoMap map);

	/**
	 * Make the viewModeGlyph a joined glyph.
	 * Note - the viewModeGlyph will leave its tierGlyph pointing to this glyph.
	 * @param comboGlyph the ComboGlyph
	 */
	void enjoin(ViewModeGlyph comboGlyph, NeoMap map);

	Rectangle2D.Double getTierCoordBox();

	ViewModeGlyph getViewModeGlyph();

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
