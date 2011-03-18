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
import com.affymetrix.igb.tiers.AffyTieredMap;
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
	}

	public void mouseMoved(MouseEvent evt) { }

	public void mouseDragged(MouseEvent evt) {
		if (!(evt instanceof NeoMouseEvent)) { return; }
		NeoMouseEvent nevt = (NeoMouseEvent)evt;

		AffyTieredMap map = gviewer.getSeqMap();

		if (upperGl != null) {
			double height = (nevt.getCoordY() - upperGl.getCoordBox().getY()) - upperGl.getSpacing();
			upperGl.resizeHeight(upperGl.getCoordBox().getY(), height, map.getView());
		}
		if (lowerGl != null) {
			double height = (lowerGl.getCoordBox().getY() + lowerGl.getCoordBox().getHeight() - nevt.getCoordY()) - 3 * lowerGl.getSpacing();
			lowerGl.resizeHeight(nevt.getCoordY() + lowerGl.getSpacing(), height, map.getView());
		}

		map.packTiers(false, true, false);
		map.stretchToFit(false, true);
		map.updateWidget();
	}

	public void mousePressed(MouseEvent evt) { }
	public void mouseClicked(MouseEvent evt) { }
	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }

	public void mouseReleased(MouseEvent evt) {
		mouseDragged(evt);
		widget.removeMouseListener(this);
		widget.removeMouseMotionListener(this);
		lowerGl = null; // helps with garbage collection
		upperGl = null; // helps with garbage collection
	}
}
