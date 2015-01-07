/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.VersionDiscoverer;
import com.affymetrix.igb.general.ServerList;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
public interface GeneralLoadUtilsService {

    void addFeature(GenericFeature gFeature);

    void addGenomeVirtualSeq(AnnotatedSeqGroup group);

    /**
     * Add specified server, finding species and versions associated with it.
     *
     * @param serverName
     * @param serverURL
     * @param serverType
     * @return success of server add.
     */
    GenericServer addServer(ServerList serverList, ServerTypeI serverType, String serverName, String serverURL, int order, boolean isDefault, String mirrorURL);

    boolean discoverServer(GenericServer gServer);

    GenericFeature getFeature(URI uri, String fileName, String speciesName, AnnotatedSeqGroup loadGroup, boolean isReferenceSequence);

    /**
     * Returns the list of features for the genome with the given version name.
     * The list may (rarely) be empty, but never null.
     */
    List<GenericFeature> getFeatures(AnnotatedSeqGroup group);

    List<String> getGenericVersions(final String speciesName);

    GenericVersion getIGBFilesVersion(AnnotatedSeqGroup group, String speciesName);

    GenericFeature getLoadedFeature(URI uri);

    public boolean loadResidues(String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span);

    public void bufferDataForAutoload();

    public void iterateSeqList(final GenericFeature feature);

    /**
     * An AnnotatedSeqGroup was added independently of the GeneralLoadUtils.
     * Update GeneralLoadUtils state.
     *
     * @return genome version
     */
    GenericVersion getLocalFilesVersion(AnnotatedSeqGroup group, String speciesName);

    /**
     * Get residues from servers: DAS/2, Quickload, or DAS/1. Also gets partial
     * residues.
     *
     * @param genomeVersionName -- name of the genome.
     * @param span	-- May be null. If not, then it's used for partial loading.
     */
    // Most confusing thing here -- certain parsers update the composition, and certain ones do not.
    // DAS/1 and partial loading in DAS/2 do not update the composition, so it's done separately.
    boolean getResidues(Set<GenericVersion> versionsWithChrom, String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span);

    /*
     * Returns the list of features for currently selected group.
     */
    List<GenericFeature> getSelectedVersionFeatures();

    /**
     * Get directory url on cached server from servermapping map.
     *
     * @param url	URL of the server.
     * @return	Returns a directory if exists else null.
     */
    URL getServerDirectory(String url);

    /**
     * Returns the list of servers associated with the given versions.
     *
     * @param features -- assumed to be non-null.
     * @return A list of servers associated with the given versions.
     */
    List<GenericServer> getServersWithAssociatedFeatures(List<GenericFeature> features);

    List<String> getSpeciesList();

    /**
     * An AnnotatedSeqGroup was added independently of the GeneralLoadUtils.
     * Update GeneralLoadUtils state.
     *
     * @param aseq
     * @return genome version
     */
    GenericVersion getUnknownVersion(AnnotatedSeqGroup aseq);

    /**
     * Only want to display features with visible attribute set to true.
     *
     * @return list of visible features
     */
    List<GenericFeature> getVisibleFeatures();

    /**
     * Make sure this genome version has been initialized.
     *
     * @param versionName
     */
    void initVersionAndSeq(final String versionName);

    boolean isLoaded(GenericFeature gFeature);

    /**
     * Get synonyms of version.
     *
     * @param versionName - version name
     * @return a friendly HTML string of version synonyms (not including
     * versionName).
     */
    String listSynonyms(String versionName);

    /**
     * For unoptimized file formats load symmetries and add them.
     *
     * @param feature
     */
    void loadAllSymmetriesThread(final GenericFeature feature);

    /**
     * Load and display annotations (requested for the specific feature). Adjust
     * the load status accordingly.
     */
    void loadAndDisplayAnnotations(GenericFeature gFeature);

    void loadAndDisplaySpan(final SeqSpan span, final GenericFeature feature);

    /**
     * Load the sequence info for the given group. Try loading from DAS/2 before
     * loading from DAS; chances are DAS/2 will be faster, and that the
     * chromosome names will be closer to what is expected.
     */
    void loadChromInfo(AnnotatedSeqGroup group);

    /**
     * Load the annotations for the given version. This is specific to one
     * server.
     *
     * @param gVersion
     */
    void loadFeatureNames(final GenericVersion gVersion);

    //TO DO: Make this private again.
    Map<String, List<? extends SeqSymmetry>> loadFeaturesForSym(GenericFeature feature, SeqSymmetry optimized_sym) throws OutOfMemoryError, Exception;

    /**
     * Method to load server directory mapping.
     */
    void loadServerMapping();

    void openURI(URI uri, String fileName, AnnotatedSeqGroup loadGroup, String speciesName, boolean isReferenceSequence);

    /**
     * Set autoload variable in features.
     *
     * @param autoload
     */
    void setFeatureAutoLoad(boolean autoload);

    @Reference
    void setSeqGroupView(SeqGroupViewI seqGroupView);

    @Reference
    void setVersionDiscoverer(VersionDiscoverer versionDiscoverer);

}
