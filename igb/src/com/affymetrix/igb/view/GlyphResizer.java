/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.igb.view;

import java.awt.event.*;
import java.util.List;

import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;

import com.affymetrix.igb.tiers.TierLabelGlyph;

/**
 * A class to handle generic resizing of Glyphs (border between Glyphs)
 * on a NeoWidget.
 * So far this is only for vertical resizing and is only used by the TierLabelManager.
 * Perhaps it should be renamed to reflect this
 * or generalized to handle other cases.
 */
public class GlyphResizer implements MouseListener, MouseMotionListener {

	TierLabelGlyph lowerGl;
	TierLabelGlyph upperGl;
	boolean force_within_parent = false;
	NeoAbstractWidget widget;
	SeqMapView gviewer = null;
	double start;
	private double ourFloor, ourCeiling;
	private List<TierLabelGlyph> fixedInterior;
	
	/**
	 * Tiers should have a minimum height. Perhaps they do. Look into it.
	 * The minimum height should probably be tall enough to keep the little +/- box discernable.
	 * That box can be used to expand or collapse, respectively, the tier.
	 * Hard code this for now.
	 * This should probably be in pixels rather than scene or view coordinates.
	 * Or whatever determines the size of the +/- boxes.
	 */
	private final double minimumTierHeight = 10.0;
	
	/**
	 * Construct a resizer with a given widget and view.
	 * @param widg
	 * @param gviewer 
	 */
	public GlyphResizer(NeoAbstractWidget widg, SeqMapView gviewer) {
		this.widget = widg;
		this.gviewer = gviewer;
	}

	/**
	 * Establish some context and boundaries for the drag.
	 * @param theRegion is a list of contiguous tiers affected by the resize.
	 * @param nevt is the event starting the drag.
	 */
	public void startDrag(List<TierLabelGlyph> theRegion, NeoMouseEvent nevt) {
		this.upperGl = theRegion.get(0);
		this.lowerGl = theRegion.get(theRegion.size()-1);
		// flushing, just in case...
		widget.removeMouseListener(this);
		widget.removeMouseMotionListener(this);
		widget.addMouseListener(this);
		widget.addMouseMotionListener(this);
		
		start = nevt.getCoordY();
		
		ourCeiling = this.upperGl.getCoordBox().getY() + minimumTierHeight;
		java.awt.geom.Rectangle2D.Double box = this.lowerGl.getCoordBox();
		ourFloor = box.getY() + box.getHeight() - minimumTierHeight;
		
		this.fixedInterior = theRegion.subList(1, theRegion.size()-1);
		for (TierLabelGlyph g: this.fixedInterior) {
			java.awt.geom.Rectangle2D.Double b = g.getCoordBox();
			if (b.getY() <= start) {
				ourCeiling += b.getHeight();
			}
			if (start <= b.getY()) {
				ourFloor -= b.getHeight();
			}
		}
		
	}

	public void mouseMoved(MouseEvent evt) { }

	/**
	 * Adjust the tiers on either side of the mouse pointer.
	 * @param evt is the drag event.
	 */
	public void mouseDragged(MouseEvent evt) {
		if (!(evt instanceof NeoMouseEvent)) { return; }
		NeoMouseEvent nevt = (NeoMouseEvent)evt;
		double diff = nevt.getCoordY() - start;

		if (upperGl != null && null != lowerGl) {
			if (ourCeiling < nevt.getCoordY() && nevt.getCoordY() < ourFloor) {
				double height = upperGl.getCoordBox().getHeight() + diff;
				upperGl.resizeHeight(upperGl.getCoordBox().getY(), height);
				
				// Move the fixed height glyphs in the middle.
				double y = upperGl.getCoordBox().getY() + upperGl.getCoordBox().getHeight();
				for (TierLabelGlyph g: this.fixedInterior) {
					g.resizeHeight(y, g.getCoordBox().getHeight());
					y += g.getCoordBox().getHeight();
				}
				
				height = lowerGl.getCoordBox().getHeight() - diff;
				lowerGl.resizeHeight(lowerGl.getCoordBox().getY() + diff, height);
				gviewer.getSeqMap().updateWidget();
			}
			else { // then we're out of bounds.
				// Ignore it.
				//System.err.println("Out of bounds.");
			}
		}
		else {
			System.err.println("NO UPPER GLYPH or NO LOWER GLYPH");
		}

		start = nevt.getCoordY();
	}

	public void mousePressed(MouseEvent evt) { }
	public void mouseClicked(MouseEvent evt) { }
	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }

	/**
	 * Finish the resizing and clean up.
	 * @param evt is the event ending the mouse drag.
	 */
	public void mouseReleased(MouseEvent evt) {
		mouseDragged(evt);
		widget.removeMouseListener(this);
		widget.removeMouseMotionListener(this);
		
		boolean needRepacking = (upperGl != null && lowerGl != null); // KLUDGE for now
		
		if(upperGl != null){
			upperGl.getReferenceTier().setPreferredHeight(upperGl.getCoordBox().getHeight(), gviewer.getSeqMap().getView());
			//upperGl.pack(gviewer.getSeqMap().getView(), false);
			upperGl = null; // helps with garbage collection
			
		}
		if(lowerGl != null){
			lowerGl.getReferenceTier().setPreferredHeight(lowerGl.getCoordBox().getHeight(), gviewer.getSeqMap().getView());
			//lowerGl.pack(gviewer.getSeqMap().getView(), false);
			lowerGl = null; // helps with garbage collection
		}
		
		if (needRepacking) {
		//gviewer.setAnnotatedSeq(GenometryModel.getGenometryModel().getSelectedSeq(), true, true, true);
		gviewer.getSeqMap().repackTheTiers(true, true, false);
			//com.affymetrix.igb.tiers.AffyTieredMap m = gviewer.getSeqMap();
			//if (m instanceof com.affymetrix.igb.tiers.AffyLabelledTierMap) {
			//	com.affymetrix.igb.tiers.AffyLabelledTierMap lm = (com.affymetrix.igb.tiers.AffyLabelledTierMap) m;
			//	lm.kludgeRepackingTheTiers(needRepacking, needRepacking, needRepacking);
			//}
		}
	}
}
