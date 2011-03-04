package com.affymetrix.igb.action;

import java.awt.Image;
import javax.swing.ImageIcon;
import com.affymetrix.igb.util.IGBUtils;

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
		ICONS.put(MapMode.MapSelectMode, "arrow.gif");
		ICONS.put(MapMode.MapScrollMode, "open_hand.gif");
		ICONS.put(MapMode.MapZoomMode, "close_hand.gif");
	}
	private SeqMapView seqMapView;
	private MapMode mapMode;

	public MapModeAction(SeqMapView seqMapView, MapMode mapMode) {
		super(BUNDLE.getString(mapMode.name() + "Button"));
		Image icon = IGBUtils.getIcon(ICONS.get(mapMode));
		if(icon != null){
			this.putValue(AbstractAction.SMALL_ICON, new ImageIcon(icon));
		}
		this.seqMapView = seqMapView;
		this.mapMode = mapMode;
		this.putValue(SHORT_DESCRIPTION, BUNDLE.getString(mapMode.name() + "Tip"));
	}

	public void actionPerformed(ActionEvent ae) {
		seqMapView.setMapMode(mapMode);
	}
}