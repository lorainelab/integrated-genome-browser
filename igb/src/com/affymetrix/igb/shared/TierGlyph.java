package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.igb.glyph.MapViewModeHolder;

import java.awt.Color;
import java.util.*;
import java.awt.geom.Rectangle2D;

/**
 *  TierGlyph is intended for use with AffyTieredMap.
 *  Each tier in the TieredNeoMap is implemented as a TierGlyph, which can have different
 *  states as indicated below.
 *  In a AffyTieredMap, TierGlyphs pack relative to each other but not to other glyphs added
 *  directly to the map.
 *
 */
public class TierGlyph extends SolidGlyph {
	private ViewModeGlyph viewModeGlyph;

	public Direction direction = Direction.NONE;
	/** glyphs to be drawn in the "middleground" --
	 *    in front of the solid background, but behind the child glyphs
	 *    For example, to indicate how much of the xcoord range has been covered by feature retrieval attempts
	 */
	private final List<GlyphI> middle_glyphs = new ArrayList<GlyphI>();

	public static enum Direction {

		FORWARD, NONE, REVERSE, BOTH, AXIS
	};
	/** A property for the IAnnotStyle.getTransientPropertyMap().  If set to
	 *  Boolean.TRUE, the tier will draw a label next to where the handle
	 *  would be.
	 *  Note: You probably do NOT want the TierGlyph to draw a label and for the
	 *  included GraphGlyph to also draw a label.
	 */
	public static final String SHOW_TIER_LABELS_PROPERTY = "Show Track Labels";
	/** A property for the IAnnotStyle.getTransientPropertyMap().  If set to
	 *  Boolean.TRUE, the tier will draw a handle on the left side.
	 *  Note: You probably do NOT want the TierGlyph to draw a handle and for the
	 *  included GraphGlyph to also draw a handle.
	 */
	public static final String SHOW_TIER_HANDLES_PROPERTY = "Show Track Handles";

	/*
	 * other_fill_color is derived from fill_color whenever setFillColor() is called.
	 * if there are any "middle" glyphs, then background is drawn with other_fill_color and
	 *    middle glyphs are drawn with fill_color
	 * if no "middle" glyphs, then background is drawn with fill_color
	 */
	private String label = null;
	private ITrackStyleExtended style;

	public TierGlyph(ITrackStyleExtended style) {
		setHitable(false);
		setStyle(style);
	}

	private MapViewGlyphFactoryI getViewGlyphFactory(String viewMode) {
		// TO DO - cannot access class outside com.affymetrix.igb.shared
		return MapViewModeHolder.getInstance().getViewFactory(viewMode);
	}

	public final void setStyle(ITrackStyleExtended style) {
		this.style = style;
		if (viewModeGlyph == null || !viewModeGlyph.getViewMode().equals(style.getViewMode())) {
			MapViewGlyphFactoryI factory = getViewGlyphFactory(style.getViewMode());
			viewModeGlyph = factory.getViewModeGlyph((SeqSymmetry)getInfo(), style);
		}
	}

	public ITrackStyleExtended getAnnotStyle() {
		return style;
	}

	/**
	 *  Adds "middleground" glyphs, which are drawn in front of the background but
	 *    behind all "real" child glyphs.
	 *  These are generally not considered children of
	 *    the glyph.  The TierGlyph will render these glyphs, but they can't be selected since they
	 *    are not considered children in pickTraversal() method.
	 *  The only way to remove these is via removeAllChildren() method,
	 *    there is currently no external access to them.
	 */
	public final void addMiddleGlyph(GlyphI gl) {
		middle_glyphs.add(gl);
	}
	@Override
	public void addChild(GlyphI glyph, int position) {
		throw new RuntimeException("TierGlyph.addChild(glyph, position) not allowed, "
				+ "use TierGlyph.addChild(glyph) instead");
	}

	// overriding addChild() to keep track of whether children are sorted
	//    by ascending min
	@Override
	public void addChild(GlyphI glyph) {
		viewModeGlyph.addChild(glyph);
	}

	public final void setLabel(String str) {
		label = str;
	}

	public final String getLabel() {
		return label;
	}

	public void drawTraversal(ViewI view) {
		viewModeGlyph.drawTraversal(view);
	}

	// overriding pack to ensure that tier is always the full width of the scene
	@Override
	public void pack(ViewI view, boolean manual) {
		viewModeGlyph.pack(view, manual);
	}

	public List<GlyphI> getMiddle_glyphs() {
		return middle_glyphs;
	}

	/**
	 *  Overridden to allow background shading by a collection of non-child
	 *    "middleground" glyphs.  These are rendered after the solid background but before
	 *    all of the children (which could be considered the "foreground").
	 */
	@Override
	public void draw(ViewI view) {
		viewModeGlyph.draw(view);
	}

	protected boolean shouldDrawToolBar(){
		return style.drawCollapseControl();
	}
	public boolean toolBarHit(Rectangle2D.Double coord_hitbox, ViewI view){
		return viewModeGlyph.toolBarHit(coord_hitbox, view);
	}

	/**
	 *  Remove all children of the glyph, including those added with
	 *  addMiddleGlyph(GlyphI).
	 */
	@Override
	public void removeAllChildren() {
		viewModeGlyph.removeAllChildren();
	}

	/** Sets the color used to fill the tier background, or null if no color
	 *  @param col  A color, or null if no background color is desired.
	 */
	public final void setFillColor(Color col) {
		viewModeGlyph.setFillColor(col);
	}

	// very, very deprecated
	@Override
	public Color getColor() {
		return getForegroundColor();
	}

	// very, very deprecated
	@Override
	public void setColor(Color c) {
		setForegroundColor(c);
	}

	/** Returns the color used to draw the tier background, or null
	if there is no background. */
	public final Color getFillColor() {
		return style.getBackground();
	}

	@Override
	public void setForegroundColor(Color color) {
		if (style.getForeground() != color) {
			style.setForeground(color);
		}
	}

	@Override
	public Color getForegroundColor() {
		return style.getForeground();
	}

	@Override
	public void setBackgroundColor(Color color) {
		setFillColor(color);
	}

	@Override
	public Color getBackgroundColor() {
		return getFillColor();
	}

	public final Direction getDirection() {
		return direction;
	}

	/**
	 *  Sets direction.  Must be one of DIRECTION_FORWARD, DIRECTION_REVERSE,
	 *  DIRECTION_BOTH or DIRECTION_NONE.
	 */
	public final void setDirection(Direction d) {
		this.direction = d;
	}

	public int getActualSlots(){
		return viewModeGlyph.getActualSlots();
	}

	public void setPreferredHeight(double height, ViewI view){
		viewModeGlyph.setPreferredHeight(height, view);
	}

	/** Not implemented.  Will behave the same as drawSelectedOutline(ViewI). */
	@Override
	protected void drawSelectedFill(ViewI view) {
		this.drawSelectedOutline(view);
	}

	/** Not implemented.  Will behave the same as drawSelectedOutline(ViewI). */
	@Override
	protected void drawSelectedReverse(ViewI view) {
		this.drawSelectedOutline(view);
	}

}
