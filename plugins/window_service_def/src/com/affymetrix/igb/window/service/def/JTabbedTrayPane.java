package com.affymetrix.igb.window.service.def;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.affymetrix.igb.osgi.service.IGBTabPanel;

public abstract class JTabbedTrayPane extends JSplitPane implements TabHolder {
	private static final long serialVersionUID = 1L;
	private static final int DIVIDER_SIZE = 8;
	protected enum TrayState {
		HIDDEN,
		RETRACTED,
		EXTENDED;
	}
	protected double saveDividerProportionalLocation; // saved as percent, but implemented as pixels, due to problems with Swing
	protected final JTabbedPane tab_pane;
	protected TrayState trayState;
	
	protected abstract int getFullSize();
	private int getExtendDividerLocation() {
		return (int)Math.round(getFullSize() * saveDividerProportionalLocation);
	}
	protected abstract int getRetractDividerLocation();
	protected abstract int getHideDividerLocation();
	private void saveDividerLocation() {
		saveDividerProportionalLocation = (double)getDividerLocation() / (double)getFullSize();
	}

	public JTabbedTrayPane(JComponent _baseComponent, int orientation, int splitOrientation, double _saveDividerProportionalLocation) {
		super(splitOrientation);
		trayState = TrayState.HIDDEN;
		saveDividerProportionalLocation = _saveDividerProportionalLocation;
		tab_pane = createTabbedPane(orientation);
		
		setOneTouchExpandable(true);
		setDividerSize(0);

		// Using JTabbedPane.SCROLL_TAB_LAYOUT makes it impossible to add a
		// pop-up menu (or any other mouse listener) on the tab handles.
		// (A pop-up with "Open tab in a new window" would be nice.)
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4465870
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4499556
		tab_pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tab_pane.setMinimumSize(new Dimension(0, 0));
		MouseListener[] mouseListeners = tab_pane.getMouseListeners();
		if (mouseListeners == null || mouseListeners.length != 1) {
			System.out.println("Internal error in " + this.getClass().getName() + " constructor, mouseListeners");
		}
		else {
			final MouseListener originalMouseListener = mouseListeners[0];
			tab_pane.removeMouseListener(originalMouseListener);
			tab_pane.addMouseListener(
				new MouseListener() {
					@Override
					public void mouseReleased(MouseEvent e) {
						originalMouseListener.mouseReleased(e);
					}
					@Override
					public void mousePressed(MouseEvent e) {
		               	int beforeIndex = tab_pane.getSelectedIndex();
						originalMouseListener.mousePressed(e);
//						if (getBottomComponent() != null) { // tabbed pane not in a separate window
							if (trayState == TrayState.EXTENDED) {
				               	int afterIndex = tab_pane.getSelectedIndex();
				               	if (beforeIndex == afterIndex) {
									retractTray();
				               	}
							}
							else if (trayState == TrayState.RETRACTED) {
				               	extendTray();
							}
//						}
					}
					@Override
					public void mouseExited(MouseEvent e) {
						originalMouseListener.mouseExited(e);
					}
					@Override
					public void mouseEntered(MouseEvent e) {
						originalMouseListener.mouseEntered(e);
					}
					@Override
					public void mouseClicked(MouseEvent e) {
						originalMouseListener.mouseClicked(e);
					}
				}
			);
		}
	}

	private void hideTray() {
		if (trayState == TrayState.EXTENDED) {
			saveDividerLocation();
		}
		setDividerLocation(getHideDividerLocation());
		setDividerSize(0);
		trayState = TrayState.HIDDEN;
	}

	private void extendTray() {
		if (tab_pane.getComponentCount() == 0) {
			hideTray();
			return;
		}
		setDividerLocation(getExtendDividerLocation());
		setDividerSize(DIVIDER_SIZE);
		trayState = TrayState.EXTENDED;
	}

	private void retractTray() {
		if (tab_pane.getComponentCount() == 0) {
			hideTray();
			return;
		}
		if (trayState == TrayState.EXTENDED) {
			saveDividerLocation();
		}
		setDividerLocation(getRetractDividerLocation());
		setDividerSize(0);
		trayState = TrayState.RETRACTED;
	}

	private void invokeTrayState(TrayState newState) {
		switch (newState) {
		case HIDDEN:
			hideTray();
			break;
		case RETRACTED:
			retractTray();
			break;
		case EXTENDED:
			extendTray();
			break;
		}
	}
/*
	public JComponent cutTabs() {
		JComponent baseComponent = (JComponent)getBottomComponent();
		remove(baseComponent);
		validate();
		return baseComponent;
	}

	public void restoreTabs() {
		setBottomComponent(tab_pane);
		setDividerLocation(defaultDividerProportionalLocation);
	}

	public JTabbedPane getTabbedPane() {
		return tab_pane;
	}
*/
	@Override
	public void addTab(final IGBTabPanel plugin, boolean setFocus) {
		int index = 0;
		while (index < tab_pane.getTabCount() && plugin.compareTo((IGBTabPanel)tab_pane.getComponentAt(index)) > 0) {
			index++;
		}
		tab_pane.insertTab(plugin.getTitle(), plugin.getIcon(), plugin, plugin.getToolTipText(), index);
		tab_pane.validate();
		if (setFocus) {
			tab_pane.setSelectedIndex(index);
		}
		if (tab_pane.getTabCount() > 0 && trayState == TrayState.HIDDEN) {
			invokeTrayState(TrayState.EXTENDED);
		}
	}

	@Override
	public void removeTab(final IGBTabPanel plugin) {
		String name = plugin.getName();
		for (int i = 0; i < tab_pane.getTabCount(); i++) {
			if (name.equals(((IGBTabPanel)tab_pane.getComponentAt(i)).getName())) {
				tab_pane.remove(i);
				tab_pane.validate();
			}
		}
		if (tab_pane.getTabCount() == 0) {
			hideTray();
		}
	}

	@Override
	public void init() {
		hideTray();
	}

	public Set<IGBTabPanel> getPlugins() {
		Set<IGBTabPanel> plugins = new HashSet<IGBTabPanel>();
		for (int i = 0; i < tab_pane.getTabCount(); i++) {
			plugins.add((IGBTabPanel)tab_pane.getComponentAt(i));
		}
		return plugins;
	}

	protected JTabbedPane createTabbedPane(int tabPlacement){
		return new JTabbedPane(tabPlacement);
	}
	
	@Override
	public void resize() {
		switch (trayState) {
		case HIDDEN:
			setDividerLocation(getHideDividerLocation());
			break;
		case RETRACTED:
			setDividerLocation(getRetractDividerLocation());
			break;
		case EXTENDED:
			setDividerLocation(getExtendDividerLocation());
			break;
		}
	}
}
