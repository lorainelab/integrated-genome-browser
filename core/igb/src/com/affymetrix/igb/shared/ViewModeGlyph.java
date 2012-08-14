package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;


public interface ViewModeGlyph extends GlyphI{
	
	public ITrackStyleExtended getAnnotStyle();
	
	public TierGlyph getTierGlyph();

	public java.util.List<SeqSymmetry> getSelected();

	public boolean isManuallyResizable();

	public String getLabel();

	public TierGlyph.Direction getDirection();
	
//	public void drawMiddle(ViewI view);
//	
//	public void setLabel(String str);
//
//	public int getSlotsNeeded(ViewI theView);
//
//	public void setPreferredHeight(double height, ViewI view);
//
//	public int getActualSlots();
//
//	public void copyChildren(ViewModeGlyph vmg);
//
//	public void setTierGlyph(TierGlyph aThis);
//
//	public void processParentCoordBox(Double coordBox);
//
//	public void setStyle(ITrackStyleExtended style);
//
//	public boolean initUnloaded();
//
//	public void setDirection(Direction d);
//
//	public boolean isGarbage();
//
//	public void addMiddleGlyph(GlyphI gl);
//
//	public double getChildHeight();
//
//	public Font getFont();
//
//	public Rectangle getPixelBox();
//
//	public boolean drawTransients();
//
//	public boolean hit(Rectangle pixel_hitbox, ViewI view);
//
//	public boolean inside(int x, int y);
//
//	public boolean intersects(Rectangle rect);
//
//	public int getDrawOrder();
//
//	public void calcPixels(ViewI view);
//
//	public void drawSelected(ViewI view);
//
//	public void setDrawOrder(int order);
//
//	public void setFont(Font f);
//
//	public void setHitable(boolean hitable);
//
//	public List<GlyphI> getMiddleGlyphs();
//
//	public void setMinimumPixelBounds(Graphics graphics);
//
//	public boolean toolBarHit(Double hitrect, ViewI view);

}
