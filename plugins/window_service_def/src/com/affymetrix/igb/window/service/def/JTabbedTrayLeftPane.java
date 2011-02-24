package com.affymetrix.igb.window.service.def;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.affymetrix.igb.osgi.service.TabState;

public class JTabbedTrayLeftPane extends JTabbedTrayHorizontalPane {
	private static final long serialVersionUID = 1L;
	private static final double LEFT_DIVIDER_PROPORTIONAL_LOCATION = 0.30;

	public JTabbedTrayLeftPane(JComponent _baseComponent) {
		super(TabState.COMPONENT_STATE_LEFT_TAB, _baseComponent, JTabbedPane.LEFT, JSplitPane.HORIZONTAL_SPLIT, LEFT_DIVIDER_PROPORTIONAL_LOCATION);
		setRightComponent(_baseComponent);
		setDividerLocation(0);
	}

	@Override
	protected int getFullSize() {
		return getWidth();
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
	protected void setTabComponent() {
		setLeftComponent(tab_pane);
	}
}
