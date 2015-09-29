package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithResidues;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author hiralv
 */
public class ResidueTrackSymLoader extends SymLoader {

    private final SymLoader symL;

    public ResidueTrackSymLoader(SymLoader loader) {
        super(loader.uri, Optional.ofNullable(loader.indexUri), loader.featureName, loader.genomeVersion);
        this.symL = loader;
    }

    @Override
    public void init() throws Exception {
        symL.init();
        this.isInitialized = true;
    }

    public void loadAsReferenceSequence(boolean bool) {
        this.isResidueLoader = bool;
    }

    @Override
    public List<LoadStrategy> getLoadChoices() {
        return symL.getLoadChoices();
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        init();
        return symL.getChromosomeList();
    }

    @Override
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
        init();
        return getResidueTrack(new SimpleSeqSpan(0, seq.getLength(), seq));
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
        init();
        return getResidueTrack(overlapSpan);
    }

    @Override
    public String getRegionResidues(SeqSpan span) throws Exception {
        return symL.getRegionResidues(span);
    }

    private List<? extends SeqSymmetry> getResidueTrack(SeqSpan span) throws Exception {
        List<SeqSymmetry> list = new ArrayList<>();
        SymWithProps sym = new SimpleSymWithResidues(uri.toString(), span.getBioSeq(), span.getStart(), span.getEnd(), "",
                0.0f, span.isForward(), 0, 0, null, null, getRegionResidues(span));
        // sym.setProperty(BAM.SHOWMASK, false);
        list.add(sym);
        return list;
    }
}
