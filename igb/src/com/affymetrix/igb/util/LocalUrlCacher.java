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

package com.affymetrix.igb.util;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.security.MessageDigest;
import java.math.BigInteger;


public class LocalUrlCacher {
  static String cache_root = UnibrowPrefsUtil.getAppDataDirectory()+"cache";
  static String long_url_map = cache_root + "/long_url_map.props";
  static boolean DEBUG_CONNECTION = false;
  static boolean CACHE_FILE_URLS = false;
  static MessageDigest md5_generator;
  //  static Properties long2short_filenames = new Properties();

  public static int IGNORE_CACHE = 100;
  public static int ONLY_CACHE = 101;
  public static int NORMAL_CACHE = 102;
  public static int long_file_count = 0;

  // the "quickload" part of the constant value is there for historical reasons
  public static final String PREF_CACHE_USAGE = "quickload_cache_usage";
  public static final int CACHE_USAGE_DEFAULT = LocalUrlCacher.NORMAL_CACHE;

  static {
    // initialize cache
    try {
      md5_generator = MessageDigest.getInstance("MD5");
      /**
      File long_url_file = new File(long_url_map);
      System.out.println("properties map for conversion of long URLs: " + long_url_map);
      if (long_url_file.exists()) {
	long2short_filenames.load(new BufferedInputStream(new FileInputStream(long_url_file)));
      }
      */
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static InputStream getInputStream(String url) throws IOException  {
    return getInputStream(url, getPreferredCacheUsage(), true);
  }

  /** Determines whether the given URL string represents a file URL. */
  public static boolean isFile(String url) {
    return (url.substring(0,5).compareToIgnoreCase("file:") == 0);
  }

  /** Returns the local File object for the given URL;
   *  you must check File.exists() to determine if the file exists in the cache.
   *
   * For long URLs, the file may be contained in additional subdirectories of the
   *    the cache root directory in order to ensure that each path segment is
   *    within the file name limits of the OS
   *  If additional subdirectories are needed, getCacheFileForURL automatically creates
   *     these directories
   *  The File object returned is created by getCacheFileForURL, but the actual on-disk file is not created --
   *     that is up to other methods in LocalUrlCacher
   */
  static File getCacheFileForURL(String url) {
    String encoded_url = UrlToFileName.encode(url);
    //    String cache_file_name = cache_root + File.separator + encoded_url;
    String cache_file_name = cache_root + "/" + encoded_url;
    if (cache_file_name.length() > 255) {
      System.out.println("WARNING! Trying to encode file, but full file path > 255 characters: " +
			 cache_file_name.length());
      System.out.println("    " + cache_file_name);
      byte[] md5_digest = md5_generator.digest(encoded_url.getBytes());
      BigInteger md5_big_int = new BigInteger(md5_digest);
      String md5_string = md5_big_int.toString(16);
      cache_file_name = cache_root + "/" + md5_string;
      System.out.println("new file path: " + cache_file_name);
    }
    File cache_file = new File(cache_file_name);

      //    File parent_dir = cache_file.getParentFile();
      //    if (! parent_dir.exists()) {
      //      // if directories are missing, create them for this file's path
      //      parent_dir.mkdirs();
      //    }
    return cache_file;
  }

  /** Returns the cache directory, creating it if necessary. */
  static File getCacheDirectory() {
    File fil = new File(cache_root);
    if (! fil.exists()) {
      System.out.println("creating new cache directory: " + fil.getAbsolutePath());
      fil.mkdirs();
      // It is possible that mkdirs() will fail.  Do what then?
    }
    return fil;
  }

  public static final String TYPE_FILE = "In Filesystem";
  public static final String TYPE_CACHED = "Cached";
  public static final String TYPE_STALE_CACHE = "Stale Cache";
  public static final String TYPE_NOT_CACHED = "Remote File";
  public static final String TYPE_UNREACHABLE = "Not Available?";

  /**
   *  Returns the accesibility of the file represented by the URL.
   *  Will be one of {@link #TYPE_FILE}, {@link #TYPE_CACHED},
   *  {@link #TYPE_STALE_CACHE}, {@link #TYPE_NOT_CACHED},
   *  {@link #TYPE_UNREACHABLE}.
   */
  protected static String getLoadType(String url, int cache_option) {

    // if url is a file url, and not caching files, then just directly return stream
    if (isFile(url)) {
      try {
        URI file_url = new URI(url);
        File f = new File(file_url);
        System.out.println("Checking for existence of: " + f.getPath());
        if (f.exists()) {
          return TYPE_FILE;
        } else {
          return TYPE_UNREACHABLE;
        }
      } catch (URISyntaxException use) {
        System.out.println("URISyntaxException: " + url);
        return TYPE_FILE;
      }
    }

    File cache_dir = getCacheDirectory(); // Make sure cache directory exists. Maybe not needed.

    File cache_file = getCacheFileForURL(url);
    boolean cached = cache_file.exists();

    if (cache_option == ONLY_CACHE) {
      if (cached) return TYPE_CACHED;
      else return TYPE_NOT_CACHED;
    }

    URLConnection conn = null;

    long remote_timestamp = 0;
    int content_length = -1;
    String content_type = null;
    boolean url_reachable = false;
    boolean has_timestamp = false;
    // if cache_option == ONLY_CACHE, then don't even try to retrieve from url

    try {
      URL theurl = new URL(url);
      conn = theurl.openConnection();
      // adding a conn.connect() call here to force throwing of error here if can't open connection
      //    because some method calls on URLConnection like those below don't always throw errors
      //    when connection can't be opened -- which would end up allowing url_reachable to be set to true
      ///   even when there's no connection
      conn.connect();
      if (DEBUG_CONNECTION) {
        reportHeaders(conn);
      }
      remote_timestamp = conn.getLastModified();
      has_timestamp = (remote_timestamp > 0);
      content_type = conn.getContentType();
      content_length = conn.getContentLength();
      url_reachable = true;
    }
    catch (IOException ioe) {
      url_reachable = false;
    }
    conn = null; // there is no close() method for URLConnection

    if (! url_reachable) {
      if (cached && cache_option != IGNORE_CACHE) {
        return TYPE_CACHED;
      } else {
        return TYPE_UNREACHABLE;
      }
    }

    // We have normal cache usage and the remote file is reachable.
    if (cached) {
      long local_timestamp = cache_file.lastModified();
      if ((has_timestamp && (remote_timestamp <= local_timestamp))) {
        return TYPE_CACHED;
      }
      else {
        return TYPE_STALE_CACHE;
      }
    } else {
      return TYPE_NOT_CACHED;
    }
  }

  public static InputStream getInputStream(String url, boolean write_to_cache)
       throws IOException {
    return getInputStream(url, getPreferredCacheUsage(), write_to_cache);
  }

  public static InputStream getInputStream(String url, int cache_option, boolean write_to_cache)
       throws IOException {

    // if url is a file url, and not caching files, then just directly return stream
    if ((! CACHE_FILE_URLS) && isFile(url)) {
      InputStream fstr = null;
      URL furl = new URL(url);
      fstr = furl.openConnection().getInputStream();
      System.out.println("URL is file url, so not caching: " + furl);
      return fstr;
    }
    File fil = new File(cache_root);
    if (! fil.exists()) {
      System.out.println("creating new cache directory: " + fil.getAbsolutePath());
      fil.mkdirs();
    }
    fil = null;

    // if NORMAL_CACHE:
    //   if not in cache, then return input stream from http connection
    //   if in cache, then check URL content has changed via GET with if-modified-since header based
    //        on modification date of cached file
    //      if content is returned, check last-modified header just to be sure (some servers might ignore
    //        if-modified-since header?)
    InputStream result_stream = null;

    File cache_file = getCacheFileForURL(url);
    boolean cached = cache_file.exists();
    long local_timestamp = -1;
    if (cached) { local_timestamp = cache_file.lastModified(); }
    URLConnection conn = null;

    long remote_timestamp = 0;
    int content_length = -1;
    String content_type = null;
    boolean url_reachable = false;
    boolean has_timestamp = false;
    HttpURLConnection hcon = null;
    int http_status = -1;


    if (cache_option == ONLY_CACHE) {
    }
    else if (cache_option == IGNORE_CACHE) {
    }
    else if (cache_option == NORMAL_CACHE) {
    }
    else {
      // SHOULD NEVER GET HERE
    }

    // if cache_option == ONLY_CACHE, then don't even try to retrieve from url
    if (cache_option != ONLY_CACHE) {
      try {
	URL theurl = new URL(url);
	conn = theurl.openConnection();
	if (cached) {
	  conn.setIfModifiedSince(local_timestamp);
	}
	// adding a conn.connect() call here to force throwing of error here if can't open connection
	//    because some method calls on URLConnection like those below don't always throw errors
	//    when connection can't be opened -- which would end up allowing url_reachable to be set to true
	///   even when there's no connection
	conn.connect();
	if (DEBUG_CONNECTION) {
	  reportHeaders(conn);
	}
	remote_timestamp = conn.getLastModified();
	has_timestamp = (remote_timestamp > 0);
	content_type = conn.getContentType();
	content_length = conn.getContentLength();
	//	String remote_date = DateFormat.getDateTimeInstance().format(new Date(remote_timestamp)); ;
	if (conn instanceof HttpURLConnection) {
	  hcon = (HttpURLConnection)conn;
	  http_status = hcon.getResponseCode();
        }
	url_reachable = true;
      }
      catch (IOException ioe) {
	System.out.println("URL not reachable: " + url);
	url_reachable = false;
        if (! cached) { throw ioe; }
      }
    }

    // if cache_option == IGNORE_CACHE, then don't even try to retrieve from cache
    if (cached && (cache_option != IGNORE_CACHE)) {
      if (url_reachable) {
	//  response contents not modified since local cached copy last modified, so use local
	if (http_status == HttpURLConnection.HTTP_NOT_MODIFIED) {
	  System.out.println("Received HTTP_NOT_MODIFIED status for URL, using cache: " + cache_file);
	  result_stream = new BufferedInputStream(new FileInputStream(cache_file));
	}
	//        long local_timestamp = cache_file.lastModified();
	else if ((has_timestamp && (remote_timestamp <= local_timestamp))) {
	  System.out.println("Cache exists and is more recent, using cache: " + cache_file);
	  result_stream = new BufferedInputStream(new FileInputStream(cache_file));
        }
        else {
	  System.out.println("cached file exists, but URL is more recent, so reloading cache");
          result_stream = null;
        }
      }
      else { // url is reachable
        if (cache_option != ONLY_CACHE) {
          System.out.println("Remote URL not reachable.");
        }
	System.out.println("Loading cached file for URL");
	result_stream = new BufferedInputStream(new FileInputStream(cache_file));
      }
    }

    // if cache_option == ONLY_CACHE, then don't even try to retrieve from url
    if (result_stream == null && url_reachable && (cache_option != ONLY_CACHE)) {
      // no cache hit, or stale, or cache_option set to IGNORE_CACHE...
      InputStream connstr = conn.getInputStream();
      BufferedInputStream bis = new BufferedInputStream(connstr);
      byte[] content = null;
      if (content_length >= 0) {       // if content_length header was set, can load based on length
	content = new byte[content_length];
	//      int bytes_read = bis.read(content, 0, content_length);
	int total_bytes_read = 0;
	while (total_bytes_read < content_length) {
	  int bytes_read = bis.read(content, total_bytes_read, (content_length - total_bytes_read));
	  total_bytes_read += bytes_read;
	}
	if (total_bytes_read != content_length) {
	  System.out.println("%%%% problem: bytes read != content length %%%%");
	}
      }
      else {
	if (DEBUG_CONNECTION) { System.out.println("No content length header, so doing piecewise loading"); }
	// if no content_length header, then need to load a chunk at a time
	//   till find end, then piece back together into content byte array
	ArrayList chunks = new ArrayList(100);
	IntList byte_counts = new IntList(100);
	int chunk_count = 0;
	int chunk_size = 256 * 256;  // reading in 64KB chunks
	int total_byte_count = 0;
	int bytes_read = 0;
	while (bytes_read != -1) {  // if bytes_read == -1, then end of data reached
	  byte[] chunk = new byte[chunk_size];
	  bytes_read = bis.read(chunk, 0, chunk_size);
	  if (DEBUG_CONNECTION) {
	    System.out.println("   chunk: " + chunk_count + ", byte count: " + bytes_read);
	  }
	  if (bytes_read > 0)  { // want to ignore EOF byte_count of -1, and empty reads (0 bytes due to blocking)
	    total_byte_count += bytes_read;
	    chunks.add(chunk);
	    byte_counts.add(bytes_read);
	  }
	  chunk_count++;
	}
	if (DEBUG_CONNECTION) {
	  System.out.println("total bytes: " + total_byte_count +
			     ", total chunks with > 0 bytes: " + chunks.size());
	}

	content_length = total_byte_count;
	content = new byte[content_length];
	total_byte_count = 0;
	for (int i=0; i<chunks.size(); i++) {
	  byte[] chunk = (byte[])chunks.get(i);
	  int byte_count = byte_counts.get(i);
	  if (byte_count > 0) {
	    System.arraycopy(chunk, 0, content, total_byte_count, byte_count);
	    total_byte_count += byte_count;
	  }
	}
      }
      bis.close();
      connstr.close();
      if (write_to_cache) {
	System.out.println("writing to cache: " + cache_file.getPath());
	// write data from URL into a File
	BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cache_file));
	// no API for returning number of bytes successfully written, so write all in one shot...
	bos.write(content, 0, content_length);
	bos.close();
      }
      result_stream = new ByteArrayInputStream(content);
    }

    if (result_stream == null) {
      String message;
      if (cache_option == ONLY_CACHE) {
        message = "Local cache file not found.  You may wish to change your caching preferences in QuickLoad options.";
      } else if (cache_option == IGNORE_CACHE) {
        message = "Remote URL could not be opened.";
      } else {
        message = "Either the remote URL or the local cached copy could not be opened.";
      }
      throw new IOException(message);
    }

    //    System.out.println("returning stream: " + result_stream);
    return result_stream;
  }

