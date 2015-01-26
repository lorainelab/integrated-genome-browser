package com.affymetrix.sequenceviewer;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.event.GenericActionDoneCallback;
import com.affymetrix.genometry.symmetry.SymWithResidues;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

import com.affymetrix.igb.service.api.IGBService;

public class ReadSequenceViewer extends AbstractSequenceViewer {

    public ReadSequenceViewer(IGBService igbService) {
        super(igbService);
    }

    @Override
    public String getResidues(SeqSymmetry sym, BioSeq aseq) {
        return ((SymWithResidues) sym).getResidues();
    }

    @Override
    public void doBackground(final GenericActionDoneCallback doneback) {
        doneback.actionDone(null);
    }

    @Override
    protected void addIntron(SeqSymmetry sym, BioSeq aseq) {
        if (!(sym instanceof BAMSym)) {
            return;
        }

        BAMSym bamSym = (BAMSym) sym;
        for (int i = 0; i < bamSym.getInsChildCount(); i++) {
            addSequenceViewerItem(bamSym.getInsChild(i), SequenceViewerItems.TYPE.INTRON.ordinal(), aseq);
        }
    }

    @Override
    protected boolean shouldReverseOnNegative() {
        return false;
    }
}
