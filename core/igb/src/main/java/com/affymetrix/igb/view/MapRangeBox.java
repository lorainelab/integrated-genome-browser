/**
 * Copyright (c) 2006-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy of the license must be included with
 * any distribution of this source code. Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.event.GenomeVersionSelectionEvent;
import com.affymetrix.genometry.event.GroupSelectionListener;
import com.affymetrix.genometry.event.SeqSelectionEvent;
import com.affymetrix.genometry.event.SeqSelectionListener;
import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometry.util.ModalUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.swing.CCPUtils;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.swing.JRPTextField;
import com.jidesoft.hints.ListDataIntelliHints;
import com.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import com.lorainelab.igb.services.search.ISearchHints;
import com.lorainelab.igb.services.search.ISearchModeSym;
import com.lorainelab.igb.services.search.IStatus;
import com.lorainelab.igb.services.search.SearchListener;
import com.lorainelab.igb.services.search.SearchModeRegistry;
import com.lorainelab.igb.services.search.SearchResults;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Text Box for displaying and setting the range of a SeqMapView.
 *
 * @version $Id: MapRangeBox.java 10972 2012-04-05 17:00:51Z lfrohman $
 */
public final class MapRangeBox implements ActionListener, NeoViewBoxListener, GroupSelectionListener, SeqSelectionListener, SymSelectionListener {

    private static final Logger logger = LoggerFactory.getLogger(MapRangeBox.class);
    public static final int NO_ZOOM_SPOT = -1;

    private final NeoMap map;
    private final SeqMapView gview;
    public final JRPTextField range_box;
    private List<SeqSpan> foundSpans;
    private int spanPointer;
    private final Set<SearchListener> search_listeners = new CopyOnWriteArraySet<>();
    private double start, end;
//	private static String[] regexChars = new String[]{"|","(",")","+"};//Tk

    // Use the ENGLISH locale here because we want the user to be able to
    // cut and paste this text into the UCSC browser.
    // (Also, the Pattern's below were written to work for the English locale.)
    private static final NumberFormat nformat = NumberFormat.getIntegerInstance(Locale.ENGLISH);
    private static final List<EmptySearch> BASE_SEARCH_MODES = new ArrayList<>();

    static {
        BASE_SEARCH_MODES.add(new ChromStartEndSearch());
        BASE_SEARCH_MODES.add(new ChromStartWidthSearch());
        BASE_SEARCH_MODES.add(new ChromPositionSearch());
        BASE_SEARCH_MODES.add(new StartEndSearch());
        BASE_SEARCH_MODES.add(new StartWidthSearch());
        BASE_SEARCH_MODES.add(new CenterSearch());
    }

    IStatus application_statusbar = text -> IGB.getInstance().setStatus(text);

    @Override
    public void symSelectionChanged(SymSelectionEvent evt) {
        setRangeText(start, end);
    }

    private static abstract class EmptySearch {

        protected abstract Matcher getMatcher(String search_text);

        public boolean testInput(String search_text) {
            return getMatcher(search_text).matches();
        }

        public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
            return new ArrayList<>();
        }

