package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.thread.CThreadHolder;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class CancelAllAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final CancelAllAction ACTION = new CancelAllAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	public static CancelAllAction getAction() {
		return ACTION;
	}
	public CancelAllAction() {
		super(BUNDLE.getString("cancelAllAction"),
			  BUNDLE.getString("cancelAllActionTooltip"),
			  "16x16/actions/stop.png",
			  "16x16/actions/stop.png", // just so it's eligible for the tool bar.
			  KeyEvent.VK_UNDEFINED);
		this.ordinal = -9006200;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		CThreadHolder.getInstance().cancelAllTasks();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Cancelled all threads");
	}
}
