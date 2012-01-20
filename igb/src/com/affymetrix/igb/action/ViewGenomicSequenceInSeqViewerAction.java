/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.view.SequenceViewer;
import java.awt.event.ActionEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author auser
 */
public class ViewGenomicSequenceInSeqViewerAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ViewGenomicSequenceInSeqViewerAction ACTION = new ViewGenomicSequenceInSeqViewerAction();

	public static ViewGenomicSequenceInSeqViewerAction getAction() {
		return ACTION;
	}

	private ViewGenomicSequenceInSeqViewerAction() {
		super();
		this.setEnabled(false);
//		KeyStroke ks = MenuUtil.addAccelerator(comp, this, BUNDLE.getString("ViewGenomicSequenceInSeqViewer"));
//		if (ks != null) {
//			this.putValue(MNEMONIC_KEY, ks.getKeyCode());
//		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		try {
			SequenceViewer sv = new SequenceViewer();
			sv.startSequenceViewer();
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex);
		}
	}

	@Override
	public String getText() {
		return BUNDLE.getString("ViewGenomicSequenceInSeqViewer");
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
