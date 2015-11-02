package com.affymetrix.igb.window.service.def;

import com.lorainelab.igb.services.window.tabs.IgbTabPanel.TabState;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

/**
 * JTabbedTrayPane that is on the left
 */
public class JTabbedTrayLeftPane extends JTabbedTrayHorizontalPane {

    private static final long serialVersionUID = 1L;
    private static final double LEFT_DIVIDER_PROPORTIONAL_LOCATION = 0.20;

    public JTabbedTrayLeftPane(JComponent _baseComponent) {
        super("Main_leftPane", TabState.COMPONENT_STATE_LEFT_TAB, _baseComponent, JTabbedPane.LEFT, JSplitPane.HORIZONTAL_SPLIT, LEFT_DIVIDER_PROPORTIONAL_LOCATION);
        setRightComponent(_baseComponent);
        setDividerLocation(0);
    }

    @Override
    protected int getFullSize() {
        return getWidth();
    }

    private int getTabKnobWidth(Component tabComponent) {
        return tabPane.getWidth() - tabComponent.getWidth();
    }

    @Override
    protected int getTabWidth(Component tabComponent) {
        return tabPane.getWidth() - tabComponent.getWidth();
    }

    @Override
    protected int getHideDividerLocation() {
        return 0;
    }

    @Override
    protected boolean isOnTab(Point p) {
        if (tabPane.getTabCount() < 1) {
            return false;
        }
        int index = tabPane.getSelectedIndex() < 1 ? 1 : tabPane.getSelectedIndex();
        return p.getX() < getTabKnobWidth(tabPane.getComponentAt(index));
    }

    @Override
    protected void setTabComponent() {
        setLeftComponent(tabPane);
    }

    @Override
    protected void setMinSize() {
        baseComponent.setMinimumSize(new Dimension(MINIMUM_WIDTH, (int) baseComponent.getMinimumSize().getHeight()));
        tabPane.setMinimumSize(new Dimension(MINIMUM_WIDTH, (int) tabPane.getMinimumSize().getHeight()));
    }

    @Override
    protected Icon getRetractIcon() {
        return LEFT_ICON;
    }

    @Override
    protected Icon getExtendIcon() {
        return RIGHT_ICON;
    }

    @Override
    protected String getLeftIconString() {
        if (isMac()) {
            return "16x16/actions/up.png";
        }
        return "16x16/actions/left.png";
    }

    @Override
    protected String getRightIconString() {
        if (isMac()) {
            return "16x16/actions/down.png";
        }
        return "16x16/actions/right.png";
    }
}
