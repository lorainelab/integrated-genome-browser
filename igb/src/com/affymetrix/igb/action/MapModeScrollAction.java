package com.affymetrix.igb.action;

import com.affymetrix.igb.view.SeqMapView.MapMode;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 * button action for SeqMapView modes
 */
public class MapModeScrollAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1l;

	public MapModeScrollAction(String id) {
		super(
			  BUNDLE.getString(MapMode.MapScrollMode.name() + "Button"),
			  BUNDLE.getString(MapMode.MapScrollMode.name() + "Tip"),
			  "images/open_hand.png",
			  KeyEvent.VK_UNDEFINED
		);
		this.id = id;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		getSeqMapView().setMapMode(MapMode.MapScrollMode);
	}
}