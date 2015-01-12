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
import com.affymetrix.genometryImpl.util.BioSeqUtils;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.Delegate;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.igb.bookmarks.Bookmark.GRAPH;
import com.affymetrix.igb.bookmarks.Bookmark.SYM;
import com.affymetrix.igb.osgi.service.IGBService;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows creation of bookmarks based on a SeqSymmetry, and viewing of a
 * bookmark.
 *
 * @version $Id: BookmarkController.java 7007 2010-10-11 14:26:55Z hiralv $
 */
public abstract class BookmarkController {
    
    private static final Logger logger = LoggerFactory.getLogger(BookmarkController.class);
    public static final String DEFAULT_BOOKMARK_NAME_FORMAT = "{0}, {1} : {2} - {3}";
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    /**
     * Causes a bookmark to be executed. If this is a Unibrow bookmark, it will
     * be opened in the viewer using
     * {@link BookmarkUnibrowControlServlet.getInstance()#goToBookmark}. If it
     * is an external bookmark, it will be opened in an external browser.
     */
    public static void viewBookmark(IGBService igbService, Bookmark bm) {
        logger.debug("Loading bookmark");
        if (bm.isValidBookmarkFormat()) {
            try {
                ListMultimap<String, String> props = Bookmark.parseParameters(bm.getURL());
                BookmarkUnibrowControlServlet.getInstance().goToBookmark(igbService, props, false);
            } catch (Exception e) {
                String message = e.getClass().getName() + ": " + e.getMessage();
                ErrorHandler.errorPanel("Error opening bookmark.\n" + message);
            }
        } else {
            GeneralUtils.browse(bm.getURL().toExternalForm());
        }
    }

