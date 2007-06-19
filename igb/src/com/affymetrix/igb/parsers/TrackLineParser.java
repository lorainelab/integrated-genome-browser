/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.parsers;

import java.awt.*;
import java.util.*;
import java.util.regex.*;

import com.affymetrix.igb.glyph.GraphStateI;
import com.affymetrix.igb.tiers.AnnotStyle;
import com.affymetrix.igb.tiers.IAnnotStyle;
import com.affymetrix.igb.tiers.IAnnotStyleExtended;

public class TrackLineParser {

  static Pattern line_regex = Pattern.compile("\t");
  static Pattern comma_regex = Pattern.compile(",");

  public static final String NAME="name";
  public static final String COLOR="color";
  public static final String DESCRIPTION="description";
  public static final String VISIBILITY ="visibility";
  public static final String URL="url";
  
  /** Value will be stored in the IAnnotStyle extended properties. */
  public static final String USE_SCORE="usescore";
  
  /** If this property has the value "on" (case-insensitive) and the SeqSymmetry
   *  has a property {@link #ITEM_RGB}, then that color will be used for drawing that
   *  symmetry.  Value is stored in the IAnnotStyle extended properties.
   */
  public static final String ITEM_RGB = "itemrgb";

  /** A pattern that matches things like   <b>aaa=bbb</b> or <b>aaa="bbb"</b>
   *  or even <b>"aaa"="bbb=ccc"</b>.
   */
  static Pattern track_line_parser = Pattern.compile(
     "([\\S&&[^=]]+)"     // Group 1: One or more non-whitespace characters (except =)
    + "="                 //  an equals sign
    + "("                 // Group 2: Either ....
      + "(?:\"[^\"]*\")"  // Any characters (except quote) inside quotes
      + "|"               //    ... or ...
      + "(?:\\S+)"        // Any non-whitespace characters
    + ")");               //    ... end of group 2

  Map track_hash = new TreeMap();

  public TrackLineParser() {}

  public Map getCurrentTrackHash() { return track_hash; }
    
  /**
   *  Convert a color in string representation "RRR,GGG,BBB" into a Color.
   *  Note that this can throw an exception if the String is poorly formatted.
   */
  public static Color reformatColor(String color_string) {
    String[] rgb = comma_regex.split(color_string);
    if (rgb.length == 3) {
        int red = Integer.parseInt(rgb[0]);
        int green = Integer.parseInt(rgb[1]);
        int blue = Integer.parseInt(rgb[2]);
        return new Color(red, green, blue);
    } else {
      return null;
    }
  }
  
  /** If the string starts and ends with '\"' characters, this removes them. */
  public static final String unquote(String str) {
    int length = str.length();
    if (length>1 && str.charAt(0)=='\"' && str.charAt(length-1)=='\"') {
      return str.substring(1, length-1);
    }
    else {
      return str;
    }
  }
  
  /** Parses a track line putting the keys and values into the current value
   *  of getCurrentTrackHash(), but does not use these properties to change 
   *  any settings of AnnotStyle, etc.
   *  The Map is returned and is also available as {@link #getCurrentTrackHash()}.
   *  Any old values are cleared from the existing track line hash first.
   */
  public Map parseTrackLine(String track_line) {
    track_hash.clear();
    
    Matcher matcher = track_line_parser.matcher(track_line);
    // If performance becomes important, it is possible to save and re-use a Matcher,
    // but it isn't thread-safe
    while (matcher.find()) {
      if (matcher.groupCount() == 2) {
        String tag = unquote(matcher.group(1).toLowerCase().trim());
        String val = unquote(matcher.group(2));
        track_hash.put(unquote(tag), unquote(val));
      } else {
        // We will only get here if the definition of track_line_parser has been messed with
        System.out.println("Couldn't parse this part of the track line: "+matcher.group(0));
      }
    }
        
    return track_hash;
  }

