package com.affymetrix.igb.searchmodeidorprops;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.igb.shared.ISearchHints;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author hiralv
 */
public class SearchHints implements ISearchHints {

	public Set<String> search(String search_term) {
		String regexText = search_term;
		if (!(regexText.contains("*") || regexText.contains("^") || regexText.contains("$"))) {
			// Not much of a regular expression.  Assume the user wants to match at the start and end
			regexText = ".*" + regexText + ".*";
		}
		Pattern regex = Pattern.compile(regexText, Pattern.CASE_INSENSITIVE);

		return GenometryModel.getGenometryModel().getSelectedSeqGroup().find(regex, 20);
	}
}
