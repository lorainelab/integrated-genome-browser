package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.osgi.service.SeqMapViewI;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewExtendedI extends SeqMapViewI {
	public TierGlyph getTrack(ITrackStyleExtended style, TierGlyph.Direction tier_direction);
	public boolean shouldAddCytobandGlyph();
}
