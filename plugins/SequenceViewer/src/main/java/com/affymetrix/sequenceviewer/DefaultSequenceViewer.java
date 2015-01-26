package com.affymetrix.sequenceviewer;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericActionDoneCallback;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.service.api.IGBService;
import com.affymetrix.igb.shared.SequenceLoader;

public class DefaultSequenceViewer extends AbstractSequenceViewer {

    public DefaultSequenceViewer(IGBService igbService) {
        super(igbService);
    }

    @Override
    public void doBackground(final GenericActionDoneCallback doneback) {
        SequenceLoader worker = new SequenceLoader("default sequence viewer", residues_sym.getSpan(aseq), doneback);
        CThreadHolder.getInstance().execute(this, worker);
    }

    @Override
    public String getResidues(SeqSymmetry sym, BioSeq aseq) {
        return SeqUtils.getResidues(sym, aseq);
    }

    @Override
    protected void addIntron(SeqSymmetry residues_sym, BioSeq aseq) {
        addSequenceViewerItems(SeqUtils.getIntronSym(residues_sym, aseq), SequenceViewerItems.TYPE.INTRON.ordinal(), aseq);
    }

    @Override
    protected boolean shouldReverseOnNegative() {
        return true;
    }
}
