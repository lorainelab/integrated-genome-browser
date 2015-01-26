/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.bookmarks;

import static com.affymetrix.igb.bookmarks.BookmarkConstants.DEFAULT_SERVLET_URL;
import static com.affymetrix.igb.bookmarks.BookmarkConstants.VALID_CONTEXT_ROOT_VALUES;
import static com.affymetrix.igb.service.api.IGBService.UTF8;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds a bookmark, which is simply a name associated with a URL.
 */
public final class Bookmark implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(Bookmark.class);
    
    public static final String SEQID = "seqid";
    public static final String VERSION = "version";
    public static final String START = "start";
    public static final String END = "end";
    public static final String CREATE = "create";
    public static final String MODIFIED = "modified";
    public static final String SELECTSTART = "selectstart";
    public static final String SELECTEND = "selectend";
    public static final String LOADRESIDUES = "loadresidues";
    public static final String COMMENT = "comment";
    public static final String DATA_URL = "data_url";
    public static final String DAS2_QUERY_URL = "das2_query";
    public static final String DAS2_SERVER_URL = "das2_server";
    public static final String QUERY_URL = "query_url";
    public static final String SERVER_URL = "server_url";
    /**
     * Optional paramater can be used to give the filetype extensions, such as
     * ".gff" of each of the urls given with {@link #DATA_URL}. If these
     * parameters are not used, then the filetype will be guessed based on the
     * content type returned from the URLConnection, or from the file name in
     * the URL. This parameter is optional, but if given there must be exactly
     * one paramater for each of the {@link #DATA_URL} parameters given
     */
    public static final String DATA_URL_FILE_EXTENSIONS = "data_url_file_extension";
    
    public static enum SYM {
        
        FEATURE_URL("feature_url_"),
        METHOD("sym_method_"),
        YPOS("sym_ypos_"),
        YHEIGHT("sym_yheight_"),
        COL("sym_col_"),
        BG("sym_bg_"),
        NAME("sym_name_");
        private String name;
        
        SYM(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }

    public static enum GRAPH {
        
        FLOAT("graph_float_"),
        SHOW_LABEL("graph_show_label_"),
        SHOW_AXIS("graph_show_axis_"),
        MINVIS("graph_minvis_"),
        MAXVIS("graph_maxvis_"),
        SCORE_THRESH("graph_score_thresh_"),
        MAXGAP_THRESH("graph_maxgap_thresh_"),
        MINRUN_THRESH("graph_minrun_thresh_"),
        SHOW_THRESH("graph_show_thresh_"),
        STYLE("graph_style_"),
        THRESH_DIRECTION("graph_thresh_direction_"),
        HEATMAP("graph_heatmap_"),
        COMBO("graph_combo_");
        private String name;
        
        GRAPH(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }

    private String name;
    private String comment;
    private URL url;
    
    public Bookmark(String name, String comment, String url) throws MalformedURLException {
        logger.trace("Creating bookmark");
        this.name = name;
        this.comment = comment;
        if (StringUtils.isBlank(name)) {
            this.name = "bookmark";
        }
        this.url = new URL(url);
    }

    /**
     * Takes a URL and parses the query parameters into a map. All entries will
     * be String arrays, as is expected by HttpServletRequest objects. Thus if
     * the url is http://www.abc.com/page?x=3&z&y=4&y=5 then the resulting Map
     * will have three String[] entries, for x={"3"} and z={""} and y={"4",
     * "5"}.
     *
     * @param url
     * @return a Map, which can be empty. All entries will be Strings. All keys
     * and values will be decoded with {@link URLDecoder}.
     */
    public static ListMultimap<String, String> parseParameters(URL url) {
        if (url != null) {
            return parseParametersFromQuery(url);
        } else {
            //return empty map;
            return ImmutableListMultimap.<String, String>builder().build();
        }
    }

    /**
     * Takes the query parameter string from a URL and parses the parameters
     * into a the given map. All entries will be String arrays, as is expected
     * by HttpServletRequest objects. Thus if the query string is x=3&z&y=4&y=5
     * then the resulting Map will have three String[] entries, for x={"3"} and
     * z={""} and y={"4", "5"}. All entries will be Strings.
     *
     * @param url
     * @return
     */
    public static ListMultimap<String, String> parseParametersFromQuery(URL url) {
        checkNotNull(url);
        return parseParametersFromQuery(url.getQuery());
    }
    
    public static ListMultimap<String, String> parseParametersFromQuery(String queryString) {
        checkNotNull(queryString);
        ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.<String, String>builder();
        try {
            final Iterable<String> results = Splitter.on("&")
                    //	.omitEmptyStrings() for now not omiting the empty strings to preserve old functionality
                    .trimResults()
                    .split(queryString);
            for (String result : results) {
                final String[] keyValuePair = result.split("=");
                String key = keyValuePair[0];
                String value = "";
                if (keyValuePair.length > 1) {
                    value = keyValuePair[1];
                }
                key = URLDecoder.decode(key, Charsets.UTF_8.displayName());
                value = URLDecoder.decode(value, UTF8);
                builder.put(key, value);
            }
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return builder.build();
    }

    /**
     * Constructs a UnibrowControlServer Bookmark URL based on the properties in
     * the Map. All keys and values will be encoded with {@link URLEncoder}. All
     * values should be String[] arrays, but any that are not will be converted
     * to a String by calling the toString() method of the object. (For String[]
     * objects, each String gets appended individually as a key=value pair, with
     * the same key name.)
     *
     * @param props
     * @return
     */
    public static String constructURL(ListMultimap<String, String> props) throws UnsupportedEncodingException {
        return constructURL(DEFAULT_SERVLET_URL, props);
    }

    /**
     * Constructs a GENERIC Bookmark URL based on the properties in the Map. All
     * keys and values will be encoded with {@link URLEncoder}. All values
     * should be String[] arrays, but any that are not will be converted to a
     * String by calling the toString() method of the object. (For String[]
     * objects, each String gets appended individually as a key=value pair, with
     * the same key name.)
     *
     * @param url_base The beginning part of a url, like "http://www.xxx.com" or
     * even "http://www.xxx.com?x=1&y=2".
     * @param props
     * @return
     * @throws java.io.UnsupportedEncodingException
     */
    public static String constructURL(String url_base, ListMultimap<String, String> props) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append(url_base);
        sb.append('?');
        ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.<String, String>builder();
        Set<String> keySet = props.keySet();
        for (String key : keySet) {
            key = URLEncoder.encode(key, Charsets.UTF_8.displayName());
            List<String> values = props.get(key);
            for (String value : values) {
                value = URLEncoder.encode(value, Charsets.UTF_8.displayName());
                builder.put(key, value);
            }
        }
        String joinedResult = Joiner.on("&")
                .withKeyValueSeparator("=")
                .join(builder.build().entries());
        sb.append(joinedResult);
        return sb.toString();
    }
    
    public ListMultimap<String, String> getParameters() {
        return parseParameters(url);
    }

    /**
     * Returns true if the Path of the Url matches
     * {@link SimpleBookmarkServer#SERVLET_NAME} or
     * {@link SimpleBookmarkServer#SERVLET_NAME_OLD} and the Host is
     * "localhost".
     *
     * @return
     */
    public boolean isValidBookmarkFormat() {
        String host = getURL().getHost();
        String path = getURL().getPath();
        String contextRoot = path.substring(1);
        if (StringUtils.equalsIgnoreCase(host, "localhost") || StringUtils.equals(host, "127.0.0.1")) {
            if (VALID_CONTEXT_ROOT_VALUES.contains(contextRoot)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "Bookmark: '" + this.name + "' -> '" + this.url.toExternalForm() + "'";
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public String getComment() {
        return this.comment;
    }
    
    public URL getURL() {
        return this.url;
    }
    
    void setURL(URL url) {
        this.url = url;
    }
}
