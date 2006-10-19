/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

import com.affymetrix.igb.tiers.AnnotStyle;

public class TrackLineParser {

  static Pattern line_regex = Pattern.compile("\t");
  static Pattern comma_regex = Pattern.compile(",");

  public static final String NAME="name";
  public static final String COLOR="color";
  public static final String DESCRIPTION="description";
  public static final String VISIBILITY ="visibility";
  public static final String USE_SCORE="useScore";
  public static final String URL="url";

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
   *  If the map contains a key named color with a vaule that is a String,
   *  converts the value to a Color object and puts it back in the Map.
   *  If the Map contains a value for "name", then adds that
   *  color association to the named instance of AnnotStyle.
   */
  static void reformatColor(Map m) {
    Object o = m.get(COLOR);
    if (o instanceof String) {
      String color_string = (String) o;
      String[] rgb = comma_regex.split(color_string);
      int red = Integer.parseInt(rgb[0]);
      int green = Integer.parseInt(rgb[1]);
      int blue = Integer.parseInt(rgb[2]);
      Color col = new Color(red, green, blue);
      m.put(COLOR, col);

      String name = (String) m.get(NAME);
      if (name != null) {
        AnnotStyle style = AnnotStyle.getInstance(name);
        style.setColor(col);
      }
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
  
  /** Parses a track line putting the keys and values into a Map.
   *  The Map is returned and is also available as {@link #getCurrentTrackHash()}.
   *  This method also stores the properties, such as color, in a temporary
   *  AnnotStyle.
   */
  public Map setTrackProperties(String track_line, String default_track_name) {
    //System.out.println("setting track properties from: "+track_line);
    
    track_hash = parseTrackLine(track_line);
    
    reformatColor(track_hash);
    
    String name = (String) track_hash.get(NAME);
    if (name == null) {
      // In UCSC browser, the default is always "User Track"
      track_hash.put(NAME, default_track_name);
      name = default_track_name;
    }

    if (name != null) {
      String url = (String) track_hash.get(URL);
      AnnotStyle style = AnnotStyle.getInstance(name, false);
      if (url != null) {
        style.setUrl(url);      
      }
      String description = (String) track_hash.get(DESCRIPTION);
      if (description != null) {
        style.setHumanName(description);
      } else {
        // Unless we explicitly set the human name, it will be the lower-case
        // version of the name used in AnnotStyle.getInstance().
        // Explicitly setting the name keeps the case intact.
        style.setHumanName(name);
      }
      String visibility = (String) track_hash.get(VISIBILITY);
      
      java.util.List collapsed_modes = Arrays.asList(new String[] {"1", "dense"});
      java.util.List expanded_modes = Arrays.asList(new String[] 
        {"2", "full", "3", "pack", "4", "squish"});
      
      if (visibility != null) {
        // 0 - hide, 1 - dense, 2 - full, 3 - pack, and 4 - squish. 
        // The numerical values or the words can be used, i.e. full mode may be 
        // specified by "2" or "full". The default is "1".
        if (collapsed_modes.contains(visibility)) {
          style.setCollapsed(true);
        }
        else if (expanded_modes.contains(visibility)) {
          style.setCollapsed(false);
        }
      }
    }
    
    return track_hash;
  }

  /** Performs a test of the track-line parsing. */
  public static void main(String[] args) {
    String str = "track foo=bar this=\"that\" color=123,100,10 nothing=\"\" url=\"http://www.foo.bar?moo=cow&this=$$\"";
    TrackLineParser tlp = new TrackLineParser();
    Map m;
    m = tlp.setTrackProperties(str, null);
    
    // Now print that map
    Iterator iter = m.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      Object key = entry.getKey();
      Object value = entry.getValue();
      System.out.println("" + key + " --> " + value);
    }
  }
}
