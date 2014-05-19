package com.affymetrix.igb.window.service.def;

import java.awt.Component;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.affymetrix.igb.swing.MenuUtil;
import com.affymetrix.igb.swing.JRPTabbedPane;
import com.affymetrix.igb.osgi.service.IGBTabPanel.TabState;

/**
 * JTabbedTrayPane that is on the left or right
 */
public abstract class JTabbedTrayHorizontalPane extends JTabbedTrayPane {

    private static final long serialVersionUID = 1L;
    protected final Icon LEFT_ICON = MenuUtil.getIcon(getLeftIconString());
    protected final Icon RIGHT_ICON = MenuUtil.getIcon(getRightIconString());

    public JTabbedTrayHorizontalPane(String id, TabState tabState, JComponent _baseComponent, int orientation, int splitOrientation, double _saveDividerProportionalLocation) {
        super(id, tabState, _baseComponent, orientation, splitOrientation, _saveDividerProportionalLocation);
    }

    /**
     * @author Santhosh Kumar
     * http://www.jroller.com/santhosh/date/20050617#adobe_like_tabbedpane_in_swing
     *
     */
    @Override
    protected JRPTabbedPane createTabbedPane(String id, int tabPlacement) {
        if (isMac()) {
            return new JRPTabbedPane(id, tabPlacement);
        }

        Object textIconGap = UIManager.get("TabbedPane.textIconGap");
        Insets tabInsets = UIManager.getInsets("TabbedPane.tabInsets");
        if (tabInsets == null) {
            tabInsets = new Insets(0, 0, 0, 0);
        }
        UIManager.put("TabbedPane.textIconGap", new Integer(1));
        UIManager.put("TabbedPane.tabInsets", new Insets(tabInsets.left, tabInsets.top, tabInsets.right, tabInsets.bottom));
        JRPTabbedPane tabPane = new JRPTabbedPane(id, tabPlacement) {
            private static final long serialVersionUID = 1L;

            @Override
            public void insertTab(String title, Icon icon, Component component, String tip, int index) {
                super.insertTab(null, new VerticalTextIcon(" " + title + " ", tabPlacement == JTabbedPane.RIGHT), component, tip, index);
            }
        };
        UIManager.put("TabbedPane.textIconGap", textIconGap);
        UIManager.put("TabbedPane.tabInsets", tabInsets);
        return tabPane;
    }

    /**
     * determines if the OS is windows
     *
     * @return true if the OS is windows, false for MacOS, Linux, etc.
     */
    protected static boolean isMac() {
        String os = System.getProperty("os.name");
        if (os != null && "Mac OS X".equals(os)) {
            return true;
        }
        return false;
    }

    protected abstract String getLeftIconString();

    protected abstract String getRightIconString();

}
