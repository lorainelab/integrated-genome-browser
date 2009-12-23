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
package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.IntList;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.igb.Application;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

public final class LocalUrlCacher {

	private static final String cache_content_root = UnibrowPrefsUtil.getAppDataDirectory() + "cache/";
	private static final String cache_header_root = cache_content_root + "headers/";
	private static final String HTTP_STATUS_HEADER = "HTTP_STATUS";
	private static boolean DEBUG_CONNECTION = false;
	private static boolean CACHE_FILE_URLS = false;
	public static final int IGNORE_CACHE = 100;
	public static final int ONLY_CACHE = 101;
	public static final int NORMAL_CACHE = 102;

	// the "quickload" part of the constant value is there for historical reasons
	public static final String PREF_CACHE_USAGE = "quickload_cache_usage";
	public static final int CACHE_USAGE_DEFAULT = LocalUrlCacher.NORMAL_CACHE;
	public static final String URL_NOT_REACHABLE = "URL_NOT_REACHABLE";

	public static final int CONNECT_TIMEOUT = 20000;	// If you can't connect in 20 seconds, fail.
	public static final int READ_TIMEOUT = 60000;		// If you can't read any data in 1 minute, fail.

	private static enum CacheType { FILE, CACHED, STALE_CACHE, NOT_CACHED, UNREACHABLE};

	private static boolean offline = false;

	/** Sets the cacher to off-line mode, in which case only cached data will
	 *  be used, will never try to get data from the web.
	 */
	public static void setOffLine(boolean b) {
		offline = b;
	}

	/** Returns the value of the off-line flag. */
	public static boolean getOffLine() {
		return offline;
	}

