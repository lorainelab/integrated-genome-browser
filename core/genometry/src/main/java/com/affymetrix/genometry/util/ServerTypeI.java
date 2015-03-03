package com.affymetrix.genometry.util;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.das.DasServerType;
import com.affymetrix.genometry.das2.Das2ServerType;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.quickload.QuickloadServerType;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public interface ServerTypeI extends Comparable<ServerTypeI> {

    // preinstalled server types
    ServerTypeI QuickLoad = QuickloadServerType.getInstance();
    ServerTypeI DAS = DasServerType.getInstance();
    ServerTypeI DAS2 = Das2ServerType.getInstance();
    ServerTypeI LocalFiles = LocalFilesServerType.getInstance();

    ServerTypeI DEFAULT = LocalFiles;

    /**
     * @return the name of the Server Type
     */
    public String getName();

    /**
     * used for ordering the ServerTypes
     *
     * @return ordinal for comparing/ordering
     */
    public int getOrdinal();

    /**
     * format the URL, e.g. add or remove trailing /
     *
     * @param url to format
     * @return formatted URL
     */
    public String formatURL(String url);

    /**
     * Initialize the server.
     *
     * @param url
     * @param name
     * @return initialized server
     */
    public Object getServerInfo(String url, String name);

    /**
     * adjustments to the URL specific to the Server Type
     *
     * @param url the URL to adjust
     * @return adjusted URL
     */
    public String adjustURL(String url);

    /**
     * indicates that the only load strategy is visible
     *
     * @return if the only load strategy is visible
     */
    public boolean loadStrategyVisibleOnly();

    /**
     * Discover feature names from version
     *
     * @param gVersion the version to search for fetatures
     * @param autoload indicates that autoload is selected by user
     */
    public void discoverFeatures(GenericVersion gVersion, boolean autoload);

    /**
     * Discover all the chromosomes (bioseq) for the passed object
     *
     * @param versionSourceObj server type specific objec
     */
    public void discoverChromosomes(Object versionSourceObj);

    /**
     * hack to ignore server hyperlinks for DAS/1.
     *
     * @return use freindly URL
     */
    public boolean hasFriendlyURL();

    /**
     * if the server type can handle a feature with an unknown extension
     *
     * @return if the server type can handle a feature with an unknown extension
     */
    public boolean canHandleFeature();

    /**
     * get all species and versions from the server
     *
     * @param gServer the server
     * @param primaryServer the primary server
     * @param primaryURL the primary URL
     * @param versionDiscoverer VersionDiscoverer callback
     * @return true if successful, false otherwise
     */
    public boolean getSpeciesAndVersions(GenericServer gServer, GenericServer primaryServer, URL primaryURL, VersionDiscoverer versionDiscoverer);

    /**
     * @param span span containing the range for which you want annotations.
     * @param feature the generic feature that is to be loaded from the server.
     * @return Map<String, <? extends SeqSymmetry>> list of data that were loaded.
     * @throws IOException
     */
    public Map<String, List<? extends SeqSymmetry>> loadFeatures(SeqSpan span, GenericFeature feature) throws Exception;

    /**
     * if user authorization is optional for this ServerType
     *
     * @return if user authorization is optional for this ServerType
     * (true if authorization is optional, false if no authorization)
     */
    public boolean isAuthOptional();

    /**
     * get Residues from the sepcified server
     *
     * @param version the version
     * @param genomeVersionName name of the version
     * @param aseq the BioSeq (chromosome)
     * @param min minimum location on bioseq
     * @param max maximum location on bioseq
     * @param span the span to get the residues
     * @return true if residues loaded, false otherwise
     */
    public boolean getResidues(GenericVersion version, String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span);

    /**
     * Removes server and cleans up anything that was added by it.
     *
     * @return
     */
    public void removeServer(GenericServer server);

    public boolean isSaveServersInPrefs();

    public String getFriendlyURL(GenericServer gServer);

    /**
     * Determines should mirror url be used or not.
     *
     * @param gServer
     * @return
     */
    public boolean useMirrorSite(GenericServer gServer);

    // the following method is required by the CacheScript class
    // this method does not need to be implemented unless there are
    // servers of this server type that will be cached.
    /**
     * Creates directory of server name.
     * Determines the server type and process it accordingly.
     *
     * @param gServer GenericServer to be processed.
     * @param path path to use.
     * @return true if successful, false otherwise
     */
    public boolean processServer(GenericServer gServer, String path);

}
