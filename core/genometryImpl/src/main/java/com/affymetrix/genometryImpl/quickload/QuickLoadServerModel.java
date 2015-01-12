/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometryImpl.quickload;

import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.parsers.LiftParser;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import com.google.common.base.Optional;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.jdom.JDOMException;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import org.xml.sax.SAXParseException;

/**
 *
 * @version $Id: QuickLoadServerModel.java 10893 2012-03-29 14:05:22Z hiralv $
 */
public final class QuickLoadServerModel {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(QuickLoadServerModel.class);
    private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
    private static final Pattern tab_regex = Pattern.compile("\t");
    private final String root_url;
    /**
     * Stores the names of the data set name. For example A_thaliana_Jun_2008
     * populated by loadGenomeNames()
     */
    private final List<String> genome_names = new ArrayList<>();
    /**
     * A set containing initialized genomes
     */
    private final Set<String> initialized = new HashSet<>();
    // A map from String genome name to a Map of (typeName,fileName) on the server for that group
    private final Map<String, List<AnnotMapElt>> genome2annotsMap = new HashMap<>();
    private static final Map<String, QuickLoadServerModel> url2quickload = new HashMap<>();
    private final String primary_url;
    private final GenericServer primaryServer;

    /**
     * Initialize quickload server model for given url.
     *
     * @param url	server url.
     */
    public QuickLoadServerModel(String url) {
        this(url, null, null);
    }

    private QuickLoadServerModel(String url, String pri_url, GenericServer priServer) {
        url = ServerUtils.formatURL(url, ServerTypeI.QuickLoad);

        if (pri_url != null) {
            pri_url = ServerUtils.formatURL(pri_url, ServerTypeI.QuickLoad);
        }

        root_url = url;
        primary_url = pri_url;
        primaryServer = priServer;

        logger.trace("( {}, {} )", new Object[]{root_url, primary_url});
    }

    /**
     * Initialize quickload server model for given url. Then add gnomes names
     * provided in set.
     *
     * @param url	server URL.
     * @param primary_url	URL of primary server.
     */
    public static synchronized QuickLoadServerModel getQLModelForURL(URL url,
            URL primary_url, GenericServer primaryServer) {

        String ql_http_root = url.toExternalForm();

        String primary_root = null;
        if (primary_url != null) {
            primary_root = primary_url.toExternalForm();
        }

        QuickLoadServerModel ql_server = url2quickload.get(ql_http_root);
        if (ql_server == null) {
            LocalUrlCacher.loadSynonyms(LOOKUP, ql_http_root + Constants.SYNONYMS_TXT);
            ql_server = new QuickLoadServerModel(ql_http_root, primary_root, primaryServer);
            url2quickload.put(ql_http_root, ql_server);
        }

        return ql_server;

    }

    /**
     * Initialize quickload server model for given url.
     */
    public static synchronized QuickLoadServerModel getQLModelForURL(URL url) {
        return getQLModelForURL(url, null, null);
    }

    /*
     * Remove server from list.
     */
    public static synchronized void removeQLModelForURL(String url) {
        if (url2quickload.get(url) != null) {
            QuickLoadServerModel model = url2quickload.get(url);
            if (model != null) {
                model.clean();
            }
            url2quickload.remove(url);
        }
    }

    private static boolean getCacheAnnots() {
        return true;
    }

    private String getRootUrl() {
        return root_url;
    }

    public List<String> getGenomeNames() {
        return genome_names;
    }

    private AnnotatedSeqGroup getSeqGroup(String genome_name) {
        return GenometryModel.getInstance().addSeqGroup(LOOKUP.findMatchingSynonym(
                GenometryModel.getInstance().getSeqGroupNames(), genome_name));
    }

    public List<AnnotMapElt> getAnnotsMap(String genomeName) {
        return this.genome2annotsMap.get(genomeName);
    }

    /**
     * Returns the list of String typeNames that this QuickLoad server has for
     * the genome with the given name. The list may (rarely) be empty, but never
     * null.
     */
    public List<String> getTypes(String genome_name) {
        genome_name = LOOKUP.findMatchingSynonym(genome_names, genome_name);
        if (!initialized.contains(genome_name)) {
            if (!initGenome(genome_name)) {
                return null;
            }
        }
        if (getAnnotsMap(genome_name) == null) {
            return Collections.<String>emptyList();
        }
        List<String> typeNames = new ArrayList<>();
        for (AnnotMapElt annotMapElt : getAnnotsMap(genome_name)) {
            typeNames.add(annotMapElt.title);
        }

        return typeNames;
    }

