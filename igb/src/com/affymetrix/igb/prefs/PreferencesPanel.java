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

import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.view.TierPrefsView;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public final class PreferencesPanel extends JPanel {

  private static final String WINDOW_NAME = "Preferences Window";
  private static final String HELP_WINDOW_NAME = "Preferences Help Window";

  JFrame frame = null;
  public static PreferencesPanel singleton = null;

  JTabbedPane tab_pane;

  Action export_action;
  Action import_action;
  //Action clear_action;
  Action help_action;
  Action help_for_tab_action;

  private PreferencesPanel() {
    this.setLayout(new BorderLayout());
    tab_pane = new JTabbedPane();

    this.add(tab_pane, BorderLayout.CENTER);

    // using SCROLL_TAB_LAYOUT would disable the tool-tips, due to a Swing bug.
    //tab_pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
  }

  public static int TAB_NUM_TIERS = -1;
  /*private static int TAB_NUM_KEY_STROKES = -1;
  private static int TAB_NUM_MISC_OPTIONS = -1;
  private static int TAB_NUM_GRAPHS_VIEW = -1;*/
	
  public final static String IMPORT_ACTION_COMMAND = WINDOW_NAME + " / Import";
  public final static String EXPORT_ACTION_COMMAND = WINDOW_NAME + " / Export";
  public final static String HELP_ACTION_COMMAND  = WINDOW_NAME + " / Help";
  public final static String HELP_TAB_ACTION_COMMAND  = WINDOW_NAME + " / Help for current tab";

  private TierPrefsView tpv = null;

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
		singleton.tpv = new TierPrefsView(false, true);
		singleton.tpv.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentHidden(ComponentEvent e) {
				singleton.tpv.removedFromView();
			}
		});
		singleton.getFrame().addWindowListener(singleton.tpv);

		TAB_NUM_TIERS = singleton.addPrefEditorComponent(singleton.tpv);
		singleton.addPrefEditorComponent(new KeyStrokesView());
		singleton.addPrefEditorComponent(new GraphsView());
		singleton.addPrefEditorComponent(new OptionsView());

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
    tab_pane.addTab(pec.getName(), pec.getIcon(), (Component) pec, pec.getToolTip());
    ((Component) pec).addComponentListener(new ComponentAdapter() {
			@Override
      public void componentShown(ComponentEvent e) {
        pec.refresh();
      }
    });
    return tab_pane.indexOfComponent((Component) pec);
  }

  private IPrefEditorComponent[] getPrefEditorComponents() {
    int count = tab_pane.getTabCount();
    IPrefEditorComponent[] comps = new IPrefEditorComponent[count];
    for (int i=0; i<count; i++) {
      comps[i] = (IPrefEditorComponent) tab_pane.getComponentAt(i);
    }
    return comps;
  }

  /** Gets a JFrame containing the PreferencesView */
  public JFrame getFrame() {
    if (frame == null) {
      frame = new JFrame("Preferences");
      final Container cont = frame.getContentPane();
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      frame.addWindowListener( new WindowAdapter() {
				@Override
        public void windowClosing(WindowEvent evt) {
          // save the current size into the preferences, so the window
          // will re-open with this size next time
          UnibrowPrefsUtil.saveWindowLocation(frame, WINDOW_NAME);
          // if the TierPrefsView is being displayed, the apply any changes from it.
          // if it is not being displayed, then its changes have already been applied in componentHidden()
          if (singleton.tpv != null) {
            if (singleton.tab_pane.getSelectedComponent() == singleton.tpv) {
              singleton.tpv.removedFromView();
            }
          }
          frame.dispose();
        }
      });

      JMenuBar menubar = this.getMenuBar();
      frame.setJMenuBar(menubar);

      cont.add(this);
      frame.pack(); // pack() to set frame to its preferred size
      Rectangle pos = UnibrowPrefsUtil.retrieveWindowLocation(WINDOW_NAME, new Rectangle(1000, 400));
      if (pos != null) {
        UnibrowPrefsUtil.setWindowSize(frame, pos);
      }

		ImageIcon icon = MenuUtil.getIcon("toolbarButtonGraphics/general/Preferences16.gif");
		if (icon != null) {
			frame.setIconImage(icon.getImage());
		}
    }    

	singleton.tpv.refresh();	// update component list

    return frame;
  }

  private JMenuBar getMenuBar() {
    JMenuBar menu_bar = new JMenuBar();
    JMenu prefs_menu = new JMenu("Preferences");
    prefs_menu.setMnemonic('P');

    prefs_menu.add(getExportAction());
    prefs_menu.add(getImportAction());

    menu_bar.add(prefs_menu);

    JMenu help_menu = new JMenu("Help");
    help_menu.setMnemonic('H');
    menu_bar.add(help_menu);
    help_menu.add(getHelpAction());
    help_menu.add(getHelpTabAction());

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
    final JDialog dialog = new JDialog(frameAncestor, "Help", true);
    dialog.getContentPane().add(scroller, "Center");
    Action close_action = new AbstractAction("OK") {
      public void actionPerformed(ActionEvent e) {
        dialog.dispose();
      }
    };
    JButton close = new JButton(close_action);
    Box button_box = new Box(BoxLayout.X_AXIS);
    button_box.add(Box.createHorizontalGlue());
    button_box.add(close);
    button_box.add(Box.createHorizontalGlue());
    dialog.getContentPane().add(button_box, "South");
    dialog.pack();
    dialog.setLocationRelativeTo(this);

    Rectangle pos = UnibrowPrefsUtil.retrieveWindowLocation(HELP_WINDOW_NAME, new Rectangle(400, 400));
    if (pos != null) {
      UnibrowPrefsUtil.setWindowSize(dialog, pos);
    }
    dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener( new WindowAdapter() {
			@Override
      public void windowClosing(WindowEvent evt) {
        UnibrowPrefsUtil.saveWindowLocation(dialog, HELP_WINDOW_NAME);
        dialog.dispose();
      }
    });

    dialog.setVisible(true);
  }


  private String getHelpTextHTML() {
    StringBuffer sb = new StringBuffer(1000);

    sb.append("<h1>Preferences</h1>\n");
    sb.append("<p>\n");
    sb.append("The tabs in the Help window control different aspects of the program.  ");
    sb.append("In each case, any values you set will remain in effect when you shut down and restart the program.  ");
    sb.append("In some cases, the changes will take effect immediately.  ");
    sb.append("In other cases, it will be necessary to shut down and restart the program before the changes take effect.  ");
    sb.append("</p>\n");

    sb.append("<h2>Export</h2>\n");
    sb.append("<p>\n");
    sb.append("<b>Export</b> allows you to save all the persistent preferences in the program to an XML file.  ");
    sb.append("A file chooser will open allowing you to choose the location to save the XML file.  ");
    sb.append("All preferences set by the user will be saved.  ");
    sb.append("</p>\n");
    sb.append("<h2>Import</h2>\n");
    sb.append("<p>\n");
    sb.append("<b>Import</b> allows you to load persistent preferences from an XML file.  ");
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

  private Action getExportAction() {
    if (export_action == null) {
      export_action = new AbstractAction("Export Preferences ...") {
        public void actionPerformed(ActionEvent ae) {
          UnibrowPrefsUtil.exportPreferences(PreferencesPanel.this);
        }
      };
      export_action.putValue(Action.ACTION_COMMAND_KEY, EXPORT_ACTION_COMMAND);
      export_action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
      export_action.putValue(Action.ACCELERATOR_KEY, UnibrowPrefsUtil.getAccelerator(EXPORT_ACTION_COMMAND));
      ImageIcon icon = MenuUtil.getIcon("toolbarButtonGraphics/general/Export16.gif");
      export_action.putValue(Action.SMALL_ICON, icon);
    }
    return export_action;
  }

  private Action getImportAction() {
    if (import_action == null) {
      import_action = new AbstractAction("Import Preferences ...") {
        public void actionPerformed(ActionEvent ae) {
          UnibrowPrefsUtil.importPreferences(PreferencesPanel.this);
          IPrefEditorComponent[] components = PreferencesPanel.this.getPrefEditorComponents();
          for (int i=0; i<components.length; i++) {
            components[i].refresh();
          }
        }
      };
      import_action.putValue(Action.ACTION_COMMAND_KEY, IMPORT_ACTION_COMMAND);
      import_action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
      import_action.putValue(Action.ACCELERATOR_KEY, UnibrowPrefsUtil.getAccelerator(IMPORT_ACTION_COMMAND));
      ImageIcon icon = MenuUtil.getIcon("toolbarButtonGraphics/general/Import16.gif");
      import_action.putValue(Action.SMALL_ICON, icon);
    }
    return import_action;
  }

  private Action getHelpAction() {
    if (help_action == null) {
      help_action = new AbstractAction("General Help") {
        public void actionPerformed(ActionEvent ae) {
          showHelp(getHelpTextHTML());
        }
      };
      help_action.putValue(Action.ACTION_COMMAND_KEY, HELP_ACTION_COMMAND);
      help_action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_G));
      help_action.putValue(Action.ACCELERATOR_KEY, UnibrowPrefsUtil.getAccelerator(HELP_ACTION_COMMAND));
      Icon icon = MenuUtil.getIcon("toolbarButtonGraphics/general/Help16.gif");
      help_action.putValue(Action.SMALL_ICON, icon);
    }
    return help_action;
  }

  private Action getHelpTabAction() {
    if (help_for_tab_action == null) {
      help_for_tab_action = new AbstractAction("Help for Current Tab") {
        public void actionPerformed(ActionEvent ae) {
          showHelpForTab();
        }
      };
      help_for_tab_action.putValue(Action.ACTION_COMMAND_KEY, HELP_TAB_ACTION_COMMAND);
      help_for_tab_action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
      help_for_tab_action.putValue(Action.ACCELERATOR_KEY, UnibrowPrefsUtil.getAccelerator(HELP_TAB_ACTION_COMMAND));
      Icon icon = MenuUtil.getIcon("toolbarButtonGraphics/general/ContextualHelp16.gif");
      help_for_tab_action.putValue(Action.SMALL_ICON, icon);
    }
    return help_for_tab_action;
  }

}
