package com.affymetrix.igb.util;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.ReportBugAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.InfoLabel;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPCheckBoxMenuItem;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.action.AboutIGBAction;
import com.affymetrix.igb.action.AutoLoadThresholdAction;
import com.affymetrix.igb.action.AutoScrollAction;
import com.affymetrix.igb.action.ClampViewAction;
import com.affymetrix.igb.action.CopyResiduesAction;
import com.affymetrix.igb.action.DocumentationAction;
import com.affymetrix.igb.action.DrawCollapseControlAction;
import com.affymetrix.igb.action.ExitAction;
import com.affymetrix.igb.action.ExportFileAction;
import com.affymetrix.igb.action.ExportImageAction;
import com.affymetrix.igb.action.ForumHelpAction;
import com.affymetrix.igb.action.LoadFileAction;
import com.affymetrix.igb.action.LoadRefTrackAction;
import com.affymetrix.igb.action.LoadURLAction;
import com.affymetrix.igb.action.NextSearchSpanAction;
import com.affymetrix.igb.action.PreferencesAction;
import com.affymetrix.igb.action.PrintAction;
import com.affymetrix.igb.action.PrintFrameAction;
import com.affymetrix.igb.action.RequestFeatureAction;
import com.affymetrix.igb.action.ShowConsoleAction;
import com.affymetrix.igb.action.ShowMinusStrandAction;
import com.affymetrix.igb.action.ShowMixedStrandAction;
import com.affymetrix.igb.action.ShowPlusStrandAction;
import com.affymetrix.igb.action.ShrinkWrapAction;
import com.affymetrix.igb.action.ToggleHairlineAction;
import com.affymetrix.igb.action.ToggleHairlineLabelAction;
import com.affymetrix.igb.action.ToggleToolTipAction;
import com.affymetrix.igb.action.ViewGenomicSequenceInSeqViewerAction;
import com.affymetrix.igb.action.ViewAlignmentSequenceInSeqViewerAction;
import com.affymetrix.igb.action.WebLinksAction;

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

		Preferences mainMenuPrefs = PreferenceUtils.getAltNode(PreferenceUtils.MENU_NODE_NAME);
		try {
			if (mainMenuPrefs.childrenNames().length == 0) {
				loadDefaultMenu(menuBar, id);
			}
			else {
				for (String childMenu : mainMenuPrefs.childrenNames()) {
					loadTopMenu(menuBar, id, mainMenuPrefs.node(childMenu));
				}
			}
		} catch (BackingStoreException x) {
			Logger.getLogger(IGBUtils.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
		}
	}

	private static void fileMenu(JMenuBar menuBar, String id) {
		JRPMenu file_menu = MenuUtil.getRPMenu(menuBar, id + "_main_fileMenu", BUNDLE.getString("fileMenu"));
		file_menu.setMnemonic(BUNDLE.getString("fileMenuMnemonic").charAt(0));
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_loadFile", LoadFileAction.getAction()));
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_loadURL", LoadURLAction.getAction()));
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_loadRefTrack", LoadRefTrackAction.getAction()));
		file_menu.addSeparator();
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_print", PrintAction.getAction()));
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_printFrame", PrintFrameAction.getAction()));
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_exportImage", ExportImageAction.getAction()));
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_exportFile", ExportFileAction.getAction()));
		file_menu.addSeparator();
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_preferences", PreferencesAction.getAction()));
		file_menu.addSeparator();
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_exit", ExitAction.getAction()));
	}

	private static void editMenu(JMenuBar menuBar, String id) {
		JRPMenu edit_menu = MenuUtil.getRPMenu(menuBar, id + "_main_editMenu", BUNDLE.getString("editMenu"));
		edit_menu.setMnemonic(BUNDLE.getString("editMenuMnemonic").charAt(0));
		MenuUtil.addToMenu(edit_menu, new JRPMenuItem(id + "_main_editMenu_copyResidues", CopyResiduesAction.getAction()));
		MenuUtil.addToMenu(edit_menu, new JRPMenuItem(id + "_main_editMenu_zoomingRepack",
				new com.affymetrix.igb.action.ZoomingRepackAction(com.affymetrix.igb.Application.getSingleton().getMapView())));
	}

	private static void viewMenu(JMenuBar menuBar, String id) {
		JRPMenu view_menu = MenuUtil.getRPMenu(menuBar, id + "_main_viewMenu", BUNDLE.getString("viewMenu"));
		view_menu.setMnemonic(BUNDLE.getString("viewMenuMnemonic").charAt(0));
		JRPMenu strands_menu = new JRPMenu(id + "_main_viewMenu_strands", "Strands");
		strands_menu.add(new JRPCheckBoxMenuItem(id + "_main_viewMenu_strands_showPlus", ShowPlusStrandAction.getAction()));
		strands_menu.add(new JRPCheckBoxMenuItem(id + "_main_viewMenu_strands_showMinus", ShowMinusStrandAction.getAction()));
		strands_menu.add(new JRPCheckBoxMenuItem(id + "_main_viewMenu_strands_showMixed", ShowMixedStrandAction.getAction()));
		view_menu.add(strands_menu);
		MenuUtil.addToMenu(view_menu, new JRPMenuItem(id + "_main_viewMenu_autoscroll", AutoScrollAction.getAction()));
		MenuUtil.addToMenu(view_menu, new JRPMenuItem(id + "_main_viewMenu_viewGenomicSequenceInSeqViewer", ViewGenomicSequenceInSeqViewerAction.getAction()));
		MenuUtil.addToMenu(view_menu, new JRPMenuItem(id + "_main_viewMenu_viewAlignmentSequenceInSeqViewer", ViewAlignmentSequenceInSeqViewerAction.getAction()));
		ViewAlignmentSequenceInSeqViewerAction.getAction().setEnabled(false);
		MenuUtil.addToMenu(view_menu, new JRPMenuItem(id + "_main_viewMenu_nextSearchSpanAction", NextSearchSpanAction.getAction()));
		NextSearchSpanAction.getAction().setEnabled(false);
		view_menu.addSeparator();
		MenuUtil.addToMenu(view_menu, new JRPMenuItem(id + "_main_viewMenu_setThreshold", AutoLoadThresholdAction.getAction()));
		view_menu.addSeparator();
		MenuUtil.addToMenu(view_menu, new JRPCheckBoxMenuItem(id + "_main_viewMenu_clampView", ClampViewAction.getAction()));
		view_menu.addSeparator();
		MenuUtil.addToMenu(view_menu, new JRPCheckBoxMenuItem(id + "_main_viewMenu_shrinkWrap", ShrinkWrapAction.getAction()));
		MenuUtil.addToMenu(view_menu, new JRPCheckBoxMenuItem(id + "_main_viewMenu_showHairline", ToggleHairlineAction.getAction()));
		MenuUtil.addToMenu(view_menu, new JRPCheckBoxMenuItem(id + "_main_viewMenu_toggleHairlineLabel", ToggleHairlineLabelAction.getAction()));
		MenuUtil.addToMenu(view_menu, new JRPCheckBoxMenuItem(id + "_main_viewMenu_toggleToolTip", ToggleToolTipAction.getAction()));
		MenuUtil.addToMenu(view_menu, new JRPCheckBoxMenuItem(id + "_main_viewMenu_drawCollapseControl", DrawCollapseControlAction.getAction()));
	}

	private static void toolMenu(JMenuBar menuBar, String id) {
		JRPMenu tools_menu = MenuUtil.getRPMenu(menuBar, id + "_main_toolsMenu", BUNDLE.getString("toolsMenu"));
		tools_menu.setMnemonic(BUNDLE.getString("toolsMenuMnemonic").charAt(0));
		MenuUtil.addToMenu(tools_menu, new JRPMenuItem(id + "_main_toolsMenu_webLinks", WebLinksAction.getAction()));
	}

	private static void helpMenu(JMenuBar menuBar, String id) {
		JRPMenu help_menu = MenuUtil.getRPMenu(menuBar, id + "_main_helpMenu", BUNDLE.getString("helpMenu"));
		help_menu.setMnemonic(BUNDLE.getString("helpMenuMnemonic").charAt(0));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_aboutIGB", AboutIGBAction.getAction()));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_forumHelp", ForumHelpAction.getAction()));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_reportBug", ReportBugAction.getAction()));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_requestFeature", RequestFeatureAction.getAction()));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_documentation", DocumentationAction.getAction()));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_showConsole", ShowConsoleAction.getAction()));
	}

	private static void loadDefaultMenu(JMenuBar menuBar, String id) {
		fileMenu(menuBar, id);
		editMenu(menuBar, id);
		viewMenu(menuBar, id);
		toolMenu(menuBar, id);
		helpMenu(menuBar, id);
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
