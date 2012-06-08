package com.affymetrix.igb.util;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.ReportBugAction;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPCheckBoxMenuItem;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.*;
import com.affymetrix.igb.shared.CollapseExpandAction;
import com.affymetrix.igb.shared.DeselectAllAction;
import com.affymetrix.igb.shared.SelectAllAction;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 *
 * @author hiralv
 */
public class MainMenuUtil {
	private static final MainMenuUtil instance = new MainMenuUtil();
	private MainMenuUtil() {
		super();
	}
	public static MainMenuUtil getInstance() {
		return instance;
	}
	public void loadMenu(JMenuBar menuBar, String id) {
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
			Logger.getLogger(MainMenuUtil.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
		}
	}

	private void fileMenu(JMenuBar menuBar, String id) {
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
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_closeTracks", CloseTracksAction.getAction()));
		file_menu.addSeparator();
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_preferences", PreferencesAction.getAction()));
		file_menu.addSeparator();
		MenuUtil.addToMenu(file_menu, new JRPMenuItem(id + "_main_fileMenu_exit", ExitAction.getAction()));
	}

	private void editMenu(JMenuBar menuBar, String id) {
		JRPMenu edit_menu = MenuUtil.getRPMenu(menuBar, id + "_main_editMenu", BUNDLE.getString("editMenu"));
		edit_menu.setMnemonic(BUNDLE.getString("editMenuMnemonic").charAt(0));
		MenuUtil.addToMenu(edit_menu, new JRPMenuItem(id + "_main_editMenu_copyResidues", CopyResiduesAction.getAction()));
		MenuUtil.addToMenu(edit_menu, new JRPMenuItem(id + "_main_editMenu_zoomingRepack", ZoomingRepackAction.getAction()));
		MenuUtil.addToMenu(edit_menu, new JRPMenuItem(id + "_main_editMenu_colorChoice", ColorSchemeChoiceAction.getAction()));
		MenuUtil.addToMenu(edit_menu, new JRPMenuItem(id + "_main_editMenu_canonicalize", CanonicalizeTracksAction.getAction()));
		JRPMenu select_menu = new JRPMenu(id + "_main_editMenu_select", IGBConstants.BUNDLE.getString("selectAllAction"));
		select_menu.add(new JRPMenuItem(id + "_main_editMenu_select_all", SelectAllAction.getAction()));
		select_menu.add(new JRPMenuItem(id + "_main_editMenu_deselect_all", DeselectAllAction.getAction()));
		for (FileTypeCategory category : FileTypeCategory.values()) {
			JRPMenuItem item = new JRPMenuItem(id + "_main_editMenu_select_all_" + category.name(), SelectAllAction.getAction(category));
			select_menu.add(item);
		}
		edit_menu.add(select_menu);
	}

	private void viewMenu(JMenuBar menuBar, String id) {
		JRPMenu view_menu = MenuUtil.getRPMenu(menuBar, id + "_main_viewMenu", BUNDLE.getString("viewMenu"));
		view_menu.setMnemonic(BUNDLE.getString("viewMenuMnemonic").charAt(0));
		JRPMenu strands_menu = new JRPMenu(id + "_main_viewMenu_strands", BUNDLE.getString("strands"));
		strands_menu.add(new JRPCheckBoxMenuItem(id + "_main_viewMenu_strands_showPlus", ShowPlusStrandAction.getAction()));
		strands_menu.add(new JRPCheckBoxMenuItem(id + "_main_viewMenu_strands_showMinus", ShowMinusStrandAction.getAction()));
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
		MenuUtil.addToMenu(view_menu, new JRPCheckBoxMenuItem(id + "_main_viewMenu_showIGBTrackMark", ShowIGBTrackMarkAction.getAction()));
		view_menu.addSeparator();
		JRPMenu track_resize_behavior = MenuUtil.getRPMenu(menuBar, id + "_main_viewMenu_trackResizeBehavior", BUNDLE.getString("trackResizeBehavior"));
		MenuUtil.addToMenu(view_menu, track_resize_behavior);
		MenuUtil.addToMenu(track_resize_behavior, new JRPCheckBoxMenuItem(id + "_main_viewMenu_trackResizeBehavior_adjustAllTrack", TrackResizingAction.getAdjustAllAction()));
		MenuUtil.addToMenu(track_resize_behavior, new JRPCheckBoxMenuItem(id + "_main_viewMenu_trackResizeBehavior_adjustAdjacentTrack", TrackResizingAction.getAdjustAdjacentAction()));
		//MenuUtil.addToMenu(view_menu, new JRPCheckBoxMenuItem(id + "_main_viewMenu_test", CollapseExpandAction.createSingleton()));
	}

