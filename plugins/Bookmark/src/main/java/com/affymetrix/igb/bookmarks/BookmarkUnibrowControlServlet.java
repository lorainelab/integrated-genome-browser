/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.SimpleTrackStyle;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark.SYM;
import com.affymetrix.igb.osgi.service.IGBService;
import com.lorainelab.igb.genoviz.extensions.api.SeqMapViewI;
import com.affymetrix.igb.shared.LoadURLAction;
import com.affymetrix.igb.shared.OpenURIAction;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.primitives.Ints;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * A way of allowing IGB to be controlled via hyperlinks. (This used to be an
 * implementation of HttpServlet, but it isn't now.)
 * <pre>
 *  Can specify:
 *      genome version
 *      chromosome
 *      start of region in view
 *      end of region in view
 *  and bring up corect version, chromosome, and region with (at least)
 *      annotations that can be loaded via QuickLoaderView
 *  If the currently loaded genome doesn't match the one requested, might
 *      ask the user before switching.
 *
 * @version $Id: UnibrowControlServlet.java 7505 2011-02-10 20:27:35Z hiralv $
 * </pre>
 */
public final class BookmarkUnibrowControlServlet {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BookmarkUnibrowControlServlet.class);
    private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();

    private BookmarkUnibrowControlServlet() {
    }

    public static BookmarkUnibrowControlServlet getInstance() {
        return BookmarkUnibrowControlServletHolder.INSTANCE;
    }

    private static class BookmarkUnibrowControlServletHolder {

        private static final BookmarkUnibrowControlServlet INSTANCE = new BookmarkUnibrowControlServlet();
    }

    private static final GenometryModel gmodel = GenometryModel.getInstance();
    private static final Pattern query_splitter = Pattern.compile("[;\\&]");

    /**
     * Loads a bookmark.
     *
     * @param igbService
     * @param parameters Must be a Map where the only values are String and
     * String[] objects. For example, this could be the Map returned by
     * {@link javax.servlet.ServletRequest#getParameterMap()}.
     */
    public void goToBookmark(final IGBService igbService, final ListMultimap<String, String> parameters, final boolean isGalaxyBookmark) {
        String batchFileStr = getFirstValueEntry(parameters, IGBService.SCRIPTFILETAG);
        if (StringUtils.isNotBlank(batchFileStr)) {
            igbService.doActions(batchFileStr);
            return;
        }

        String threadDescription = "Loading Bookmark Data";
        if (isGalaxyBookmark) {
            threadDescription = "Loading Your Galaxy Data";
        }

        CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>(threadDescription) {

            @Override
            protected Object runInBackground() {
                try {
                    String seqid = getFirstValueEntry(parameters, Bookmark.SEQID);
                    String version = getFirstValueEntry(parameters, Bookmark.VERSION);
                    String start_param = getFirstValueEntry(parameters, Bookmark.START);
                    String end_param = getFirstValueEntry(parameters, Bookmark.END);
//				String comment_param = getStringParameter(parameters, Bookmark.COMMENT);
                    String select_start_param = getFirstValueEntry(parameters, Bookmark.SELECTSTART);
                    String select_end_param = getFirstValueEntry(parameters, Bookmark.SELECTEND);
                    boolean loadResidue = Boolean.valueOf(getFirstValueEntry(parameters, Bookmark.LOADRESIDUES));
                    // For historical reasons, there are two ways of specifying graphs in a bookmark
                    // Eventually, they should be treated more similarly, but for now some
                    // differences remain
                    // parameter "graph_file" can be handled by goToBookmark()
                    //    Does not check whether the file was previously loaded
                    //    Loads in GUI-friendly thread
                    //    Must be a file name, not a generic URL
                    // parameter "graph_source_url_0", "graph_source_url_1", ... is handled elsewhere
                    //    Checks to avoid double-loading of files
                    //    Loading can freeze the GUI
                    //    Can be any URL, not just a file
                    boolean has_properties = (parameters.get(SYM.FEATURE_URL + "0") != null);
                    boolean loaddata = true;
                    boolean loaddas2data = true;
                    int start = 0;
                    int end = 0;

                    //missing seqid or start or end? Attempt to set to current view
                    if (missingString(new String[]{seqid, start_param, end_param})) {
                        boolean pickOne = false;
                        //get AnnotatedSeqGroup for bookmark
                        String preferredVersionName = LOOKUP.getPreferredName(version);
                        AnnotatedSeqGroup bookMarkGroup = gmodel.getSeqGroup(preferredVersionName);
                        if (bookMarkGroup != null) {
                            //same genome version as that in view?
                            SeqMapViewI currentSeqMap = igbService.getSeqMapView();
                            if (currentSeqMap != null) {
                                //get visible span
                                SeqSpan currentSpan = currentSeqMap.getVisibleSpan();
                                if (currentSpan != null && currentSpan.getBioSeq() != null) {
                                    //check genome version, if same then set coordinates								
                                    AnnotatedSeqGroup currentGroup = currentSpan.getBioSeq().getSeqGroup();
                                    if (!isGalaxyBookmark && (currentGroup != null && currentGroup.equals(bookMarkGroup))) {
                                        start = currentSpan.getStart();
                                        end = currentSpan.getEnd();
                                        seqid = currentSpan.getBioSeq().getID();
                                    } else {
                                        pickOne = true;
                                    }
                                } else {
                                    pickOne = true;
                                }
                            } //pick first chromosome and 1M span
                            else {
                                pickOne = true;
                            }
                        }
                        //pick something, only works if version was loaded.
                        if (pickOne & !isGalaxyBookmark) {
                            BioSeq bs = bookMarkGroup.getSeq(0);
                            if (bs != null) {
                                int len = bs.getLength();
                                seqid = bs.getID();
                                start = len / 3 - 500000;
                                if (start < 0) {
                                    start = 0;
                                }
                                end = start + 500000;
                                if (end > len) {
                                    end = len - 1;
                                }
                            }
                        }
                    }

                    //attempt to parse from bookmark?
                    if (start == 0 && end == 0) {
                        List<Integer> intValues = initializeIntValues(start_param, end_param, select_start_param, select_end_param);
                        start = intValues.get(0);
                        end = intValues.get(1);
                    }

                    List<String> server_urls = parameters.get(Bookmark.SERVER_URL);
                    List<String> query_urls = parameters.get(Bookmark.QUERY_URL);
                    List<GenericServer> gServers = null;
                    if (server_urls.isEmpty() || query_urls.isEmpty() || server_urls.size() != query_urls.size()) {
                        loaddata = false;
                    } else {
                        gServers = loadServers(igbService, server_urls);
                    }

                    List<String> das2_query_urls = parameters.get(Bookmark.DAS2_QUERY_URL);
                    List<String> das2_server_urls = parameters.get(Bookmark.DAS2_SERVER_URL);

                    List<GenericServer> gServers2 = null;

                    if (das2_server_urls.isEmpty() || das2_query_urls.isEmpty() || das2_server_urls.size() != das2_query_urls.size()) {
                        loaddas2data = false;
                    } else {
                        gServers2 = loadServers(igbService, das2_server_urls);
                    }

                    final BioSeq seq = goToBookmark(igbService, seqid, version, start, end).orNull();

                    if (seq == null) {
                        if (isGalaxyBookmark) {
                            loadUnknownData(parameters);
                            return null;
                        } else if (loaddata) {
                            AnnotatedSeqGroup seqGroup = gmodel.getSelectedSeqGroup();
                            if (seqGroup == null || !seqGroup.isSynonymous(version)) {
                                seqGroup = gmodel.addSeqGroup(version);
                            }
                            loadChromosomesFor(igbService, seqGroup, gServers, query_urls);
                        }
                        return null; /* user cancelled the change of genome, or something like that */

                    }

                    if (loaddata) {
                        // TODO: Investigate edge case at max
                        if (seq.getMin() == start && seq.getMax() == end) {
                            end -= 1;
                        }

                        List<GenericFeature> gFeatures = loadData(igbService, gmodel.getSelectedSeqGroup(), gServers, query_urls, start, end);

                        if (has_properties) {
                            List<String> graph_urls = getGraphUrls(parameters);
                            final Map<String, ITrackStyleExtended> combos = new HashMap<>();

                            for (int i = 0; !parameters.get(SYM.FEATURE_URL.toString() + i).isEmpty(); i++) {
                                String combo_name = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(parameters, Bookmark.GRAPH.COMBO.toString() + i);
                                if (combo_name != null) {
                                    ITrackStyleExtended combo_style = combos.get(combo_name);
                                    if (combo_style == null) {
                                        combo_style = new SimpleTrackStyle("Joined Graphs", true);
                                        combo_style.setTrackName("Joined Graphs");
                                        combo_style.setExpandable(true);
                                        //combo_style.setCollapsed(true);
                                        //combo_style.setLabelBackground(igbService.getDefaultBackgroundColor());
                                        combo_style.setBackground(igbService.getDefaultBackgroundColor());
                                        //combo_style.setLabelForeground(igbService.getDefaultForegroundColor());	
                                        combo_style.setForeground(igbService.getDefaultForegroundColor());
                                        combo_style.setTrackNameSize(igbService.getDefaultTrackSize());
                                        combos.put(combo_name, combo_style);
                                    }
                                }
                            }

                            for (final GenericFeature feature : gFeatures) {
                                if (feature != null && graph_urls.contains(feature.getURI().toString())) {
                                    ThreadUtils.getPrimaryExecutor(feature).execute(new Runnable() {

                                        @Override
                                        public void run() {
                                            BookmarkController.applyProperties(igbService, seq, parameters, feature, combos);
                                        }
                                    });
                                }
                            }
                        }
                    }

                    if (loaddas2data) {
                        loadOldBookmarks(igbService, gServers2, das2_query_urls, start, end);
                    }

                    //loadDataFromDas2(uni, das2_server_urls, das2_query_urls);
                    //String[] data_urls = parameters.get(Bookmark.DATA_URL);
                    //String[] url_file_extensions = parameters.get(Bookmark.DATA_URL_FILE_EXTENSIONS);
                    //loadDataFromURLs(uni, data_urls, url_file_extensions, null);
                    String selectParam = getFirstValueEntry(parameters, "select");
                    if (selectParam != null) {
                        igbService.performSelection(selectParam);
                    }

                    if (loadResidue) {
                        BioSeq vseq = GenometryModel.getInstance().getSelectedSeq();
                        SeqSpan span = new SimpleMutableSeqSpan(start, end, vseq);
                        igbService.loadResidues(span, true);
                    }
                } catch (Throwable t) {
                    //Catch all to ensure thread does not continue indefinitely
                    logger.error("Error while loading bookmark.", t);
                    return null;
                }

                return null;
            }

            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance().execute(parameters, worker);
    }

    /**
     * Checks for nulls or Strings with zero length.
     *
     * @param params
     * @return
     */
    public static boolean missingString(String[] params) {
        for (String s : params) {
            if (StringUtils.isBlank(s)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getGraphUrls(ListMultimap<String, String> multimap) {
        List<String> graph_paths = new ArrayList<>();
        for (int i = 0; !multimap.get(SYM.FEATURE_URL.toString() + i).isEmpty(); i++) {
            graph_paths.add(getFirstValueEntry(multimap, SYM.FEATURE_URL.toString() + i));
        }
        return graph_paths;
    }

    private void loadOldBookmarks(final IGBService igbService, List<GenericServer> gServers, List<String> das2_query_urls, int start, int end) {
        List<String> opaque_requests = new ArrayList<>();
        int i = 0;
        for (String url : das2_query_urls) {
            String das2_query_url = GeneralUtils.URLDecode(url);
            String seg_uri = null;
            String type_uri = null;
            String overstr = null;
            String format = null;
            boolean use_optimizer = true;
            int qindex = das2_query_url.indexOf('?');
            if (qindex > -1) {
                String query = das2_query_url.substring(qindex + 1);
                String[] query_array = query_splitter.split(query);
                for (int k = -0; k < query_array.length; k++) {
                    String tagval = query_array[k];
                    int eqindex = tagval.indexOf('=');
                    String tag = tagval.substring(0, eqindex);
                    String val = tagval.substring(eqindex + 1);
                    if (tag.equals("format") && (format == null)) {
                        format = val;
                    } else if (tag.equals("type") && (type_uri == null)) {
                        type_uri = val;
                    } else if (tag.equals("segment") && (seg_uri == null)) {
                        seg_uri = val;
                    } else if (tag.equals("overlaps") && (overstr == null)) {
                        overstr = val;
                    } else {
                        use_optimizer = false;
                        break;
                    }
                }
                if (type_uri == null || seg_uri == null || overstr == null) {
                    use_optimizer = false;
                }
            } else {
                use_optimizer = false;
            }
            //
            // only using optimizer if query has 1 segment, 1 overlaps, 1 type, 0 or 1 format, no other params
            // otherwise treat like any other opaque data url via loadDataFromURLs call
            //
            if (!use_optimizer) {
                opaque_requests.add(das2_query_url);
                continue;
            }

            GenericFeature feature = getFeature(igbService, gmodel.getSelectedSeqGroup(), gServers.get(i), type_uri);
            if (feature != null) {
                loadFeature(igbService, feature, start, end);
            }
            i++;
        }

        if (!opaque_requests.isEmpty()) {
            String[] data_urls = new String[opaque_requests.size()];
            for (int r = 0; r < opaque_requests.size(); r++) {
                data_urls[r] = opaque_requests.get(r);
            }
            loadDataFromURLs(igbService, data_urls, null, null);
        }
    }

    private List<GenericFeature> loadData(final IGBService igbService, final AnnotatedSeqGroup seqGroup, final List<GenericServer> gServers, final List<String> query_urls, int start, int end) {
        BioSeq seq = GenometryModel.getInstance().getSelectedSeq();
        List<GenericFeature> gFeatures = new ArrayList<>();
        int i = 0;
        for (String queryUrl : query_urls) {
            gFeatures.add(getFeature(igbService, seqGroup, gServers.get(i), queryUrl));
            i++;
        }
        boolean show_message = false;
        for (GenericFeature gFeature : gFeatures) {
            if (gFeature != null) {
                loadFeature(igbService, gFeature, start, end);
                if (!show_message && (gFeature.getLoadStrategy() == LoadStrategy.VISIBLE)) {
                    gFeature.setVisible();
                    show_message = true;
                }
            }
        }
        igbService.updateGeneralLoadView();

        if (show_message) {
            igbService.infoPanel(GenericFeature.LOAD_WARNING_MESSAGE,
                    GenericFeature.show_how_to_load, GenericFeature.default_show_how_to_load);
        }
        return gFeatures;
    }

    private void loadChromosomesFor(final IGBService igbService, final AnnotatedSeqGroup seqGroup, final List<GenericServer> gServers, final List<String> query_urls) {
        List<GenericFeature> gFeatures = getFeatures(igbService, seqGroup, gServers, query_urls);
        for (GenericFeature gFeature : gFeatures) {
            if (gFeature != null) {
                igbService.loadChromosomes(gFeature);
            }
        }
    }

    private List<GenericFeature> getFeatures(final IGBService igbService, final AnnotatedSeqGroup seqGroup, final List<GenericServer> gServers, final List<String> query_urls) {
        List<GenericFeature> gFeatures = new ArrayList<>();

        boolean show_message = false;
        int i = 0;
        for (String query_url : query_urls) {
            GenericFeature gFeature = getFeature(igbService, seqGroup, gServers.get(i), query_url);
            gFeatures.add(gFeature);
            if (gFeature != null) {
                gFeature.setVisible();
                gFeature.setPreferredLoadStrategy(LoadStrategy.VISIBLE);
                if (gFeature.getLoadStrategy() == LoadStrategy.VISIBLE /*||
                         gFeatures[i].getLoadStrategy() == LoadStrategy.CHROMOSOME*/) {
                    show_message = true;
                }
            }
            i++;
        }

        igbService.updateGeneralLoadView();

        // Show message on how to load
        if (show_message) {
            igbService.infoPanel(GenericFeature.LOAD_WARNING_MESSAGE,
                    GenericFeature.show_how_to_load, GenericFeature.default_show_how_to_load);
        }

        return gFeatures;
    }

    private GenericFeature getFeature(final IGBService igbService, final AnnotatedSeqGroup seqGroup, final GenericServer gServer, final String query_url) {

        if (gServer == null) {
            return null;
        }

        // If server requires authentication then.
        // If it cannot be authenticated then don't add the feature.
        // This method of authentication does not work for Das2
        //if (!LocalUrlCacher.isValidURL(query_url)) {
        //	return null;
        //}
        GenericFeature feature = igbService.getFeature(seqGroup, gServer, query_url, false);

        if (feature == null) {
            Logger.getLogger(GeneralUtils.class.getName()).log(
                    Level.SEVERE, "Couldn''t find feature for bookmark url {}", query_url);
        }

        return feature;
    }

    private void loadFeature(IGBService igbService, GenericFeature gFeature, int start, int end) {
        BioSeq seq = GenometryModel.getInstance().getSelectedSeq();
        //a bit of a hack to force track creation since with no overlap there is currently no track being created.
        if (end == 0) {
            end = 1;
        }
        SeqSpan overlap = new SimpleSeqSpan(start, end, seq);
        gFeature.setVisible();
        gFeature.setPreferredLoadStrategy(LoadStrategy.VISIBLE);
        if (gFeature.getLoadStrategy() != LoadStrategy.VISIBLE) {
            overlap = new SimpleSeqSpan(seq.getMin(), seq.getMax(), seq);
        }
        igbService.loadAndDisplaySpan(overlap, gFeature);
    }

    private List<GenericServer> loadServers(IGBService igbService, List<String> server_urls) {
        final ImmutableList.Builder<GenericServer> builder = ImmutableList.<GenericServer>builder();

        for (String server_url : server_urls) {
            builder.add(igbService.loadServer(server_url));
        }

        return builder.build();
    }

    private void loadDataFromURLs(final IGBService igbService, final String[] data_urls, final String[] extensions, final String[] tier_names) {
        try {
            if (data_urls != null && data_urls.length != 0) {
                URL[] urls = new URL[data_urls.length];
                for (int i = 0; i < data_urls.length; i++) {
                    urls[i] = new URL(data_urls[i]);
                }
                final UrlLoaderThread t = new UrlLoaderThread(igbService, urls, extensions, tier_names);
                t.runEventually();
                t.join();
            }
        } catch (MalformedURLException e) {
            ErrorHandler.errorPanel("Error loading bookmark\nData URL malformed\n", e);
        } catch (InterruptedException ex) {
        }
    }

    private List<Integer> initializeIntValues(String start_param, String end_param,
            String select_start_param, String select_end_param) {

        Integer start = 0;
        Integer end = 0;
        if (StringUtils.isNotBlank(start_param)) {
            start = Ints.tryParse(start_param);
            if (start == null) {
                start = 0;
            }
        }
        if (StringUtils.isNotBlank(end_param)) {
            end = Ints.tryParse(end_param);
            if (end == null) {
                end = 0;
            }
        }
        Integer selstart = -1;
        Integer selend = -1;
        if (StringUtils.isNotBlank(select_start_param) && StringUtils.isNotBlank(select_end_param)) {
            selstart = Ints.tryParse(select_start_param);
            selend = Ints.tryParse(select_end_param);
            if (selstart == null) {
                selstart = -1;
            }
            if (selend == null) {
                selend = -1;
            }
        }
        ImmutableList<Integer> intValues = ImmutableList.<Integer>builder().add(start).add(end).add(selstart).add(selend).build();
        return intValues;
    }

    /**
     * Loads the sequence and goes to the specified location. If version doesn't
     * match the currently-loaded version, asks the user if it is ok to proceed.
     * NOTE: This schedules events on the AWT event queue. If you want to make
     * sure that everything has finished before you do something else, then you
     * have to schedule that something else to occur on the AWT event queue.
     *
     * @param graph_files it is ok for this parameter to be null.
     * @return true indicates that the action succeeded
     */
    private Optional<BioSeq> goToBookmark(IGBService igbService, String seqid, String version, int start, int end) {
        AnnotatedSeqGroup book_group = null;
        try {
            book_group = igbService.determineAndSetGroup(version).orNull();
        } catch (Throwable ex) {
            logger.error("info", ex);
        }

        if (book_group == null) {
            return Optional.fromNullable(null);
        }

        final BioSeq book_seq = determineSeq(seqid, book_group);
        if (book_seq == null) {
            ErrorHandler.errorPanel("No seqid", "The bookmark did not specify a valid seqid: specified '" + seqid + "'");
            return null;
        } else {
            // gmodel.setSelectedSeq() should trigger a gviewer.setAnnotatedSeq() since
            //     gviewer is registered as a SeqSelectionListener on gmodel
            if (book_seq != gmodel.getSelectedSeq()) {
                gmodel.setSelectedSeq(book_seq);
            }
        }
        igbService.getSeqMapView().setRegion(start, end, book_seq);
        return Optional.fromNullable(book_seq);
    }

    public static BioSeq determineSeq(String seqid, AnnotatedSeqGroup group) {
        // hopefully setting gmodel's selected seq group above triggered population of seqs
        //   for group if not already populated
        BioSeq book_seq;
        if (StringUtils.isBlank(seqid) || "unknown".equals(seqid)) {
            book_seq = gmodel.getSelectedSeq();
            if (book_seq == null && gmodel.getSelectedSeqGroup() != null && gmodel.getSelectedSeqGroup().getSeqCount() > 0) {
                book_seq = gmodel.getSelectedSeqGroup().getSeq(0);
            }
        } else {
            book_seq = group.getSeq(seqid);
        }
        return book_seq;
    }

    String getFirstValueEntry(ListMultimap<String, String> multimap, String key) {
        if (multimap.get(key).isEmpty()) {
            return null;
        }
        return multimap.get(key).get(0);
    }

    private void loadUnknownData(final ListMultimap<String, String> parameters) {
        List<String> query_urls = parameters.get(Bookmark.QUERY_URL);
        //These bookmarks should only contain one url
        if (!query_urls.isEmpty()) {
            try {
                String urlToLoad = query_urls.get(0);
                AnnotatedSeqGroup loadGroup = OpenURIAction.retrieveSeqGroup("Custom Genome");
                LoadURLAction.getAction().openURI(new URI(urlToLoad), urlToLoad, false, loadGroup, "Custom Species", false);
            } catch (URISyntaxException ex) {
                logger.error("Invalid bookmark syntax.", ex);
            }
        }
    }
}
