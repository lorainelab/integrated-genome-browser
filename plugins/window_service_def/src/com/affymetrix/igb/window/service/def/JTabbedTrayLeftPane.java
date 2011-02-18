package com.affymetrix.igb.window.service.def;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

public class JTabbedTrayLeftPane extends JTabbedTrayHorizontalPane {
	private static final long serialVersionUID = 1L;
	private static final double LEFT_DIVIDER_PROPORTIONAL_LOCATION = 0.30;

	public JTabbedTrayLeftPane(JComponent _baseComponent) {
		super(_baseComponent, JTabbedPane.LEFT, JSplitPane.HORIZONTAL_SPLIT, LEFT_DIVIDER_PROPORTIONAL_LOCATION);
		setLeftComponent(tab_pane);
		setRightComponent(_baseComponent);
		setDividerLocation(0);
	}

	@Override
	protected int getFullSize() {
		return getWidth();
	}

	@Override
	protected int getRetractDividerLocation() {
		return tab_pane.getWidth() - tab_pane.getComponentAt(tab_pane.getSelectedIndex()).getWidth();
	}

	@Override
	protected int getHideDividerLocation() {
		return 0;
	}
}
