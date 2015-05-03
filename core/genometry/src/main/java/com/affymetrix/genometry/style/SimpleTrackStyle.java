/**
 * Copyright (c) 2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.style;

import com.affymetrix.genometry.color.ColorProviderI;
import com.affymetrix.genometry.filter.SymmetryFilterI;
import com.affymetrix.genometry.general.DataSet;

import java.awt.Color;

public class SimpleTrackStyle extends DefaultTrackStyle implements ITrackStyleExtended {

    /**
     * for height on the reverse strand. To help with track resizing.
     */
    private double reverseHeight;
    private float track_name_size = 12;
    private Color label_foreGround = null;
    private Color label_backGround = null;
    private int directionType;
    private boolean showAsPaired = false;
    private Color forwardColor, reverseColor;

    @Override
    public void setReverseHeight(double theNewHeight) {
        this.reverseHeight = theNewHeight;
    }

    @Override
    public double getReverseHeight() {
        return this.reverseHeight;
    }

    @Override
    public void setForwardHeight(double theNewHeight) {
        super.setHeight(theNewHeight);
    }

    @Override
    public double getForwardHeight() {
        return super.getHeight();
    }

    /**
     * for maximum depth of stacked glyphs on the reverse strand. To help with
     * resizing.
     */
    private int reverseMaxDepth = 0;

    @Override
    public void setReverseMaxDepth(int theNewDepth) {
        this.reverseMaxDepth = theNewDepth;
    }

    @Override
    public int getReverseMaxDepth() {
        return this.reverseMaxDepth;
    }

    @Override
    public void setForwardMaxDepth(int theNewDepth) {
        int rd = this.getMaxDepth();
        this.setMaxDepth(theNewDepth);
        this.reverseMaxDepth = rd;
    }

    @Override
    public int getForwardMaxDepth() {
        return this.getMaxDepth();
    }

    @Override
    public void setHeight(double theNewHeight) {
        super.setHeight(theNewHeight);
        this.reverseHeight = super.getHeight();
    }

    // Should be called only from within package or from StateProvider.
    public SimpleTrackStyle(String name, boolean is_graph) {
        super(name, is_graph);
        this.reverseHeight = super.getHeight();
    }

    String url;

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getUrl() {
        return url;
    }

    int depth = 2;

    @Override
    public void setGlyphDepth(int i) {
        this.depth = i;
    }

    @Override
    public int getGlyphDepth() {
        return this.depth;
    }

    boolean separate = true;

    @Override
    public void setSeparate(boolean b) {
        this.separate = b;
    }

    @Override
    public boolean getSeparate() {
        return this.separate;
    }

    boolean separable = true;

    @Override
    public void setSeparable(boolean b) {
        this.separable = b;
    }

    @Override
    public boolean getSeparable() {
        return this.separable;
    }

    String labelField = "id";

    @Override
    public void setLabelField(String s) {
        this.labelField = s;
    }

    @Override
    public String getLabelField() {
        return labelField;
    }

    @Override
    public void copyPropertiesFrom(ITrackStyle g) {
        super.copyPropertiesFrom(g);
        if (g instanceof ITrackStyleExtended) {
            ITrackStyleExtended as = (ITrackStyleExtended) g;
            setUrl(as.getUrl());
            setColorProvider(as.getColorProvider());
            setGlyphDepth(as.getGlyphDepth());
            setSeparate(as.getSeparate());
            setLabelField(as.getLabelField());
            setSummaryThreshold(as.getSummaryThreshold());
        }
    }

    public boolean drawCollapseControl() {
        return getExpandable();
    }

    @Override
    public DataSet getFeature() {
        return null;
    }

    @Override
    public void setFeature(DataSet f) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getDirectionType() {
        return directionType;
    }

    @Override
    public void setDirectionType(int ordinal) {
        directionType = ordinal;
    }

    @Override
    public void setForwardColor(Color c) {
        forwardColor = c;
    }

    @Override
    public Color getForwardColor() {
        return forwardColor;
    }

    @Override
    public void setReverseColor(Color c) {
        reverseColor = c;
    }

    @Override
    public Color getReverseColor() {
        return reverseColor;
    }

    @Override
    public final boolean isFloatTier() {
        return false;
    }

    @Override
    public int getSummaryThreshold() {
        return 0;
    }

    @Override
    public final void setFloatTier(boolean b) {

    }

    @Override
    public void setTrackNameSize(float font_size) {
        track_name_size = font_size;
    }

    @Override
    public float getTrackNameSize() {
        return track_name_size;
    }

    @Override
    public Color getLabelForeground() {
        if (label_foreGround == null) {
            return getForeground();
        }
        return label_foreGround;
    }

    @Override
    public Color getLabelBackground() {
        if (label_backGround == null) {
            return getBackground();
        }
        return label_backGround;
    }

    @Override
    public void setLabelForeground(Color c) {
        label_foreGround = c;
    }

    @Override
    public void setLabelBackground(Color c) {
        label_backGround = c;
    }

    @Override
    public void setSummaryThreshold(int level) {

    }

    boolean join;

    @Override
    public boolean getJoin() {
        return join;
    }

    @Override
    public void setJoin(boolean b) {
        join = b;
    }

    ColorProviderI color_provider;

    @Override
    public void setColorProvider(ColorProviderI cp) {
        this.color_provider = cp;
    }

    @Override
    public ColorProviderI getColorProvider() {
        return color_provider;
    }

    private SymmetryFilterI filter;

    @Override
    public void setFilter(SymmetryFilterI filter) {
        this.filter = filter;
    }

    @Override
    public SymmetryFilterI getFilter() {
        return filter;
    }

    private boolean showResidueMask;

    @Override
    public boolean getShowResidueMask() {
        return showResidueMask;
    }

    @Override
    public void setShowResidueMask(boolean showResidueMask) {
        this.showResidueMask = showResidueMask;
    }

    private boolean shadeBasedOnQualityScore;

    @Override
    public boolean getShadeBasedOnQualityScore() {
        return shadeBasedOnQualityScore;
    }

    @Override
    public void setShadeBasedOnQualityScore(boolean shadeBasedOnQualityScore) {
        this.shadeBasedOnQualityScore = shadeBasedOnQualityScore;
    }

    @Override
    public boolean isShowAsPaired() {
        return showAsPaired;
    }

    @Override
    public void setShowAsPaired(boolean showAsPaired) {
        this.showAsPaired = showAsPaired;
    }

}
