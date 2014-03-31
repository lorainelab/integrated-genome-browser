/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.bookmarks;

import com.google.common.collect.ImmutableList;

/**
 *
 * @author lorainelab
 */
public class BookmarkConstants {
	private BookmarkConstants() {
		//private constructor to prevent instantiation
	}
	public static final String GALAXY_REQUEST = "igbGalaxyDataView";
	public static final String GALAXY_THREAD_DESCRIPTION = "Loading Galaxy Data";
	public static final String DEFAULT_BOOKMARK_THREAD_DESCRPTION = "Loading Bookmark";
	public static final String DEFAULT_SCRIPT_EXTENSION = "igb";
	public static final String FAVICON_REQUEST = "favicon.ico";
	public static final String FOCUS_IGB_COMMAND = "bringIGBToFront";
	public static final String UNKNOWN_SEQID = "unknown";
	
	/**
	 * The OLD name of the IGB servlet, "UnibrowControl".
	 */
	public static final String SERVLET_NAME_OLD = "UnibrowControl";
	/**
	 * The current name of the IGB servlet, "IGBControl". Current versions of
	 * IGB will respond to both this and {@link #SERVLET_NAME_OLD}, but versions
	 * up to and including 4.56 will respond ONLY to the old name.
	 */
	public static final String SERVLET_NAME = "IGBControl";
	public static final ImmutableList<String> VALID_CONTEXT_ROOT_VALUES = ImmutableList.<String>builder().add(GALAXY_REQUEST).add(SERVLET_NAME_OLD).add(SERVLET_NAME).build();
	
	public static final int DEFAULT_SERVER_PORT = 7085;
	/**
	 * The basic localhost URL that starts a call to IGB; for
	 * backwards-compatibility with versions of IGB 4.56 and earlier, the old
	 * name {@link #SERVLET_NAME_OLD} is used.
	 */
	public static final String DEFAULT_SERVLET_URL = "http://localhost:" + DEFAULT_SERVER_PORT + "/" + SERVLET_NAME_OLD;

}
