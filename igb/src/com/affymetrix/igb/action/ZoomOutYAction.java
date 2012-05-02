package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyTieredMap;

public class ZoomOutYAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ZoomOutYAction ACTION = new ZoomOutYAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ZoomOutYAction getAction() {
		return ACTION;
	}

	public ZoomOutYAction() {
		super("Zoom out vertically", "toolbarButtonGraphics/general/ZoomOut16.gif", null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap seqmap = getSeqMapView().getSeqMap();
		Adjustable adj = seqmap.getZoomer(NeoMap.Y);
		adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
	}
}
