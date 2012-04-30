package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.tiers.AffyTieredMap;

public class ScrollDownAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ScrollDownAction ACTION;

	public static ScrollDownAction getAction() {
		if (ACTION == null) {
			ACTION = new ScrollDownAction();
		}
		return ACTION;
	}

	public ScrollDownAction() {
		super("Scroll Down",  "16x16/actions/go-down.png", "22x22/actions/go-down.png");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap seqmap = getSeqMapView().getSeqMap();
		int[] visible = seqmap.getVisibleOffset();
		seqmap.scroll(NeoAbstractWidget.Y, visible[0] + (visible[1] - visible[0]) / 10);
		seqmap.updateWidget();
	}
}
