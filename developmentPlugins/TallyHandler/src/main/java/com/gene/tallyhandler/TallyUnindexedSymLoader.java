package com.gene.tallyhandler;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.symloader.LineProcessor;
import com.affymetrix.genometry.symloader.UnindexedSymLoader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TallyUnindexedSymLoader extends UnindexedSymLoader {

    public static final List<String> pref_list = new ArrayList<>();

    static {
        pref_list.add("tally");
    }

    public TallyUnindexedSymLoader(URI uri, String featureName, GenomeVersion genomeVersion) {
        super(uri, Optional.empty(), featureName, genomeVersion);
    }

    @Override
    public List<String> getFormatPrefList() {
        return pref_list;
    }

    /**
     * Returns "text/tally".
     */
    public String getMimeType() {
        return "text/tally";
    }

    @Override
    protected LineProcessor createLineProcessor(String featureName) {
        return new TallyLineProcessor(featureName);
    }
}
