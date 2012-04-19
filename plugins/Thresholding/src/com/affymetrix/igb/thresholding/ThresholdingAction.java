package com.affymetrix.igb.thresholding;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ResourceBundle;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.AbstractGraphGlyph;

public class ThresholdingAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("thresholding");
//	private final IGBService igbService;
	private GraphVisibleBoundsSetter vis_bounds_setter;
	private GraphScoreThreshSetter score_thresh_adjuster;

	public ThresholdingAction(IGBService igbService) {
		super(BUNDLE.getString("thresholding"), null, null, KeyEvent.VK_UNDEFINED, null, true);
//		this.igbService = igbService;
		vis_bounds_setter = new GraphVisibleBoundsSetter(igbService.getSeqMap());
		score_thresh_adjuster = new GraphScoreThreshSetter(igbService, vis_bounds_setter);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		showGraphScoreThreshSetter();
	}

	private void showGraphScoreThreshSetter() {
		score_thresh_adjuster.showFrame();
	}

	public void setGraphs(List<AbstractGraphGlyph> glyphs) {
		vis_bounds_setter.setGraphs(glyphs);
		score_thresh_adjuster.setGraphs(glyphs);
	}
}
