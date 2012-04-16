package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.SeqMapView;

public class ScrollRightAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ScrollRightAction ACTION;
	private final AffyTieredMap seqmap;

	public static ScrollRightAction getAction() {
		if (ACTION == null) {
			ACTION = new ScrollRightAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	public ScrollRightAction(SeqMapView gviewer) {
		super(gviewer, "Scroll Right", null, null);
		seqmap = gviewer.getSeqMap();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		int[] visible = seqmap.getVisibleRange();
		seqmap.scroll(NeoAbstractWidget.X, visible[0] + (visible[1] - visible[0]) / 10);
		seqmap.updateWidget();
	}
}
