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
import java.util.prefs.*;

public class TierPreferenceState {
  
  static Preferences tiers_root_node = UnibrowPrefsUtil.getTopNode().node("tiers");

  // The String constants named PREF_* are for use in the persistent preferences
  // They are not displayed to users, and should never change
  static final String PREF_SHOW = "Show";
  static final String PREF_SEPARATE = "Separate Tiers";
  static final String PREF_COLLAPSED = "Collaped";
  static final String PREF_MAX_DEPTH = "Max Depth";
  static final String PREF_SORT_ORDER = "Sort Order";
  static final String PREF_COLOR = "Color";

  static final boolean default_show = true;
  static final boolean default_separate = true;
  static final boolean default_collapsed = false;
  static final int default_max_depth = 0;
  static final int default_sort_order = 100;
  static final Color default_color = Color.GREEN;

  public static final int MAX_SORT_ORDER = 1000;
  public static final int MAX_MAX_DEPTH = Integer.MAX_VALUE;
  
  boolean show;
  boolean separate;
  boolean collapsed;
  int max_depth;
  int sort_order;
  Color color;
  
  String tier_name;
  Preferences node;

  /** Creates a new TierPreferenceState associated with the tier_name.
   *  Tier name preferences are stored in a case-insensitive way.
   */
  public TierPreferenceState(String tier_name) {
    this.tier_name = tier_name;
    
    this.node = tiers_root_node.node(this.tier_name.toLowerCase());

    separate = node.getBoolean(PREF_SEPARATE, default_separate);
    show = node.getBoolean(PREF_SHOW, default_show);
    collapsed = node.getBoolean(PREF_COLLAPSED, default_collapsed);
    max_depth = node.getInt(PREF_MAX_DEPTH, default_max_depth);
    sort_order = node.getInt(PREF_SORT_ORDER, default_sort_order);
    color = UnibrowPrefsUtil.getColor(node, PREF_COLOR, default_color);
  }
  
//  static TierPreferenceState default_tier_preference_state = null;
//  
//  // EEE - I'm not sure whether I really want to use this defualt object for anything.
//  /* Gets an instance of TierPreferenceState that can be used for holding
//   *  default values.  As currently written, this does NOT have any effect
//   *  on new instances of this class.
//   */
//  public static TierPreferenceState defaultTierPreferenceState() {
//    if (default_tier_preference_state == null) {
//      default_tier_preference_state = new TierPreferenceState("* DEFAULT *");
//    }
//    return default_tier_preference_state;
//  }
  
  public String getTierName() {
    return tier_name;
  }
  
  /** Whether the tier is shown or hidden. */
  public Boolean getShow() {
    return Boolean.valueOf(show);
  }
  
  public void setShow(boolean b) {
    this.show = b;
    node.putBoolean(PREF_SHOW, b);
  }
  
  
  /** Whether PLUS and MINUS strand should be in separate tiers. */
  public Boolean getSeparate() {
    return Boolean.valueOf(separate);
  }
  
  public void setSeparate(boolean b) {
    this.separate = b;
    node.putBoolean(PREF_SEPARATE, b);
  }
  
  /** Whether tier is collapsed. */
  public Boolean getCollapsed() {
    return Boolean.valueOf(collapsed);
  }
  
  public void setCollapsed(boolean b) {
    this.collapsed = b;
    node.putBoolean(PREF_COLLAPSED, b);
  }
  
  /** Maximum number of rows of annotations for this tier. */
  public int getMaxDepth() {
    return max_depth;
  }
  
  /** Set the maximum number of rows of annotations for this tier. 
   *  Any attempt to set this less than zero or larger than MAX_MAX_DEPTH will 
   *  fail, the value will be truncated to fit the range.
   *  @param order  a number between 0 and {@link #MAX_MAX_DEPTH}.
   */
  public void setMaxDepth(int max) {
    if (max < 0) { max = 0; }
    if (max > MAX_MAX_DEPTH) { max = MAX_MAX_DEPTH; }
    this.max_depth = max;
    node.putInt(PREF_MAX_DEPTH, max);
  }
  
  /** Order for sorting tiers; lower numbers are closer to the axis. */
  public int getSortOrder() {
    return sort_order;
  }
  
  /** Set the order for sorting tiers; lower numbers are closer to the axis. 
   *  Any attempt to set this less than zero or larger than MAX_SORT_ORDER will 
   *  fail, the value will be truncated to fit the range.
   *  @param order  a number between 0 and {@link #MAX_SORT_ORDER}.
   */
  public void setSortOrder(int order) {
    if (order < 0) { order = 0; }
    if (order > MAX_SORT_ORDER) { order = MAX_SORT_ORDER; }
    this.sort_order = order;
    node.putInt(PREF_SORT_ORDER, order);
  }
  
  /** The color of annotations in the tier. */
  public Color getColor() {
    return color;
  }

  public void setColor(Color c) {
    this.color = c;
    UnibrowPrefsUtil.putColor(node, PREF_COLOR, c);
  }
}
