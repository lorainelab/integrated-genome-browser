package com.affymetrix.igb.view.factories;

import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 *
 * @author hiralv
 */
public abstract class AbstractTransformTierGlyph extends AbstractTierGlyph {

    // Variable for transformable tier
    protected LinearTransform tier_transform = new LinearTransform();
    protected Rectangle2D.Double internal_pickRect = new Rectangle2D.Double();
    protected LinearTransform modified_view_transform = new LinearTransform();
    protected final Rectangle2D.Double modified_view_coordbox = new Rectangle2D.Double();
    protected LinearTransform incoming_view_transform;
    protected Rectangle2D.Double incoming_view_coordbox;

    protected abstract void setModifiedViewCoords(ViewI view);

    public AbstractTransformTierGlyph(ITrackStyleExtended style) {
        super(style);
    }

    protected void superDrawChildren(ViewI view) {
        super.drawChildren(view);
    }

    protected void superPickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList,
            ViewI view) {
        super.pickTraversal(pickRect, pickList, view);
    }

    protected List<GlyphI> superPickTraversal(Rectangle2D.Double pickRect, ViewI view) {
        return super.pickTraversal(pickRect, view);
    }

    @Override
    public void drawChildren(ViewI view) {
        // MODIFY VIEW
        incoming_view_transform = view.getTransform();
        incoming_view_coordbox = view.getCoordBox();

        // figure out draw transform by combining tier transform with view transform
        // should allow for arbitrarily deep nesting of transforms too, since cumulative
        //     transform is set to be view transform, and view transform is restored after draw...
        AffineTransform trans2D = new AffineTransform();
        trans2D.translate(0.0, incoming_view_transform.getTranslateY());
        trans2D.scale(1.0, incoming_view_transform.getScaleY());
        trans2D.translate(1.0, tier_transform.getTranslateY());
        trans2D.scale(1.0, tier_transform.getScaleY());

        modified_view_transform = new LinearTransform();
        modified_view_transform.setTransform(
                incoming_view_transform.getScaleX(), 0, 0, trans2D.getScaleY(),
                incoming_view_transform.getTranslateX(), trans2D.getTranslateY());
        view.setTransform(modified_view_transform);

        // need to set view coordbox based on nested transformation
        //   (for methods like withinView(), etc.)
        setModifiedViewCoords(view);
        view.setCoordBox(modified_view_coordbox);

        // CALL NORMAL DRAWCHILDREN(), BUT WITH MODIFIED VIEW
        superDrawChildren(view);

        // RESTORE ORIGINAL VIEW
        view.setTransform(incoming_view_transform);
        view.setCoordBox(incoming_view_coordbox);
    }

    // Need to redo pickTraversal, etc. to take account of transform also...
    @Override
    public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList,
            ViewI view) {

        if (!isVisible() || !intersects(pickRect, view)) {
            return;
        }
        if (hit(pickRect, view)) {
            if (!pickList.contains(this)) {
                pickList.add(this);
            }
        }

        if (getChildren() != null) {
            // modify pickRect on the way in
            //   (transform from view coords to local (tier) coords)
            //    [ an inverse transform? ]
            LinearTransform.inverseTransform(tier_transform, pickRect, internal_pickRect);

            for (GlyphI child : getChildren()) {
                child.pickTraversal(internal_pickRect, pickList, view);
            }
        }
    }

    @Override
    public List<GlyphI> pickTraversal(Rectangle2D.Double pickRect, ViewI view) {
        Rectangle2D.Double coordrect = new Rectangle2D.Double();
        LinearTransform.inverseTransform(tier_transform, pickRect, coordrect);
        return super.pickTraversal(coordrect, view);
    }
}
