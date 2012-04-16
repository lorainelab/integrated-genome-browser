package com.affymetrix.igb.action;

import java.awt.Adjustable;
import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.SeqMapView;

public class ZoomOutFullyAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ZoomOutFullyAction ACTION;
	private final AffyTieredMap seqmap;

	public static ZoomOutFullyAction getAction() {
		if (ACTION == null) {
			ACTION = new ZoomOutFullyAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	public ZoomOutFullyAction(SeqMapView gviewer) {
		super(gviewer, "Home Position", "Zoom out fully", null);
		seqmap = gviewer.getSeqMap();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Adjustable adj = seqmap.getZoomer(NeoMap.X);
		adj.setValue(adj.getMinimum());
		adj = seqmap.getZoomer(NeoMap.Y);
		adj.setValue(adj.getMinimum());
	}
}
