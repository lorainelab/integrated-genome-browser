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
        return tab_pane.getHeight() - ((tabComponent == null) ? 0 : tabComponent.getHeight());
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
        if (tab_pane.getTabCount() < 1) {
            return false;
        }
        int index = tab_pane.getSelectedIndex() < 1 ? 1 : tab_pane.getSelectedIndex();
        return p.getY() < getTabKnobHeight(tab_pane.getComponentAt(index));
    }

    @Override
    protected void setTabComponent() {
        setBottomComponent(tab_pane);
    }

    @Override
    protected void setMinSize() {
        tab_pane.setMinimumSize(new Dimension((int) tab_pane.getMinimumSize().getWidth(), MINIMUM_WIDTH));
        _baseComponent.setMinimumSize(new Dimension((int) _baseComponent.getMinimumSize().getWidth(), MINIMUM_WIDTH));
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
