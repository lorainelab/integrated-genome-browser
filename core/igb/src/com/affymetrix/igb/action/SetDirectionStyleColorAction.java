package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

public class SetDirectionStyleColorAction extends SetDirectionStyleActionA {
	private static final long serialVersionUID = 1L;
	private static final SetDirectionStyleColorAction ACTION
			= new SetDirectionStyleColorAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}

	public static SetDirectionStyleColorAction getAction() {
		return ACTION;
	}

	private SetDirectionStyleColorAction() {
		super("Set Direction Style Color", 
				"16x16/actions/strandscoloreddifferently.png",
				"22x22/actions/strandscoloreddifferently.png");
	}

	@Override
	protected boolean isColorStyle(ITrackStyleExtended style) {
		return true;
	}

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		UnsetDirectionStyleColorAction.getAction().setEnabled(true);
		this.setEnabled(false);
	}

}
