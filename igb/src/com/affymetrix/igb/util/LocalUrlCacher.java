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

package com.affymetrix.igb.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.DateFormat;

public class LocalUrlCacher {
  static String cache_root = UnibrowPrefsUtil.getAppDataDirectory()+"cache";
  static boolean DEBUG_CONNECTION = false;
  static boolean CACHE_FILE_URLS = false;

  public static int IGNORE_CACHE = 100;
  public static int ONLY_CACHE = 101;
  public static int NORMAL_CACHE = 102;

  public static InputStream getInputStream(String url) throws IOException  {
    return getInputStream(url, NORMAL_CACHE);
  }

  public static InputStream getInputStream(String url, int return_behavior)  throws IOException {
    return getInputStream(url, NORMAL_CACHE, true);
  }


  public static InputStream getInputStream(String url, int cache_option, boolean write_to_cache)
       throws IOException {

    // if url is a file url, and not caching files, then just directly return stream
    if ((! CACHE_FILE_URLS) && (url.startsWith("file:"))) {
      InputStream fstr = null;
      try {
	URL furl = new URL(url);
	fstr = furl.openConnection().getInputStream();
        System.out.println("URL is file url, so not caching: " + furl);
      }
      catch (Exception ex) {
	System.out.println("File for URL not found: " + url);
      }
      return fstr;
    }
    File fil = new File(cache_root);
    if (! fil.exists()) {
      System.out.println("creating new cache directory: " + fil.getAbsolutePath());
      fil.mkdirs();
    }
    fil = null;

    // if not in cache, then return input stream from http connection
    // if in cache, then check timestamping
    // Should probably do this via a "just headers" http call, but for now doing standard GET and
    //    just reading headers
    InputStream result_stream = null;

    String encoded_url = UrlToFileName.encode(url);
    String cache_file_name = cache_root + "/" + encoded_url;
    File cache_file = new File(cache_file_name);
    boolean cached = cache_file.exists();
    URLConnection conn = null;

    long remote_timestamp = 0;
    int content_length = -1;
    String content_type = null;
    boolean url_reachable = false;
    boolean has_timestamp = false;
    // if cache_option == ONLY_CACHE, then don't even try to retrieve from url
    if (cache_option != ONLY_CACHE) {
      try {
	URL theurl = new URL(url);
	conn = theurl.openConnection();  
	// adding a conn.connect() call here to force throwing of error here if can't open connection
	//    because some method calls on URLConnection like those below don't always throw errors 
	//    when connection can't be opened -- which woule end up allowing url_reachable to be set to true 
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
	url_reachable = true;
      }
      catch (Exception ex) {
	System.out.println("URL not reachable: " + url);
	url_reachable = false;
      }
    }
    // if cache_option == IGNORE_CACHE, then don't even try to retrieve from cache
    if (cached && (cache_option != IGNORE_CACHE)) {
      long local_timestamp = cache_file.lastModified();
      String local_date = DateFormat.getDateTimeInstance().format(new Date(local_timestamp)); ;
      if ((! url_reachable) ||
	  (has_timestamp && (remote_timestamp <= local_timestamp)) ) {
	System.out.println("cache exists and is more recent, using cache: " + cache_file);
	result_stream = new FileInputStream(cache_file);
      }
      else {
	System.out.println("cached file exists, but URL is more recent, so reloading cache");
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
	System.out.println("writing to cache: " + encoded_url);
	// write data from URL into a File
	BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cache_file));
	// no API for returning number of bytes successfully written, so write all in one shot...
	bos.write(content, 0, content_length);
	bos.close();
      }
      result_stream = new ByteArrayInputStream(content);
    }
    //    System.out.println("returning stream: " + result_stream);
    return result_stream;
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
