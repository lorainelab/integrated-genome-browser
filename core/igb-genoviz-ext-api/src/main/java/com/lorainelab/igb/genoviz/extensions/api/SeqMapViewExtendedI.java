package com.lorainelab.igb.genoviz.extensions.api;

import com.affymetrix.genometry.style.ITrackStyleExtended;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewExtendedI extends SeqMapViewI {

    public TierGlyph getTrack(ITrackStyleExtended style, StyledGlyph.Direction tier_direction);

    public boolean shouldAddCytobandGlyph();
}
