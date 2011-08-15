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

import com.affymetrix.genometryImpl.GenometryModel;

import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;

import com.affymetrix.igb.tiers.TierLabelGlyph;

/**
 * A class to handle generic resizing of Glyphs (border between Glyphs)
 * on a NeoWidget.
 */
public class GlyphResizer implements MouseListener, MouseMotionListener {

	TierLabelGlyph lowerGl;
	TierLabelGlyph upperGl;
	boolean force_within_parent = false;
	NeoAbstractWidget widget;
	SeqMapView gviewer = null;
	double start;
	
	public GlyphResizer(NeoAbstractWidget widg, SeqMapView gviewer) {
		this.widget = widg;
		this.gviewer = gviewer;
	}

	/**
	 *  Start a drag.
	 */
	public void startDrag(TierLabelGlyph upperGl, TierLabelGlyph lowerGl, NeoMouseEvent nevt) {
		this.upperGl = upperGl;
		this.lowerGl = lowerGl;
		// flushing, just in case...
		widget.removeMouseListener(this);
		widget.removeMouseMotionListener(this);
		widget.addMouseListener(this);
		widget.addMouseMotionListener(this);
		
		start = nevt.getCoordY();
	}

	public void mouseMoved(MouseEvent evt) { }

	public void mouseDragged(MouseEvent evt) {
		if (!(evt instanceof NeoMouseEvent)) { return; }
		NeoMouseEvent nevt = (NeoMouseEvent)evt;
		double diff = nevt.getCoordY() - start;
		
		if (upperGl != null) {
			double height = upperGl.getCoordBox().getHeight() + diff;
			upperGl.resizeHeight(upperGl.getCoordBox().getY(), height);
		}
		if (lowerGl != null) {
			double height = lowerGl.getCoordBox().getHeight() - diff;
			lowerGl.resizeHeight(lowerGl.getCoordBox().getY() + diff, height);
		}

		start = nevt.getCoordY();
		gviewer.getSeqMap().updateWidget();
	}

	public void mousePressed(MouseEvent evt) { }
	public void mouseClicked(MouseEvent evt) { }
	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }

	public void mouseReleased(MouseEvent evt) {
		mouseDragged(evt);
		widget.removeMouseListener(this);
		widget.removeMouseMotionListener(this);
		
		if(upperGl != null){
			upperGl.getReferenceTier().setHeight(upperGl.getCoordBox().getHeight());
			upperGl = null; // helps with garbage collection
			
		}
		if(lowerGl != null){
			lowerGl.getReferenceTier().setHeight(lowerGl.getCoordBox().getHeight());
			lowerGl = null; // helps with garbage collection
		}
		
		gviewer.setAnnotatedSeq(GenometryModel.getGenometryModel().getSelectedSeq(), true, true);
	}
}
