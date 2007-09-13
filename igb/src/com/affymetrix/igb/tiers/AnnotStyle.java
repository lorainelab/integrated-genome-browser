
/**   Copyright (c) 2005-2007 Affymetrix, Inc.
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
package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.style.*;
import java.awt.Color;
import java.util.*;
import java.util.prefs.*;
import java.util.regex.Pattern;

import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.stylesheet.AssociationElement;
import com.affymetrix.igb.stylesheet.Stylesheet;
import com.affymetrix.igb.stylesheet.PropertyMap;

/**
 *
 *  When setting up an AnnotStyle, want to prioritize:
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
public class AnnotStyle implements IAnnotStyleExtended {

  static Preferences tiers_root_node = UnibrowPrefsUtil.getTopNode().node("tiers");

  // A pattern that matches two or more slash "/" characters.
  // A preference node name can't contain two slashes, nor end with a slash.
  static final Pattern multiple_slashes = Pattern.compile("/{2,}");

  static public final String NAME_OF_DEFAULT_INSTANCE = "* DEFAULT *";

  // The String constants named PREF_* are for use in the persistent preferences
  // They are not displayed to users, and should never change
  //static final String PREF_SHOW = "Show";
  static final String PREF_SEPARATE = "Separate Tiers";
  static final String PREF_COLLAPSED = "Collapsed";
  static final String PREF_MAX_DEPTH = "Max Depth";
  static final String PREF_COLOR = "Color";
  static final String PREF_BACKGROUND = "Background";
  static final String PREF_HUMAN_NAME = "Human Name";
  static final String PREF_LABEL_FIELD = "Label Field";
  static final String PREF_GLYPH_DEPTH = "Glyph Depth";
  static final String PREF_HEIGHT = "Height"; // height per glyph? // linear transform value?

  static final boolean default_show = true;
  static final boolean default_separate = true;
  static final boolean default_collapsed = false;
  static final boolean default_expandable = true;
  static final int default_max_depth = 0;
  static final Color default_color = Color.GREEN;
  static final Color default_background = Color.BLACK;
  static final String default_label_field = "";
  static final int default_glyph_depth = 2;
  static final double default_height = 20.0;
  static final double default_y = 0.0;
  public static final int MAX_MAX_DEPTH = Integer.MAX_VALUE;

  public static boolean DEBUG = false;
  public static boolean DEBUG_NODE_PUTS = false;
  // whether to create and use a java Preferences node object for this instance
  boolean is_persistent = true;

  boolean show = default_show;
  boolean separate = default_separate;
  boolean collapsed = default_collapsed;
  boolean expandable = default_expandable;
  int max_depth = default_max_depth;
  Color color = default_color;
  Color background = default_background;
  String label_field = default_label_field;
  int glyph_depth = default_glyph_depth;
  double height = default_height;
  double y = default_y;
  String url = null;

  boolean color_by_score = false;
  HeatMap custom_heatmap = null;

  String unique_name;
  String human_name;

  Preferences node;

  static Map<String,AnnotStyle> static_map = new LinkedHashMap<String,AnnotStyle>();

  public static AnnotStyle getInstance(String unique_name) {
    return getInstance(unique_name, true);
  }

  public static AnnotStyle getInstance(String unique_name, boolean persistent) {
      // if unique_name is null, maybe assign it "unknown" to avoid exceptions
      // if (unique_name == null)  { unique_name = "unknown";}
    AnnotStyle style = static_map.get(unique_name.toLowerCase());
    if (style == null) {
      if (DEBUG)  {System.out.println("    (((((((   in AnnotStyle.getInstance() creating AnnotStyle for name: " + unique_name); }
      // apply any default stylesheet stuff
      AnnotStyle template = getDefaultInstance();
      // at this point template should already have all modifications to default applied from stylesheets and preferences nodes (A & B)
      // apply any stylesheet stuff...
      style = new AnnotStyle(unique_name, persistent, template);
      static_map.put(unique_name.toLowerCase(), style);
    }
    return style;
  }

  /** Returns all (persistent and temporary) instances of AnnotStyle. */
  public static List<AnnotStyle> getAllLoadedInstances() {
    return new ArrayList<AnnotStyle>(static_map.values());
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

  /** Gets the parent preference node in which all the tier preferences are stored. */
  public static Preferences getTiersRootNode() {
    return tiers_root_node;
  }

  protected AnnotStyle() {
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
  protected AnnotStyle(String name, boolean is_persistent, AnnotStyle template) {
    this.human_name = name; // this is the default human name, and is not lower case
    this.unique_name = name.toLowerCase();
    this.is_persistent = is_persistent;

    if (is_persistent) {
      if (unique_name.endsWith("/")) {
        unique_name = unique_name.substring(0, unique_name.length() -1);
      }
      unique_name = multiple_slashes.matcher(unique_name).replaceAll("/");
      if (unique_name.length() > Preferences.MAX_NAME_LENGTH) {
        unique_name = unique_name.substring(0, Preferences.MAX_NAME_LENGTH);
        //System.out.println("Preferences node name trimmed to '"+unique_name+"'");
      }
    }

    if (template != null) {
        // calling initFromTemplate should take care of A) and B)
      initFromTemplate(template);
    }

    // GAH eliminated hard-coded default settings for glyph_depth, can now set in stylesheet
    //    applyHardCodedDefaults();  

    // now need to add use of stylesheet settings via AssociationElements, etc.
    Stylesheet stylesheet = XmlStylesheetParser.getUserStylesheet();
    AssociationElement assel = stylesheet.getAssociationForType(name);
    if (assel == null)  { assel = stylesheet.getAssociationForMethod(name); }
    if (assel != null)  {
        PropertyMap props = assel.getPropertyMap();
        if (props != null)  {
            initFromPropertyMap(props);
        }
    }
    if (is_persistent) {
      try {
        node = UnibrowPrefsUtil.getSubnode(tiers_root_node, this.unique_name);
      } catch (Exception e) {
        // if there is a problem creating the node, continue with a non-persistent style.
        System.out.println("Exception: " + e);
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
  void initFromNode(Preferences node) {
    if (DEBUG)  { System.out.println("    ----------- called AnnotStyle.initFromNode() for: " + unique_name); }
    human_name = node.get(PREF_HUMAN_NAME, this.human_name);

    separate = node.getBoolean(PREF_SEPARATE, this.getSeparate());
    //show = node.getBoolean(PREF_SHOW, this.getShow());
    collapsed = node.getBoolean(PREF_COLLAPSED, this.getCollapsed());
    max_depth = node.getInt(PREF_MAX_DEPTH, this.getMaxDepth());
    color = UnibrowPrefsUtil.getColor(node, PREF_COLOR, this.getColor());
    background = UnibrowPrefsUtil.getColor(node, PREF_BACKGROUND, this.getBackground());

    label_field = node.get(PREF_LABEL_FIELD, this.getLabelField());
    glyph_depth = node.getInt(PREF_GLYPH_DEPTH, this.getGlyphDepth());
    //    setGlyphDepth(node.getInt(PREF_GLYPH_DEPTH, this.getGlyphDepth()));
  }

  // Copies selected properties from a PropertyMap into this object, but does NOT persist
  // these copied values -- if values were persisted, then if PropertyMap changed between sessions,
  //      older values would override newer values since persisted nodes take precedence
  //    (only want to persists when user sets preferences in GUI)
   void initFromPropertyMap(PropertyMap props)  {

     if (DEBUG)  { 
       System.out.println("    +++++ initializing AnnotStyle from PropertyMap: " + unique_name); 
       System.out.println("             props: " + props); 
     }
      Color col = props.getColor("color");
      if (col == null)  { col = props.getColor("foreground"); }
      if (col != null)  { color = col; }
      Color bgcol = props.getColor("background");
      if (bgcol != null)  { background = bgcol; }

      String gdepth_string = (String)props.getProperty("glyph_depth");
      if (gdepth_string != null) {
	int prev_glyph_depth = glyph_depth;
	try { glyph_depth = Integer.parseInt(gdepth_string); }
	catch (Exception ex)  { glyph_depth = prev_glyph_depth; }
      }
      String labfield = (String)props.getProperty("label_field");
      if (labfield != null) { label_field = labfield; }

      String mdepth_string = (String)props.getProperty("max_depth");
      if (mdepth_string != null)  {
          int prev_max_depth = max_depth;
          try { max_depth = Integer.parseInt(mdepth_string); }
          catch (Exception ex)  { max_depth = prev_max_depth; }
      }

      String sepstring = (String)props.getProperty("separate");
      if (sepstring != null)  {
	if (sepstring.equalsIgnoreCase("false"))  { separate = false; }
	else if (sepstring.equalsIgnoreCase("true")) { separate = true; }
      }
      String showstring = (String)props.getProperty("show");
      if (showstring != null) {
	if (showstring.equalsIgnoreCase("false"))  { show = false; }
	else if (showstring.equalsIgnoreCase("true")) { show = true; }
      }
      String collapstring = (String)props.getProperty("collapsed");
      if (collapstring != null) {
	if (collapstring.equalsIgnoreCase("false"))  { collapsed = false; }
	else if (collapstring.equalsIgnoreCase("true")) { collapsed= true; }
      }
      if (DEBUG) { System.out.println("    +++++++  done initializing from PropertyMap"); }
      // height???
  }

  // Copies properties from the template into this object, but does NOT persist
  // these copied values.
  // human_name and factory_instance are not modified
  void initFromTemplate(AnnotStyle template) {
    //human_name = this.unique_name;
    //factory_instance = null;
    separate = template.getSeparate();
    show = template.getShow();
    collapsed = template.getCollapsed();
    max_depth = template.getMaxDepth();  // max stacking of annotations
    color = template.getColor();
    background = template.getBackground();
    label_field = template.getLabelField();
    glyph_depth = template.getGlyphDepth();  // depth of visible glyph tree
    // setGlyphDepth(template.getGlyphDepth());
  }

  // Returns the preferences node, or null if this is a non-persistent instance.
  Preferences getNode() {
    return this.node;
    // return (is_persistent ? tiers_root_node.node(this.unique_name) : null);
  }

  static AnnotStyle default_instance = null;

  /* Gets an instance that can be used for holding
   *  default values.  The default instance is used as a template in creating
   *  new instances.  (Although not ALL properties of the default instance are used
   *  in this way.)
   */
  public static AnnotStyle getDefaultInstance() {
    if (default_instance == null) {
      default_instance = new AnnotStyle(NAME_OF_DEFAULT_INSTANCE, true, null);
      default_instance.setGlyphDepth(2);
      default_instance.setHumanName("");
      default_instance.setShow(true);
      default_instance.setLabelField("");
      default_instance.setMaxDepth(4);
      // Note that name will become lower-case
      static_map.put(default_instance.unique_name, default_instance);
    }
    return default_instance;
  }

  public String getUniqueName() {
    return unique_name;
  }

  /** Gets a name that may be shorter and more user-friendly than the unique name.
   *  The human-readable name may contain upper- and lower-case characters.
   *  The default is equivalent to the unique name.
   */
  public String getHumanName() {
    if (human_name == null || human_name.trim().length() == 0) {
      human_name = unique_name;
    }
    return this.human_name;
  }

  public void setHumanName(String human_name) {
    this.human_name = human_name;
    if (getNode() != null) {
      if (DEBUG_NODE_PUTS) { System.out.println("   %%%%% node.put() in AnnotStyle.setHumanName(): " + human_name); }
      getNode().put(PREF_HUMAN_NAME, human_name);
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
  public boolean getSeparate() {
    return separate;
  }

  public void setSeparate(boolean b) {
    this.separate = b;
    if (getNode() != null) {
      if (DEBUG_NODE_PUTS) { System.out.println("   %%%%% node.put() in AnnotStyle.setSeparate(): " + human_name + ", " + b); }
      getNode().putBoolean(PREF_SEPARATE, b);
    }
  }

  public boolean getCustomizable() {
    return customizable;
  }

  /** Whether tier is collapsed. */
  public boolean getCollapsed() {
    return collapsed;
  }

  public void setCollapsed(boolean b) {
    this.collapsed = b;
    if (getNode() != null) {
      if (DEBUG_NODE_PUTS) { System.out.println("   %%%%% node.put() in AnnotStyle.setCollapsed(): " + human_name + ", " + b); }
      getNode().putBoolean(PREF_COLLAPSED, b);
    }
  }

  /** Maximum number of rows of annotations for this tier. */
  public int getMaxDepth() {
    return max_depth;
  }

  /** Set the maximum number of rows of annotations for this tier.
   *  Any attempt to set this less than zero or larger than MAX_MAX_DEPTH will
   *  fail, the value will be truncated to fit the range.
   *  @param max  a number between 0 and {@link #MAX_MAX_DEPTH}.
   */
  public void setMaxDepth(int max) {
    if (max < 0) { max = 0; }
    if (max > MAX_MAX_DEPTH) { max = MAX_MAX_DEPTH; }
    this.max_depth = max;
    if (getNode() != null) {
      if (DEBUG_NODE_PUTS) { System.out.println("   %%%%% node.put() in AnnotStyle.setMaxDepth(): " + human_name + ", " + max); }
      getNode().putInt(PREF_MAX_DEPTH, max);
    }
  }

  /** The color of annotations in the tier. */
  public Color getColor() {
    return color;
  }

  public void setColor(Color c) {
    if (c != this.color) {
      custom_heatmap = null;
      // get rid of old heatmap, force it to be re-created when needed
    }
    this.color = c;
    if (getNode() != null) {
      if (DEBUG_NODE_PUTS) { System.out.println("   %%%%% node.put() in AnnotStyle.setColor(): " + human_name + ", " + c); }
      UnibrowPrefsUtil.putColor(getNode(), PREF_COLOR, c);
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
      if (DEBUG_NODE_PUTS) { System.out.println("   %%%%% node.put() in AnnotStyle.setBackground(): " + human_name + ", " + c); }
      UnibrowPrefsUtil.putColor(getNode(), PREF_BACKGROUND, c);
    }
  }


  /** Returns the field name from which the glyph labels should be taken.
   *  This will never return null, but will return "" instead.
   */
  public String getLabelField() {
    return label_field;
  }

  public void setLabelField(String l) {
    if (l == null || l.trim().length() == 0) { l = ""; }
    label_field = l;
    if (getNode() != null) {
      if (DEBUG_NODE_PUTS) { System.out.println("   %%%%% node.put() in AnnotStyle.setLabelField(): " + human_name + ", " + l); }
      getNode().put(PREF_LABEL_FIELD, l);
    }
  }

  public int getGlyphDepth() {
    return glyph_depth;
  }

  public void setGlyphDepth(int i) {
    if (glyph_depth != i) {
      glyph_depth = i;
      if (getNode() != null) {
	if (DEBUG_NODE_PUTS) { System.out.println("   %%%%% node.put() in AnnotStyle.setGlyphDepth(): " + human_name + ", " + i); }
	getNode().putInt(PREF_GLYPH_DEPTH, i);
      }
    }
  }

  public double getHeight() {
    return height;
  }

  public void setHeight(double h) {
    height = h;
    if (getNode() != null) {
      if (DEBUG_NODE_PUTS) { System.out.println("   %%%%% node.put() in AnnotStyle.setHeight(): " + human_name + ", " + h); }
      getNode().putDouble(PREF_HEIGHT, h);
    }
  }

  /** Currently not used, but could be used to remember tier positions. */
  public void setY(double y) {
    this.y = y;
  }

  /** Currently not used, but could be used to remember tier positions. */
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

  public boolean getPersistent() {
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

  boolean is_graph = false;

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

  Map<String,Object> transient_properties;

  public Map<String,Object> getTransientPropertyMap() {
    if (transient_properties == null) {
      transient_properties = new HashMap<String,Object>();
    }
    return transient_properties;
  }

  /**
   *  Key for the transient property map. Indicates whether the annotations
   *  might possibly have scores that could be used for coloring.
   *  Valid values are "true" and "false".
   */
  public static final String PROP_HAS_SCORES = "HAS_SCORES";

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
   *  Returns a HeatMap that interpolates between colors based on
   *  getColor() and getBackgroundColor().  The color at the low
   *  end of the HeatMap will be slightly different from the background
   *  color so that it can be distinguished from it.
   *  This will return a HeatMap even if getColorByScore() is false.
   */
  HeatMap getCustomHeatMap() {
    if (custom_heatmap == null) {
      // Bottom color is not quite same as background, so it remains visible
      Color bottom_color = HeatMap.interpolateColor(getBackground(), getColor(), 0.20f);
      custom_heatmap = HeatMap.makeLinearHeatmap("Custom", bottom_color, getColor());
    }
    return custom_heatmap;
  }

  /**
   *  Returns a color that can be used to indicate a score between 1 and 1000.
   *  This will return a color even if getColorByScore() is false.
   */
  public Color getScoreColor(float score) {
    final float min = 1.0f; // min and max might become variables later...
    final float max = 1000.0f;

    if (score < min) {score = min;}
    else if (score > max) { score = max;}

    final float range = max - min;
    final float norm_score = (score/range);
    int index = (int) ((score/range) * 255);

    return getCustomHeatMap().getColors()[ index ];
  }

  /* (a possibility for later.... EEE)
  Set source_urls = new TreeSet();

  public Set getSourceURLs() {
    return source_urls;
  }
  */

  public void copyPropertiesFrom(IAnnotStyle g) {
    setColor(g.getColor());
    setShow(g.getShow());
    setHumanName(g.getHumanName());
    setBackground(g.getBackground());
    setCollapsed(g.getCollapsed());
    setMaxDepth(g.getMaxDepth());
    setHeight(g.getHeight());
    setY(g.getY());
    setExpandable(g.getExpandable());

    if (g instanceof IAnnotStyleExtended) {
      IAnnotStyleExtended as = (IAnnotStyleExtended) g;
      setColorByScore(as.getColorByScore());
      setGlyphDepth(as.getGlyphDepth());
      setLabelField(as.getLabelField());
      setSeparate(as.getSeparate());
    }
    if (g instanceof AnnotStyle) {
      AnnotStyle as = (AnnotStyle) g;
      setCustomizable(as.getCustomizable());
    }

    getTransientPropertyMap().putAll(g.getTransientPropertyMap());
  }

   boolean customizable = true;

    /** Whether this style should be customizable in a preferences panel.
     *  Sometimes there are temporary styles created where some of the options
     *  simply don't make sense and shouldn't be shown to the user in the
     *  customization panel.
     */
    public void setCustomizable(boolean b) {
      // Another option instead of a single set/getCustomizable flag would be
      // to have a bunch of individual flags: getSeparable(), getHumanNamable(),
      // getHasMaxDepth(), etc....
      customizable = b;
    }

   public String toString() {
    String s = "AnnotStyle: (" + Integer.toHexString(this.hashCode()) + ")"
      + " '"+unique_name+"' ('"+human_name+"') "
      + " persistent: " + is_persistent
      + " color: " + getColor()
      + " bg: " + getBackground()
    ;
    return s;
  }
}
