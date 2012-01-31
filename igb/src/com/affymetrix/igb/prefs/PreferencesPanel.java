/**
 *   Copyright (c) 2001-2006 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.prefs;

import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.igb.action.ExportPreferencesAction;
import com.affymetrix.igb.view.BundleRepositoryPrefsView;
import com.affymetrix.igb.view.TierPrefsViewGUI;
import com.affymetrix.igb.tiers.TrackDefaultViewGUI;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import javax.swing.*;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public final class PreferencesPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	public static int TAB_PLUGIN_PREFS = -1;
	private static final String WINDOW_NAME = "Preferences Window";
	private static final String HELP_WINDOW_NAME = "Preferences Help Window";
	private JFrame frame = null;
	public static PreferencesPanel singleton = null;
	private final JTabbedPane tab_pane;
	private Action export_action;
	private Action import_action;
	//Action clear_action;
	private Action help_action;
	private Action help_for_tab_action;
	public static int TAB_NUM_TIERS = -1;
	private final static String PREFERENCES = BUNDLE.getString("Preferences");
	private final static String HELP = BUNDLE.getString("helpMenu");
	public final static String IMPORT_ACTION_COMMAND = WINDOW_NAME + " / " + BUNDLE.getString("Import");
	public final static String EXPORT_ACTION_COMMAND = WINDOW_NAME + " / " + BUNDLE.getString("Export");
	public final static String HELP_ACTION_COMMAND = WINDOW_NAME + " / " + HELP;
	public final static String HELP_TAB_ACTION_COMMAND = WINDOW_NAME + " / " + BUNDLE.getString("HelpForCurrentTab");
	public TierPrefsViewGUI tpvGUI = null;

	private PreferencesPanel() {
		this.setLayout(new BorderLayout());
		tab_pane = new JTabbedPane();

		this.add(tab_pane, BorderLayout.CENTER);

		// using SCROLL_TAB_LAYOUT would disable the tool-tips, due to a Swing bug.
		//tab_pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	}

	/** Creates an instance of PreferencesView.  It will contain tabs for
	 *  setting various types of preferences.  You can put this view in any
	 *  JComponent you wish, but probably the best idea is to use
	 *  {@link #getFrame()}.
	 */
	public static PreferencesPanel getSingleton() {
		if (singleton != null) {
			return singleton;
		}
		singleton = new PreferencesPanel();
		singleton.tpvGUI = new TierPrefsViewGUI();
		singleton.tpvGUI.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentHidden(ComponentEvent e) {
				singleton.tpvGUI.tpv.removedFromView();
			}
		});
		singleton.getFrame().addWindowListener(singleton.tpvGUI);

		TAB_NUM_TIERS = singleton.addPrefEditorComponent(singleton.tpvGUI);
		singleton.addPrefEditorComponent(new TrackDefaultViewGUI());
		singleton.addPrefEditorComponent(KeyStrokesViewGUI.getSingleton());
		singleton.addPrefEditorComponent(new GraphsView());
		singleton.addPrefEditorComponent(OtherOptionsView.getSingleton());
		TAB_PLUGIN_PREFS = singleton.addPrefEditorComponent(BundleRepositoryPrefsView.getSingleton());

		return singleton;
	}

	/** Set the tab pane to the given index. */
	public void setTab(int i) {
		if (i < 0 || i >= tab_pane.getComponentCount()) {
			return;
		}
		tab_pane.setSelectedIndex(i);
		Component c = tab_pane.getComponentAt(i);
		if (c instanceof IPrefEditorComponent) {
			IPrefEditorComponent p = (IPrefEditorComponent) c;
			p.refresh();
		}
	}

	/** Adds the given component as a panel to the tab pane of preference editors.
	 *  @param pec  An implementation of PrefEditorComponent that must also be an
	 *              instance of java.awt.Component.
	 *  @return the index of the added tab in the tab pane.
	 */
	public int addPrefEditorComponent(final IPrefEditorComponent pec) {
		tab_pane.add(pec);
		pec.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentShown(ComponentEvent e) {
				pec.refresh();
			}
		});
		return tab_pane.indexOfComponent(pec);
	}

	private IPrefEditorComponent[] getPrefEditorComponents() {
		int count = tab_pane.getTabCount();
		IPrefEditorComponent[] comps = new IPrefEditorComponent[count];
		for (int i = 0; i < count; i++) {
			comps[i] = (IPrefEditorComponent) tab_pane.getComponentAt(i);
		}
		return comps;
	}

	/** Gets a JFrame containing the PreferencesView */
	public JFrame getFrame() {
		if (frame == null) {
			frame = new JFrame(PREFERENCES);
			final Container cont = frame.getContentPane();
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent evt) {
					// save the current size into the preferences, so the window
					// will re-open with this size next time
					PreferenceUtils.saveWindowLocation(frame, WINDOW_NAME);
					// if the TierPrefsView is being displayed, the apply any changes from it.
					// if it is not being displayed, then its changes have already been applied in componentHidden()
					if (singleton.tpvGUI != null) {
						if (singleton.tab_pane.getSelectedComponent() == singleton.tpvGUI) {
							singleton.tpvGUI.tpv.removedFromView();
						}
					}
					frame.dispose();
				}
			});

			JMenuBar menubar = this.getMenuBar();
			frame.setJMenuBar(menubar);

			cont.add(this);
			frame.pack(); // pack() to set frame to its preferred size
			Rectangle pos = PreferenceUtils.retrieveWindowLocation(WINDOW_NAME, new Rectangle(558, 582));
			if (pos != null) {
				PreferenceUtils.setWindowSize(frame, pos);
			}

