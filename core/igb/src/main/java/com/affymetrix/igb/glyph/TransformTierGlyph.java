package com.affymetrix.igb.view.factories;

import com.affymetrix.igb.glyph.AbstractTransformTierGlyph;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TransformTierGlyph. only use transform for operations on children.
 * coordinates of the tier itself are maintained in coordinate system of the
 * incoming view...
 *
 * currently assuming no modifications to tier_transform, etc. are made between
 * call to modifyView(view) and call to restoreView(view);
 *
 * Note that if the tier has any "middleground" glyphs, these are _not_
 * considered children, so transform does not apply to them
 *
 */
public class TransformTierGlyph extends AbstractTransformTierGlyph {

    private int fixedPixHeight = 1;
    private static final Map<String, Class<?>> PREFERENCES;

    static {
        Map<String, Class<?>> temp = new HashMap<>();
        PREFERENCES = Collections.unmodifiableMap(temp);
    }

    public TransformTierGlyph(ITrackStyleExtended style) {
        super(style);
    }

    protected void superMoveRelative(double diffx, double diffy) {
        super.moveRelative(diffx, diffy);
    }

    @Override
    public void pack(ViewI view) {
        super.pack(view);
        fixPixHeight();
    }

    protected void fixPixHeight() {
        // trying to transform according to tier's internal transform
        //   (since packing is done based on tier's children)
        if (this.getPacker() != null) {
            Rectangle2D.Double newbox = new Rectangle2D.Double();
            newbox.setRect(getCoordBox());
            LinearTransform.transform(tier_transform, newbox, newbox);
            setCoords(newbox.x, newbox.y, newbox.width, newbox.height);
        }
    }

    @Override
    protected void setModifiedViewCoords(ViewI view) {
        // This works fine too. But for now not modifying it. HV 05/19/12
        // view.transformToCoords(this.getPixelBox(), modified_view_coordbox);
        view.transformToCoords(view.getPixelBox(), modified_view_coordbox);
    }

    public void fitToPixelHeight(ViewI view) {
        // use view transform to determine how much "more" scaling must be
        //       done within tier to keep its
        LinearTransform view_transform = view.getTransform();
        double yscale = 0.0d;
        if (0.0d != getCoordBox().height) {
            yscale = fixedPixHeight / getCoordBox().height;
        }
        if (0.0d != view_transform.getScaleY()) {
            yscale /= view_transform.getScaleY();
        }
        tier_transform.setTransform(tier_transform.getScaleX(), 0, 0, tier_transform.getScaleY() * yscale, tier_transform.getTranslateX(), tier_transform.getTranslateY());
        getCoordBox().height *= yscale;
    }

    // Don't move children! Just change tier's transform offset.
    @Override
    public void moveRelative(double diffx, double diffy) {
        getCoordBox().x += diffx;
        getCoordBox().y += diffy;
        tier_transform.setTransform(tier_transform.getScaleX(), 0, 0,
                tier_transform.getScaleY(), tier_transform.getTranslateX(),
                getCoordBox().y);
    }

    public final void setFixedPixHeight(int pix_height) {
        fixedPixHeight = pix_height;
    }

    public final int getFixedPixHeight() {
        return fixedPixHeight;
    }

    @Override
    public Map<String, Class<?>> getPreferences() {
        return new HashMap<>(PREFERENCES);
    }

    @Override
    public void setPreferences(Map<String, Object> preferences) {
    }

    @Override
    public boolean isManuallyResizable() {
        return false;
    }

}
