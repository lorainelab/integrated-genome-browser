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

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.parsers.BpsParser;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.FileTypeHolder;
import com.affymetrix.genometry.parsers.PSLParser;
import com.affymetrix.genometry.parsers.das.DASFeatureParser;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.lorainelab.igb.services.IgbService;
import static com.affymetrix.igb.view.load.GeneralLoadUtils.LOADING_MESSAGE_PREFIX;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @version $Id: UrlLoaderThread.java 7997 2011-04-11 19:43:07Z lfrohman $
 */
public final class UrlLoaderThread extends Thread {

    private static final GenometryModel gmodel = GenometryModel.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(UrlLoaderThread.class);

    private final URL[] urls;
    private final String[] tier_names;
    private final IgbService igbService;
    private final String[] file_extensions;

    /**
     * Creates a thread that can be used to load data. A ThreadProgressMonitor
     * will be opened to show the user that something is happening.
     *
     * @param smv The SeqMapView instance to load data into
     * @param urls The URLs that will load data
     * @param file_extensions File extensions, such as ".gff", to help determine
     * which parser to use if it is not possible to determine that in any other
     * way. It is ok for any of these to be either blank or null.
     * @param tier_names The names for the data tiers. If you specify
     * <code>null</code>, the tier names will be determined from the "type"
     * parameter of each URL. If a non-null array is provided, the length must
     * match the length of the das_urls array.
     */
    public UrlLoaderThread(IgbService igbService, URL[] urls, String[] file_extensions, String[] tier_names) {
        if (tier_names != null && urls.length != tier_names.length) {
            throw new IllegalArgumentException("Array lengths do not match");
        }
        this.igbService = igbService;
        this.urls = urls;
        this.tier_names = tier_names;
        this.file_extensions = file_extensions;
    }

    @Override
    public void run() {
        BioSeq aseq = gmodel.getSelectedSeq();
        GenomeVersion seq_group = gmodel.getSelectedGenomeVersion();
        try {
            // should really move to using gmodel's currently selected  _group_ of sequences rather than
            //    a single sequence...
            if (aseq == null) {
                throw new RuntimeException("UrlLoaderThread: aborting because there is no currently selected seq");
            }
            if (seq_group == null) {
                throw new RuntimeException("UrlLoaderThread: aborting because there is no currently selected seq group");
            }

            for (int i = 0; i < urls.length; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                URL url = urls[i];

                String tier_name;
                if (tier_names != null) {
                    tier_name = tier_names[i];
                } else {
                    tier_name = parseTermName(url, "DAS_Data");
                }
                String file_extension = null;
                if (file_extensions != null) {
                    file_extension = file_extensions[i];
                }

                logger.info("Attempting to load data from URL: {}",
                        url.toExternalForm());
                try {
                    try {
                        parseDataFromURL(url, file_extension, tier_name);
                    } finally {
                        igbService.removeNotLockedUpMsg(LOADING_MESSAGE_PREFIX + tier_name);
                    }
                } catch (IOException ioe) {
                    handleException(ioe);
                    continue; // try the next url
                }

                // update the view, except for the last time where we let the "finally" block do it
                if (i < urls.length) {
                    updateViewer(aseq);
                }
            }

        } catch (Exception e) {
            if (!(e instanceof InterruptedException)) {
                handleException(e);
            }
        } finally {
            //if (monitor != null) {monitor.closeDialogEventually();}
            // update the view again, mainly in case the thread was interrupted
            updateViewer(aseq);
        }
    }

