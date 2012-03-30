/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericActionDoneCallback;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithResidues;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.action.LoadResidueAction;
import javax.swing.SwingWorker;

/**
 *
 * @author auser
 */
public class DefaultSequenceViewer extends AbstractSequenceViewer{

	@Override
	public void doBackground(final GenericActionDoneCallback doneback) {
		SwingWorker worker = new SwingWorker() {
			@Override
			protected Object doInBackground() throws Exception{
				LoadResidueAction loadResidue = new LoadResidueAction(residues_sym.getSpan(aseq), true);
				loadResidue.addDoneCallback(doneback);
				loadResidue.actionPerformed(null);
				loadResidue.removeDoneCallback(doneback);
				return null;
			}
		};
		worker.execute();
	}

	@Override
	public String getResidues(SeqSymmetry sym, BioSeq aseq) {
		return SeqUtils.getResidues(sym, aseq);
	}

	@Override
	protected void addIntron(SeqSymmetry residues_sym, BioSeq aseq) {
		addSequenceViewerItems(SeqUtils.getIntronSym(residues_sym, aseq), SequenceViewerItems.TYPE.INTRON.ordinal(), aseq);
	}
	
	
}
