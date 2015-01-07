package com.lorainelab.igb.genoviz.extensions.api;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewExtendedI extends SeqMapViewBaseI {

    public TierGlyph getTrack(ITrackStyleExtended style, StyledGlyph.Direction tier_direction);

    public boolean shouldAddCytobandGlyph();
}
