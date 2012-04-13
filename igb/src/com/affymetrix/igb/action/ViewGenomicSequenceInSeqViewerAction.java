/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.GenometryModel;
import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.view.DefaultSequenceViewer;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author auser
 */
public class ViewGenomicSequenceInSeqViewerAction extends GenericAction implements SymSelectionListener {
	private static final long serialVersionUID = 1l;
	private static final ViewGenomicSequenceInSeqViewerAction ACTION = new ViewGenomicSequenceInSeqViewerAction();

	public static ViewGenomicSequenceInSeqViewerAction getAction() {
		return ACTION;
	}

	private ViewGenomicSequenceInSeqViewerAction() {
		super(BUNDLE.getString("ViewGenomicSequenceInSeqViewer"), "toolbarButtonGraphics/general/Zoom16.gif");
		GenometryModel.getGenometryModel().addSymSelectionListener(this);
//		KeyStroke ks = MenuUtil.addAccelerator(comp, this, BUNDLE.getString("ViewGenomicSequenceInSeqViewer"));
//		if (ks != null) {
//			this.putValue(MNEMONIC_KEY, ks.getKeyCode());
//		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		try {
			DefaultSequenceViewer sv = new DefaultSequenceViewer();
			sv.startSequenceViewer();
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex);
		}
	}

	@Override
	public boolean isPopup() {
		return true;
	}
	
	public void symSelectionChanged(SymSelectionEvent evt) {
		if (!evt.getSelectedSyms().isEmpty() && evt.getSelectedSyms().get(0) instanceof GraphSym) {
			setEnabled(false);
		} else {
			setEnabled(true);
		}
	}
}
