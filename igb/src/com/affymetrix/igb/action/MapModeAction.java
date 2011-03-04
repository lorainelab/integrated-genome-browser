package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.MenuUtil;

import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.SeqMapView.MapMode;
import java.awt.event.ActionEvent;
import java.util.HashMap;

import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author lfrohman
 */
public class MapModeAction extends AbstractAction {
	private static final long serialVersionUID = 1l;
	private static final HashMap<MapMode, String> ICONS = new HashMap<MapMode, String>();
	static {
		ICONS.put(MapMode.MapScrollMode, "Replace");
		ICONS.put(MapMode.MapSelectMode, "AlignJustifyHorizontal");
		ICONS.put(MapMode.MapZoomMode, "Zoom");
	}
	private SeqMapView seqMapView;
	private MapMode mapMode;

	public MapModeAction(SeqMapView seqMapView, MapMode mapMode) {
		super(BUNDLE.getString(mapMode.name() + "Button"), MenuUtil.getIcon("toolbarButtonGraphics/general/" + ICONS.get(mapMode) + "16.gif"));
		this.seqMapView = seqMapView;
		this.mapMode = mapMode;
		this.putValue(SHORT_DESCRIPTION, BUNDLE.getString(mapMode.name() + "Tip"));
	}

	public void actionPerformed(ActionEvent ae) {
		seqMapView.setMapMode(mapMode);
	}
}