package com.affymetrix.igb.action;

import java.awt.Adjustable;
import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.SeqMapView;

public class ZoomOutYAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ZoomOutYAction ACTION;
	private final AffyTieredMap seqmap;

	public static ZoomOutYAction getAction() {
		if (ACTION == null) {
			ACTION = new ZoomOutYAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	public ZoomOutYAction(SeqMapView gviewer) {
		super(gviewer, "Zoom out vertically", null, null);
		seqmap = gviewer.getSeqMap();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Adjustable adj = seqmap.getZoomer(NeoMap.Y);
		adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
	}
}
