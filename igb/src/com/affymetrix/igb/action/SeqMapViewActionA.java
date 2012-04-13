package com.affymetrix.igb.action;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.view.SeqMapView;

public abstract class SeqMapViewActionA extends GenericAction {
	private static final long serialVersionUID = 1L;
	protected static final Map<String, SeqMapViewActionA> ACTION_MAP = new HashMap<String, SeqMapViewActionA>();

	protected SeqMapViewActionA(SeqMapView gviewer, String text, String iconPath) {
		this(gviewer, text, null, iconPath);
	}

	protected SeqMapViewActionA(SeqMapView gviewer, String text, String tooltip, String iconPath) {
		super(text, tooltip, iconPath, KeyEvent.VK_UNDEFINED);
		this.gviewer = gviewer;
		this.handler = gviewer.getTierManager();
		ACTION_MAP.put(gviewer.getId(), this);
	}

	protected SeqMapView gviewer;
	protected TierLabelManager handler;
	protected void refreshMap(boolean stretch_vertically, boolean stretch_horizonatally) {
		if (gviewer != null) {
			// if an AnnotatedSeqViewer is being used, ask it to update itself.
			// later this can be made more specific to just update the tiers that changed
			boolean preserve_view_x = !stretch_vertically;
			boolean preserve_view_y = !stretch_horizonatally;
			gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, preserve_view_x, preserve_view_y);
		} else {
			// if no AnnotatedSeqViewer (as in simple test programs), update the tiermap itself.
			handler.repackTheTiers(false, stretch_vertically);
		}
	}
}
