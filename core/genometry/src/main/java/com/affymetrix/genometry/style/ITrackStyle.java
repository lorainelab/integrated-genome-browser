/**
 * Copyright (c) 2006-2007 Affymetrix, Inc.
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
package com.affymetrix.genometry.style;

import java.awt.Color;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Track style information. This interface can be used regardless of
 * whether the tier contains annotation data or graph data.
 */
public interface ITrackStyle {

    public Color getForeground();

    public void setForeground(Color c);

    public boolean getShow();

    public void setShow(boolean b);

    public String getUniqueName();

    public String getTrackName();

    public void setTrackName(String s);

    public String getMethodName();

    public Color getBackground();

    public void setBackground(Color c);

    public boolean getCollapsed();

    public void setCollapsed(boolean b);

    /**
     * @return maximum rows of annotations to stack in the tier.
     */
    public int getMaxDepth();

    /**
     * How high annotations can be stacked in the tier.
     * The same number applies to both the forward and reverse strands.
     * The rest should overlay each other.
     */
    public void setMaxDepth(int m);

    public void setHeight(double h);

    public double getHeight();

    public void setY(double y);

    public double getY();

    /**
     * Indicates whether or not {@link #setCollapsed(boolean)} is allowed.
     * In some styles collapse and expand have no meaning.
     * So <code>getCollapsed()</code> and {@link #getMaxDepth()}
     * have no meaning for those styles.
     *
     * @return true iff the tier can be collapsed and expanded.
     */
    public boolean getExpandable();

    /**
     * Sets whether or not the tier can be collapsed and expanded.
     */
    public void setExpandable(boolean b);

    /**
     * Indicates whether this track will be used for a graph.
     */
    public boolean isGraphTier();

    public void setGraphTier(boolean b);

    /**
     * Gets a reference to a Map that can be used to store any arbitrary
     * extra properties. This can be used to
     * store all the properties of a UCSC track-line, for example.
     * (These properties are not persisted in the java prefs system.)
     */
    public Map<String, Object> getTransientPropertyMap();

    public void copyPropertiesFrom(ITrackStyle s);

    /*
     * Tracks operations support creating new track from existing, and in such
     * cases, their preferences should also be copied. This will allow
     * components like "color by" to store preferences below those of track and
     * hence allow them to be copied when track is getting copied
     *
     * @param propertyName
     * @return
     */
    public Optional<Preferences> getPreferenceChildForProperty(String propertyName);
}
