package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.style.HeatMap;
import java.awt.Color;
import java.util.*;
import java.util.prefs.*;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.glyph.MapViewModeHolder;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.stylesheet.AssociationElement;
import com.affymetrix.igb.stylesheet.PropertyConstants;
import com.affymetrix.igb.stylesheet.Stylesheet;
import com.affymetrix.igb.stylesheet.PropertyMap;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *  When setting up a TrackStyle, want to prioritize:
 *
 *  A) Start with default instance (from system stylesheet?)
 *
 *  B) Modify with user-set default parameters from default Preferences node
 *
 *  C) Modify with method-matching parameters from system stylesheet
 *
 *  D) Modify with user-set method parameters from Preferences nodes
 *
 *  Not sure yet where stylesheets from DAS/2 servers fits in yet -- between B/C or between C/D ?
 */
public class TrackStyle implements ITrackStyleExtended, TrackConstants, PropertyConstants {

	private static Preferences tiers_root_node = PreferenceUtils.getTopNode().node("tiers");
	public static final boolean DEBUG = false;
	public static final boolean DEBUG_NODE_PUTS = false;
	// whether to create and use a java Preferences node object for this instance
	private boolean is_persistent = true;
	private boolean show = default_show;
	private boolean show2Tracks = default_show2Tracks;
	private boolean collapsed = default_collapsed;
	private boolean expandable = default_expandable;
	private int max_depth = default_max_depth;
	private Color foreground = default_foreground;
	private Color background = default_background;
	private String label_field = default_label_field;
	private DIRECTION_TYPE direction_type = default_direction_type;
	private boolean connected = default_connected;
	private double height = default_height;
	private double y = default_y;
	private Color start_color = default_start;
	private Color end_color = default_end;
	private String view_mode = default_view_mode;
	private String url = null;
	private String file_type = null;
	private boolean color_by_score = false;
	private HeatMap custom_heatmap = null;
	private String unique_name;
	private String track_name;
	final private String method_name;
	private Preferences node;
	private static final Map<String, TrackStyle> static_map = new LinkedHashMap<String, TrackStyle>();
	private static TrackStyle default_instance = null;
	private boolean is_graph = false;
	private float track_name_size = default_track_name_size;
	private Map<String, Object> transient_properties;
	private boolean customizable = true;
	private GenericFeature feature = null;

	public static TrackStyle getInstance(String name, String human_name, String file_type, Map<String, String> props) {
		return getInstance(name, human_name, file_type, true, true, props);
	}

	public static TrackStyle getInstance(String name, String human_name, String file_type) {
		return getInstance(name, human_name, file_type, true, true, null);
	}

	public static TrackStyle getInstance(String unique_name, boolean persistent) {
		return getInstance(unique_name, null, null, persistent, false, null);
	}

	private static TrackStyle getInstance(String unique_name, String human_name, String file_type, boolean persistent, boolean force_human_name, Map<String, String> props) {
		TrackStyle style = static_map.get(unique_name.toLowerCase());
		if (style == null) {
			if (DEBUG) {
				System.out.println("    (((((((   in AnnotStyle.getInstance() creating AnnotStyle for name: " + unique_name);
			}
			// apply any default stylesheet stuff
			TrackStyle template = getDefaultInstance();
			// at this point template should already have all modifications to default applied from stylesheets and preferences nodes (A & B)
			// apply any stylesheet stuff...
			style = new TrackStyle(unique_name, file_type, persistent, template, props);
			static_map.put(unique_name.toLowerCase(), style);

			if (force_human_name) {
				style.track_name = human_name;
			}
		} else if (props != null) {
			style.initFromPropertyMap(props);
		}

		return style;
	}

	/** Returns all (persistent and temporary) instances of AnnotStyle. */
	public static List<TrackStyle> getAllLoadedInstances() {
		return new ArrayList<TrackStyle>(static_map.values());
	}

