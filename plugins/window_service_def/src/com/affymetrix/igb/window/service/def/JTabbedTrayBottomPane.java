package com.affymetrix.igb.window.service.def;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.TabState;

public class JTabbedTrayBottomPane extends JTabbedTrayPane {
	private static final long serialVersionUID = 1L;
	private static final double BOTTOM_DIVIDER_PROPORTIONAL_LOCATION = 0.30;

	public JTabbedTrayBottomPane(JComponent _baseComponent) {
		super(TabState.COMPONENT_STATE_BOTTOM_TAB, _baseComponent, JTabbedPane.BOTTOM,  JSplitPane.VERTICAL_SPLIT, 1.0 - BOTTOM_DIVIDER_PROPORTIONAL_LOCATION);
		setTopComponent(_baseComponent);
	}

	@Override
	protected int getFullSize() {
		return getHeight();
	}

	@Override
	protected int getRetractDividerLocation() {
		int index = tab_pane.getSelectedIndex() < 0 ? 0 : tab_pane.getSelectedIndex();
		return getHeight() - (tab_pane.getHeight() - tab_pane.getComponentAt(index).getHeight());
	}

	@Override
	protected int getHideDividerLocation() {
		return getHeight();
	}

	@Override
	protected void setTabComponent() {
		setBottomComponent(tab_pane);
	}

	@Override
	public void addTab(final IGBTabPanel plugin, boolean setFocus) {
		plugin.setPortrait(false);
		super.addTab(plugin, setFocus);
	}
}
