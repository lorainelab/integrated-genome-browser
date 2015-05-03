package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * common parent class for all Fasta SymLoaders
 *
 * @author jnicol
 */
public abstract class FastaCommon extends SymLoader {

    private static final List<String> pref_list = new ArrayList<>();

    static {
        pref_list.add("fa");
    }

    protected final List<BioSeq> chrSet = new ArrayList<>();

    private static final List<LoadStrategy> strategyList = new ArrayList<>();

    static {
        strategyList.add(LoadStrategy.NO_LOAD);
        strategyList.add(LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
    }

    public FastaCommon(URI uri, String featureName, GenomeVersion genomeVersion) {
        super(uri, "", genomeVersion);
        this.isResidueLoader = true;
    }

    @Override
    public void init() throws Exception {
        if (this.isInitialized) {
            return;
        }
        if (initChromosomes()) {
            super.init();
        }
    }

    @Override
    public List<LoadStrategy> getLoadChoices() {
        return strategyList;
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        init();
        return chrSet;
    }

    /**
     * Get seqids and lengths for all chromosomes.
     */
    protected abstract boolean initChromosomes() throws Exception;

    @Override
    public List<String> getFormatPrefList() {
        return pref_list;
    }
}
