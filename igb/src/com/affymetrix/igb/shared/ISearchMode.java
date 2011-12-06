package com.affymetrix.igb.shared;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;

public interface ISearchMode {
	public static final int NO_ZOOM_SPOT = -1;
	public String getName();
	public String getTooltip();
	public boolean useRemote();
	public boolean useDisplaySelected();
	public boolean useGenomeInSeqList();
	public boolean checkInput(String search_text, BioSeq vseq, String seq);
	public SearchResultsTableModel getEmptyTableModel();
	public List<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder);
	public SearchResultsTableModel run(String search_text, BioSeq chrFilter, String seq, boolean remote, IStatus statusHolder, List<GlyphI> glyphs);
	public void finished(BioSeq vseq);
	public void valueChanged(SearchResultsTableModel model, int srow, List<GlyphI> glyphs);
	public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan);
	public int getZoomSpot(String search_text);
}
