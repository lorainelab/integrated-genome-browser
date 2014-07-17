package com.affymetrix.igb.view.factories;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * copy / modification of TierGlyph for ViewModeGlyph for sequences
 */
public class SequenceTierGlyph extends AbstractTierGlyph {

    // extending solid glyph to inherit hit methods (though end up setting as not hitable by default...)

    private static final Map<String, Class<?>> PREFERENCES;

    static {
        Map<String, Class<?>> temp = new HashMap<String, Class<?>>();
        PREFERENCES = Collections.unmodifiableMap(temp);
    }

    public SequenceTierGlyph(ITrackStyleExtended style) {
        super(style);
        style.setSeparable(false);
        style.setSeparate(false);
    }

    // overriding pack to ensure that tier is always the full width of the scene
    @Override
    public void pack(ViewI view) {
        super.pack(view);
        Rectangle2D.Double mbox = getScene().getCoordBox();
        Rectangle2D.Double cbox = this.getCoordBox();

        this.setCoords(mbox.x, cbox.y, mbox.width, cbox.height);
    }

    @Override
    public void setPreferredHeight(double height, ViewI view) {
        height -= 2 * getSpacing();

        if (useLabel(style)) {
            height /= 2;
        }

        double percent = ((height * 100) / style.getHeight() - 100) / 100;
        style.setHeight(height);

        scaleChildHeights(percent, getChildren(), view);
    }

    @Override
    protected boolean shouldDrawToolBar() {
        return false;
    }

    /**
     * Not implemented. Will behave the same as drawSelectedOutline(ViewI).
     */
    @Override
    protected void drawSelectedFill(ViewI view) {
        this.drawSelectedOutline(view);
    }

    /**
     * Not implemented. Will behave the same as drawSelectedOutline(ViewI).
     */
    @Override
    protected void drawSelectedReverse(ViewI view) {
        this.drawSelectedOutline(view);
    }

    @Override
    public Map<String, Class<?>> getPreferences() {
        return new HashMap<String, Class<?>>(PREFERENCES);
    }

    @Override
    public void setPreferences(Map<String, Object> preferences) {
    }
}
