package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.tiers.AffyTieredMap;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewI {

	BioSeq getAnnotatedSeq();

	GlyphI getPixelFloaterGlyph();

	AffyTieredMap getSeqMap();

	/**
	 * Returns a forward and reverse tier for the given method, creating them if they don't
	 * already exist.
	 * Generally called by the Glyph Factory.
	 * Note that this can create empty tiers.  But if the tiers are not filled with
	 * something, they will later be removed automatically.
	 * @param meth  The tier annot; it will be treated as case-insensitive.
	 * @param next_to_axis Do you want the Tier as close to the axis as possible?
	 * @param style  a non-null instance of IAnnotStyle; tier label and other properties
	 * are determined by the IAnnotStyle.
	 * @return an array of two (not necessarily distinct) tiers, one forward and one reverse.
	 * The array may instead contain two copies of one mixed-direction tier;
	 * in this case place glyphs for both forward and revers items into it.
	 */
	TierGlyph[] getTiers(boolean next_to_axis, ITrackStyleExtended style, boolean constant_heights);

	/**
	 * Gets the view seq.
	 * Note: {@link #getViewSeq()} and {@link #getAnnotatedSeq()} may return
	 * different BioSeq's !
	 * This allows for reverse complement, coord shifting, seq slicing, etc.
	 * Returns BioSeq that is the SeqMapView's _view_ onto the
	 * BioSeq returned by getAnnotatedSeq()
	 * @see #getTransformPath()
	 */
	BioSeq getViewSeq();

	/**
	 * Get the span of the symmetry that is on the seq being viewed.
	 */
	SeqSpan getViewSeqSpan(SeqSymmetry sym);

	/**
	 * Returns a transformed copy of the given symmetry based on
	 * {@link #getTransformPath()}.  If no transform is necessary, simply
	 * returns the original symmetry.
	 */
	SeqSymmetry transformForViewSeq(SeqSymmetry insym, BioSeq seq_to_compare);
	
	
	/**
	 *  Returns the series of transformations that can be used to map
	 *  a SeqSymmetry from {@link #getAnnotatedSeq()} to
	 *  {@link #getViewSeq()}.
	 */
	SeqSymmetry[] getTransformPath();
	
}