    private void handleException(final Exception e) {
        SwingUtilities.invokeLater(() -> {
            if (e instanceof UnknownHostException) {
                igbService.setStatus("Unknown host: " + e.getMessage());
            } else if (e instanceof FileNotFoundException) {
                ErrorHandler.errorPanel("File not found", "File missing or not readable:\n " + e.getMessage());
            } else {
                igbService.setStatus(e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void updateViewer(final BioSeq seq) {
        SwingUtilities.invokeLater(() -> {
            try {
                igbService.getSeqMapView().setAnnotatedSeq(seq, true, true);
            } catch (Exception e) {
                handleException(e);
            }
        });
    }

    /**
     * Use SwingUtilities.invokeLater() to schedule this Thread to be started
     * later. This lets Swing finish-up whatever else it was doing before
     * calling start() on this Thread. (The Thread itself doesn't actually run
     * on the Swing event thread.)
     *
     * In many cases, you could just call start() instead of calling this. But,
     * if you already have some events pending on the Swing event thread, then
     * calling this will make sure they finish first. For example, this method
     * is needed in the UnibrowControlServlet when a manipulation of the
     * QuickLoad GUI needs to be followed by a file load.
     */
    public void runEventually() {
        // Note: we do NOT want to simply call SwingUtilities.invokeLater(this)
        // because that would cause this thread to actually run ON the Swing thread
        // (potentially freezing the GUI)
        SwingUtilities.invokeLater(this::start);
    }

    // Parses term names from a "type" string.
    // Returns default_value if parsing fails, or there is no "type" string.
    // Example:  "type=one;two;three"  -> {"one", "two", "three"}
    private static String parseTermName(URL url, String default_value) {
        //TODO: unlike the rest of this class, this IS specific to DAS and should be moved to DasUtils
        String value = null;
        String query = url.getQuery();
        try {
            int index1 = query.indexOf("type=");
            if (index1 > -1) {
                int index1b = index1 + 5;
                int index2 = query.indexOf(';', index1b);
                if (index2 == -1) {
                    index2 = query.length();
                }
                value = query.substring(index1b, index2);
            }
        } catch (Exception e) {
            // do nothing.  Just use the default string value
        }
        if (StringUtils.isBlank(value)) {
            value = default_value;
        }
        return value;
    }

    /**
     * Opens a binary data stream from the given url and adds the resulting data
     * to the given BioSeq.
     *
     * @param type a parameter passed on to parsePSL
     */
    private static void parseDataFromURL(URL url, String file_extension, String type)
            throws IOException {
        Map<String, List<String>> respHeaders = new HashMap<>();
        InputStream stream = null;
        List<String> list;
        String content_type = "content/unknown";
        int content_length = -1;

        try {
            stream = LocalUrlCacher.getInputStream(url, false, null, respHeaders);
            list = respHeaders.get("Content-Type");
            if (list != null && !list.isEmpty()) {
                content_type = list.get(0);
            }

            list = respHeaders.get("Content-Length");
            if (list != null && !list.isEmpty()) {
                try {
                    content_length = Integer.parseInt(list.get(0));
                } catch (NumberFormatException ex) {
                    content_length = -1;
                }
            }

            if (content_length == 0) { // Note: length == -1 means "length unknown"
                throw new IOException("\n" + url + " returned no data.");
            }

            parseDataFromStream(url, stream, content_type, file_extension, type);
        } finally {
            GeneralUtils.safeClose(stream);
        }
    }

    private static void parseDataFromStream(
            URL url, InputStream stream, String content_type, String file_extension, String type)
            throws IOException {
        BioSeq aseq = gmodel.getSelectedSeq();
        GenomeVersion group = gmodel.getSelectedGenomeVersion();
        if ("file".equalsIgnoreCase(url.getProtocol()) || "ftp".equalsIgnoreCase(url.getProtocol())) {
            logger.info("Attempting to load data from file: {}", url.toExternalForm());

            // Note: we want the filename so we can guess the filetype from the ending, like ".psl" or ".psl.gz"
            // url.getPath() is OK for this purpose, url.getFile() is not because
            // url.getFile() = url.getPath() + url.getQuery()
            String filename = url.getPath();
            load(stream, filename, group, aseq);
        } else if (content_type == null
                || content_type.startsWith("content/unknown")
                || content_type.startsWith("application/zip")
                || content_type.startsWith("application/octet-stream")) {
            logger.info("Attempting to load data from: {}", url.toExternalForm());
            logger.info("Using file extension: {}", file_extension);

            String filename = url.getPath();
            if (file_extension != null && !"".equals(file_extension.trim())) {
                if (!file_extension.startsWith(".")) {
                    filename += ".";
                }
                filename += file_extension;
            }
            load(stream, filename, group, aseq);
        } else if (content_type.startsWith("binary/bps")) {
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new BufferedInputStream(stream));
                BpsParser.parse(dis, type, null, group, false, true);
            } finally {
                GeneralUtils.safeClose(dis);
            }
        } else if (content_type.startsWith("text/plain")
                || content_type.startsWith("text/html")
                || content_type.startsWith("text/xml")) {
            // Note that some http servers will return "text/html" even when that is untrue.
            // we could try testing whether the filename extension is a recognized extension, like ".psl"
            // and if so passing to LoadFileAction.load(.. feat_request_con.getInputStream() ..)
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(stream);
                DASFeatureParser das_parser = new DASFeatureParser();
                das_parser.parse(bis, group);
            } catch (XMLStreamException ex) {
                logger.error("Unable to parse DAS response", ex);
            } finally {
                GeneralUtils.safeClose(bis);
            }
        } else if (content_type.startsWith("text/psl")) {
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(stream);
                PSLParser parser = new PSLParser();
                parser.enableSharedQueryTarget(true);
                parser.parse(bis, type, null, group, false, true);
            } finally {
                GeneralUtils.safeClose(bis);
            }
        } else {
            throw new IOException("Declared data type " + content_type + " cannot be processed");
        }
    }

    /**
     * Loads from an InputStream. Detects the type of file based on the filename
     * ending of the stream_name parameter, for example ".dasxml". The stream
     * will be passed through uncompression routines if necessary.
     */
    private static void load(InputStream instr, String stream_name, GenomeVersion selected_group, BioSeq input_seq) throws IOException {
        if (selected_group == null) {
            // this should never happen
            throw new IOException("Must select a genome before loading a file");
        }

        logger.info("loading file: {}", stream_name);

        Exception the_exception = null;
        InputStream str = null;

        try {
            StringBuffer stripped_name = new StringBuffer();
            str = GeneralUtils.unzipStream(instr, stream_name, stripped_name);
            stream_name = stripped_name.toString();

            if (str instanceof BufferedInputStream) {

            } else {
                str = new BufferedInputStream(str);
            }
            DoParse(str, selected_group, input_seq, stream_name);
        } catch (Exception ex) {
            the_exception = ex;
            //ErrorHandler.errorPanel(gviewerFrame, "ERROR", "Error loading file", ex);
        } finally {
            GeneralUtils.safeClose(str);
        }

        // The purpose of calling setSelectedGenomeVersion, even if identity of
        // the seq group has not changed, is to make sure that
        // the DataLoadView and the AnnotBrowserView update their displays.
        // (Because the contents of the seq group may have changed.)
        //
        // Note that this must be done regardless of whether this load() method was
        // called from inside this class or in loading a bookmark, etc.
        gmodel.setSelectedGenomeVersion(gmodel.getSelectedGenomeVersion());

        if (the_exception != null) {
            if (the_exception instanceof IOException) {
                throw (IOException) the_exception;
            } else {
                IOException new_exception = new IOException();
                new_exception.initCause(the_exception);
                throw new_exception;
            }
        }
    }

    private static void DoParse(
            InputStream str, GenomeVersion group, BioSeq input_seq,
            String stream_name)
            throws Exception {
        int dotIndex = stream_name.lastIndexOf('.');
        String annot_type = dotIndex <= 0 ? stream_name : stream_name.substring(0, dotIndex);
        FileTypeHandler fileTypeHandler = FileTypeHolder.getInstance().getFileTypeHandlerForURI(stream_name);
        if (fileTypeHandler == null) {
            logger.warn(
                    "ABORTING FEATURE LOADING, FORMAT NOT RECOGNIZED: {}", stream_name);
        } else {
            fileTypeHandler.getParser().parse(str, group, annot_type, stream_name, true);
        }
    }
}