  public static InputStream askAndGetInputStream(String filename, boolean cache_annots_param)
  throws IOException {
    return askAndGetInputStream(filename, getPreferredCacheUsage(), cache_annots_param);
  }

  /**
   *  Similar to {@link #getInputStream(String)}, but asks the user before
   *  downloading anything over the network.
   *  @return returns an InputStream or null if the user cancelled or the file
   *  is unreachable.
   */
  public static InputStream askAndGetInputStream(String filename, int cache_usage_param, boolean cache_annots_param)
  throws IOException {
    String cache_type = LocalUrlCacher.getLoadType(filename, cache_usage_param);

    String short_filename = "selected file";
    int index = filename.lastIndexOf('/');
    if (index > 0) {
      short_filename = filename.substring(index+1);
    }

    if (LocalUrlCacher.TYPE_FILE.equals(cache_type)) {
      // just go ahead and load it
      return LocalUrlCacher.getInputStream(filename, cache_usage_param, cache_annots_param);
    }

    else if (LocalUrlCacher.TYPE_CACHED.equals(cache_type) && ! (cache_usage_param == LocalUrlCacher.IGNORE_CACHE)) {
      // just go ahead and load from cache
      return LocalUrlCacher.getInputStream(filename, LocalUrlCacher.NORMAL_CACHE, cache_annots_param);
    }

    else if (LocalUrlCacher.TYPE_UNREACHABLE.equals(cache_type)) {
      ErrorHandler.errorPanel("File Unreachable",
        "The requested file can not be found:\n" + filename);
      return null;
    }

    else if (LocalUrlCacher.TYPE_STALE_CACHE.equals(cache_type)) {
      String[] options = { "Load remote file", "Use local cache", "Cancel" };
      String message = "The remote file \"" + short_filename +
          "\"is more recent than the local copy.";

      int choice = JOptionPane.showOptionDialog(null, message, "Load file?",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);

      if (choice == 0) {
        return LocalUrlCacher.getInputStream(filename, LocalUrlCacher.NORMAL_CACHE, cache_annots_param);
      } else if (choice == 1) {
        return LocalUrlCacher.getInputStream(filename, LocalUrlCacher.ONLY_CACHE, cache_annots_param);
      } else if (choice == 2) {
        return null;
      }
    }

    else if (LocalUrlCacher.TYPE_NOT_CACHED.equals(cache_type)) {

      String[] options = { "OK", "Cancel" };
      String message = "Load " + short_filename + " from the remote server?";

      int choice = JOptionPane.showOptionDialog(null, message, "Load file?",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);

      if (choice == JOptionPane.OK_OPTION) {
        return LocalUrlCacher.getInputStream(filename, LocalUrlCacher.NORMAL_CACHE, cache_annots_param);
      } else {
        return null;
      }
    }

    return null;
  }

