package com.affymetrix.igb.action;

import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.SeqMapView.MapMode;
import java.awt.event.ActionEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 * button action for SeqMapView modes
 */
public class MapModeScrollAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1l;

	public MapModeScrollAction(SeqMapView seqMapView) {
		super(seqMapView,
			  BUNDLE.getString(MapMode.MapScrollMode.name() + "Button"),
			  BUNDLE.getString(MapMode.MapScrollMode.name() + "Tip"),
			  "images/open_hand.png"
		);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		gviewer.setMapMode(MapMode.MapScrollMode);
	}
}