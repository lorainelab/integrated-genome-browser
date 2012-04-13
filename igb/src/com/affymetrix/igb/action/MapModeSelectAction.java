package com.affymetrix.igb.action;

import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.SeqMapView.MapMode;
import java.awt.event.ActionEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 * button action for SeqMapView modes
 */
public class MapModeSelectAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1l;

	public MapModeSelectAction(SeqMapView seqMapView) {
		super(seqMapView,
				BUNDLE.getString(MapMode.MapSelectMode.name() + "Button"),
				BUNDLE.getString(MapMode.MapSelectMode.name() + "Tip"),
				"images/arrow.png"
				);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		gviewer.setMapMode(MapMode.MapSelectMode);
	}
}