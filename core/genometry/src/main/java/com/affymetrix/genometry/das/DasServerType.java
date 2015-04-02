package com.affymetrix.genometry.das;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.parsers.das.DASFeatureParser;
import com.affymetrix.genometry.parsers.das.DASSymmetry;
import com.affymetrix.genometry.style.DefaultStateProvider;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.BioSeqUtils;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.QueryBuilder;
import com.affymetrix.genometry.util.ServerTypeI;
import com.affymetrix.genometry.util.SpeciesLookup;
import com.affymetrix.genometry.util.SynonymLookup;
import com.affymetrix.genometry.util.VersionDiscoverer;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.slf4j.LoggerFactory;

public class DasServerType implements ServerTypeI {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DasServerType.class);
    private static final String NAME = "DAS";
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

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toString() {
        return NAME;
    }

    @Override
    public String formatURL(String url) {
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    @Override
    public String adjustURL(String url) {
        String tempURL = url;
        if (tempURL.endsWith("/dsn")) {
            tempURL = tempURL.substring(0, tempURL.length() - 4);
        }
        return tempURL;
    }

    @Override
    public boolean loadStrategyVisibleOnly() {
        return true;
    }

    @Override
    public void discoverFeatures(GenericVersion gVersion, boolean autoload) {
        DasSource version = (DasSource) gVersion.getVersionSourceObj();
        List<Entry<String, String>> types = new ArrayList<>(version.getTypes().entrySet());
        for (Entry<String, String> type : types) {
            String type_name = type.getKey();
            if (type_name == null || type_name.length() == 0) {
                System.out.println("WARNING: Found empty feature name in " + gVersion.getVersionName() + ", " + gVersion.getgServer().getServerName());
                continue;
            }
            gVersion.addFeature(new GenericFeature(type_name, null, gVersion, null, type.getValue(), autoload));
        }
    }

    @Override
    public void discoverChromosomes(Object versionSourceObj) {
        // Discover chromosomes from DAS
        DasSource version = (DasSource) versionSourceObj;

        version.getGenome();
        version.getEntryPoints();
    }

    @Override
    public boolean hasFriendlyURL() {
        return false;
    }

    @Override
    public boolean canHandleFeature() {
        return true;
    }

    /**
     * Discover species from DAS
     *
     * @param gServer
     * @return false if there's an obvious problem
     */
    @Override
    public boolean getSpeciesAndVersions(GenericServer gServer, VersionDiscoverer versionDiscoverer) {
        DasServerInfo server = new DasServerInfo(gServer.getUrlString());

        Map<String, DasSource> sources = server.getDataSources(gServer);
        if (sources == null || sources.values() == null || sources.values().isEmpty()) {
            logger.info("WARNING: Couldn't find species for server: " + gServer);
            return false;
        }
        for (DasSource source : sources.values()) {
            String speciesName = SpeciesLookup.getSpeciesName(source.getID());
            String versionName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), source.getID());
            String versionID = source.getID();
            versionDiscoverer.discoverVersion(versionID, versionName, gServer, source, speciesName);
        }
        return true;
    }

    protected String getSegment(SeqSpan span, GenericFeature feature) {
        BioSeq current_seq = span.getBioSeq();
        Set<String> segments = ((DasSource) feature.getgVersion().getVersionSourceObj()).getEntryPoints();
        return SynonymLookup.getDefaultLookup().findMatchingSynonym(segments, current_seq.getID());
    }

    /**
     * Load annotations from a DAS server.
     *
     * @param feature the generic feature that is to be loaded from the server.
     * @param span containing the ranges for which you want annotations.
     */
    @Override
    public Map<String, List<? extends SeqSymmetry>> loadFeatures(SeqSpan span, GenericFeature feature) {
        try {
            feature.addLoadingSpanRequest(span);	// this span is requested to be loaded.

            String segment = getSegment(span, feature);

            QueryBuilder builder = new QueryBuilder(feature.getTypeObj().toString());
            builder.add("segment", segment);
            builder.add("segment", segment + ":" + (span.getMin() + 1) + "," + span.getMax());

            ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(feature.getTypeObj().toString(), feature.getFeatureName(), "das1", feature.getFeatureProps());
            style.setFeature(feature);

            // TODO - probably not necessary
            //style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(feature.featureName, feature.featureName, "das1");
            //style.setFeature(feature);
            URI uri = builder.build();
            List<DASSymmetry> dassyms = parseData(uri);

            return SymLoader.splitFilterAndAddAnnotation(span, dassyms, feature);
        } finally {
            if (Thread.currentThread().isInterrupted()) {
                feature.removeCurrentRequest(span);
            } else {
                feature.addLoadedSpanRequest(span);
            }
        }
    }

    /**
     * Opens a binary data stream from the given uri and adds the resulting
     * data.
     */
    private List<DASSymmetry> parseData(URI uri) {
        Map<String, List<String>> respHeaders = new HashMap<>();
        InputStream stream = null;
        List<String> list;
        String content_type = "content/unknown";
        int content_length = -1;

        try {
            stream = LocalUrlCacher.getInputStream(uri.toURL(), true, null, respHeaders);
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
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "{0} returned no data.", uri);
                return null;
            }

            if (content_type.startsWith("text/plain")
                    || content_type.startsWith("text/html")
                    || content_type.startsWith("text/xml")) {
                // Note that some http servers will return "text/html" even when that is untrue.
                // we could try testing whether the filename extension is a recognized extension, like ".psl"
                // and if so passing to LoadFileAction.load(.. feat_request_con.getInputStream() ..)
                AnnotatedSeqGroup group = GenometryModel.getInstance().getSelectedSeqGroup();
                DASFeatureParser das_parser = new DASFeatureParser();
                das_parser.setAnnotateSeq(false);

                BufferedInputStream bis = null;
                try {
                    bis = new BufferedInputStream(stream);
                    return das_parser.parse(bis, group);
                } catch (XMLStreamException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Unable to parse DAS response", ex);
                } finally {
                    GeneralUtils.safeClose(bis);
                }
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Declared data type {0} cannot be processed", content_type);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception encountered: no data returned for url " + uri, ex);
        } finally {
            GeneralUtils.safeClose(stream);
        }

        return null;
    }

    @Override
    public boolean getResidues(GenericVersion version, String genomeVersionName,
            BioSeq aseq, int min, int max, SeqSpan span) {
        String seq_name = aseq.getID();
        DasResiduesHandler dasResiduesHandler = new DasResiduesHandler();
        String residues = dasResiduesHandler.getDasResidues(version, seq_name, min, max);
//			String residues = DasLoader.getDasResidues(version, seq_name, min, max);
        if (residues != null) {
            BioSeqUtils.addResiduesToComposition(aseq, residues, span);
            return true;
        }

        return false;
    }

    @Override
    public void removeServer(GenericServer server) {
        // Do Nothing for now
    }

    @Override
    public boolean isSaveServersInPrefs() {
        return true;
    }

}
