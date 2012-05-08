package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.swing.recordplayback.ScriptManager;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class ClearScriptAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ClearScriptAction ACTION = new ClearScriptAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ClearScriptAction getAction() {
		return ACTION;
	}

	private ClearScriptAction() {
		super(BUNDLE.getString("clearScript"), null, "16x16/actions/edit-clear.png", "22x22/actions/edit-clear.png", KeyEvent.VK_C, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		ScriptManager.getInstance().clearScript();
	}
}
