package com.affymetrix.igb.trackAdjuster;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.*;

public class TrackPreferencesSeqMapViewPanel extends TrackPreferencesA implements Selections.RefreshSelectionListener {
	private static final long serialVersionUID = 1L;

	public TrackPreferencesSeqMapViewPanel(IGBService _igbService) {
		super(_igbService);
		Selections.addRefreshSelectionListener(this);
	}

	@Override
	public void selectionRefreshed() {
		resetAll();
	}
}
