package org.lorainelab.igb.genoviz.extensions;

import com.affymetrix.genometry.style.ITrackStyleExtended;
import org.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewExtendedI extends SeqMapViewI {

    public TierGlyph getTrack(ITrackStyleExtended style, StyledGlyph.Direction tier_direction);

    public boolean shouldAddCytobandGlyph();
}