	/** Determines whether the given URL string represents a file URL. */
	private static boolean isFile(String url) {
		if (url == null || url.length() < 5) {
			return false;
		}
		return (url.substring(0, 5).compareToIgnoreCase("file:") == 0);
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
	private static File getCacheFile(String root, String url) {
		File fil = new File(root);
		if (!fil.exists()) {
			Application.getSingleton().logInfo("Creating new cache directory: " + fil.getAbsolutePath());
			fil.mkdirs();
		}
		String encoded_url = UrlToFileName.encode(url);
		String cache_file_name = root + encoded_url;
		// Need to make sure that full path of file is < 255 characters to ensure
		//    cross-platform compatibility (some OS allow any length, some only restrict file name
		//    length (last path segment), but there are some that restrict full path to <= 255 characters
		if (cache_file_name.length() > 255) {
			cache_file_name = root + UrlToFileName.toMd5(encoded_url);
		}
		File cache_file = new File(cache_file_name);
		return cache_file;
	}


	public static InputStream getInputStream(String url) throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), true);
	}

	public static InputStream getInputStream(String url, boolean write_to_cache, Map<String,String> headers)
					throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), write_to_cache, headers);
	}
	public static InputStream getInputStream(String url, boolean write_to_cache, Map<String,String> headers, boolean fileMayNotExist)
					throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), write_to_cache, headers, fileMayNotExist);
	}

	public static InputStream getInputStream(String url, boolean write_to_cache)
					throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), write_to_cache);
	}

	private static InputStream getInputStream(String url, int cache_option, boolean write_to_cache)
					throws IOException {
		return getInputStream(url, cache_option, write_to_cache, null);
	}

	public static InputStream getInputStream(String url, int cache_option, boolean write_to_cache, Map<String,String> headers)
					throws IOException {
		return getInputStream(url, cache_option, write_to_cache, headers, false);
	}

	/**
	 * @param url URL to load.
	 * @param cache_option caching option (should be enum)
	 * @param write_to_cache Write to cache.
	 * @param headers a Map which when getInputStream() returns will be populated with any headers returned from the url
	 *      Each entry will be either: { header name ==> header value }
	 *        OR if multiple headers have same name, then value of entry will be a List of the header values:
	 *                                 { header name ==> [header value 1, header value 2, ...] }
	 * headers will get cleared of any entries it had before getting passed as arg
	 * @param fileMayNotExist Don't warn if file doesn't exist.
	 * @return
	 * @throws java.io.IOException
	 */
	private static InputStream getInputStream(String url, int cache_option, boolean write_to_cache, Map<String,String> headers, boolean fileMayNotExist)
					throws IOException {
		//look to see if a sessionId is present in the headers
		String sessionId = null;
		if (headers != null) {
			if (headers.containsKey("sessionId")) {
				sessionId = headers.get("sessionId");
			}
			//clear headers
			headers.clear();
		}
		
		// if url is a file url, and not caching files, then just directly return stream
		if ((!CACHE_FILE_URLS) && isFile(url)) {
			//Application.getSingleton().logInfo("URL is file url, so not caching: " + furl);
			return getUncachedFileStream(url, sessionId, fileMayNotExist);
		}


		// if NORMAL_CACHE:
		//   if not in cache, then return input stream from http connection
		//   if in cache, then check URL content has changed via GET with if-modified-since header based
		//        on modification date of cached file
		//      if content is returned, check last-modified header just to be sure (some servers might ignore
		//        if-modified-since header?)
		InputStream result_stream = null;
		File cache_file = getCacheFile(cache_content_root, url);
		File header_cache_file = getCacheFile(cache_header_root, url);
		long local_timestamp = -1;
		if (cache_file.exists()) {
			local_timestamp = cache_file.lastModified();
		}
		URLConnection conn = null;
		long remote_timestamp = 0;
		boolean url_reachable = false;
		int http_status = -1;

		if (offline) {
			// ignore whatever option was specified when we are offline, only the
			// cache is available.
			cache_option = ONLY_CACHE;
		}

		// if offline or if cache_option == ONLY_CACHE, then don't even try to retrieve from url
		if (cache_option != ONLY_CACHE) {
			try {
				URL theurl = new URL(url);
				conn = theurl.openConnection();
				conn.setConnectTimeout(CONNECT_TIMEOUT);
				conn.setReadTimeout(READ_TIMEOUT);

				conn.setRequestProperty("Accept-Encoding", "gzip");

				//set sessionId?
				if (sessionId != null) {
					conn.setRequestProperty("Cookie", sessionId);
				}
				//don't set if you are ignoring the cache, otherwise the server won't return the content if there's been no modification!
				if (cache_file.exists() && cache_option != IGNORE_CACHE) {
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

				if (conn instanceof HttpURLConnection) {
					HttpURLConnection hcon = (HttpURLConnection) conn;
					http_status = hcon.getResponseCode();
				}
				// Status codes:
				//     1xx Informational
				//     2xx Success
				//     3xx Redirection
				//     4xx Client Error
				//     5xx Server Error
				//  So only consider URL reachable if 2xx or 3xx (not quite sure what to do yet with redirection)
				url_reachable = ((http_status >= 200) && (http_status < 400));
			} catch (IOException ioe) {
				url_reachable = false;
			}
			if (!url_reachable) {
				if (!fileMayNotExist) {
					Application.getSingleton().logWarning("URL not reachable, status code = " + http_status + ": " + url);
				}
				if (headers != null) {
					headers.put("LocalUrlCacher", URL_NOT_REACHABLE);
				}
				// if (! cached) { throw new IOException("URL is not reachable, and is not cached!"); }
				if (!cache_file.exists()) {
					if (!fileMayNotExist) {
						Application.getSingleton().logWarning("URL is not reachable, and is not cached!");
					}
					return null;
				}
			}
		}
		
		// if cache_option == IGNORE_CACHE, then don't even try to retrieve from cache
		if (cache_file.exists() && (cache_option != IGNORE_CACHE)) {
			result_stream = TryToRetrieveFromCache(url_reachable, http_status, cache_file, remote_timestamp, local_timestamp, url, cache_option, result_stream, headers, header_cache_file);
		}

		// Need to get data from URL, because no cache hit, or stale, or cache_option set to IGNORE_CACHE...
		if (result_stream == null && url_reachable && (cache_option != ONLY_CACHE)) {
			result_stream = RetrieveFromURL(conn, headers, write_to_cache, cache_file, header_cache_file);
		}

		if (headers != null && DEBUG_CONNECTION) {
			reportHeaders(url, headers);
		}
		if (result_stream == null) {
			Application.getSingleton().logWarning("LocalUrlCacher couldn't get content for: " + url);
			System.out.println("LocalUrlCacher couldn't get content for: " + url);
		}
		return result_stream;
	}

		private static InputStream getUncachedFileStream(String url, String sessionId, boolean fileMayNotExist) throws MalformedURLException, IOException {
		URL furl = new URL(url);
		URLConnection huc = furl.openConnection();
		huc.setConnectTimeout(CONNECT_TIMEOUT);
		huc.setReadTimeout(READ_TIMEOUT);
		//set sessionId
		if (sessionId != null) {
			huc.setRequestProperty("Cookie", sessionId);
		}
		InputStream fstr = null;
		try {
			fstr = huc.getInputStream();
		} catch (FileNotFoundException ex) {
			if (fileMayNotExist) {
				System.out.println("Couldn't find file " + url + ", but it's optional.");
				return null; // We don't care if the file doesn't exist.
			}
		}
		//Application.getSingleton().logInfo("URL is file url, so not caching: " + furl);
		return fstr;
	}


	private static byte[] ReadIntoContentArray(int content_length, BufferedInputStream bis) throws IOException {
		if (content_length < 0) {
			return loadContentInChunks(bis);
		}

		// if content_length header was set, can load based on length
		byte[] content = new byte[content_length];
		int total_bytes_read = 0;
		while (total_bytes_read < content_length) {
			int bytes_read = bis.read(content, total_bytes_read, content_length - total_bytes_read);
			total_bytes_read += bytes_read;
		}
		if (total_bytes_read != content_length) {
			Application.getSingleton().logWarning("Bytes read not same as content length");
			System.out.println("Bytes read not same as content length");
		}
		return content;
	}


	/**
	 * if no content_length header, then need to load a chunk at a time
	 * till find end, then piece back together into content byte array
	 * Note, must set initial capacity to 1000 to avoid stream loading interruption.
	 * @param bis
	 * @param content_length
	 * @param content
	 * @return
	 * @throws IOException
	 */
	private static byte[] loadContentInChunks(BufferedInputStream bis) throws IOException {
		if (DEBUG_CONNECTION) {
			System.out.println("No content length header, so doing piecewise loading");
			Application.getSingleton().logDebug("No content length header, so doing piecewise loading");
		}
		byte[] content = null;
		ArrayList<byte[]> chunks = new ArrayList<byte[]>(1000);
		IntList byte_counts = new IntList(100);
		int total_byte_count = readChunks(bis, chunks, byte_counts);
		int content_length = total_byte_count;
		content = new byte[content_length];
		total_byte_count = 0;
		for (int i = 0; i < chunks.size(); i++) {
			byte[] chunk = chunks.get(i);
			int byte_count = byte_counts.get(i);
			if (byte_count > 0) {
				System.arraycopy(chunk, 0, content, total_byte_count, byte_count);
				total_byte_count += byte_count;
			}
		}
		chunks = null;
		return content;
	}


	private static int readChunks(BufferedInputStream bis, ArrayList<byte[]> chunks, IntList byte_counts) throws IOException {
		int chunk_count = 0;
		int chunk_size = 256 * 256;
		int total_byte_count = 0;
		int bytes_read = 0;
		while (bytes_read != -1) {
			byte[] orig_chunk = new byte[chunk_size];
			byte[] chunk;
			bytes_read = bis.read(orig_chunk, 0, chunk_size);
			if (bytes_read < chunk_size && bytes_read > 0) {
				// save space by shrinking the chunk
				chunk = new byte[bytes_read];
				System.arraycopy(orig_chunk, 0, chunk, 0, bytes_read);
				orig_chunk = null;
			} else {
				chunk = orig_chunk;
			}

			if (DEBUG_CONNECTION) {
				Application.getSingleton().logDebug("   chunk: " + chunk_count + ", byte count: " + bytes_read);
				if (bytes_read != chunk_size) {
					System.out.println("chunk: " + chunk_count + ", byte count: " + bytes_read);
				}
			}
			if (bytes_read > 0) {
				total_byte_count += bytes_read;
				chunks.add(chunk);
				byte_counts.add(bytes_read);
			}
			chunk_count++;
		}
		if (DEBUG_CONNECTION) {
			Application.getSingleton().logDebug("total bytes: " + total_byte_count + ", chunks with > 0 bytes: " + chunks.size());
			System.out.println("total bytes: " + total_byte_count + ", chunks with > 0 bytes: " + chunks.size());
		}
		return total_byte_count;
	}


	private static InputStream TryToRetrieveFromCache(boolean url_reachable, int http_status, File cache_file, long remote_timestamp, long local_timestamp, String url, int cache_option, InputStream result_stream, Map headers, File header_cache_file) throws IOException, FileNotFoundException {
		if (url_reachable) {
			//  has a timestamp and response contents not modified since local cached copy last modified, so use local
			if (http_status == HttpURLConnection.HTTP_NOT_MODIFIED) {
				if (DEBUG_CONNECTION) {
					Application.getSingleton().logInfo("Received HTTP_NOT_MODIFIED status for URL, using cache: " + cache_file);
				}
				result_stream = new BufferedInputStream(new FileInputStream(cache_file));
			} else if (remote_timestamp > 0 && remote_timestamp <= local_timestamp) {
				if (DEBUG_CONNECTION) {
					Application.getSingleton().logInfo("Cache exists and is more recent, using cache: " + cache_file);
				}
				result_stream = new BufferedInputStream(new FileInputStream(cache_file));
			} else {
				if (DEBUG_CONNECTION) {
					Application.getSingleton().logInfo("cached file exists, but URL is more recent, so reloading cache: " + url);
				}
				result_stream = null;
			}
		} else {
			// url is not reachable
			if (cache_option != ONLY_CACHE) {
				if (DEBUG_CONNECTION) {
					Application.getSingleton().logWarning("Remote URL not reachable: " + url);
				}
			}
			if (DEBUG_CONNECTION) {
				Application.getSingleton().logInfo("Loading cached file for URL: " + url);
			}
			result_stream = new BufferedInputStream(new FileInputStream(cache_file));
		}
		// using cached content, so should also use cached headers
		//   eventuallly want to improve so headers get updated if server is accessed and url is reachable
		if (result_stream != null && headers != null && header_cache_file.exists()) {
			BufferedInputStream hbis = new BufferedInputStream(new FileInputStream(header_cache_file));
			Properties headerprops = new Properties();
			headerprops.load(hbis);
			headers.putAll(headerprops);
			GeneralUtils.safeClose(hbis);
		}
		return result_stream;
	}

	private static InputStream RetrieveFromURL(URLConnection conn, Map<String,String> headers, boolean write_to_cache, File cache_file, File header_cache_file) throws IOException, IOException {
		int content_length = -1;
		InputStream connstr;
		String contentEncoding = conn.getHeaderField("Content-Encoding");
		boolean isGZipped = contentEncoding == null ? false : "gzip".equalsIgnoreCase(contentEncoding);
		if (isGZipped) {
			// unknown content length, stick with -1
			connstr = new GZIPInputStream(conn.getInputStream());
			if (DEBUG_CONNECTION) {
				System.out.println("gzipped stream, so ignoring reported content length of " + conn.getContentLength());
			}
		} else {
			connstr = conn.getInputStream();
			content_length = conn.getContentLength();
		}

		BufferedInputStream bis = null;
		byte[] content = null;
		try {
			bis = new BufferedInputStream(connstr);
			content = ReadIntoContentArray(content_length, bis);
			if (write_to_cache) {
				Properties headerprops = populateHeaderProperties(conn, headers);
				WriteToCache(content, cache_file, header_cache_file, headerprops);
			}
		} finally {
			GeneralUtils.safeClose(bis);
		}
		
		InputStream result_stream = new ByteArrayInputStream(content);
		return result_stream;
	}

	// populating header Properties (for persisting) and header input Map
	private static Properties populateHeaderProperties(URLConnection conn, Map<String, String> headers) {
		Map<String, List<String>> headermap = conn.getHeaderFields();
		Properties headerprops = new Properties();
		for (Map.Entry<String, List<String>> ent : headermap.entrySet()) {
			String key = ent.getKey();
			// making all header names lower-case
			List<String> vals = ent.getValue();
			if (vals.isEmpty()) {
				continue;
			}
			String val = vals.get(0);
			if (key == null) {
				key = HTTP_STATUS_HEADER;
			} // HTTP status code line has a null key, change so can be stored
			key = key.toLowerCase();
			headerprops.setProperty(key, val);
			if (headers != null) {
				headers.put(key, val);
			}
		}
		return headerprops;
	}

	public static void reportHeaders(String url, Map<String,String> headers) {
		if (headers != null) {
			Application.getSingleton().logInfo("   HEADERS for URL: " + url);
			for (Map.Entry<String,String> ent : headers.entrySet()) {
				Application.getSingleton().logInfo("   key: " + ent.getKey() + ", val: " + ent.getValue());
			}
		}
	}


	/**
	 *  Forces flushing of entire cache.
	 *  Simply removes all cached files.
	 */
	public static void clearCache() {
		DeleteFilesInDirectory(cache_header_root);
		DeleteFilesInDirectory(cache_content_root);
	}

	private static void DeleteFilesInDirectory(String filename) {
		File dir = new File(filename);
		if (dir.exists()) {
			for (File fil : dir.listFiles()) {
				fil.delete();
			}
		}
	}

	/** Returns the location of the root directory of the cache. */
	public static String getCacheRoot() {
		return cache_content_root;
	}

	/** Returns the current value of the persistent user preference PREF_CACHE_USAGE. */
	public static int getPreferredCacheUsage() {
		int cache_usage = UnibrowPrefsUtil.getIntParam(PREF_CACHE_USAGE, CACHE_USAGE_DEFAULT);
		return cache_usage;
	}

	public static void setPreferredCacheUsage(int usage) {
		UnibrowPrefsUtil.saveIntParam(LocalUrlCacher.PREF_CACHE_USAGE, usage);
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
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void loadSynonyms(SynonymLookup lookup, String synonym_loc) {
		InputStream syn_stream = null;
		try {
			// Don't cache.  Don't warn user if the synonyms file doesn't exist.
			syn_stream = LocalUrlCacher.getInputStream(synonym_loc, getPreferredCacheUsage(), false, null, true);
		} catch (IOException ioe) {
			GeneralUtils.safeClose(syn_stream);
		}

		if (syn_stream == null) {
			//Application.getSingleton().logWarning("Unable to load synonym data from: " + synonym_loc);
			return;
		}

		Application.getSingleton().logInfo("Synonyms found at: " + synonym_loc);

		try {
			lookup.loadSynonyms(syn_stream);
		} catch (final Throwable t) {
			// use Throwable so out-of-memory exceptions can be caught
			Application.getSingleton().logWarning("Error while loading synonym data from: " + synonym_loc);
			t.printStackTrace();
		} finally {
			GeneralUtils.safeClose(syn_stream);
		}
	}

	private static void WriteToCache(byte[] content, File cache_file, File header_cache_file, Properties headerprops) throws IOException, IOException, FileNotFoundException {
		if (content != null && content.length > 0) {
			if (DEBUG_CONNECTION) {
				Application.getSingleton().logInfo("writing content to cache: " + cache_file.getPath());
				System.out.println("writing content to cache: " + cache_file.getPath());
			}
			// write data from URL into a File
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cache_file));
			// no API for returning number of bytes successfully written, so write all in one shot...
			bos.write(content, 0, content.length);
			GeneralUtils.safeClose(bos);
		}
		// cache headers also -- in [cache_dir]/headers ?
		if (DEBUG_CONNECTION) {
			Application.getSingleton().logInfo("writing headers to cache: " + header_cache_file.getPath());
			System.out.println("writing headers to cache: " + header_cache_file.getPath());
		}
		BufferedOutputStream hbos = new BufferedOutputStream(new FileOutputStream(header_cache_file));
		headerprops.store(hbos, null);
		GeneralUtils.safeClose(hbos);
	}
}