	private void toolMenu(JMenuBar menuBar, String id) {
		JRPMenu tools_menu = MenuUtil.getRPMenu(menuBar, id + "_main_toolsMenu", BUNDLE.getString("toolsMenu"));
		tools_menu.setMnemonic(BUNDLE.getString("toolsMenuMnemonic").charAt(0));
		MenuUtil.addToMenu(tools_menu, new JRPMenuItem(id + "_main_toolsMenu_webLinks", WebLinksAction.getAction()));
		JRPMenu scripts_menu = new JRPMenu(id + "_main_toolsMenu_scripts", BUNDLE.getString("scripts"));
		MenuUtil.addToMenu(scripts_menu, new JRPMenuItem(id + "_main_toolsMenu_scripts_runScript", RunScriptAction.getAction()));
		MenuUtil.addToMenu(scripts_menu, new JRPMenuItem(id + "_main_toolsMenu_scripts_saveScript", SaveScriptAction.getAction()));
		MenuUtil.addToMenu(scripts_menu, new JRPMenuItem(id + "_main_toolsMenu_scripts_cancelScript", CancelScriptAction.getAction()));
		MenuUtil.addToMenu(scripts_menu, new JRPMenuItem(id + "_main_toolsMenu_scripts_clearScript", ClearScriptAction.getAction()));
		tools_menu.add(scripts_menu);
	}

	private void helpMenu(JMenuBar menuBar, String id) {
		JRPMenu help_menu = MenuUtil.getRPMenu(menuBar, id + "_main_helpMenu", BUNDLE.getString("helpMenu"));
		help_menu.setMnemonic(BUNDLE.getString("helpMenuMnemonic").charAt(0));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_aboutIGB", AboutIGBAction.getAction()));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_forumHelp", ForumHelpAction.getAction()));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_reportBug", ReportBugAction.getAction()));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_requestFeature", RequestFeatureAction.getAction()));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_documentation", DocumentationAction.getAction()));
		MenuUtil.addToMenu(help_menu, new JRPMenuItem(id + "_main_helpMenu_showConsole", ShowConsoleAction.getAction()));
	}

	private void loadDefaultMenu(JMenuBar menuBar, String id) {
		fileMenu(menuBar, id);
		editMenu(menuBar, id);
		viewMenu(menuBar, id);
		toolMenu(menuBar, id);
		helpMenu(menuBar, id);
	}

	private void loadTopMenu(JMenuBar menuBar, String id, Preferences menuPrefs) {
		String key = menuPrefs.get("menu", "???");
		JRPMenu menu = MenuUtil.getRPMenu(menuBar, id + "_main_" + key, BUNDLE.getString(key));
		menu.setMnemonic(BUNDLE.getString(key + "Mnemonic").charAt(0));
		try {
			for (String childMenu : menuPrefs.childrenNames()) {
				loadMenuItem(menu, id, menuPrefs.node(childMenu));
			}
		} catch (BackingStoreException x) {
			Logger.getLogger(MainMenuUtil.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
		}
	}

	private void loadMenuItem(JRPMenu menu, String id, Preferences menuItemPrefs) {
		if (menuItemPrefs.get("separator", null) != null) {
			menu.addSeparator();
		} else if (menuItemPrefs.get("menu", null) != null) {
			loadSubMenu(menu, id, menuItemPrefs);
		} else if (menuItemPrefs.get("item", null) != null) {
			loadLeafItem(menu, menuItemPrefs);
		} else {
			Logger.getLogger(MainMenuUtil.class.getName()).log(Level.SEVERE, "error in menu preferences definition");
		}
	}

	private void loadSubMenu(JRPMenu menu, String id, Preferences menuPrefs) {
		String key = menuPrefs.get("menu", "???");
		JRPMenu submenu = new JRPMenu(id + "_main_" + key, BUNDLE.getString(key));
		menu.add(submenu);
		try {
			for (String childMenu : menuPrefs.childrenNames()) {
				loadMenuItem(submenu, id, menuPrefs.node(childMenu));
			}
		} catch (BackingStoreException x) {
			Logger.getLogger(MainMenuUtil.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
		}
	}

	private void loadLeafItem(JRPMenu menu, Preferences menuItemPrefs) {
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
			Logger.getLogger(MainMenuUtil.class.getName()).log(Level.SEVERE, "error loading menu preferences", x);
		}
	}
}
