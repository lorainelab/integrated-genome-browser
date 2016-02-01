package com.affymetrix.igb.thresholding.action;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.thresholding.GraphScoreThreshSetter;
import com.affymetrix.igb.thresholding.SelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import org.lorainelab.igb.genoviz.extensions.glyph.GraphGlyph;
import org.lorainelab.igb.menu.api.model.MenuBarParentMenu;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.services.IgbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lorainelab.igb.menu.api.MenuBarEntryProvider;

@Component(name = ThresholdingAction.COMPONENT_NAME, immediate = true, provide = {GenericAction.class, MenuBarEntryProvider.class}, properties = "name=" + ThresholdingAction.COMPONENT_NAME)
public class ThresholdingAction extends GenericAction implements MenuBarEntryProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ThresholdingAction.class);
    public static final String COMPONENT_NAME = "ThresholdingAction";
    private static final long serialVersionUID = 1L;
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("thresholding");
    private IgbService igbService;
    private GraphScoreThreshSetter score_thresh_adjuster;
    private JRPMenuItem thresholdingMenuItem;
    private SelectionListener selectionListener;
    private static final String THRESHOLDING_MENU_ITEM_ICION = "blank_placeholder.png";
    private static int MENU_ITEM_WEIGHT = 30;

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
    public Optional<List<MenuItem>> getMenuItems() {
        MenuItem menuItem = new MenuItem(BUNDLE.getString("thresholding"), (Void t) -> {
            actionPerformed(null);
            return t;
        });
        try (InputStream resourceAsStream = ThresholdingAction.class.getClassLoader().getResourceAsStream(THRESHOLDING_MENU_ITEM_ICION)) {
            menuItem.setMenuIcon(new MenuIcon(resourceAsStream));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        menuItem.setWeight(MENU_ITEM_WEIGHT);
        return Optional.of(Arrays.asList(menuItem));
    }

    @Override
    public MenuBarParentMenu getMenuExtensionParent() {
        return MenuBarParentMenu.TOOLS;
    }

}
