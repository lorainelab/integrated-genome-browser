package com.affymetrix.igb.action;

import java.awt.Adjustable;
import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.SeqMapView;

public class ZoomInXAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ZoomInXAction ACTION;
	private final AffyTieredMap seqmap;

	public static ZoomInXAction getAction() {
		if (ACTION == null) {
			ACTION = new ZoomInXAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	public ZoomInXAction(SeqMapView gviewer) {
		super(gviewer, "Zoom in horizontally", null, null);
		seqmap = gviewer.getSeqMap();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Adjustable adj = seqmap.getZoomer(NeoMap.X);
		adj.setValue(adj.getValue() + (adj.getMaximum() - adj.getMinimum()) / 20);
	}
}