        public int getZoomSpot(String search_text) {
            return NO_ZOOM_SPOT;
        }
    }

    private static class ChromStartEndSearch extends EmptySearch {
        // accepts a pattern like: "chr2 : 3,040,000 : 4,502,000"  or "chr2:10000-20000"
        // (The chromosome name cannot contain any spaces.)

        protected Matcher getMatcher(String search_text) {
            Pattern chrom_start_end_pattern = Pattern.compile("^\\s*(\\S+)\\s*[:]\\s*([0-9,]+)\\s*[:-]\\s*([0-9,]+)\\s*$");
            Matcher chrom_start_end_matcher = chrom_start_end_pattern.matcher(search_text);
            return chrom_start_end_matcher;
        }

        @Override
        public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
            Matcher chrom_start_end_matcher = getMatcher(search_text);
            chrom_start_end_matcher.matches();
            String chrom_text = chrom_start_end_matcher.group(1);
            String start_text = chrom_start_end_matcher.group(2);
            String end_or_width_text = chrom_start_end_matcher.group(3);
            int start = 0;
            int end = 0;
            try {
                start = (int) nformat.parse(start_text).doubleValue();
                end = (int) nformat.parse(end_or_width_text).doubleValue();
            } catch (ParseException x) {
                return super.findSpans(search_text, visibleSpan);
            }
            GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
            BioSeq seq = genomeVersion.getSeq(chrom_text);
            List<SeqSpan> spans = new ArrayList<>();
            if (seq != null) {
                spans.add(new SimpleSeqSpan(start, end, seq));
            }
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

        @Override
        public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
            Matcher chrom_start_width_matcher = getMatcher(search_text);
            chrom_start_width_matcher.matches();
            String chrom_text = chrom_start_width_matcher.group(1);
            String start_text = chrom_start_width_matcher.group(2);
            String width_text = chrom_start_width_matcher.group(3);
            int start = 0;
            int end = 0;
            try {
                start = (int) nformat.parse(start_text).doubleValue();
                end = start + (int) nformat.parse(width_text).doubleValue();
            } catch (ParseException x) {
                return super.findSpans(search_text, visibleSpan);
            }
            GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
            BioSeq seq = genomeVersion.getSeq(chrom_text);
            List<SeqSpan> spans = new ArrayList<>();
            if (seq != null) {
                spans.add(new SimpleSeqSpan(start, end, seq));
            }
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

        @Override
        public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
            if (visibleSpan == null) {
                return super.findSpans(search_text, visibleSpan);
            }
            Matcher chrom_position_matcher = getMatcher(search_text);
            chrom_position_matcher.matches();
            String chrom_text = chrom_position_matcher.group(1);
            String position_text = chrom_position_matcher.group(2);
            int position = 0;
            try {
                position = (int) nformat.parse(position_text).doubleValue();
            } catch (ParseException x) {
                return super.findSpans(search_text, visibleSpan);
            }
            int width = (visibleSpan.getEnd() - visibleSpan.getStart());
            int start = Math.max(0, position - width / 2);
            int end = start + width;
            GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
            BioSeq seq = genomeVersion.getSeq(chrom_text);
            List<SeqSpan> spans = new ArrayList<>();
            if (seq != null) {
                if (end >= seq.getLength()) {
                    end = seq.getLength() - 1;
                    start = end - width;
                }
                spans.add(new SimpleSeqSpan(start, end, seq));
            }
            return spans;
        }

        @Override
        public int getZoomSpot(String search_text) {
            Matcher chrom_position_matcher = getMatcher(search_text);
            chrom_position_matcher.matches();
            String position_text = chrom_position_matcher.group(2);
            int position = 0;
            try {
                position = (int) nformat.parse(position_text).doubleValue();
            } catch (ParseException x) {
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

        @Override
        public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
            Matcher start_end_matcher = getMatcher(search_text);
            start_end_matcher.matches();
            String start_text = start_end_matcher.group(1);
            String end_or_width_text = start_end_matcher.group(2);
            int start = 0;
            int end = 0;
            try {
                start = (int) nformat.parse(start_text).doubleValue();
                end = (int) nformat.parse(end_or_width_text).doubleValue();
            } catch (ParseException x) {
                return super.findSpans(search_text, visibleSpan);
            }
            BioSeq seq = GenometryModel.getInstance().getSelectedSeq().orElse(null);
            List<SeqSpan> spans = new ArrayList<>();
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

        @Override
        public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
            Matcher start_width_matcher = getMatcher(search_text);
            start_width_matcher.matches();
            String start_text = start_width_matcher.group(1);
            String width_text = start_width_matcher.group(2);
            int start = 0;
            int end = 0;
            try {
                start = (int) nformat.parse(start_text).doubleValue();
                end = start + (int) nformat.parse(width_text).doubleValue();
            } catch (ParseException x) {
                return super.findSpans(search_text, visibleSpan);
            }
            BioSeq seq = GenometryModel.getInstance().getSelectedSeq().orElse(null);
            List<SeqSpan> spans = new ArrayList<>();
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

        @Override
        public List<SeqSpan> findSpans(String search_text, SeqSpan visibleSpan) {
            Matcher center_matcher = getMatcher(search_text);
            center_matcher.matches();
            String center_text = center_matcher.group(1);
            int center = 0;
            try {
                center = (int) nformat.parse(center_text).doubleValue();
            } catch (ParseException x) {
                return super.findSpans(search_text, visibleSpan);
            }
            int start = visibleSpan.getStart();
            int end = visibleSpan.getEnd();
            int width = end - start;
            start = (center - width / 2);
            end = (center + width / 2);
            BioSeq seq = GenometryModel.getInstance().getSelectedSeq().orElse(null);
            List<SeqSpan> spans = new ArrayList<>();
            spans.add(new SimpleSeqSpan(start, end, seq));
            return spans;
        }

        @Override
        public int getZoomSpot(String search_text) {
            Matcher chrom_position_matcher = getMatcher(search_text);
            chrom_position_matcher.matches();
            String center_text = chrom_position_matcher.group(1);
            int center = 0;
            try {
                center = (int) nformat.parse(center_text).doubleValue();
            } catch (ParseException x) {
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

        range_box.setToolTipText(IGBConstants.BUNDLE.getString("goToRegionToolTip"));

        range_box.setEditable(true);
        range_box.addActionListener(this);
        range_box.addFocusListener(focus_listener);

        range_box.setComponentPopupMenu(CCPUtils.getCCPPopup());
        map.addViewBoxListener(this);

        SearchHints hints = new SearchHints(range_box);

        GenometryModel.getInstance().addGroupSelectionListener(this);
        GenometryModel.getInstance().addSeqSelectionListener(this);
        GenometryModel.getInstance().addSymSelectionListener(this);
    }

    public String getText() {
        return range_box.getText();
    }

    public void setText(String text) {
        range_box.setText(text);
    }

    public void viewBoxChanged(NeoViewBoxChangeEvent e) {
        Rectangle2D.Double vbox = e.getCoordBox();
        this.start = vbox.x;
        this.end = vbox.width + vbox.x;
        setRangeText(start, end);
    }

    @Override
    public void groupSelectionChanged(GenomeVersionSelectionEvent evt) {
        range_box.setText("");
        resetSearch();
    }

    @Override
    public void seqSelectionChanged(SeqSelectionEvent evt) {
        resetSearch();
    }

    public void setRangeText(double start, double end) {
        BioSeq seq = GenometryModel.getInstance().getSelectedSeq().orElse(null);
        range_box.setText((seq == null ? "" : seq.getId() + ":") + nformat.format(start) + "-" + nformat.format(end));
    }

    public void actionPerformed(ActionEvent evt) {
        setRange(range_box.getText().trim());
        range_box.requestFocus();
    }

    FocusListener focus_listener = new FocusListener() {

        @Override
        public void focusGained(FocusEvent e) {
            range_box.setSelectionStart(0);
            range_box.setSelectionEnd(range_box.getText().length());
        }

        @Override
        public void focusLost(FocusEvent e) {
        }
    };

    private List<SeqSpan> mergeSpans(List<SeqSpan> unmergedSpans) {
        List<SeqSpan> mergedSpans = new ArrayList<>();
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

    private List<SeqSpan> findSpansFromSyms(List<SeqSymmetry> syms) {
        List<SeqSpan> spans = new ArrayList<>();
        if (syms != null) {
            for (SeqSymmetry sym : syms) {
                for (int i = 0; i < sym.getSpanCount(); i++) {
                    spans.add(sym.getSpan(i));
                }
            }
        }
        return spans;
    }

    private List<SeqSpan> getSpanList(SeqMapView gview, String searchText) {
        for (EmptySearch emptySearch : BASE_SEARCH_MODES) {
            if (emptySearch.testInput(searchText)) {
                List<SeqSpan> rawSpans = emptySearch.findSpans(searchText, gview.getVisibleSpan());
                if (rawSpans.size() > 0) {
                    List<SeqSpan> mergedSpans = mergeSpans(rawSpans);
                    zoomToSeqAndSpan(gview, mergedSpans.get(0));
                    int zoomSpot = emptySearch.getZoomSpot(searchText);
                    if (zoomSpot != NO_ZOOM_SPOT) {
                        gview.setZoomSpotX(zoomSpot);
                    }
                    return mergedSpans;
                }
            }
        }
        String searchTerm = searchText;
        searchTerm = Pattern.quote(searchTerm);// kTs n Tk
        //for(String c : regexChars){
        //	search_term = search_term.replace(c, "\\"+c);
        //}
        for (ISearchModeSym searchMode : SearchModeRegistry.getSearchModeSyms()) {
            if (searchMode.checkInput(searchTerm, null, null) == null /*&& searchMode.searchAllUse() >= 0*/) {
//				for (TypeContainerAnnot trackSym : trackSyms) {
                List<SeqSymmetry> res = null;
                SearchResults<SeqSymmetry> searchResults = null;
                String errorMessage = searchMode.checkInput(searchTerm, null, null);
                if (errorMessage == null) {
                    searchResults = searchMode.search(searchTerm, null, application_statusbar, false);
                    res = searchResults != null ? searchResults.getResults() : null;
                }
                if (searchResults != null && res != null && res.size() > 0) {

                    //sort by similarity to original search
                    Collections.sort(res, (SeqSymmetry o1, SeqSymmetry o2) -> {
                        Integer i1 = StringUtils.getLevenshteinDistance(o1.getID().toLowerCase(), searchText.toLowerCase());
                        Integer i2 = StringUtils.getLevenshteinDistance(o2.getID().toLowerCase(), searchText.toLowerCase());
                        return (i2 > i1) ? 1 : -1;
                    });

                    fireSearchResult(searchResults);
                    List<SeqSpan> rawSpans = findSpansFromSyms(res);
                    if (rawSpans.size() > 0) {
                        zoomToSeqAndSpan(gview, rawSpans.get(0));
                        return rawSpans;
                    }
                }
//				}
            }
        }
        return null;
    }

    private List<TypeContainerAnnot> getTrackSyms() {
        List<TypeContainerAnnot> trackSyms = new ArrayList<>();
        List<TierGlyph> tierGlyphs = gview.getTierManager().getAllTierGlyphs(false);
        for (GlyphI selectedTierGlyph : tierGlyphs) {
            Object info = selectedTierGlyph.getInfo();
            if (info instanceof TypeContainerAnnot) {
                trackSyms.add((TypeContainerAnnot) info);
            }
        }
        return trackSyms;
    }

    /**
     * Set range of view. This will go through all the ISearchMode instances registered, including plugins. The standard
     * forms of region entry are hard coded. This method tries all the ISearchModes until the first one that gives a
     * positive result.
     *
     * @param search_text - any search string like "chr1: 40000 - 60000", or "ADAR" (a gene name)
     */
    public void setRange(String search_text) {
        List<SeqSpan> mergedSpans = getSpanList(gview, search_text);
        if (mergedSpans != null && mergedSpans.size() > 0) {
            foundSpans = mergedSpans;
            spanPointer = 0;
            if (foundSpans.size() > 1) {
                IGB.getInstance().setStatus(null);
            } else {
            }
        } else {
            String errorMessage = MessageFormat.format(IGBConstants.BUNDLE.getString("searchTextNotFound"), search_text);
            ModalUtils.infoPanel(errorMessage);
            IGB.getInstance().setStatus(errorMessage);
        }
    }

    public boolean nextSpan() {
        if (spanPointer + 1 >= foundSpans.size()) {
            IGB.getInstance().setStatus("no span to zoom to");
            return false;
        }
        spanPointer++;
        List<SeqSpan> saveFoundSpans = foundSpans;
        int saveSpanPointer = spanPointer;
        zoomToSeqAndSpan(gview, foundSpans.get(spanPointer));
        foundSpans = saveFoundSpans;
        spanPointer = saveSpanPointer;
        IGB.getInstance().setStatus("zoom to span " + (spanPointer + 1) + " of " + foundSpans.size());
//		NextSearchSpanAction.getAction().setEnabled(spanPointer + 1 < foundSpans.size());
        return true;
    }

    private void resetSearch() {
        foundSpans = null;
        spanPointer = -1;
//		NextSearchSpanAction.getAction().setEnabled(false);
    }

    private void zoomToSeqAndSpan(SeqMapView gview, SeqSpan span) throws NumberFormatException {
        zoomToSeqAndSpan(gview, span.getBioSeq().getId(), span.getStart(), span.getEnd());
    }

    public void zoomToSeqAndSpan(SeqMapView gview, String chrom_text, int start, int end) throws NumberFormatException {
        GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
        if (genomeVersion == null) {
            logger.error("Group wasn't set");
            return;
        }

        BioSeq newSeq = genomeVersion.getSeq(chrom_text);
        if (newSeq == null) {
            logger.error("Couldn''t find chromosome {0} in group {1}", new Object[]{chrom_text, genomeVersion.getName()});
            return;
        }

        if (newSeq != GenometryModel.getInstance().getSelectedSeq().orElse(null)) {
            // set the chromosome, and sleep until it's set.
            GenometryModel.getInstance().setSelectedSeq(newSeq);
            for (int i = 0; i < 100; i++) {
                if (GenometryModel.getInstance().getSelectedSeq().orElse(null) != newSeq) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            }
        }

        gview.setRegion(start, end, newSeq);
    }

    public void addSearchListener(SearchListener listener) {
        search_listeners.add(listener);
    }

    public void removeSearchListener(SearchListener listener) {
        search_listeners.remove(listener);
    }

    private void fireSearchResult(SearchResults<SeqSymmetry> searchResults) {
        for (SearchListener listener : search_listeners) {
            listener.searchResults(searchResults);
        }
    }

    private class SearchHints extends ListDataIntelliHints<String> {

        public SearchHints(JRPTextField searchBox) {
            super(searchBox, new String[]{});
        }

        @Override
        public void acceptHint(Object context) {
            String text = (String) context;
            super.acceptHint(context);
            setText(text);
            setRange(text);
        }

        @Override
        public boolean updateHints(Object context) {
            String search_term = (String) context;
            if (GenometryModel.getInstance().getSelectedGenomeVersion() == null || search_term.length() <= 1) {
                return false;
            } else {
                List<ISearchHints> modes = new ArrayList<>();
                modes.addAll(ExtensionPointHandler.getExtensionPoint(ISearchHints.class).getExtensionPointImpls());
                Set<String> results = new HashSet<>(ISearchHints.MAX_HITS * modes.size());

                for (ISearchHints mode : modes) {
                    Set<String> syms = mode.search(search_term);
                    if (syms != null && !syms.isEmpty()) {
                        results.addAll(syms);
                    }
                }

                if (results.size() >= 1) {
                    this.setListData(results.toArray());
                    return true;
                }
            }
            return false;
        }
    }
}
