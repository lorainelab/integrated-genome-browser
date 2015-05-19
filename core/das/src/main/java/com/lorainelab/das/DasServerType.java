package com.lorainelab.das;

import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.util.Constants;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.SynonymLookup;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DasServerType {

    /**
     * boolean to indicate should script continue to run if error occurs *
     */
    private static final boolean DEBUG = true;
    private static final boolean exitOnError = false;
    private static final String dsn = "dsn.xml";
    private static final String name = "DAS";
    public static final int ordinal = 30;
    private static final GenometryModel gmodel = GenometryModel.getInstance();
    /**
     * Private copy of the default Synonym lookup
     *
     * @see SynonymLookup#getDefaultLookup()
     */
    private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
    private static final DasServerType instance = new DasServerType();

    public static DasServerType getInstance() {
        return instance;
    }

    protected DasServerType() {
        super();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Gets server path for a mapping on DAS server.
     *
     * @param id	Genome id
     * @param file	File name.
     */
    private String getPath(String id, URL server, String file) {
        try {
            URL server_path = new URL(server, id + "/" + file);
            return server_path.toExternalForm();
        } catch (MalformedURLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Gets files for a genome and copies it to it's directory.
     *
     * @param local_path	Local path from where mapping is to saved.
     */
    @SuppressWarnings("unused")
    private boolean getAllDasFiles(String id, URL server, URL master, String local_path) {
        local_path += "/" + id;
        GeneralUtils.makeDir(local_path);

        File file;
        final Map<String, String> DasFilePath = new HashMap<>();

        String entry_point = getPath(master.getPath(), master, DasSource.ENTRY_POINTS);

        String types = getPath(id, server, DasSource.TYPES);

        DasFilePath.put(entry_point, DasSource.ENTRY_POINTS + Constants.XML_EXTENSION);
        DasFilePath.put(types, DasSource.TYPES + Constants.XML_EXTENSION);

        for (Entry<String, String> fileDet : DasFilePath.entrySet()) {
            file = GeneralUtils.getFile(fileDet.getKey(), false);

            if ((file == null || !GeneralUtils.moveFileTo(file, fileDet.getValue(), local_path)) && exitOnError) {
                return false;
            }

        }

        return true;
    }

//    public boolean processServer(DataProvider gServer, String path) {
//        File file = GeneralUtils.getFile(gServer.getUrl(), false);
//        if (!GeneralUtils.moveFileTo(file, dsn, path)) {
//            return false;
//        }
//
//        DasServerInfo server = (DasServerInfo) gServer.serverObj;
//        Map<String, DasSource> sources = server.getDataSources();
//
//        if (sources == null || sources.values() == null || sources.values().isEmpty()) {
//            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Couldn't find species for server: ", gServer);
//            return false;
//        }
//
//        for (DasSource source : sources.values()) {
//
//            if (!getAllDasFiles(source.getId(), source.getServerURL(), source.getMasterURL(), path)) {
//                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Could not find all files for {0} !!!", gServer.serverName);
//                return false;
//            }
//
//            for (String src : source.getSources()) {
//                if (!getAllDasFiles(src, source.getServerURL(), source.getMasterURL(), path)) {
//                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Could not find all files for {0} !!!", gServer.serverName);
//                    return false;
//                }
//            }
//
//        }
//
//        return true;
//    }
    public String formatURL(String url) {
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

//    public Object getServerInfo(String url, String name) {
//        return new DasServerInfo(url);
//    }
    public String adjustURL(String url) {
        String tempURL = url;
        if (tempURL.endsWith("/dsn")) {
            tempURL = tempURL.substring(0, tempURL.length() - 4);
        }
        return tempURL;
    }

    public boolean loadStrategyVisibleOnly() {
        return true;
    }

//    public void discoverFeatures(DataContainer gVersion, boolean autoload) {
//        DasSource version = (DasSource) gVersion.versionSourceObj;
//        List<Entry<String, String>> types = new ArrayList<>(version.getTypes().entrySet());
//        for (Entry<String, String> type : types) {
//            String type_name = type.getKey();
//            if (type_name == null || type_name.length() == 0) {
//                System.out.println("WARNING: Found empty feature name in " + gVersion.versionName + ", " + gVersion.gServer.serverName);
//                continue;
//            }
//            gVersion.addDataSet(new DataSet(type_name, null, gVersion, null, type.getValue(), autoload));
//        }
//    }
    public void discoverChromosomes(Object versionSourceObj) {
        // Discover chromosomes from DAS
        DasSource version = (DasSource) versionSourceObj;

        version.getGenome();
        version.getEntryPoints();
    }

    public boolean hasFriendlyURL() {
        return false;
    }

    public boolean canHandleFeature() {
        return true;
    }

    /**
     * Discover species from DAS
     *
     * @param gServer
     * @return false if there's an obvious problem
     */
//    public boolean getSpeciesAndVersions(DataProvider gServer, DataProvider primaryServer, URL primaryURL, VersionDiscoverer versionDiscoverer) {
//        DasServerInfo server = (DasServerInfo) gServer.serverObj;
//        if (primaryURL == null) {
//            try {
//                primaryURL = new URL(gServer.URL);
//                primaryServer = null;
//            } catch (MalformedURLException x) {
//                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot load URL " + gServer.URL + " for DAS server " + gServer.serverName, x);
//            }
//        }
//        Map<String, DasSource> sources = server.getDataSources(primaryURL, primaryServer);
//        if (sources == null || sources.values() == null || sources.values().isEmpty()) {
//            System.out.println("WARNING: Couldn't find species for server: " + gServer);
//            return false;
//        }
//        for (DasSource source : sources.values()) {
//            String speciesName = SpeciesLookup.getSpeciesName(source.getId());
//            String versionName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), source.getId());
//            String versionID = source.getId();
//            versionDiscoverer.discoverVersion(versionID, versionName, gServer, source, speciesName);
//        }
//        return true;
//    }
//    protected String getSegment(SeqSpan span, GenericFeature feature) {
//        BioSeq current_seq = span.getBioSeq();
//        Set<String> segments = ((DasSource) feature.gVersion.versionSourceObj).getEntryPoints();
//        return SynonymLookup.getDefaultLookup().findMatchingSynonym(segments, current_seq.getID());
//    }
    /**
     * Load annotations from a DAS server.
     *
     * @param feature the generic feature that is to be loaded from the server.
     * @param span containing the ranges for which you want annotations.
     */
//    public Map<String, List<? extends SeqSymmetry>> loadFeatures(SeqSpan span, GenericFeature feature) {
//        try {
//            feature.addLoadingSpanRequest(span);	// this span is requested to be loaded.
//
//            String segment = getSegment(span, feature);
//
//            QueryBuilder builder = new QueryBuilder(feature.typeObj.toString());
//            builder.add("segment", segment);
//            builder.add("segment", segment + ":" + (span.getMin() + 1) + "," + span.getMax());
//
//            ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(feature.typeObj.toString(), feature.featureName, "das1", feature.featureProps);
//            style.setFeature(feature);
//
//            // TODO - probably not necessary
//            //style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(feature.featureName, feature.featureName, "das1");
//            //style.setFeature(feature);
//            URI uri = builder.build();
//            if (DEBUG) {
//                System.out.println("Loading DAS feature " + feature.featureName + " with uri " + uri);
//            }
//            List<DASSymmetry> dassyms = parseData(uri);
//
//            return SymLoader.splitFilterAndAddAnnotation(span, dassyms, feature);
//        } finally {
//            if (Thread.currentThread().isInterrupted()) {
//                feature.removeCurrentRequest(span);
//            } else {
//                feature.addLoadedSpanRequest(span);
//            }
//        }
//    }
//
//    /**
//     * Opens a binary data stream from the given uri and adds the resulting
//     * data.
//     */
//    private List<DASSymmetry> parseData(URI uri) {
//        Map<String, List<String>> respHeaders = new HashMap<>();
//        InputStream stream = null;
//        List<String> list;
//        String content_type = "content/unknown";
//        int content_length = -1;
//
//        try {
//            stream = LocalUrlCacher.getInputStream(uri.toURL(), true, null, respHeaders);
//            list = respHeaders.get("Content-Type");
//            if (list != null && !list.isEmpty()) {
//                content_type = list.get(0);
//            }
//
//            list = respHeaders.get("Content-Length");
//            if (list != null && !list.isEmpty()) {
//                try {
//                    content_length = Integer.parseInt(list.get(0));
//                } catch (NumberFormatException ex) {
//                    content_length = -1;
//                }
//            }
//
//            if (content_length == 0) { // Note: length == -1 means "length unknown"
//                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "{0} returned no data.", uri);
//                return null;
//            }
//
//            if (content_type.startsWith("text/plain")
//                    || content_type.startsWith("text/html")
//                    || content_type.startsWith("text/xml")) {
//                // Note that some http servers will return "text/html" even when that is untrue.
//                // we could try testing whether the filename extension is a recognized extension, like ".psl"
//                // and if so passing to LoadFileAction.load(.. feat_request_con.getInputStream() ..)
////                AnnotatedSeqGroup group = GenometryModel.getInstance().getSelectedSeqGroup();
//                DASFeatureParser das_parser = new DASFeatureParser();
//                das_parser.setAnnotateSeq(false);
//
//                BufferedInputStream bis = null;
//                try {
//                    bis = new BufferedInputStream(stream);
//                    return das_parser.parse(bis, group);
//                } catch (XMLStreamException ex) {
//                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Unable to parse DAS response", ex);
//                } finally {
//                    GeneralUtils.safeClose(bis);
//                }
//            } else {
//                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Declared data type {0} cannot be processed", content_type);
//            }
//        } catch (Exception ex) {
//            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception encountered: no data returned for url " + uri, ex);
//        } finally {
//            GeneralUtils.safeClose(stream);
//        }
//
//        return null;
//    }
//
//    public boolean isAuthOptional() {
//        return false;
//    }
//
//    public boolean getResidues(GenericVersion version, String genomeVersionName,
//            BioSeq aseq, int min, int max, SeqSpan span) {
//        String seq_name = aseq.getID();
//        DasResiduesHandler dasResiduesHandler = new DasResiduesHandler();
//        String residues = dasResiduesHandler.getDasResidues(version, seq_name, min, max);
////			String residues = DasLoader.getDasResidues(version, seq_name, min, max);
//        if (residues != null) {
//            BioSeqUtils.addResiduesToComposition(aseq, residues, span);
//            return true;
//        }
//
//        return false;
//    }
//
//    public void removeServer(GenericServer server) {
//        // Do Nothing for now
//    }
    public boolean isSaveServersInPrefs() {
        return true;
    }

    // No friendly URL for DAS
//    public String getFriendlyURL(GenericServer gServer) {
//        return null;
//    }
//
//    // No mirror site for DAS
//    public boolean useMirrorSite(GenericServer gServer) {
//        return false;
//    }
}
