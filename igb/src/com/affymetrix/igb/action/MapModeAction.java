package com.affymetrix.igb.action;

import javax.swing.ImageIcon;
import com.affymetrix.igb.util.IGBUtils;

import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.SeqMapView.MapMode;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author lfrohman
 */
public class MapModeAction extends AbstractAction {
	private static final long serialVersionUID = 1l;
	private SeqMapView seqMapView;
	private MapMode mapMode;

	public MapModeAction(SeqMapView seqMapView, MapMode mapMode, String iconName) {
		super(BUNDLE.getString(mapMode.name() + "Button"));
		ImageIcon icon = IGBUtils.getIcon(iconName);
		if(icon != null){
			this.putValue(AbstractAction.SMALL_ICON, icon);
		}
		this.seqMapView = seqMapView;
		this.mapMode = mapMode;
		this.putValue(SHORT_DESCRIPTION, BUNDLE.getString(mapMode.name() + "Tip"));
	}

	public void actionPerformed(ActionEvent ae) {
		seqMapView.setMapMode(mapMode);
	}
}