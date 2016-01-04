package com.affymetrix.igb.window.service.def;

import org.lorainelab.igb.igb.frame.api.FrameManagerService;
import org.lorainelab.igb.igb.services.window.tabs.IgbTabPanel.TabState;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JTabbedTrayPane that is on the right
 */
public class JTabbedTrayRightPane extends JTabbedTrayHorizontalPane {

    private static final Logger logger = LoggerFactory.getLogger(JTabbedTrayRightPane.class);

    private static final long serialVersionUID = 1L;
    private static final double RIGHT_DIVIDER_PROPORTIONAL_LOCATION = 0.80;
    private FrameManagerService frameManagerService;

    public JTabbedTrayRightPane(JComponent _baseComponent) {
        super("Main_rightPane", TabState.COMPONENT_STATE_RIGHT_TAB, _baseComponent, JTabbedPane.RIGHT, JSplitPane.HORIZONTAL_SPLIT, RIGHT_DIVIDER_PROPORTIONAL_LOCATION);
        setLeftComponent(_baseComponent);
    }

    JTabbedTrayRightPane(FrameManagerService frameManagerService, JTabbedTrayPane left_pane) {
        this(left_pane);
        this.frameManagerService = frameManagerService;
    }

    @Override
    protected int getFullSize() {
        return frameManagerService.getIgbMainFrame().getBounds().width;
    }

    private int getTabKnobWidth(Component tabComponent) {
        return tabPane.getWidth() - tabComponent.getWidth();
    }

    @Override
    protected int getTabWidth(Component tabComponent) {
        return getWidth() - getTabKnobWidth(tabComponent);
    }

    @Override
    protected int getHideDividerLocation() {
        return getWidth();
    }

    @Override
    protected boolean isOnTab(Point p) {
        if (tabPane.getTabCount() < 1) {
            return false;
        }
        int index = tabPane.getSelectedIndex() < 1 ? 1 : tabPane.getSelectedIndex();
        return p.getX() > tabPane.getComponentAt(index).getWidth();
    }

    @Override
    protected void setTabComponent() {
        setRightComponent(tabPane);
//		tab_pane.setMinimumSize(new Dimension(MINIMUM_WIDTH, (int)tab_pane.getMinimumSize().getHeight()));
    }

    @Override
    protected void setMinSize() {
        baseComponent.setMinimumSize(new Dimension(MINIMUM_WIDTH, (int) baseComponent.getMinimumSize().getHeight()));
        tabPane.setMinimumSize(new Dimension(MINIMUM_WIDTH, (int) tabPane.getMinimumSize().getHeight()));
    }

    @Override
    protected Icon getRetractIcon() {
        return RIGHT_ICON;
    }

    @Override
    protected Icon getExtendIcon() {
        return LEFT_ICON;
    }

    @Override
    protected String getLeftIconString() {
        if (isMac()) {
            return "16x16/actions/down.png";
        }
        return "16x16/actions/left.png";
    }

    @Override
    protected String getRightIconString() {
        if (isMac()) {
            return "16x16/actions/up.png";
        }
        return "16x16/actions/right.png";
    }
}
