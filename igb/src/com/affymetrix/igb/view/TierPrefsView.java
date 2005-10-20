/**
*   Copyright (c) 2005 Affymetrix, Inc.
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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.event.SymMapChangeEvent;
import com.affymetrix.igb.event.SymMapChangeListener;
import com.affymetrix.igb.util.TableSorter2;
import com.affymetrix.igb.tiers.AnnotStyle;
import com.affymetrix.swing.*;
import java.util.prefs.*;

/**
 *  A panel for choosing tier properties for the {@link SeqMapView}.
 */
public class TierPrefsView extends JPanel implements ListSelectionListener  {

  private final JTable table = new JTable();
  
  private static final String TIER_NAME = "Tier";
  private static final String COLOR = "Color";
  private static final String SHOW = "Show";
  private static final String SEPARATE = "Separate";
  private static final String COLLAPSED = "Collapsed";
  private static final String MAX_DEPTH = "Max Depth";
  private static final String BACKGROUND = "Background";
  private static final String GLYPH_DEPTH = "Glyph Depth";
  private static final String LABEL_FIELD = "Label Field";
  private static final String HUMAN_NAME = "Display Name";
  //private static final String PERSISTENT = "Persistent";  
  //private static final String ORDER = "Order";
  //private static final String GLYPH_FACTORY = "Glyph Factory";

  private final static String[] col_headings = {
    TIER_NAME, COLOR, SHOW, SEPARATE, COLLAPSED,
    MAX_DEPTH, BACKGROUND, GLYPH_DEPTH, LABEL_FIELD,
    HUMAN_NAME,
  };
  
  private final int COL_TIER_NAME = 0;
  private final int COL_COLOR = 1;
  private final int COL_SHOW = 2;
  private final int COL_SEPARATE = 3;
  private final int COL_COLLAPSED = 4;
  private final int COL_MAX_DEPTH = 5;
  private final int COL_BACKGROUND = 6;
  private final int COL_GLYPH_DEPTH = 7;
  private final int COL_LABEL_FIELD = 8;
  private final int COL_HUMAN_NAME = 9;

//  private final int COL_ORDER = -1; // unused
  private final int COL_GLYPH_FACTORY = -2; // unused
  private final int COL_PERSISTENT = -3; // unused
  
  private final TierPrefsTableModel model;
  private final ListSelectionModel lsm;

  JButton refresh_list_B = new JButton("Refresh List");
  
  JButton refresh_map_B = new JButton("Refresh Map");
  SeqMapView smv;
  //FooPanel annot_style_panel;
  
  public TierPrefsView() {
    this(true, true);
  }
  
  public TierPrefsView(boolean add_refresh_list_button, boolean add_refresh_map_button) {
    super();
    this.setLayout(new BorderLayout());

    
    AnnotStyle.getDefaultInstance(); // make sure at least the default instance exists;
    
    JScrollPane table_scroll_pane = new JScrollPane(table);
    //this.add(table_scroll_pane, BorderLayout.CENTER);

    
    //annot_style_panel = new FooPanel();
    //JScrollPane other_scroll_pane = new JScrollPane(annot_style_panel);
    
//    JSplitPane split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
//      table_scroll_pane, other_scroll_pane);
//    split_pane.setDividerLocation(0.5);
//    this.add(split_pane, BorderLayout.CENTER);
    this.add(table_scroll_pane, BorderLayout.CENTER);
    
    IGB igb = IGB.getSingletonIGB();
    if (igb != null) {
      smv = igb.getMapView();
    }

    // Add a "refresh map" button, iff there is an instance of IGB
    if (smv != null && add_refresh_map_button) {
      refresh_map_B.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          refreshSeqMapView();
        }
      });
      this.add(refresh_map_B, BorderLayout.SOUTH);
    }

    if (add_refresh_list_button) {
      this.add(refresh_list_B, BorderLayout.NORTH);
      refresh_list_B.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          refreshList();
        }
      });
    }
    
    model = new TierPrefsTableModel();
    model.addTableModelListener(new javax.swing.event.TableModelListener() {
      public void tableChanged(javax.swing.event.TableModelEvent e) {
//        resetStylePanel();
      }
    });

    lsm = table.getSelectionModel();
    lsm.addListSelectionListener(this);
    lsm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    TableSorter2 sort_model = new TableSorter2(model);
    sort_model.setTableHeader(table.getTableHeader());
    table.setModel(sort_model);

    table.setRowSelectionAllowed(true);
    table.setEnabled( true );
    
    table.setDefaultRenderer(Color.class, new ColorTableCellRenderer(true));
    table.setDefaultEditor(Color.class, new ColorTableCellEditor());
    table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());

    
    
