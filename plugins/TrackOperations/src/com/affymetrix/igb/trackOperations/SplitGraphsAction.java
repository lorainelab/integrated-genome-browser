package com.affymetrix.igb.trackOperations;

import java.awt.event.ActionEvent;
import java.util.List;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *  Puts all selected graphs in the same tier.
 *  Current glyph factories do not support floating the combined graphs.
 */
public class SplitGraphsAction extends GenericAction {

	private static final long serialVersionUID = 1l;
	private static final SplitGraphsAction ACTION = new SplitGraphsAction();
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static SplitGraphsAction getAction() {
		return ACTION;
	}

	private SplitGraphsAction() {
		super(TrackOperationsTab.BUNDLE.getString("splitButton"), null, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		List<RootSeqSymmetry> rootSyms = TrackOperationsTab.getSingleton().getRootSyms();
		if (rootSyms.isEmpty()) {
			return;
		}

		for (SeqSymmetry sym : rootSyms) {
			if (sym instanceof GraphSym) {
				GraphSym gsym = (GraphSym)sym;
				GraphState gstate = gsym.getGraphState();
				gstate.setComboStyle(null, 0);

				// For simplicity, set the floating state of all new tiers to false.
				// Otherwise, have to calculate valid, non-overlapping y-positions and heights.
				gstate.getTierStyle().setFloatTier(false); // for simplicity
			}
		}
		TrackOperationsTab.getSingleton().updateViewer();
	}
}
