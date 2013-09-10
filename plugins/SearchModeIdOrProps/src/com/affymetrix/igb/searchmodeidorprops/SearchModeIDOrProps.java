package com.affymetrix.igb.searchmodeidorprops;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.shared.SearchUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchModeSym;
import com.affymetrix.igb.shared.IStatus;
import com.affymetrix.igb.shared.SearchResults;

public abstract class SearchModeIDOrProps implements ISearchModeSym {
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("searchmodeidorprops");
	private static final int MAX_HITS = 100000;
	protected static final String FRIENDLY_PATTERN = BUNDLE.getString("friendlyPattern");
	protected IGBService igbService;
	protected static final IStatus DUMMY_STATUS = new IStatus() { public void setStatus(String s){}};

	protected SearchModeIDOrProps(IGBService igbService) {
		super();
		this.igbService = igbService;
	}

	private Pattern getRegex(String search_text) throws Exception  {
		Pattern regex = null;
		String regexText = search_text;
		// Make sure this search is reasonable to do on a remote server.
		if (!(regexText.contains("*") || regexText.contains("^") || regexText.contains("$"))) {
			// Not much of a regular expression.  Assume the user wants to match at the start and end
			regexText = ".*" + regexText + ".*";
		}
		regex = Pattern.compile(regexText, Pattern.CASE_INSENSITIVE);
		return regex;
	}

	@Override
	public String checkInput(String search_text, BioSeq vseq, String seq) {
		try {
			getRegex(search_text);
		} catch (PatternSyntaxException pse) {
			return MessageFormat.format(BUNDLE.getString("searchErrorSyntax"), pse.getMessage());
		} catch (Exception ex) {
			return MessageFormat.format(BUNDLE.getString("searchError"), ex.getMessage());
		}
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

	protected SearchResults search(final String search_text, final BioSeq chrFilter, IStatus statusHolder, boolean remote, final boolean search_props) {
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		String text = search_text;

		String seq = chrFilter == null ? Constants.GENOME_SEQ_ID : chrFilter.getID();
		List<SeqSymmetry> localSymList = findLocalSyms(search_text, chrFilter, seq, search_props, statusHolder);
		List<SeqSymmetry> remoteSymList = null;
		
		// Make sure this search is reasonable to do on a remote server.
		if (!(text.contains("*") || text.contains("^") || text.contains("$"))) {
			// Not much of a regular expression.  Assume the user wants to match at the start and end
			text = "*" + text + "*";
		}
		String friendlySearchStr = MessageFormat.format(FRIENDLY_PATTERN, text, seq);
		int actualChars = text.length();
		if (text.startsWith(".*")) {
			actualChars -= 2;
		} else if (text.startsWith("*")) {
			actualChars -= 1;
		}
		if (text.endsWith(".*")) {
			text = text.substring(0, text.length() - 2) + "*";	// hack for bug in DAS/2 server
			actualChars -= 2;
		} else if (text.endsWith("*")) {
			actualChars -= 1;
		}

		String statusStr;
		if (remote) {
			if (actualChars < 3) {
				statusStr = MessageFormat.format(BUNDLE.getString("searchErrorShort"), friendlySearchStr);
				ErrorHandler.errorPanel(statusStr);
				return new SearchResults(null, search_text, chrFilter != null ? chrFilter.getID() : "genome", statusStr, null);
			}

			//remoteSearches
			remoteSymList = new ArrayList<SeqSymmetry>();
			RemoteSearchDAS2 remoteSearch = new RemoteSearchDAS2();
			statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchSearchingRemote"), friendlySearchStr, remoteSearch.getClass().getName()));
			List<SeqSymmetry> symList = remoteSearch.searchFeatures(group, text, chrFilter);
			symList = filterBySeq(symList, chrFilter);	// make sure we filter out other chromosomes
			remoteSymList.addAll(symList);
		}

		if (localSymList.isEmpty() && (remoteSymList == null || remoteSymList.isEmpty())) {
			statusStr = MessageFormat.format(BUNDLE.getString("searchNoResults"), friendlySearchStr);
			statusHolder.setStatus(statusStr);
			return new SearchResults(null, search_text, chrFilter != null ? chrFilter.getID() : "genome", statusStr, null);
		}
		
		statusStr = MessageFormat.format(BUNDLE.getString("searchLocalResults"), friendlySearchStr,  (localSymList == null ? "0" : "" + localSymList.size()));
		if (remote && actualChars >= 3) {
			statusStr += MessageFormat.format(BUNDLE.getString("searchRemoteResults"), (remoteSymList == null ? "0" : "" + remoteSymList.size()));
		}
		statusHolder.setStatus(statusStr);

		if (remoteSymList != null) {
			localSymList.addAll(remoteSymList);
		}

		List<SeqSymmetry> tableRows = filterBySeq(localSymList, chrFilter);
		Collections.sort(tableRows, new Comparator<SeqSymmetry>() {
			public int compare(SeqSymmetry s1, SeqSymmetry s2) {
				if (s1.getID() == null || s2.getID() == null) {
					return 0;
				}
				return s1.getID().compareTo(s2.getID());
			}
		});

		return new SearchResults(null, search_text, chrFilter != null ? chrFilter.getID() : "genome", statusStr, tableRows);
	}

	protected List<SeqSymmetry> findLocalSyms(String search_text, final BioSeq chrFilter, final String seq, final boolean search_props, final IStatus statusHolder) {
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		String text = search_text;
		Pattern regex = null;
		try {
			regex = getRegex(search_text);
		} catch (Exception e) {} // should not happen, already checked above

		String friendlySearchStr = MessageFormat.format(FRIENDLY_PATTERN, text, seq);
		statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchSearchingLocal"), friendlySearchStr));
		List<SeqSymmetry> localSymList = SearchUtils.findLocalSyms(group, chrFilter, regex, search_props);
		return localSymList;
	}

	@Override
	public List<SeqSymmetry> searchTrack(String search_text, TypeContainerAnnot contSym) {
		return null;
	}
}
