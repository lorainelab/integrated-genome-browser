package com.affymetrix.igb.window.service.def;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.lorainelab.igb.service.api.IgbTabPanel.TabState;

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
        return tab_pane.getWidth() - tabComponent.getWidth();
    }

    @Override
    protected int getTabWidth(Component tabComponent) {
        return tab_pane.getWidth() - tabComponent.getWidth();
    }

    @Override
    protected int getHideDividerLocation() {
        return 0;
    }

    @Override
    protected boolean isOnTab(Point p) {
        if (tab_pane.getTabCount() < 1) {
            return false;
        }
        int index = tab_pane.getSelectedIndex() < 1 ? 1 : tab_pane.getSelectedIndex();
        return p.getX() < getTabKnobWidth(tab_pane.getComponentAt(index));
    }

    @Override
    protected void setTabComponent() {
        setLeftComponent(tab_pane);
    }

    @Override
    protected void setMinSize() {
        _baseComponent.setMinimumSize(new Dimension(MINIMUM_WIDTH, (int) _baseComponent.getMinimumSize().getHeight()));
        tab_pane.setMinimumSize(new Dimension(MINIMUM_WIDTH, (int) tab_pane.getMinimumSize().getHeight()));
    }

    @Override
    protected Icon getRetractIcon() {
        return LEFT_ICON;
    }

    @Override
    protected Icon getExtendIcon() {
        return RIGHT_ICON;
    }

    protected String getLeftIconString() {
        if (isMac()) {
            return "16x16/actions/up.png";
        }
        return "16x16/actions/left.png";
    }

    protected String getRightIconString() {
        if (isMac()) {
            return "16x16/actions/down.png";
        }
        return "16x16/actions/right.png";
    }
}
