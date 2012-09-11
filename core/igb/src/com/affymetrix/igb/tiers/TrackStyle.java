package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.stylesheet.*;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * When setting up a TrackStyle, want to prioritize: <ol type="A"> <li> Start
 * with default instance (from system style sheet?) <li> Modify with user-set
 * default parameters from default Preferences node. <li> Modify with
 * method-matching parameters from system style sheet. <li> Modify with user-set
 * method parameters from Preferences nodes. </ol> Not sure yet where style
 * sheets from DAS/2 servers fits in yet -- between B and C or between C and D?
 */
public class TrackStyle implements ITrackStyleExtended, TrackConstants, PropertyConstants {

	private static Preferences tiers_root_node = PreferenceUtils.getTopNode().node("tiers");
	public static final boolean DEBUG = false;
	public static final boolean DEBUG_NODE_PUTS = false;
	private static boolean draw_collapse_icon = getDrawCollapseState();
	// whether to create and use a java Preferences node object for this instance
	private boolean is_persistent = true;
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
	private DIRECTION_TYPE direction_type = default_direction_type;
	private int glyph_depth = default_glyphDepth;
	private double height = default_height;
	private double y = default_y;
	private Color start_color = default_start;
	private Color end_color = default_end;
	private float min_score_color = default_min_score_color;
	private float max_score_color = default_max_score_color;
	private String url = null;
	private String file_type = null;
	private boolean color_by_score = false;
	private HeatMap custom_heatmap = null;
	private String unique_name;
	private String track_name;
	private String original_track_name;
	final private String method_name;
	private Preferences node;
	private static final Map<String, TrackStyle> static_map = new LinkedHashMap<String, TrackStyle>();
	private static TrackStyle default_instance = null;
	private boolean is_graph = false;
	private float track_name_size = default_track_name_size;
	private Map<String, Object> transient_properties;
	private boolean customizable = true;
	private GenericFeature feature = null;
	private int colorIntervals = 255;
	public boolean customise = false;
	private int summaryThreshold;
	private boolean separable = true;
	// if float_graph, then graph should float above annotations in tiers
	// if !float_graph, then graph should be in its own tier
	private boolean float_graph = false;

	public static TrackStyle getInstance(String unique_name, String track_name, String file_type, Map<String, String> props) {
		FileTypeCategory file_type_category = null;
		FileTypeHandler handler = FileTypeHolder.getInstance().getFileTypeHandler(file_type);
		if (handler != null) {
			file_type_category = handler.getFileTypeCategory();
		}
		return getInstance(unique_name, track_name, file_type, file_type_category, true, true, props);
	}
	
	public static TrackStyle getInstance(String unique_name, String track_name, FileTypeCategory file_type_category, Map<String, String> props) {
		return getInstance(unique_name, track_name, null, file_type_category, true, true, props);
	}
	
	public static TrackStyle getInstance(String unique_name) {
		return getInstance(unique_name, null, null, null, true, false, null);
	}

	private static TrackStyle getInstance(String unique_name, String track_name, String file_type, FileTypeCategory file_type_category, boolean persistent, boolean force_human_name, Map<String, String> props) {
		TrackStyle style = static_map.get(unique_name.toLowerCase());
		if (style == null) {
			if (DEBUG) {
				System.out.println("    (((((((   in AnnotStyle.getInstance() creating AnnotStyle for name: " + unique_name);
			}
			// apply any default stylesheet stuff
			TrackStyle template = getDefaultInstance();
			// at this point template should already have all modifications to default applied from stylesheets and preferences nodes (A & B)
			// apply any stylesheet stuff...
			style = new TrackStyle(unique_name, track_name, file_type, file_type_category, persistent, template, props);
			static_map.put(unique_name.toLowerCase(), style);
		}

		return style;
	}
	
	public void restoreToDefault() {
		TrackStyle template = getDefaultInstance();

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
		if (track_name.equalsIgnoreCase(TrackConstants.NAME_OF_COORDINATE_INSTANCE)) {
			this.setForeground(CoordinateStyle.default_coordinate_color);
			this.setBackground(CoordinateStyle.default_coordinate_background);
			Application igb = Application.getSingleton();
			PreferenceUtils.getTopNode().put(SeqMapView.PREF_COORDINATE_LABEL_FORMAT, SeqMapView.VALUE_COORDINATE_LABEL_FORMAT_COMMA);
			SeqMapView.setAxisFormatFromPrefs(igb.getMapView().getAxisGlyph());
		}
		if (!track_name.equals(TrackConstants.NAME_OF_COORDINATE_INSTANCE)) {
			this.setTrackName(original_track_name);
		}
	}