  /**
   *  Creates an instance of AnnotStyle based on the given track hash.
   *  A default track name must be provided in case none is specified by the
   *  track line itself.
   */
  public static IAnnotStyle createAnnotStyle(Map track_hash, String default_track_name) {
    String name = (String) track_hash.get(NAME);
    if (name == null) {
      track_hash.put(NAME, default_track_name);
      name = default_track_name;
    }

    AnnotStyle style = AnnotStyle.getInstance(name, false); // should the style be persistent?
    applyTrackProperties(track_hash, style);
    return style;
  }
    
  /**
   *  Copies the properties, such as color, into a given IAnnotStyle.
   *  (For a graph, the IAnnotStyle will be an instance of DefaultIAnnotStyle,
   *   for a non-graph, it will be an instance of AnnotStyle.)
   */
  public static void applyTrackProperties(Map track_hash, IAnnotStyle style) {
    //System.out.println("setting track properties from: "+track_line);
    
    String description = (String) track_hash.get(DESCRIPTION);
    if (description != null) {
      style.setHumanName(description);
    } else {
      // Unless we explicitly set the human name, it will be the lower-case
      // version of the name used in AnnotStyle.getInstance().
      // Explicitly setting the name keeps the case intact.
      String name = (String) track_hash.get(NAME);
      if (name != null) {
        style.setHumanName(name);
      }
    }
    String visibility = (String) track_hash.get(VISIBILITY);
    
    String color_string = (String) track_hash.get(COLOR);
    if (color_string != null) {
      Color color = reformatColor(color_string);
      if (color != null) {
        style.setColor(color);
      }
    }
    
    java.util.List collapsed_modes = Arrays.asList(new String[] {"1", "dense"});
    java.util.List expanded_modes = Arrays.asList(new String[]
    {"2", "full", "3", "pack", "4", "squish"});
    
    if (visibility != null) {
      // 0 - hide, 1 - dense, 2 - full, 3 - pack, and 4 - squish.
      // The numerical values or the words can be used, i.e. full mode may be
      // specified by "2" or "full". The default is "1".
      if (collapsed_modes.contains(visibility)) {
        style.setCollapsed(true);
      } else if (expanded_modes.contains(visibility)) {
        style.setCollapsed(false);
      }
    }
    
    if (style instanceof IAnnotStyleExtended) { // for non-graph tiers
      IAnnotStyleExtended annot_style = (IAnnotStyleExtended) style;
      String url = (String) track_hash.get(URL);
      if (url != null) {
        annot_style.setUrl(url);
      }
      if ("1".equals(track_hash.get(USE_SCORE))) {
        annot_style.setColorByScore(true);
      } else if (track_hash.get(USE_SCORE) != null) {
        annot_style.setColorByScore(false);
      }
    }
    
    // Probably shouldn't copy ALL keys to the extended values
    // since some are already included in the standard values above
    Iterator iter = track_hash.keySet().iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      Object value = track_hash.get(key);
      style.getTransientPropertyMap().put(key, value);
    }
  }

  /**
   *  Applies the UCSC track properties that it understands to the GraphState 
   *  object.  Understands: "viewlimits", "graphtype" = "bar" or "points".
   */
  public static void applyTrackProperties(Map track_hash, GraphStateI gstate) {
    applyTrackProperties(track_hash, gstate.getTierStyle());
    
    String view_limits = (String) track_hash.get("viewlimits");
    if (view_limits != null) {
      String[] limits = view_limits.split(":");
      if (limits.length == 2) {
        float min = Float.parseFloat(limits[0]);
        float max = Float.parseFloat(limits[1]);
        gstate.setVisibleMinY(min);
        gstate.setVisibleMaxY(max);
      }
    }
    
    String graph_type = (String) track_hash.get("graphtype");
    // UCSC browser supports only the types "points" and "bar"
    if ("points".equalsIgnoreCase(graph_type)) {
      gstate.setGraphStyle(GraphStateI.DOT_GRAPH);
    }
    else if ("bar".equalsIgnoreCase(graph_type)) {
      gstate.setGraphStyle(GraphStateI.BAR_GRAPH);
    }
  }
}
