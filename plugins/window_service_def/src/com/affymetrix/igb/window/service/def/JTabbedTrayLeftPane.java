package com.affymetrix.igb.window.service.def;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;

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
		if (tab_pane.getTabCount() == 0) {
			return false;
		}
		int index = tab_pane.getSelectedIndex() < 0 ? 0 : tab_pane.getSelectedIndex();
		return p.getX() < getTabKnobWidth(tab_pane.getComponentAt(index));
	}

	@Override
	protected void setTabComponent() {
		setLeftComponent(tab_pane);
	}
}