	/**
	 * Returns all (persistent and temporary) instances of AnnotStyle.
	 */
	public static List<TrackStyle> getAllLoadedInstances() {
		return new ArrayList<TrackStyle>(static_map.values());
	}

	/**
	 * If there is no AnnotStyle with the given name, just returns the given
	 * name; else modifies the name such that there are no instances that are
	 * currently using it.
	 */
	public static String getUniqueName(String name) {
		String result = name.toLowerCase();
		while (static_map.get(result) != null) {
			result = name.toLowerCase() + "." + System.currentTimeMillis();
		}
		return result;
	}

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
	private TrackStyle(String unique_ame, String track_name, String file_type, FileTypeCategory file_type_category, boolean is_persistent, TrackStyle template, Map<String, String> properties) {
		this.method_name = unique_ame;
		this.track_name = track_name; // this is the default human name, and is not lower case
		this.original_track_name = track_name;
		this.file_type = file_type;
		this.unique_name = unique_ame.toLowerCase();
		this.is_persistent = is_persistent;
		this.float_graph = false;

		if (is_persistent) {
			if (this.unique_name.endsWith("/")) {
				this.unique_name = this.unique_name.substring(0, this.unique_name.length() - 1);
			}
			this.unique_name = multiple_slashes.matcher(this.unique_name).replaceAll("/");
			// transforming to shortened but unique name if name exceeds Preferences.MAX_NAME_LENGTH
			//   is now handled within PreferenceUtils.getSubnod() call
		}

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
		assel = stylesheet.getAssociationForType(unique_ame);
		if (assel == null) {
			assel = stylesheet.getAssociationForMethod(unique_ame);
		}
		if (assel != null) {
			PropertyMap props = assel.getPropertyMap();
			if (props != null) {
				initFromPropertyMap(props);
			}
		}

		if (is_persistent) {
			try {
				node = PreferenceUtils.getSubnode(tiers_root_node, this.unique_name);
			} catch (Exception e) {
				// if there is a problem creating the node, continue with a non-persistent style.
				e.printStackTrace();
				node = null;
				is_persistent = false;
			}
			if (node != null) {
				initFromNode(node);
			}
		} else {
			node = null;
		}
	}

