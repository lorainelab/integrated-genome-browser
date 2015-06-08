package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.widget.NeoWidget;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.view.SeqMapView;
import com.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import com.lorainelab.igb.services.search.ITransformableTierGlyph;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.event.MouseInputAdapter;

/**
 * A class to handle resizing the tiers on a labeled tiered map. Tiers are
 * resized by adjusting the border between their labels. So far this is only for
 * vertical resizing and is only used by the TierLabelManager.
 *
 * @author blossome
 */
public class OldTierResizer extends MouseInputAdapter {

    private static final double RESIZE_THRESHOLD = 4.0;
    protected static Cursor[] ourCursors = new Cursor[2];

    static {
        ourCursors[0] = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
        ourCursors[1] = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
    }

    /**
     * Determines the scope of resizing. Given a border between two tiers
     * determine a list of tiers that will be affected by the resize. Note that
     * the returned list is of contiguous tiers. The top and bottom tiers will
     * be resized. Interior tiers that cannot be resized will just go along for
     * the ride.
     *
     * @param theFirst points to tier just above the border being dragged.
     * @param theTierMouseIsAbove points to the tier just below the border being
     * dragged.
     * @param theList of tiers that might be resized.
     * @return a maximal (possibly empty) section of theList such that the tiers
     * in this list can be resized and none of the others can.
     */
    private static List<TierLabelGlyph> pertinentTiers(int theFirst, int theTierMouseIsAbove, List<TierLabelGlyph> theList) {
        assert 0 <= theFirst;
        assert theFirst < theTierMouseIsAbove;
        assert theTierMouseIsAbove < theList.size();
        int top = theTierMouseIsAbove, limit = theTierMouseIsAbove;
        for (int i = theFirst; 0 <= i; i--) {
            TierLabelGlyph g = theList.get(i);
            if (g.isManuallyResizable()) {
                top = i;
                break;
            }
        }
        for (int i = theTierMouseIsAbove; i < theList.size(); i++) {
            TierLabelGlyph g = theList.get(i);
            if (g.isManuallyResizable()) {
                limit = i + 1;
                break;
            }
        }
        return theList.subList(top, limit);
    }
    private AffyLabelledTierMap tiermap;
    private SeqMapView gviewer = null;
    private double start;
    private double ourFloor, ourCeiling;

    private List<TierLabelGlyph> fixedInterior;
    private TierLabelGlyph lowerGl;
    private TierLabelGlyph upperGl;

    private boolean dragStarted = false; // it's our drag, we started it.

    /**
     * Construct a resizer for the given tiered map.
     */
    public OldTierResizer(AffyLabelledTierMap theDataTiers) {
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
        AffyTieredMap m = IGB.getInstance().getMapView().getSeqMap();
        assert m != src; // This seems odd.
        // Seems both cursors are the same, but you never know...
        if (atResizeTop(nevt)) {
            m.setCursor(ourCursors[0]);
        } else if (atResizeBottom(nevt)) {
            m.setCursor(ourCursors[1]);
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
        this.upperGl = theRegion.get(0);
        this.lowerGl = theRegion.get(theRegion.size() - 1);

        this.start = nevt.getCoordY();

        // These minimum heights are in coord space.
        // Shouldn't we be dealing in pixels?
        ourCeiling = this.upperGl.getCoordBox().getY()
                + this.upperGl.getMinimumHeight();
        java.awt.geom.Rectangle2D.Double box = this.lowerGl.getCoordBox();
        ourFloor = box.getY() + box.getHeight() - this.lowerGl.getMinimumHeight();

        this.fixedInterior = theRegion.subList(1, theRegion.size() - 1);
        for (TierLabelGlyph g : this.fixedInterior) {
            Rectangle2D.Double b = g.getCoordBox();
            double middle = b.y + b.height / 2;
            if (middle <= start) {
                ourCeiling += b.getHeight();
            }
            if (start < middle) {
                ourFloor -= b.getHeight();
            }
        }
    }

    /**
     * Get the mouse cursor right.
     */
    @Override
    public void mouseEntered(MouseEvent theEvent) {
        if (this.dragStarted) {
            AffyTieredMap m = IGB.getInstance().getMapView().getSeqMap();
            m.setCursor(ourCursors[0]);
        }
    }

    @Override
    public void mousePressed(MouseEvent evt) {

        // We only want to react when we're supposed to.
        // i.e. when we have set the mouse cursor.
        AffyTieredMap m = IGB.getInstance().getMapView().getSeqMap();
        assert m != evt.getSource(); // This seems odd.
        Cursor c = m.getCursor();
        if (c != ourCursors[0]
                && c != ourCursors[1]) {
            return;
        }

        if (null == this.gviewer) {
            this.gviewer = IGB.getInstance().getMapView();
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
        List<TierLabelGlyph> resizeRegion = null;
        if (atResizeTop(nevt)) {
            resizeRegion = pertinentTiers(index - 1, index, orderedGlyphs);
        } else if (atResizeBottom(nevt)) {
            resizeRegion = pertinentTiers(index, index + 1, orderedGlyphs);
        }
        if (null != resizeRegion && 1 < resizeRegion.size()) {
            startDrag(resizeRegion, nevt);
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

        if (this.upperGl != null && null != this.lowerGl) {
            if (ourCeiling < nevt.getCoordY() && nevt.getCoordY() < ourFloor) {
                double y = this.upperGl.getCoordBox().getY();
                double inital_height = this.upperGl.getCoordBox().getHeight();
                double height = inital_height + delta;
                this.upperGl.resizeHeight(y, height);
                this.upperGl.getReferenceTier().resizeHeight(0, height);
                if (this.upperGl.getReferenceTier() instanceof ITransformableTierGlyph) {
                    if (this.upperGl.getReferenceTier().getDirection() != StyledGlyph.Direction.REVERSE) {
                        ((ITransformableTierGlyph) this.upperGl.getReferenceTier()).setOffset(
                                ((ITransformableTierGlyph) this.upperGl.getReferenceTier()).getOffset() + (int) delta);
                    }
                }

                // Move the fixed height glyphs in the middle,
                // assuming that the list is sorted top to bottom.
                height = this.upperGl.getCoordBox().getHeight();
                y = this.upperGl.getCoordBox().getY() + height;
                for (TierLabelGlyph g : this.fixedInterior) {
                    g.resizeHeight(y, g.getCoordBox().getHeight());
                    g.getReferenceTier().resizeHeight(delta, g.getCoordBox().getHeight());
                }

                y = this.lowerGl.getCoordBox().getY() + delta;
                height = this.lowerGl.getCoordBox().getHeight() - delta;
                this.lowerGl.resizeHeight(y, height);
                this.lowerGl.getReferenceTier().resizeHeight(delta, height);
                if (this.lowerGl.getReferenceTier() instanceof ITransformableTierGlyph) {
                    if (this.lowerGl.getReferenceTier().getDirection() != StyledGlyph.Direction.REVERSE) {
                        ((ITransformableTierGlyph) this.lowerGl.getReferenceTier()).setOffset(
                                ((ITransformableTierGlyph) this.lowerGl.getReferenceTier()).getOffset() - (int) delta);
                    }
                }

                this.gviewer.getSeqMap().updateWidget();
            } else { // then we're out of bounds.
                // Ignore it.
                //System.err.println("TierResizer: Out of bounds.");
            }
        }

    }

    /**
     * Resize the data tiers to match the resized labels. The data tiers are
     * also repacked if necessary.
     */
    @Override
    public void mouseReleased(MouseEvent evt) {
        this.upperGl = null; // helps with garbage collection
        this.lowerGl = null; // helps with garbage collection
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
