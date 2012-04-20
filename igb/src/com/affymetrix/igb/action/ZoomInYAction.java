package com.affymetrix.igb.action;

import java.awt.Adjustable;
import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyTieredMap;

public class ZoomInYAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ZoomInYAction ACTION;

	public static ZoomInYAction getAction() {
		if (ACTION == null) {
			ACTION = new ZoomInYAction();
		}
		return ACTION;
	}

	public ZoomInYAction() {
		super("Zoom in vertically", null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap seqmap = getSeqMapView().getSeqMap();
		Adjustable adj = seqmap.getZoomer(NeoMap.Y);
		adj.setValue(adj.getValue() + (adj.getMaximum() - adj.getMinimum()) / 20);
	}
}
