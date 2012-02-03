package com.affymetrix.genometryImpl.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class SymmetryFilterId implements SymmetryFilterI {
	private static final int ID_RANK = 1000;
	private Object param;
	private Pattern regex;
	private String match;

	@Override
	public String getName() {
		return "id";
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
		boolean passes = false;
		if (regex == null) {
			throw new IllegalStateException("invalid filter");
		}
		Matcher matcher = regex.matcher("");
		int ranking = 0;
		match = sym.getID();
		if (match != null) {
			matcher.reset(match);
			if (matcher.matches()) {
				ranking = ID_RANK;
			}
			if (ranking > 0) {
				Map<String,String> searchTerms = new HashMap<String,String>();
				searchTerms.put("id", sym.getID());
				passes = true;
			}
		}
		return passes;
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
}
