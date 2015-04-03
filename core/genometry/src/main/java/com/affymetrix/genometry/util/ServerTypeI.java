package com.affymetrix.genometry.util;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.GenericFeature;
import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.List;
import java.util.Map;

public interface ServerTypeI {

    public String getServerName();

    public void discoverFeatures(GenericVersion gVersion, boolean autoload);

    public void discoverChromosomes(Object versionSourceObj);

    public boolean getSpeciesAndVersions(GenericServer gServer, VersionDiscoverer versionDiscoverer);

    public Map<String, List<? extends SeqSymmetry>> loadFeatures(SeqSpan span, GenericFeature feature) throws Exception;

    public boolean loadResidues(GenericVersion version, String genomeVersionName, BioSeq aseq, int min, int max, SeqSpan span);

    public boolean supportsUserAddedInstances();

}
