package com.affymetrix.genometryImpl.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;

public class SymmetryFilterSearchProps implements SymmetryFilterSearchI {
	private static final String ID_SEARCH_TERM = "id";
	private static final int ID_EXACT_RANK = 1000000;
	private static final int ID_REGEX_RANK = 10000;
	private static final int PROPERTY_EXACT_RANK = 100;
	private static final int PROPERTY_REGEX_RANK = 1;
	private Object param;
	private Pattern regex;
	private SymWithProps swp;
	private String match;

	@Override
	public String getName() {
		return "properties";
	}

	@Override
	public Object getParam() {
		return param;
	}

	@Override
	public boolean setParam(Object param) {
		this.param = param;
		if (param.getClass() != String.class) {
			return false;
		}
		regex = getRegex((String)param);
		return regex != null;
	}

	@Override
	public boolean filterSymmetry(SeqSymmetry sym) {
		return searchSymmetry(sym) != null;
	}

	private Pattern getRegex(String search_text)  {
		if (search_text == null) {
			search_text = "";
		}
		String regexText = search_text;
		// Make sure this search is reasonable to do on a remote server.
		if (!(regexText.contains("*") || regexText.contains("^") || regexText.contains("$"))) {
			// Not much of a regular expression.  Assume the user wants to match at the start and end
			regexText = ".*" + regexText + ".*";
		}
		Pattern regex = null;
		try {
			regex = Pattern.compile(regexText, Pattern.CASE_INSENSITIVE);
		}
		catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "error with regular expression " + search_text, e);
			regex = null;
		}
		return regex;
	}

	@Override
	public SearchResult searchSymmetry(SeqSymmetry sym) {
		Matcher matcher = regex.matcher("");
		Map<String,String> searchTerms = new HashMap<String,String>();
		int ranking = 0;
		match = sym.getID();
		if (match != null) {
			matcher.reset(match);
			if (matcher.matches()) {
				ranking = match.equals(param) ? ID_EXACT_RANK : ID_REGEX_RANK;
				searchTerms.put(ID_SEARCH_TERM, (String)param);
			}
		}
		if (sym instanceof SymWithProps) {
			swp = (SymWithProps) sym;

			// Iterate through each properties.
			for (Map.Entry<String, Object> prop : swp.getProperties().entrySet()) {
				if (prop.getValue() != null) {
					match = ArrayUtils.toString(prop.getValue());
					matcher.reset(match);
					if (matcher.matches()) {
						ranking += match.equals(param) ? PROPERTY_EXACT_RANK : PROPERTY_REGEX_RANK;
						searchTerms.put(prop.getKey(), (String)param);
					}
				}
			}
		}
		SearchResult result = null;
		if (ranking > 0) {
			result = new SearchResult(sym, searchTerms, ranking);
		}
		return result;
	}
}
