package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.widget.NeoWidget;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.event.MouseInputAdapter;

/**
 * A class to handle resizing the tiers on a labeled tiered map. Tiers are
 * resized by adjusting the border between their labels. So far this is only for
 * vertical resizing and is only used by the TierLabelManager. This one resizes
 * uniformly all the resizable tiers. What happens when one of them reaches a
 * minimum? Keep resizing others? If so, how do we expand again? Backtrack? TODO
 * There is a (not very harmful) bug. When there is a fixed height tier at the
 * top or bottom of the selection then the mouse indicates resizability at the
 * penultimate border. However such resizing is not possible, so the mouse
 * cursor should not indicate such.
 *
 * @author blossome
 */
public class AccordionTierResizer extends MouseInputAdapter {

    private static final double RESIZE_THRESHOLD = 4.0;

    /**
     * Determines the scope of resizing. Given a border between two tiers
     * determine a list of tiers that will be affected by the resize. Note that
     * the returned list is of contiguous tiers. All tiers in the region than
     * can be resized will be. Interior tiers that cannot be resized will just
     * go along for the ride.
     *
     * @param theTierMouseIsAbove points to the tier just below the border being
     * dragged.
     * @param theList of tiers that might be resized.
     * @return a maximal (possibly empty) section of theList such that some
     * tiers in this list can be resized.
     */
    private static List<TierLabelGlyph> pertinentTiers(int theTierMouseIsAbove, List<TierLabelGlyph> theList) {
        assert 0 <= theTierMouseIsAbove;
        assert theTierMouseIsAbove < theList.size();
        int top = theTierMouseIsAbove, limit;
        for (int i = 0; i < theTierMouseIsAbove; i++) {
            TierLabelGlyph g = theList.get(i);
            if (g.isManuallyResizable()) {
                top = i;
                break;
            }
        }
        limit = top;
        for (int i = theList.size() - 1; theTierMouseIsAbove <= i; i--) {
            TierLabelGlyph g = theList.get(i);
            if (g.isManuallyResizable()) {
                limit = i;
                break;
            }
        }
        return theList.subList(top, limit + 1);
    }

    private static List<TierLabelGlyph> consideringSelection(int theTierMouseIsAbove, List<TierLabelGlyph> theList) {
        int top = 0, limit = theList.size() - 1;
        if (0 < theTierMouseIsAbove
                && (theList.get(theTierMouseIsAbove).isSelected()
                || theList.get(theTierMouseIsAbove - 1).isSelected())) {
            if (theList.get(theTierMouseIsAbove).isSelected()) {
                for (int i = theTierMouseIsAbove; i < theList.size(); i++) {
                    if (!theList.get(i).isSelected()) {
                        limit = i - 1;
                        break;
                    }
                }
            }
            if (theList.get(theTierMouseIsAbove - 1).isSelected()) {
                for (int i = theTierMouseIsAbove - 1; 0 <= i; i--) {
                    if (!theList.get(i).isSelected()) {
                        top = i + 1;
                        break;
                    }
                }
            }
        }
        return theList.subList(top, limit + 1);
    }
    private AffyLabelledTierMap tiermap;
    private SeqMapView gviewer = null;
    private double start;
    private double ourFloor, ourCeiling;
    private List<TierLabelGlyph> resizeRegion;
    private int atBorder; // points to the tier immediately below the mouse pointer.
    private boolean wasResizeable = false;

    private boolean dragStarted = false; // It's our drag; we started it.

    /**
     * Construct a resizer for the given tiered map.
     */
    public AccordionTierResizer(AffyLabelledTierMap theDataTiers) {
        assert null != theDataTiers;
        this.tiermap = theDataTiers;
    }

    /**
     * Manage the mouse cursor indicating when a resizing drag is possible.
     */
    @Override
    public void mouseMoved(MouseEvent theEvent) {
        NeoMouseEvent nevt = (NeoMouseEvent) theEvent;
        Object src = theEvent.getSource();
        AffyTieredMap m = Application.getSingleton().getMapView().getSeqMap();
        assert m != (AffyTieredMap) src; // This seems odd.
        // Seems both cursors are the same, but you never know...
        if (atResizeTop(nevt)) {
            m.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
            wasResizeable = true;
        } else if (atResizeBottom(nevt)) {
            m.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
            wasResizeable = true;
        } else if (wasResizeable) {
            m.setCursor(Application.getSingleton().getMapView().getMapMode().defCursor);
            wasResizeable = false;
        }
        // Otherwise, leave it alone. Other listeners can (and will) handle it.
    }

