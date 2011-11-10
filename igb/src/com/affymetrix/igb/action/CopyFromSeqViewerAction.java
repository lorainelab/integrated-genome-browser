package com.affymetrix.igb.action;

import com.affymetrix.igb.view.SequenceViewer;
import com.affymetrix.genometryImpl.event.GenericAction;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class CopyFromSeqViewerAction extends GenericAction {
	private static final long serialVersionUID = 1l;

	SequenceViewer sv;
	public CopyFromSeqViewerAction(SequenceViewer sv) {
		super();
		this.sv=sv;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		sv.copyAction();
	}

	@Override
	public String getIconPath() {
		return null;
	}

	@Override
	public String getText() {
		return BUNDLE.getString("copySelectedResiduesToClipboard");
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_C;
	}
}