//		ImageIcon icon = MenuUtil.getIcon("toolbarButtonGraphics/general/Preferences16.gif");
//		if (icon != null) {
//			frame.setIconImage(icon.getImage());
//		}
		}

		singleton.tpvGUI.refresh();	// update component list

		return frame;
	}

	private JMenuBar getMenuBar() {
		JMenuBar menu_bar = new JMenuBar();
		JMenu prefs_menu = new JMenu(PREFERENCES);
		prefs_menu.setMnemonic('P');
		
		JMenuItem exp = new JMenuItem(ExportPreferencesAction.getAction());
		JMenuItem imp = new JMenuItem(getImportAction());
		MenuUtil.addToMenu(prefs_menu, exp, PREFERENCES);
		MenuUtil.addToMenu(prefs_menu, imp, PREFERENCES);
		
		menu_bar.add(prefs_menu);

		JMenu help_menu = new JMenu(HELP);
		help_menu.setMnemonic('H');
		
		JMenuItem help = new JMenuItem(getHelpAction());
		JMenuItem helpTab = new JMenuItem(getHelpTabAction());
		MenuUtil.addToMenu(help_menu, help, PREFERENCES);
		MenuUtil.addToMenu(help_menu, helpTab, PREFERENCES);
		
		menu_bar.add(help_menu);
		return menu_bar;
	}

	private void showHelp(String s) {
		JEditorPane text = new JEditorPane();
		text.setContentType("text/html");
		text.setText(s);
		text.setEditable(false);
		text.setCaretPosition(0); // force a scroll to the top
		JScrollPane scroller = new JScrollPane(text);
		scroller.setPreferredSize(new java.awt.Dimension(300, 400));

		JFrame frameAncestor = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
		final JDialog dialog = new JDialog(frameAncestor, HELP, true);
		dialog.getContentPane().add(scroller, "Center");
		Action close_action = new GenericAction() {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				dialog.dispose();
			}

			@Override
			public String getText() {
				return "OK";
			}
		};
		JRPButton close = new JRPButton("PreferencesPanel_close", close_action);
		Box button_box = new Box(BoxLayout.X_AXIS);
		button_box.add(Box.createHorizontalGlue());
		button_box.add(close);
		button_box.add(Box.createHorizontalGlue());
		dialog.getContentPane().add(button_box, "South");
		dialog.pack();
		dialog.setLocationRelativeTo(this);

		Rectangle pos = PreferenceUtils.retrieveWindowLocation(HELP_WINDOW_NAME, new Rectangle(400, 400));
		if (pos != null) {
			PreferenceUtils.setWindowSize(dialog, pos);
		}
		dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent evt) {
				PreferenceUtils.saveWindowLocation(dialog, HELP_WINDOW_NAME);
				dialog.dispose();
			}
		});

		dialog.setVisible(true);
	}

	private String getHelpTextHTML() {
		StringBuffer sb = new StringBuffer(1000);

		sb.append("<h1>" + PREFERENCES + "</h1>\n");
		sb.append("<p>\n");
		sb.append("The tabs in the Help window control different aspects of the program.  ");
		sb.append("In each case, any values you set will remain in effect when you shut down and restart the program.  ");
		sb.append("In some cases, the changes will take effect immediately.  ");
		sb.append("In other cases, it will be necessary to shut down and restart the program before the changes take effect.  ");
		sb.append("</p>\n");

		sb.append("<h2>" + BUNDLE.getString("Export") + "</h2>\n");
		sb.append("<p>\n");
		sb.append("<b>" + BUNDLE.getString("Export") + "</b> allows you to save all the persistent preferences in the program to an XML file.  ");
		sb.append("A file chooser will open allowing you to choose the location to save the XML file.  ");
		sb.append("All preferences set by the user will be saved.  ");
		sb.append("</p>\n");
		sb.append("<h2>" + BUNDLE.getString("Import") + "</h2>\n");
		sb.append("<p>\n");
		sb.append("<b>" + BUNDLE.getString("Import") + "</b> allows you to load persistent preferences from an XML file.  ");
		sb.append("A file chooser will open allowing you to choose the file.  ");
		sb.append("Use this to load an XML file previously saved with <b>Export</b>. ");
		sb.append("All loaded preferences are <em>merged</em> with your existing preferences.  ");
		sb.append("Be sure you trust the provider of the file.  ");
		sb.append("</p>\n");
		return sb.toString();
	}

	private void showHelpForTab() {
		Component c = tab_pane.getSelectedComponent();
		String text = null;
		if (c instanceof IPrefEditorComponent) {
			IPrefEditorComponent pec = (IPrefEditorComponent) c;
			text = pec.getHelpTextHTML();
		}
		if (text == null) {
			JOptionPane.showMessageDialog(this, "No help available for this tab",
					"No Help", JOptionPane.INFORMATION_MESSAGE);
		} else {
			showHelp(text);
		}
	}

	private Action getImportAction() {
		if (import_action == null) {
			import_action = new GenericAction() {

				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent ae) {
					super.actionPerformed(ae);
					JFileChooser chooser = PreferenceUtils.getJFileChooser();
					int option = chooser.showOpenDialog(PreferencesPanel.this);
					if (option == JFileChooser.APPROVE_OPTION) {
						File f = chooser.getSelectedFile();
						try {
							PreferenceUtils.importPreferences(f);
						} catch (InvalidPreferencesFormatException ipfe) {
							ErrorHandler.errorPanel("ERROR", "Invalid preferences format:\n" + ipfe.getMessage()
									+ "\n\nYou can only IMPORT preferences from a file that was created with EXPORT.  "
									+ "In particular, you cannot import the file 'igb_prefs.xml' that was "
									+ "used in earlier versions of this program.");
						} catch (Exception e) {
							ErrorHandler.errorPanel("ERROR", "Error importing preferences from file", e);
						}
					}
					IPrefEditorComponent[] components = PreferencesPanel.this.getPrefEditorComponents();
					for (int i = 0; i < components.length; i++) {
						components[i].refresh();
					}
				}

				@Override
				public String getText() {
					return "Import Preferences";
				}

				public String getIconPath() {
					return null;
				}

				public int getMnemonic() {
					return KeyEvent.VK_I;
				}

				@Override
				public boolean isPopup() {
					return true;
				}
			};
			import_action.putValue(Action.ACTION_COMMAND_KEY, IMPORT_ACTION_COMMAND);
//			import_action.putValue(Action.ACCELERATOR_KEY, PreferenceUtils.getAccelerator(IMPORT_ACTION_COMMAND));
		}
		return import_action;
	}

	private Action getHelpAction() {
		if (help_action == null) {
			help_action = new GenericAction() {

				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent ae) {
					super.actionPerformed(ae);
					showHelp(getHelpTextHTML());
				}

				@Override
				public String getText() {
					return "General Help";
				}

				public String getIconPath() {
					return null;
				}

				public int getMnemonic() {
					return KeyEvent.VK_G;
				}

				@Override
				public boolean isPopup() {
					return true;
				}
			};
			help_action.putValue(Action.ACTION_COMMAND_KEY, HELP_ACTION_COMMAND);
//			help_action.putValue(Action.ACCELERATOR_KEY, PreferenceUtils.getAccelerator(HELP_ACTION_COMMAND));
		}
		return help_action;
	}

	private Action getHelpTabAction() {
		if (help_for_tab_action == null) {
			help_for_tab_action = new GenericAction() {

				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent ae) {
					super.actionPerformed(ae);
					showHelpForTab();
				}

				@Override
				public String getText() {
					return BUNDLE.getString("HelpForCurrentTab");
				}

				public String getIconPath() {
					return null;
				}

				public int getMnemonic() {
					return KeyEvent.VK_C;
				}

				@Override
				public boolean isPopup() {
					return true;
				}
			};
			help_for_tab_action.putValue(Action.ACTION_COMMAND_KEY, HELP_TAB_ACTION_COMMAND);
//			help_for_tab_action.putValue(Action.ACCELERATOR_KEY, PreferenceUtils.getAccelerator(HELP_TAB_ACTION_COMMAND));
		}
		return help_for_tab_action;
	}
}
