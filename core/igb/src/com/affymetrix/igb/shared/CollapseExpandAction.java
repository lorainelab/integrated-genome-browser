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

	/**
	 * Load the class and, so, run the static code
	 * which creates a singleton.
	 * Add it to the {@link GenericActionHolder}.
	 * This is in lieu of the static {@link #getAction} method
	 * used by other actions.
	 */
	public static void createSingleton() {
		GenericActionHolder.getInstance().addGenericAction(ACTION);
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
