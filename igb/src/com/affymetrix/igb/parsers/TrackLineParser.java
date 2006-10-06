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

  static Map default_track_hash;
  static Pattern line_regex = Pattern.compile("\t");
  static Pattern comma_regex = Pattern.compile(",");

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

  Map track_hash = default_track_hash;

  static  {
    default_track_hash = new TreeMap();
    //    default_track_hash.put("name", null);
    default_track_hash.put("description", "User Supplied Track");
    default_track_hash.put("visibility", new Integer(1));
    default_track_hash.put("color", Color.gray);  // was black, but that conflicts with background...
    //default_track_hash.put("altcolor", null);
    default_track_hash.put("usescore", new Integer(0));
    //default_track_hash.put("priority", null);  // no default assigned priority...
    default_track_hash.put("offset", new Integer(0));
    //default_track_hash.put("url", null);
  }

  public TrackLineParser() {}

  public Map getDefaultTrackHash() { return default_track_hash; }
  public Map getCurrentTrackHash() { return track_hash; }
  
  /**
   *  If the map contains a key named color with a vaule that is a String,
   *  converts the value to a Color object and puts it back in the Map.
   *  If the Map contains a value for "name", then adds that
   *  color association to the named instance of AnnotStyle.
   */
  public static void reformatColor(Map m) {
    Object o = m.get("color");
    if (o instanceof String) {
      String color_string = (String) o;
      String[] rgb = comma_regex.split(color_string);
      int red = Integer.parseInt(rgb[0]);
      int green = Integer.parseInt(rgb[1]);
      int blue = Integer.parseInt(rgb[2]);
      Color col = new Color(red, green, blue);
      m.put("color", col);

      String name = (String) m.get("name");
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
  
  /** Parses a track line putting the keys and values into a Map.
   *  The Map is returned and is also available as {@link #getCurrentTrackHash()}.
   */
  public Map setTrackProperties(String track_line) {
    System.out.println("setting track properties from: "+track_line);
    
    track_hash = new TreeMap(default_track_hash); // default_track_hash.clone();
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
    
    reformatColor(track_hash);
    
    String name = (String) track_hash.get("name");
    String url = (String) track_hash.get("url");
    
    if (name != null && url != null) {
      AnnotStyle style = AnnotStyle.getInstance(name, false);
      style.setUrl(url);
    }
    
    return track_hash;
  }

  /** Performs a test of the track-line parsing. */
  public static void main(String[] args) {
    String str = "track foo=bar this=\"that\" color=123,100,10 nothing=\"\" url=\"http://www.foo.bar?moo=cow&this=$$\"";
    TrackLineParser tlp = new TrackLineParser();
    Map m;
    m = tlp.setTrackProperties(str);
    
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
