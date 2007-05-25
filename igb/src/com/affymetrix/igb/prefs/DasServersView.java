/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import com.affymetrix.igb.das.DasDiscovery;
import com.affymetrix.igb.util.TableSorter2;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.swing.BooleanTableCellRenderer;
import com.affymetrix.swing.DisplayUtils;

/**
 *  A panel that shows the preferences mapping between KeyStroke's and Actions. 
 */
public class DasServersView extends JPanel implements ListSelectionListener, NodeChangeListener, IPrefEditorComponent  {

  private final JTable table = new JTable();
  private final static String[] col_headings = {"Enabled", "Name", "URL"};
  private final static int COL_ENABLED = 0;
  private final static int COL_NAME = 1;
  private final static int COL_URL = 2;
  
  
  private final DefaultTableModel model;
  private final ListSelectionModel lsm; 
 
  String chosen_url = null;

  Action add_action;
  Action edit_action;
  Action remove_action;

  DasServerInfoEditor editor;

  public DasServersView() {
    super();
    this.setName("DAS/1 Servers");
    this.setLayout(new BorderLayout());

    JScrollPane scroll_pane = new JScrollPane(table);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    this.add(scroll_pane, BorderLayout.CENTER);
    this.add(scroll_pane);

    model = new DefaultTableModel() {
      public boolean isCellEditable(int row, int column) {
        return (column == COL_ENABLED);
      }
      public Class getColumnClass(int column) {
        if (column == COL_NAME) {return Preferences.class;}
        if (column == COL_ENABLED) {return Boolean.class;}
        else {return String.class;}
      }
      public void setValueAt(Object value, int row, int column) {
        if (column == COL_ENABLED) {
          Boolean b = (Boolean) value;
          Preferences node = (Preferences) getValueAt(row, COL_NAME);
          node.putBoolean(DasDiscovery.KEY_ENABLED, b.booleanValue());
          refresh();
        }
      }
      public void fireTableStructureChanged() {
        // The columns never change, so suppress tableStructureChanged events
        // converting to normal table-rows-changed-type events.
        // This allows the column-order and column-based sorting settings to be preserved when
        // the data changes.
        fireTableChanged(new javax.swing.event.TableModelEvent(this));
      }
    };
    model.setDataVector(new Object[0][0], col_headings);

    lsm = table.getSelectionModel();
    lsm.addListSelectionListener(this);
    lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    TableSorter2 sort_model = new TableSorter2(model);
    //sort_model.addMouseListenerToHeaderInTable(table);
    sort_model.setTableHeader(table.getTableHeader());
    sort_model.setColumnComparator(Preferences.class, new Comparator() {
      public int compare(Object o1, Object o2) {
        String s1 = ((Preferences) o1).get(DasDiscovery.KEY_NAME, "");
        String s2 = ((Preferences) o2).get(DasDiscovery.KEY_NAME, "");
        return s1.compareTo(s2);
      }
    });

    table.setModel(model);
    table.setModel(sort_model);
    table.setRowSelectionAllowed(true);
    table.setEnabled( true );
    table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
    table.setDefaultRenderer(Preferences.class, special_renderer);

    add_action = new AbstractAction("Add ...") {
      public void actionPerformed(ActionEvent evt) {
        performAdd();
      }
    };
    edit_action = new AbstractAction("Edit ...") {
      public void actionPerformed(ActionEvent evt) {
        if (chosen_url == null) { // shouldn't happen
          this.setEnabled(false);
        } else {
          performEdit(chosen_url);
        }
      }
    };
    remove_action = new AbstractAction("Remove ...") {
      public void actionPerformed(ActionEvent evt) {
        if (chosen_url == null) { // shouldn't happen
          this.setEnabled(false);
        } else {
          performRemove(chosen_url);
        }
      }
    };
    
    add_action.setEnabled(true);
    edit_action.setEnabled(false);
    remove_action.setEnabled(false);
    
    Box button_box = new Box(BoxLayout.X_AXIS);
    button_box.add(Box.createHorizontalGlue());
    button_box.add(new JButton(add_action));
    button_box.add(Box.createHorizontalStrut(5));
    button_box.add(new JButton(edit_action));
    button_box.add(Box.createHorizontalStrut(5));
    button_box.add(new JButton(remove_action));
    button_box.add(Box.createHorizontalGlue());
    button_box.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    this.add("South", button_box);
    
    DasDiscovery.getPreferencesNode().addNodeChangeListener(this);

    refresh();
    validate();
    
    DisplayUtils.adjustColumnPreferredWidths(table, true);
    
    editor = new DasServerInfoEditor(this);
    sort_model.setSortingStatus(COL_NAME, TableSorter2.ASCENDING);
  }

  TableCellRenderer special_renderer = new DefaultTableCellRenderer() {
    public Component getTableCellRendererComponent(JTable table, Object value, 
        boolean isSelected, boolean hasFocus, int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      String name = ((Preferences) value).get(DasDiscovery.KEY_NAME, "<Unnamed DAS Server>");
      ((DefaultTableCellRenderer) c).setText(name);
      return c;
    }
  };
  
  
  public void performEdit(String url) {
    //System.out.println("Edit: "+url);
    Preferences node = DasDiscovery.getNodeForURL(url, false);
    if (node != null) {
      editor.showDialog(node);
    } else {
      ErrorHandler.errorPanel("ERROR", "No preferences node for URL: \n"+url);
    }
    refresh();
  }
  
