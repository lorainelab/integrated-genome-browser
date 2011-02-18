package com.affymetrix.igb.window.service.def;

import java.awt.Component;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

public abstract class JTabbedTrayHorizontalPane extends JTabbedTrayPane {
	private static final long serialVersionUID = 1L;

	public JTabbedTrayHorizontalPane(JComponent _baseComponent, int orientation, int splitOrientation, double _saveDividerProportionalLocation) {
		super(_baseComponent, orientation, splitOrientation, _saveDividerProportionalLocation);
	}

	/**
	 * @author Santhosh Kumar
	 * http://www.jroller.com/santhosh/date/20050617#adobe_like_tabbedpane_in_swing
	 *
	 */
	protected JTabbedPane createTabbedPane(int tabPlacement) {
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

	protected JTabbedPane createTabbedPane2(int tabPlacement) {
        final JTabbedPane tabPane = new JTabbedPane(tabPlacement) {
			private static final long serialVersionUID = 1L;
		    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
		    	super.insertTab(null, new VTextIcon(this, title, tabPlacement==JTabbedPane.RIGHT ? VTextIcon.ROTATE_RIGHT : VTextIcon.ROTATE_LEFT), component, tip, index);
		    }
        };
        return tabPane;
	}
}
