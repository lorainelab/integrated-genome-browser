package com.gene.dasregistryservertype;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.das.DasServerType;
import com.affymetrix.genometry.das.DasSource;
import com.affymetrix.genometry.event.GroupSelectionEvent;
import com.affymetrix.genometry.event.GroupSelectionListener;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.ServerTypeI;
import com.affymetrix.genometry.util.SynonymLookup;
import com.affymetrix.genometry.util.VersionDiscoverer;
import com.affymetrix.genometry.util.XMLUtils;
import com.lorainelab.igb.services.IgbService;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This uses the DAS Registry to find features for the selected species (and
 * version if possible) note - the various DAS servers can handle the queries
 * different. There are different versions of the DAS spec, and they are
 * interpreted differently, and not all servers follow the spec, and some are
 * out of date or have bugs, etc. So the results of the query are only sometimes
 * useful.
 */
public class DASRegistryServerType extends DasServerType implements ServerTypeI, GroupSelectionListener {

    private static final boolean DEBUG = true;
    private static final String FEATURES_SUFFIX = "/features";
    private static final String DAS_SUFFIX = "/das";
    private static final String DAS1_SUFFIX = "/das1";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("species");
//	private static final String URL = "http://www.dasregistry.org";
    private static final String FEATURES_URL = "http://www.dasregistry.org/das/sources?capability=features&organism={0}";
    private static final String FEATURES_VERSION_URL = "http://www.dasregistry.org/das/sources?capability=features&organism={0}&version={1}";
    private static final String name = "DAS Registry";
    private static final int ordinal = Integer.MAX_VALUE;
    // key is URL, value is gServer
    private final Map<String, GenericServer> serverMap = new HashMap<>();
    // first key is server URL, second key is featureURL, third key is property
    private final Map<String, Map<String, Map<String, Object>>> featuresMap = new HashMap<>();
    private AnnotatedSeqGroup currentGroup;
    private final IgbService igbService;

    public DASRegistryServerType(IgbService igbService) {
        super();
        this.igbService = igbService;
        GenometryModel.getInstance().addGroupSelectionListener(this);
    }

