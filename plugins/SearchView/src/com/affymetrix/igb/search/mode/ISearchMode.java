package com.affymetrix.igb.search.mode;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.search.IStatus;

public interface ISearchMode {
	public String getName();
	public String getTooltip();
	public boolean useRemote();
	public boolean useDisplaySelected();
	public boolean useGenomeInSeqList();
	public boolean checkInput(String search_text, BioSeq vseq, String seq);
	public SearchResultsTableModel getEmptyTableModel();
	public SearchResultsTableModel run(String search_text, BioSeq chrFilter, String seq, boolean remote, IStatus statusHolder, List<GlyphI> glyphs);
	public void finished(BioSeq vseq);
	public void valueChanged(SearchResultsTableModel model, int srow, List<GlyphI> glyphs);
}
