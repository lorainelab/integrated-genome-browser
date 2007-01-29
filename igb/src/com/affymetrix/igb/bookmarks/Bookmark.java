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

package com.affymetrix.igb.bookmarks;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

import com.affymetrix.igb.servlets.UnibrowControlServer;
import com.affymetrix.igb.servlets.UnibrowControlServlet;
import com.affymetrix.igb.IGB;

/**
 *  Holds a bookmark, which is simply a name associated with a URL.
 */
public class Bookmark implements Serializable {

  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String SEQID = "seqid";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String VERSION = "version";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String START = "start";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String END = "end";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String SELECTSTART = "selectstart";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String SELECTEND = "selectend";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark,
      this one can occur 0,1, or more times in the URL of a UnibrowControlServlet bookmark. */
  public static final String DATA_URL = "data_url";
  
  
  private static boolean DEBUG = false;

  private String name;
  private URL url;

  public Bookmark(String name, String url) throws MalformedURLException {
    this.name = name;
    if (this.name == null || this.name.length() == 0) {
      this.name = "bookmark";
    }
    this.url = new URL(url);
  }

  /** Takes a URL and parses the query parameters into a map.
   *  All entries will be String arrays, as is expected by
   *  HttpServletRequest objects.
   *  Thus if the url is http://www.abc.com/page?x=3&z&y=4&y=5 then the
   *  resulting Map will have three String[] entries, for x={"3"} and z={""} and y={"4", "5"}.
   *  @return a Map, which can be empty.  All entries will be Strings.
   *  All keys and values will be decoded with {@link URLDecoder}.
   */
  public static Map parseParameters(URL url) {
    Map map = new LinkedHashMap();
    String query = url.getQuery();
    if (query != null) {
      parseParametersFromQuery(map, query, true);
    }
    if (DEBUG) System.out.println("Finished parsing");
    return map;
  }

