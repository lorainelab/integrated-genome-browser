package com.affymetrix.igb.osgi.service;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.widget.NeoMap;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewI {

	BioSeq getAnnotatedSeq();

	void addToPixelFloaterGlyph(GlyphI glyph);

	NeoMap getSeqMap();

	void setDataModelFromOriginalSym(GlyphI g, SeqSymmetry sym);

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
