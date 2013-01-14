package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;

public class ZoomOutYAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final ZoomOutYAction ACTION = new ZoomOutYAction();
	private static final ZoomOutYAction ICON_ONLY_ACTION = new ZoomOutYAction("");

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ZoomOutYAction getAction() {
		return ACTION;
	}
	
	public static ZoomOutYAction getIconOnlyAction() {
		return ICON_ONLY_ACTION;
	}

	public ZoomOutYAction() {
		super("Zoom Out Vertically",
				"16x16/actions/list-remove.png", null
				);
		this.ordinal = -4004220;
	}
	
	public ZoomOutYAction(String label) {
		super("",
				"16x16/actions/list-remove.png", null
				);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap seqmap = getSeqMapView().getSeqMap();
		Adjustable adj = seqmap.getZoomer(NeoMap.Y);
		adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
	}
}
