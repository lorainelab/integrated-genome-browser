package com.affymetrix.searchmodesymmetryfilter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.filter.SymmetryFilter;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.IStatus;
import com.affymetrix.igb.shared.SearchResults;

public class SearchModeSymmetryFilter implements ISearchModeSym {
	private final int searchAllOrdinal;
	private final IGBService igbService;
	private final SymmetryFilterI filter;
	private boolean optionSelected;
	
	public SearchModeSymmetryFilter(IGBService igbService, SymmetryFilterI filter, int searchAllOrdinal) {
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
	public boolean useGenomeInSeqList() {
		return false;
	}

	@Override
	public String checkInput(String search_text, BioSeq vseq, String seq) {
		if(filter instanceof SymmetryFilter) {
			SymmetryFilter absFilter = (SymmetryFilter)filter;
			return absFilter.setParameterValue(absFilter.getParametersType().entrySet().iterator().next().getKey(), search_text) ? 
					null : "Error setting param " + search_text;
		}
		return "Current filter does not accept any parameters";
	}

	@Override
	public SearchResults search(String search_text, BioSeq chrFilter, IStatus statusHolder, boolean option) {
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		if (filter instanceof SymmetryFilter && 
				!search_text.equals(((SymmetryFilter)filter).getParameterValue(((SymmetryFilter)filter).getParametersType().entrySet().iterator().next().getKey()))) {
			throw new IllegalStateException("filter value changed from " + 
					((SymmetryFilter)filter).getParameterValue(((SymmetryFilter)filter).getParametersType().entrySet().iterator().next().getKey()) + " to " + search_text);
		}
		List<Glyph> glyphs = igbService.getAllTierGlyphs();
		for (Glyph selectedTierGlyph : glyphs) {
			Object info = selectedTierGlyph.getInfo();
			if (info instanceof TypeContainerAnnot) {
				List<SeqSymmetry> searchResults = searchTrack(search_text, (TypeContainerAnnot)info);
				if (searchResults != null) {
					results.addAll(searchResults);
				}
			}
		}
		String statusStr = MessageFormat.format("Searching {0} - found {1} matches", search_text, "" + results.size());
		statusHolder.setStatus(statusStr);
		return new SearchResults(getName(), search_text, chrFilter != null ? chrFilter.getID() : "genome", statusStr, results);
	}

	private List<SeqSymmetry> searchSym(SeqSymmetry sym) {
		List<SeqSymmetry> searchResults = new ArrayList<SeqSymmetry>();
		if (filter.filterSymmetry(null, sym)) {
			searchResults.add(sym);
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
	public List<SeqSymmetry> searchTrack(String search_text, TypeContainerAnnot trackSym) {
		if (filter instanceof SymmetryFilter && 
				!search_text.equals(((SymmetryFilter)filter).getParameterValue(((SymmetryFilter)filter).getParametersType().entrySet().iterator().next().getKey()))) {
			throw new IllegalStateException("filter value changed from " + 
					((SymmetryFilter)filter).getParameterValue(((SymmetryFilter)filter).getParametersType().entrySet().iterator().next().getKey()) + " to " + search_text);
		}
		List<SeqSymmetry> results = searchSym(trackSym);
		return results;
	}
}
