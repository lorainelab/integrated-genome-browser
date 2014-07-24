package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.Parameter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;

public class SymmetryFilterId extends SymmetryFilter {
	private final static String REGEX = "regex";
	private final static String DEFAULT_REGEX = "*";
	
    private Parameter<String> regexString = new Parameter<String>(DEFAULT_REGEX) {
		
		@Override
		public boolean set(Object e){
			super.set(e);
			regex = getRegex((String)e);
			return regex != null;
		}
	};
	
	private Pattern regex;
	private String match;

	public SymmetryFilterId(){
		super();
		parameters.addParameter(REGEX, String.class, regexString);
	}
	
	@Override
	public String getName() {
		return "id";
	}

	@Override
	public String getDisplay() {
		return getName();
	}
	
	@Override
	public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym) {
		boolean passes = false;
		if (regex == null) {
			throw new IllegalStateException("invalid filter");
		}
		Matcher matcher = regex.matcher("");
		match = sym.getID();
		if (match != null) {
			matcher.reset(match);
			passes = true;
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