    @Override
    public int compareTo(ServerTypeI o) {
        return ordinal - o.getOrdinal();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String formatURL(String url) {
        return url;
    }

    @Override
    public String adjustURL(String url) {
        return url;
    }

    @Override
    public boolean loadStrategyVisibleOnly() {
        return false;
    }

    @Override
    public void discoverFeatures(GenericVersion gVersion, boolean autoload) {
        Map<String, Map<String, Object>> featureList = featuresMap.get(gVersion.getgServer().getUrlString());
        if (featureList != null) {
            for (String url : featureList.keySet()) {
                Map<String, Object> featureProps = featureList.get(url);
                System.out.flush();
                int pos = url.lastIndexOf('/');
                String featureName = (String) featureProps.get("title");
                if (featureName == null) {
                    featureName = url.substring(pos + 1);
                }
                gVersion.addFeature(new GenericFeature(featureName, null, gVersion, null, featureProps.get("feature_uri") + "?" + (((String) featureProps.get("type")).length() == 0 ? "" : "&type=" + featureProps.get("type")), autoload));
            }
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

    @Override
    public boolean getSpeciesAndVersions(GenericServer gServer, VersionDiscoverer versionDiscoverer) {
        if (currentGroup == null) {
            return false;
        }
        String versionID = currentGroup.getID();
        String versionName = currentGroup.getID();
        String speciesName = currentGroup.getOrganism();
        try {
            URL url = new URL(gServer.getUrlString());
            versionDiscoverer.discoverVersion(versionID, versionName, gServer, url, speciesName);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean getResidues(GenericVersion versions, String genomeVersionName,
            BioSeq aseq, int min, int max, SeqSpan span) {
        return false;
    }

    @Override
    public boolean processServer(GenericServer gServer, String path) {
        return false;
    }

    @Override
    public boolean isSaveServersInPrefs() {
        return false;
    }

    private String getServerURL(String url) {
        try {
            URL testURL = new URL(url);
            return testURL.getProtocol() + "://" + testURL.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addFeature(final String feature_uri, final String entry_points_uri, final String stylesheet_query_uri, final String title, final List<String> types) {
        String serverURL = getServerURL(feature_uri);
        if (feature_uri.endsWith(FEATURES_SUFFIX) && serverURL != null) {
            String url = feature_uri.substring(0, feature_uri.length() - FEATURES_SUFFIX.length());
            int pos = url.lastIndexOf('/');
            String featureName = url.substring(pos + 1);
            String dasURL = url.substring(0, url.length() - (featureName.length() + 1));
            if (dasURL.endsWith(DAS_SUFFIX) || dasURL.endsWith(DAS1_SUFFIX)) {
                Map<String, Map<String, Object>> featureMap = featuresMap.get(serverURL);
                if (featureMap == null) {
                    featureMap = new HashMap<>();
                    featuresMap.put(serverURL, featureMap);
                } else if (featureMap.keySet().contains(url)) {
                    return;
                }
                List<String> typesCopy = types;
                if (typesCopy == null) {
                    typesCopy = new ArrayList<>();
                    typesCopy.add("");
                }
                for (String type : typesCopy) {
                    Map<String, Object> featureProps = new HashMap<>();
                    featureProps.put("url", url);
                    featureProps.put("feature_uri", feature_uri);
                    if (entry_points_uri != null) {
                        featureProps.put("entry_points_uri", entry_points_uri);
                    }
                    if (stylesheet_query_uri != null) {
                        featureProps.put("stylesheet_query_uri", stylesheet_query_uri);
                    }
                    if (title != null) {
                        featureProps.put("title", title);
                    }
                    if (type != null) {
                        featureProps.put("type", type);
                    }
                    featureMap.put(url, featureProps);
                }
                return;
            }
        }
        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.WARNING, "invalid uri {0}", feature_uri);
    }

    private void addServer(final String serverURL) {
        CThreadWorker<Void, Void> worker = new CThreadWorker<Void, Void>("add DAS Registry feature " + serverURL) {
            @Override
            protected Void runInBackground() {
                GenericServer gServer = igbService.addServer(DASRegistryServerType.this, serverURL, serverURL, Integer.MAX_VALUE);
                synchronized (serverMap) {
                    serverMap.put(serverURL, gServer);
                }
                return null;
            }

            @Override
            protected void finished() {
            }
        };
        CThreadHolder.getInstance().execute(serverURL, worker);
    }

    @Override
    public void groupSelectionChanged(GroupSelectionEvent evt) {
        setGroup(evt.getSelectedGroup());
    }

    /**
     * process one sources node in the document. This may create a feature for
     * the server.
     *
     * @param node the sources node
     * @param loadSub load the sources document found (not used)
     */
    private void processSourceNode(Node node, boolean loadSub) {
        String title = null;
        String feature_query_uri = null;
        String entry_points_query_uri = null;
        String sources_query_uri = null;
        String types_query_uri = null;
        String stylesheet_query_uri = null;
        if (node.hasChildNodes()) {
            if (node.hasAttributes() && node.getAttributes().getNamedItem("title") != null) {
                title = node.getAttributes().getNamedItem("title").getNodeValue();
            }
            NodeList ch1l = node.getChildNodes();
            for (int j = 0; j < ch1l.getLength(); j++) {
                Node ch1 = ch1l.item(j);
                if ("VERSION".equals(ch1.getNodeName()) && ch1.hasChildNodes()) {
                    NodeList ch2l = ch1.getChildNodes();
                    for (int k = 0; k < ch2l.getLength(); k++) {
                        Node ch2 = ch2l.item(k);
                        if ("CAPABILITY".equals(ch2.getNodeName())
                                && ch2.hasAttributes()
                                && ch2.getAttributes().getNamedItem("type") != null
                                && ch2.getAttributes().getNamedItem("query_uri") != null
                                && !ch2.getAttributes().getNamedItem("query_uri").getNodeValue().startsWith("http://das.cbs.dtu.dk")) {
                            String capability = ch2.getAttributes().getNamedItem("type").getNodeValue();
                            if ("das1:features".equals(capability)) {
                                feature_query_uri = ch2.getAttributes().getNamedItem("query_uri").getNodeValue();
                            }
                            if ("das1:entry_points".equals(capability)) {
                                entry_points_query_uri = ch2.getAttributes().getNamedItem("query_uri").getNodeValue();
                            }
                            if ("das1:sources".equals(capability)) {
                                sources_query_uri = ch2.getAttributes().getNamedItem("query_uri").getNodeValue();
                            }
                            if ("das1:types".equals(capability)) {
                                types_query_uri = ch2.getAttributes().getNamedItem("query_uri").getNodeValue();
                            }
                            if ("das1:stylesheet".equals(capability)) {
                                stylesheet_query_uri = ch2.getAttributes().getNamedItem("query_uri").getNodeValue();
                            }
                        }
                    }
                }
            }
        }
        List<String> types = null;
        if (feature_query_uri != null) {
            addFeature(feature_query_uri, entry_points_query_uri, stylesheet_query_uri, title, types);
        }
    }

    /**
     * process the sources document. Save any source elements with features
     * capab as a feature of the server
     *
     * @param url the sources URL
     * @param loadSub load the sources document found (not used)
     */
    private void loadSourcesURL(String url, boolean loadSub) {
        InputStream is = null;
        try {
            is = new URL(url).openConnection().getInputStream();
            Document dom = XMLUtils.getDocument(is);
            NodeList nl = dom.getElementsByTagName("SOURCE");
            for (int i = 0; i < nl.getLength(); i++) {
                processSourceNode(nl.item(i), loadSub);
            }
        } catch (Exception x) {
            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.SEVERE, "failed to load sources URL " + url, x);
        } finally {
            GeneralUtils.safeClose(is);
        }
    }

    /**
     * try to find the ncbi version using the synonyms, return null if not found
     *
     * @param group the AnnotationSeqGroup
     * @return the ncbi version
     */
    private String getNCBIVersion(AnnotatedSeqGroup group) {
        for (String synonym : SynonymLookup.getDefaultLookup().getSynonyms(group.getID())) {
            if (synonym.toLowerCase().contains("ncbi")) {
                int pos;
                for (pos = synonym.length() - 1; pos >= 0; pos--) {
                    if (synonym.charAt(pos) < '0' || synonym.charAt(pos) > '9') {
                        break;
                    }
                }
                if (pos < synonym.length() - 1) {
                    return (synonym.substring(pos + 1));
                }
            }
        }

        return null;
    }

    /**
     * search the DAS registry for the specified AnnotatedSeqGroup. - get the
     * NCBI taxonomy id from the Organism name (use the species.properties file)
     * - try to find the NCBI version from the synonyms (synonym will contain
     * "ncbi" and end with digits) - do a query with the species and optional
     * version to find all features - parse the results to save servers and
     * their features - at the end, add the servers
     *
     * @param group the group to research
     */
    public void setGroup(AnnotatedSeqGroup group) {
        Date start = new Date();
        String organism = "";
        try {
            for (GenericServer gServer : serverMap.values()) {
                igbService.removeServer(gServer);
            }
            serverMap.clear();
            featuresMap.clear();
            currentGroup = group;
            if (currentGroup == null) {
                if (DEBUG) {
                    System.out.println("!!! DAS Registry end groupSelectionChanged() - no group");
                }
                return;
            }
            organism = currentGroup.getOrganism();
            String ncbiCode = "0000";
            try {
                ncbiCode = BUNDLE.getString(organism.replaceAll(" ", "_"));
            } catch (MissingResourceException x) {
                ncbiCode = "0000";
            }
            if ("0000".equals(ncbiCode)) {
                if (DEBUG) {
                    System.out.println("!!! DAS Registry end groupSelectionChanged() - no ncbi code for " + organism);
                }
                return;
            }
            String ncbiVersion = getNCBIVersion(group);
            String featuresUrl;
            if (ncbiVersion == null) {
                featuresUrl = MessageFormat.format(FEATURES_URL, ncbiCode);
            } else {
                featuresUrl = MessageFormat.format(FEATURES_VERSION_URL, ncbiCode, ncbiVersion);
            }
            loadSourcesURL(featuresUrl, true);
        } catch (Exception x) {
            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.WARNING, "cannot load species " + organism, x);
        }
        int count = 0;
        for (String serverURL : featuresMap.keySet()) {
            addServer(serverURL);
            count++;
            if (count >= 5) {
                break;
            }
        }
        Date end = new Date();
    }

    @Override
    public Object getServerInfo(String url, String name) {
        return url;
    }

    private Map<String, Object> getFeatureProps(String uri) {
        String serverURL = getServerURL(uri);
        return featuresMap.get(serverURL).get(uri);
    }

    @Override
    protected String getSegment(SeqSpan span, GenericFeature feature) {
        String segment = null;
        BioSeq current_seq = span.getBioSeq();
        try {
            String uri = feature.getURI().toString();
            uri = uri.substring(0, uri.length() - (FEATURES_SUFFIX + "?").length());
            Map<String, Object> featureProps = getFeatureProps(uri);
            // if the server has entry_points try that to get the segment (AKA sequence AKA chromosome)
            if (featureProps != null && featureProps.get("entry_points_uri") != null) {
                DasSource source = new DasSource(null, new URL(uri));
                Set<String> segments = source.getEntryPoints();
                segment = SynonymLookup.getDefaultLookup().findMatchingSynonym(segments, null);
            }
        } catch (Exception x) {
        }
        if (segment == null) {
            // just try to guess the segment, this works often, but there needs to be a better way.
            segment = current_seq.getID();
            if (segment.startsWith("chr")) { // TODO fix this, but how?
                segment = segment.substring("chr".length());
            }
        }
        return segment;
    }

    @Override
    public Map<String, List<? extends SeqSymmetry>> loadFeatures(SeqSpan span, GenericFeature feature) {
        try {
            return super.loadFeatures(span, feature);
        } catch (Exception x) {
            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.SEVERE, "cannot load {0}, {1}", new Object[]{feature.getFeatureName(), x.getMessage()});
        }
        return Collections.<String, List<? extends SeqSymmetry>>emptyMap();
    }

}
