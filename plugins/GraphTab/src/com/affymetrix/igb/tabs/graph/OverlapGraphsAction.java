package com.affymetrix.igb.tabs.graph;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.SimpleTrackStyle;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.tiers.AffyTieredMap;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Map;

/**
 *
 * @author fwang4
 */
public class OverlapGraphsAction extends CombineGraphsAction {
	public OverlapGraphsAction(IGBService igbService) {
		super(igbService, "Overlap");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
	}
	
	@Override
	protected ITrackStyleExtended createComboStyle(IGBService igbService, Map<Color, Integer> colorMap) {
		ITrackStyleExtended combo_style = new SimpleTrackStyle("Joined Graphs", true);
		combo_style.setTrackName("Joined Graphs");
		combo_style.setExpandable(true);
		combo_style.setCollapsed(true);
		combo_style.setForeground(igbService.getDefaultForegroundColor());
		Color background = igbService.getDefaultBackgroundColor();
		int c = -1;
		for (Map.Entry<Color, Integer> color : colorMap.entrySet()) {
			if (color.getValue() > c) {
				background = color.getKey();
				c = color.getValue();
			}
		}
		combo_style.setBackground(background);
		combo_style.setTrackNameSize(igbService.getDefaultTrackSize());
		
		return combo_style;
	}
	
	@Override
	protected void updateDisplay() {
		ThreadUtils.runOnEventQueue(new Runnable() {
	
			public void run() {
				igbService.getSeqMapView().updatePanel(true, true);
				igbService.getSeqMapView().repackTheTiers(true, true);
			}
		});
	}
}