  /**
   *  Forces flushing of entire cache.
   *  Simply removes all cached files.
   */
  public static void clearCache() {
    File cache_dir = new File(cache_root);
    if (cache_dir.exists()) {
      File[] fils =  cache_dir.listFiles();
      int file_count = fils.length;
      for (int i=0; i<file_count; i++) {
	File fil = fils[i];
	fil.delete();
      }
    }
  }

  /** Returns the location of the root directory of the cache. */
  public static String getCacheRoot() {
    return cache_root;
  }

  /** Returns the current value of the persistent user preference PREF_CACHE_USAGE. */
  public static int getPreferredCacheUsage() {
    int cache_usage =
      UnibrowPrefsUtil.getIntParam(PREF_CACHE_USAGE, CACHE_USAGE_DEFAULT);
    return cache_usage;
  }

  public static void updateCacheUrlInBackground(final String url) {
    Runnable r = new Runnable() {
      public void run() {
        try {
          updateCacheUrlAndWait(url);
        } catch (IOException ioe) {
          // Don't worry about these exceptions.  It only means the cache will remain stale.
          //System.out.println("Problem while trying to update cache for: " + url);
          //System.out.println("Caused by: " + ioe.toString());
        }
      }
    };
    Thread t = new Thread(r);
    t.start();
  }

  static void updateCacheUrlAndWait(String url) throws IOException {
    InputStream is = null;
    try {
      getInputStream(url, NORMAL_CACHE, true);
      System.out.println("Updated cache for: " + url);
    } finally {
      if (is != null) try { is.close(); } catch (IOException ioe) {}
    }
  }

  public static void reportHeaders(URLConnection query_con) {
    try {
      System.out.println("URL: " + query_con.getURL().toString());
      int hindex = 0;
      while (true) {
	String val = query_con.getHeaderField(hindex);
	String key = query_con.getHeaderFieldKey(hindex);
	if (val == null && key == null) {
	  break;
	}
	System.out.println("   header:   key = " + key + ", val = " + val);
	hindex++;
      }
    }
    catch (Exception ex)  { ex.printStackTrace(); }
  }
}
