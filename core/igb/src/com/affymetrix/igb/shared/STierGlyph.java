package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.AbstractCoordPacker;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class STierGlyph extends SolidGlyph implements TierGlyph {
	protected ITrackStyleExtended style;
	/** glyphs to be drawn in the "middleground" --
	 *    in front of the solid background, but behind the child glyphs
	 *    For example, to indicate how much of the xcoord range has been covered by feature retrieval attempts
	 */
	private final List<GlyphI> middle_glyphs = new ArrayList<GlyphI>();
	/*
	 * other_fill_color is derived from fill_color whenever setFillColor() is called.
	 * if there are any "middle" glyphs, then background is drawn with other_fill_color and
	 *    middle glyphs are drawn with fill_color
	 * if no "middle" glyphs, then background is drawn with fill_color
	 */
	protected Color other_fill_color = null;
	protected Direction direction = Direction.NONE;
	private static final int handle_width = 10;  // width of handle in pixels
	private final Rectangle pixel_hitbox = new Rectangle();  // caching rect for hit detection
	
	/**
	 * Determine how short a glyph can be so we can avoid empty vertical space.
	 * Originally implemented for annotation tracks.
	 * Here we hope for a {@link GraphSym} as the glyph's info.
	 * If we don't find one, we return the answer from the super class.
	 * Subclasses can specialize this, of course.
	 * TODO Do we want y max? or |y max - y min| or [y max|?
	 *      or even max(|y min|, [y max|)?
	 *      The old basic graph glyph used to flip y values
	 *      because pixels start at 0 and go negative.
	 * @param theView limits the data to consider.
	 * @return How tall the glyph must be to show all the data in view.
	 *         Cannot be negative?
	 */
	public abstract int getSlotsNeeded(ViewI theView);
	public abstract void setPreferredHeight(double height, ViewI view);
	public abstract int getActualSlots();

	protected abstract boolean shouldDrawToolBar();
	

	public abstract Map<String, Class<?>> getPreferences();
	public abstract void setPreferences(Map<String, Object> preferences);
	protected void updateParent(STierGlyph vmg){
		//Do Nothing
	}
	
	public STierGlyph(ITrackStyleExtended style) {
		setHitable(false);
		setStyle(style);
	}
	
	protected RootSeqSymmetry loadRegion(SeqSpan span){
		loadData(span);
		return (RootSeqSymmetry) this.getInfo();
	}
	
	protected List<SeqSymmetry> loadData(SeqSpan span) {
		try {
			GenericFeature feature = style.getFeature();
			if(feature == null){
				return Collections.<SeqSymmetry>emptyList();
			}
			
			SeqSymmetry optimized_sym = feature.optimizeRequest(span);
			if (optimized_sym == null) {
				return Collections.<SeqSymmetry>emptyList();
			}
			
			Application.getSingleton().addNotLockedUpMsg("Loading "+ style.getTrackName());
			
			return GeneralLoadUtils.loadFeaturesForSym(feature, optimized_sym);
		} catch (Exception ex) {
			Logger.getLogger(STierGlyph.class.getName()).log(Level.SEVERE, null, ex);
		}

		return Collections.<SeqSymmetry>emptyList();
	}
	
	public final void copyChildren(STierGlyph temp) {
		if(temp == null)
			return;
		
		List<GlyphI> childrens = new ArrayList<GlyphI>();
		childrens.addAll(temp.getChildren());

		for (int i = 0; i < childrens.size(); i++) {
			addChild(childrens.get(i));
		}
		
		childrens.clear();
		childrens.addAll(temp.middle_glyphs);
		
		for (int i = 0; i < childrens.size(); i++) {
			addMiddleGlyph(childrens.get(i));
		}
		
		//TODO: Set list of all getInfo
//		if(!(getInfo() instanceof List)){
//			List<Object> info = new ArrayList<Object>();
//			info.add(getInfo());
//			setInfo(info);
//		}else{
//			((List)(getInfo())).add(temp.getInfo());
//		}
		
	}
	
	@Override
	public void resizeHeight(double diffy, double height) {
		Rectangle2D.Double cbox = getCoordBox();
		setCoords(cbox.x, cbox.y, cbox.width, height);
		this.moveRelative(0, diffy);
	}
		
	public void setStyle(ITrackStyleExtended style) {
		this.style = style;

		// most tier glyphs ignore their foreground color, but AffyTieredLabelMap copies
		// the fg color to the TierLabel glyph, which does pay attention to that color.
		setForegroundColor(style.getForeground());
		setFillColor(style.getBackground());

		//If any visibilty bug occurs, fix here. -HV 22/03/2012
		setVisibility(style.getShow());
	}
	
	public ITrackStyleExtended getAnnotStyle() {
		return style;
	}
	
	/** Returns the color used to draw the tier background, or null
	 * if there is no background. 
	 */
	public final Color getFillColor() {
		return style.getBackground();
	}
	
	/**
	 * Sets direction.
	 */
	public void setDirection(TierGlyph.Direction d) {
		direction = d;
	}
	
	/**
	 * Gets direction
	 * @return 
	 */
	public TierGlyph.Direction getDirection(){
		return direction;
	}
	
	protected final double getSpacing() {
		if(getPacker() instanceof AbstractCoordPacker){
			return ((AbstractCoordPacker)getPacker()).getSpacing();
		}
		return 2;
	}
	
	public boolean initUnloaded() {
		Glyph glyph;

		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();//smv.getAnnotatedSeq();
		SeqSymmetry sym = new SimpleMutableSeqSymmetry();
		double height = style.getHeight();
		if (!style.isGraphTier()) {
			height = style.getLabelField() == null || style.getLabelField().isEmpty() ? height : height * 2;
		}

		if (style.getFeature() != null) {
			sym = style.getFeature().getRequestSym();
		}

		// Add middle glyphs.
		SeqSymmetry inverse = SeqUtils.inverse(sym, seq);
		int child_count = inverse.getChildCount();
		//If any request was made.
		if (child_count > 0) {
			for (int i = 0; i < child_count; i++) {
				SeqSymmetry child = inverse.getChild(i);
				for (int j = 0; j < child.getSpanCount(); j++) {
					SeqSpan ospan = child.getSpan(j);
					if (ospan.getLength() > 1) {
						glyph = new FillRectGlyph();
						glyph.setCoords(ospan.getMin(), 0, ospan.getLength() - 1, 0);
						addMiddleGlyph(glyph);
					}
				}
			}
		} else {
			glyph = new FillRectGlyph();
			glyph.setCoords(seq.getMin(), 0, seq.getLength() - 1, 0);
			addMiddleGlyph(glyph);
		}
		return false;
	}

	public final void setFillColor(Color col) {
		if (style.getBackground() != col) {
			style.setBackground(col);
		}

		// Now set the "middleground" color based on the fill color
		if (col == null) {
			other_fill_color = Color.DARK_GRAY;
		} else {
			int intensity = col.getRed() + col.getGreen() + col.getBlue();
			if (intensity == 0) {
				other_fill_color = Color.darkGray;
			} else if (intensity > (255 + 127)) {
				other_fill_color = col.darker();
			} else {
				other_fill_color = col.brighter();
			}
		}
	}
	
	public final void addMiddleGlyph(GlyphI gl) {
		middle_glyphs.add(gl);
	}

	public void drawMiddle(ViewI view) {
		view.transformToPixels(getCoordBox(), getPixelBox());

		getPixelBox().width = Math.max(getPixelBox().width, getMinPixelsWidth());
		getPixelBox().height = Math.max(getPixelBox().height, getMinPixelsHeight());

		Graphics g = view.getGraphics();
		Rectangle vbox = view.getPixelBox();
		setPixelBox(getPixelBox().intersection(vbox));

		if (middle_glyphs.isEmpty()) { // no middle glyphs, so use fill color to fill entire tier
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				//Hack : Add one to height to resolve black line bug.
				g.fillRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height + 1);
			}
		} else {
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				//Hack : Add one to height to resolve black line bug.
				g.fillRect(getPixelBox().x, getPixelBox().y, 2 * getPixelBox().width, getPixelBox().height + 1);
			}

			// cycle through "middleground" glyphs,
			//   make sure their coord box y and height are set to same as TierGlyph,
			//   then call mglyph.draw(view)
			// TODO: This will draw middle glyphs on the Whole Genome, which appears to cause problems due to coordinates vs. pixels
			// See bug 3032785
			if (other_fill_color != null) {
				for (GlyphI mglyph : middle_glyphs) {
					Rectangle2D.Double mbox = mglyph.getCoordBox();
					mbox.setRect(mbox.x, getCoordBox().y, mbox.width, getCoordBox().height);
					mglyph.setColor(other_fill_color);
					mglyph.drawTraversal(view);
				}
			}
		}
	}

	/**
	 * Remove all children of the glyph, including those added with
	 * addMiddleGlyph(GlyphI).
	 */
	@Override
	public void removeAllChildren() {
		super.removeAllChildren();
		// also remove all middleground glyphs
		// this is currently the only place where middleground glyphs are treated as if they were children
		//   maybe should rename this method clear() or something like that...
		// only reference to middle glyphs should be in this.middle_glyphs, so should be able to GC them by
		//     clearing middle_glyphs.  These glyphs never have setScene() called on them,
		//     so it is not necessary to call setScene(null) on them.
		middle_glyphs.clear();
	}
	
	public Rectangle2D.Double getTierCoordBox() {
		return getCoordBox();
	}
	
	public boolean isManuallyResizable() {
		return true;
	}
	
	/****************** ViewModeGlyph Method **********************************/
	public boolean toolBarHit(Rectangle2D.Double coord_hitbox, ViewI view){
		if (shouldDrawToolBar() && isVisible() && coord_hitbox.intersects(getCoordBox())) {
			// overlapping handle ?  (need to do this one in pixel space?)
			Rectangle hpix = new Rectangle();
			view.transformToPixels(coord_hitbox, hpix);
			if (getToolbarPixel(view).intersects(hpix)) {
				return true;
			}
		}
		return false;
	}
	
	protected Rectangle getToolbarPixel(ViewI view){
		pixel_hitbox.setBounds(getPixelBox().x + 4, getPixelBox().y + 4, handle_width, handle_width);
		return pixel_hitbox;
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
