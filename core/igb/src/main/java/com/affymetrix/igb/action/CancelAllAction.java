package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

public class CancelAllAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final CancelAllAction ACTION = new CancelAllAction();
	private static final Logger ourLogger =
			Logger.getLogger(CancelAllAction.class.getPackage().getName());

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
		ourLogger.info("Cancelled all threads.");
	}
}
