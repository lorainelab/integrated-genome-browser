package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;

import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.SeqMapView.MapMode;
import java.awt.event.ActionEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 * button action for SeqMapView modes
 */
public class MapModeSelectAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private SeqMapView seqMapView;

	public MapModeSelectAction(SeqMapView seqMapView) {
		super();
		this.seqMapView = seqMapView;
	}

	public void actionPerformed(ActionEvent ae) {
		seqMapView.setMapMode(MapMode.MapSelectMode);
	}

	@Override
	public String getText() {
		return BUNDLE.getString(MapMode.MapSelectMode.name() + "Button");
	}

	@Override
	public String getIconPath() {
		return "images/arrow.gif";
	}

	@Override
	public String getTooltip() {
		return BUNDLE.getString(MapMode.MapSelectMode.name() + "Tip");
	}
}