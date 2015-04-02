package com.affymetrix.genometry.util;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ServerTypeI {

    /**
     * @return the name of the Server Type
     */
    public String getName();

    /**
     * format the URL, e.g. add or remove trailing /
     *
     * @param url to format
     * @return formatted URL
     */
    public String formatURL(String url);

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
     */
    public boolean getSpeciesAndVersions(GenericServer gServer, VersionDiscoverer versionDiscoverer);

    /**
     * @param span span containing the range for which you want annotations.
     * @param feature the generic feature that is to be loaded from the server.
     * @return Map<String, <? extends SeqSymmetry>> list of data that were loaded.
     * @throws IOException
     */
    public Map<String, List<? extends SeqSymmetry>> loadFeatures(SeqSpan span, GenericFeature feature) throws Exception;

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

}