  /** Takes the query parameter string from a URL and parses the parameters
   *  into a the given map.
   *  All entries will be String arrays, as is expected by
   *  HttpServletRequest objects.
   *  Thus if the query string is  x=3&z&y=4&y=5  then the
   *  resulting Map will have three String[] entries, for x={"3"} and z={""} and y={"4", "5"}.
   *  @return the same Map that was passed in, which can be empty.  All entries will be Strings.
   *  @param use_url_decoding whether or not to apply {@link URLDecoder} to all keys and values.
   */
  public static void parseParametersFromQuery(Map map, String query, boolean use_url_decoding) {
    if (query != null) {
      StringTokenizer st = new StringTokenizer(query, "&");
      while (st.hasMoreTokens()) {
        String token = (String) st.nextToken();
        int ind_1 = token.indexOf('=');

        String key, value;
        if (ind_1 > 0) {
          key = token.substring(0, ind_1);
          value = token.substring(ind_1+1);
        } else {
          key = token;
          value = "";
        }

        if (use_url_decoding) try {
          key = URLDecoder.decode(key, "UTF-8");
          value = URLDecoder.decode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {}

        addToMap(map, key, value);

        if (DEBUG) System.out.println("Bookmark.parseParameters: Key  ->  "+key+",  value -> "+value);
      }
    }
    if (DEBUG) System.out.println("Finished parsing");
  }
  
  public static final String IGB_GRAPHS_PRAGMA = "##IGB-graphs ";
  
  public static void parseIGBGraphsPragma(Map map, String line, boolean use_url_decoding) {
    if (line.startsWith(IGB_GRAPHS_PRAGMA)) {
      String graph_props = line.substring(IGB_GRAPHS_PRAGMA.length());
      parseParametersFromQuery(map, graph_props, use_url_decoding);
    }
  }
  
  /**
   *  Adds a key->value mapping to a map where the key will map to
   *  a String array.  If the key already has a String[] mapped to it,
   *  this method will increase the length of that array.  Otherwise it
   *  will create a new String[] of length 1.
   *  
   *  @param map  a Map.  It is good to use a LinkedHashMap, if you care
   *     about the order of the entries, but this is not required.
   *  @param key  a non-null, non-empty String.  If null or empty, it will not
   *     be added to the map. (Empty means "String.trim().length()==0" )
   *  @param value a String.  Null is ok.
   */
  static void addToMap(Map map, String key, String value) {
    if (key == null || key.trim().length()==0) {
      return;
    }
    String[] array = (String[]) map.get(key);
    if (array == null) {
      String[] new_array = new String[] {value};
      map.put(key, new_array);
    } else {
      String[] new_array = new String[array.length+1];
      System.arraycopy(array, 0, new_array, 0, array.length);
      new_array[new_array.length - 1] = value;
      map.put(key, new_array);
    }
  }
  
  /** Constructs a UnibrowControlServer Bookmark URL based on the properties
   *  in the Map.  All keys and values will be encoded with
   *  {@link URLEncoder}.  All values should be String[] arrays, but any that are not
   *  will be converted to a String by calling the toString() method of the object.
   *  (For String[] objects, each String gets appended individually as a
   *  key=value pair, with the same key name.)
   */
  public static String constructURL(Map props) {
    return constructURL(UnibrowControlServer.DEFAULT_SERVLET_URL, props);
  }

  /** Constructs a GENERIC Bookmark URL based on the properties
   *  in the Map.  All keys and values will be encoded with
   *  {@link URLEncoder}.  All values should be String[] arrays, but any that are not
   *  will be converted to a String by calling the toString() method of the object.
   *  (For String[] objects, each String gets appended individually as a
   *  key=value pair, with the same key name.)
   *  @param url_base The beginning part of a url, like "http://www.xxx.com"
   *    or even "http://www.xxx.com?x=1&y=2".
   */
  public static String constructURL(String url_base, Map props) {
    StringBuffer sb = new StringBuffer();
    sb.append(url_base);
    
    Iterator iter = props.keySet().iterator();
    
    // The first key in props is usually the first tag in the URL query string,
    // but *not* if the url_base already contains a '?' character.
    boolean first_tag = (url_base.indexOf('?') < 0);

    while (iter.hasNext()) {
      // for all properties, add as tag-val parameter pair in URL
      String tag = (String)iter.next();
      Object val = props.get(tag);
      if (first_tag) {sb.append('?');}
      else {sb.append('&');}
      appendTag(sb, tag, val);
      first_tag = false;
    }
    if (DEBUG) System.out.println("Constructed URL: "+sb);
    return sb.toString();
  }

  /** Appends a key-value pair to a StringBuffer in URL parameter format "key=value".
   *  All keys and values will be encoded with {@link URLEncoder}.  
   *  All value objects should be Strings or String[]s, but any that are not
   *  will be converted to a String by calling the toString() method of the object.
   *  For String[] objects, each String will get converted individually to a
   *  "key=value" pair, with the same key name. Example:  "key=value1&key=value2&key=value3".
   */
  private static void appendTag(StringBuffer sb, String key, Object o) {
    try {
      if (o instanceof String[]) {
        String[] values = (String[]) o;
        for (int i=0; i<values.length; i++) {
          if (i>0) {sb.append('&');}
          sb.append(URLEncoder.encode(key, "UTF-8"));
          String val = values[i];
          if (val != null && val.length()>0) {
            sb.append('=');
            sb.append(URLEncoder.encode(values[i], "UTF-8"));
          }
        }
      } else {
        sb.append(URLEncoder.encode(key, "UTF-8"));
        if (o != null) {
          String value = o.toString();
          if (value.length()>0) {
            sb.append('=');
            sb.append(URLEncoder.encode(value, "UTF-8"));
          }
        }
      }
    } catch (java.io.UnsupportedEncodingException e) {}
  }
  
  public Map getParameters() {
    return parseParameters(url);
  }
  
  /** Returns true if the Path of the Url matches 
   *  {@link UnibrowControlServer#SERVLET_NAME} and
   *  the Host is "localhost". 
   */
  public boolean isUnibrowControl() {
    URL url = getURL();
    String host = url.getHost();
    String path = url.getPath();
    return (("localhost".equals(host) || "127.0.0.1".equals(host)) 
      && path.equals("/"+UnibrowControlServer.SERVLET_NAME));
  }

  public String toString() {
    return "Bookmark: '"+this.name+"' -> '"+this.url.toExternalForm()+"'";
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public java.net.URL getURL() {
    return this.url;
  }

  public void setURL(URL url) {
    this.url = url;
  }
}
