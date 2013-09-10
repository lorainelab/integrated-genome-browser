package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class SearchResults {
	
	private String searchType, searchTerm, searchFilter, searchSummary;
	private List<SeqSymmetry> searchResults;
	
	public SearchResults(String searchType, String searchTerm, String searchFilter, 
						 String searchSummary, List<SeqSymmetry> searchResults){
		this.searchType = searchType;
		this.searchTerm = searchTerm;
		this.searchFilter = searchFilter;
		this.searchSummary = searchSummary;
		this.searchResults = searchResults;
	}
	
	/**
	 * Returns the type of search that was used
	 * @return
	 */
	public String getSearchType(){
		return searchType;
	}
	
	/**
	 * Returns search term
	 * @return 
	 */
	public String getSearchTerm(){
		return searchTerm;
	}
	
	/**
	 * Returns search message
	 * @return 
	 */
	public String getSearchSummary(){
		return searchSummary;
	}
	
	/**
	 * Returns search filter
	 * @return 
	 */
	public String getSearchFilter(){
		return searchFilter;
	}
	
	/**
	 * Returns search result
	 * @return 
	 */
	public List<SeqSymmetry> getResults(){
		return searchResults;
	}
}