	/** If there is no AnnotStyle with the given name, just returns the given name;
	 * else modifies the name such that there are no instances that are currently
	 * using it.
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

	public TrackStyle(AssociationElement element) {
		this();
		initFromTemplate(getDefaultInstance());
		initFromPropertyMap(element.getPropertyMap());
	}

	/** Creates an instance associated with a case-insensitive form of the unique name.
	 *
	 *   When setting up an AnnotStyle, want to prioritize:
	 *
	 *  A) Start with default instance (from system stylesheet?)
	 *  B) Modify with user-set default parameters from default Preferences node
	 *  C) Modify with method-matching parameters from system stylesheet
	 *  D) Modify with user-set method parameters from Preferences nodes
	 *
	 *  Not sure yet where stylesheets from DAS/2 servers fits in yet -- between B/C or between C/D ?
	 */
	private TrackStyle(String name, String file_type, boolean is_persistent, TrackStyle template, Map<String, String> properties) {
		this.method_name = name;
		this.track_name = name; // this is the default human name, and is not lower case
		this.file_type = file_type;
		this.unique_name = name.toLowerCase();
		this.is_persistent = is_persistent;

		if (is_persistent) {
			if (unique_name.endsWith("/")) {
				unique_name = unique_name.substring(0, unique_name.length() - 1);
			}
			unique_name = multiple_slashes.matcher(unique_name).replaceAll("/");
			// transforming to shortened but unique name if name exceeds Preferences.MAX_NAME_LENGTH
			//   is now handled within PreferenceUtils.getSubnod() call
		}

		if (template != null) {
			// calling initFromTemplate should take care of A) and B)
			initFromTemplate(template);
		}

		// GAH eliminated hard-coded default settings for glyph_depth, can now set in stylesheet
		//    applyHardCodedDefaults();

		// now need to add use of stylesheet settings via AssociationElements, etc.
		Stylesheet stylesheet = XmlStylesheetParser.getUserStylesheet();
		AssociationElement assel = stylesheet.getAssociationForFileType(file_type);
		if (assel != null) {
			PropertyMap props = assel.getPropertyMap();
			if (props != null) {
				initFromPropertyMap(props);
			}
		}
		assel = stylesheet.getAssociationForType(name);
		if (assel == null) {
			assel = stylesheet.getAssociationForMethod(name);
		}
		if (assel != null) {
			PropertyMap props = assel.getPropertyMap();
			if (props != null) {
				initFromPropertyMap(props);
			}
		}
		if (properties != null) {
			initFromPropertyMap(properties);
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

		show2Tracks = node.getBoolean(PREF_SHOW2TRACKS, this.getShow2Tracks());
		collapsed = node.getBoolean(PREF_COLLAPSED, this.getCollapsed());
		max_depth = node.getInt(PREF_MAX_DEPTH, this.getMaxDepth());
		foreground = PreferenceUtils.getColor(node, PREF_FOREGROUND, this.getForeground());
		background = PreferenceUtils.getColor(node, PREF_BACKGROUND, this.getBackground());
		start_color = PreferenceUtils.getColor(node, PREF_START_COLOR, this.getForwardColor());
		end_color = PreferenceUtils.getColor(node, PREF_END_COLOR, this.getReverseColor());

		label_field = node.get(PREF_LABEL_FIELD, this.getLabelField());
		connected = node.getBoolean(PREF_CONNECTED, this.getConnected());
		track_name_size = node.getFloat(PREF_TRACK_SIZE, this.getTrackNameSize());
		view_mode = node.get(PREF_VIEW_MODE, this.getViewMode());
		direction_type = DIRECTION_TYPE.valueFor(node.getInt(PREF_DIRECTION_TYPE, this.getDirectionType()));
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
			foreground = col;
		}
		Color bgcol = props.getColor(PROP_BACKGROUND);
		if (bgcol != null) {
			background = bgcol;
		}

		Color stcol = props.getColor(PROP_START_COLOR);
		if (stcol != null) {
			start_color = stcol;
		}

		Color edcol = props.getColor(PROP_END_COLOR);
		if (edcol != null) {
			end_color = edcol;
		}

		String labfield = (String) props.getProperty(PROP_LABEL_FIELD);
		if (labfield != null) {
			label_field = labfield;
		}

		String mdepth_string = (String) props.getProperty(PROP_MAX_DEPTH);
		if (mdepth_string != null) {
			int prev_max_depth = max_depth;
			try {
				max_depth = Integer.parseInt(mdepth_string);
			} catch (Exception ex) {
				max_depth = prev_max_depth;
			}
		}

		String show2tracks_string = (String) props.getProperty(PROP_SHOW_2_TRACKS);
		if (show2tracks_string != null) {
			if (show2tracks_string.equalsIgnoreCase(FALSE)) {
				show2Tracks = false;
			} else if (show2tracks_string.equalsIgnoreCase(TRUE)) {
				show2Tracks = true;
			}
		}
		String connected_string = (String) props.getProperty(PROP_CONNECTED);
		if (connected_string != null) {
			if (connected_string.equalsIgnoreCase(FALSE)) {
				connected = false;
			} else if (connected_string.equalsIgnoreCase(TRUE)) {
				connected = true;
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
				collapsed = false;
			} else if (collapstring.equalsIgnoreCase(TRUE)) {
				collapsed = true;
			}
		}
		String fontstring = (String) props.getProperty(PROP_FONT_SIZE);
		if (fontstring != null) {
			float prev_font_size = track_name_size;
			try {
				track_name_size = Float.parseFloat(fontstring);
			} catch (Exception ex) {
				track_name_size = prev_font_size;
			}
		}
		String directionstring = (String) props.getProperty(PROP_DIRECTION_TYPE);
		if (directionstring != null) {
			DIRECTION_TYPE prev_direction_type = direction_type;
			try {
				direction_type = DIRECTION_TYPE.valueFor(directionstring);
			} catch (Exception ex) {
				direction_type = prev_direction_type;
			}
		}
		String viewmodestring = (String) props.getProperty(PROP_VIEW_MODE);
		if (viewmodestring != null) {
			setViewMode(viewmodestring);
		}

		if (DEBUG) {
			System.out.println("    +++++++  done initializing from PropertyMap");
		}
		// height???
	}

