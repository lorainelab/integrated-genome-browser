package com.affymetrix.igb.searchmodegeneric;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.IStatus;

public abstract class SearchModeGeneric implements ISearchModeSym {
	private static final int MAX_HITS = 100000;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("searchmodegeneric");
	protected static final String FRIENDLY_PATTERN = BUNDLE.getString("friendlyPattern");
	protected IGBService igbService;

	protected SearchModeGeneric(IGBService igbService) {
		super();
		this.igbService = igbService;
	}

	@Override
	public List<SeqSymmetry> getAltSymList() {
		return null;
	}

	@Override
	public List<SeqSymmetry> searchTrack(String search_text, final BioSeq chrFilter, TypeContainerAnnot contSym, IStatus statusHolder, boolean option) {
		return null;
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
}
