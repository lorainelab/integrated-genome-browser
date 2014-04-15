package com.affymetrix.sequenceviewer;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.util.ErrorHandler;

import com.affymetrix.igb.osgi.service.IGBService;

public class ViewGenomicSequenceInSeqViewerAction extends GenericAction implements SymSelectionListener {
	private static final long serialVersionUID = 1l;
	private IGBService igbService;

	public ViewGenomicSequenceInSeqViewerAction(IGBService igbService) {
		super(AbstractSequenceViewer.BUNDLE.getString("ViewGenomicSequenceInSeqViewer"), null, "16x16/actions/Sequence_Viewer.png", "22x22/actions/Sequence_Viewer.png", KeyEvent.VK_UNDEFINED, null, true);
		GenometryModel.getGenometryModel().addSymSelectionListener(this);
		setEnabled(false);
		this.igbService = igbService;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		try {
			DefaultSequenceViewer sv = new DefaultSequenceViewer(igbService);
			sv.startSequenceViewer();
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex, Level.WARNING);
		}
	}
	
	public void symSelectionChanged(SymSelectionEvent evt) {
		if ((evt.getSelectedGraphSyms().isEmpty() && igbService.getSeqMapView().getSeqSymmetry() == null) 
				|| (!evt.getSelectedGraphSyms().isEmpty() && evt.getSelectedGraphSyms().get(0) instanceof GraphSym)) {
			setEnabled(false);
		} else {
			setEnabled(true);
		}		
	}
}
