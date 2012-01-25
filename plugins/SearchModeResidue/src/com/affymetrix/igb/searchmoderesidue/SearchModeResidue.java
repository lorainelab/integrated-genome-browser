package com.affymetrix.igb.searchmoderesidue;

import java.awt.Color;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.SwingConstants;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SeqMapRefreshed;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.ColorTableCellRenderer;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.ISearchMode;
import com.affymetrix.igb.shared.IStatus;
import com.affymetrix.igb.shared.SearchResultsTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class SearchModeResidue implements ISearchMode, 
		SeqMapRefreshed, SeqSelectionListener {
	
	private static final int SEARCH_ALL_ORDINAL = -1;
	private static final String CONFIRM_BEFORE_SEQ_CHANGE = "Confirm before sequence change";
	private static final boolean default_confirm_before_seq_change = true;
	
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("searchmoderesidue");
	private static final int MAX_RESIDUE_LEN_SEARCH = 1000000;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final Color hitcolors[] = {
		Color.magenta,
		new Color(0x00cd00),
		Color.orange,
		new Color(0x00d7d7),
		new Color(0xb50000),
		Color.blue,
		Color.gray,
		Color.pink};//Distinct Colors for View/Print Ease
	
	private final List<GlyphI> glyphs = new ArrayList<GlyphI>();
	private IGBService igbService;
	private int color = 0;

	@SuppressWarnings("serial")
	private class GlyphSearchResultsTableModel extends SearchResultsTableModel {
		private final int[] colWidth = {20,8,10,10,5,10,65};
		private final int[] colAlign = {SwingConstants.LEFT,SwingConstants.CENTER,SwingConstants.RIGHT,SwingConstants.RIGHT,SwingConstants.CENTER,SwingConstants.CENTER,SwingConstants.LEFT};
		
		private final List<GlyphI> tableRows = new ArrayList<GlyphI>(0);
		protected final String seq;

		public GlyphSearchResultsTableModel(List<GlyphI> results, String seq) {
			tableRows.addAll(results);
			this.seq = seq;
		}

		private final String[] column_names = {
			BUNDLE.getString("searchTablePattern"),
			BUNDLE.getString("searchTableColor"),
			BUNDLE.getString("searchTableStart"),
			BUNDLE.getString("searchTableEnd"),
			BUNDLE.getString("searchTableStrand"),
			BUNDLE.getString("searchTableChromosome"),
			BUNDLE.getString("searchTableMatch")
		};

		private static final int PATTERN_COLUMN = 0;
		private static final int COLOR_COLUMN = 1;
		private static final int START_COLUMN = 2;
		private static final int END_COLUMN = 3;
		private static final int STRAND_COLUMN = 4;
		private static final int CHROM_COLUMN = 5;
		private static final int MATCH_COLUMN = 6;

		@Override
		public GlyphI get(int i) {
			return tableRows.get(i);
		}

		@Override
		public void clear() {
			tableRows.clear();
		}

		public int getRowCount() {
			return tableRows.size();
		}

		public int getColumnCount() {
			return column_names.length;
		}

		@SuppressWarnings("unchecked")
		public Object getValueAt(int row, int col) {
			GlyphI glyph = tableRows.get(row);
			Map<Object, Object> map = (Map<Object, Object>) glyph.getInfo();

			switch (col) {
			
				case PATTERN_COLUMN:
					Object pattern = map.get("pattern");
					if (pattern != null) {
						return pattern.toString();
					}
					return "";
					
				case COLOR_COLUMN:
					return glyph.getColor();
					
				case START_COLUMN:
					return (int)glyph.getCoordBox().x;

				case END_COLUMN:
					return (int)(glyph.getCoordBox().x  + glyph.getCoordBox().width);

				case STRAND_COLUMN:
					Object direction = map.get("direction");
					if (direction != null) {
						if (direction.toString().equalsIgnoreCase("forward")) {
							return "+";
						} else if (direction.toString().equalsIgnoreCase("reverse")) {
							return "-";
						}
					}
					return "";

				case CHROM_COLUMN:
					return seq;
					
				case MATCH_COLUMN:
					Object match = map.get("match");
					if (match != null) {
						return match.toString();
					}
				return "";
			}

			return "";
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public String getColumnName(int col) {
			return column_names[col];
		}
		
		@Override
		public Class<?> getColumnClass(int column) {
			if(column == START_COLUMN || column == END_COLUMN) {
				return Number.class;
			}
			if(column == COLOR_COLUMN) {
				return Color.class;
			}
			return String.class;
		}

		@Override
		public int[] getColumnWidth() {
			return colWidth;
		}

		@Override
		public int[] getColumnAlign() {
			return colAlign;
		}

		@Override
		public DefaultTableCellRenderer getColumnRenderer(int column){
			if(column == COLOR_COLUMN){
				return new ColorTableCellRenderer();
			}
			return super.getColumnRenderer(column);
		}
	}
	
	public SearchModeResidue(IGBService igbService) {
		super();
		this.igbService = igbService;
		igbService.getSeqMapView().addToRefreshList(this);
		
		gmodel.addSeqSelectionListener(this);
	}
	
	public String checkInput(String search_text, final BioSeq vseq, final String seq) {
		if (vseq == null ) {
			return MessageFormat.format(BUNDLE.getString("searchErrorNotLoaded"), seq);
		}
		if (search_text.length() < 3) {
			return BUNDLE.getString("searchErrorShort");
		}
		try {
			Pattern.compile(search_text, Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException pse) {
			return MessageFormat.format(BUNDLE.getString("searchErrorSyntax"), pse.getMessage());
		} catch (Exception ex) {
			return MessageFormat.format(BUNDLE.getString("searchError"), ex.getMessage());
		}
		
		if (vseq != igbService.getSeqMapView().getAnnotatedSeq()){
			boolean confirm = igbService.confirmPanel(MessageFormat.format(BUNDLE.getString("searchSelectSeq"), vseq.getID(), vseq.getID()),
					PreferenceUtils.getTopNode(), CONFIRM_BEFORE_SEQ_CHANGE, default_confirm_before_seq_change);
			if(!confirm) {
				return BUNDLE.getString("searchCancelled");
			}
			SeqSpan newspan = new SimpleSeqSpan(vseq.getMin(), vseq.getMax(), vseq);
			gmodel.setSelectedSeq(vseq);
			igbService.getSeqMapView().zoomTo(newspan);
		}

//		boolean isComplete = vseq.isComplete();
//		boolean confirm = isComplete ? true : igbService.confirmPanel(MessageFormat.format(BUNDLE.getString("searchConfirmLoad"), seq));
//		if (!confirm) {
//			return false;
//		}
		return null;
	}

	@Override
	public void finished(BioSeq vseq) {
		boolean isComplete = vseq.isComplete();
		if (!isComplete) {
			igbService.getSeqMapView().setAnnotatedSeq(vseq, true, true, true);
		}
	}
	public SearchResultsTableModel getEmptyTableModel() {
		return new GlyphSearchResultsTableModel(Collections.<GlyphI>emptyList(),"");
	}

	/**
	 * Display (highlight on SeqMap) the residues matching the specified regex.
	 */
	public SearchResultsTableModel run(String search_text, BioSeq chrFilter, String seq, boolean overlay, IStatus statusHolder) {
		if(!overlay){
			clearResults();
		}
		
		SeqSpan visibleSpan = igbService.getSeqMapView().getVisibleSpan();
		GenericAction loadResidue = igbService.loadResidueAction(visibleSpan, true);
		loadResidue.actionPerformed(null);
		
//		boolean isComplete = chrFilter.isComplete();
//		if (!isComplete) {
//			igbService.loadResidues(igbService.getSeqMapView().getVisibleSpan(), true);
//		}
		
		String friendlySearchStr = MessageFormat.format(BUNDLE.getString("friendlyPattern"), search_text, chrFilter.getID());
		Pattern regex = null;
		try {
			regex = Pattern.compile(search_text, Pattern.CASE_INSENSITIVE);
		} catch (Exception ex) { // should not happen already checked above
			return null;
		}

		statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchConfirmLoad"), friendlySearchStr));

		int residuesLength = chrFilter.getLength();
		int hit_count1 = 0;
		int hit_count2 = 0;
		int residue_offset1 = chrFilter.getMin();
		int residue_offset2 = chrFilter.getMax();
		Thread current_thread = Thread.currentThread();
		
		for(int i=visibleSpan.getMin(); i<visibleSpan.getMax(); i+=MAX_RESIDUE_LEN_SEARCH){
			if(current_thread.isInterrupted())
				break;
			
			int start = Math.max(i-search_text.length(), 0);
			int end = Math.min(i+MAX_RESIDUE_LEN_SEARCH, residuesLength);
			
			String residues = chrFilter.getResidues(start, end);
			hit_count1 += igbService.searchForRegexInResidues(true, regex, residues, Math.max(residue_offset1,start), glyphs, hitcolors[color]);

			// Search for reverse complement of query string
			// flip searchstring around, and redo nibseq search...
			String rev_searchstring = DNAUtils.reverseComplement(residues);
			hit_count2 += igbService.searchForRegexInResidues(false, regex, rev_searchstring, Math.min(residue_offset2,end), glyphs, hitcolors[color]);
		}

		statusHolder.setStatus(MessageFormat.format(BUNDLE.getString("searchFound"), hit_count1, hit_count2));
		igbService.getSeqMap().updateWidget();

		Collections.sort(glyphs, new Comparator<GlyphI>() {
			public int compare(GlyphI g1, GlyphI g2) {
				return Integer.valueOf((int)g1.getCoordBox().x).compareTo((int)g2.getCoordBox().x);
			}
		});
		color++;
		return new GlyphSearchResultsTableModel(glyphs, chrFilter.getID());
	}

	public void clear(){
		clearResults();
	}
	
	public void mapRefresh() {
		igbService.mapRefresh(glyphs);
	}
	
	public void seqSelectionChanged(SeqSelectionEvent evt) {
		clearResults();
	}
	
	private void clearResults() {
		if (!glyphs.isEmpty()) {
			glyphs.clear();
			igbService.getSeqMapView().setAnnotatedSeq(igbService.getSeqMapView().getAnnotatedSeq(), true, true, true);
		}
		color = 0;
	}
		
	@Override
	public String getName() {
		return BUNDLE.getString("searchRegexResidue");
	}

	@Override
	public int searchAllUse() {
		return SEARCH_ALL_ORDINAL;
	}

	@Override
	public String getTooltip() {
		return BUNDLE.getString("searchRegexResidueTF");
	}

	@Override
	public String getOptionName(int i) {
		return BUNDLE.getString("optionCheckBox");
	}

	@Override
	public String getOptionTooltip(int i) {
		return BUNDLE.getString("optionCheckBoxTT");
	}
	
	@Override
	public boolean getOptionEnable(int i) {
		return hitcolors.length - 1 > color;
	}
		
	@Override
	public void valueChanged(SearchResultsTableModel model, int srow) {
		GlyphI glyph = ((GlyphSearchResultsTableModel)model).get(srow);
		for(GlyphI g : glyphs){
			igbService.getSeqMap().deselect(g);
		}
		if(glyph != null){
			int start = (int)glyph.getCoordBox().x;
			int end = (int)(glyph.getCoordBox().x + glyph.getCoordBox().width);
			igbService.getSeqMap().select(glyph);
			igbService.zoomToCoord(((GlyphSearchResultsTableModel)model).seq, start, end);
			igbService.getSeqMapView().centerAtHairline();
		}
	}

	@Override
	public boolean useOption() {
		return true;
	}

	@Override
	public boolean useGenomeInSeqList() {
		return false;
	}

	@Override
	public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
		return new ArrayList<SeqSpan>();
	}

	@Override
	public List<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder) {
		return null;
	}

}
