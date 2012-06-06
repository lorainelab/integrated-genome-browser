package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.action.CollapseAction;
import com.affymetrix.igb.action.ExpandAction;
import com.affymetrix.igb.action.SeqMapToggleAction;
import com.affymetrix.igb.action.SeqMapViewActionA;

public class CollapseExpandAction extends SeqMapToggleAction implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final CollapseExpandAction ACTION =
		new CollapseExpandAction(
			ExpandAction.getAction(),
			CollapseAction.getAction()
		);

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static CollapseExpandAction getAction() {
		return ACTION;
	}

	protected CollapseExpandAction(SeqMapViewActionA a, SeqMapViewActionA b) {
		super(a, b);
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