    public static void applyProperties(IGBService igbService, final BioSeq seq, final ListMultimap<String, String> map, final GenericFeature gFeature, Map<String, ITrackStyleExtended> combos) {
        double default_ypos = GraphState.DEFAULT_YPOS;
        double default_yheight = GraphState.DEFAULT_YHEIGHT;
        Color defaultForegroundColor = Color.decode("0x0247FE");
        Color defaultBackgroundColor = Color.WHITE;
        boolean default_float = GraphState.DEFAULT_FLOAT;
        boolean default_show_label = GraphState.DEFAULT_SHOW_LABEL;
        boolean default_show_axis = GraphState.DEFAULT_SHOW_AXIS;
        double default_minvis = GraphState.DEFAULT_MINVIS;
        double default_maxvis = GraphState.DEFAULT_MAXVIS;
        double default_score_thresh = GraphState.DEFAULT_SCORE_THRESH;
        int default_minrun_thresh = GraphState.DEFAULT_MINRUN_THRESH;
        int default_maxgap_thresh = GraphState.DEFAULT_MAXGAP_THRESH;
        boolean default_show_thresh = GraphState.DEFAULT_SHOW_THRESH;
        int default_thresh_direction = GraphState.THRESHOLD_DIRECTION_GREATER;

        try {
            for (int i = 0; !map.get(SYM.FEATURE_URL.toString() + i).isEmpty(); i++) {
                String feature_path = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, SYM.FEATURE_URL.toString() + i);

                if (gFeature == null || !feature_path.equals(gFeature.getURI().toString())) {
                    continue;
                }

                String method = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, SYM.METHOD.toString() + i);

                SeqSymmetry sym = seq.getAnnotation(method);

                if (sym == null) {
                    continue;
                }

                if (!(sym instanceof GraphSym) && !(sym instanceof TypeContainerAnnot)) {
                    continue;
                }

                // for some parameters, testing more than one parameter name because how some params used to have
                //    slightly different names, and we need to support legacy bookmarks
                String sym_name = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, SYM.NAME.toString() + i);
                String sym_ypos = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, SYM.YPOS.toString() + i);
                String sym_height = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, SYM.YHEIGHT.toString() + i);

                // sym_col is String rep of RGB integer
                String sym_col = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, SYM.COL.toString() + i);
                String sym_bg_col = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, SYM.BG.toString() + i);
				// sym_bg_col will often be null

                //        int graph_min = (graph_visible_min == null) ?
                String graph_style = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.STYLE.toString() + i);
                String heatmap_name = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.HEATMAP.toString() + i);

                String combo_name = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.COMBO.toString() + i);

                double ypos = (sym_ypos == null) ? default_ypos : Double.parseDouble(sym_ypos);
                double yheight = (sym_height == null) ? default_yheight : Double.parseDouble(sym_height);
                Color foregroundColor = defaultForegroundColor;
                Color backgroundColor = defaultBackgroundColor;
                if (sym_col != null) {
                    try {
                        // Color.decode() can handle colors in plain integer format
                        // as well as hex format: "-20561" == "#FFAFAF" == "0xFFAFAF" == "16756655"
                        // We now write in the hex format, but can still read the older int format.
                        foregroundColor = Color.decode(sym_col);
                    } catch (NumberFormatException nfe) {
                        ErrorHandler.errorPanel("Couldn't parse graph color from '" + sym_col + "'\n"
                                + "Please use a hexidecimal RGB format,\n e.g. red = '0xFF0000', blue = '0x0000FF'.");
                    }
                }
                if (sym_bg_col != null) {
                    try {
                        backgroundColor = Color.decode(sym_bg_col);
                    } catch (NumberFormatException nfe) {
                        ErrorHandler.errorPanel("Couldn't parse graph background color from '" + sym_bg_col + "'\n"
                                + "Please use a hexidecimal RGB format,\n e.g. red = '0xFF0000', blue = '0x0000FF'.");
                    }
                }

                if (StringUtils.isBlank(sym_name)) {
                    sym_name = feature_path;
                }

                ITrackStyleExtended style = null;

                if (sym instanceof GraphSym) {
                    String graph_float = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.FLOAT.toString() + i);
                    String show_labelstr = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.SHOW_LABEL.toString() + i);
                    String show_axisstr = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.SHOW_AXIS.toString() + i);
                    String minvis_str = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.MINVIS.toString() + i);
                    String maxvis_str = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.MAXVIS.toString() + i);
                    String score_threshstr = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.SCORE_THRESH.toString() + i);
                    String maxgap_threshstr = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.MAXGAP_THRESH.toString() + i);
                    String minrun_threshstr = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.MINRUN_THRESH.toString() + i);
                    String show_threshstr = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.SHOW_THRESH.toString() + i);
                    String thresh_directionstr = BookmarkUnibrowControlServlet.getInstance().getFirstValueEntry(map, GRAPH.THRESH_DIRECTION.toString() + i);

                    boolean use_floating_graphs
                            = (graph_float == null) ? default_float : (graph_float.equals("true"));
                    boolean show_label
                            = (show_labelstr == null) ? default_show_label : (show_labelstr.equals("true"));
                    boolean show_axis
                            = (show_axisstr == null) ? default_show_axis : (show_axisstr.equals("true"));
                    double minvis = (minvis_str == null) ? default_minvis : Double.parseDouble(minvis_str);
                    double maxvis = (maxvis_str == null) ? default_maxvis : Double.parseDouble(maxvis_str);
                    double score_thresh
                            = (score_threshstr == null) ? default_score_thresh : Double.parseDouble(score_threshstr);
                    int maxgap_thresh
                            = (maxgap_threshstr == null) ? default_maxgap_thresh : Integer.parseInt(maxgap_threshstr);

                    int minrun_thresh
                            = (minrun_threshstr == null) ? default_minrun_thresh : Integer.parseInt(minrun_threshstr);
                    boolean show_thresh
                            = (show_threshstr == null) ? default_show_thresh : (show_threshstr.equals("true"));
                    int thresh_direction
                            = (thresh_directionstr == null) ? default_thresh_direction : Integer.parseInt(thresh_directionstr);

                    GraphState gstate = ((GraphSym) sym).getGraphState();
                    style = gstate.getTierStyle();
                    GenericFeature feature = style.getFeature();

                    if (!gFeature.equals(feature)) {
                        continue;
                    }

                    GraphType graph_style_num = null;
                    if (graph_style != null) {
                        graph_style_num = GraphState.getStyleNumber(graph_style);
                    }

                    ((GraphSym) sym).setGraphName(sym_name);

                    applyGraphProperties(igbService, gstate, graph_style_num, heatmap_name, use_floating_graphs,
                            show_label, show_axis, minvis, maxvis, score_thresh, minrun_thresh,
                            maxgap_thresh, show_thresh, thresh_direction, combo_name, combos);

                } else {
                    style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(BioSeqUtils.determineMethod(sym));
                    GenericFeature feature = style.getFeature();

                    if (!gFeature.equals(feature)) {
                        continue;
                    }
                    style.setTrackName(sym_name);
                }

                applyStyleProperties(style, foregroundColor, backgroundColor, ypos, yheight);

            }

        } catch (Exception ex) {
            ErrorHandler.errorPanel("Error while applying symmetry properties", ex, Level.WARNING);
        } catch (Error er) {
            ErrorHandler.errorPanel("Error while applying symmetry properties", er, Level.WARNING);
        }
    }

    private static void applyStyleProperties(ITrackStyle tier_style, Color col, Color bg_col, double ypos, double yheight) {
        tier_style.setForeground(col);
        tier_style.setBackground(bg_col);
        tier_style.setY(ypos);
        tier_style.setHeight(yheight);
    }

    private static void applyGraphProperties(IGBService igbService, GraphState gstate, GraphType graph_style_num, String heatmap_name,
            boolean use_floating_graphs, boolean show_label, boolean show_axis, double minvis, double maxvis,
            double score_thresh, int minrun_thresh, int maxgap_thresh, boolean show_thresh, int thresh_direction,
            String combo_name, Map<String, ITrackStyleExtended> combos) {

        if (graph_style_num != null) {
            gstate.setGraphStyle(graph_style_num);
        }
        if (heatmap_name != null) {
            HeatMap heat_map = HeatMap.getStandardHeatMap(heatmap_name);
            if (heat_map != null) {
                gstate.setHeatMap(heat_map);
            }
        }

        gstate.getTierStyle().setFloatTier(use_floating_graphs);
        gstate.setShowLabel(show_label);
        gstate.setShowAxis(show_axis);
        gstate.setVisibleMinY((float) minvis);
        gstate.setVisibleMaxY((float) maxvis);
        gstate.setMinScoreThreshold((float) score_thresh);
        gstate.setMinRunThreshold(minrun_thresh);
        gstate.setMaxGapThreshold(maxgap_thresh);
        gstate.setShowThreshold(show_thresh);
        gstate.setThresholdDirection(thresh_direction);
        if (combo_name != null) {
            ITrackStyleExtended combo_style = combos.get(combo_name);
            if (combo_style != null) {
                gstate.setComboStyle(combo_style, 0);
                gstate.getTierStyle().setJoin(true);
            }
        }
    }

    public static void addSymmetries(Bookmarks bookmark) {
        BioSeq seq = GenometryModel.getInstance().getSelectedSeq();
        if (seq != null) {
            AnnotatedSeqGroup group = seq.getSeqGroup();

            for (GenericVersion version : group.getEnabledVersions()) {
                for (GenericFeature feature : version.getFeatures()) {
                    if (feature.getLoadStrategy() != LoadStrategy.NO_LOAD
                            && !Delegate.EXT.equals(feature.getExtension())) {
                        bookmark.add(feature, false);
                    }
                }
            }
        }
    }

    public static void addProperties(SymWithProps mark_sym) {
        BioSeq seq = GenometryModel.getInstance().getSelectedSeq();

        Map<ITrackStyle, Integer> combo_styles = new HashMap<>();

        // Holds a list of labels of graphs for which no url could be found.
        Set<String> unfound_labels = new LinkedHashSet<>();

        // "j" loops throug all graphs, while "i" counts only the ones
        // that are actually book-markable (thus i <= j)
        int i = -1;

        for (int j = 0; j < seq.getAnnotationCount(); j++) {

            SeqSymmetry sym = seq.getAnnotation(j);

            if (!(sym instanceof GraphSym) && !(sym instanceof TypeContainerAnnot)) {
                continue;
            }

            i++;

            if (sym instanceof TypeContainerAnnot) {
                TypeContainerAnnot tca = (TypeContainerAnnot) sym;
                ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(tca.getType());
                GenericFeature feature = style.getFeature();

                if (feature == null) {
                    unfound_labels.add(tca.getType());
                    continue;
                }

                addStyleProps(style, mark_sym, feature.getURI().toString(), style.getTrackName(), tca.getType(), i);
                continue;
            }

            GraphSym graph = (GraphSym) sym;
            GraphState gstate = graph.getGraphState();
            ITrackStyleExtended style = gstate.getTierStyle();
            GenericFeature feature = style.getFeature();

            if (feature == null) {
                unfound_labels.add(sym.getID());
                continue;
            }

            addStyleProps(style, mark_sym, feature.getURI().toString(), graph.getGraphName(), graph.getID(), i);

            mark_sym.setProperty(GRAPH.SHOW_LABEL.toString() + i, (Boolean.toString(gstate.getShowLabel())));
            mark_sym.setProperty(GRAPH.SHOW_AXIS.toString() + i, (Boolean.toString(gstate.getShowAxis())));
            mark_sym.setProperty(GRAPH.MINVIS.toString() + i, Double.toString(gstate.getVisibleMinY()));
            mark_sym.setProperty(GRAPH.MAXVIS.toString() + i, Double.toString(gstate.getVisibleMaxY()));
            mark_sym.setProperty(GRAPH.SCORE_THRESH.toString() + i, Double.toString(gstate.getMinScoreThreshold()));
            mark_sym.setProperty(GRAPH.MAXGAP_THRESH.toString() + i, Integer.toString(gstate.getMaxGapThreshold()));
            mark_sym.setProperty(GRAPH.MINRUN_THRESH.toString() + i, Integer.toString(gstate.getMinRunThreshold()));
            mark_sym.setProperty(GRAPH.SHOW_THRESH.toString() + i, (Boolean.toString(gstate.getShowThreshold())));
            mark_sym.setProperty(GRAPH.STYLE.toString() + i, gstate.getGraphStyle().toString());
            mark_sym.setProperty(GRAPH.THRESH_DIRECTION.toString() + i, Integer.toString(gstate.getThresholdDirection()));
            if (gstate.getGraphStyle() == GraphType.HEAT_MAP && gstate.getHeatMap() != null) {
                mark_sym.setProperty(GRAPH.HEATMAP.toString() + i, gstate.getHeatMap().getName());
            }

            ITrackStyle combo_style = gstate.getComboStyle();
            if (combo_style != null) {
                Integer combo_style_num = combo_styles.get(combo_style);
                if (combo_style_num == null) {
                    combo_style_num = combo_styles.size() + 1;
                    combo_styles.put(combo_style, combo_style_num);
                }
                mark_sym.setProperty(GRAPH.COMBO.toString() + i, combo_style_num.toString());
            }

        }

        // TODO: Now save the colors and such of the combo graphs!
        if (!unfound_labels.isEmpty()) {
            ErrorHandler.errorPanel("WARNING: Cannot bookmark some graphs",
                    "Warning: could not bookmark some graphs.\n"
                    + "No source URL was available for: " + unfound_labels.toString(), Level.WARNING);
        }

    }

    private static void addStyleProps(ITrackStyleExtended style, SymWithProps mark_sym,
            String featureURI, String name, String method, int i) {

        mark_sym.setProperty(SYM.FEATURE_URL.toString() + i, featureURI);
        mark_sym.setProperty(SYM.METHOD.toString() + i, method);
        mark_sym.setProperty(SYM.YPOS.toString() + i, Integer.toString((int) style.getY()));
        mark_sym.setProperty(SYM.YHEIGHT.toString() + i, Integer.toString((int) style.getHeight()));
        mark_sym.setProperty(SYM.COL.toString() + i, sixDigitHex(style.getForeground()));
        mark_sym.setProperty(SYM.BG.toString() + i, sixDigitHex(style.getBackground()));
        mark_sym.setProperty(SYM.NAME.toString() + i, name);
    }

    /**
     * Creates a Map containing bookmark properties. All keys and values are
     * Strings. Assumes correct span is the first span in the sym.
     *
     * @param sym The symmetry to generate the bookmark properties from
     * @return a map of bookmark properties
     */
    private static ListMultimap<String, String> constructBookmarkProperties(SeqSymmetry sym) {
        SeqSpan span = sym.getSpan(0);
        BioSeq seq = span.getBioSeq();
        ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.<String, String>builder();
        builder.put(Bookmark.SEQID, seq.getID());
        builder.put(Bookmark.VERSION, seq.getSeqGroup().getID());
        builder.put(Bookmark.START, Integer.toString(span.getMin()));
        builder.put(Bookmark.END, Integer.toString(span.getMax()));
        return builder.build();
    }

    /**
     * Constructs a bookmark from a SeqSymmetry. Assumes correct span is the
     * first span in the sym. Passes through to makeBookmark(Map props, String
     * name).
     *
     * @param sym The symmetry to bookmark
     * @param name name of the bookmark
     * @return the bookmark for the symmetry
     * @throws MalformedURLException if the URL specifies an unknown protocol
     */
    public static Bookmark makeBookmark(SeqSymmetry sym, String name) throws MalformedURLException, UnsupportedEncodingException {
        ListMultimap<String, String> props = constructBookmarkProperties(sym);
        String url = Bookmark.constructURL(props);
        return new Bookmark(name, "", url);
    }

    /**
     * Returns a hexadecimal representation of the color with "0x" plus exactly
     * 6 digits. Example Color.BLUE -> "0x0000FF".
     */
    private static String sixDigitHex(Color c) {
        int i = c.getRGB() & 0xFFFFFF;
        String s = Integer.toHexString(i).toUpperCase();
        while (s.length() < 6) {
            s = "0" + s;
        }
        s = "0x" + s;
        return s;
    }

    /**
     * A simple extension of SimpleSymWithProps that uses a LinkedHashMap to
     * store the properties. This ensures the bookmark properties will be listed
     * in a predictable order.
     */
    private static class BookmarkSymmetry extends SimpleSymWithProps {

        public BookmarkSymmetry() {
            super();
            props = new LinkedHashMap<>();
        }
    }

    public static Bookmark getCurrentBookmark(boolean include_sym_and_props, SeqSpan span)
            throws MalformedURLException, UnsupportedEncodingException {
        BioSeq aseq = span.getBioSeq();
        if (aseq == null) {
            return null;
        }

        String version = aseq.getSeqGroup().getID();
        Date date = new Date();

        SimpleSymWithProps mark_sym = new BookmarkSymmetry();
        mark_sym.addSpan(span);

        String default_name = MessageFormat.format(DEFAULT_BOOKMARK_NAME_FORMAT, version, aseq.getID(), span.getMin(), span.getMax());
        mark_sym.setProperty(Bookmark.VERSION, version);
        mark_sym.setProperty(Bookmark.SEQID, aseq.getID());
        mark_sym.setProperty(Bookmark.START, span.getMin());
        mark_sym.setProperty(Bookmark.END, span.getMax());
        mark_sym.setProperty(Bookmark.LOADRESIDUES, Boolean.toString(aseq.isComplete()));
//		props.put(Bookmark.USER, new String[]{System.getProperty("user.name")});
        mark_sym.setProperty(Bookmark.CREATE, DATE_FORMAT.format(date));
        mark_sym.setProperty(Bookmark.MODIFIED, DATE_FORMAT.format(date));

        Bookmarks bookmarks = new Bookmarks();

        if (include_sym_and_props) {
            BookmarkController.addSymmetries(bookmarks);
            BookmarkController.addProperties(mark_sym);
        }
        bookmarks.set(mark_sym);

        String url = Bookmark.constructURL(convertSymPropMapToMultiMap(mark_sym.getProperties()));
        return new Bookmark(default_name, "", url);
    }

    private static ListMultimap<String, String> convertSymPropMapToMultiMap(Map<String, Object> symPropertyMap) {
        ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.<String, String>builder();
        for (Map.Entry<String, Object> entry : symPropertyMap.entrySet()) {
            String key = entry.getKey();
            if (entry.getValue() instanceof String) {
                builder.put(key, String.class.cast(entry.getValue()));
            } else if (entry.getValue() instanceof Integer) {
                Integer value = (Integer) entry.getValue();
                builder.put(key, value.toString());
            } else if (entry.getValue() instanceof List) {
                List<String> valueList = (List<String>) entry.getValue();
                for (String value : valueList) {
                    builder.put(key, value);
                }
            } else if (entry.getValue() instanceof String[]) {
                String[] valueList = (String[]) entry.getValue();
                for (String value : valueList) {
                    builder.put(key, value);
                }
            }
        }
        return builder.build();
    }

    public static boolean hasSymmetriesOrGraphs() {
        Bookmarks bookmarks = new Bookmarks();
        BookmarkController.addSymmetries(bookmarks);
        return !bookmarks.getSyms().isEmpty();
    }

    public static String getDefaultBookmarkName(SeqSpan span) {
        BioSeq aseq = span.getBioSeq();
        if (aseq == null) {
            return null;
        }
        return MessageFormat.format(DEFAULT_BOOKMARK_NAME_FORMAT, aseq.getSeqGroup().getID(), aseq.getID(), span.getMin(), span.getMax());
    }
}
