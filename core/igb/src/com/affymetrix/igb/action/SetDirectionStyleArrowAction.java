package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

public class SetDirectionStyleArrowAction extends SetDirectionStyleActionA {
	private static final long serialVersionUID = 1L;
	private static final SetDirectionStyleArrowAction ACTION = new SetDirectionStyleArrowAction();
	
	public static SetDirectionStyleArrowAction getAction() {
		return ACTION;
	}

	private SetDirectionStyleArrowAction() {
		super("Set Direction Style Arrow", null, null);
	}

	@Override
	protected boolean isArrowStyle(ITrackStyleExtended style) {
		return true;
	}
}
