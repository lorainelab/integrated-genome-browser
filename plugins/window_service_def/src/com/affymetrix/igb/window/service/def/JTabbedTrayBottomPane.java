package com.affymetrix.igb.window.service.def;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;

/**
 * JTabbedTrayPane that is on the bottom
 */
public class JTabbedTrayBottomPane extends JTabbedTrayPane {
	private static final long serialVersionUID = 1L;
	private static final double BOTTOM_DIVIDER_PROPORTIONAL_LOCATION = 0.30;

	public JTabbedTrayBottomPane(JComponent _baseComponent) {
		super(TabState.COMPONENT_STATE_BOTTOM_TAB, _baseComponent, JTabbedPane.TOP,  JSplitPane.VERTICAL_SPLIT, 1.0 - BOTTOM_DIVIDER_PROPORTIONAL_LOCATION);
		setTopComponent(_baseComponent);
	}

	@Override
	protected int getFullSize() {
		return getHeight();
	}

	private int getTabKnobHeight(Component tabComponent) {
		return tab_pane.getHeight() - tabComponent.getHeight();
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
		if (tab_pane.getTabCount() == 0) {
			return false;
		}
		int index = tab_pane.getSelectedIndex() < 0 ? 0 : tab_pane.getSelectedIndex();
		return p.getY() < getTabKnobHeight(tab_pane.getComponentAt(index));
	}

	@Override
	protected void setTabComponent() {
		setBottomComponent(tab_pane);
	}
}
