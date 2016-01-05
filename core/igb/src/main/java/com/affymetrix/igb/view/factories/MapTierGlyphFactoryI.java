/**
 * Copyright (c) 2001-2004 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view.factories;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import org.lorainelab.igb.genoviz.extensions.SeqMapViewExtendedI;
import java.util.Set;

/**
 * Factory to create a specific type of ViewModeGlyph.
 */
public interface MapTierGlyphFactoryI {

    public static final int DEFAULT_CHILD_HEIGHT = 25;

    /**
     * initialize the factory
     *
     * @param options - any options appropriate to the factory
     */
    public void init(java.util.Map<String, Object> options);

    /**
     * create a ViewModeGlyph for the SeqSymmetry
     *
     * @param sym - The SeqSymmetry (object model) for the TierGlyph
     * @param style - track style
     * @param smv - reference to the SeqMapView parent of the Tier
     */
    public void createGlyphs(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI smv, BioSeq seq);

    /**
     * Create glyph for given syms
     *
     * @param rootSym
     * @param syms
     * @param style
     * @param smv
     * @param seq
     */
    public void createGlyphs(RootSeqSymmetry rootSym, java.util.List<? extends SeqSymmetry> syms, ITrackStyleExtended style, SeqMapViewExtendedI smv, BioSeq seq);

    /**
     * unique identifier
     *
     * @return name of the factory
     */
    public String getName();

    /**
     * name that will be displayed to the user
     *
     * @return display name
     */
    public String getDisplayName();

    /**
     * if this view mode glyph supports two (forward and reverse) tracks
     *
     * @return supports two track
     */
    public boolean supportsTwoTrack();

    public Set<FileTypeCategory> getSupportedCategories();
}