	// Copies properties from the given node, using the currently-loaded values as defaults.
	// generally call initFromTemplate before this.
	// Make sure to set human_name to some default before calling this.
	// Properties set this way do NOT get put in persistent storage.
	private void initFromNode(Preferences node) {
		if (DEBUG) {
			System.out.println("    ----------- called AnnotStyle.initFromNode() for: " + unique_name);
		}
		track_name = node.get(PREF_TRACK_NAME, this.track_name);
		show2tracks = node.getBoolean(PREF_SHOW2TRACKS, this.getSeparate());
		glyph_depth = node.getInt(PREF_GLYPH_DEPTH, this.getGlyphDepth());
		connected = node.getBoolean(PREF_CONNECTED, this.getConnected());
		collapsed = node.getBoolean(PREF_COLLAPSED, this.getCollapsed());
		max_depth = node.getInt(PREF_MAX_DEPTH, this.getMaxDepth());
		foreground = PreferenceUtils.getColor(node, PREF_FOREGROUND, this.getForeground());
		background = PreferenceUtils.getColor(node, PREF_BACKGROUND, this.getBackground());
		start_color = PreferenceUtils.getColor(node, PREF_START_COLOR, this.getForwardColor());
		end_color = PreferenceUtils.getColor(node, PREF_END_COLOR, this.getReverseColor());

		label_field = node.get(PREF_LABEL_FIELD, this.getLabelField());
		track_name_size = node.getFloat(PREF_TRACK_SIZE, this.getTrackNameSize());
		direction_type = DIRECTION_TYPE.valueFor(node.getInt(PREF_DIRECTION_TYPE, this.getDirectionType()));
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
	private void initFromPropertyMap(PropertyMap props) {

		if (DEBUG) {
			System.out.println("    +++++ initializing AnnotStyle from PropertyMap: " + unique_name);
			System.out.println("             props: " + props);
		}

		Color col = props.getColor(PROP_COLOR);
		if (col == null) {
			col = props.getColor(PROP_FOREGROUND);
		}
		if (col != null) {
			this.setForeground(col);
		}
		col = props.getColor(PROP_BACKGROUND);
		if (col != null) {
			this.setBackground(col);
		}

		col = props.getColor(PROP_START_COLOR);
		if (col != null) {
			this.setForwardColor(col);
		}

		col = props.getColor(PROP_END_COLOR);
		if (col != null) {
			this.setReverseColor(col);
		}

		String gdepth_string = (String) props.getProperty(PROP_GLYPH_DEPTH);
		if (gdepth_string != null) {
			int prev_glyph_depth = glyph_depth;
			try {
				this.setGlyphDepth(Integer.parseInt(gdepth_string));
			} catch (Exception ex) {
				this.setGlyphDepth(prev_glyph_depth);
			}
		}

		String labfield = (String) props.getProperty(PROP_LABEL_FIELD);
		if (labfield != null) {
			this.setLabelField(labfield);
		}

		String mdepth_string = (String) props.getProperty(PROP_MAX_DEPTH);
		if (mdepth_string != null) {
			int prev_max_depth = max_depth;
			try {
				this.setMaxDepth(Integer.parseInt(mdepth_string));
			} catch (Exception ex) {
				this.setMaxDepth(prev_max_depth);
			}
		}

		String sepstring = (String) props.getProperty(PROP_SEPARATE);
		if (sepstring != null) {
			if (sepstring.equalsIgnoreCase(FALSE)) {
				this.setSeparate(false);
			} else if (sepstring.equalsIgnoreCase(TRUE)) {
				this.setSeparate(true);
			}
		}

		String showstring = (String) props.getProperty(PROP_SHOW);
		if (showstring != null) {
			if (showstring.equalsIgnoreCase(FALSE)) {
				show = false;
			} else if (showstring.equalsIgnoreCase(TRUE)) {
				show = true;
			}
		}

		String collapstring = (String) props.getProperty(PROP_COLLAPSED);
		if (collapstring != null) {
			if (collapstring.equalsIgnoreCase(FALSE)) {
				this.setCollapsed(false);
			} else if (collapstring.equalsIgnoreCase(TRUE)) {
				this.setCollapsed(true);
			}
		}
		String fontstring = (String) props.getProperty(PROP_FONT_SIZE);
		if (fontstring != null) {
			float prev_font_size = track_name_size;
			try {
				this.setTrackNameSize(Float.parseFloat(fontstring));
			} catch (Exception ex) {
				this.setTrackNameSize(prev_font_size);
			}
		}
		String directionstring = (String) props.getProperty(PROP_DIRECTION_TYPE);
		if (directionstring != null) {
			DIRECTION_TYPE prev_direction_type = direction_type;
			try {
				this.setDirectionType(DIRECTION_TYPE.valueFor(directionstring));
			} catch (Exception ex) {
				this.setDirectionType(prev_direction_type);
			}
		}
		
		if (DEBUG) {
			System.out.println("    +++++++  done initializing from PropertyMap");
		}
		// height???
	}

	private void initFromPropertyMap(Map<String, String> props) {
		String fgString = props.get("foreground");
		if (fgString != null && !"".equals(fgString)) {
			this.setForeground(Color.decode("0x" + fgString));
		}

		String bgString = props.get("background");
		if (bgString != null && !"".equals(bgString)) {
			this.setBackground(Color.decode("0x" + bgString));
		}

		String startColorString = props.get("positive_strand_color");
		if (startColorString != null && !"".equals(startColorString)) {
			this.setForwardColor(Color.decode("0x" + startColorString));
		}

		String endColorString = props.get("negative_strand_color");
		if (endColorString != null && !"".equals(endColorString)) {
			this.setReverseColor(Color.decode("0x" + endColorString));
		}

		String labfield = props.get("label_field");
		if (labfield != null && !"".equals(labfield) && label_field != null) {
			this.setLabelField(labfield);
		}

		String mDepthString = props.get("max_depth");
		if (mDepthString != null && !"".equals(mDepthString)) {
			int prev_max_depth = max_depth;
			try {
				this.setMaxDepth(Integer.parseInt(mDepthString));
			} catch (Exception ex) {
				this.setMaxDepth(prev_max_depth);
			}
		}

		String nameSizeString = props.get("name_size");
		if (nameSizeString != null && !"".equals(nameSizeString)) {
			float prev_font_size = track_name_size;
			try {
				this.setTrackNameSize(Float.parseFloat(nameSizeString));
			} catch (Exception ex) {
				this.setTrackNameSize(prev_font_size);
			}
		}

		String connectedString = props.get("connected");
		if (connectedString != null && !"".equals(connectedString)) {
			if (connectedString.equalsIgnoreCase(FALSE)) {
				this.setGlyphDepth(1);
			} else if (connectedString.equalsIgnoreCase(TRUE)) {
				this.setGlyphDepth(2);
			}
		}

		String collapsedString = props.get("collapsed");
		if (collapsedString != null && !"".equals(collapsedString)) {
			if (collapsedString.equalsIgnoreCase(FALSE)) {
				this.setCollapsed(false);
			} else if (collapsedString.equalsIgnoreCase(TRUE)) {
				this.setCollapsed(true);
			}
		}

		String show2tracksString = props.get("show2tracks");
		if (show2tracksString != null && !"".equals(show2tracksString)) {
			if (show2tracksString.equalsIgnoreCase(FALSE)) {
				this.setSeparate(false);
			} else if (show2tracksString.equalsIgnoreCase(TRUE)) {
				this.setSeparate(true);
			}
		}

		String directionstring = props.get("direction_type");
		if (directionstring != null) {
			DIRECTION_TYPE prev_direction_type = direction_type;
			try {
				this.setDirectionType(DIRECTION_TYPE.valueFor(directionstring));
			} catch (Exception ex) {
				this.setDirectionType(prev_direction_type);
			}
		}

	}

	// Copies properties from the template into this object, but does NOT persist
	// these copied values.
	// human_name and factory_instance are not modified
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
		this.setLabelForeground(null);
	}

