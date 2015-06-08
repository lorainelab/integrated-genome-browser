package com.lorainelab.igb.genoviz.extensions;

import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewExtendedI extends SeqMapViewI {

    public TierGlyph getTrack(ITrackStyleExtended style, StyledGlyph.Direction tier_direction);

    public boolean shouldAddCytobandGlyph();
}
