package com.affymetrix.genometry.util;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.quickload.QuickLoadSymLoader;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalFilesServerType implements ServerTypeI {

    private static final String name = "Local Files";
    public static final int ordinal = 40;
    private static final LocalFilesServerType instance = new LocalFilesServerType();

    public static LocalFilesServerType getInstance() {
        return instance;
    }

    private LocalFilesServerType() {
        super();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int compareTo(ServerTypeI o) {
        return ordinal - o.getOrdinal();
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean processServer(GenericServer gServer, String path) {
        return false;
    }

    @Override
    public String formatURL(String url) {
        return url;
    }

    @Override
    public Object getServerInfo(String url, String name) {
        return null;
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
    public boolean getSpeciesAndVersions(GenericServer gServer,
            GenericServer primaryServer, URL primaryURL, VersionDiscoverer versionDiscoverer) {
        return false;
    }

    @Override
    public Map<String, List<? extends SeqSymmetry>> loadFeatures(SeqSpan span, GenericFeature feature)
            throws Exception {
        if (((QuickLoadSymLoader) feature.symL).getSymLoader() != null
                && (((QuickLoadSymLoader) feature.symL).getSymLoader().isResidueLoader())) {
            return Collections.<String, List<? extends SeqSymmetry>>emptyMap();
        }
        return (((QuickLoadSymLoader) feature.symL).loadFeatures(span, feature));
    }

    @Override
    public boolean isAuthOptional() {
        return false;
    }

    @Override
    public boolean getResidues(GenericVersion version, String genomeVersionName,
            BioSeq aseq, int min, int max, SeqSpan span) {
        for (GenericFeature feature : version.getFeatures()) {
            if (feature.symL == null || !feature.symL.isResidueLoader()) {
                continue;
            }
            try {
                String residues = feature.symL.getRegionResidues(span);
                if (residues != null) {
                    BioSeqUtils.addResiduesToComposition(aseq, residues, span);
                    return true;
                }
            } catch (Exception ex) {
                Logger.getLogger(LocalFilesServerType.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    @Override
    public void removeServer(GenericServer server) {
        // Do Nothing for now
    }

    @Override
    public boolean isSaveServersInPrefs() {
        return false;
    }

    @Override
    public String getFriendlyURL(GenericServer gServer) {
        if (gServer.getURL() == null) {
            return null;
        }
        String tempURL = gServer.getURL();
        if (tempURL.endsWith("/")) {
            tempURL = tempURL.substring(0, tempURL.length() - 1);
        }
        if (gServer.getServerType() != null) {
            tempURL = gServer.getServerType().adjustURL(tempURL);
        }
        return tempURL;
    }

    @Override
    public boolean useMirrorSite(GenericServer server) {
        return false;
    }
}
