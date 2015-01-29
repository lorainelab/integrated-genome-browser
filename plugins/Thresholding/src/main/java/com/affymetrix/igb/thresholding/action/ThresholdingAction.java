package com.affymetrix.igb.thresholding.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ResourceBundle;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.service.api.IgbService;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.thresholding.GraphScoreThreshSetter;

public class ThresholdingAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("thresholding");
//	private final IgbService igbService;
    private GraphScoreThreshSetter score_thresh_adjuster;
    private static ThresholdingAction ACTION;

    public static void createAction(IgbService igbService) {
        ACTION = new ThresholdingAction(igbService);
    }

    public static ThresholdingAction getAction() {
        return ACTION;
    }

    private ThresholdingAction(IgbService igbService) {
        super(BUNDLE.getString("thresholding"), null, "16x16/actions/blank_placeholder.png", null, KeyEvent.VK_UNDEFINED, null, true);
//		this.igbService = igbService;
        score_thresh_adjuster = new GraphScoreThreshSetter(igbService);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        setGraphs(Selections.graphGlyphs);
        showGraphScoreThreshSetter();
    }

    private void showGraphScoreThreshSetter() {
        score_thresh_adjuster.showFrame();
    }

    public void setGraphs(List<GraphGlyph> glyphs) {
        score_thresh_adjuster.setGraphs(glyphs);
    }
}
