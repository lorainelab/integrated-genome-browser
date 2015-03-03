package com.affymetrix.igb.thresholding.action;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.genoviz.extensions.GraphGlyph;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.thresholding.GraphScoreThreshSetter;
import com.affymetrix.igb.thresholding.SelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JMenuItem;

@Component(name = ThresholdingAction.COMPONENT_NAME, immediate = true, provide = {GenericAction.class, IgbMenuItemProvider.class}, properties = "name=" + ThresholdingAction.COMPONENT_NAME)
public class ThresholdingAction extends GenericAction implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "ThresholdingAction";
    private static final long serialVersionUID = 1L;
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("thresholding");
    private IgbService igbService;
    private GraphScoreThreshSetter score_thresh_adjuster;
    private JRPMenuItem thresholdingMenuItem;
    private SelectionListener selectionListener;

    public ThresholdingAction() {
        super(BUNDLE.getString("thresholding"), null, "16x16/actions/blank_placeholder.png", null, KeyEvent.VK_UNDEFINED, null, true);
//		this.igbService = igbService;

    }

    @Activate
    public void activate() {
        score_thresh_adjuster = new GraphScoreThreshSetter(igbService);
        thresholdingMenuItem = new JRPMenuItem("Thresholding_thresholding", this);
        thresholdingMenuItem.setEnabled(false);
        selectionListener = new SelectionListener(thresholdingMenuItem);
        Selections.addRefreshSelectionListener(selectionListener);
    }

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
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

    @Override
    public String getParentMenuName() {
        return "tools";
    }

    @Override
    public JRPMenuItem getMenuItem() {
        return thresholdingMenuItem;
    }

    @Override
    public int getMenuItemWeight() {
        return -1;
    }

}
