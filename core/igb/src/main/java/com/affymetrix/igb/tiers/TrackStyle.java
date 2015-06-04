package com.affymetrix.igb.tiers;

import com.affymetrix.genometry.color.ColorProviderI;
import com.affymetrix.genometry.filter.SymmetryFilterI;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.style.ITrackStyle;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.style.PropertyConstants;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.stylesheet.AssociationElement;
import com.affymetrix.igb.stylesheet.PropertyMap;
import com.affymetrix.igb.stylesheet.Stylesheet;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.util.ColorUtils;
import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.prefs.Preferences;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * When setting up a TrackStyle, want to prioritize: <ol type="A"> <li> Start
 * with default instance (from system style sheet?) <li> Modify with user-set
 * default parameters from default Preferences node. <li> Modify with
 * method-matching parameters from system style sheet. <li> Modify with user-set
 * method parameters from Preferences nodes. </ol> Not sure yet where style
 * sheets from DAS/2 servers fits in yet -- between B and C or between C and D?
 */
public class TrackStyle implements ITrackStyleExtended, TrackConstants, PropertyConstants {

    private static Preferences tiersRootNode = PreferenceUtils.getTopNode().node("tiers");
    public static final boolean DEBUG_NODE_PUTS = false;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TrackStyle.class);

    private static Color getColor(Map<String, ? extends Object> props, String key) {
        Color c = null;
        Object o = props.get(key);
        if ("".equals(o)) {
            // setting the value of color to "" means that you want to ignore the
            // color settings in any inherited context and revert to the default.
            return null;
        } else if (o instanceof Color) {
            c = (Color) o;
        } else if (o instanceof String) {
            c = ColorUtils.getColor((String) o);
        }
        return c;
    }

    public static synchronized boolean autoSaveUserStylesheet() {
        Stylesheet stylesheet = XmlStylesheetParser.getUserStylesheet();
        if (stylesheet == null) {
            logger.debug("No user stylesheet present.");
            return false;
        }

        java.io.File f = XmlStylesheetParser.getUserStylesheetFile();
        String filename = f.getAbsolutePath();
        java.io.FileWriter fw = null;
        java.io.BufferedWriter bw = null;
        try {
            logger.info("Saving user stylesheet to file {}", filename);
            File parent_dir = f.getParentFile();
            if (parent_dir != null) {
                parent_dir.mkdirs();
            }
            StringBuffer sb = new StringBuffer(1000);
            sb = stylesheet.appendXML("\t", sb);
            fw = new java.io.FileWriter(f);
            bw = new java.io.BufferedWriter(fw);
            bw.write(sb.toString());
            bw.flush();

            return true;
        } catch (java.io.FileNotFoundException fnfe) {
            logger.error("Could not auto-save user stylesheet to {}", filename);
        } catch (java.io.IOException ioe) {
            logger.error("Error while saving user stylesheet to {}", filename);
        } finally {
            GeneralUtils.safeClose(bw);
            GeneralUtils.safeClose(fw);
        }

        return false;
    }

    private boolean show = default_show;
    private boolean connected = default_connected;
    private boolean collapsed = default_collapsed;
    private boolean show2tracks = default_show2tracks;
    private boolean expandable = default_expandable;
    private int max_depth = default_max_depth;
    private Color foreground = default_foreground;
    private Color background = default_background;
    private Color labelForeground;
    private Color labelBackground;
    private String label_field = default_label_field;
    private DirectionType direction_type = DEFAULT_DIRECTION_TYPE;
    private int glyph_depth = default_glyphDepth;
    private double height = default_height;
    private double y = default_y;
    private Color start_color = default_start;
    private Color end_color = default_end;
    private boolean showResidueMask = default_showResidueMask;
    private boolean shadeBasedOnQualityScore = default_shadeBasedOnQualityScore;
    private String url = null;
    private String file_type = null;
    private String unique_name;
    private String track_name;
    private String original_track_name;
    final private String method_name;
    private Preferences node;
    private boolean is_graph = false;
    private float track_name_size = default_track_name_size;
    private Map<String, Object> transient_properties;
    private DataSet feature = null;
    private boolean customizable = true;
    public boolean customise = false;
    private int summaryThreshold;
    private boolean separable = true;
    private boolean join = false;
    // if float_graph, then graph should float above annotations in tiers
    // if !float_graph, then graph should be in its own tier
    private boolean float_graph = false;
    private ColorProviderI color_provider = null;
    private SymmetryFilterI filter = null;
    //Only appropriate for Bam/Sam files, but being placed here for now
    private boolean showAsPaired = false;
    /**
     * for height on the reverse strand. To help with track resizing.
     */
    private double reverseHeight = TrackConstants.default_height;
    /**
     * for maximum depth of stacked glyphs on the reverse strand. To help with
     * resizing.
     */
    private int reverseMaxDepth = 0;

    protected TrackStyle() {
        method_name = null;
    }

    public TrackStyle(PropertyMap props) {
        this();
        initFromPropertyMap(props);
    }

    /**
     * Creates an instance associated with a case-insensitive form of the unique
     * name.
     *
     * When setting up an AnnotStyle, want to prioritize:
     *
     * A) Start with default instance (from system stylesheet?) B) Modify with
     * user-set default parameters from default Preferences node C) Modify with
     * method-matching parameters from system stylesheet D) Modify with user-set
     * method parameters from Preferences nodes
     *
     * Not sure yet where stylesheets from DAS/2 servers fits in yet -- between
     * B/C or between C/D ?
     */
    TrackStyle(String unique_ame, String track_name, String file_type, TrackStyle template, Map<String, String> properties) {
        this.method_name = unique_ame;
        this.track_name = track_name; // this is the default human name, and is not lower case
        this.original_track_name = track_name;
        this.file_type = file_type;
        this.unique_name = unique_ame.toLowerCase();
        this.float_graph = false;

        if (this.unique_name.endsWith("/")) {
            this.unique_name = this.unique_name.substring(0, this.unique_name.length() - 1);
        }
        this.unique_name = multiple_slashes.matcher(this.unique_name).replaceAll("/");
        // transforming to shortened but unique name if name exceeds Preferences.MAX_NAME_LENGTH
        //   is now handled within PreferenceUtils.getSubnod() call

        initStyle(template, properties);

        try {
            node = PreferenceUtils.getSubnode(tiersRootNode, this.unique_name);
        } catch (Exception e) {
            // if there is a problem creating the node, continue with a non-persistent style.
            e.printStackTrace();
            node = null;
        }

        if (node != null) {
            initFromNode();
        }
    }

    public void restoreToDefault() {
        if (this.getFeature() != null) {
            initStyle(IGBStateProvider.getDefaultTrackStyle(), this.getFeature().getProperties());
        }
        this.setTrackName(original_track_name);

        if (node != null) {
            try {
                node.removeNode();
                node.flush();
                node = PreferenceUtils.getSubnode(tiersRootNode, this.unique_name);
            } catch (Exception ex) {
                logger.error(null, ex);
            }
        }
    }

    private void initStyle(TrackStyle template, Map<String, String> properties) {

        if (template != null) {
            // calling initFromTemplate should take care of A) and B)
            initFromTemplate(template);
        }

        // Hidden default file type pref. from system stylesheet
        Stylesheet stylesheet = XmlStylesheetParser.getSystemStylesheet();
        AssociationElement assel = stylesheet.getAssociationForFileType(file_type);
        if (assel != null) {
            PropertyMap props = assel.getPropertyMap();
            if (props != null) {
                initFromPropertyMap(props);
            }
        }

        // File defaults panel user stylesheet
        stylesheet = XmlStylesheetParser.getUserStylesheet();
        assel = stylesheet.getAssociationForFileType(file_type);
        if (assel != null) {
            PropertyMap props = assel.getPropertyMap();
            if (props != null) {
                initFromPropertyMap(props);
            }
        }

        // File added stylesheets
        stylesheet = XmlStylesheetParser.getAddedStylesheets();
        assel = stylesheet.getAssociationForFileType(file_type);
        if (assel != null) {
            PropertyMap props = assel.getPropertyMap();
            if (props != null) {
                initFromPropertyMap(props);
            }
        }

        // From server annots.xml
        if (properties != null) {
            initFromPropertyMap(properties);
        }

        // Saved settings ????
        assel = stylesheet.getAssociationForType(unique_name);
        if (assel == null) {
            assel = stylesheet.getAssociationForMethod(unique_name);
        }
        if (assel != null) {
            PropertyMap props = assel.getPropertyMap();
            if (props != null) {
                initFromPropertyMap(props);
            }
        }
    }

    // Copies properties from the given node, using the currently-loaded values as defaults.
    // generally call initFromTemplate before this.
    // Make sure to set human_name to some default before calling this.
    // Properties set this way do NOT get put in persistent storage.
    private void initFromNode() {
        if (logger.isDebugEnabled()) {
            System.out.println("    ----------- called AnnotStyle.initFromNode() for: " + unique_name);
        }
        track_name = (String) load(PREF_TRACK_NAME, this.track_name);
        show2tracks = (Boolean) load(PREF_SHOW2TRACKS, this.getSeparate());
        glyph_depth = (Integer) load(PREF_GLYPH_DEPTH, this.getGlyphDepth());
        connected = (Boolean) load(PREF_CONNECTED, this.getConnected());
        showAsPaired = (Boolean) load(PREF_SHOW_AS_PAIRED, this.isShowAsPaired());
        collapsed = (Boolean) load(PREF_COLLAPSED, this.getCollapsed());
        max_depth = (Integer) load(PREF_MAX_DEPTH, this.getMaxDepth());
        foreground = (Color) load(PREF_FOREGROUND, this.getForeground());
        background = (Color) load(PREF_BACKGROUND, this.getBackground());
        start_color = (Color) load(PREF_START_COLOR, this.getForwardColor());
        end_color = (Color) load(PREF_END_COLOR, this.getReverseColor());
        label_field = (String) load(PREF_LABEL_FIELD, this.getLabelField());
        track_name_size = (Float) load(PREF_TRACK_SIZE, this.getTrackNameSize());
        direction_type = DirectionType.valueFor((Integer) load(PREF_DIRECTION_TYPE, this.getDirectionType()));
        Color temp_fg = (Color) load(PREF_LABEL_FOREGROUND, this.getLabelForeground());
        Color temp_bg = (Color) load(PREF_LABEL_BACKGROUND, this.getLabelBackground());
        if (temp_fg != foreground) {
            labelForeground = temp_fg;
        }
        if (temp_bg != background) {
            labelBackground = temp_bg;
        }
    }

    public PropertyMap getProperties() {
        PropertyMap props = new PropertyMap();
        props.put(PROP_FOREGROUND, PreferenceUtils.getColorString(getForeground()));
        props.put(PROP_BACKGROUND, PreferenceUtils.getColorString(getBackground()));
        props.put(PROP_START_COLOR, PreferenceUtils.getColorString(getForwardColor()));
        props.put(PROP_END_COLOR, PreferenceUtils.getColorString(getReverseColor()));
        props.put(PROP_GLYPH_DEPTH, String.valueOf(getGlyphDepth()));
        props.put(PROP_LABEL_FIELD, getLabelField());
        props.put(PROP_MAX_DEPTH, String.valueOf(getMaxDepth()));
        props.put(PROP_CONNECTED, String.valueOf(getConnected()));
        props.put(PROP_SHOW_AS_PAIRED, String.valueOf(isShowAsPaired()));
        props.put(PROP_SEPARATE, String.valueOf(getSeparate()));
        props.put(PROP_SHOW, String.valueOf(getShow()));
        props.put(PROP_COLLAPSED, String.valueOf(getCollapsed()));
        props.put(PROP_FONT_SIZE, String.valueOf(getTrackNameSize()));
        props.put(PROP_DIRECTION_TYPE, String.valueOf(getDirectionName()));
        return props;
    }

    // Copies selected properties from a PropertyMap into this object, but does NOT persist
    // these copied values -- if values were persisted, then if PropertyMap changed between sessions,
    //      older values would override newer values since persisted nodes take precedence
    //    (only want to persists when user sets preferences in GUI)
    private void initFromPropertyMap(Map<String, ?> props) {

        if (logger.isDebugEnabled()) {
            logger.debug("    +++++ initializing AnnotStyle from PropertyMap: " + unique_name);
            logger.debug("             props: " + props);
        }

        Color col = getColor(props, PROP_COLOR);
        if (col == null) {
            col = getColor(props, PROP_FOREGROUND);
        }
        if (col != null) {
            this.setForeground(col);
        }
        col = getColor(props, PROP_BACKGROUND);
        if (col != null) {
            this.setBackground(col);
        }

        col = getColor(props, PROP_START_COLOR);
        if (col == null) {
            col = getColor(props, PROP_POSITIVE_STRAND);
        }
        if (col != null) {
            this.setForwardColor(col);
        }

        col = getColor(props, PROP_END_COLOR);
        if (col == null) {
            col = getColor(props, PROP_NEGATIVE_STRAND);
        }
        if (col != null) {
            this.setReverseColor(col);
        }

        String gdepth_string = (String) props.get(PROP_GLYPH_DEPTH);
        if (StringUtils.isNotBlank(gdepth_string)) {
            int prev_glyph_depth = glyph_depth;
            try {
                this.setGlyphDepth(Integer.parseInt(gdepth_string));
            } catch (Exception ex) {
                this.setGlyphDepth(prev_glyph_depth);
            }
        }

        String labfield = (String) props.get(PROP_LABEL_FIELD);
        if (StringUtils.isNotBlank(labfield)) {
            this.setLabelField(labfield);
        }

        String mdepth_string = (String) props.get(PROP_MAX_DEPTH);
        if (StringUtils.isNotBlank(mdepth_string)) {
            int prev_max_depth = max_depth;
            try {
                this.setMaxDepth(Integer.parseInt(mdepth_string));
            } catch (Exception ex) {
                this.setMaxDepth(prev_max_depth);
            }
        }

        String sepstring = (String) props.get(PROP_SEPARATE);
        if (StringUtils.isNotBlank(sepstring)) {
            if (sepstring.equalsIgnoreCase(FALSE)) {
                this.setSeparate(false);
            } else if (sepstring.equalsIgnoreCase(TRUE)) {
                this.setSeparate(true);
            }
        }

        String showAsPairedString = (String) props.get(PROP_SHOW_AS_PAIRED);
        if (StringUtils.isNotBlank(showAsPairedString)) {
            if (showAsPairedString.equalsIgnoreCase(FALSE)) {
                this.setShowAsPaired(false);
            } else if (showAsPairedString.equalsIgnoreCase(TRUE)) {
                this.setShowAsPaired(true);
            }
        }

        String showstring = (String) props.get(PROP_SHOW);
        if (StringUtils.isNotBlank(showstring)) {
            if (showstring.equalsIgnoreCase(FALSE)) {
                show = false;
            } else if (showstring.equalsIgnoreCase(TRUE)) {
                show = true;
            }
        }
        String collapstring = (String) props.get(PROP_COLLAPSED);
        if (StringUtils.isNotBlank(collapstring)) {
            if (collapstring.equalsIgnoreCase(FALSE)) {
                this.setCollapsed(false);
            } else if (collapstring.equalsIgnoreCase(TRUE)) {
                this.setCollapsed(true);
            }
        }
        String fontstring = (String) props.get(PROP_FONT_SIZE);
        if (StringUtils.isNotBlank(fontstring)) {
            float prev_font_size = track_name_size;
            try {
                this.setTrackNameSize(Float.parseFloat(fontstring));
            } catch (Exception ex) {
                this.setTrackNameSize(prev_font_size);
            }
        }
        String directionstring = (String) props.get(PROP_DIRECTION_TYPE);
        if (StringUtils.isNotBlank(directionstring)) {
            DirectionType prev_direction_type = direction_type;
            try {
                this.setDirectionType(DirectionType.valueFor(directionstring));
            } catch (Exception ex) {
                this.setDirectionType(prev_direction_type);
            }
        }
//		String color_by_rgb_string = (String) props.get(PROP_COLOR_BY_RGB);
//		if (color_by_rgb_string != null && !"".equals(color_by_rgb_string)){
//			if(color_by_rgb_string.equalsIgnoreCase(TRUE)){
//				this.color_provider = new RGB();
//			}
//		}
        String nameSizeString = (String) props.get(PROP_NAME_SIZE);
        if (StringUtils.isNotBlank(nameSizeString)) {
            float prev_font_size = track_name_size;
            try {
                this.setTrackNameSize(Float.parseFloat(nameSizeString));
            } catch (Exception ex) {
                this.setTrackNameSize(prev_font_size);
            }
        }
        String show2tracksString = (String) props.get(PROP_SHOW_2TRACK);
        if (StringUtils.isNotBlank(show2tracksString)) {
            if (show2tracksString.equalsIgnoreCase(FALSE)) {
                this.setSeparate(false);
            } else if (show2tracksString.equalsIgnoreCase(TRUE)) {
                this.setSeparate(true);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("    +++++++  done initializing from PropertyMap");
        }
        // height???
    }

    // Copies properties from the template into this object, but does NOT persist
    private void initFromTemplate(TrackStyle template) {
        this.setSeparate(template.getSeparate());
        this.setShow(template.getShow());
        this.setCollapsed(template.getCollapsed());
        this.setMaxDepth(template.getMaxDepth());  // max stacking of annotations
        this.setForeground(template.getForeground());
        this.setBackground(template.getBackground());
        this.setLabelField(template.getLabelField());
        this.setGlyphDepth(template.getGlyphDepth());  // depth of visible glyph tree
        this.setTrackNameSize(template.getTrackNameSize());
        this.setForwardColor(template.getForwardColor());
        this.setReverseColor(template.getReverseColor());
//		this.setViewMode(template.getViewMode());
        this.setDirectionType(template.getDirectionName());
        this.setShowAsPaired(template.isShowAsPaired());
        this.setLabelForeground(null);
    }

    // Returns the preferences node, or null if this is a non-persistent instance.
    private Preferences getNode() {
        return this.node;
    }

    /*
     * Gets an instance that can be used for holding default values. The default
     * instance is used as a template in creating new instances. (Although not
     * ALL properties of the default instance are used in this way.)
     */
    @Override
    public String getUniqueName() {
        return unique_name;
    }

    @Override
    public String getMethodName() {
        return method_name;
    }

    /**
     * Gets a name that may be shorter and more user-friendly than the unique
     * name. The human-readable name may contain upper- and lower-case
     * characters. The default is equivalent to the unique name.
     */
    @Override
    public String getTrackName() {
        if (track_name == null || track_name.trim().length() == 0) {
            track_name = unique_name;
        }
        return this.track_name;
    }

    @Override
    public void setTrackName(String track_name) {
        this.track_name = track_name;
        save(PREF_TRACK_NAME, track_name);
    }

    public void resetTrackName(String track_name) {
        track_name = (String) load(PREF_TRACK_NAME, track_name);
        this.track_name = track_name;
    }

    /**
     * Whether the tier is shown or hidden.
     */
    @Override
    public boolean getShow() {
        return show;
    }

    /**
     * Sets whether the tier is shown or hidden; this is a non-persistent
     * setting.
     */
    @Override
    public void setShow(boolean b) {
        this.show = b;
    }

    /**
     * Whether PLUS and MINUS strand should be in separate tiers.
     */
    //Show2Tracks
    @Override
    public boolean getSeparate() {
        return show2tracks;
    }

    @Override
    public void setSeparate(boolean b) {
        if (is_graph && b) {
            return;
        }
        this.show2tracks = b;
        save(PREF_SHOW2TRACKS, b);
    }

    /**
     * Whether tier is collapsed.
     */
    @Override
    public boolean getCollapsed() {
        return collapsed;
    }

    @Override
    public void setCollapsed(boolean b) {
        this.collapsed = b;
        save(PREF_COLLAPSED, b);
    }

    /**
     * Maximum number of rows of annotations for this tier (both directions).
     */
    @Override
    public int getMaxDepth() {
        return max_depth;
    }

    /**
     * Set the maximum number of rows of annotations for this tier (both
     * directions). Any attempt to set this less than zero will fail, the value
     * will be truncated to fit the range.
     *
     * @param max a non-negative number.
     */
    @Override
    public void setMaxDepth(int max) {
        if (max < 0) {
            max = 0;
        }
        this.setForwardMaxDepth(max);
        this.setReverseMaxDepth(max);
        save(PREF_MAX_DEPTH, max);
    }

    /**
     * The color of annotations in the tier.
     */
    @Override
    public Color getForeground() {
        return foreground;
    }

    @Override
    public void setForeground(Color c) {
        this.foreground = c;
        save(PREF_FOREGROUND, c);
    }

    /**
     * The color of the tier Background.
     */
    @Override
    public Color getBackground() {
        return background;
    }

    @Override
    public void setBackground(Color c) {
        this.background = c;
        save(PREF_BACKGROUND, c);
    }

    /**
     * The color of the start direction.
     */
    @Override
    public Color getForwardColor() {
        return start_color;
    }

    @Override
    public void setForwardColor(Color c) {
        this.start_color = c;
        save(PREF_START_COLOR, c);
    }

    /**
     * The color of the start direction.
     */
    @Override
    public Color getReverseColor() {
        return end_color;
    }

    @Override
    public void setReverseColor(Color c) {
        this.end_color = c;
        save(PREF_END_COLOR, c);
    }

    /**
     * Returns the field name from which the glyph labels should be taken. This
     * will never return null, but will return "" instead.
     */
    @Override
    public String getLabelField() {
        return label_field;
    }

    @Override
    public void setLabelField(String l) {
        if (l == null || l.trim().length() == 0) {
            l = "";
        }
        label_field = l;
        save(PREF_LABEL_FIELD, l);
    }

    public boolean getConnected() {
        return connected;
    }

    public void setConnected(boolean b) {
        connected = b;
        save(PREF_CONNECTED, b);
    }

    @Override
    public int getGlyphDepth() {
        return glyph_depth;
    }

    @Override
    public void setGlyphDepth(int i) {
        if (glyph_depth != i) {
            glyph_depth = i;
            save(PREF_GLYPH_DEPTH, i);
        }

        if (glyph_depth == 1) {
            setConnected(false);
        } else {
            setConnected(true);
        }
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public void setHeight(double h) {
        height = h;
        save(PREF_HEIGHT, h);
        this.reverseHeight = this.height;
    }

    @Override
    public float getTrackNameSize() {
        return track_name_size;
    }

    @Override
    public void setTrackNameSize(float font_size) {
        this.track_name_size = font_size;
        save(PREF_TRACK_SIZE, font_size);
    }

    @Override
    public void setFeature(DataSet f) {
        this.feature = f;
    }

    @Override
    public DataSet getFeature() {
        return this.feature;
    }

    public void setDirectionType(DirectionType type) {
        this.direction_type = type;
        save(PREF_DIRECTION_TYPE, type.ordinal());
    }

    @Override
    public int getDirectionType() {
        return direction_type.ordinal();
    }

    @Override
    public void setDirectionType(int ordinal) {
        direction_type = TrackConstants.DirectionType.values()[ordinal];
    }

    public DirectionType getDirectionName() {
        return direction_type;
    }

    /**
     * could be used to remember tier positions.
     */
    @Override
    public void setY(double y) {
        this.y = y;
    }

    /**
     * could be used to remember tier positions.
     */
    @Override
    public double getY() {
        return y;
    }

    /**
     * A non-persistent property. Usually set by UCSC browser "track" lines.
     */
    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * A non-persistent property. Usually set by UCSC browser "track" lines. Can
     * return null.
     */
    @Override
    public String getUrl() {
        return this.url;
    }

    @Override
    public boolean getExpandable() {
        return expandable;
    }

    @Override
    public void setExpandable(boolean b) {
        // currently there is no need to make this property persistent.
        // there is rarly any reason to change it from the defualt value for
        // annotation tiers, only for graph tiers, which don't use this class
        expandable = b;
    }

    /**
     * Returns false by default. This class is only intended for annotation
     * tiers, not graph tiers.
     */
    @Override
    public boolean isGraphTier() {
        return is_graph;
    }

    /**
     * Avoid setting to anything but false. This class is only intended for
     * annotation tiers, not graph tiers.
     */
    @Override
    public void setGraphTier(boolean b) {
        is_graph = b;
    }

    public Map<String, Object> getTransientPropertyMap() {
        if (transient_properties == null) {
            transient_properties = new HashMap<>();
        }
        return transient_properties;
    }

    @Override
    public void setColorProvider(ColorProviderI cp) {
        this.color_provider = cp;
    }

    @Override
    public ColorProviderI getColorProvider() {
        return color_provider;
    }

    @Override
    public void setFilter(SymmetryFilterI filter) {
        this.filter = filter;
    }

    @Override
    public SymmetryFilterI getFilter() {
        return filter;
    }

    @Override
    public boolean isShowAsPaired() {
        return showAsPaired;
    }

    @Override
    public void setShowAsPaired(boolean showAsPaired) {
        this.showAsPaired = showAsPaired;
        save(PREF_SHOW_AS_PAIRED, showAsPaired);
        if (showAsPaired) {
            setSeparate(false);
            setDirectionType(TrackConstants.DirectionType.BOTH);
            setColorProvider(null);
        }
    }

    public boolean drawCollapseControl() {
        return (IGBStateProvider.getDrawCollapseState() && getExpandable());
    }

    public void copyPropertiesFrom(ITrackStyle g) {
        setForeground(g.getForeground());
        setShow(g.getShow());
        setTrackName(g.getTrackName());
        setBackground(g.getBackground());
        setCollapsed(g.getCollapsed());
        setMaxDepth(g.getMaxDepth());
        setHeight(g.getHeight());
        setY(g.getY());
        setExpandable(g.getExpandable());
        //setFeature(g.getFeature());

        if (g instanceof ITrackStyleExtended) {
            ITrackStyleExtended as = (ITrackStyleExtended) g;
            setGlyphDepth(as.getGlyphDepth());
            setLabelField(as.getLabelField());
            setSeparate(as.getSeparate());
            setSummaryThreshold(as.getSummaryThreshold());
            setReverseMaxDepth(as.getReverseMaxDepth());
        }

        if (g instanceof TrackStyle) {
            TrackStyle as = (TrackStyle) g;
            customizable = as.getCustomizable();
        }
        getTransientPropertyMap().putAll(g.getTransientPropertyMap());
    }

    public boolean getSeparable() {
        return separable;
    }

    public void setSeparable(boolean b) {
        separable = b;
    }

    @Override
    public String toString() {
        String s = "AnnotStyle: (" + Integer.toHexString(this.hashCode()) + ")"
                + " '" + unique_name + "' ('" + track_name + "') "
                + " color: " + getForeground()
                + " bg: " + getBackground();
        return s;
    }

    @Override
    public void setReverseHeight(double theNewHeight) {
        this.reverseHeight = theNewHeight;
    }

    @Override
    public double getReverseHeight() {
        return this.reverseHeight;
    }

    @Override
    public void setForwardHeight(double theNewHeight) {
        double rh = this.reverseHeight;
        this.setHeight(theNewHeight); // Because it also does something else. Should that same thing be done for setReverseHeight? - elb
        this.reverseHeight = rh; // Because setHeight sets both forward and reverse.
    }

    @Override
    public double getForwardHeight() {
        return this.getHeight();
    }

    @Override
    public void setReverseMaxDepth(int theNewDepth) {
        if (theNewDepth < 0) {
            theNewDepth = 0;
        }
        this.reverseMaxDepth = theNewDepth;
    }

    @Override
    public int getReverseMaxDepth() {
        return this.reverseMaxDepth;
    }

    @Override
    public void setForwardMaxDepth(int theNewDepth) {
        if (theNewDepth < 0) {
            theNewDepth = 0;
        }
        this.max_depth = theNewDepth;
    }

    @Override
    public int getForwardMaxDepth() {
        return this.max_depth;
    }

    @Override
    public final boolean isFloatTier() {
        return float_graph;
    }

    @Override
    public final void setFloatTier(boolean b) {
        float_graph = b;
    }

    @Override
    public Color getLabelForeground() {
        if (labelForeground == null) {
            return foreground;
        }
        return labelForeground;
    }

    @Override
    public Color getLabelBackground() {
        if (labelBackground == null) {
            return background;
        }
        return labelBackground;
    }

    @Override
    public void setLabelForeground(Color c) {
        labelForeground = c;
        save(PREF_LABEL_FOREGROUND, c);
    }

    @Override
    public void setLabelBackground(Color c) {
        labelBackground = c;
        save(PREF_LABEL_BACKGROUND, c);
    }

    @Override
    public int getSummaryThreshold() {
        return summaryThreshold;
    }

    @Override
    public void setSummaryThreshold(int level) {
        summaryThreshold = level;
    }

    public String getExt() {
        return file_type;
    }

    @Override
    public boolean getJoin() {
        return join;
    }

    @Override
    public void setJoin(boolean b) {
        join = b;
    }

    private boolean save(String key, Object value) {
        return PreferenceUtils.save(getNode(), key, value);
    }

    private Object load(String key, Object def) {
        return PreferenceUtils.load(getNode(), key, def);
    }

    public void setProperties(Map<String, Object> properties) {
        for (Entry<String, Object> entry : properties.entrySet()) {
            setProperty(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean getShowResidueMask() {
        return showResidueMask;
    }

    @Override
    public void setShowResidueMask(boolean showResidueMask) {
        this.showResidueMask = showResidueMask;
        save(PREF_SHOW_RESIDUE_MASK, showResidueMask);
    }

    @Override
    public boolean getShadeBasedOnQualityScore() {
        return shadeBasedOnQualityScore;
    }

    @Override
    public void setShadeBasedOnQualityScore(boolean shadeBasedOnQualityScore) {
        this.shadeBasedOnQualityScore = shadeBasedOnQualityScore;
        save(PREF_SHADE_BASED_ON_QUALITY_SCORE, shadeBasedOnQualityScore);
    }

    public void setProperty(String key, Object value) {
        if (PROP_COLOR.equals(key) && value instanceof Color) {
            this.setForeground((Color) value);
        } else if (PROP_FOREGROUND.equals(key) && value instanceof Color) {
            this.setForeground((Color) value);
        } else if (PROP_BACKGROUND.equals(key) && value instanceof Color) {
            this.setBackground((Color) value);
        } else if (PROP_POSITIVE_STRAND.equals(key) && value instanceof Color) {
            this.setForwardColor((Color) value);
        } else if (PROP_NEGATIVE_STRAND.equals(key) && value instanceof Color) {
            this.setReverseColor((Color) value);
        } else if (PROP_NAME_SIZE.equals(key) && value instanceof Number) {
            this.setTrackNameSize((Float) value);
        } else if (PROP_SHOW_2TRACK.equals(key) && value instanceof Boolean) {
            this.setSeparate((Boolean) value);
        } else if (PROP_CONNECTED.equals(key) && value instanceof Boolean) {
            this.setConnected((Boolean) value);
        } else if (PROP_SHOW_AS_PAIRED.equals(key) && value instanceof Boolean) {
            this.setShowAsPaired((Boolean) value);
        } else if (PROP_GLYPH_DEPTH.equals(key) && value instanceof Number) {
            this.setGlyphDepth((Integer) value);
        } else if (PROP_LABEL_FIELD.equals(key) && value instanceof String) {
            this.setLabelField((String) value);
        } else if (PROP_LABEL_FOREGROUND.equals(key) && value instanceof Color) {
            this.setLabelForeground((Color) value);
        } else if (PROP_LABEL_BACKGROUND.equals(key) && value instanceof Color) {
            this.setLabelBackground((Color) value);
        } else if (PROP_MAX_DEPTH.equals(key) && value instanceof Number) {
            this.setMaxDepth((Integer) value);
        } else if (PROP_SEPARATE.equals(key) && value instanceof Boolean) {
            this.setSeparate((Boolean) value);
        } else if (PROP_SHOW.equals(key) && value instanceof Boolean) {
            this.setShow((Boolean) value);
        } else if (PROP_COLLAPSED.equals(key) && value instanceof Boolean) {
            this.setCollapsed((Boolean) value);
        } else if (PROP_FONT_SIZE.equals(key) && value instanceof Number) {
            this.setTrackNameSize((Float) value);
        } else if (PROP_DIRECTION_TYPE.equals(key) && value instanceof String) {

        } else if (PROP_START_COLOR.equals(key) && value instanceof Color) {
            this.setForwardColor((Color) value);
        } else if (PROP_END_COLOR.equals(key) && value instanceof Color) {
            this.setReverseColor((Color) value);
        } else if (PREF_SHOW_RESIDUE_MASK.equals(key) && value instanceof Boolean) {
            this.setShowResidueMask((Boolean) value);
        } else if (PREF_SHADE_BASED_ON_QUALITY_SCORE.equals(key) && value instanceof Boolean) {
            this.setShadeBasedOnQualityScore((Boolean) value);
        }
    }

    public boolean getCustomizable() {
        return customizable;
    }

}
