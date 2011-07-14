package com.affymetrix.igb.window.service.def;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;

/**
 * JTabbedTrayPane that is on the right
 */
public class JTabbedTrayRightPane extends JTabbedTrayHorizontalPane {
	private static final long serialVersionUID = 1L;
	private static final double RIGHT_DIVIDER_PROPORTIONAL_LOCATION = 0.20;

	public JTabbedTrayRightPane(JComponent _baseComponent) {
		super("Main.rightPane", TabState.COMPONENT_STATE_RIGHT_TAB, _baseComponent, JTabbedPane.RIGHT, JSplitPane.HORIZONTAL_SPLIT, 1.0 - RIGHT_DIVIDER_PROPORTIONAL_LOCATION);
		setLeftComponent(_baseComponent);
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
		return getWidth() - getTabKnobWidth(tabComponent);
	}

	@Override
	protected int getHideDividerLocation() {
		return getWidth();
	}

	@Override
	protected boolean isOnTab(Point p) {
		if (tab_pane.getTabCount() == 0) {
			return false;
		}
		int index = tab_pane.getSelectedIndex() < 0 ? 0 : tab_pane.getSelectedIndex();
		return p.getX() > tab_pane.getComponentAt(index).getWidth();
	}

	@Override
	protected void setTabComponent() {
		setRightComponent(tab_pane);
	}
}
