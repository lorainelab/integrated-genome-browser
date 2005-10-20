/**
*   Copyright (c) 2005 Affymetrix, Inc.
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

import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.awt.Color;
import java.util.*;
import java.util.prefs.*;

public class AnnotStyle {
  
  static Preferences tiers_root_node = UnibrowPrefsUtil.getTopNode().node("tiers");

  static public final String NAME_OF_DEFAULT_INSTANCE = "* DEFAULT *";
  
  // The String constants named PREF_* are for use in the persistent preferences
  // They are not displayed to users, and should never change
  static final String PREF_SHOW = "Show";
  static final String PREF_SEPARATE = "Separate Tiers";
  static final String PREF_COLLAPSED = "Collapsed";
  static final String PREF_MAX_DEPTH = "Max Depth";
  static final String PREF_COLOR = "Color";
  static final String PREF_BACKGROUND = "Background";
  static final String PREF_HUMAN_NAME = "Human Name";
  static final String PREF_LABEL_FIELD = "Label Field";
  static final String PREF_GLYPH_DEPTH = "Glyph Depth";

  static final boolean default_show = true;
  static final boolean default_separate = true;
  static final boolean default_collapsed = false;
  static final int default_max_depth = 0;
  static final Color default_color = Color.GREEN;
  static final Color default_background = Color.BLACK;
  static final String default_label_field = "";
  static final int default_glyph_depth = 2;  
  public static final int MAX_MAX_DEPTH = Integer.MAX_VALUE;
  
  // whether to create and use a java Preferences node object for this instance
  boolean is_persistent = true;
  
  boolean show = default_show;
  boolean separate = default_separate;
  boolean collapsed = default_collapsed;
  int max_depth = default_max_depth;
  Color color = default_color;
  Color background = default_background;
  String label_field = default_label_field;
  int glyph_depth = default_glyph_depth;  
  
  String unique_name;
  String human_name;
  
  Preferences node;

  static Map static_map = new LinkedHashMap();
  
  public static AnnotStyle getInstance(String unique_name) {
    return getInstance(unique_name, true);
  }
  
  public static AnnotStyle getInstance(String unique_name, boolean persistent) {
    AnnotStyle style = (AnnotStyle) static_map.get(unique_name);
    if (style == null) {
      AnnotStyle template = getDefaultInstance();
      static_map.put(template.getUniqueName(), template);
      style = new AnnotStyle(unique_name, persistent, template);
    }
    static_map.put(unique_name, style);
    return style;
  }
  
  public static java.util.List getAllLoadedInstances() {
    return new ArrayList(static_map.values());
  }
    
  /** Gets the parent preference node in which all the tier preferences are stored. */
  public static Preferences getTiersRootNode() {
    return tiers_root_node;
  }
  
  protected AnnotStyle() {
  }
    
  /** Creates an instance associated with a case-insensitive form of the unique name.
   */
  protected AnnotStyle(String unique_name, boolean is_persistent, AnnotStyle template) {
    if (is_persistent && unique_name.length() > Preferences.MAX_NAME_LENGTH) {
      unique_name = unique_name.substring(0,Preferences.MAX_NAME_LENGTH);
      System.out.println("!!!  Preferences node name trimmed to '"+unique_name+"'");
    }

    this.human_name = unique_name; // this is the default human name, and is not lower case
    this.unique_name = unique_name.toLowerCase();
    this.is_persistent = is_persistent;
    
    
    if (template != null) {
      initFromTemplate(template);
    }
    applyHardCodedDefaults();
    if (is_persistent) {
      node = tiers_root_node.node(this.unique_name);
      initFromNode(node);
    } else {
      node = null;
    }
  }
  
  // Apply a few hard-coded defaults
  // This is a hack, but helps ease the transition away from the igb_default_prefs.xml file
  void applyHardCodedDefaults() {
    if ("contig".equals(unique_name) || "contigs".equals(unique_name) 
        || "repeats".equals(unique_name) || "repeat".equals(unique_name)
        || "encode regions".equals(unique_name) || "encoderegions".equals(unique_name) || "encode".equals(unique_name)) {
      this.glyph_depth = 1;
    }
    else if ("refseq".equals(unique_name)) {
      this.label_field = "gene_name";
    }
  }
  
  // Copies properties from the given node, using the currently-loaded values as defaults.
  // generally call initFromTemplate before this.
  // Make sure to set human_name to some default before calling this.
  // Properties set this way do NOT get put in persistent storage.
  void initFromNode(Preferences node) {
    human_name = node.get(PREF_HUMAN_NAME, this.human_name);
    //factory_instance = null;
    
    separate = node.getBoolean(PREF_SEPARATE, this.getSeparate());
    show = node.getBoolean(PREF_SHOW, this.getShow());
    collapsed = node.getBoolean(PREF_COLLAPSED, this.getCollapsed());
    max_depth = node.getInt(PREF_MAX_DEPTH, this.getMaxDepth());
    color = UnibrowPrefsUtil.getColor(node, PREF_COLOR, this.getColor());
    background = UnibrowPrefsUtil.getColor(node, PREF_BACKGROUND, this.getBackground());
  
    label_field = node.get(PREF_LABEL_FIELD, this.getLabelField());
    glyph_depth = node.getInt(PREF_GLYPH_DEPTH, this.getGlyphDepth());
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
    max_depth = template.getMaxDepth();
    color = template.getColor();
    background = template.getBackground();
  
    label_field = template.getLabelField();
    glyph_depth = template.getGlyphDepth();
  }
  
  // Returns the preferences node, or null if this is a non-persistent instance.
  Preferences getNode() {
    return this.node;
    // return (is_persistent ? tiers_root_node.node(this.unique_name) : null);
  }
    
  static AnnotStyle default_instance = null;
  
  /* Gets an instance that can be used for holding
   *  default values.  As currently written, this does NOT have any effect
   *  on new instances of this class.
   */
  public static AnnotStyle getDefaultInstance() {
    if (default_instance == null) {
      default_instance = new AnnotStyle(NAME_OF_DEFAULT_INSTANCE, true, null);
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
      getNode().put(PREF_HUMAN_NAME, human_name);
    }
  }
  
  /** Whether the tier is shown or hidden. */
  public boolean getShow() {
    return show;
  }
  
  public void setShow(boolean b) {
    this.show = b;
    if (getNode() != null) {
      getNode().putBoolean(PREF_SHOW, b);
    }
  }
  
  
  /** Whether PLUS and MINUS strand should be in separate tiers. */
  public boolean getSeparate() {
    return separate;
  }
  
  public void setSeparate(boolean b) {
    this.separate = b;
    if (getNode() != null) {
      getNode().putBoolean(PREF_SEPARATE, b);
    }
  }
  
  /** Whether tier is collapsed. */
  public boolean getCollapsed() {
    return collapsed;
  }
  
  public void setCollapsed(boolean b) {
    this.collapsed = b;
    if (getNode() != null) {
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
      getNode().putInt(PREF_MAX_DEPTH, max);
    }
  }
    
  /** The color of annotations in the tier. */
  public Color getColor() {
    return color;
  }

  public void setColor(Color c) {
    this.color = c;
    if (getNode() != null) {
      UnibrowPrefsUtil.putColor(getNode(), PREF_COLOR, c);
    }
  }
  
  /** The color of the tier Background. */
  public Color getBackground() {
    return background;
  }

  public void setBackground(Color c) {
    this.background = c;
    if (getNode() != null) {
      UnibrowPrefsUtil.putColor(getNode(), PREF_BACKGROUND, c);
    }
  }
    
  public String getLabelField() {
    return label_field;
  }
  
  public void setLabelField(String l) {
    if (l == null || l.trim().length() == 0) { l = ""; }
    label_field = l;
    if (getNode() != null) {
      getNode().put(PREF_LABEL_FIELD, l);
    }
  }
  
  public int getGlyphDepth() {
    return glyph_depth;
  }
  
  public void setGlyphDepth(int i) {
    glyph_depth = i;
    if (getNode() != null) {
      getNode().putInt(PREF_GLYPH_DEPTH, i);
    }
  }
  
  public boolean getPersistent() {
    return (is_persistent && getNode() != null);
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
