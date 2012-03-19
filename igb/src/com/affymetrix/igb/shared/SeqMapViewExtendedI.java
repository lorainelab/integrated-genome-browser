package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.osgi.service.SeqMapViewI;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewExtendedI extends SeqMapViewI {
	/**
	 * Returns a forward and reverse tier for the given method, creating them if they don't
	 * already exist.
	 * Generally called by the Glyph Factory.
	 * Note that this can create empty tiers.  But if the tiers are not filled with
	 * something, they will later be removed automatically.
	 * @param meth  The tier annot; it will be treated as case-insensitive.
	 * @param style  a non-null instance of IAnnotStyle; tier label and other properties
	 * are determined by the IAnnotStyle.
	 * @param factory the factory to use initially
	 * @return an array of two (not necessarily distinct) tiers, one forward and one reverse.
	 * The array may instead contain two copies of one mixed-direction tier;
	 * in this case place glyphs for both forward and revers items into it.
	 */
	TierGlyph getTrack(SeqSymmetry sym, ITrackStyleExtended style, TierGlyph.Direction tier_direction, MapViewGlyphFactoryI factory);

	boolean autoChangeView();
	
	int getAverageSlots();
}
