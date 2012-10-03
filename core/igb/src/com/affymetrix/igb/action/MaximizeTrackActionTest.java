package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.ActionEvent;

import com.affymetrix.igb.shared.TierGlyph;

public class MaximizeTrackActionTest extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final MaximizeTrackActionTest ACTION = new MaximizeTrackActionTest();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static MaximizeTrackActionTest getAction() {
		return ACTION;
	}

	private MaximizeTrackActionTest() {
		super("Focus on Track (2)","16x16/actions/fit to window like IGV.png", "22x22/actions/fit to window like IGV.png");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		focusTrack(getTierManager().getSelectedTiers().get(0));
	}
	
	private void focusTrack(TierGlyph selectedTier) {
		// set zoom to height of selected track
		double tierCoordHeight = selectedTier.getCoordBox().getHeight();
		int totalHeight = getTierMap().getView().getPixelBox().height;
		double zoom_scale = totalHeight / tierCoordHeight;
		getTierMap().zoom(getSeqMapView().getSeqMap().Y, zoom_scale);
		// set scroll to top of selected track
		double coord_value = 0;
		// add up height of all tiers up to selected tier
		for (TierGlyph tierGlyph : getTierMap().getTiers()) {
			if (tierGlyph == selectedTier) {
				break;
			}
			coord_value += tierGlyph.getCoordBox().getHeight();
		}
		coord_value += 1; // fudge factor
		getTierMap().scroll(getSeqMapView().getSeqMap().Y, coord_value);
		getTierMap().updateWidget();
	}
}
