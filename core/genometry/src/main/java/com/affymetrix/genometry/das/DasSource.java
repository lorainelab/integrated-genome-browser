/**
 * Copyright (c) 2001-2004 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License").
 * A copy of the license must be included with any distribution of
 * this source code.
 * Distributions from Affymetrix, Inc., place this in the
 * IGB_LICENSE.html file.
 *
 * The license is also available at
 * http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.das;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.XMLUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @version $Id: DasSource.java 9455 2011-11-17 16:37:09Z lfrohman $
 */
public final class DasSource {

    public final static String ENTRY_POINTS = "entry_points";
    public final static String TYPES = "types";

    private final URL server;
    private final URL master;
    private final String id;
    private final Set<String> sources = new HashSet<>();
    private final Set<String> entry_points = new LinkedHashSet<>();
    private final Map<String, String> types = new LinkedHashMap<>();
    private boolean entries_initialized = false;
    private boolean types_initialized = false;
    private AnnotatedSeqGroup genome = null;	// lazily instantiate

    public DasSource(URL server, URL master) {
        this.server = server;
        this.master = master;
        this.id = getID(master);
    }

    static String getID(URL master) {
        String path = master.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.substring(1 + path.lastIndexOf('/'), path.length());
    }

    public String getID() {
        return id;
    }

    synchronized void add(String source) {
        sources.add(source);
    }

    /**
     * Equivalent to {@link GenometryModel#addSeqGroup(String)} with the
     * id from {@link #getID()}.
     *
     * @return a non-null AnnotatedSeqGroup representing this genome
     */
    public AnnotatedSeqGroup getGenome() {
        if (genome == null) {
            // cache, otherwise we potentially are doing thousands of synonym lookups
            genome = GenometryModel.getInstance().addSeqGroup(this.getID());
        }
        return genome;
    }

    public Set<String> getEntryPoints() {
        if (!entries_initialized) {
            initEntryPoints();
        }
        return entry_points;
    }

    public Map<String, String> getTypes() {
        if (!types_initialized) {
            initTypes();
        }
        return types;
    }

    /**
     * Get entry points from das server.
     */
    private boolean initEntryPoints() {
        InputStream stream = null;
        try {
            String entry_point = master.getPath() + "/" + ENTRY_POINTS;
            stream = getInputStream(master, entry_point, "Das Entry Request");
            if (stream == null) {
                Logger.getLogger(this.getClass().getName()).log(
                        Level.SEVERE, "Could not contact server at {0}", master);
                return false;
            }
            Document doc = XMLUtils.getDocument(stream);
            NodeList segments = doc.getElementsByTagName("SEGMENT");
            addSegments(segments);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            ErrorHandler.errorPanel("Error initializing DAS entry points for\n" + getID() + " on " + server, ex, Level.SEVERE);
        } finally {
            GeneralUtils.safeClose(stream);
            entries_initialized = true;	// set even if there's an error?
        }
        return true;
    }

    private void addSegments(NodeList segments) throws NumberFormatException {
        int length = segments.getLength();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "segments: {0}", length);
        for (int i = 0; i < length; i++) {
            Element seg = (Element) segments.item(i);
            String segid = seg.getAttribute("id");
            String stopstr = seg.getAttribute("stop");
            String sizestr = seg.getAttribute("size"); // can optionally use "size" instead of "start" and "stop"
            int stop = 1;
            if (stopstr != null && !stopstr.isEmpty()) {
                stop = Integer.parseInt(stopstr);
            } else if (sizestr != null) {
                stop = Integer.parseInt(sizestr);
            }
            synchronized (this) {
                getGenome().addSeq(segid, stop);
                entry_points.add(segid);
            }
        }
    }

    private void initTypes() {
        Set<String> badSources = new HashSet<>();

        for (String source : sources) {
            if (!initType(source)) {
                badSources.add(source);
            }
        }
        /* Remove any failed sources */
        synchronized (this) {
            badSources.forEach(sources::remove);
            types_initialized = true;
        }
    }

    private boolean initType(String source) {
        InputStream stream = null;
        try {
            String type_req = source + "/" + TYPES;
            URL typesURL = new URL(server, type_req);
            URL testMasterURL = new URL(master, master.getPath() + "/" + TYPES);
            stream = getInputStream(server, type_req, "Das Types Request");
            if (stream == null) {
                Logger.getLogger(this.getClass().getName()).log(
                        Level.WARNING, "Types request failed for {0}, skipping", typesURL);
                return false;
            }
            Document doc = XMLUtils.getDocument(stream);
            NodeList typelist = doc.getElementsByTagName("TYPE");
            int typeLength = typelist.getLength();
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "types: {0}", typeLength);
            for (int i = 0; i < typeLength; i++) {
                Element typenode = (Element) typelist.item(i);
                String typeid = typenode.getAttribute("id");

                /* URL.equals() does DNS lookups! */
                String name = URLEquals(typesURL, testMasterURL) ? null : source + "/" + typeid;
                URL url = new URL(server, source + "/features?type=" + typeid + ";");
                //DasType type = new DasType(server, typeid, source, name);
                name = name == null ? typeid : name;
                synchronized (this) {
                    types.put(name, url.toExternalForm());
                }
            }
        } catch (MalformedURLException ex) {
            ErrorHandler.errorPanel("Error initializing DAS types for\n" + getID() + " on " + server, ex, Level.SEVERE);
            return false;
        } catch (ParserConfigurationException ex) {
            ErrorHandler.errorPanel("Error initializing DAS types for\n" + getID() + " on " + server, ex, Level.SEVERE);
            return false;
        } catch (SAXException ex) {
            ErrorHandler.errorPanel("Error initializing DAS types for\n" + getID() + " on " + server, ex, Level.SEVERE);
            return false;
        } catch (IOException ex) {
            ErrorHandler.errorPanel("Error initializing DAS types for\n" + getID() + " on " + server, ex, Level.SEVERE);
            return false;
        } finally {
            GeneralUtils.safeClose(stream);
        }
        return true;
    }

    /**
     * Custom equals for URLs since the default implementation will do a DNS lookup
     * on both URLs.
     *
     * @param url1
     * @param url2
     * @return true if the URLs are equal
     */
    private static boolean URLEquals(URL url1, URL url2) {
        int port1 = url1.getPort() == -1 ? url1.getDefaultPort() : url1.getPort();
        int port2 = url2.getPort() == -1 ? url2.getDefaultPort() : url2.getPort();
        String ref1 = url1.getRef() == null ? "" : url1.getRef();
        String ref2 = url2.getRef() == null ? "" : url2.getRef();

        return port1 == port2
                && ref1.equals(ref2)
                && url1.getProtocol().equalsIgnoreCase(url2.getProtocol())
                && url1.getHost().equalsIgnoreCase(url2.getHost())
                && url1.getFile().equals(url2.getFile());
    }

    private InputStream getInputStream(URL server, String query, String log_string) throws IOException {
        URL load_url = getLoadURL(server, query);
        InputStream istr = LocalUrlCacher.getInputStream(load_url);

        Logger.getLogger(DasServerInfo.class.getName()).log(
                Level.INFO, "{0} : {1}", new Object[]{log_string, load_url});
        return istr;
    }

    private URL getLoadURL(URL server, String query) throws MalformedURLException {
        return new URL(server, query);
    }

    public Set<String> getSources() {
        return sources;
    }

    public URL getMasterURL() {
        return master;
    }

    public URL getServerURL() {
        return server;
    }
}
