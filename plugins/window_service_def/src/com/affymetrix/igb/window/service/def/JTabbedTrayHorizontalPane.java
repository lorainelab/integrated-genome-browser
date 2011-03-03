package com.affymetrix.igb.window.service.def;

import java.awt.Component;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;

public abstract class JTabbedTrayHorizontalPane extends JTabbedTrayPane {
	private static final long serialVersionUID = 1L;

	public JTabbedTrayHorizontalPane(TabState tabState, JComponent _baseComponent, int orientation, int splitOrientation, double _saveDividerProportionalLocation) {
		super(tabState, _baseComponent, orientation, splitOrientation, _saveDividerProportionalLocation);
	}

	/**
	 * @author Santhosh Kumar
	 * http://www.jroller.com/santhosh/date/20050617#adobe_like_tabbedpane_in_swing
	 *
	 */
	protected JTabbedPane createTabbedPane(int tabPlacement) {
		if(!isWindows()){
			 JTabbedPane tabPane = new JTabbedPane();
			 tabPane.setTabPlacement(tabPlacement);
			 return tabPane;
		}

        Object textIconGap = UIManager.get("TabbedPane.textIconGap");
        Insets tabInsets = UIManager.getInsets("TabbedPane.tabInsets");
        UIManager.put("TabbedPane.textIconGap", new Integer(1));
        UIManager.put("TabbedPane.tabInsets", new Insets(tabInsets.left, tabInsets.top, tabInsets.right, tabInsets.bottom));
        JTabbedPane tabPane = new JTabbedPane(tabPlacement) {
			private static final long serialVersionUID = 1L;
		    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
		    	super.insertTab(null, new VerticalTextIcon(" " + title + " ", tabPlacement==JTabbedPane.RIGHT), component, tip, index);
		    }
        };
        UIManager.put("TabbedPane.textIconGap", textIconGap);
        UIManager.put("TabbedPane.tabInsets", tabInsets);
        return tabPane;
	}

	private static boolean isWindows(){
		String os = System.getProperty("os.name");
		if (os != null && os.toLowerCase().contains("windows")) {
			return true;
		}
		return false;
	}
}
