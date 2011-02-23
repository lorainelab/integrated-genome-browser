package com.affymetrix.igb.window.service.def;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.affymetrix.igb.osgi.service.TabState;

public class JTabbedTrayRightPane extends JTabbedTrayHorizontalPane {
	private static final long serialVersionUID = 1L;
	private static final double RIGHT_DIVIDER_PROPORTIONAL_LOCATION = 0.30;

	public JTabbedTrayRightPane(JComponent _baseComponent) {
		super(TabState.COMPONENT_STATE_RIGHT_TAB, _baseComponent, JTabbedPane.RIGHT, JSplitPane.HORIZONTAL_SPLIT, 1.0 - RIGHT_DIVIDER_PROPORTIONAL_LOCATION);
		setLeftComponent(_baseComponent);
	}

	@Override
	protected int getFullSize() {
		return getWidth();
	}

	@Override
	protected int getRetractDividerLocation() {
		int index = tab_pane.getSelectedIndex() < 0 ? 0 : tab_pane.getSelectedIndex();
		return getWidth() - (tab_pane.getWidth() - tab_pane.getComponentAt(index).getWidth());
	}

	@Override
	protected int getHideDividerLocation() {
		return getWidth();
	}

	@Override
	protected void setTabComponent() {
		setRightComponent(tab_pane);
	}
}