    public Map<String, String> getProps(String genomeName, String featureName) {
        Map<String, String> props = null;
        List<AnnotMapElt> annotList = getAnnotsMap(genomeName);
        if (annotList != null) {
            AnnotMapElt annotElt = AnnotMapElt.findTitleElt(featureName, annotList);
            if (annotElt != null) {
                return annotElt.props;
            }
        }
        return props;
    }

    private synchronized boolean initGenome(String genome_name) {
        logger.debug("initializing data for genome: {}", genome_name);
        boolean metaOK = loadSeqInfo(genome_name);
        if (metaOK && loadAnnotationNames(genome_name)) {
            initialized.add(genome_name);
            return true;
        }

        // Clear the type list if something went wrong.
        List<AnnotMapElt> annotList = getAnnotsMap(genome_name);
        if (annotList != null) {
            annotList.clear();
        }
        return metaOK;
    }

    /**
     * Determines the list of annotation files available in the genome
     * directory. Looks for ~genome_dir/annots.txt file which lists annotation
     * files available in same directory. Returns true or false depending on
     * whether the file is successfully loaded. You can retrieve the typeNames
     * with {@link #getTypes(String)}
     */
    private boolean loadAnnotationNames(String genome_name) {
        genome_name = LOOKUP.findMatchingSynonym(genome_names, genome_name);
        logger.debug("loading list of available annotations for genome: {}", genome_name);

        // Make a new list of typeNames, in case this is being re-initialized
        // If this search fails, then we're just returning an empty map.
        List<AnnotMapElt> annotList = new ArrayList<>();
        genome2annotsMap.put(genome_name, annotList);

        InputStream istr = null;
        InputStream validationIstr = null;
        String filename = null;
        try {
            filename = getPath(genome_name, Constants.ANNOTS_XML);
            istr = getInputStream(filename, false, true);
            validationIstr = getInputStream(filename, false, true);
            boolean annots_found = false;
            try {
                annots_found = processAnnotsXml(istr, validationIstr, annotList);
            } catch (SAXParseException x) {
                String errorMessage = "QuickLoad Server {0} has an invalid annotations (annots.xml) file for {1}: {2}. Please contact the server administrators or the IGB development team to let us know about the problem.";
                String errorText = MessageFormat.format(errorMessage, root_url, genome_name, x.getMessage());
                String title = "Invalid annots.xml file";
                ErrorHandler.errorPanelWithReportBug(title, errorText, Level.SEVERE);
                return false;
            } catch (JDOMException x) {
                String errorMessage = "QuickLoad Server {0} has an invalid annotations (annots.xml) file for {1}. Please contact the server administrators or the IGB development team to let us know about the problem.";
                String errorText = MessageFormat.format(errorMessage, root_url, genome_name);
                String title = "Invalid annots.xml file";
                ErrorHandler.errorPanelWithReportBug(title, errorText, Level.SEVERE);
                return false;
            }

            if (annots_found) {
                logger.debug("Found {} files in {} on server {}.", annotList.size(), genome_name, root_url);
                return true;
            }

            logger.debug("Couldn''t found annots.xml for {}. Looking for annots.txt now.", genome_name);
            filename = getPath(genome_name, Constants.ANNOTS_TXT);
            istr = getInputStream(filename, getCacheAnnots(), false);

            annots_found = processAnnotsTxt(istr, annotList);

            if (!annots_found) {
                ErrorHandler.errorPanelWithReportBug("Missing Required File", MessageFormat.format("QuickLoad Server {0} does not contain required annots.xml/annots.txt metadata "
                        + "file for requested genome version {1}. "
                        + "IGB may not be able to display this genome.", root_url, genome_name), Level.SEVERE);
            } else {
                logger.debug("Found {} files in {} on server {}.", annotList.size(), genome_name, root_url);
            }

            return annots_found;

        } catch (Exception ex) {
            logger.error("Couldn''t found either annots.xml or annots.txt for {}", genome_name, ex);
            return false;
        } finally {
            GeneralUtils.safeClose(istr);
        }

    }

    /**
     * Process the annots.xml file (if it exists). This has friendly type names.
     */
    private static boolean processAnnotsXml(InputStream istr, InputStream validationIstr, List<AnnotMapElt> annotList) throws JDOMException, IOException, SAXException {
        if (istr == null) {
            // Search failed.  That's fine, since there's a backup test for annots.txt.
            return false;
        }

        AnnotsXmlParser.parseAnnotsXml(istr, validationIstr, annotList);
        return true;
    }

