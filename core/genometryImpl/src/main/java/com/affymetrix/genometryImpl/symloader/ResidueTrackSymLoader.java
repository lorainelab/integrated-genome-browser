package com.affymetrix.genometryImpl.symloader;

import java.util.List;
import java.util.ArrayList;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithResidues;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;

/**
 *
 * @author hiralv
 */
public class ResidueTrackSymLoader extends SymLoader {

    private final SymLoader symL;

    public ResidueTrackSymLoader(SymLoader loader) {
        super(loader.uri, loader.featureName, loader.group);
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
        List<SeqSymmetry> list = new ArrayList<SeqSymmetry>();
        SymWithProps sym = new SimpleSymWithResidues(uri.toString(), span.getBioSeq(), span.getStart(), span.getEnd(), "",
                0.0f, span.isForward(), 0, 0, null, null, getRegionResidues(span));
        // sym.setProperty(BAM.SHOWMASK, false);
        list.add(sym);
        return list;
    }
}
