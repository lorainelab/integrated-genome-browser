/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import java.awt.*;
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

//import com.affymetrix.igb.util.TableSorter;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.view.PluginInfo;
import java.awt.event.ActionEvent;

/**
 *  A panel that shows the preferences mapping between KeyStroke's and Actions. 
 */
public class PluginsView extends JPanel implements ListSelectionListener, NodeChangeListener, IPrefEditorComponent  {

  private final JTable table = new JTable();
  private final static String[] col_headings = {"Name", "Class", "Enabled"};
  private final DefaultTableModel model;
  private final ListSelectionModel lsm; 
 
  String chosen_name = null;

  Action add_action;
  //Action edit_action;
  Action toggle_action;
  Action remove_action;

  PluginEditor editor;

  public PluginsView() {
    super();
    this.setName("Plugins");
    this.setLayout(new BorderLayout());

    JScrollPane scroll_pane = new JScrollPane(table);
    this.add(scroll_pane, BorderLayout.CENTER);
    this.add(scroll_pane);

    model = new DefaultTableModel() {
      public boolean isCellEditable(int row, int column) {return false;}
      public Class getColumnClass(int column) {
        if (column == 2) {return Boolean.class;}
        else {return String.class;}
      }
    };
    model.setDataVector(new Object[0][0], col_headings);

    lsm = table.getSelectionModel();
    lsm.addListSelectionListener(this);
    lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    //TableSorter sort_model = new TableSorter(model);
    //sort_model.addMouseListenerToHeaderInTable(table);

    table.setModel(model);
    //table.setModel(sort_model);
    table.setRowSelectionAllowed(true);
    table.setEnabled( true );

    add_action = new AbstractAction("Add ...") {
      public void actionPerformed(ActionEvent evt) {
        performAdd();
      }
    };
    /*
    edit_action = new AbstractAction("Edit ...") {
      public void actionPerformed(ActionEvent evt) {
        if (chosen_name == null) { // shouldn't happen
          this.setEnabled(false);
        } else {
          performEdit(chosen_name);
        }
      }
    };
     */
    toggle_action = new AbstractAction("Toggle") {
      public void actionPerformed(ActionEvent evt) {
        if (chosen_name == null) { // shouldn't happen
          this.setEnabled(false);
        } else {
          performToggle(chosen_name);
        }
      }
    };
    remove_action = new AbstractAction("Remove ...") {
      public void actionPerformed(ActionEvent evt) {
        if (chosen_name == null) { // shouldn't happen
          this.setEnabled(false);
        } else {
          performRemove(chosen_name);
        }
      }
    };
    
    add_action.setEnabled(true);
    //edit_action.setEnabled(false);
    toggle_action.setEnabled(false);
    remove_action.setEnabled(false);
    
    Box button_box = new Box(BoxLayout.X_AXIS);
    button_box.add(Box.createHorizontalGlue());
    button_box.add(new JButton(add_action));
    button_box.add(Box.createHorizontalStrut(5));
    //button_box.add(new JButton(edit_action));
    //button_box.add(Box.createHorizontalStrut(5));
    button_box.add(new JButton(toggle_action));
    button_box.add(Box.createHorizontalStrut(5));
    button_box.add(new JButton(remove_action));
    button_box.add(Box.createHorizontalGlue());
    this.add("South", button_box);
    
    getPreferencesNode().addNodeChangeListener(this);

    refresh();
    validate();
    
    editor = new PluginEditor(this);
  }

  /*
  public void performEdit(String name) {
    Preferences node = PluginInfo.getNodeForName(name);
    if (node != null) {
      editor.showDialog(node);
    } else {
      ErrorHandler.errorPanel("ERROR", "No preferences node for name: \n"+name);
    }
    refresh();
  }
  */

  public void performToggle(String name) {
    Preferences node = PluginInfo.getNodeForName(name);
    if (node != null) {
      boolean current_value = node.getBoolean(PluginInfo.KEY_LOAD, true);
      node.putBoolean(PluginInfo.KEY_LOAD, ! current_value);
    } else {
      ErrorHandler.errorPanel("ERROR", "No preferences node for name: \n"+name);
    }
    refresh();
  }
  
