package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;

public class ZoomOutXAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final ZoomOutXAction ACTION = new ZoomOutXAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ZoomOutXAction getAction() {
		return ACTION;
	}

	public ZoomOutXAction() {
		super("Zoom out horizontally",
				"toolbarButtonGraphics/general/ZoomOut16.gif",
				"toolbarButtonGraphics/general/ZoomOut16.gif" // for tool bar
				);
		this.ordinal = -4004020;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap seqmap = getSeqMapView().getSeqMap();
		Adjustable adj = seqmap.getZoomer(NeoMap.X);
		adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
	}
}
