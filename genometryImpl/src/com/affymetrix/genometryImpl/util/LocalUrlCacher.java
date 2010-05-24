package com.affymetrix.genometryImpl.util;

import cern.colt.list.IntArrayList;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public final class LocalUrlCacher {

	private static final String cache_content_root = PreferenceUtils.getAppDataDirectory() + "cache/";
	private static final String cache_header_root = cache_content_root + "headers/";
	private static final String HTTP_STATUS_HEADER = "HTTP_STATUS";
	private static boolean DEBUG_CONNECTION = false;
	private static boolean CACHE_FILE_URLS = false;
	public static final int IGNORE_CACHE = 100;
	public static final int ONLY_CACHE = 101;
	public static final int NORMAL_CACHE = 102;

	public static enum CacheUsage {
		Normal(NORMAL_CACHE),
		Disabled(IGNORE_CACHE),
		Offline(ONLY_CACHE);

		public final int usage;
		
		CacheUsage(int usage) {
			this.usage = usage;
		}
	};

	public static enum CacheOption {
		IGNORE,
		ONLY,
		NORMAL;
	}

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
	 *
	 * @param b
	 */
	public static void setOffLine(boolean b) {
		offline = b;
	}

	/** Returns the value of the off-line flag.
	 *
	 * @return true if offline
	 */
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

	public static InputStream getInputStream(URL url) throws IOException {
		return getInputStream(url, true, null, null);
	}

	public static InputStream getInputStream(URL url, boolean write_to_cache, Map<String, String> rqstHeaders, Map<String, List<String>> respHeaders) throws IOException {
		return getInputStream(url.toString(), getPreferredCacheUsage(), write_to_cache, rqstHeaders, respHeaders, false);
	}

	public static InputStream getInputStream(String url) throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), true);
	}

	public static InputStream getInputStream(String url, boolean write_to_cache, Map<String,String> rqstHeaders)
					throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), write_to_cache, rqstHeaders);
	}
	public static InputStream getInputStream(String url, boolean write_to_cache, Map<String,String> rqstHeaders, boolean fileMayNotExist)
					throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), write_to_cache, rqstHeaders, null, fileMayNotExist);
	}

	public static InputStream getInputStream(String url, boolean write_to_cache)
					throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), write_to_cache);
	}

	private static InputStream getInputStream(String url, int cache_option, boolean write_to_cache)
					throws IOException {
		return getInputStream(url, cache_option, write_to_cache, null);
	}

	public static InputStream getInputStream(String url, int cache_option, boolean write_to_cache, Map<String,String> rqstHeaders)
					throws IOException {
		return getInputStream(url, cache_option, write_to_cache, rqstHeaders, null, false);
	}

	/**
	 * @param url URL to load.
	 * @param cache_option caching option (should be enum)
	 * @param write_to_cache Write to cache.
	 * @param rqstHeaders a Map which when getInputStream() returns will be populated with any headers returned from the url
	 *      Each entry will be either: { header name ==> header value }
	 *        OR if multiple headers have same name, then value of entry will be a List of the header values:
	 *                                 { header name ==> [header value 1, header value 2, ...] }
	 * headers will get cleared of any entries it had before getting passed as arg
	 * @param fileMayNotExist Don't warn if file doesn't exist.
	 * @return input stream from the loaded url
	 * @throws java.io.IOException
	 */
	private static InputStream getInputStream(String url, int cache_option, boolean write_to_cache, Map<String,String> rqstHeaders, Map<String, List<String>> respHeaders, boolean fileMayNotExist)
					throws IOException {
		//look to see if a sessionId is present in the headers
		String sessionId = null;
		if (rqstHeaders != null) {
			if (rqstHeaders.containsKey("sessionId")) {
				sessionId = rqstHeaders.get("sessionId");
			}
			//clear headers
			rqstHeaders.clear();
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
				conn = connectToUrl(url, conn, sessionId, cache_file, cache_option, local_timestamp);

				if (DEBUG_CONNECTION) {
					reportHeaders(conn);
				}

				if (respHeaders != null) {
					respHeaders.putAll(conn.getHeaderFields());
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
			} catch (Exception e) {
				e.printStackTrace();
				url_reachable = false;
			}
			if (!url_reachable) {
				if (!fileMayNotExist) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING,
							"URL not reachable, status code = " + http_status + ": " + url);
				}
				if (rqstHeaders != null) {
					rqstHeaders.put("LocalUrlCacher", URL_NOT_REACHABLE);
				}
				// if (! cached) { throw new IOException("URL is not reachable, and is not cached!"); }
				if (!cache_file.exists()) {
					if (!fileMayNotExist) {
						Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING,
								"URL is not reachable, and is not cached!");
					}
					return null;
				}
			}
		}
		
		// if cache_option == IGNORE_CACHE, then don't even try to retrieve from cache
		if (cache_file.exists() && (cache_option != IGNORE_CACHE)) {
			result_stream = TryToRetrieveFromCache(url_reachable, http_status, cache_file, remote_timestamp, local_timestamp, url, cache_option);
			if (rqstHeaders != null && header_cache_file.exists()) {
				retrieveHeadersFromCache(rqstHeaders, header_cache_file);
			}
		}

		// Need to get data from URL, because no cache hit, or stale, or cache_option set to IGNORE_CACHE...
		if (result_stream == null && url_reachable && (cache_option != ONLY_CACHE)) {
			result_stream = RetrieveFromURL(conn, rqstHeaders, write_to_cache, cache_file, header_cache_file);
		}

		if (rqstHeaders != null && DEBUG_CONNECTION) {
			reportHeaders(url, rqstHeaders);
		}
		if (result_stream == null) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING,
					"couldn't get content for: " + url);
		}
		return result_stream;
	}


	private static URLConnection connectToUrl(String url, URLConnection conn, String sessionId, File cache_file, int cache_option, long local_timestamp) throws MalformedURLException, IOException {
		URL theurl = new URL(url);
		conn = theurl.openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setRequestProperty("Accept-Encoding", "gzip");
		//set sessionId?
		if (sessionId != null) {
			conn.setRequestProperty("Cookie", sessionId);
		}
		if (cache_file.exists() && cache_option != IGNORE_CACHE) {
			conn.setIfModifiedSince(local_timestamp);
		} //    because some method calls on URLConnection like those below don't always throw errors
		//    when connection can't be opened -- which would end up allowing url_reachable to be set to true
		///   even when there's no connection
		conn.connect();
		return conn;
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
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
						"Couldn't find file " + url + ", but it's optional.");
				return null; // We don't care if the file doesn't exist.
			}
		}
		//Application.getSingleton().logInfo("URL is file url, so not caching: " + furl);
		return fstr;
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
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO, "Creating new cache directory: " + fil.getAbsolutePath());
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

	/**
	 * Invalidate cache file so it will be rebuilt if needed.
	 * @param url
	 */
	public static void invalidateCacheFile(String url) {
		File cache_file = getCacheFile(cache_content_root, url);
		if (cache_file.exists()) {
			cache_file.delete();
		}

		File header_cache_file = getCacheFile(cache_header_root, url);
		if (header_cache_file.exists()) {
			header_cache_file.delete();
		}
	}


	/**
	 * if no content_length header, then need to load a chunk at a time
	 * till find end, then piece back together into content byte array
	 * Note, must set initial capacity to 1000 to avoid stream loading interruption.
	 * @param bis
	 * @return loaded content in the form of a byte array
	 * @throws java.io.IOException
	 */
	private static byte[] loadContentInChunks(BufferedInputStream bis) throws IOException {
		if (DEBUG_CONNECTION) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
					"No content length header, so doing piecewise loading");
		}
		byte[] content = null;
		ArrayList<byte[]> chunks = new ArrayList<byte[]>(1000);
		IntArrayList byte_counts = new IntArrayList(100);
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


	private static int readChunks(BufferedInputStream bis, ArrayList<byte[]> chunks, IntArrayList byte_counts) throws IOException {
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
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
						"   chunk: " + chunk_count + ", byte count: " + bytes_read);
			}
			if (bytes_read > 0) {
				total_byte_count += bytes_read;
				chunks.add(chunk);
				byte_counts.add(bytes_read);
			}
			chunk_count++;
		}
		if (DEBUG_CONNECTION) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
					"total bytes: " + total_byte_count + ", chunks with > 0 bytes: " + chunks.size());
		}
		return total_byte_count;
	}


	private static InputStream TryToRetrieveFromCache(
			boolean url_reachable, int http_status, File cache_file, long remote_timestamp, long local_timestamp,
			String url, int cache_option)
			throws IOException, FileNotFoundException {
		if (url_reachable) {
			//  has a timestamp and response contents not modified since local cached copy last modified, so use local
			if (http_status == HttpURLConnection.HTTP_NOT_MODIFIED) {
				if (DEBUG_CONNECTION) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
							"Received HTTP_NOT_MODIFIED status for URL, using cache: " + cache_file);
				}		
			} else if (remote_timestamp > 0 && remote_timestamp <= local_timestamp) {
				if (DEBUG_CONNECTION) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
							"Cache exists and is more recent, using cache: " + cache_file);
				}
			} else {
				if (DEBUG_CONNECTION) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
						"cached file exists, but URL is more recent, so reloading cache: " + url);
				}
				return null;
			}
		} else {
			// url is not reachable
			if (cache_option != ONLY_CACHE) {
				if (DEBUG_CONNECTION) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING,
						"Remote URL not reachable: " + url);
				}
			}
			if (DEBUG_CONNECTION) {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
					"Loading cached file for URL: " + url);
			}
			
		}
		return new BufferedInputStream(new FileInputStream(cache_file));
	}

	private static void retrieveHeadersFromCache(Map<String, String> rqstHeaders, File header_cache_file) throws IOException {
		// using cached content, so should also use cached headers
		//   eventuallly want to improve so headers get updated if server is accessed and url is reachable
		BufferedInputStream hbis = null;
		try {
			hbis = new BufferedInputStream(new FileInputStream(header_cache_file));
			Properties headerprops = new Properties();
			headerprops.load(hbis);
			for (String propKey : headerprops.stringPropertyNames()) {
				rqstHeaders.put(propKey, headerprops.getProperty(propKey));
			}
		} finally {
			GeneralUtils.safeClose(hbis);
		}
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
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
						"gzipped stream, so ignoring reported content length of " + conn.getContentLength());
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
		
		return new ByteArrayInputStream(content);
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
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING,
					"Bytes read not same as content length");
		}
		return content;
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

	private static void reportHeaders(String url, Map<String, String> headers) {
		Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
				"   HEADERS for URL: " + url);
		for (Map.Entry<String, String> ent : headers.entrySet()) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
					"   key: " + ent.getKey() + ", val: " + ent.getValue());
		}
	}


	/**
	 *  Forces flushing of entire cache.
	 *  Simply removes all cached files.
	 */
	public static void clearCache() {
		Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO, "Clearing cache");
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

	/** Returns the location of the root directory of the cache.
	 * @return 
	 */
	public static String getCacheRoot() {
		return cache_content_root;
	}

	/** Returns the current value of the persistent user preference PREF_CACHE_USAGE.
	 *
	 * @return the preferred cache usage
	 */
	public static int getPreferredCacheUsage() {
		return PreferenceUtils.getIntParam(PREF_CACHE_USAGE, CACHE_USAGE_DEFAULT);
	}

	public static void setPreferredCacheUsage(int usage) {
		Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO, "Setting Caching mode to " + getCacheUsage(usage));
		PreferenceUtils.saveIntParam(LocalUrlCacher.PREF_CACHE_USAGE, usage);
	}

	public static void reportHeaders(URLConnection query_con) {
		try {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
					"URL: " + query_con.getURL().toString());
			int hindex = 0;
			while (true) {
				String val = query_con.getHeaderField(hindex);
				String key = query_con.getHeaderFieldKey(hindex);
				if (val == null && key == null) {
					break;
				}
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
						"   header:   key = " + key + ", val = " + val);
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
			syn_stream = LocalUrlCacher.getInputStream(synonym_loc, getPreferredCacheUsage(), false, null, null, true);
			if (syn_stream == null) {
				return;
			}
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
					"Synonyms found at: " + synonym_loc);
			lookup.loadSynonyms(syn_stream);
		} catch (IOException ioe) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING, "Unable to load synonyms from '" + synonym_loc + "'", ioe);
		} finally {
			GeneralUtils.safeClose(syn_stream);
		}
	}

	private static void WriteToCache(byte[] content, File cache_file, File header_cache_file, Properties headerprops) throws IOException, IOException, FileNotFoundException {
		writeContentToCache(content, cache_file);
		writeHeadersToCache(header_cache_file, headerprops);
	}

	private static void writeContentToCache(byte[] content, File cache_file) throws IOException {
		if (content != null && content.length > 0) {
			if (DEBUG_CONNECTION) {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
					"writing content to cache: " + cache_file.getPath());
			}
			BufferedOutputStream bos = null;
			try {
				bos = new BufferedOutputStream(new FileOutputStream(cache_file));
				// no API for returning number of bytes successfully written, so write all in one shot...
				bos.write(content, 0, content.length);
			} finally {
				GeneralUtils.safeClose(bos);
			}
		}
	}

	private static void writeHeadersToCache(File header_cache_file, Properties headerprops) throws IOException {
		// cache headers also -- in [cache_dir]/headers ?
		if (DEBUG_CONNECTION) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
				"writing headers to cache: " + header_cache_file.getPath());
		}
		BufferedOutputStream hbos = null;
		try {
			hbos = new BufferedOutputStream(new FileOutputStream(header_cache_file));
			headerprops.store(hbos, null);
		} finally {
			GeneralUtils.safeClose(hbos);
		}
	}

	public static final CacheUsage getCacheUsage(int usage) {
		for(CacheUsage u : CacheUsage.values()) {
			if(u.usage == usage) { return u; }
		}

		return null;
	}

	public static File convertURIToFile(URI uri) {
		String scheme = uri.getScheme().toLowerCase();
		if (scheme.length() == 0 || scheme.equals("file")) {
			return new File(uri);
		}
		if (scheme.startsWith("http")) {
			InputStream istr = null;
			try {
				String uriStr = uri.toString();
				istr = LocalUrlCacher.getInputStream(uriStr);
				StringBuffer stripped_name = new StringBuffer();
				InputStream str = GeneralUtils.unzipStream(istr, uriStr, stripped_name);
				String stream_name = stripped_name.toString();
				if (str instanceof BufferedInputStream) {
					str = (BufferedInputStream) str;
				} else {
					str = new BufferedInputStream(str);
				}
				return GeneralUtils.convertStreamToFile(str, stream_name.substring(stream_name.lastIndexOf("/")));
			} catch (IOException ex) {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(istr);
			}
		}
		Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.SEVERE,
				"URL scheme: " + scheme + " not recognized");
		return null;
	}

	/**
	 * Get stream associated with this uri.  Don't unzip here.
	 * @param uri
	 * @return
	 */
	public static BufferedInputStream convertURIToBufferedUnzippedStream(URI uri) {
		String scheme = uri.getScheme().toLowerCase();
		InputStream is = null;
		try {
			if (scheme.length() == 0 || scheme.equals("file")) {
				is = new FileInputStream(new File(uri));
			} else if (scheme.startsWith("http")) {
				is = LocalUrlCacher.getInputStream(uri.toString());
			} else {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.SEVERE,
					"URL scheme: " + scheme + " not recognized");
				return null;
			}

			StringBuffer stripped_name = new StringBuffer();
			InputStream str = GeneralUtils.unzipStream(is, uri.toString(), stripped_name);
			if (str instanceof BufferedInputStream) {
				return (BufferedInputStream) str;
			}
			return new BufferedInputStream(str);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Get stream associated with this uri.  Don't unzip here.
	 * @param uri
	 * @return
	 */
	public static BufferedInputStream convertURIToBufferedStream(URI uri) {
		String scheme = uri.getScheme().toLowerCase();
		InputStream is = null;
		try {
			if (scheme.length() == 0 || scheme.equals("file")) {
				is = new FileInputStream(new File(uri));
			} else if (scheme.startsWith("http")) {
				is = LocalUrlCacher.getInputStream(uri.toString());
			} else {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.SEVERE,
					"URL scheme: " + scheme + " not recognized");
				return null;
			}

			if (is instanceof BufferedInputStream) {
				return (BufferedInputStream) is;
			}
			return new BufferedInputStream(is);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static boolean isValidURL(String url){
		URI uri = null;

		try {
			uri = new URI(url);
		} catch (URISyntaxException ex) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING, null, "Invalid url :" + url);
		}
		
		return isValidURI(uri);

	}
	public static boolean isValidURI(URI uri){
		
		String scheme = uri.getScheme().toLowerCase();
		if (scheme.length() == 0 || scheme.equals("file")) {
			File f = new File(uri);
			if(f != null && f.exists()){
				return true;
			}
		}

		if (scheme.startsWith("http")) {
			InputStream istr = null;
			try {
				String uriStr = uri.toString();
				istr = LocalUrlCacher.getInputStream(uriStr);
				if(istr != null){
					return true;
				}
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING, "Invalid uri :" + uriStr);
			}catch(Exception ex){
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING, "Invalid uri :" + uri.getPath(), ex);
			}
			finally{GeneralUtils.safeClose(istr);}
		}
		
		return false;
	}
}
