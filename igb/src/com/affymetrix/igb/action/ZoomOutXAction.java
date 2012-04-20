package com.affymetrix.igb.action;

import java.awt.Adjustable;
import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyTieredMap;

public class ZoomOutXAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ZoomOutXAction ACTION;

	public static ZoomOutXAction getAction() {
		if (ACTION == null) {
			ACTION = new ZoomOutXAction();
		}
		return ACTION;
	}

	public ZoomOutXAction() {
		super("Zoom out horizontally", null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap seqmap = getSeqMapView().getSeqMap();
		Adjustable adj = seqmap.getZoomer(NeoMap.X);
		adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
	}
}
