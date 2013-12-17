package com.affymetrix.igb.osgi.service;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.genometryImpl.event.PropertyHandler;
import com.affymetrix.genometryImpl.event.SeqMapRefreshed;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FloaterGlyph;
import com.affymetrix.genoviz.widget.NeoMap;
import java.awt.Color;
import java.util.regex.Pattern;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewI {

	public BioSeq getAnnotatedSeq();

	public void addToPixelFloaterGlyph(GlyphI glyph);

	public FloaterGlyph getFloaterGlyph();

	public NeoMap getSeqMap();

//	public void setDataModelFromOriginalSym(GlyphI g, SeqSymmetry sym);

	/**
	 * Gets the view seq.
	 * Note: {@link #getViewSeq()} and {@link #getAnnotatedSeq()} may return
	 * different BioSeq's !
	 * This allows for reverse complement, coord shifting, seq slicing, etc.
	 * Returns BioSeq that is the SeqMapView's _view_ onto the
	 * BioSeq returned by getAnnotatedSeq()
	 * @see #getTransformPath()
	 */
	public BioSeq getViewSeq();

	/**
	 * Check if map should display genome sequence
	 */
	public boolean isGenomeSequenceSupported();
	
	/**
	 * Get the span of the symmetry that is on the seq being viewed.
	 */
	public SeqSpan getViewSeqSpan(SeqSymmetry sym);

	/**
	 * Returns a transformed copy of the given symmetry based on
	 * {@link #getTransformPath()}.  If no transform is necessary, simply
	 * returns the original symmetry.
	 */
	public SeqSymmetry transformForViewSeq(SeqSymmetry insym, BioSeq seq_to_compare);

	/**
	 * Returns a transformed copy of the given symmetry based on
	 * {@link #getTransformPath()} in result.  If no transform is necessary, simply
	 * returns the original symmetry.
	 */
	public SeqSymmetry transformForViewSeq(SeqSymmetry insym, MutableSeqSymmetry result, BioSeq seq_to_compare);
	
	/**
	 *  Returns the series of transformations that can be used to map
	 *  a SeqSymmetry from {@link #getAnnotatedSeq()} to
	 *  {@link #getViewSeq()}.
	 */
	public SeqSymmetry[] getTransformPath();

	/**
	 *  return a SeqSpan representing the visible bounds of the view seq
	 */
	public SeqSpan getVisibleSpan();
	/**
	 * @param start start of region
	 * @param end end of region
	 * @param seq seq for region
	 */
	public void setRegion(int start, int end, BioSeq seq);
	/**
	 *   Sets the sequence.  If null, has the same effect as calling clear().
	 *   @param preserve_selection  if true, then try and keep same selections
	 *   @param preserve_view  if true, then try and keep same scroll and zoom / scale and offset in
	 *       // both x and y direction.
	 *       [GAH: temporarily changed to preserve scale in only the x direction]
	 */
	public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view);
	public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view_x, boolean preserve_view_y);
	public void zoomTo(SeqSpan span);
	/**
	 * Center at the hairline.
	 */
	public void centerAtHairline();
	public void addToRefreshList(SeqMapRefreshed smr);
	/**
	 *  Figures out which symmetries are currently selected and then calls
	 *  {@link GenometryModel#setSelectedSymmetries(List, List, Object)}.
	 */
	public void postSelections();
	public void setPropertyHandler(PropertyHandler propertyHandler);
	public Object getMouseListener();
	public void addPopupListener(ContextualPopupListener listener);
	public void removePopupListener(ContextualPopupListener listener);
	public void selectAllGraphs();
	public void select(List<SeqSymmetry> sym_list, boolean call_listener);
	public void repackTheTiers(boolean full_repack, boolean stretch_vertically);
	public void setZoomSpotX(double x);
	public void setZoomSpotY(double y);

	/**
	 * Update the widget in this panel.
	 */
	public void updatePanel();
	/**
	 * Update the widget in this panel.
	 */
	public void updatePanel(boolean preserveViewX, boolean preserveViewY);
	
	/**
	 * Returns the list of glyphs on the axis tier
	 * @param forward - true = forward search, false = reverse search
	 * @param regex - the regular expression to match
	 * @param residues the residues to search
	 * @param residue_offset the starting offset within the residues
	 * @param hitColor the color to mark them with
	 * @return 
	 */
	public List<GlyphI> searchForRegexInResidues(boolean forward, Pattern regex, 
			String residues, int residue_offset, Color hitColor);
	
	public List<? extends GlyphI> getSelectedFloatingGraphGlyphs();

	public List<SeqSymmetry> getSelectedSyms();

	public SeqSymmetry getSeqSymmetry();
	
	public <G extends GlyphI> G getItemFromTier(Object datamodel);
	
	public void split(GlyphI glyph);
	
	public void setBackGroundProvider(ViewI.BackGroundProvider bgp, ViewI.BackGroundProvider labelbgp);
}
