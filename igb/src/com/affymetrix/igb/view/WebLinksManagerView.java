/**
*   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.igb.view;

import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.parsers.XmlPrefsParser;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.swing.DisplayUtils;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 *  A panel for viewing and editing weblinks.
 */
public class WebLinksManagerView extends JPanel {
  JList the_list;

  JMenuBar mbar = new JMenuBar();

  Action import_action;
  Action export_action;

  Action delete_action;
  Action edit_action;
  Action add_action;
  
  WebLinkEditorPanel edit_panel;


  /** Creates a new instance of Class */
  protected WebLinksManagerView() {
    super();

    the_list = createJList();

    JScrollPane scroll_pane = new JScrollPane(the_list);

    this.setLayout(new BorderLayout());
    scroll_pane.setMinimumSize(new Dimension(50,50));
    this.add(scroll_pane, BorderLayout.CENTER);

    export_action = makeExportAction();
    import_action = makeImportAction();

    delete_action = makeDeleteAction();
    add_action = makeAddAction();
    edit_action = makeEditAction();

    setUpMenuBar();
    setUpButtons();
    setUpPopupMenu();
        
    enableActions();
    this.validate();

    edit_panel = new WebLinkEditorPanel();
  }
  
  public static Action getShowFrameAction() {
    Action a = new AbstractAction("Configure Web Links") {
      public void actionPerformed(ActionEvent evt) {
        showManager();
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/general/Search16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Manage Web Links");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_W));
    setAccelerator(a);
    return a;
  }

  JList createJList() {
    JList j_list = new JList(WebLink.getWebLinkListModel());
    
    j_list.setCellRenderer(list_renderer);    
    j_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    j_list.addListSelectionListener(list_listener);
    
    return j_list;
  }

  ListCellRenderer list_renderer = new DefaultListCellRenderer() {
    public Component getListCellRendererComponent(JList list,
        Object value, int index, boolean isSelected, boolean cellHasFocus) {
      WebLink wl = (WebLink) value;
      String name = wl.getName();
      String regex = "All Tiers";
      if (wl.getRegex() != null) {
        regex = wl.getRegex();
        if (regex.startsWith("(?i)")) {
          regex = regex.substring(4);
        }
      }
      String msg = "<html><b>'" + name 
        + "'</b>:&nbsp;&nbsp;&nbsp;&nbsp;<font color=red>" + regex + "</font>";
      
      Component c = super.getListCellRendererComponent(list, msg, index,isSelected,cellHasFocus);
      return c;
    }
  };

  ListSelectionListener list_listener = new ListSelectionListener() {
    public void valueChanged(ListSelectionEvent e) {
      if (! e.getValueIsAdjusting()) {
        enableActions();
      }
    }
  };
  
  void enableActions() {
    int num_selections = the_list.getSelectedValues().length;
    
    import_action.setEnabled(true);
    export_action.setEnabled(the_list.getModel().getSize() > 0);

    delete_action.setEnabled(num_selections > 0);
    edit_action.setEnabled(num_selections == 1);
    add_action.setEnabled(true);
  }
  
  static void setAccelerator(Action a) {
    KeyStroke ks = UnibrowPrefsUtil.getAccelerator("Web Links Manager / "+a.getValue(Action.NAME));
    a.putValue(Action.ACCELERATOR_KEY, ks);
  }

  void setUpMenuBar() {
    JMenuBar menu_bar = new JMenuBar();
    JMenu links_menu = new JMenu("Web Links") {      
      public JMenuItem add(Action a) {
        JMenuItem menu_item = super.add(a);
        menu_item.setToolTipText(null);
        return menu_item;
      }
    };
    links_menu.setMnemonic('L');

    links_menu.add(edit_action);
    links_menu.add(add_action);
    links_menu.add(delete_action);
    links_menu.addSeparator();
    links_menu.add(import_action);
    links_menu.add(export_action);

    menu_bar.add(links_menu);
    this.add(menu_bar, BorderLayout.NORTH);
  }

  void setUpPopupMenu() {
    final JPopupMenu popup = new JPopupMenu() {      
      public JMenuItem add(Action a) {
        JMenuItem menu_item = super.add(a);
        menu_item.setToolTipText(null);
        return menu_item;
      }
    };
    popup.add(edit_action);
    popup.add(add_action);
    popup.add(delete_action);
    popup.addSeparator();
    popup.add(import_action);
    popup.add(export_action);
    MouseAdapter mouse_adapter = new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if (popup.isPopupTrigger(e)) {
          popup.show(the_list, e.getX(), e.getY());
        }
      }
      public void mouseReleased(MouseEvent e) {
        if (popup.isPopupTrigger(e)) {
          popup.show(the_list, e.getX(), e.getY());
        }
      }
    };
    the_list.addMouseListener(mouse_adapter);
  }

  void setUpButtons() {
    JToolBar tool_bar = new JToolBar(JToolBar.HORIZONTAL);
    tool_bar.setFloatable(false);

    tool_bar.add(new JButton(edit_action));
    tool_bar.addSeparator();
    tool_bar.add(new JButton(add_action));
    tool_bar.addSeparator();
    tool_bar.add(new JButton(delete_action));
    this.add(tool_bar, BorderLayout.SOUTH);
  }

