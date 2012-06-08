package com.affymetrix.igb.shared;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractViewModeGlyph extends ViewModeGlyph {
	protected ITrackStyleExtended style;
	protected Direction direction = Direction.NONE;
	/*
	 * other_fill_color is derived from fill_color whenever setFillColor() is called.
	 * if there are any "middle" glyphs, then background is drawn with other_fill_color and
	 *    middle glyphs are drawn with fill_color
	 * if no "middle" glyphs, then background is drawn with fill_color
	 */
	protected Color other_fill_color = null;
	/** glyphs to be drawn in the "middleground" --
	 *    in front of the solid background, but behind the child glyphs
	 *    For example, to indicate how much of the xcoord range has been covered by feature retrieval attempts
	 */
	protected final List<GlyphI> middle_glyphs = new ArrayList<GlyphI>();
	private static final int handle_width = 10;  // width of handle in pixels
	private final Rectangle pixel_hitbox = new Rectangle();  // caching rect for hit detection
	protected String label = null;
	
	@Override
	public ITrackStyleExtended getAnnotStyle() {
		return style;
	}

	public void setStyle(ITrackStyleExtended style) {
		this.style = style;

		// most tier glyphs ignore their foreground color, but AffyTieredLabelMap copies
		// the fg color to the TierLabel glyph, which does pay attention to that color.
		setForegroundColor(style.getForeground());
		setFillColor(style.getBackground());

		//If any visibilty bug occurs, fix here. -HV 22/03/2012
		setVisibility(style.getShow());
		setLabel(style.getTrackName());
	}

	@Override
	public Direction getDirection() {
		return direction;
	}

	/**
	 * Sets direction.
	 */
	@Override
	public void setDirection(Direction d) {
		direction = d;
	}

	@Override
	public void setLabel(String str) {
		label = str;
	}

	@Override
	public String getLabel() {
		return label;
	}

	/** Returns the color used to draw the tier background, or null
	if there is no background. */
	public Color getFillColor() {
		return style.getBackground();
	}

	/** Sets the color used to fill the tier background, or null if no color
	 *  @param col  A color, or null if no background color is desired.
	 */
	@Override
	public void setFillColor(Color col) {
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
	
	@Override
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
				g.fillRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height+1);
			}
		} else {
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				//Hack : Add one to height to resolve black line bug.
				g.fillRect(getPixelBox().x, getPixelBox().y, 2 * getPixelBox().width, getPixelBox().height+1);
			}

			// cycle through "middleground" glyphs,
			//   make sure their coord box y and height are set to same as TierGlyph,
			//   then call mglyph.draw(view)
			// TODO: This will draw middle glyphs on the Whole Genome, which appears to cause problems due to coordinates vs. pixels
			// See bug 3032785
			if(other_fill_color != null){
				for (GlyphI mglyph : middle_glyphs) {
					Rectangle2D.Double mbox = mglyph.getCoordBox();
					mbox.setRect(mbox.x, getCoordBox().y, mbox.width, getCoordBox().height);
					mglyph.setColor(other_fill_color);
					mglyph.drawTraversal(view);
				}
			}
		}
	}

	@Override
	public void drawTraversal(ViewI view)  {
		super.drawTraversal(view);
		if (shouldDrawToolBar()) {
			drawExpandCollapse(view);
		}
	}
			
	private void drawExpandCollapse(ViewI view) {
		Rectangle hpix = getToolbarPixel(view);
		if (hpix != null) {
			Graphics g = view.getGraphics();
			g.setColor(Color.WHITE);
			g.fill3DRect(hpix.x, hpix.y, hpix.width, hpix.height, true);
//			g.drawOval(hpix.x, hpix.y, hpix.width, hpix.height);
			g.setColor(Color.BLACK);
			g.drawRect(hpix.x, hpix.y, hpix.width, hpix.height);
			g.drawLine(hpix.x + hpix.width/5, hpix.y + hpix.height/2, hpix.x + hpix.width - hpix.width/5, hpix.y + hpix.height/2);
			if(style.getCollapsed()){
				g.drawLine(hpix.x + hpix.width/2, hpix.y + hpix.height/5, hpix.x + hpix.width/2, hpix.y + hpix.height - hpix.height/5);
			}
		}
	}
		
	@Override
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
		
	@Override
	protected boolean shouldDrawToolBar(){
		return style.drawCollapseControl();
	}

	private Rectangle getToolbarPixel(ViewI view){
		pixel_hitbox.setBounds(getPixelBox().x + 4, getPixelBox().y + 4, handle_width, handle_width);
		return pixel_hitbox;
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
	@Override
	public void addMiddleGlyph(GlyphI gl) {
		middle_glyphs.add(gl);
	}
	
	/**
	 * Remove all children of the glyph,
	 * including those added with addMiddleGlyph(GlyphI).
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
	
	@Override
	public void setInfo(Object info) {
		super.setInfo(info);
		if(info != null && !(info instanceof RootSeqSymmetry)){
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "!!!!! {0} is not instance of RootSeqSymmetry !!!!!", info);
		}
	}
	
		
	@Override
	public void copyChildren(ViewModeGlyph temp) {
		if(temp == null)
			return;
		
		List<GlyphI> childrens = new ArrayList<GlyphI>();
		childrens.addAll(temp.getChildren());

		for (int i = 0; i < childrens.size(); i++) {
			addChild(childrens.get(i));
		}
		
		childrens.clear();
		childrens.addAll(((AbstractViewModeGlyph)temp).middle_glyphs);
		
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
	public List<SeqSymmetry> getSelected(){
	
		int childCount = getChildCount();
		List<SeqSymmetry> selectedSyms = new ArrayList<SeqSymmetry>(childCount);
		for(int i=0;i<childCount;i++){
			if(getChild(i).isSelected()){
				if(getChild(i).getInfo() instanceof SeqSymmetry){
					selectedSyms.add((SeqSymmetry)(getChild(i).getInfo()));
				}
			}
		}
		return selectedSyms;
	}

	@Override
	public boolean initUnloaded() {
		Glyph glyph;

		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();//smv.getAnnotatedSeq();
		SeqSymmetry sym = new SimpleMutableSeqSymmetry();
		double height = style.getHeight();
		if(!style.isGraphTier()){
			height = style.getLabelField() == null || style.getLabelField().isEmpty() ? height : height * 2;
		}
		
		if(style.getFeature() != null){
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
	
	@Override
	public double getChildHeight(){
		return style.getHeight();
	}
	
}
