package com.affymetrix.igb.search.mode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.das2.SimpleDas2Feature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.SearchUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.search.IStatus;
import com.affymetrix.igb.search.SearchView;

public abstract class SearchModeIDOrProps implements ISearchMode {
	private final static int MAX_HITS = 100000;
	private List<SeqSymmetry> remoteSymList;

	private IGBService igbService;

	@SuppressWarnings("serial")
	private class SymSearchResultsTableModel extends SearchResultsTableModel {
		private final int[] colWidth = {};
		private final int[] colAlign = {};
		
		private final List<SeqSymmetry> tableRows = new ArrayList<SeqSymmetry>(0);
		
		private final String[] column_names = {
			SearchView.BUNDLE.getString("searchTableID"),
			SearchView.BUNDLE.getString("searchTableTier"),
			SearchView.BUNDLE.getString("searchTableGeneName"),
			SearchView.BUNDLE.getString("searchTableStart"),
			SearchView.BUNDLE.getString("searchTableEnd"),
			SearchView.BUNDLE.getString("searchTableChromosome"),
			SearchView.BUNDLE.getString("searchTableStrand")
		};
		private static  final int ID_COLUMN = 0;
		private static final int TIER_COLUMN = 1;
		private static  final int GENE_NAME_COLUMN = 2;
		private static final int START_COLUMN = 3;
		private static final int END_COLUMN = 4;
		private static final int CHROM_COLUMN = 5;
		private static final int STRAND_COLUMN = 6;

		public SymSearchResultsTableModel(List<SeqSymmetry> results) {
			tableRows.addAll(results);
		}

		public Object getValueAt(int row, int col) {
			SeqSymmetry sym = tableRows.get(row);
			SeqSpan span = sym.getSpan(0);
			switch (col) {
				case ID_COLUMN:
					return sym.getID();
				case TIER_COLUMN:
					return BioSeq.determineMethod(sym);
				case GENE_NAME_COLUMN:
					if (sym instanceof SimpleDas2Feature) {
						String geneName = ((SimpleDas2Feature)sym).getName();
						return geneName == null ? "" : geneName;
					}
					if (sym instanceof SymWithProps) {
						String geneName = (String)((SymWithProps)sym).getProperty("gene name");
						return geneName == null ? "" : geneName;
					}
					return "";
				case START_COLUMN:
					if (sym instanceof UcscPslSym) {
						return (((UcscPslSym) sym).getSameOrientation()) ? 
							((UcscPslSym) sym).getTargetMin() : ((UcscPslSym) sym).getTargetMax();
					}
					return (span == null ? "" : span.getStart());
				case END_COLUMN:
					if (sym instanceof UcscPslSym) {
						return (((UcscPslSym) sym).getSameOrientation()) ?
							((UcscPslSym) sym).getTargetMax() : ((UcscPslSym) sym).getTargetMin();
					}
					return (span == null ? "" : span.getEnd());
				case CHROM_COLUMN:
					if (sym instanceof UcscPslSym) {
						return ((UcscPslSym) sym).getTargetSeq().getID();
					}
					return (span == null ? "" : span.getBioSeq().getID());
				case STRAND_COLUMN:
					if (sym instanceof UcscPslSym) {
						return (
								(((UcscPslSym) sym).getSameOrientation())
								? "+" : "-");
					}
					if (span == null) {
						return "";
					}
					return (span.isForward() ? "+" : "-");
			}
			return "";
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
			
		public int getColumnCount() {
			return column_names.length;
		}

		@Override
		public String getColumnName(int col) {
			return column_names[col];
		}

		public int getRowCount() {
			return tableRows.size();
		}

		@Override
		public Class<?> getColumnClass(int column) {
			if(column == START_COLUMN || column == END_COLUMN) {
				return Number.class;
			}
			return String.class;
		}

		@Override
		public SeqSymmetry get(int i) {
			return tableRows.get(i);
		}

		@Override
		public void clear(){
			tableRows.clear();
		}

		@Override
		public int[] getColumnWidth() {
			return colWidth;
		}

		@Override
		public int[] getColumnAlign() {
			return colAlign;
		}
	}

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
	public boolean checkInput(String search_text, BioSeq vseq, String seq) {
		try {
			getRegex(search_text);
		} catch (PatternSyntaxException pse) {
			ErrorHandler.errorPanel("Regular expression syntax error...\n" + pse.getMessage());
			return false;
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem with regular expression...", ex);
			return false;
		}
		return true;
	}

	public SearchResultsTableModel getEmptyTableModel() {
		return new SymSearchResultsTableModel(Collections.<SeqSymmetry>emptyList());
	}

