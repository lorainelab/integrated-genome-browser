package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.SeqMapView;

public class ScrollLeftAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ScrollLeftAction ACTION;
	private final AffyTieredMap seqmap;

	public static ScrollLeftAction getAction() {
		if (ACTION == null) {
			ACTION = new ScrollLeftAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	public ScrollLeftAction(SeqMapView gviewer) {
		super(gviewer, "Scroll Left", null, null);
		seqmap = gviewer.getSeqMap();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		int[] visible = seqmap.getVisibleRange();
		seqmap.scroll(NeoAbstractWidget.X, visible[0] - (visible[1] - visible[0]) / 10);
		seqmap.updateWidget();
	}
}
