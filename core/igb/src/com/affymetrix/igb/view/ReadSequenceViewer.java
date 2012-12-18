/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericActionDoneCallback;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithResidues;

/**
 *
 * This class is used to display the Read Sequence of a BAM file
 * 
 * @author Anuj
 */
public class ReadSequenceViewer extends AbstractSequenceViewer{

	@Override
	public String getResidues(SeqSymmetry sym, BioSeq aseq) {
		return ((SymWithResidues)sym).getResidues();
	}

	@Override
	public void doBackground(final GenericActionDoneCallback doneback) {
		doneback.actionDone(null);
	}
	
	@Override
	protected void addIntron(SeqSymmetry sym, BioSeq aseq) {
		if(!(sym instanceof BAMSym)){
			return;
		}
		
		BAMSym bamSym = (BAMSym)sym;
		for (int i = 0; i < bamSym.getInsChildCount(); i++) {
			addSequenceViewerItem(bamSym.getInsChild(i), SequenceViewerItems.TYPE.INTRON.ordinal(), aseq);
		}
	}
	
	@Override
	protected boolean shouldReverseOnNegative(){
		return false;
	}
}
