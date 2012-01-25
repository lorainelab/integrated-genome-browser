/**
 *   Copyright (c) 2006-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.action.NextSearchSpanAction;
import com.affymetrix.igb.shared.ISearchMode;
import com.affymetrix.igb.shared.IStatus;
import com.affymetrix.igb.shared.SearchResultsTableModel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Text Box for displaying and setting the range of a SeqMapView.
 * 
 * @version $Id$
 */
public final class MapRangeBox implements NeoViewBoxListener, GroupSelectionListener, SeqSelectionListener {

	private final NeoMap map;
	private final SeqMapView gview;
	public final JRPTextField range_box;
	private List<SeqSpan> foundSpans;
	private int spanPointer;

	// Use the ENGLISH locale here because we want the user to be able to
	// cut and paste this text into the UCSC browser.
	// (Also, the Pattern's below were written to work for the English locale.)
	private static final NumberFormat nformat = NumberFormat.getIntegerInstance(Locale.ENGLISH);
	private static final List<ISearchMode> BASE_SEARCH_MODES = new ArrayList<ISearchMode>();
	static {
		BASE_SEARCH_MODES.add(new ChromStartEndSearch());
		BASE_SEARCH_MODES.add(new ChromStartWidthSearch());
		BASE_SEARCH_MODES.add(new ChromPositionSearch());
		BASE_SEARCH_MODES.add(new StartEndSearch());
		BASE_SEARCH_MODES.add(new StartWidthSearch());
		BASE_SEARCH_MODES.add(new CenterSearch());
	}