    /**
     * Process the annots.txt file (if it exists).
     */
    private static boolean processAnnotsTxt(InputStream istr, List<AnnotMapElt> annotList) {
        BufferedReader br = null;
        try {
            if (istr == null) {
                // Search failed.  getInputStream has already logged warnings about this.
                return false;
            }
            br = new BufferedReader(new InputStreamReader(istr));
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = tab_regex.split(line);
                if (fields.length >= 1) {
                    String annot_file_name = fields[0];
                    if (annot_file_name == null || annot_file_name.length() == 0) {
                        continue;
                    }
                    String friendlyName = LoadUtils.stripFilenameExtensions(annot_file_name);
                    AnnotMapElt annotMapElt = new AnnotMapElt(annot_file_name, friendlyName);
                    annotList.add(annotMapElt);
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            GeneralUtils.safeClose(br);
        }
    }

    private boolean loadSeqInfo(String genome_name) {
        String liftAll = Constants.LIFT_ALL_LFT;
        String modChromInfo = Constants.MOD_CHROM_INFO_TXT;
        String genomeTxt = Constants.GENOME_TXT;
        String chromosomeTxt = Constants.CHROMOSOMES_TXT;

        genome_name = LOOKUP.findMatchingSynonym(genome_names, genome_name);
        boolean success = false;
        InputStream lift_stream = null;
        InputStream cinfo_stream = null;
        InputStream ginfo_stream = null;
        InputStream chrom_stream = null;
        try {
            String lift_path = getPath(genome_name, liftAll);
            try {
                // don't warn about this file, since we'll look for modChromInfo file
                lift_stream = getInputStream(lift_path, getCacheAnnots(), true);
            } catch (Exception ex) {
                // exception can be ignored, since we'll look for modChromInfo file.
                logger.debug("couldn''t find {}, looking instead for {}", new Object[]{liftAll, modChromInfo});
                lift_stream = null;
            }

            String ginfo_path = getPath(genome_name, genomeTxt);
            if (lift_stream == null) {
                try {
                    ginfo_stream = getInputStream(ginfo_path, getCacheAnnots(), false);
                } catch (Exception ex) {
                    logger.error("couldn''t find {}, looking instead for {}", new Object[]{liftAll, genomeTxt});
                    ginfo_stream = null;
                }
            }

            if (ginfo_stream == null) {
                String cinfo_path = getPath(genome_name, modChromInfo);
                try {
                    cinfo_stream = getInputStream(cinfo_path, getCacheAnnots(), false);
                } catch (Exception ex) {
                    logger.error("ERROR: could find {} or {} or {}", lift_path, ginfo_path, cinfo_path, ex);
                    cinfo_stream = null;
                }
            }

            boolean annot_contigs = false;
            AnnotatedSeqGroup group = GenometryModel.getInstance().addSeqGroup(genome_name);

            try {
                String chromosome_path = getPath(genome_name, chromosomeTxt);
                chrom_stream = getInputStream(chromosome_path, true, true);
                if (chrom_stream != null) {
                    group.loadSynonyms(chrom_stream);
                }
            } catch (Exception ex) {
                chrom_stream = null;
            }

            if (lift_stream != null) {
                success = LiftParser.parse(lift_stream, group, annot_contigs);
            } else if (ginfo_stream != null) {
                success = ChromInfoParser.parse(ginfo_stream, group, getLoadURL());
            } else if (cinfo_stream != null) {
                success = ChromInfoParser.parse(cinfo_stream, group, getLoadURL());
            }
        } catch (Exception ex) {
            ErrorHandler.errorPanel("Error loading data for genome '" + genome_name + "'", ex, Level.SEVERE);
        } finally {
            GeneralUtils.safeClose(lift_stream);
            GeneralUtils.safeClose(ginfo_stream);
            GeneralUtils.safeClose(cinfo_stream);
            GeneralUtils.safeClose(chrom_stream);
        }
//		if(!success){
//			ErrorHandler.errorPanel("Missing Required File", MessageFormat.format("QuickLoad Server {0} does not contain required sequence metadata "
//					+ "file for requested genome version {1}. "
//					+ "IGB may not be able to display this genome.",new Object[]{root_url,genome_name}));
//		}
        return success;
    }

    void loadGenomeNames() {
        URL contentsTextUrl = null;
        try {
            contentsTextUrl = new URL(getLoadURL() + Constants.CONTENTS_TXT);
            String contents = Resources.toString(contentsTextUrl, Charsets.UTF_8);
            LocalUrlCacher.writeToCache(contentsTextUrl, contents);
            processContentsTextFile(contents);
        } catch (MalformedURLException ex) {
            logger.error("Invalid url", ex);
        } catch (IOException ex) {
            logger.warn("Unable to obtaint {} from the server", contentsTextUrl.toString());
            Optional<String> cachedContent = LocalUrlCacher.retrieveFileAsStringFromCache(contentsTextUrl);
            if (cachedContent.isPresent()) {
                logger.info("Loading {} from cache", contentsTextUrl);
                processContentsTextFile(cachedContent.get());
            }
        }
    }

    private boolean isCommentLine(String line) {
        return line.startsWith("#") || (line.startsWith("<") && line.endsWith(">"));
    }

    private void processContentsTextFile(String contents) {
        List<String> lines = Splitter.on("\n").omitEmptyStrings().trimResults().splitToList(contents);
        for (String line : lines) {
            if (Strings.isNullOrEmpty(line) || isCommentLine(line)) {
                continue;
            }
            List<String> fields = Splitter.on("\t").omitEmptyStrings().trimResults().splitToList(line);
            if (fields.size() >= 1) {
                String genomeName = fields.get(0);
                AnnotatedSeqGroup group = this.getSeqGroup(genomeName);  // returns existing group if found, otherwise creates a new group
                genome_names.add(genomeName);
                // if quickload server has description, and group is new or doesn't yet have description, add description to group
                if ((fields.size() >= 2) && (group.getDescription() == null)) {
                    group.setDescription(fields.get(1));
                }
            }

        }
    }

    /**
     * Translate a file in a genome to a relative path under this QuickLoad
     * directory. If file is null or an empty string, this will return the path
     * to the version directory.
     *
     * @param genome_name name of this genome version
     * @param file the file in question. Can be null
     * @return path to the file or version directory if file is null
     */
    public String getPath(String genome_name, String file) {
        StringBuilder builder = new StringBuilder();

        builder.append(LOOKUP.findMatchingSynonym(genome_names, genome_name));
        builder.append("/");

        if (file != null && !file.isEmpty()) {
            builder.append(file);
        }

        return builder.toString();
    }

    private InputStream getInputStream(String append_url, boolean write_to_cache, boolean fileMayNotExist) throws IOException {
        return getInputStream(append_url, write_to_cache, fileMayNotExist, true);
    }

    private InputStream getInputStream(String append_url, boolean write_to_cache, boolean fileMayNotExist, boolean allowHtml) throws IOException {
        String load_url = getLoadURL() + append_url;
        if (logger.isDebugEnabled()) {
            if (append_url.equals(Constants.CONTENTS_TXT)) {
                try {
                    logger.debug("Contents.txt from server");
                    logger.debug(Resources.toString(new URL(load_url), Charsets.UTF_8));
                    logger.debug("Contents.txt from LocalUrlCacher");
                    String content = CharStreams.toString(new InputStreamReader(LocalUrlCacher.getInputStream(load_url, write_to_cache, null, fileMayNotExist, allowHtml), Charsets.UTF_8));
                    logger.debug(content);
                } catch (FileNotFoundException ex) {
                    //do nothing
                }
            }
        }
        InputStream istr = LocalUrlCacher.getInputStream(load_url, write_to_cache, null, fileMayNotExist, allowHtml);

        /**
         * Check to see if trying to load from primary server but primary server
         * is not responding *
         */
        if (istr == null && isLoadingFromPrimary() && !fileMayNotExist) {

            logger.warn("Primary Server :{} is not responding. So disabling it for this session.", primaryServer.serverName);
            primaryServer.setServerStatus(ServerStatus.NotResponding);

            load_url = getLoadURL() + append_url;
            istr = LocalUrlCacher.getInputStream(load_url, write_to_cache, null, fileMayNotExist, allowHtml);
        }

        logger.debug("Load URL: {}", load_url);
        return istr;
    }

    private boolean isLoadingFromPrimary() {
        return (primary_url != null && primaryServer != null && !primaryServer.getServerStatus().equals(ServerStatus.NotResponding));
    }

    /**
     * Returns a stream of the file species.txt at the quickload server or null
     * if the file is not present.
     *
     * @return stream of the text file or null if file not present.
     */
    public InputStream getSpeciesTxt() {
        InputStream stream = null;
        try {
            stream = getInputStream(Constants.SPECIES_TXT, false, true);
        } catch (IOException ex) {
            logger.error(null, ex);
        }
        return stream;
    }

    /**
     * Returns true if the quickload server has a species.txt file.
     *
     *
     * @return true if the quickload server has a species.txt file
     */
    public boolean hasSpeciesTxt() {
        InputStream stream;
        try {
            stream = getInputStream(Constants.SPECIES_TXT, false, true);
        } catch (IOException ex) {
            return false;
        }
        return stream != null;
    }

    private String getLoadURL() {
        if (!isLoadingFromPrimary()) {
            return root_url;
        }

        return primary_url;
    }

    private void clean() {
        for (String genome : getGenomeNames()) {
            AnnotatedSeqGroup group = GenometryModel.getInstance().getSeqGroup(genome);
            if (group != null) {
                group.removeSeqsForUri(root_url);
            }
        }
    }

    @Override
    public String toString() {
        return "QuickLoadServerModel: url='" + getRootUrl() + "'";
    }
}
