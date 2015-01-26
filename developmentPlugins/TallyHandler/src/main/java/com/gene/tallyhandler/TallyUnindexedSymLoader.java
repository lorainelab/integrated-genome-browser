package com.gene.tallyhandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.symloader.UnindexedSymLoader;
import com.affymetrix.genometry.symloader.LineProcessor;

public class TallyUnindexedSymLoader extends UnindexedSymLoader {

    public static final List<String> pref_list = new ArrayList<>();

    static {
        pref_list.add("tally");
    }

    public TallyUnindexedSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
        super(uri, featureName, group);
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