	private void initFromPropertyMap(Map<String, String> props) {
		String labfield = props.get("label_field");
		if (labfield != null && !"".equals(labfield) && label_field != null) {
			label_field = labfield;
		}
	}

	// Copies properties from the template into this object, but does NOT persist
	// these copied values.
	// human_name and factory_instance are not modified
	private void initFromTemplate(TrackStyle template) {
		show2Tracks = template.getShow2Tracks();
		show = template.getShow();
		collapsed = template.getCollapsed();
		max_depth = template.getMaxDepth();  // max stacking of annotations
		foreground = template.getForeground();
		background = template.getBackground();
		label_field = template.getLabelField();
		connected = template.getConnected();  // depth of visible glyph tree
		track_name_size = template.getTrackNameSize();
		start_color = template.getForwardColor();
		end_color = template.getReverseColor();
		view_mode = template.getViewMode();
		direction_type = template.direction_type;
	}

	// Returns the preferences node, or null if this is a non-persistent instance.
	private Preferences getNode() {
		return this.node;
	}

	/* Gets an instance that can be used for holding
	 *  default values.  The default instance is used as a template in creating
	 *  new instances.  (Although not ALL properties of the default instance are used
	 *  in this way.)
	 */
	public static TrackStyle getDefaultInstance() {
		if (default_instance == null) {
			// Use a temporary variable here to avoid possible synchronization problems.
			TrackStyle instance = new TrackStyle(NAME_OF_DEFAULT_INSTANCE, null, true, null, null);
			instance.setTrackName("");
			instance.setShow(true);
			default_instance = instance;
			// Note that name will become lower-case
			static_map.put(default_instance.unique_name, default_instance);
		}
		return default_instance;
	}

	public String getUniqueName() {
		return unique_name;
	}

	public String getMethodName() {
		return method_name;
	}

	/** Gets a name that may be shorter and more user-friendly than the unique name.
	 *  The human-readable name may contain upper- and lower-case characters.
	 *  The default is equivalent to the unique name.
	 */
	public String getTrackName() {
		if (track_name == null || track_name.trim().length() == 0) {
			track_name = unique_name;
		}
		return this.track_name;
	}

