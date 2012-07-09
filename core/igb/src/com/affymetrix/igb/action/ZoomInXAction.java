package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;

public class ZoomInXAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final ZoomInXAction ACTION = new ZoomInXAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ZoomInXAction getAction() {
		return ACTION;
	}

	public ZoomInXAction() {
		super("Zoom in horizontally",
				"toolbarButtonGraphics/general/ZoomIn16.gif",
				"toolbarButtonGraphics/general/ZoomIn16.gif" // for tool bar
				);
		this.ordinal = -4004010;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap seqmap = getSeqMapView().getSeqMap();
		Adjustable adj = seqmap.getZoomer(NeoMap.X);
		adj.setValue(adj.getValue() + (adj.getMaximum() - adj.getMinimum()) / 20);
	}
}
