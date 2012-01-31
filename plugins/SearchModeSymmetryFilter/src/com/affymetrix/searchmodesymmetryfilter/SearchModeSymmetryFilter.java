package com.affymetrix.searchmodesymmetryfilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.filter.SearchResult;
import com.affymetrix.genometryImpl.filter.SymmetryFilterSearchI;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.IStatus;

public class SearchModeSymmetryFilter implements ISearchModeSym {
	private final int searchAllOrdinal;
	private final IGBService igbService;
	private final SymmetryFilterSearchI filter;
	
	public SearchModeSymmetryFilter(IGBService igbService, SymmetryFilterSearchI filter, int searchAllOrdinal) {
		super();
		this.igbService = igbService;
		this.filter = filter;
		this.searchAllOrdinal = searchAllOrdinal;
	}

	@Override
	public String getName() {
		return "Filter " + filter.getName();
	}

	@Override
	public int searchAllUse() {
		return searchAllOrdinal;
	}

	@Override
	public String getTooltip() {
		return getName();
	}

	@Override
	public String getOptionName() {
		return null;
	}

	@Override
	public String getOptionTooltip() {
		return null;
	}

	@Override
	public boolean getOptionEnable() {
		return false;
	}

	@Override
	public boolean useOption() {
		return false;
	}

	@Override
	public void clear() {}

	@Override
	public boolean useGenomeInSeqList() {
		return false;
	}

	@Override
	public String checkInput(String search_text, BioSeq vseq, String seq) {
		return filter.setParam(search_text) ? null : "Error setting param " + search_text;
	}

	@Override
	public void finished(BioSeq vseq) {
	}

	@Override
	public List<SeqSymmetry> search(String search_text, BioSeq chrFilter,
			IStatus statusHolder, boolean option) {
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		if (!search_text.equals(filter.getParam())) {
			throw new IllegalStateException("filter value changed from " + filter.getParam() + " to " + search_text);
		}
		List<Glyph> glyphs = igbService.getAllTierGlyphs();
		for (Glyph selectedTierGlyph : glyphs) {
			Object info = selectedTierGlyph.getInfo();
			if (info instanceof TypeContainerAnnot) {
				List<SearchResult> searchResults = searchTrack(search_text, chrFilter,
					(TypeContainerAnnot)info, statusHolder, option);
				if (searchResults != null) {
					for (SearchResult searchResult : searchResults) {
						results.add(searchResult.getSym());
					}
				}
			}
		}
		return results;
	}

	private List<SearchResult> searchSym(SeqSymmetry sym) {
		List<SearchResult> searchResults = new ArrayList<SearchResult>();
		SearchResult searchResult = filter.searchSymmetry(sym);
		if (searchResult != null) {
			searchResults.add(searchResult);
		}
		int childCount = sym.getChildCount();
		for (int i = 0; i < childCount; i++) {
			//if(current_thread.isInterrupted())
			//	break;
			
			searchResults.addAll(searchSym(sym.getChild(i)));
		}
		return searchResults;
	}
	
	@Override
	public List<SearchResult> searchTrack(String search_text, BioSeq chrFilter,
			TypeContainerAnnot trackSym, IStatus statusHolder, boolean option) {
		if (!search_text.equals(filter.getParam())) {
			throw new IllegalStateException("filter value changed from " + filter.getParam() + " to " + search_text);
		}
		List<SearchResult> results = searchSym(trackSym);
		Collections.sort(results);
		return results;
	}

	@Override
	public void valueChanged(SeqSymmetry sym) {
	}
}
