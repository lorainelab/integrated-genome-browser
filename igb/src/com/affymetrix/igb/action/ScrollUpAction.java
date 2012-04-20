package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;

import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.tiers.AffyTieredMap;

public class ScrollUpAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static ScrollUpAction ACTION;

	public static ScrollUpAction getAction() {
		if (ACTION == null) {
			ACTION = new ScrollUpAction();
		}
		return ACTION;
	}

	public ScrollUpAction() {
		super("Scroll Up", null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		AffyTieredMap seqmap = getSeqMapView().getSeqMap();
		int[] visible = seqmap.getVisibleOffset();
		seqmap.scroll(NeoAbstractWidget.Y, visible[0] - (visible[1] - visible[0]) / 10);
		seqmap.updateWidget();
	}
}
