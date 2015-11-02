package com.affymetrix.igb.window.service.def;

import com.affymetrix.igb.swing.MenuUtil;
import com.lorainelab.igb.services.window.tabs.IgbTabPanel.TabState;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

/**
 * JTabbedTrayPane that is on the bottom
 */
public class JTabbedTrayBottomPane extends JTabbedTrayPane {

    private static final long serialVersionUID = 1L;
    private static final Icon UP_ICON = MenuUtil.getIcon("16x16/actions/up.png");
    private static final Icon DOWN_ICON = MenuUtil.getIcon("16x16/actions/down.png");
    private static final double BOTTOM_DIVIDER_PROPORTIONAL_LOCATION = 0.39;

    public JTabbedTrayBottomPane(JComponent _baseComponent) {
        super("Main_bottomPane", TabState.COMPONENT_STATE_BOTTOM_TAB, _baseComponent, JTabbedPane.TOP, JSplitPane.VERTICAL_SPLIT, 1.0 - BOTTOM_DIVIDER_PROPORTIONAL_LOCATION);
        setTopComponent(_baseComponent);
    }

    @Override
    protected int getFullSize() {
        return getHeight();
    }

    private int getTabKnobHeight(Component tabComponent) {
        return tabPane.getHeight() - ((tabComponent == null) ? 0 : tabComponent.getHeight());
    }

    @Override
    protected int getTabWidth(Component tabComponent) {
        return getHeight() - getTabKnobHeight(tabComponent);
    }

    @Override
    protected int getHideDividerLocation() {
        return getHeight();
    }

    @Override
    protected boolean isOnTab(Point p) {
        if (tabPane.getTabCount() < 1) {
            return false;
        }
        int index = tabPane.getSelectedIndex() < 1 ? 1 : tabPane.getSelectedIndex();
        return p.getY() < getTabKnobHeight(tabPane.getComponentAt(index));
    }

    @Override
    protected void setTabComponent() {
        setBottomComponent(tabPane);
    }

    @Override
    protected void setMinSize() {
        tabPane.setMinimumSize(new Dimension((int) tabPane.getMinimumSize().getWidth(), MINIMUM_WIDTH));
        baseComponent.setMinimumSize(new Dimension((int) baseComponent.getMinimumSize().getWidth(), MINIMUM_WIDTH));
    }

    @Override
    protected Icon getRetractIcon() {
        return DOWN_ICON;
    }

    @Override
    protected Icon getExtendIcon() {
        return UP_ICON;
    }
}