  public void performRemove(String name) {
    if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
      this, "Really remove this Plugin?", "Confirm", JOptionPane.OK_CANCEL_OPTION)) {
      Preferences node = PluginInfo.getNodeForName(name);
      if (node != null) {
        try {
          node.removeNode();
        } catch (BackingStoreException e) {
          UnibrowPrefsUtil.handleBSE(this, e);
        }
      } else {
        ErrorHandler.errorPanel("ERROR", "No preferences node for name: \n"+name);
      }
      refresh();
    }
  }
  
  public void performAdd() {
    editor.showDialog(null);
    refresh();
  }
  
  public static Preferences getPreferencesNode() {
    return UnibrowPrefsUtil.getTopNode().node(PluginInfo.NODE_PLUGINS);
  }
  
  Preferences[] nodes = new Preferences[0];

  protected Object[][] buildRows(Preferences top_node) throws BackingStoreException {
    // builds the table 'rows' and the array 'nodes'

    java.util.List plugins = PluginInfo.getAllPlugins();
    
    int num_rows = plugins.size();
    int num_cols = 3;
    Object[][] rows = new Object[num_rows][num_cols];
    for (int i = 0 ; i < num_rows ; i++) {
      PluginInfo pi = (PluginInfo) plugins.get(i);
      rows[i][0] = pi.getPluginName();
      rows[i][1] = pi.getClassName();
      rows[i][2] = Boolean.valueOf(pi.shouldLoad());
    }
    return rows;
  }
  
  /** Re-populates the table with the preferences data. */
  public void refresh() {
    Object[][] rows = null;
    try {
      rows = buildRows(getPreferencesNode());
      model.setDataVector(rows, col_headings);
    } catch (BackingStoreException bse) {
      UnibrowPrefsUtil.handleBSE(this, bse);
    }
  }
    
  /** This is called when a row of the table is selected. */
  public void valueChanged(ListSelectionEvent evt) {
    if (evt.getSource()==lsm && ! evt.getValueIsAdjusting()) {
      int srow = table.getSelectedRow();
      if (srow >= 0) {
        chosen_name = (String) table.getModel().getValueAt(srow, 0);
        //edit_action.setEnabled(true);
        toggle_action.setEnabled(true);
        remove_action.setEnabled(true);
      } else {
        //edit_action.setEnabled(false);
        toggle_action.setEnabled(false);
        remove_action.setEnabled(false);
      }
    }
  }
  
  public void childAdded(NodeChangeEvent evt) {
    // update the whole table.  Inelegant, but works.
    refresh();
  }
  
  public void childRemoved(NodeChangeEvent evt) {
    // update the whole table.  Inelegant, but works.
    refresh();
  }

  public String getHelpTextHTML() {
    StringBuffer sb = new StringBuffer();
    sb.append("<h1>Preferences</h1>\n");
    sb.append("<p>\n");
    sb.append("Use this panel to choose which plugins should be loaded at start-up.  ");
    sb.append("Changes take effect when you re-start the program.  ");
    sb.append("Note that some plugins, such as QuickLoad, cannot be disabled or removed.  ");
    sb.append("</p>\n");
    
    sb.append("<h2>Turning a plugin on or off</h2>\n");
    sb.append("<p>\n");
    sb.append("Press the \'toggle\' button to \'enable\' or \'disable\' a plugin.  ");
    sb.append("The change will take effect when you re-start the program. ");
    sb.append("(There may be some plugins which cannot be disabled.)  ");
    sb.append("</p>\n");
    
    sb.append("<h2>Adding a plugin</h2>\n");
    sb.append("<p>\n");
    sb.append("You can add a plugin by pressing the \'Add\' button.  ");
    sb.append("You must enter the full name of the java class,  ");
    sb.append("and that class must be included in your java class path.  ");
    sb.append("You may give the plugin any name you wish, but each plugin must have a separate name.  ");
    sb.append("You can have multiple instances of the same plugin, but you must give each a unique name.  ");
    sb.append("Any plugin that extends \'JComponent\' will be added to the tabs in the main window.  ");
    sb.append("</p>\n");
    
    sb.append("<h2>Removing a plugin</h2>\n");
    sb.append("<p>\n");
    sb.append("If no longer need a plugin that you added earlier, you can remove it.  ");
    sb.append("The default plugins cannot be removed, but can be disabled.  ");
    sb.append("</p>\n");
    
    return sb.toString();
  }
  
  public Icon getIcon() {
    return null;
  }
  
  public String getToolTip() {
    return "Manage Plugins";
  }  

  public void destroy() {
    removeAll();
    if (lsm != null) {lsm.removeListSelectionListener(this);}
    getPreferencesNode().removeNodeChangeListener(this);
  }

  /** A main method for testing. */
  public static void main(String[] args) throws Exception {
    PluginsView p = new PluginsView();
   
    JDialog d = new JDialog();
    d.setTitle(p.getName());
    d.getContentPane().add(p);
    d.pack();
    
    d.setVisible(true);
    d.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
        System.exit(0);
      }
    }
    );
  }
  
  public String getInfoURL() {
    return null;
  }
  
}