//    ActionListener foo_panel_listener = new ActionListener() {
//      // This action listener should be activated when the "OK" button
//      // is pressed on the FooPanel
//      public void actionPerformed(ActionEvent e) {
//        model.fireTableDataChanged();
//      }
//    };
//    annot_style_panel.addOKListener(foo_panel_listener);

    validate();
    
    // setDividerLocation((double) 0.5) only works if the component is already showing.
//    split_pane.setDividerLocation(300);
  }

  public void setStyleList(java.util.List styles) {
    model.setStyles(styles);
    model.fireTableDataChanged();
  }
  
  /** Called when the user selects a row of the table.
   */
  public void valueChanged(ListSelectionEvent evt) {
    if (evt.getSource()==lsm && ! evt.getValueIsAdjusting()) {
//      resetStylePanel();
    }
  }

//  void resetStylePanel() {
//    if (annot_style_panel != null) {
//      int[] srows = table.getSelectedRows();
//
//      ArrayList list = new ArrayList(srows.length);
//      for (int i=0; i<srows.length; i++) {
//        AnnotStyle style = (AnnotStyle) model.tier_styles.get(srows[i]);
//        list.add(style);
//      }
//      annot_style_panel.setAnnotStyles(list);
//    }
//  }
  
  public void destroy() {
    removeAll();
    if (lsm != null) {lsm.removeListSelectionListener(this);}
  }

  void refreshSeqMapView() {
    if (smv != null) {
      smv.setAnnotatedSeq(smv.getAnnotatedSeq(), true, true);
    }
  }  
  
  void refreshList() {
    java.util.List styles = AnnotStyle.getAllLoadedInstances();
    this.setStyleList(styles);
  }
  
  class TierPrefsTableModel extends AbstractTableModel {
    
    java.util.List tier_styles;
    
    TierPrefsTableModel() {
      this.tier_styles = Collections.EMPTY_LIST;
    }
    
    public void setStyles(java.util.List tier_styles) {
      this.tier_styles = tier_styles;
    }
    
    public java.util.List getStyles() {
      return this.tier_styles;
    }
    
    public boolean isCellEditable(int row, int column) {
      return (column != COL_TIER_NAME && column != COL_PERSISTENT);
    }
    
    public Class getColumnClass(int c) {
      return getValueAt(0, c).getClass();
    }
    
    public int getColumnCount() {
      return col_headings.length;
    }
    
    public String getColumnName(int columnIndex) {
      return col_headings[columnIndex];
    }
    
    public int getRowCount() {
      return tier_styles.size();
    }
    
    public Object getValueAt(int row, int column) {
      AnnotStyle style = (AnnotStyle) tier_styles.get(row);
      switch (column) {
        case COL_COLOR: 
          return style.getColor();
        case COL_SEPARATE: 
          return Boolean.valueOf(style.getSeparate());
        case COL_COLLAPSED: 
          return Boolean.valueOf(style.getCollapsed());
        case COL_SHOW:  
          return Boolean.valueOf(style.getShow());
        case COL_TIER_NAME:
          String name = style.getUniqueName();
          if (! style.getPersistent()) { name = "<html><i>" + name + "</i></html>"; }
          return name;
//        case COL_ORDER: 
//          return String.valueOf(style.getSortOrder());
        case COL_MAX_DEPTH: 
          int md = style.getMaxDepth();
          if (md == 0) { return ""; }
          else { return String.valueOf(md); }
        case COL_BACKGROUND:
          return style.getBackground();
        case COL_GLYPH_DEPTH:
          return String.valueOf(style.getGlyphDepth());
//        case COL_GLYPH_FACTORY:
//          return style.getFactoryClassName();
        case COL_PERSISTENT:
          return Boolean.valueOf(style.getPersistent());
        case COL_LABEL_FIELD:
          return style.getLabelField();
        case COL_HUMAN_NAME:
          return style.getHumanName();
        default:
          return null;
      }
    }
    
    public void setValueAt(Object value, int row, int col) {
      try {
      AnnotStyle style = (AnnotStyle) tier_styles.get(row);
      switch (col) {
        case COL_COLOR:
          style.setColor((Color) value);
          break;
        case COL_SEPARATE:
          style.setSeparate(((Boolean) value).booleanValue());
          break;
        case COL_COLLAPSED:
          style.setCollapsed(((Boolean) value).booleanValue());
          break;
        case COL_SHOW:
          style.setShow(((Boolean) value).booleanValue());
          break;
        case COL_TIER_NAME:
          System.out.println("Tier name is not changeable!");
          break;
//        case COL_ORDER: 
//          {
//            int i = parseInteger(value, style.getSortOrder(), style.getSortOrder());
//            style.setSortOrder(i);
//          }
//        break;
        case COL_MAX_DEPTH: 
          {
            int i = parseInteger(((String) value), 0, style.getMaxDepth());
            style.setMaxDepth(i);
          }
          break;
        case COL_BACKGROUND:
          style.setBackground((Color) value);
          break;
        case COL_GLYPH_DEPTH: 
          {
            int i = parseInteger(((String) value), 0, style.getGlyphDepth());
            if (i<1) { i = 1; } else if (i>2) { i = 2; }
            style.setGlyphDepth(i);
          }
          break;
//        case COL_GLYPH_FACTORY:
//          style.setFactoryClassName((String) value);
//          break;
        case COL_PERSISTENT:
          break;
        case COL_LABEL_FIELD:
          style.setLabelField((String) value);
          break;
        case COL_HUMAN_NAME:
          style.setHumanName((String) value);
          break;
        default:
          System.out.println("Unknown column selected: " + col);;
      }
      fireTableCellUpdated(row, col);
      } catch (Exception e) {
        // exceptions should not happen, but must be caught if they do
        System.out.println("Exception in TierPrefsView.setValueAt(): " + e);
      }
    }
  
    /** Parse an integer, using the given fallback if any exception occurrs.
     *  @param s  The String to parse.
     *  @param empty_string  the value to return if the input is an empty string.
     *  @param fallback  the value to return if the input String is unparseable.
     */
    int parseInteger(String s, int empty_string, int fallback) {
      System.out.println("Parsing string: '" + s + "'");
      int i = fallback;
      try {
        if ("".equals(s.trim())) {i = empty_string; }
        else { i = Integer.parseInt(s); }
      }
      catch (Exception e) {
        System.out.println("Exception: " + e);
        // don't report the error, use the fallback value
      }
      return i;
    }
    
  };
  
  
  static JFrame static_frame;
  static TierPrefsView static_instance;
  
  static final String WINDOW_NAME = "Tier Customizer";
  
  /**
   *  Gets an instance of TierPrefsView wrapped in a JFrame, useful
   *  as a pop-up dialog for setting annotatin styles. 
   */
  public static JFrame showFrame() {
    if (static_frame == null) {
      static_frame = new JFrame(WINDOW_NAME);
      static_instance = new TierPrefsView(false, false);
      static_frame.getContentPane().add(static_instance);

      static_frame.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          static_instance.refreshSeqMapView();
          // save window size
        }
      });
      static_frame.pack();
      // restore saved window size
    }
    
    static_instance.refreshList();
    
    
    static_frame.show();
    return static_frame;
  }
  
  /** Used for testing.  Opens a window with the TierPrefsView in it. */
  public static void main(String[] args) {

    TierPrefsView t = new TierPrefsView();

    AnnotStyle.getInstance("RefSeq");
    AnnotStyle.getInstance("EnsGene");
    AnnotStyle.getInstance("Contig");
    AnnotStyle.getInstance("KnownGene");
    AnnotStyle.getInstance("TwinScan", false);
    
    t.setStyleList(AnnotStyle.getAllLoadedInstances());
    
    JFrame f = new JFrame(WINDOW_NAME);
    f.getContentPane().add(t);
    
    f.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
        System.exit(0);
      }
    });
    f.pack();
    
    f.setSize(800, 800);
    f.show();
  }
}


