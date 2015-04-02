package com.affymetrix.genometry.quickload;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryConstants;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometry.symloader.BNIB;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symloader.TwoBitNew;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.BioSeqUtils;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.ServerTypeI;
import com.affymetrix.genometry.util.SpeciesLookup;
import com.affymetrix.genometry.util.SynonymLookup;
import com.affymetrix.genometry.util.VersionDiscoverer;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

public class QuickloadServerType implements ServerTypeI {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(QuickloadServerType.class);

    enum QFORMAT {

        BNIB,
        VTWOBIT,
        TWOBIT,
        FA
    }

    private static final String name = "Quickload";
    private static final GenometryModel gmodel = GenometryModel.getInstance();
    private static final List<QuickLoadSymLoaderHook> quickLoadSymLoaderHooks = new ArrayList<>();
    /**
     * Private copy of the default Synonym lookup
     *
     * @see SynonymLookup#getDefaultLookup()
     */
    private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();

    private static final QuickloadServerType instance = new QuickloadServerType();

    public static QuickloadServerType getInstance() {
        return instance;
    }

    private QuickloadServerType() {
        super();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String formatURL(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    @Override
    public String adjustURL(String url) {
        return url;
    }

    @Override
    public boolean loadStrategyVisibleOnly() {
        return false;
    }

    public static void addQuickLoadSymLoaderHook(QuickLoadSymLoaderHook quickLoadSymLoaderHook) {
        quickLoadSymLoaderHooks.add(quickLoadSymLoaderHook);
    }

    private QuickLoadSymLoader getQuickLoad(GenericVersion version, String featureName) {
        URI uri = determineURI(version, featureName);
        QuickLoadSymLoader quickLoadSymLoader = new QuickLoadSymLoader(uri, featureName, version.getGroup());
        for (QuickLoadSymLoaderHook quickLoadSymLoaderHook : quickLoadSymLoaderHooks) {
            quickLoadSymLoader = quickLoadSymLoaderHook.processQuickLoadSymLoader(quickLoadSymLoader);
        }
        return quickLoadSymLoader;
    }

    private static URI determineURI(GenericVersion version, String featureName) {
        URI uri = null;

        if (version.getgServer().getUrlString() == null || version.getgServer().getUrlString().length() == 0) {
            int httpIndex = featureName.toLowerCase().indexOf(HTTP_PROTOCOL);
            if (httpIndex > -1) {
                // Strip off initial characters up to and including http:
                // Sometimes this is necessary, as URLs can start with invalid "http:/"
                featureName = GeneralUtils.convertStreamNameToValidURLName(featureName);
                uri = URI.create(featureName);
            } else {
                uri = (new File(featureName)).toURI();
            }
        } else {
            String fileName = determineFileName(version, featureName);
            int httpIndex = fileName.toLowerCase().indexOf(HTTP_PROTOCOL);
            int httpsIndex = fileName.toLowerCase().indexOf(HTTPS_PROTOCOL);
            int ftpIndex = fileName.toLowerCase().indexOf(FTP_PROTOCOL);
            if (httpIndex > -1 || httpsIndex > -1 || ftpIndex > -1) {
                uri = URI.create(fileName);
            } else {
                uri = URI.create(
                        version.getgServer().getUrlString() // Changed from 'version.gServer.URL' since quickload uses serverObj

                        + version.getVersionID() + "/"
                        + determineFileName(version, featureName));
            }
        }

        return uri;
    }

    private static String determineFileName(GenericVersion version, String featureName) {
        URL quickloadURL = version.getgServer().getURL();

        QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
        List<AnnotMapElt> annotsList = quickloadServer.getAnnotsMap(version.getVersionID());

        // Linear search, but over a very small list.
        for (AnnotMapElt annotMapElt : annotsList) {
            if (annotMapElt.title.equals(featureName)) {
                return annotMapElt.fileName;
            }
        }
        return "";
    }

    @Override
    public void discoverFeatures(GenericVersion gVersion, boolean autoload) {
        // Discover feature names from QuickLoad
        try {
            URL quickloadURL = gVersion.getgServer().getURL();
            if (logger.isDebugEnabled()) {
                logger.debug("Discovering Quickload features for " + gVersion.getVersionName() + ". URL:" + gVersion.getgServer().getUrlString());
            }

            QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
            List<String> typeNames = quickloadServer.getTypes(gVersion.getVersionName());
            if (typeNames == null) {
                String errorText = MessageFormat.format(GenometryConstants.BUNDLE.getString("quickloadGenomeError"), gVersion.getgServer().getServerName(), gVersion.getGroup().getOrganism(), gVersion.getVersionName());
                ErrorHandler.errorPanelWithReportBug(gVersion.getgServer().getServerName(), errorText, Level.SEVERE);
                return;
            }

            for (String type_name : typeNames) {
                if (type_name == null || type_name.length() == 0) {
                    logger.warn("WARNING: Found empty feature name in " + gVersion.getVersionName() + ", " + gVersion.getgServer().getServerName());
                    continue;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding feature " + type_name);
                }
                Map<String, String> type_props = quickloadServer.getProps(gVersion.getVersionName(), type_name);
                gVersion.addFeature(
                        new GenericFeature(
                                type_name, type_props, gVersion, getQuickLoad(gVersion, type_name), null, autoload));
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void discoverChromosomes(Object versionSourceObj) {
    }

    @Override
    public boolean hasFriendlyURL() {
        return true;
    }

    @Override
    public boolean canHandleFeature() {
        return false;
    }

    /**
     * Discover genomes from Quickload
     *
     * @return false if there's an obvious failure.
     */
    @Override
    public boolean getSpeciesAndVersions(GenericServer gServer, VersionDiscoverer versionDiscoverer) {
        URL quickloadURL = gServer.getURL();
        QuickLoadServerModel quickLoadServerModel = QuickLoadServerModel.getQLModelForURL(quickloadURL);

        if (quickLoadServerModel == null) {
            logger.error("ERROR: No quickload server model found for server: " + gServer);
            return false;
        }

        quickLoadServerModel.loadGenomeNames();
        List<String> genomeList = quickLoadServerModel.getGenomeNames();
        if (genomeList == null || genomeList.isEmpty()) {
            logger.warn("WARNING: No species found in server: " + gServer);
            return false;
        }

        //update species.txt with information from the server.
        if (quickLoadServerModel.hasSpeciesTxt()) {
            try {
                SpeciesLookup.load(quickLoadServerModel.getSpeciesTxt());
            } catch (IOException ex) {
                logger.warn("No species.txt found at this quickload server.", ex);
            }
        }
        for (String genomeID : genomeList) {
            String genomeName = LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), genomeID);
            String versionName, speciesName;
            // Retrieve group identity, since this has already been added in QuickLoadServerModel.
            Set<GenericVersion> gVersions = gmodel.addSeqGroup(genomeName).getEnabledVersions();
            if (!gVersions.isEmpty()) {
                // We've found a corresponding version object that was initialized earlier.
                versionName = GeneralUtils.getPreferredVersionName(gVersions);
//				speciesName = versionName2species.get(versionName);
                speciesName = versionDiscoverer.versionName2Species(versionName);
            } else {
                versionName = genomeName;
                speciesName = SpeciesLookup.getSpeciesName(genomeName);
            }
            versionDiscoverer.discoverVersion(genomeID, versionName, gServer, quickLoadServerModel, speciesName);
        }
        return true;
    }

    //Note  exception may be thrown on invalid SSL certificates.
    public static boolean ping(URL url, int timeout) {
        try {
            if (url.getProtocol().equals("file")) {
                File f;
                try {
                    f = new File(url.toURI());
                } catch (URISyntaxException e) {
                    f = new File(url.getPath());
                }
                return f.exists();
            } else {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                return (200 <= responseCode && responseCode <= 399);
            }
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public Map<String, List<? extends SeqSymmetry>> loadFeatures(SeqSpan span, GenericFeature feature) throws Exception {
        return (((QuickLoadSymLoader) feature.getSymL()).loadFeatures(span, feature));
    }

    // Generate URI (e.g., "http://www.bioviz.org/das2/genome/A_thaliana_TAIR8/chr1.bnib")
    private String generateQuickLoadURI(String common_url, String vPath, QFORMAT Format) {
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "trying to load residues via Quickload");
        switch (Format) {
            case BNIB:
                common_url += "bnib";
                break;

            case FA:
                common_url += "fa";
                break;

            case VTWOBIT:
                common_url = vPath;
                break;

            case TWOBIT:
                common_url += "2bit";
                break;

        }

        return common_url;
    }

    private QFORMAT determineFormat(String common_url, String vPath) {

        for (QFORMAT format : QFORMAT.values()) {
            String url_path = generateQuickLoadURI(common_url, vPath, format);
            if (LocalUrlCacher.isValidURL(url_path)) {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE,
                        "  Quickload location of " + format + " file: {0}", url_path);

                return format;
            }
        }

        return null;
    }

    private SymLoader determineLoader(String common_url, String vPath, AnnotatedSeqGroup seq_group, String seq_name) {
        QFORMAT format = determineFormat(common_url, vPath);

        if (format == null) {
            return null;
        }

        URI uri = null;
        try {
            uri = new URI(generateQuickLoadURI(common_url, vPath, format));
        } catch (URISyntaxException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }

        switch (format) {
            case BNIB:
                return new BNIB(uri, "", seq_group);

            case VTWOBIT:
                return new TwoBitNew(uri, "", seq_group);

            case TWOBIT:
                return new TwoBitNew(uri, "", seq_group);

//			case FA:
//				return new Fasta(uri, seq_group);
        }

        return null;
    }

    /**
     * Get the partial residues from the specified QuickLoad server.
     *
     * @return residue String.
     */
    private String getQuickLoadResidues(GenericServer server, GenericVersion version, AnnotatedSeqGroup seqGroup, String seqName, SeqSpan span, BioSeq aseq) {
        String common_url = "";
        String path = "";
        SymLoader symloader;
        try {
            URL quickloadURL = server.getURL();
            QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
            path = quickloadServer.getPath(version.getVersionName(), seqName);
            common_url = server.getUrlString() + path + ".";
            String vPath = server.getUrlString() + quickloadServer.getPath(version.getVersionName(), version.getVersionName()) + ".2bit";

            symloader = determineLoader(common_url, vPath, seqGroup, seqName);

            if (symloader != null) {
                return symloader.getRegionResidues(span);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public boolean getResidues(GenericVersion version, String genomeVersionName,
            BioSeq aseq, int min, int max, SeqSpan span) {
        String seq_name = aseq.getID();
        AnnotatedSeqGroup seq_group = aseq.getSeqGroup();
        String residues = getQuickLoadResidues(version.getgServer(), version, seq_group, seq_name, span, aseq);
        if (residues != null) {
            BioSeqUtils.addResiduesToComposition(aseq, residues, span);
            return true;
        }
        return false;
    }

    @Override
    public void removeServer(GenericServer server) {
        QuickLoadServerModel.removeQLModelForURL(server.getUrlString());
    }

    @Override
    public boolean isSaveServersInPrefs() {
        return true;
    }

    @Override
    public String getFriendlyURL(GenericServer gServer) {
        String tempURL = gServer.getUrlString();
        if (tempURL.endsWith("/")) {
            tempURL = tempURL.substring(0, tempURL.length() - 1);
        }
        if (gServer.getServerType() != null) {
            tempURL = gServer.getServerType().adjustURL(tempURL);
        }
        return tempURL;
    }

}
