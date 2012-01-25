package com.affymetrix.igb.searchmodegeneric;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchMode;
import com.affymetrix.igb.shared.SearchResultsTableModel;

public abstract class SearchModeGeneric implements ISearchMode {
	private static final int MAX_HITS = 100000;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("searchmodegeneric");
	protected static final String FRIENDLY_PATTERN = BUNDLE.getString("friendlyPattern");
	protected IGBService igbService;

	protected SearchModeGeneric(IGBService igbService) {
		super();
		this.igbService = igbService;
	}

	protected List<SeqSymmetry> getAltSymList() {
		return null;
	}

	@Override
	public void valueChanged(SearchResultsTableModel model, int srow) {
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		SeqSymmetry sym = ((SymSearchResultsTableModel)model).get(srow);

		if (sym != null) {
			List<SeqSymmetry> altSymList = getAltSymList();
			if (altSymList != null && altSymList.contains(sym)) {
				if (group == null) {
					return;
				}
				zoomToCoord(sym);
				return;
			}

			if (igbService.getSeqMap().getItem(sym) == null) {
				if (group == null) {
					return;
				}
				// Couldn't find sym in map view! Go ahead and zoom to it.
				zoomToCoord(sym);
				return;
			}

			// Set selected symmetry normally
			List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(1);
			syms.add(sym);
			igbService.getSeqMapView().select(syms, true);
		}
	}

	private void zoomToCoord(SeqSymmetry sym) throws NumberFormatException {
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		String seqID = sym.getSpanSeq(0).getID();
		BioSeq seq = group.getSeq(seqID);
		if (seq != null) {
			SeqSpan span = sym.getSpan(0);
			if (span != null) {
				// zoom to its coordinates
				igbService.zoomToCoord(seqID, span.getStart(), span.getEnd());
			}
		}
	}

	protected static List<SeqSymmetry> filterBySeq(List<SeqSymmetry> results, BioSeq seq) {

		if (results == null || results.isEmpty()) {
			return new ArrayList<SeqSymmetry>();
		}

		int num_rows = results.size();

		List<SeqSymmetry> rows = new ArrayList<SeqSymmetry>(num_rows / 10);
		for (int j = 0; j < num_rows && rows.size() < MAX_HITS; j++) {
			SeqSymmetry result = results.get(j);

			SeqSpan span = null;
			if (seq != null) {
				span = result.getSpan(seq);
				if (span == null) {
					// Special case when chromosomes are not equal, but have same ID (i.e., really they're equal)
					SeqSpan tempSpan = result.getSpan(0);
					if (tempSpan != null && tempSpan.getBioSeq() != null && seq.getID().equals(tempSpan.getBioSeq().getID())) {
						span = tempSpan;
					}
				}
			} else {
				span = result.getSpan(0);
			}
			if (span == null) {
				continue;
			}

			rows.add(result);
		}

		return rows;
	}	

	protected List<SeqSpan> findSpans(List<SeqSymmetry> syms) {
		List<SeqSpan> spans = new ArrayList<SeqSpan>();
		for (SeqSymmetry sym : syms) {
			for (int i = 0; i < sym.getSpanCount(); i++) {
				spans.add(sym.getSpan(i));
			}
		}
		return spans;
	}

	@Override
	public void finished(BioSeq vseq) {	}
}
