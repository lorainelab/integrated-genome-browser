package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.osgi.service.SeqMapViewI;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewExtendedI extends SeqMapViewI {
	boolean autoChangeView();

	int getAverageSlots();
	
	public TierGlyph getTrack(SeqSymmetry sym, ITrackStyleExtended style, TierGlyph.Direction tier_direction, MapViewGlyphFactoryI factory);
}
