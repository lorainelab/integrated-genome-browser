package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;

public class UnsetDirectionStyleArrowAction extends SetDirectionStyleActionA {
	private static final long serialVersionUID = 1L;
	private static final UnsetDirectionStyleArrowAction ACTION = new UnsetDirectionStyleArrowAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static UnsetDirectionStyleArrowAction getAction() {
		return ACTION;
	}

	private UnsetDirectionStyleArrowAction() {
		super("Unset Direction Style Arrow", null, null);
	}

	@Override
	protected boolean isArrowStyle(ITrackStyleExtended style) {
		return false;
	}
}
