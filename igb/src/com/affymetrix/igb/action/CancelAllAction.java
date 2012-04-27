package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.util.ThreadHandler;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class CancelAllAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final CancelAllAction ACTION = new CancelAllAction();

	public static CancelAllAction getAction() {
		return ACTION;
	}
	public CancelAllAction() {
		super(BUNDLE.getString("cancelAllAction"), BUNDLE.getString("cancelAllActionTooltip"), "images/stop.png", KeyEvent.VK_UNDEFINED);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		ThreadHandler.getThreadHandler().cancelAllTasks();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Cancelled all threads");
	}
}
