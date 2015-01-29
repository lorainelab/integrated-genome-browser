package com.affymetrix.sequenceviewer;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.event.GenericActionDoneCallback;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.service.api.IgbService;
import com.affymetrix.igb.shared.SequenceLoader;

public class DefaultSequenceViewer extends AbstractSequenceViewer {

    public DefaultSequenceViewer(IgbService igbService) {
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
