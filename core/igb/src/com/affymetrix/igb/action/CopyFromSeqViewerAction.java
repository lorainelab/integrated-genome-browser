package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.view.AbstractSequenceViewer;

public class CopyFromSeqViewerAction extends GenericAction {
	private static final long serialVersionUID = 1l;

	AbstractSequenceViewer sv;
	public CopyFromSeqViewerAction(AbstractSequenceViewer sv) {
		super(BUNDLE.getString("copySelectedResiduesToClipboard"), KeyEvent.VK_C);
		this.sv=sv;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		sv.copyAction();
	}
}
