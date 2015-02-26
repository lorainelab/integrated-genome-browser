/**
 * Copyright (c) 1998-2005 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License").
 * A copy of the license must be included with any distribution of
 * this source code.
 * Distributions from Affymetrix, Inc., place this in the
 * IGB_LICENSE.html file.
 *
 * The license is also available at
 * http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

/**
 * A class to handle generic resizing of Glyphs (border between Glyphs)
 * on a NeoWidget.
 * So far this is only for vertical resizing and is only used by the
 * TierLabelManager.
 * Perhaps it should be renamed to reflect this
 * or generalized to handle other cases.
 *
 * @deprecated replaced by com.affymetrix.igb.tiers.TierResizer - elb
 */
@Deprecated
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
     * Construct a resizer with a given widget and view.
     *
     * @param widg
     * @param gviewer
     */
    public GlyphResizer(NeoAbstractWidget widg, SeqMapView gviewer) {
        this.widget = widg;
        this.gviewer = gviewer;
    }

    /**
     * Establish some context and boundaries for the drag.
     *
     * @param theRegion is a list of contiguous tiers affected by the resize.
     * @param nevt is the event starting the drag.
     */
    public void startDrag(List<TierLabelGlyph> theRegion, NeoMouseEvent nevt) {
        this.upperGl = theRegion.get(0);
        this.lowerGl = theRegion.get(theRegion.size() - 1);
        // flushing, just in case...
        widget.removeMouseListener(this);
        widget.removeMouseMotionListener(this);
        widget.addMouseListener(this);
        widget.addMouseMotionListener(this);

        start = nevt.getCoordY();

        // These minimum heights are in coord space. Shouldn't we be dealing in pixels?
        ourCeiling = this.upperGl.getCoordBox().getY() + this.upperGl.getMinimumHeight();
        java.awt.geom.Rectangle2D.Double box = this.lowerGl.getCoordBox();
        ourFloor = box.getY() + box.getHeight() - this.lowerGl.getMinimumHeight();

        this.fixedInterior = theRegion.subList(1, theRegion.size() - 1);
        for (TierLabelGlyph g : this.fixedInterior) {
            java.awt.geom.Rectangle2D.Double b = g.getCoordBox();
            if (b.getY() <= start) {
                ourCeiling += b.getHeight();
            }
            if (start <= b.getY()) {
                ourFloor -= b.getHeight();
            }
        }

    }

    @Override
    public void mouseMoved(MouseEvent evt) {
    }

    @Override
    public void mouseDragged(MouseEvent evt) {
        if (evt instanceof NeoMouseEvent) {
            neoMouseDragged((NeoMouseEvent) evt);
        }
    }

    /**
     * Adjust the tiers on either side of the mouse pointer.
     * This adjustment is going on in coord space rather than pixel space.
     * That doesn't seem quite right. - elb
     *
     * @param nevt is the drag event.
     */
    private void neoMouseDragged(NeoMouseEvent nevt) {
        double diff = nevt.getCoordY() - start;

        if (this.upperGl != null && null != this.lowerGl) {
            if (ourCeiling < nevt.getCoordY() && nevt.getCoordY() < ourFloor) {
                double height = this.upperGl.getCoordBox().getHeight() + diff;
                this.upperGl.resizeHeight(this.upperGl.getCoordBox().getY(), height);

                // Move the fixed height glyphs in the middle,
                // assuming that the list is sorted top to bottom.
                double y = this.upperGl.getCoordBox().getY() + this.upperGl.getCoordBox().getHeight();
                for (TierLabelGlyph g : this.fixedInterior) {
                    g.resizeHeight(y, g.getCoordBox().getHeight());
                    y += g.getCoordBox().getHeight();
                }

                height = this.lowerGl.getCoordBox().getHeight() - diff;
                this.lowerGl.resizeHeight(this.lowerGl.getCoordBox().getY() + diff, height);
                this.gviewer.getSeqMap().updateWidget();
            } else { // then we're out of bounds.
                // Ignore it.
                //System.err.println("Out of bounds.");
            }
        } else {
            System.err.println("NO UPPER GLYPH or NO LOWER GLYPH");
        }

        start = nevt.getCoordY();
    }

    public void mousePressed(MouseEvent evt) {
    }

    public void mouseClicked(MouseEvent evt) {
    }

    public void mouseEntered(MouseEvent evt) {
    }

    public void mouseExited(MouseEvent evt) {
    }

    /**
     * Finish the resizing and clean up.
     *
     * @param evt is the event ending the mouse drag.
     */
    @Override
    public void mouseReleased(MouseEvent evt) {
        mouseDragged(evt); // Not sure this is needed. - elb
        widget.removeMouseListener(this);
        widget.removeMouseMotionListener(this);

        boolean needRepacking = (this.upperGl != null && this.lowerGl != null);

        if (this.upperGl != null) {
            com.lorainelab.igb.genoviz.extensions.TierGlyph gl = this.upperGl.getReferenceTier();
            gl.setPreferredHeight(this.upperGl.getCoordBox().getHeight(), this.gviewer.getSeqMap().getView());
        }

        if (this.lowerGl != null) {
            com.lorainelab.igb.genoviz.extensions.TierGlyph gl = this.lowerGl.getReferenceTier();
            gl.setPreferredHeight(this.lowerGl.getCoordBox().getHeight(), this.gviewer.getSeqMap().getView());
        }

        if (needRepacking) {

			// This was in the code before, commented out.
            // It was also suggested by Lance.
            // It doesn't work.
            // It seems to snap the axis back to the center.
            // It also wants to enforce vertical symetry.
            //this.gviewer.setAnnotatedSeq(com.affymetrix.genometry.GenometryModel.getInstance().getSelectedSeq(), true, true, true);
            // This is also resizing and repositioning the labels and the tiers.
            // After a few resizings things get pretty confused.
            // This is happening even when no data are loaded and, hence, there are no glyphs in the tiers.
            // Otherwise, it sort of works.
            // We would like the label sizes and positions to remain as they are before this call.
            // - elb
            com.affymetrix.igb.tiers.AffyTieredMap m = this.gviewer.getSeqMap();
            assert ((com.affymetrix.igb.tiers.AffyLabelledTierMap) m).getLabelMap() == this.widget : "this.widget is not this.gviewer.getSeqMap().getLabelMap().";
            com.affymetrix.igb.tiers.AffyLabelledTierMap lm = (com.affymetrix.igb.tiers.AffyLabelledTierMap) m;
            boolean full_repack = true, stretch_vertically = true;
            lm.repackTheTiers(full_repack, stretch_vertically);
			//lm.repackTiersToLabels();
            // The above repack (either one I think) changes (enlarges) the tier map's bounds. This probably affects the tiers' spacing. - elb 2012-02-21

			// Vanilla repack seems to have worse symptoms.
            //m.repack();
            //m.packTiers(true, false, false, true);
            // This was also commented out.
            // From the name "kludgeRepackingTheTiers" it looks like someone tried a specialized repack.
            // Don't know who or how far they got.
            //com.affymetrix.igb.tiers.AffyTieredMap m = this.gviewer.getSeqMap();
            //if (m instanceof com.affymetrix.igb.tiers.AffyLabelledTierMap) {
            //	com.affymetrix.igb.tiers.AffyLabelledTierMap lm = (com.affymetrix.igb.tiers.AffyLabelledTierMap) m;
            //	lm.kludgeRepackingTheTiers(needRepacking, needRepacking, needRepacking);
            //}
            // The above may not have worked, but
            // it would seem we need something to repack the tiers based on the label glyphs' height and position.
            // Would have thought that's what the last paramater in repackTheTiers was for.
            // Just updating the widget doesn't work.
            // Weird things happen in the data tiers.
            // The backgrounds do not change. So it looks like the tiers remain fixed,
            // but the data glyphs all get painted at the top of the widget.
            //this.gviewer.getSeqMap().updateWidget();
            //this.widget.updateWidget();
            //this.widget.updateWidget(true); // full repacking
        }

        this.upperGl = null; // helps with garbage collection
        this.lowerGl = null; // helps with garbage collection
    }

}
