package com.affymetrix.igb.thresholding.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ResourceBundle;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.thresholding.GraphScoreThreshSetter;

public class ThresholdingAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("thresholding");
//	private final IGBService igbService;
	private GraphScoreThreshSetter score_thresh_adjuster;

	public ThresholdingAction(IGBService igbService) {
		super(BUNDLE.getString("thresholding"), null, null, KeyEvent.VK_UNDEFINED, null, true);
//		this.igbService = igbService;
		score_thresh_adjuster = new GraphScoreThreshSetter(igbService);
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
		score_thresh_adjuster.setGraphs(glyphs);
	}
}
