package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

public class UnsetDirectionStyleColorAction extends SetDirectionStyleActionA {
	private static final long serialVersionUID = 1L;
	private static final UnsetDirectionStyleColorAction ACTION = new UnsetDirectionStyleColorAction();
	
	public static UnsetDirectionStyleColorAction getAction() {
		return ACTION;
	}

	private UnsetDirectionStyleColorAction() {
		super("Unset Direction Style Color", null, null);
	}

	@Override
	protected boolean isColorStyle(ITrackStyleExtended style) {
		return false;
	}
}