    /**
     * Establish some context and boundaries for the drag.
     *
     * @param theRegion is a list of contiguous tiers affected by the resize.
     * @param nevt is the event starting the drag.
     */
    public void startDrag(List<TierLabelGlyph> theRegion, NeoMouseEvent nevt) {
        this.dragStarted = true;

        this.start = nevt.getCoordY();

        // These minimum heights are in coord space.
        // Shouldn't we be dealing in pixels?
        ourCeiling = 0;
        ourFloor = Double.POSITIVE_INFINITY;
    }

    /**
     * Get the mouse cursor right.
     */
    @Override
    public void mouseEntered(MouseEvent theEvent) {
        if (this.dragStarted) {
            AffyTieredMap m = Application.getSingleton().getMapView().getSeqMap();
            m.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
        }
    }

    @Override
    public void mousePressed(MouseEvent evt) {

        // We only want to react when we're supposed to.
        // i.e. when we have set the mouse cursor.
        AffyTieredMap m = Application.getSingleton().getMapView().getSeqMap();
        assert m != evt.getSource(); // This seems odd.
        Cursor c = m.getCursor();
        if (c != Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
                && c != Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)) {
            return;
        }

        if (null == this.gviewer) {
            this.gviewer = Application.getSingleton().getMapView();
            assert null != this.gviewer;
        }
        NeoMouseEvent nevt = (NeoMouseEvent) evt;
        List<GlyphI> glyphsClicked = nevt.getItems();
        GlyphI topgl = null;
        if (!glyphsClicked.isEmpty()) {
            // DANGER: Herin lies secret knowlege of another object.
            // The list of label glyphs will be in order from bottom to top.
            topgl = glyphsClicked.get(glyphsClicked.size() - 1);
            // Slower, but more prudent would be to check the coord boxes.
        }
        List<TierLabelGlyph> orderedGlyphs = tiermap.getOrderedTierLabels();
        int index = orderedGlyphs.indexOf(topgl);
        this.atBorder = -1;
        if (atResizeTop(nevt)) {
            this.atBorder = index;
        } else if (atResizeBottom(nevt)) {
            this.atBorder = index + 1;
        }
        if (-1 < this.atBorder) {
            TierLabelGlyph tierMouseIsAbove = orderedGlyphs.get(this.atBorder);
            this.resizeRegion = consideringSelection(this.atBorder, orderedGlyphs);
            this.atBorder = this.resizeRegion.indexOf(tierMouseIsAbove);
            this.resizeRegion = pertinentTiers(this.atBorder, resizeRegion);
            if (null != this.resizeRegion) {
                this.atBorder = this.resizeRegion.indexOf(tierMouseIsAbove);
                if (1 < this.resizeRegion.size()) {
                    startDrag(this.resizeRegion, nevt);
                }
            }
        }
    }

    /**
     * Adjust the tier labels on either side of the mouse pointer. This
     * adjustment is going on in scene space rather than pixel space. That
     * doesn't seem quite right. - elb
     *
     * @param evt is the drag event.
     */
    @Override
    public void mouseDragged(MouseEvent evt) {
        if (!this.dragStarted) {
            return;
        }
        NeoMouseEvent nevt = (NeoMouseEvent) evt;
        double delta = nevt.getCoordY() - this.start;
        this.start = nevt.getCoordY();

        if (!(this.ourCeiling < this.start && this.start < this.ourFloor)) {
            return;
        }

        // Count 'em.
        int n = 0;
        for (int i = 0; i < this.atBorder; i++) {
            TierLabelGlyph g = this.resizeRegion.get(i);
            if (g.isManuallyResizable()) {
                n += 1;
            }
        }
        int m = 0;
        for (int i = this.atBorder; i < this.resizeRegion.size(); i++) {
            TierLabelGlyph g = this.resizeRegion.get(i);
            if (g.isManuallyResizable()) {
                m += 1;
            }
        }

        // Move 'em.
        double miniDelta = delta / n;
        double y = this.resizeRegion.get(0).getCoordBox().getY();
        for (int i = 0; i < this.atBorder; i++) {
            TierLabelGlyph g = this.resizeRegion.get(i);
            if (g.isManuallyResizable()) {
                // Resize the variable height glyphs.
                double height = g.getCoordBox().getHeight() + miniDelta;
                if (height <= g.getMinimumHeight()) {
                    if (0 < delta) {
                        this.ourFloor = this.start;
                    } else if (delta < 0) {
                        this.ourCeiling = this.start;
                    }
                    height = Math.max(height, g.getMinimumHeight());
                }
                g.resizeHeight(y, height);
            } else {
                // Move the fixed height glyphs.
                g.resizeHeight(y, g.getCoordBox().getHeight());
            }
            y += g.getCoordBox().height;
        }
        miniDelta = delta / m;
        for (int i = this.atBorder; i < this.resizeRegion.size(); i++) {
            TierLabelGlyph g = this.resizeRegion.get(i);
            if (g.isManuallyResizable()) {
                // Resize the variable height glyphs.
                double height = g.getCoordBox().getHeight() - miniDelta;
                if (height <= g.getMinimumHeight()) {
                    if (0 < delta) {
                        this.ourFloor = this.start;
                    } else if (delta < 0) {
                        this.ourCeiling = this.start;
                    }
                    height = Math.max(height, g.getMinimumHeight());
                }
                g.resizeHeight(y, height);
            } else {
                // Move the fixed height glyphs.
                g.resizeHeight(y, g.getCoordBox().getHeight());
            }
            y += g.getCoordBox().height;
        }
        // What about last one? Do we need to avoid rounding errors above?

        this.gviewer.getSeqMap().updateWidget();

    }

    /**
     * Resize the data tiers to match the resized labels. The data tiers are
     * also repacked if necessary.
     */
    @Override
    public void mouseReleased(MouseEvent evt) {

        if (!this.dragStarted) {
            return;
        }

        assert this.resizeRegion != null;
        this.dragStarted = false;
        boolean needRepacking = (1 < this.resizeRegion.size());

        for (TierLabelGlyph g : this.resizeRegion) {
            if (g.isManuallyResizable()) {
                TierGlyph tg = g.getReferenceTier();
                tg.setPreferredHeight(
                        g.getCoordBox().height,
                        this.gviewer.getSeqMap().getView()
                );

            }
        }

        if (needRepacking) {
            com.affymetrix.igb.tiers.AffyTieredMap m = this.gviewer.getSeqMap();
            com.affymetrix.igb.tiers.AffyLabelledTierMap lm
                    = (com.affymetrix.igb.tiers.AffyLabelledTierMap) m;
            boolean full_repack = true, stretch_vertically = true;
            lm.repackTheTiers(full_repack, stretch_vertically);
        }

    }

    /**
     * Indicates that the mouse is over the resizing border at the top of a
     * label glyph.
     */
    private boolean atResizeTop(NeoMouseEvent nevt) {
        if (nevt == null || nevt.getItems().isEmpty()) {
            return false;
        }
        GlyphI topgl = nevt.getItems().get(nevt.getItems().size() - 1);
        NeoWidget w = (NeoWidget) nevt.getSource();
        LinearTransform trans = w.getView().getTransform();
        double threshold = RESIZE_THRESHOLD / trans.getScaleY();
        if (threshold < nevt.getCoordY() - topgl.getCoordBox().getY()) {
            // then not at the top of this glyph.
            // So, not at the top of any tier.
            return false;
        }
        List<TierLabelGlyph> orderedGlyphs = tiermap.getOrderedTierLabels();
        int index = orderedGlyphs.indexOf(topgl);
        int i;
        for (i = index; i < orderedGlyphs.size(); i++) {
            // Keep going down looking for one that is resizable.
            if (orderedGlyphs.get(i).isManuallyResizable()) {
                break;
            }
        }
        if (orderedGlyphs.size() <= i) {
            // No resizable tiers below this point.
            return false;
        }
        for (i = index - 1; 0 <= i; i--) {
            // Keep going up looking for one that is resizable.
            if (orderedGlyphs.get(i).isManuallyResizable()) {
                break;
            }
        }
        return (0 <= i);
    }

    /**
     * Indicates that the mouse is over the resizing border at the bottom of a
     * label glyph.
     */
    private boolean atResizeBottom(NeoMouseEvent nevt) {
        if (nevt == null || nevt.getItems().isEmpty()) {
            return false;
        }
        GlyphI topgl = nevt.getItems().get(nevt.getItems().size() - 1);
        NeoWidget w = (NeoWidget) nevt.getSource();
        LinearTransform trans = w.getView().getTransform();
        double threshhold = RESIZE_THRESHOLD / trans.getScaleY();
        if (threshhold
                < topgl.getCoordBox().getY() + topgl.getCoordBox().getHeight()
                - nevt.getCoordY()) { // then not at the bottom of this glyph.
            // So, not at the bottom of any tier.
            return false;
        }
        List<TierLabelGlyph> orderedGlyphs = tiermap.getOrderedTierLabels();
        int index = orderedGlyphs.indexOf(topgl);
        int i;
        for (i = index + 1; i < orderedGlyphs.size(); i++) {
            // Keep going down looking for one that is resizable.
            if (orderedGlyphs.get(i).isManuallyResizable()) {
                break;
            }
        }
        if (orderedGlyphs.size() <= i) {
            // No resizable tiers below this point.
            return false;
        }
        for (i = index; 0 <= i; i--) {
            // Keep going up looking for one that is resizable.
            if (orderedGlyphs.get(i).isManuallyResizable()) {
                break;
            }
        }
        return (0 <= i);
    }

}
