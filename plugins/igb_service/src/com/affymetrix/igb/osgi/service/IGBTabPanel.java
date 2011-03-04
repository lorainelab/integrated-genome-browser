package com.affymetrix.igb.osgi.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public abstract class IGBTabPanel extends JPanel implements Comparable<IGBTabPanel> {
	private static final long serialVersionUID = 1L;

	public enum TabState {
		COMPONENT_STATE_LEFT_TAB(true, true),
		COMPONENT_STATE_RIGHT_TAB(true, true),
		COMPONENT_STATE_BOTTOM_TAB(true, false),
		COMPONENT_STATE_WINDOW(false, false),
		COMPONENT_STATE_HIDDEN(false, false);

		private final boolean tab;
		private final boolean portrait;

		TabState(boolean tab, boolean portrait) {
			this.tab = tab;
			this.portrait = portrait;
		}

		public boolean isTab() {
			return tab;
		}

		public static TabState getDefaultTabState() {
			return COMPONENT_STATE_BOTTOM_TAB;
		}

		public List<TabState> getCompatibleTabStates() {
			List<TabState> compatibleTabStates = new ArrayList<TabState>();
			for (TabState tabState : TabState.values()) {
				if (portrait == tabState.portrait || !isTab() || !tabState.isTab()) {
					compatibleTabStates.add(tabState);
				}
			}
			return compatibleTabStates;
		}
	}

	protected final IGBService igbService;
	private final String displayName;
	private final String title;
	private final boolean main;
	private final int position;
	private JFrame frame;

	public IGBTabPanel(IGBService igbService, String displayName, String title, boolean main) {
		this(igbService, displayName, title, main, Integer.MAX_VALUE - 1);
	}
	
	protected IGBTabPanel(IGBService igbService, String displayName, String title, boolean main, int position) {
		super();
		this.igbService = igbService;
		this.displayName = displayName;
		this.title = title;
		this.main = main;
		this.position = position;
	}

	public String getName() {
		return getClass().getName();
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getTitle() {
		return title;
	}

	public boolean isMain() {
		return main;
	}

	public boolean isFocus() {
		return false;
	}

	public TabState getDefaultState() {
		return TabState.COMPONENT_STATE_BOTTOM_TAB;
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setFrame(JFrame frame) {
		this.frame = frame;
	}

	@Override
	public String toString() {
		return "IGBTabPanel: " + "displayName = " + displayName + ", class = " + this.getClass().getName();
	}

	/** Returns the icon stored in the jar file.
	 *  It is expected to be at com.affymetrix.igb.igb.gif.
	 *  @return null if the image file is not found or can't be opened.
	 */
	public Icon getIcon() {
		ImageIcon icon = null;
		try {
			URL url = IGBTabPanel.class.getResource("igb.gif");
			if (url != null) {
				icon = new ImageIcon(url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// It isn't a big deal if we can't find the icon, just return null
		}
		return icon;
	}

	@Override
	public int compareTo(IGBTabPanel o) {
		int ret = Integer.valueOf(position).compareTo(o.position);

		if(ret != 0)
			return ret;

		return this.getDisplayName().compareTo(o.getDisplayName());
	}
}