  public void performRemove(String url) {
    if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
      this, "Really remove this URL?", "Confirm", JOptionPane.OK_CANCEL_OPTION)) {
      //System.out.println("Remove: "+chosen_url);
      Preferences node = DasDiscovery.getNodeForURL(url, false);
      if (node != null) {
        try {
          node.removeNode();
        } catch (BackingStoreException e) {
          UnibrowPrefsUtil.handleBSE(this, e);
        }
      } else {
        ErrorHandler.errorPanel("ERROR", "No preferences node for URL: \n"+url);
      }
      refresh();
    }
  }
  
  public void performAdd() {
    editor.showDialog(null);
    refresh();
  }
    
  Preferences[] nodes = new Preferences[0];

  protected Object[][] buildRows(Preferences top_node) throws BackingStoreException {
    // builds the table 'rows' and the array 'nodes'

    String[] node_names = top_node.childrenNames();
    
    int num_rows = node_names.length;
    int num_cols = 3;
    Object[][] rows = new Object[num_rows][num_cols];
    for (int i = 0 ; i < num_rows ; i++) {
      Preferences kid = top_node.node(node_names[i]);
      String the_url = kid.get(DasDiscovery.KEY_URL, "???");
      rows[i][COL_URL] = the_url;
      rows[i][COL_NAME] = kid;
//      rows[i][1] = kid.get(DasDiscovery.KEY_NAME, "<Unnamed DAS Server>");
      boolean default_enabled = ! the_url.equals("???");
      rows[i][COL_ENABLED] = Boolean.valueOf(kid.getBoolean(DasDiscovery.KEY_ENABLED, default_enabled));
    }
    return rows;
  }
  
  /** Re-populates the table with the preferences data. */
  public void refresh() {
    Object[][] rows = null;
    TableSorter2 sort_model = (TableSorter2) table.getModel();
    try {
      //System.out.println("I am refreshing now!");
      rows = buildRows(DasDiscovery.getPreferencesNode());
      model.setDataVector(rows, col_headings);
    } catch (BackingStoreException bse) {
      UnibrowPrefsUtil.handleBSE(this, bse);
    }
    sort_model.setSortingStatus(COL_NAME, TableSorter2.ASCENDING);
    DasDiscovery.reset(); //TODO: I hate this harsh way of handling a reset
  }
    
  /** This is called when the user selects a row of the table;
   */
  public void valueChanged(ListSelectionEvent evt) {
    boolean old_way = true;
    if (evt.getSource()==lsm && ! evt.getValueIsAdjusting()) {
      int srow = table.getSelectedRow();
      if (srow >= 0) {
        chosen_url = (String) table.getModel().getValueAt(srow, COL_URL);
        edit_action.setEnabled(true);
        remove_action.setEnabled(true);
      } else {
        edit_action.setEnabled(false);
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

    sb.append("<h1>DAS Servers</h1>\n");
    sb.append("<p>\n");
    sb.append("Add or edit locations and names of DAS servers here.  ");
    sb.append("Changes and additions will take effect immediately.  ");
    sb.append("</p>\n");
    
    sb.append("<p>\n");
    sb.append("You can add, re-name, disable, or delete a DAS server URL.  ");
    sb.append("Although DAS servers may have 'official' names, you can assign other names for convenience.  ");
    sb.append("You can disable servers that you aren't interested in to keep the number of choices presented to you managable.  ");
    sb.append("You can re-enable the server if you change your mind.  ");
    sb.append("</p>\n");
    
    sb.append("<p>\n");
    sb.append("Use this panel only for servers implementing the DAS/1 specification.  \n");
    sb.append("Do not enter the URL of any DAS/2 server here.  ");
    sb.append("For details of these DAS specifications, please see biodas.org  \n");
    sb.append("</p>\n");
    
    sb.append("<h2>Deleting vs Disabling</h2>\n");
    sb.append("<p>\n");
    sb.append("The program will discover the URLs of some DAS servers automatically.  ");
    sb.append("For example, it can learn about DAS servers from communicating with a QuickLoad server.  ");
    sb.append("If you delete one of these servers, you may find that it gets added again when the program discovers it again later.  ");
    sb.append("If you wish to ignore such a DAS server, you can disable it.  ");
    sb.append("Deleting is primarily useful for removing servers you added yourself.  ");
    sb.append("</p>\n");
    return sb.toString();
  }
  
  public Icon getIcon() {
    return null;
  }
  
  public String getToolTip() {
    return "Edit Locations";
  }  

  public void destroy() {
    removeAll();
    if (lsm != null) {lsm.removeListSelectionListener(this);}
    DasDiscovery.getPreferencesNode().removeNodeChangeListener(this);
  }

  /** A main method for testing. */
  public static void main(String[] args) throws Exception {
    DasServersView p = new DasServersView();
   
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
