/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import com.affymetrix.igb.shared.SequenceLoader;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericActionDoneCallback;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.util.SeqUtils;

/**
 *
 * @author auser
 */
public class DefaultSequenceViewer extends AbstractSequenceViewer{

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
	protected boolean shouldReverseOnNegative(){
		return true;
	}
}
