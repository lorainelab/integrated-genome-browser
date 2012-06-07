package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

public class SetDirectionStyleColorAction extends SetDirectionStyleActionA {
	private static final long serialVersionUID = 1L;
	private static final SetDirectionStyleColorAction ACTION = new SetDirectionStyleColorAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}

	public static SetDirectionStyleColorAction getAction() {
		return ACTION;
	}

	private SetDirectionStyleColorAction() {
		super("Set Direction Style Color", null, null);
	}

	@Override
	protected boolean isColorStyle(ITrackStyleExtended style) {
		return true;
	}
}
