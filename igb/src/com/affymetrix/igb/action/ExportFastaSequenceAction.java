package com.affymetrix.igb.action;


import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.view.SequenceViewer;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ExitAction.java 5804 2010-04-28 18:54:46Z sgblanch $
 */
public class ExportFastaSequenceAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	SequenceViewer sv;
	public ExportFastaSequenceAction(SequenceViewer sv) {
		super();
		this.sv=sv;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		sv.exportSequenceFasta(false);
	}

	@Override
	public String getText() {
		return BUNDLE.getString("fastasequence");
	}

	@Override
	public int getShortcut() {
		return KeyEvent.VK_S;
	}
}
