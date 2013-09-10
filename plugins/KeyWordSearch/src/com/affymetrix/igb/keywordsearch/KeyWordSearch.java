package com.affymetrix.igb.keywordsearch;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.igb.shared.IKeyWordSearch;
import com.affymetrix.igb.shared.IStatus;
import com.affymetrix.igb.shared.SearchResults;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 *
 * @author hiralv
 */
public class KeyWordSearch implements IKeyWordSearch{
	private static final int SEARCH_ALL_ORDINAL = -8000;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("keywordsearch");
	final private List<IKeyWordSearch> searchModes;
	
	public KeyWordSearch(){
		searchModes = new ArrayList<IKeyWordSearch>();
	}
	
	public String getName(){
		return BUNDLE.getString("searchKeyWord");
	}
	
	public int searchAllUse(){
		return SEARCH_ALL_ORDINAL;
	}
	
	public String getTooltip(){
		return BUNDLE.getString("searchTooltip");
	}
	
	public boolean useGenomeInSeqList(){
		return true;
	}
	
	public String checkInput(String search_text, BioSeq vseq, String seq){
		return null;
	}
	
	public SearchResults search(String search_text, final BioSeq chrFilter, IStatus statusHolder, boolean option){
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		StringBuilder status = new StringBuilder();
		StatusHolder sh = new StatusHolder(statusHolder);
		for(IKeyWordSearch searchMode : searchModes){
			statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchSearching"), searchMode.getName(), search_text));
			SearchResults searchResults = searchMode.search(search_text, chrFilter, sh, option);
			List<SeqSymmetry> res = searchResults != null ? searchResults.getResults() : null;
			if(res != null && !res.isEmpty()){
				results.addAll(res);
			}
			status.append(searchMode.getName()).append(" :").append(sh.getLastStatus()).append(", ");
		}
		statusHolder.setStatus(status.toString());
		return new SearchResults(null, search_text, chrFilter != null ? chrFilter.getID() : "genome", status.toString(), results);
	}
	
	public List<SeqSymmetry> searchTrack(String search_text, TypeContainerAnnot contSym){
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		for(IKeyWordSearch searchMode : searchModes){
			List<SeqSymmetry> res = searchMode.searchTrack(search_text, contSym);
			if(res != null && !res.isEmpty()){
				results.addAll(res);
			}
		}
		return results;
	}
	
	public synchronized void initSearchModes() {
		searchModes.clear();
		searchModes.addAll(ExtensionPointHandler.getExtensionPoint(IKeyWordSearch.class).getExtensionPointImpls());
	}
	
	private static class StatusHolder implements IStatus {
		private String lastStatus;
		IStatus internalSH;

		public StatusHolder(IStatus internalSH) {
			this.internalSH = internalSH;
		}

		public void setStatus(String text) {
			internalSH.setStatus(text);
			lastStatus = text;
		}

		String getLastStatus() {
			return lastStatus;
		}
	}
}
