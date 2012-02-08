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
	
	/**
	 * Tiers should have a minimum height. Perhaps they do. Look into it.
	 * The minimum height should probably be tall enough to keep the little +/- box discernable.
	 * That box can be used to expand or collapse, respectively, the tier.
	 * Hard code this for now.
	 * This should probably be in pixels rather than scene or view coordinates.
	 * Or whatever determines the size of the +/- boxes.
	 */
	private final double minimumTierHeight = 10.0;
	
	public GlyphResizer(NeoAbstractWidget widg, SeqMapView gviewer) {
		this.widget = widg;
		this.gviewer = gviewer;
	}

	/**
	 *  Start a drag.
	 */
	public void startDrag(java.util.List<TierLabelGlyph> theRegion, NeoMouseEvent nevt) {
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
		
		java.util.List<TierLabelGlyph> fixedInterior = theRegion.subList(1, theRegion.size()-1);
		for (TierLabelGlyph g: fixedInterior) {
			System.out.println("adjusting");
			java.awt.geom.Rectangle2D.Double b = g.getCoordBox();
			if (b.getY() <= start) {
				System.out.println("adjusting ceiling");
				ourCeiling += b.getHeight();
			}
			if (start <= b.getY()) {
				System.out.println("adjusting floor");
				ourFloor -= b.getHeight();
			}
		}
		/*
		if (atResizeTop(nevt)) {
			// adjust bottom up by what? last tier height?
		}
		if (atResizeBottom(nevt)) {
			// adjust ceiling down by what? first tier height?
		}
		/* */
		
	}

	public void mouseMoved(MouseEvent evt) { }

	public void mouseDragged(MouseEvent evt) {
		if (!(evt instanceof NeoMouseEvent)) { return; }
		NeoMouseEvent nevt = (NeoMouseEvent)evt;
		double diff = nevt.getCoordY() - start;

		if (upperGl != null && null != lowerGl) {
			//if (upperGl.getCoordBox().getY() + minimumTierHeight < nevt.getCoordY() && nevt.getCoordY() + minimumTierHeight < (lowerGl.getCoordBox().getY() + lowerGl.getCoordBox().getHeight())) {
			if (ourCeiling < nevt.getCoordY() && nevt.getCoordY() < ourFloor) {
				double height = upperGl.getCoordBox().getHeight() + diff;
				upperGl.resizeHeight(upperGl.getCoordBox().getY(), height);
				height = lowerGl.getCoordBox().getHeight() - diff;
				lowerGl.resizeHeight(lowerGl.getCoordBox().getY() + diff, height);
				gviewer.getSeqMap().updateWidget();
			}
			else {
				System.err.println("Out of bounds.");
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
