/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.util.ObjectUtils;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PluginInfo {

  public static final String NODE_PLUGINS = "plugins";

  public static final String KEY_PLACEMENT = "placement";
  public static final String KEY_LOAD = "load";
  public static final String KEY_CLASS = "class";
  public static final String KEY_DISPLAY_NAME = "display name";

  public static final String PLACEMENT_TAB = "tab";
  public static final String PLACEMENT_WINDOW = "window";

  String class_name;// required
  String plugin_name;// required
  boolean load;// required

  String placement = PLACEMENT_TAB;
  String display_name = null; // display name is optional

  public PluginInfo(String class_name, String plugin_name, boolean load) {
    this.class_name = class_name;
    this.plugin_name = plugin_name;
    // If the plugin_name is null, it will try to set itself to the
    // class name, with the package name removed.
    if (plugin_name == null || plugin_name.trim().equals("") && class_name != null) {
      this.plugin_name = class_name;
      int index = plugin_name.lastIndexOf('.');
      if (index >= 0) {
        plugin_name = plugin_name.substring(index);
      }
    }
    this.load = load;
    this.placement = PLACEMENT_TAB;
    this.display_name = this.plugin_name;
  }

  public String toString() {
    return "IGB PluginInfo: " +
        "name = " + plugin_name + ", class = " + class_name + ", load = " + load;
  }


  public String getClassName() { return class_name; }
  public String getPluginName() { return plugin_name; }
  public boolean shouldLoad() { return load; }

  /** Returns either {@link #PLACEMENT_WINDOW} or {@link #PLACEMENT_TAB}. */
  public String getPlacement() { return placement; }

  /** Set to either {@link #PLACEMENT_WINDOW} or {@link #PLACEMENT_TAB}.
   *  @throws IllegalArgumentException if not one of the acceptable choices
   */
  public void setPlacement(String s) {
    if (PLACEMENT_WINDOW.equals(s) || PLACEMENT_TAB.equals(s)) {
      this.placement = s;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /** It is possible to set a display name that is different from the real name. */
  public void setDisplayName(String s) {
    this.display_name = s;
  }

  public String getDisplayName() {
    if (this.display_name == null || this.display_name.trim().length() == 0) {
      return this.plugin_name;
    } else {
      return this.display_name;
    }
  }

  public static String[] getAllPluginNames() throws BackingStoreException {
    createStandardPlugins();
    String[] names = UnibrowPrefsUtil.getTopNode().node(NODE_PLUGINS).childrenNames();

    return names;
  }

  /** Returns a List of PluginInfo's. */
  public static List getAllPlugins() throws BackingStoreException {
    String[] names = getAllPluginNames();
    Vector list = new Vector(names.length);
    for (int i=0; i<names.length; i++) {
      PluginInfo pi = getPluginInfoForName(names[i]);
      list.add(pi);
    }
    Comparator comp = new Comparator() {
      // This comparator simply makes sure QuickLoad is listed first
      public int compare(Object a, Object b) {
        PluginInfo pa = (PluginInfo) a;
        PluginInfo pb = (PluginInfo) b;
        if (QuickLoaderView.class.getName().equals(pa.getClassName())) { return -1; }
        else if (QuickLoaderView.class.getName().equals(pb.getClassName())) { return 1; }
        return 0;
      }
    };

    Collections.sort(list, comp);
    return list;
  }

  public static Preferences getNodeForName(String name) {
    Preferences node = UnibrowPrefsUtil.getTopNode().node(NODE_PLUGINS).node(name);
    return node;
  }

  public static PluginInfo getPluginInfoForName(String name) {
    Preferences node = getNodeForName(name);
    return getPluginInfoFromNode(node);
  }

  public static PluginInfo getPluginInfoFromNode(Preferences node) {
    String plugin_name = node.name();
    String class_name = node.get(KEY_CLASS, null);
    boolean load = node.getBoolean(KEY_LOAD, (class_name != null));

    PluginInfo info = new PluginInfo(class_name, plugin_name, load);

    String placement = node.get(KEY_PLACEMENT, PLACEMENT_TAB);
    info.setPlacement(placement);
    info.setDisplayName(node.get(KEY_DISPLAY_NAME, null));

    return info;
  }

  public Object instantiatePlugin() {
    return instantiatePlugin(this.class_name);
  }

  public static Object instantiatePlugin(String class_name) {

    Object plugin = null;
    try {
      plugin = ObjectUtils.classForName(class_name).newInstance();
    } catch (Exception e) {
      ErrorHandler.errorPanel("Could not instantiate plugin\n"+
       "class name: '"+class_name+"'\n", e);
    }
    return plugin;
  }

  public void persist() {
    if ((this.plugin_name == null) || (this.class_name == null)) {
      System.out.println("Cannot persist PluginInfo with null name or class name: "+this.toString());
      return;
    }

    Preferences node = getNodeForName(this.plugin_name);
    node.put(KEY_CLASS, this.class_name);
    node.putBoolean(KEY_LOAD, this.load);
    node.put(KEY_PLACEMENT, this.placement);
    if (this.display_name == null) {
      node.remove(KEY_DISPLAY_NAME);
    } else {
      node.put(KEY_DISPLAY_NAME, this.display_name);
    }
  }

  // Creates preference nodes for all the standard plugins.
  // In addition to saving the user from having to type these in,
  // it makes sure that necessary plugins are always present
  public static void createStandardPlugins() {
     // The "QuickLoad" plugin is absolutely required, so always force "load" to "true"
     PluginInfo.getNodeForName("QuickLoad").put(KEY_CLASS, QuickLoaderView.class.getName());
     PluginInfo.getNodeForName("QuickLoad").putBoolean(KEY_LOAD, true);

     PluginInfo.getNodeForName("Selection Info").put(KEY_CLASS, SymTableView.class.getName());

     PluginInfo.getNodeForName("Sliced View").put(KEY_CLASS, AltSpliceView.class.getName());
     PluginInfo.getNodeForName("Graph Adjuster").put(KEY_CLASS, GraphAdjusterView.class.getName());
     PluginInfo.getNodeForName("Pattern Search").put(KEY_CLASS, SeqSearchView.class.getName());
     PluginInfo.getNodeForName("Bookmarks").put(KEY_CLASS, BookmarkManagerView.class.getName());
     PluginInfo.getNodeForName("Pivot View").put(KEY_CLASS, ExperimentPivotView.class.getName());
     PluginInfo.getNodeForName("Annotation Browser").put(KEY_CLASS, AnnotBrowserView.class.getName());
     PluginInfo.getNodeForName("Restriction Sites").put(KEY_CLASS, RestrictionControlView.class.getName());
  }
}
