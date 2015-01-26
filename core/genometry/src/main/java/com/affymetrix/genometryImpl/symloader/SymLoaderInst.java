package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.comparator.BioSeqComparator;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class SymLoaderInst extends SymLoader {

    private final List<BioSeq> chromosomeList = new ArrayList<>();

    public SymLoaderInst(URI uri, String featureName, AnnotatedSeqGroup group) {
        super(uri, featureName, group);
    }

    @Override
    public void init() throws Exception {
        if (this.isInitialized) {
            return;
        }
        super.init();

        for (BioSeq seq : SymLoader.getChromosomes(uri, featureName, group.getID())) {
            chromosomeList.add(group.addSeq(seq.getID(), seq.getLength(), uri.toString()));
        }
        Collections.sort(chromosomeList, new BioSeqComparator());
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        init();
        return chromosomeList;
    }

    @Override
    public List<? extends SeqSymmetry> getGenome() throws Exception {
        init();
        return super.getGenome();
    }

    @Override
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
        init();
        return super.getChromosome(seq);
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
        init();
        return super.getRegion(overlapSpan);
    }
}
