package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TrackStyle;
import static com.affymetrix.igb.shared.Selections.*;

public class RestoreToDefaultAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	private static final RestoreToDefaultAction ACTION = new RestoreToDefaultAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static RestoreToDefaultAction getAction() {
		return ACTION;
	}

	public RestoreToDefaultAction() {
		super("Restore track to Default settings", null, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (getTierManager().getSelectedTiers() == null) {
			return;
		}
		for (ITrackStyleExtended style : allStyles) {
			if (style instanceof TrackStyle) {
				((TrackStyle)style).restoreToDefault();
			}
		}
		
		for (GraphState graphState: graphStates) {
			graphState.restoreToDefault();
		}
		
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		getSeqMapView().updatePanel();
		getSeqMapView().getPopup().refreshMap(false, true);
	}
}