	protected SearchResultsTableModel run(final String search_text, final BioSeq chrFilter, final String seq, final boolean search_props, final boolean remote, final IStatus statusHolder, List<GlyphI> glyphs) {
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		String text = search_text;
		Pattern regex = null;
		try {
			regex = getRegex(search_text);
		} catch (Exception e) {} // should not happen, already checked above

		String friendlySearchStr = SearchModeHolder.friendlyString(text, seq);
		statusHolder.setStatus(friendlySearchStr + ": Searching locally...");
		List<SeqSymmetry> localSymList = SearchUtils.findLocalSyms(group, chrFilter, regex, search_props);
		remoteSymList = null;

		// Make sure this search is reasonable to do on a remote server.
		if (!(text.contains("*") || text.contains("^") || text.contains("$"))) {
			// Not much of a regular expression.  Assume the user wants to match at the start and end
			text = "*" + text + "*";
		}
		friendlySearchStr = SearchModeHolder.friendlyString(text, seq);
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

		if (remote) {
			if (actualChars < 3) {
				ErrorHandler.errorPanel(friendlySearchStr + ": Text is too short to allow remote search.");
				return null;
			}

			statusHolder.setStatus(friendlySearchStr + ": Searching remotely...");
			remoteSymList = remoteSearchFeaturesByName(group, text, chrFilter);
		}

		if (localSymList.isEmpty() && (remoteSymList == null || remoteSymList.isEmpty())) {
			statusHolder.setStatus(friendlySearchStr + ": No matches");
			return null;
		}

		String statusStr = friendlySearchStr + ": " + (localSymList == null ? 0 : localSymList.size()) + " local matches";
		if (remote && actualChars >= 3) {
			statusStr += ", " + (remoteSymList == null ? 0 : remoteSymList.size()) + " remote matches";
		}
		statusHolder.setStatus(statusStr);
		gmodel.setSelectedSymmetriesAndSeq(localSymList, this);
		if (remoteSymList != null) {
			localSymList.addAll(remoteSymList);
		}

		List<SeqSymmetry> tableRows = filterBySeq(localSymList, chrFilter);
		Collections.sort(tableRows, new Comparator<SeqSymmetry>() {
			public int compare(SeqSymmetry s1, SeqSymmetry s2) {
				return s1.getID().compareTo(s2.getID());
			}
		});

		return new SymSearchResultsTableModel(tableRows);
	}

	private static List<SeqSymmetry> filterBySeq(List<SeqSymmetry> results, BioSeq seq) {

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

	private static List<SeqSymmetry> remoteSearchFeaturesByName(AnnotatedSeqGroup group, String name, BioSeq chrFilter) {
		List<SeqSymmetry> features = new ArrayList<SeqSymmetry>();

		if (name == null || name.isEmpty()) {
			return features;
		}

		for (GenericVersion gVersion : group.getEnabledVersions()) {
			if (gVersion.gServer.serverType == ServerType.DAS2) {
				Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;
				if (version != null) {
					List<SeqSymmetry> newFeatures = version.getFeaturesByName(name, group, chrFilter);
					if (newFeatures != null) {
						newFeatures = filterBySeq(newFeatures, chrFilter);	// make sure we filter out other chromosomes
						features.addAll(newFeatures);
					}
				}
			}
		}

		return features;
	}

	@Override
	public void finished(BioSeq vseq) {	}

	@Override
	public void valueChanged(SearchResultsTableModel model, int srow, List<GlyphI> glyphs) {
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		SeqSymmetry sym = ((SymSearchResultsTableModel)model).get(srow);

		if (sym != null) {
			if (remoteSymList != null && remoteSymList.contains(sym)) {
				if (group == null) {
					return;
				}
				zoomToCoord(sym);
				return;
			}

			if (igbService.getItem(sym) == null) {
				if (group == null) {
					return;
				}
				// Couldn't find sym in map view! Go ahead and zoom to it.
				zoomToCoord(sym);
				return;
			}

			// Set selected symmetry normally
			List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(1);
			syms.add(sym);
			gmodel.setSelectedSymmetriesAndSeq(syms, this);
		}
	}

	private void zoomToCoord(SeqSymmetry sym) throws NumberFormatException {
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		String seqID = sym.getSpanSeq(0).getID();
		BioSeq seq = group.getSeq(seqID);
		if (seq != null) {
			SeqSpan span = sym.getSpan(0);
			if (span != null) {
				// zoom to its coordinates
				igbService.zoomToCoord(seqID, span.getStart(), span.getEnd());
			}
		}
	}

}
