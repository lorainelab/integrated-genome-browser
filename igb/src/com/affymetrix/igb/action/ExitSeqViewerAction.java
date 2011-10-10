package com.affymetrix.igb.action;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import com.affymetrix.genometryImpl.event.GenericAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ExitAction.java 5804 2010-04-28 18:54:46Z sgblanch $
 */
public class ExitSeqViewerAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	Frame mapframe;
	public ExitSeqViewerAction(Frame mapframe) {
		super();
		this.mapframe=mapframe;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
				new WindowEvent(mapframe,
					WindowEvent.WINDOW_CLOSING));
	}

	@Override
	public String getText() {
		return BUNDLE.getString("closeSequenceViewer");
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_W;
	}
}
