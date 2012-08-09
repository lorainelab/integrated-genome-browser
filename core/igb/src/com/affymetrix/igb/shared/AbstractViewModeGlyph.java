package com.affymetrix.igb.shared;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.util.Collections;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractViewModeGlyph extends ViewModeGlyph {
	
	@Override
	protected RootSeqSymmetry loadRegion(SeqSpan span){
		loadData(span);
		return (RootSeqSymmetry) this.getInfo();
	}
		
	@Override
	protected void updateParent(ViewModeGlyph vmg){
		//Do Nothing
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
			
			Application.getSingleton().addNotLockedUpMsg("Loading "+getAnnotStyle().getTrackName());
			
			return GeneralLoadUtils.loadFeaturesForSym(feature, optimized_sym);
		} catch (Exception ex) {
			Logger.getLogger(AbstractViewModeGlyph.class.getName()).log(Level.SEVERE, null, ex);
		}

		return Collections.<SeqSymmetry>emptyList();
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
	protected boolean shouldDrawToolBar(){
		return style.drawCollapseControl();
	}
		
	@Override
	public void setInfo(Object info) {
		super.setInfo(info);
		if(info != null && !(info instanceof RootSeqSymmetry)){
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "!!!!! {0} is not instance of RootSeqSymmetry !!!!!", info);
		}
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
	public void pack(ViewI view) {
		super.pack(view);
		// Make sure the parent is not too short.
		// This was needed for tiers in tiered maps.
		int minPixelHeight = getMinPixelsHeight();
		// P.S. Why isn't getMinPixelsHeight in GlyphI?
		int currentPixelHeight = getPixelBox(view).height;
		if (currentPixelHeight < minPixelHeight) {
			// Only do this for resizable tiers for now.
			// It would screw up the axis tier, for one.
			if (isManuallyResizable()) {
				Rectangle2D.Double oldBox = getCoordBox();
				Rectangle r = getPixelBox(view);
				r.height = minPixelHeight; // Make it tall enough.
				view.transformToCoords(r, oldBox);
				setCoords(oldBox.x, oldBox.y, oldBox.width, oldBox.height);
			}

		}
	}
	
}
