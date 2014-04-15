package com.affymetrix.sequenceviewer;

import java.util.List;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author hiralv
 */
public class PopupListener implements ContextualPopupListener{
	
	JMenuItem genomicSequenceViewer, readSequenceViewer;
	
	PopupListener(GenericAction genomicSequenceAction, GenericAction readSequencAction){
		this.genomicSequenceViewer = new JMenuItem(genomicSequenceAction);
		this.genomicSequenceViewer.setIcon(null);
		
		this.readSequenceViewer = new JMenuItem(readSequencAction);
		this.readSequenceViewer.setIcon(null);
	}
	
	@Override
	public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_syms, SeqSymmetry primary_sym){
		 if (!selected_syms.isEmpty() && !(selected_syms.get(0) instanceof GraphSym)) {			
			popup.add(genomicSequenceViewer);
			popup.add(readSequenceViewer);
		}
	}
}
