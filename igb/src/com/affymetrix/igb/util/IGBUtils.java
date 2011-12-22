package com.affymetrix.igb.util;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.InfoLabel;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPCheckBoxMenuItem;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

/**
 *
 * @author hiralv
 */
public class IGBUtils {

	public static JPanel setInfoLabel(JComponent component, String tooltip){
		JLabel infolabel = new InfoLabel(CommonUtils.getInstance().getIcon("images/info.png"));
		infolabel.setToolTipText(tooltip);
		
		JPanel pane = new JPanel();
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		pane.add(component, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.PAGE_START;
		c.insets = new Insets(0,0,10,0);  
		pane.add(infolabel, c);
		
		return pane;
	}

	public static void loadMenu(JMenuBar menuBar, String id) {
		// load the menu from the Preferences

		Preferences mainMenuPrefs = PreferenceUtils.getTopNode().node("main_menu");
		try {
			for (String childMenu : mainMenuPrefs.childrenNames()) {
				loadTopMenu(menuBar, id, mainMenuPrefs.node(childMenu));
			}
		} catch (BackingStoreException x) {
			Logger.getLogger(IGBUtils.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
		}
	}

	private static void loadTopMenu(JMenuBar menuBar, String id, Preferences menuPrefs) {
		String key = menuPrefs.get("menu", "???");
		JRPMenu menu = MenuUtil.getRPMenu(menuBar, id + "_main_" + key, BUNDLE.getString(key));
		menu.setMnemonic(BUNDLE.getString(key + "Mnemonic").charAt(0));
		try {
			for (String childMenu : menuPrefs.childrenNames()) {
				loadMenuItem(menu, id, menuPrefs.node(childMenu));
			}
		} catch (BackingStoreException x) {
			Logger.getLogger(IGBUtils.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
		}
	}

	private static void loadMenuItem(JRPMenu menu, String id, Preferences menuItemPrefs) {
		if (menuItemPrefs.get("separator", null) != null) {
			menu.addSeparator();
		} else if (menuItemPrefs.get("menu", null) != null) {
			loadSubMenu(menu, id, menuItemPrefs);
		} else if (menuItemPrefs.get("item", null) != null) {
			loadLeafItem(menu, menuItemPrefs);
		} else {
			Logger.getLogger(IGBUtils.class.getName()).log(Level.SEVERE, "error in menu preferences definition");
		}
	}

	private static void loadSubMenu(JRPMenu menu, String id, Preferences menuPrefs) {
		String key = menuPrefs.get("menu", "???");
		JRPMenu submenu = new JRPMenu(id + "_main_" + key, BUNDLE.getString(key));
		menu.add(submenu);
		try {
			for (String childMenu : menuPrefs.childrenNames()) {
				loadMenuItem(submenu, id, menuPrefs.node(childMenu));
			}
		} catch (BackingStoreException x) {
			Logger.getLogger(IGBUtils.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
		}
	}

	private static void loadLeafItem(JRPMenu menu, Preferences menuItemPrefs) {
		String className = menuItemPrefs.get("item", null);
		if (className.indexOf('.') == -1) {
			className = "com.affymetrix.igb.action." + className; // default
		}
		try {
			Class<?> clazz = Class.forName(className);
			Method m = clazz.getDeclaredMethod("getAction");
			GenericAction action = (GenericAction) m.invoke(null);
			String id = menu.getId() + "_" + menuItemPrefs.get("item", "???");
			JMenuItem item = action.isToggle() ? new JRPCheckBoxMenuItem(id, action) : new JRPMenuItem(id, action);
			if (action.usePrefixInMenu()) {
				MenuUtil.addToMenu(menu, item, menu.getText());
			} else {
				MenuUtil.addToMenu(menu, item);
			}
		} catch (Exception x) {
			Logger.getLogger(IGBUtils.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
		}
	}
}
