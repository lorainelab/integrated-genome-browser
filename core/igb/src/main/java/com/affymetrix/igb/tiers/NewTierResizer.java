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
import java.util.List;
import javax.swing.event.MouseInputAdapter;

/**
 *
 * @author blossome
 */
public class NewTierResizer extends MouseInputAdapter {

    private static final double RESIZE_THRESHOLD = 4.0;
    protected static final Cursor ourCursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
    private AffyLabelledTierMap tiermap;
    private SeqMapView gviewer = null;
    private double start;
    private double ourCeiling;

    private List<TierLabelGlyph> fixedInterior;
    private TierLabelGlyph upperGl;

    private boolean dragStarted = false; // it's our drag, we started it.

    /**
     * Construct a resizer for the given tiered map.
     */
    public NewTierResizer(AffyLabelledTierMap theDataTiers) {
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
        if (atResizeBottom(nevt)) {
            m.setCursor(ourCursor);
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
        this.start = nevt.getCoordY();

        // These minimum heights are in coord space.
        // Shouldn't we be dealing in pixels?
        ourCeiling = this.upperGl.getCoordBox().getY()
                + this.upperGl.getMinimumHeight();
        //ourFloor = ourCeiling + upperGl.getReferenceTier().getViewModeGlyph().getChildHeight() * upperGl.getReferenceTier().getActualSlots();
        this.fixedInterior = theRegion;
    }

    /**
     * Get the mouse cursor right.
     */
    @Override
    public void mouseEntered(MouseEvent theEvent) {
        if (this.dragStarted) {
            AffyTieredMap m = IGB.getInstance().getMapView().getSeqMap();
            m.setCursor(ourCursor);
        }
    }

    @Override
    public void mousePressed(MouseEvent evt) {

        // We only want to react when we're supposed to.
        // i.e. when we have set the mouse cursor.
        AffyTieredMap m = IGB.getInstance().getMapView().getSeqMap();
        assert m != evt.getSource(); // This seems odd.
        Cursor c = m.getCursor();
        if (c != ourCursor) {
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
        List<TierLabelGlyph> resizeRegion = orderedGlyphs.subList(index, orderedGlyphs.size());

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

        if (this.upperGl != null) {
            if (ourCeiling < nevt.getCoordY() /**
                     * & nevt.getCoordY() < ourFloor *
                     */
                    ) {
//				this.gviewer.getSeqMap().setFloatBounds(NeoWidget.Y,
//					this.gviewer.getSeqMap().getCoordBounds().y,
//					this.gviewer.getSeqMap().getCoordBounds().height + delta);

                double y = this.upperGl.getCoordBox().getY() - delta;
                double inital_height = this.upperGl.getCoordBox().getHeight();
                double height = inital_height + delta;
                this.upperGl.resizeHeight(y, height);
                this.upperGl.getReferenceTier().resizeHeight(-delta, height);
                if (this.upperGl.getReferenceTier() instanceof ITransformableTierGlyph) {
                    if (this.upperGl.getReferenceTier().getDirection() != StyledGlyph.Direction.REVERSE) {
                        ((ITransformableTierGlyph) this.upperGl.getReferenceTier()).setOffset(
                                ((ITransformableTierGlyph) this.upperGl.getReferenceTier()).getOffset() + (int) delta);
                    }
                }

                // Move the fixed height glyphs in the middle,
                // assuming that the list is sorted top to bottom.
                for (TierLabelGlyph g : this.fixedInterior) {
                    g.moveRelative(0, delta);
                    g.getReferenceTier().moveRelative(0, delta);
                }

                this.gviewer.getSeqMap().repackTheTiers(false, false);
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