  Action makeImportAction() {
    Action a = new AbstractAction("Import ...") {
      public void actionPerformed(ActionEvent ae) {
        importWebLinks();
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/general/Import16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Import Web Links");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
    setAccelerator(a);
    return a;
  }

  Action makeExportAction() {
    Action a = new AbstractAction("Export ...") {
      public void actionPerformed(ActionEvent ae) {
        exportWebLinks();
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/general/Export16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Export Web Links");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
    setAccelerator(a);
    return a;
  }
  

  Action makeDeleteAction() {
    Action a = new AbstractAction("Delete ...") {
      public void actionPerformed(ActionEvent ae) {
        Object[] selections = the_list.getSelectedValues();
        Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, the_list);
        if (selections.length == 0) {
          this.setEnabled(false);
          return;
        }
        int yes = JOptionPane.showConfirmDialog(frame,
          "Delete these "+selections.length+" selected link(s)?",
          "Delete?", JOptionPane.YES_NO_OPTION);
        if (yes == JOptionPane.YES_OPTION) {
          for (int i=0; i<selections.length; i++) {
            WebLink.removeWebLink((WebLink) selections[i]);
          }
        }
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/general/Delete16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Delete Selected Link(s)");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
    setAccelerator(a);
    return a;
  }

  Action makeAddAction() {
    Action a = new AbstractAction("Add...") {
      public void actionPerformed(ActionEvent ae) {
        WebLink link = new WebLink();
        edit_panel.setWebLink(link);
        boolean ok = edit_panel.showDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, the_list));
        if (ok) {
          edit_panel.setLinkPropertiesFromGUI();
          WebLink.addWebLink(link);
        }
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/development/WebComponentAdd16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Add New Web Link");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
    setAccelerator(a);
    return a;
  }
  
  Action makeEditAction() {
    Action a = new AbstractAction("Edit...") {
      public void actionPerformed(ActionEvent ae) {
        WebLink link = (WebLink) the_list.getSelectedValue();
        edit_panel.setWebLink(link);
        boolean ok = edit_panel.showDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, the_list));
        if (ok) {
          edit_panel.setLinkPropertiesFromGUI();
          the_list.invalidate();
          the_list.repaint();
        }        
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/development/WebComponent16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Edit Selected Web Link");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
    setAccelerator(a);
    return a;
  }
  
  static JFileChooser static_chooser = null;

  /** Gets a static re-usable file chooser that prefers "html" files. */
  public static JFileChooser getJFileChooser() {
    if (static_chooser == null) {
      static_chooser = UniFileChooser.getFileChooser("XML file", "xml");
      static_chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
    }
    static_chooser.rescanCurrentDirectory();
    return static_chooser;
  }

  /**
   *  Tries to import weblinks.
   *  Makes use of {@link BookmarksParser#parse(BookmarkList, File)}.
   */
  void importWebLinks() {
    JFileChooser chooser = getJFileChooser();
    chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
    Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, the_list);
    int option = chooser.showOpenDialog(frame);
    if (option == JFileChooser.APPROVE_OPTION) {
      FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
      File fil = chooser.getSelectedFile();
      try {
        WebLink.importWebLinks(fil);
      }
      catch (FileNotFoundException fe) {
        ErrorHandler.errorPanel("Error", "Error importing web links: File Not Found " + 
           fil.getAbsolutePath(), the_list, fe);
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel("Error", "Error importing web links", the_list, ex);
      }
    }
    enableActions();
  }

  void exportWebLinks() {
    Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, the_list);
    if (the_list.getModel().getSize() == 0) {
      ErrorHandler.errorPanel("Error", "No web links to save", frame);
      return;
    }
    JFileChooser chooser = getJFileChooser();
    chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
    int option = chooser.showSaveDialog(frame);
    if (option == JFileChooser.APPROVE_OPTION) {
      try {
        FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
        File fil = chooser.getSelectedFile();
        String full_path = fil.getCanonicalPath();

        if (! full_path.endsWith(".xml")) {
          fil = new File(full_path + ".xml");
        }
        WebLink.exportWebLinks(fil, false);
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel("Error", "Error exporting web links", frame, ex);
      }
    }
  }

  // initialize the static_panel early, because this will cause the accelerator
  // key-strokes to be configured early through the UnibrowPrefsUtil and thus
  // for them to be visible in the KeyStrokesView
  static WebLinksManagerView static_panel = new WebLinksManagerView();

  public static synchronized WebLinksManagerView getManager() {
    if (static_panel == null) {
      static_panel = new WebLinksManagerView();
    }
    return static_panel;
  }

  
  static JFrame static_frame = null;
  public static synchronized JFrame showManager() {
    if (static_frame == null) {
      static_frame = UnibrowPrefsUtil.createFrame("Web Links", getManager());
      ImageIcon icon = MenuUtil.getIcon("toolbarButtonGraphics/general/Search16.gif");
      if (icon != null) { static_frame.setIconImage(icon.getImage()); }
    }
    DisplayUtils.bringFrameToFront(static_frame);
    return static_frame;
  }

  public void destroy() {
    the_list.removeListSelectionListener(list_listener);
    the_list = null;
  }
  
  /** Main, for testing. */
  public static void main(String[] args) throws Exception {
    WebLink.autoLoad();
    JFrame frame = showManager();
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        WebLink.autoSave();
        System.exit(0);
      }
    });
  }
}
