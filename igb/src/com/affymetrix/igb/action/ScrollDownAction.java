package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.SeqMapView;

public class ScrollDownAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ScrollDownAction ACTION;
	private final AffyTieredMap seqmap;

	public static ScrollDownAction getAction() {
		if (ACTION == null) {
			ACTION = new ScrollDownAction(Application.getSingleton().getMapView());
		}
		return ACTION;
	}

	public ScrollDownAction(SeqMapView gviewer) {
		super(gviewer, "Scroll Down", null, null);
		seqmap = gviewer.getSeqMap();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		int[] visible = seqmap.getVisibleOffset();
		seqmap.scroll(NeoAbstractWidget.Y, visible[0] + (visible[1] - visible[0]) / 10);
		seqmap.updateWidget();
	}
}