	private static abstract class EmptySearch implements ISearchMode {
		protected abstract Matcher getMatcher(String search_text);
		@Override public String checkInput(String search_text, BioSeq vseq, String seq) {
			Matcher matcher = getMatcher(search_text);
			return matcher.matches() ? null : "";
		}
		@Override public String getName() { return null; }
		@Override public int searchAllUse() { return 0; }
		@Override public String getTooltip() { return null; }
		@Override public String getOptionName(int i) { return null; }
		@Override public String getOptionTooltip(int i) { return null; }
		@Override public boolean getOptionEnable(int i) { return false; }
		@Override public boolean useOption() { return false; }
		@Override public boolean useDisplaySelected() { return false; }
		@Override public boolean useGenomeInSeqList() { return true; }
		@Override public SearchResultsTableModel getEmptyTableModel() { return null; }
		@Override public SearchResultsTableModel run(String search_text,
				BioSeq chrFilter, String seq, boolean remote,
				IStatus statusHolder) { return null; }
		@Override public List<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder) { return null; }
		@Override public void finished(BioSeq vseq) { }
		@Override public void clear() {}
		@Override public void valueChanged(SearchResultsTableModel model, int srow) { }
		@Override public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) { return new ArrayList<SeqSpan>(); }
		@Override public int getZoomSpot(String search_text) { return NO_ZOOM_SPOT; }
	}
	private static class ChromStartEndSearch extends EmptySearch {
		// accepts a pattern like: "chr2 : 3,040,000 : 4,502,000"  or "chr2:10000-20000"
		// (The chromosome name cannot contain any spaces.)
		protected Matcher getMatcher(String search_text) {
			Pattern chrom_start_end_pattern = Pattern.compile("^\\s*(\\S+)\\s*[:]\\s*([0-9,]+)\\s*[:-]\\s*([0-9,]+)\\s*$");
			Matcher chrom_start_end_matcher = chrom_start_end_pattern.matcher(search_text);
			return chrom_start_end_matcher;
		}
		@Override public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
			Matcher chrom_start_end_matcher = getMatcher(search_text);
			chrom_start_end_matcher.matches();
			String chrom_text = chrom_start_end_matcher.group(1);
			String start_text = chrom_start_end_matcher.group(2);
			String end_or_width_text = chrom_start_end_matcher.group(3);
			int start = 0;
			int end = 0;
			try {
				start = (int)nformat.parse(start_text).doubleValue();
				end  = (int)nformat.parse(end_or_width_text).doubleValue();
			}
			catch (ParseException x) {
				return super.findSpans(search_text, visibleSpan);
			}
			AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
			BioSeq seq = group.getSeq(chrom_text);
			List<SeqSpan> spans = new ArrayList<SeqSpan>();
			spans.add(new SimpleSeqSpan(start, end, seq));
			return spans;
		}
	}
	private static class ChromStartWidthSearch extends EmptySearch {
		// accepts a pattern like: "chr2 : 3,040,000 + 20000"
		// (The chromosome name cannot contain any spaces.)
		protected Matcher getMatcher(String search_text) {
			Pattern chrom_start_width_pattern = Pattern.compile("^\\s*(\\S+)\\s*[:]\\s*([0-9,]+)\\s*\\+\\s*([0-9,]+)\\s*$");
			Matcher chrom_start_width_matcher = chrom_start_width_pattern.matcher(search_text);
			return chrom_start_width_matcher;
		}
		@Override public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
			Matcher chrom_start_width_matcher = getMatcher(search_text);
			chrom_start_width_matcher.matches();
			String chrom_text = chrom_start_width_matcher.group(1);
			String start_text = chrom_start_width_matcher.group(2);
			String width_text = chrom_start_width_matcher.group(3);
			int start = 0;
			int end = 0;
			try {
				start = (int)nformat.parse(start_text).doubleValue();
				end = start + (int)nformat.parse(width_text).doubleValue();
			}
			catch (ParseException x) {
				return super.findSpans(search_text, visibleSpan);
			}
			AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
			BioSeq seq = group.getSeq(chrom_text);
			List<SeqSpan> spans = new ArrayList<SeqSpan>();
			spans.add(new SimpleSeqSpan(start, end, seq));
			return spans;
		}
	}
	private static class ChromPositionSearch extends EmptySearch {
		// accepts a pattern like: "chr2 :10000"
		// (The chromosome name cannot contain any spaces.)
		protected Matcher getMatcher(String search_text) {
			Pattern chrom_position_pattern = Pattern.compile("^\\s*(\\S+)\\s*\\:\\s*([0-9,]+)\\s*$");
			Matcher chrom_position_matcher = chrom_position_pattern.matcher(search_text);
			return chrom_position_matcher;
		}
		@Override public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
			if (visibleSpan == null) {
				return super.findSpans(search_text, visibleSpan);
			}
			Matcher chrom_position_matcher = getMatcher(search_text);
			chrom_position_matcher.matches();
			String chrom_text = chrom_position_matcher.group(1);
			String position_text = chrom_position_matcher.group(2);
			int position = 0;
			try {
				position = (int)nformat.parse(position_text).doubleValue();
			}
			catch (ParseException x) {
				return super.findSpans(search_text, visibleSpan);
			}
			int width = (visibleSpan.getEnd() - visibleSpan.getStart());
			int start = Math.max(0, position - width / 2);
			int end = start + width;
			AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
			BioSeq seq = group.getSeq(chrom_text);
			if (end >= seq.getLength()) {
				end = seq.getLength() - 1;
				start = end - width;
			}
			List<SeqSpan> spans = new ArrayList<SeqSpan>();
			spans.add(new SimpleSeqSpan(start, end, seq));
			return spans;
		}
		@Override public int getZoomSpot(String search_text) {
			Matcher chrom_position_matcher = getMatcher(search_text);
			chrom_position_matcher.matches();
			String position_text = chrom_position_matcher.group(2);
			int position = 0;
			try {
				position = (int)nformat.parse(position_text).doubleValue();
			}
			catch (ParseException x) {
				return super.getZoomSpot(search_text);
			}
			return position;
		}
	}
	private static class StartEndSearch extends EmptySearch {
		// accepts a pattern like: "10000-20000"
		// (The chromosome name cannot contain any spaces.)
		protected Matcher getMatcher(String search_text) {
			Pattern start_end_pattern = Pattern.compile("^\\s*([0-9,]+)\\s*\\-\\s*([0-9,]+)\\s*$");
			Matcher start_end_matcher = start_end_pattern.matcher(search_text);
			return start_end_matcher;
		}
		@Override public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
			Matcher start_end_matcher = getMatcher(search_text);
			start_end_matcher.matches();
			String start_text = start_end_matcher.group(1);
			String end_or_width_text = start_end_matcher.group(2);
			int start = 0;
			int end = 0;
			try {
				start = (int)nformat.parse(start_text).doubleValue();
				end  = (int)nformat.parse(end_or_width_text).doubleValue();
			}
			catch (ParseException x) {
				return super.findSpans(search_text, visibleSpan);
			}
			BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
			List<SeqSpan> spans = new ArrayList<SeqSpan>();
			spans.add(new SimpleSeqSpan(start, end, seq));
			return spans;
		}
	}
	private static class StartWidthSearch extends EmptySearch {
		// accepts a pattern like: "3,040,000 + 20000"
		// (The chromosome name cannot contain any spaces.)
		protected Matcher getMatcher(String search_text) {
			Pattern start_width_pattern = Pattern.compile("^\\s*([0-9,]+)\\s*[+]\\s*([0-9,]+)\\s*$");
			Matcher start_width_matcher = start_width_pattern.matcher(search_text);
			return start_width_matcher;
		}
		@Override public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
			Matcher start_width_matcher = getMatcher(search_text);
			start_width_matcher.matches();
			String start_text = start_width_matcher.group(1);
			String width_text = start_width_matcher.group(2);
			int start = 0;
			int end = 0;
			try {
				start = (int)nformat.parse(start_text).doubleValue();
				end = start + (int)nformat.parse(width_text).doubleValue();
			}
			catch (ParseException x) {
				return super.findSpans(search_text, visibleSpan);
			}
			BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
			List<SeqSpan> spans = new ArrayList<SeqSpan>();
			spans.add(new SimpleSeqSpan(start, end, seq));
			return spans;
		}
	}
	private static class CenterSearch extends EmptySearch {
		// accepts a pattern like: "3,040,000 + 20000"
		// (The chromosome name cannot contain any spaces.)
		protected Matcher getMatcher(String search_text) {
			Pattern center_pattern = Pattern.compile("^\\s*([0-9,]+)\\s*\\s*$");
			Matcher center_matcher = center_pattern.matcher(search_text);
			return center_matcher;
		}
		@Override public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
			Matcher center_matcher = getMatcher(search_text);
			center_matcher.matches();
			String center_text = center_matcher.group(1);
			int center = 0;
			try {
				center = (int)nformat.parse(center_text).doubleValue();
			}
			catch (ParseException x) {
				return super.findSpans(search_text, visibleSpan);
			}
			int start = visibleSpan.getStart();
			int end = visibleSpan.getEnd();
			int width = end - start;
			start = (center - width / 2);
			end = (center + width / 2);
			BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
			List<SeqSpan> spans = new ArrayList<SeqSpan>();
			spans.add(new SimpleSeqSpan(start, end, seq));
			return spans;
		}
		@Override public int getZoomSpot(String search_text) { 
			Matcher chrom_position_matcher = getMatcher(search_text);
			chrom_position_matcher.matches();
			String center_text = chrom_position_matcher.group(1);
			int center = 0;
			try {
				center = (int)nformat.parse(center_text).doubleValue();
			}
			catch (ParseException x) {
				return super.getZoomSpot(search_text);
			}
			return center;
		}
	}
	public MapRangeBox(SeqMapView gview) {
		this.gview = gview;
		this.map = gview.getSeqMap();
		range_box = new JRPTextField(gview.getClass().getSimpleName() + "_SeqMap_range", "");
		Dimension d = new Dimension(250, range_box.getPreferredSize().height);
		range_box.setPreferredSize(d);
		range_box.setMaximumSize(d);

		range_box.setToolTipText("<html>Enter a coordinate range here.<br>"
				+ "Use the format 'start : end' or 'start + width' or 'center',<br>"
				+ "or use the UCSC browser format 'chrom:start-end'.<html>");

		range_box.setEditable(true);
		range_box.addActionListener(action_listener);
		range_box.addFocusListener(focus_listener);
		map.addViewBoxListener(this);
		GenometryModel.getGenometryModel().addGroupSelectionListener(this);
		GenometryModel.getGenometryModel().addSeqSelectionListener(this);
	}

	public void viewBoxChanged(NeoViewBoxChangeEvent e) {
		Rectangle2D.Double vbox = e.getCoordBox();
		setRangeText(vbox.x, vbox.width + vbox.x);
	}

	@Override
	public void groupSelectionChanged(GroupSelectionEvent evt) {
		range_box.setText("");
		resetSearch();
	}

	@Override
	public void seqSelectionChanged(SeqSelectionEvent evt) {
		resetSearch();
	}

	void setRangeText(double start, double end) {
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		range_box.setText((seq == null ? "" : seq.getID() + " : ") + nformat.format(start) + " - " + nformat.format(end));
	}
	ActionListener action_listener = new ActionListener() {

		public void actionPerformed(ActionEvent evt) {
			setRange(gview, range_box.getText());
			// But if the user tries to zoom to something illogical, this can be helpful
			// generally this is redundant, because zooming the view will make
			// a call back to change this text.
			// But if the user tries to zoom to something illogical, this can be helpful
			SeqSpan span = gview.getVisibleSpan();
			if (span == null) {
				range_box.setText("");
			} else {
				setRangeText(span.getStart(), span.getEnd());
			}
		}
	};

	FocusListener focus_listener = new FocusListener() {

		@Override
		public void focusGained(FocusEvent e) {
			range_box.setSelectionStart(0);
			range_box.setSelectionEnd(range_box.getText().length());
	    }

		@Override
		public void focusLost(FocusEvent e) {}
	};

	private List<SeqSpan> mergeSpans(List<SeqSpan> unmergedSpans) {
		List<SeqSpan> mergedSpans = new ArrayList<SeqSpan>();
		for (SeqSpan rawSpan : unmergedSpans) {
			SeqSpan span = new SimpleSeqSpan(rawSpan.getMin(), rawSpan.getMax(), rawSpan.getBioSeq());
			boolean overlap = false;
			for (int i = 0; i < mergedSpans.size() && !overlap; i++) {
				SeqSpan loopSpan = mergedSpans.get(i);
				if (span.getBioSeq().equals(loopSpan.getBioSeq()) && span.getEnd() >= loopSpan.getStart() && span.getStart() <= loopSpan.getEnd()) {
					int start = Math.min(span.getStart(), loopSpan.getStart());
					int end = Math.max(span.getEnd(), loopSpan.getEnd());
					mergedSpans.set(i, new SimpleSeqSpan(start, end, span.getBioSeq()));
					overlap = true;
				}
			}
			if (!overlap) {
				mergedSpans.add(span);
			}
		}
		return mergedSpans;
	}

	/**
	 * Set range of view. This will go through all the ISearchMode
	 * instances registered, including plugins. The standard forms
	 * of region entry are hard coded. This method tries all the
	 * ISearchModes until the first one that gives a positive result.
	 * @param gview - the SeqMapView.
	 * @param range - any search string like "chr1: 40000 - 60000",
	 * or "ADAR" (a gene name)
	 */
	public void setRange(SeqMapView gview, String search_text) {
		List<ISearchMode> modes = new ArrayList<ISearchMode>(BASE_SEARCH_MODES);
		modes.addAll(ExtensionPointHandler.getExtensionPoint(ISearchMode.class).getExtensionPointImpls());
		for (ISearchMode mode : modes) {
			if (mode.useGenomeInSeqList() && mode.checkInput(search_text, null, null) == null && mode.searchAllUse() >= 0) {
				List<SeqSpan> rawSpans = mode.findSpans(search_text, gview.getVisibleSpan());
				if (rawSpans.size() > 0) {
					List<SeqSpan> mergedSpans = mergeSpans(rawSpans);
					zoomToSeqAndSpan(gview, mergedSpans.get(0));
					int zoomSpot = mode.getZoomSpot(search_text);
					if (zoomSpot != ISearchMode.NO_ZOOM_SPOT) {
						gview.setZoomSpotX(zoomSpot);
					}
					foundSpans = mergedSpans;
					spanPointer = 0;
					if (foundSpans.size() > 1) {
						Application.getSingleton().setStatus("found " + foundSpans.size() + " spans");
						NextSearchSpanAction.getAction().setEnabled(true);
					}
					else {
						NextSearchSpanAction.getAction().setEnabled(false);
					}
				}
				return;
			}
		}
		NextSearchSpanAction.getAction().setEnabled(false);
		Application.getSingleton().setStatus("unable to match entry");
	}

	public boolean nextSpan() {
		if (spanPointer + 1 >= foundSpans.size()) {
			Application.getSingleton().setStatus("no span to zoom to");
			return false;
		}
		spanPointer++;
		List<SeqSpan> saveFoundSpans = foundSpans;
		int saveSpanPointer = spanPointer;
		zoomToSeqAndSpan(gview, foundSpans.get(spanPointer));
		foundSpans = saveFoundSpans;
		spanPointer = saveSpanPointer;
		Application.getSingleton().setStatus("zoom to span " + (spanPointer + 1) + " of " + foundSpans.size());
		NextSearchSpanAction.getAction().setEnabled(spanPointer + 1 < foundSpans.size());
		return true;
	}

	private void resetSearch() {
		foundSpans = null;
		spanPointer = -1;
		NextSearchSpanAction.getAction().setEnabled(false);
	}

	private void zoomToSeqAndSpan(SeqMapView gview, SeqSpan span) throws NumberFormatException {
		zoomToSeqAndSpan(gview, span.getBioSeq().getID(), span.getStart(), span.getEnd());
	}

	public void zoomToSeqAndSpan(SeqMapView gview, String chrom_text, int start, int end) throws NumberFormatException {
		AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		if (group == null) {
			Logger.getLogger(MapRangeBox.class.getName()).severe("Group wasn't set");
			return;
		}

		BioSeq newSeq = group.getSeq(chrom_text);
		if (newSeq == null) {
			Logger.getLogger(MapRangeBox.class.getName()).severe("Couldn't find chromosome " + chrom_text + " in group " + group.getID());
			return;
		}

		if (newSeq != GenometryModel.getGenometryModel().getSelectedSeq()) {
			// set the chromosome, and sleep until it's set.
			GenometryModel.getGenometryModel().setSelectedSeq(newSeq);
			for (int i = 0; i < 100; i++) {
				if (GenometryModel.getGenometryModel().getSelectedSeq() != newSeq) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException ex) {
						Logger.getLogger(MapRangeBox.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		}

		gview.setRegion(start, end, newSeq);
	}
}