	// Returns the preferences node, or null if this is a non-persistent instance.
	private Preferences getNode() {
		return this.node;
	}

	public int getColorIntervals() {
		return colorIntervals;
	}

	public void setColorIntervals(int intervals) {
		colorIntervals = intervals;
	}

	public void setCustomHeatMap(HeatMap map) {
		custom_heatmap = map;
	}
	/*
	 * Gets an instance that can be used for holding default values. The default
	 * instance is used as a template in creating new instances. (Although not
	 * ALL properties of the default instance are used in this way.)
	 */

	public static TrackStyle getDefaultInstance() {
		if (default_instance == null) {
			// Use a temporary variable here to avoid possible synchronization problems.
			TrackStyle instance = new TrackStyle(NAME_OF_DEFAULT_INSTANCE, NAME_OF_DEFAULT_INSTANCE, null, null, true, null, null);
			instance.setTrackName("");
			instance.setShow(true);
			default_instance = instance;
			// Note that name will become lower-case
			static_map.put(default_instance.unique_name, default_instance);
		}
		return default_instance;
	}

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
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setTrackName(): " + track_name);
			}
			getNode().put(PREF_TRACK_NAME, track_name);
		}
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
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setSeparate(): " + track_name + ", " + b);
			}
			getNode().putBoolean(PREF_SHOW2TRACKS, b);
		}
	}

	public final boolean getCustomizable() {
		return customizable;
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
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setCollapsed(): " + track_name + ", " + b);
			}
			getNode().putBoolean(PREF_COLLAPSED, b);
		}
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
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setMaxDepth(): " + track_name + ", " + max);
			}
			getNode().putInt(PREF_MAX_DEPTH, max);
		}
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
		if (c != this.foreground) {
			custom_heatmap = null;
			// get rid of old heatmap, force it to be re-created when needed
		}
		this.foreground = c;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setColor(): " + track_name + ", " + c);
			}
			PreferenceUtils.putColor(getNode(), PREF_FOREGROUND, c);
		}
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
		if (c != this.background) {
			custom_heatmap = null;
			// get rid of old heatmap, force it to be re-created when needed
		}
		this.background = c;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setBackground(): " + track_name + ", " + c);
			}
			PreferenceUtils.putColor(getNode(), PREF_BACKGROUND, c);
		}
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
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setBackground(): " + track_name + ", " + c);
			}
			PreferenceUtils.putColor(getNode(), PREF_START_COLOR, c);
		}
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
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setBackground(): " + track_name + ", " + c);
			}
			PreferenceUtils.putColor(getNode(), PREF_END_COLOR, c);
		}
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
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setLabelField(): " + track_name + ", " + l);
			}
			getNode().put(PREF_LABEL_FIELD, l);
		}
	}

	public boolean getConnected() {
		return connected;
	}

	public void setConnected(boolean b) {
		connected = b;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setConnected(): " + track_name + ", " + b);
			}
			getNode().putBoolean(PREF_CONNECTED, b);
		}
	}

	@Override
	public int getGlyphDepth() {
		return glyph_depth;
	}

	@Override
	public void setGlyphDepth(int i) {
		if (glyph_depth != i) {
			glyph_depth = i;
			if (getNode() != null) {
				if (DEBUG_NODE_PUTS) {
					System.out.println("   %%%%% node.put() in AnnotStyle.setGlyphDepth(): " + track_name + ", " + i);
				}
				getNode().putInt(PREF_GLYPH_DEPTH, i);
			}
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
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setHeight(): " + track_name + ", " + h);
			}
			getNode().putDouble(PREF_HEIGHT, h);
		}
		this.reverseHeight = this.height;
	}

	@Override
	public float getTrackNameSize() {
		return track_name_size;
	}

	@Override
	public void setTrackNameSize(float font_size) {
		this.track_name_size = font_size;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setFontSize(): " + track_name + ", " + font_size);
			}
			getNode().putFloat(PREF_TRACK_SIZE, font_size);
		}
	}

	@Override
	public void setFeature(GenericFeature f) {
		this.feature = f;
	}

	@Override
	public GenericFeature getFeature() {
		return this.feature;
	}

	public void setDirectionType(DIRECTION_TYPE type) {
		this.direction_type = type;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setDirectionType(): " + track_name + ", " + direction_type);
			}
			getNode().putInt(PREF_DIRECTION_TYPE, type.ordinal());
		}
	}

	@Override
	public int getDirectionType() {
		return direction_type.ordinal();
	}

	@Override
	public void setDirectionType(int ordinal) {
		direction_type = TrackConstants.DIRECTION_TYPE.values()[ordinal];
	}

	public DIRECTION_TYPE getDirectionName() {
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

	public final boolean getPersistent() {
		return (is_persistent && getNode() != null);
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
			transient_properties = new HashMap<String, Object>();
		}
		return transient_properties;
	}

	/**
	 * Indicates whether the scores of the annotations should be marked by
	 * colors.
	 */
	@Override
	public void setColorByScore(boolean b) {
		color_by_score = b;
	}

	/**
	 * Indicates whether the scores of the annotations should be marked by
	 * colors.
	 */
	@Override
	public boolean getColorByScore() {
		return color_by_score;
	}

	public void setMinScoreColor(float min_score_color) {
		this.min_score_color = min_score_color;
	}

	public float getMinScoreColor() {
		return min_score_color;
	}

	public void setMaxScoreColor(float max_score_color) {
		this.max_score_color = max_score_color;
	}

	public float getMaxScoreColor() {
		return max_score_color;
	}

	/**
	 * Returns a color that can be used to indicate a score between 1 and 1000.
	 * This will return a color even if getColorByScore() is false.
	 */
	@Override
	public Color getScoreColor(float score) {
		final float min = getMinScoreColor();
		final float max = getMaxScoreColor();

		if (score < min) {
			score = min;
		} else if (score >= max) {
			score = max;
		}

		final float range = max - min;
		int index = (int) (((score - min) / range) * 255);

		return getCustomHeatMap().getColors()[index];
	}

	/**
	 * Returns a HeatMap that interpolates between colors based on getColor()
	 * and getBackgroundColor(). The color at the low end of the HeatMap will be
	 * slightly different from the background color so that it can be
	 * distinguished from it. This will return a HeatMap even if
	 * getColorByScore() is false.
	 */
	private HeatMap getCustomHeatMap() {
		if (custom_heatmap == null) {
			// Bottom color is not quite same as background, so it remains visible
			Color bottom_color = HeatMap.interpolateColor(getBackground(), getForeground(), 0.20f);
			custom_heatmap = HeatMap.makeLinearHeatmap("Custom", bottom_color, getForeground());
		}
		return custom_heatmap;
	}

	public static void setDrawCollapseControl(boolean b) {
		draw_collapse_icon = b;
		if (tiers_root_node != null) {
			tiers_root_node.putBoolean(PREF_DRAW_COLLAPSE_ICON, b);
		}
	}

	public static boolean getDrawCollapseState() {
		return tiers_root_node.getBoolean(PREF_DRAW_COLLAPSE_ICON, default_draw_collapse_icon);
	}

	public static void setShowIGBTrackMark(boolean b) {
		if (tiers_root_node != null) {
			tiers_root_node.putBoolean(PREF_SHOW_IGB_TRACK_MARK, b);
		}
	}

	public static boolean getShowIGBTrackMarkState() {
		return tiers_root_node.getBoolean(PREF_SHOW_IGB_TRACK_MARK, default_show_igb_track_mark);
	}

	public boolean drawCollapseControl() {
		return (draw_collapse_icon && getExpandable());
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
			setColorByScore(as.getColorByScore());
			setGlyphDepth(as.getGlyphDepth());
			setLabelField(as.getLabelField());
			setSeparate(as.getSeparate());
			setSummaryThreshold(as.getSummaryThreshold());
		}
		if (g instanceof TrackStyle) {
			TrackStyle as = (TrackStyle) g;
			setCustomizable(as.getCustomizable());
		}

		getTransientPropertyMap().putAll(g.getTransientPropertyMap());
	}

	/**
	 * Whether this style should be customizable in a preferences panel.
	 * Sometimes there are temporary styles created where some of the options
	 * simply don't make sense and shouldn't be shown to the user in the
	 * customization panel.
	 */
	public final void setCustomizable(boolean b) {
		// Another option instead of a single set/getCustomizable flag would be
		// to have a bunch of individual flags: getSeparable(), getHumanNamable(),
		// getHasMaxDepth(), etc....
		customizable = b;
	}

	public boolean getSeparable(){
		return separable;
	}
	
	public void setSeparable(boolean b){
		separable = b;
	}
	
	@Override
	public String toString() {
		String s = "AnnotStyle: (" + Integer.toHexString(this.hashCode()) + ")"
				+ " '" + unique_name + "' ('" + track_name + "') "
				+ " persistent: " + is_persistent
				+ " color: " + getForeground()
				+ " bg: " + getBackground();
		return s;
	}

	public static synchronized boolean autoSaveUserStylesheet() {
		Stylesheet stylesheet = XmlStylesheetParser.getUserStylesheet();
		if (stylesheet == null) {
			Logger.getLogger(TrackStyle.class.getName()).log(Level.SEVERE, "No user stylesheet present.");
			return false;
		}

		java.io.File f = XmlStylesheetParser.getUserStylesheetFile();
		String filename = f.getAbsolutePath();
		java.io.FileWriter fw = null;
		java.io.BufferedWriter bw = null;
		try {
			Logger.getLogger(TrackStyle.class.getName()).log(Level.INFO, "Saving user stylesheet to file {0}", filename);
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
			Logger.getLogger(TrackStyle.class.getName()).log(Level.SEVERE, "Could not auto-save user stylesheet to {0}", filename);
		} catch (java.io.IOException ioe) {
			Logger.getLogger(TrackStyle.class.getName()).log(Level.SEVERE, "Error while saving user stylesheet to {0}", filename);
		} finally {
			GeneralUtils.safeClose(bw);
			GeneralUtils.safeClose(fw);
		}

		return false;
	}

	/**
	 * for height on the reverse strand. To help with track resizing.
	 */
	private double reverseHeight = TrackConstants.default_height;

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
	/**
	 * for maximum depth of stacked glyphs on the reverse strand. To help with
	 * resizing.
	 */
	private int reverseMaxDepth = 0;

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
	public final boolean getFloatTier() {
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
	}

	@Override
	public void setLabelBackground(Color c) {
		labelBackground = c;
	}

	@Override
	public int getSummaryThreshold() {
		return summaryThreshold;
	}

	@Override
	public void setSummaryThreshold(int level) {
		summaryThreshold = level;
	}
}