	public void setTrackName(String human_name) {
		this.track_name = human_name;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setHumanName(): " + human_name);
			}
			getNode().put(PREF_TRACK_NAME, human_name);
		}
	}

	/** Whether the tier is shown or hidden. */
	public boolean getShow() {
		return show;
	}

	/** Sets whether the tier is shown or hidden; this is a non-persistent setting. */
	public void setShow(boolean b) {
		this.show = b;
	}

	/** Whether PLUS and MINUS strand should be in separate tiers. */
	//Show2Tracks
	public boolean getShow2Tracks() {
		return show2Tracks;
	}

	public void setShow2Tracks(boolean b) {
		this.show2Tracks = b;
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

	/** Whether tier is collapsed. */
	public boolean getCollapsed() {
		return collapsed;
	}

	public void setCollapsed(boolean b) {
		this.collapsed = b;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setCollapsed(): " + track_name + ", " + b);
			}
			getNode().putBoolean(PREF_COLLAPSED, b);
		}
	}

	/** Maximum number of rows of annotations for this tier. */
	public int getMaxDepth() {
		return max_depth;
	}

	/** Set the maximum number of rows of annotations for this tier.
	 *  Any attempt to set this less than zero will
	 *  fail, the value will be truncated to fit the range.
	 *  @param max a non-negative number.
	 */
	public void setMaxDepth(int max) {
		if (max < 0) {
			max = 0;
		}
		this.max_depth = max;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setMaxDepth(): " + track_name + ", " + max);
			}
			getNode().putInt(PREF_MAX_DEPTH, max);
		}
	}

	/** The color of annotations in the tier. */
	public Color getForeground() {
		return foreground;
	}

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

	/** The color of the tier Background. */
	public Color getBackground() {
		return background;
	}

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

	/** The color of the start direction. */
	public Color getForwardColor() {
		return start_color;
	}

	public void setForwardColor(Color c) {
		this.start_color = c;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setBackground(): " + track_name + ", " + c);
			}
			PreferenceUtils.putColor(getNode(), PREF_START_COLOR, c);
		}
	}

	/** The color of the start direction. */
	public Color getReverseColor() {
		return end_color;
	}

	public void setReverseColor(Color c) {
		this.end_color = c;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setBackground(): " + track_name + ", " + c);
			}
			PreferenceUtils.putColor(getNode(), PREF_END_COLOR, c);
		}
	}

	/** Returns the field name from which the glyph labels should be taken.
	 *  This will never return null, but will return "" instead.
	 */
	public String getLabelField() {
		return label_field;
	}

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
		this.connected = b;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setCollapsed(): " + track_name + ", " + b);
			}
			getNode().putBoolean(PREF_CONNECTED, b);
		}
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double h) {
		height = h;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setHeight(): " + track_name + ", " + h);
			}
			getNode().putDouble(PREF_HEIGHT, h);
		}
	}

	public float getTrackNameSize() {
		return track_name_size;
	}

	public void setTrackNameSize(float font_size) {
		this.track_name_size = font_size;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setFontSize(): " + track_name + ", " + font_size);
			}
			getNode().putFloat(PREF_TRACK_SIZE, font_size);
		}
	}

	public void setFeature(GenericFeature f) {
		this.feature = f;
	}

	public GenericFeature getFeature() {
		return this.feature;
	}

	public String getFileType() {
		return file_type;
	}

	public void setDirectionType(DIRECTION_TYPE type) {
		this.direction_type = type;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setDirectionType(): " + track_name + ", " + direction_type);
			}
			getNode().putInt(PREF_DIRECTION_TYPE, direction_type.ordinal());
		}
	}

	public int getDirectionType() {
		return direction_type.ordinal();
	}

	public DIRECTION_TYPE getDirectionName() {
		return direction_type;
	}

	/** could be used to remember tier positions. */
	public void setY(double y) {
		this.y = y;
	}

	/** could be used to remember tier positions. */
	public double getY() {
		return y;
	}

	/** A non-persistent property.  Usually set by UCSC browser "track" lines. */
	public void setUrl(String url) {
		this.url = url;
	}

	/** A non-persistent property.  Usually set by UCSC browser "track" lines. Can return null. */
	public String getUrl() {
		return this.url;
	}

	public final boolean getPersistent() {
		return (is_persistent && getNode() != null);
	}

	public boolean getExpandable() {
		return expandable;
	}

	public void setExpandable(boolean b) {
		// currently there is no need to make this property persistent.
		// there is rarly any reason to change it from the defualt value for
		// annotation tiers, only for graph tiers, which don't use this class
		expandable = b;
	}

	/** Returns false by default.  This class is only intended for annotation tiers,
	 *  not graph tiers.
	 */
	public boolean isGraphTier() {
		return is_graph;
	}

	/** Avoid setting to anything but false.  This class is only intended for annotation tiers,
	 *  not graph tiers.
	 */
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
	 *  Indicates whether the scores of the annotations should be marked by colors.
	 */
	public void setColorByScore(boolean b) {
		color_by_score = b;
	}

	/**
	 *  Indicates whether the scores of the annotations should be marked by colors.
	 */
	public boolean getColorByScore() {
		return color_by_score;
	}

	/**
	 *  Returns a color that can be used to indicate a score between 1 and 1000.
	 *  This will return a color even if getColorByScore() is false.
	 */
	public Color getScoreColor(float score) {
		final float min = 1.0f; // min and max might become variables later...
		final float max = 1000.0f;

		if (score < min) {
			score = min;
		} else if (score > max) {
			score = max;
		}

		final float range = max - min;
		int index = (int) ((score / range) * 255);

		return getCustomHeatMap().getColors()[index];
	}

	/**
	 *  Returns a HeatMap that interpolates between colors based on
	 *  getColor() and getBackgroundColor().  The color at the low
	 *  end of the HeatMap will be slightly different from the background
	 *  color so that it can be distinguished from it.
	 *  This will return a HeatMap even if getColorByScore() is false.
	 */
	private HeatMap getCustomHeatMap() {
		if (custom_heatmap == null) {
			// Bottom color is not quite same as background, so it remains visible
			Color bottom_color = HeatMap.interpolateColor(getBackground(), getForeground(), 0.20f);
			custom_heatmap = HeatMap.makeLinearHeatmap("Custom", bottom_color, getForeground());
		}
		return custom_heatmap;
	}

	public void setViewMode(String s) {
		if (s != null && !default_view_mode.equalsIgnoreCase(s)
				&& MapViewModeHolder.getInstance().getViewFactory(s) == null) {
			Logger.getLogger(TrackStyle.class.getName()).log(Level.WARNING, "No view mode factory found for {0}. Using default view mode only.", s);
			s = default_view_mode;
		}
		view_mode = s;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setViewMode(): " + s);
			}
			getNode().put(PREF_VIEW_MODE, s);
		}
	}

	public String getViewMode() {
		return view_mode;
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
		setFeature(g.getFeature());

		if (g instanceof ITrackStyleExtended) {
			ITrackStyleExtended as = (ITrackStyleExtended) g;
			setColorByScore(as.getColorByScore());
			setConnected(as.getConnected());
			setLabelField(as.getLabelField());
			setShow2Tracks(as.getShow2Tracks());
		}
		if (g instanceof TrackStyle) {
			TrackStyle as = (TrackStyle) g;
			setCustomizable(as.getCustomizable());
		}

		getTransientPropertyMap().putAll(g.getTransientPropertyMap());
	}

	/** Whether this style should be customizable in a preferences panel.
	 *  Sometimes there are temporary styles created where some of the options
	 *  simply don't make sense and shouldn't be shown to the user in the
	 *  customization panel.
	 */
	public final void setCustomizable(boolean b) {
		// Another option instead of a single set/getCustomizable flag would be
		// to have a bunch of individual flags: getSeparable(), getHumanNamable(),
		// getHasMaxDepth(), etc....
		customizable = b;
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
}
