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
package com.lorainelab.das;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.util.ErrorHandler;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.NotResponding;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.XMLUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @version $Id: DasSource.java 9455 2011-11-17 16:37:09Z lfrohman $
 */
public final class DasSource {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DasSource.class);

    public final static String ENTRY_POINTS = "entry_points";
    public final static String TYPES = "types";

    private final URL server;
    private final URL master;
    private final URL primary;
    private final DataProvider primaryServer;
    private final String id;
    private final Set<String> sources;
    private final Set<String> entryPoints;
    private boolean entriesInitialized = false;
    private GenomeVersion genomeVersion;

    public DasSource(URL server, URL master, URL primary, DataProvider primaryServer) {
        sources = new HashSet<>();
        entryPoints = new LinkedHashSet<>();
        this.server = server;
        this.master = master;
        this.primary = primary;
        this.id = getID(master);
        this.primaryServer = primaryServer;
    }

    static String getID(URL master) {
        String path = master.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.substring(1 + path.lastIndexOf('/'), path.length());
    }

    public String getId() {
        return id;
    }

    synchronized void add(String source) {
        sources.add(source);
    }

    /**
     * Equivalent to {@link GenometryModel#addSeqGroup(String)} with the
     * id from {@link #getId()}.
     *
     * @return a non-null AnnotatedSeqGroup representing this genome
     */
    public GenomeVersion getGenome() {
        if (genomeVersion == null) {
            // cache, otherwise we potentially are doing thousands of synonym lookups
            genomeVersion = GenometryModel.getInstance().addGenomeVersion(this.getId());
        }
        return genomeVersion;
    }

    public Set<String> getEntryPoints() {
        if (!entriesInitialized) {
            initEntryPoints();
        }
        return entryPoints;
    }

    /**
     * Get entry points from das server.
     */
    private boolean initEntryPoints() {
        String entry_point = master.getPath() + "/" + ENTRY_POINTS;
        String pri_entry_point = id + "/" + ENTRY_POINTS;
        try (InputStream stream = getInputStream(master, entry_point, pri_entry_point, "Das Entry Request")) {
            if (stream == null) {
                Logger.getLogger(this.getClass().getName()).log(
                        Level.SEVERE, "Could not contact server at {0}", master);
                return false;
            }
            Document doc = XMLUtils.getDocument(stream);
            NodeList segments = doc.getElementsByTagName("SEGMENT");
            addSegments(segments);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            ErrorHandler.errorPanel("Error initializing DAS entry points for\n" + getId() + " on " + server, ex, Level.SEVERE);
        } finally {
            synchronized (this) {
                entriesInitialized = true;	// set even if there's an error?
            }
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
                entryPoints.add(segid);
            }
        }
    }

    private InputStream getInputStream(URL server, String query, String pri_default, String log_string) throws IOException {
        URL load_url = getLoadURL(server, query, pri_default);
        InputStream istr = LocalUrlCacher.getInputStream(load_url);
        /**
         * Check to see if trying to load from primary server but primary server is not responding *
         */
        if (istr == null && isLoadingFromPrimary()) {

            Logger.getLogger(DasSource.class.getName()).log(
                    Level.WARNING, "Primary Server :{0} is not responding. So disabling it for this session.", primaryServer.getName());
            primaryServer.setStatus(NotResponding);

            load_url = getLoadURL(server, query, pri_default);
            istr = LocalUrlCacher.getInputStream(load_url);
        }
        Logger.getLogger(DasServerInfo.class.getName()).log(
                Level.INFO, "{0} : {1}", new Object[]{log_string, load_url});
        return istr;
    }

    private boolean isLoadingFromPrimary() {
        return (primary != null && primaryServer != null && !primaryServer.getStatus().equals(NotResponding));
    }

    private URL getLoadURL(URL server, String query, String pri_default) throws MalformedURLException {
        if (!isLoadingFromPrimary()) {
            return new URL(server, query);
        }

        return new URL(primary, pri_default + ".xml");
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
