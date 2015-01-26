package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import java.util.List;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author hiralv
 */
public class SymLoaderInstNC extends SymLoaderInst {

    private static final List<LoadStrategy> strategyList = new ArrayList<>();

    static {
        strategyList.add(LoadStrategy.NO_LOAD);
        strategyList.add(LoadStrategy.GENOME);
    }

    public SymLoaderInstNC(URI uri, String featureName, AnnotatedSeqGroup group) {
        super(uri, featureName, group);
    }

    @Override
    public List<LoadStrategy> getLoadChoices() {
        return strategyList;
    }

    @Override
    public List<BioSeq> getChromosomeList() {
        return Collections.<BioSeq>emptyList();
    }

    @Override
    public List<? extends SeqSymmetry> getGenome() throws Exception {
        return super.getGenome();
    }
}
