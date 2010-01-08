/**
*   Copyright (c) 2005-2006 Affymetrix, Inc.
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

import com.affymetrix.igb.Application;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.affymetrix.igb.prefs.IPrefEditorComponent;
import com.affymetrix.igb.tiers.AnnotStyle;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.genoviz.swing.*;
import com.affymetrix.genometryImpl.style.IAnnotStyle;

/**
 *  A panel for choosing tier properties for the {@link SeqMapView}.
 */
public final class TierPrefsView extends JPanel implements ListSelectionListener, IPrefEditorComponent, WindowListener  {

  private final JTable table = new JTable();

  private static final String TIER_NAME = "Tier ID";
  private static final String COLOR = "Color";
  private static final String SEPARATE = "2 Tiers";
  private static final String COLLAPSED = "Collapsed";
  private static final String MAX_DEPTH = "Max Depth";
  private static final String BACKGROUND = "Background";
  private static final String GLYPH_DEPTH = "Connected";
  private static final String LABEL_FIELD = "Label Field";
  private static final String HUMAN_NAME = "Display Name";

  private final static String[] col_headings = {
    HUMAN_NAME,
    COLOR, BACKGROUND,
    SEPARATE, COLLAPSED,
    MAX_DEPTH, GLYPH_DEPTH, LABEL_FIELD, TIER_NAME,
    //    GRAPH_TIER,
  };

  private static final int COL_HUMAN_NAME = 0;
  private static final int COL_COLOR = 1;
  private static final int COL_BACKGROUND = 2;
  private static final int COL_SEPARATE = 3;
  private static final int COL_COLLAPSED = 4;
  private static final int COL_MAX_DEPTH = 5;
  private static final int COL_GLYPH_DEPTH = 6;
  private static final int COL_LABEL_FIELD = 7;
  private static final int COL_TIER_NAME = 8;

  private final TierPrefsTableModel model;
  private final ListSelectionModel lsm;
  private final TableRowSorter<TierPrefsTableModel> sorter;

  private static final String PREF_AUTO_REFRESH = "Auto-Apply Tier Customizer Changes";
  private static final boolean default_auto_refresh = true;
  private JCheckBox auto_refresh_CB;

  private static final String REFRESH_LIST = "Refresh List";
  private final JButton refresh_list_B = new JButton(REFRESH_LIST);

  private static final String REFRESH_MAP = "Refresh Map";
  private final JButton refresh_map_B = new JButton(REFRESH_MAP);

  private static final String AUTO_REFRESH = "Auto Refresh";
  private static final String APPLY_DEFAULT_BG = "Apply Default Background";

  private SeqMapView smv;

  private static AnnotStyle default_annot_style = AnnotStyle.getDefaultInstance(); // make sure at least the default instance exists;

  public TierPrefsView() {
    this(false, true);
  }

  public TierPrefsView(boolean add_refresh_list_button, boolean add_refresh_map_button) {
    super();
    this.setLayout(new BorderLayout());

    JScrollPane table_scroll_pane = new JScrollPane(table);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    this.add(table_scroll_pane, BorderLayout.CENTER);
    JPanel button_panel = new JPanel();
    button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.X_AXIS));

    JButton apply_bg_button = new JButton(APPLY_DEFAULT_BG);
    button_panel.add(Box.createHorizontalGlue());
    button_panel.add(apply_bg_button);
    apply_bg_button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        copyDefaultBG();
      }
    });


    Application igb = Application.getSingleton();
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
      button_panel.add(Box.createHorizontalStrut(10));
      button_panel.add(refresh_map_B);

      auto_refresh_CB = UnibrowPrefsUtil.createCheckBox(AUTO_REFRESH,
        UnibrowPrefsUtil.getTopNode(), PREF_AUTO_REFRESH, default_auto_refresh);
      auto_refresh_CB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          if (refresh_map_B != null) {
            refresh_map_B.setEnabled(! auto_refresh_CB.isSelected());
            if (auto_refresh_CB.isSelected()) {
              refreshSeqMapView();
            }
          }
        }
      });
      refresh_map_B.setEnabled(! auto_refresh_CB.isSelected());
      button_panel.add(Box.createHorizontalStrut(10));
      button_panel.add(auto_refresh_CB);
    }

    if (add_refresh_list_button) {
      button_panel.add(Box.createHorizontalStrut(10));
      button_panel.add(refresh_list_B);
      //this.add(refresh_list_B, BorderLayout.SOUTH);
      refresh_list_B.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          refreshList();
        }
      });
    }
    button_panel.add(Box.createHorizontalGlue());
    this.add(button_panel, BorderLayout.SOUTH);

    model = new TierPrefsTableModel();
    model.addTableModelListener(new javax.swing.event.TableModelListener() {
      public void tableChanged(javax.swing.event.TableModelEvent e) {
        // do nothing.
      }
    });

    lsm = table.getSelectionModel();
    lsm.addListSelectionListener(this);
    lsm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

	sorter = new TableRowSorter<TierPrefsTableModel>(model);

    table.setModel(model);
	table.setRowSorter(sorter);

    table.getColumnModel().getColumn(COL_TIER_NAME).setPreferredWidth(150);
    table.getColumnModel().getColumn(COL_HUMAN_NAME).setPreferredWidth(150);

    table.setRowSelectionAllowed(true);
    table.setEnabled( true ); // doesn't do anything ?

    table.setDefaultRenderer(Color.class, new ColorTableCellRenderer(true));
    table.setDefaultEditor(Color.class, new ColorTableCellEditor());
    table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());

    validate();
  }

  public void applyChanges() {
    refreshSeqMapView();
  }

  public void setStyleList(List styles) {
    model.setStyles(styles);
    model.fireTableDataChanged();
  }

  /** Called when the user selects a row of the table.
   */
  public void valueChanged(ListSelectionEvent evt) {
    if (evt.getSource()==lsm && ! evt.getValueIsAdjusting()) {
    }
  }

  public void destroy() {
    removeAll();
    if (lsm != null) {lsm.removeListSelectionListener(this);}
  }

  private void refreshSeqMapView() {
    if (smv != null) {
      smv.setAnnotatedSeq(smv.getAnnotatedSeq(), true, true, true);
    }
  }

  void refreshList() {
    boolean only_displayed_tiers = true;
    boolean include_graph_styles = false;
    List<AnnotStyle> styles;
    // if only_displayed_tiers, then only put AnnotStyles in table that are being used in tiers currently displayed in main view
    if (only_displayed_tiers) {
      styles = new ArrayList<AnnotStyle>();
      styles.add(default_annot_style);
      if (smv != null) {  
	List<TierGlyph> tiers = smv.getSeqMap().getTiers();
	LinkedHashMap<AnnotStyle,AnnotStyle> stylemap = new LinkedHashMap<AnnotStyle,AnnotStyle>();
	Iterator titer = tiers.iterator();
	while (titer.hasNext()) {
	  TierGlyph tier = (TierGlyph)titer.next();
	  IAnnotStyle style = tier.getAnnotStyle();
	  if ((style instanceof AnnotStyle) &&
	      (style.getShow()) && 
	      (tier.getChildCount() > 0) ) {
	    stylemap.put((AnnotStyle)style, (AnnotStyle)style);
	  }
	}
	styles.addAll(stylemap.values());
      }
    }
    else { styles = AnnotStyle.getAllLoadedInstances(); }
    ArrayList<AnnotStyle> customizables = new ArrayList<AnnotStyle>(styles.size());
    for (int i=0; i<styles.size(); i++) {
      AnnotStyle the_style = styles.get(i);
      if (the_style.getCustomizable()) {
	// if graph tier style then only include if include_graph_styles toggle is set (app is _not_ IGB)
	if ((! the_style.isGraphTier()) || include_graph_styles) {
	  customizables.add(the_style);
	}
      }
    }
    this.setStyleList(customizables);
  }

  // Copy the background color from the default style to all loaded styles.
  void copyDefaultBG() {
    Iterator iter = AnnotStyle.getAllLoadedInstances().iterator();
    while (iter.hasNext()) {
      AnnotStyle as = (AnnotStyle) iter.next();
      as.setBackground(default_annot_style.getBackground());
    }
    table.repaint(); // table needs to redraw itself due to changed values
    if (autoApplyChanges()) {
      refreshSeqMapView();
    }
  }

  /**
   *  Call this whenver this component is removed from the view, due to the
   *  tab pane closing or the window closing.  It will decide whether it is
   *  necessary to update the SeqMapView in response to changes in settings
   *  in this panel.
   */
  public void removedFromView() {
    // if autoApplyChanges(), then the changes were already applied,
    // otherwise apply changes as needed.
    if (! autoApplyChanges()) {
      SwingUtilities.invokeLater( new Runnable() {
        public void run() {
          applyChanges();
        }
      });
    }
  }


  /** Whether or not changes to the table should automatically be
   *  applied to the view.
   */
  boolean autoApplyChanges() {
    boolean auto_apply_changes = true;
    if (auto_refresh_CB == null) {
      auto_apply_changes = true;
    } else {
      auto_apply_changes = UnibrowPrefsUtil.getBooleanParam(
        PREF_AUTO_REFRESH, default_auto_refresh);
    }
    return auto_apply_changes;
  }

  class TierPrefsTableModel extends AbstractTableModel {

    List tier_styles;

    TierPrefsTableModel() {
      this.tier_styles = Collections.EMPTY_LIST;
    }

    public void setStyles(List tier_styles) {
      this.tier_styles = tier_styles;
    }

    public List getStyles() {
      return this.tier_styles;
    }

    // Allow editing most fields in normal rows, but don't allow editing some
    // fields in the "default" style row.
    public boolean isCellEditable(int row, int column) {
      if (tier_styles.get(row) == default_annot_style) {
        if (column == COL_COLOR || column == COL_BACKGROUND || column == COL_SEPARATE
            || column == COL_COLLAPSED || column == COL_MAX_DEPTH) {
          return true;
        }
        else {
          return false;
        }
      } else {
        return (column != COL_TIER_NAME);
      }
    }

    public Class getColumnClass(int c) {
      Object val = getValueAt(0, c);
      if (val == null) {
        return Object.class;
      } else {
        return val.getClass();
      }
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
        case COL_TIER_NAME:
          String name = style.getUniqueName();
	  if (name == null) { name = ""; }
          if (! style.getPersistent()) { name = "<html><i>" + name + "</i></html>"; }
          return name;
        case COL_MAX_DEPTH:
          int md = style.getMaxDepth();
          if (md == 0) { return ""; }
          else { return String.valueOf(md); }
        case COL_BACKGROUND:
          return style.getBackground();
        case COL_GLYPH_DEPTH:
          return (style.getGlyphDepth()==2 ? Boolean.TRUE : Boolean.FALSE);
        case COL_LABEL_FIELD:
          return style.getLabelField();
        case COL_HUMAN_NAME:
          if (style == default_annot_style) { return "* default *"; }
          else { return style.getHumanName(); }
	//        case COL_GRAPH_TIER:
	//	  return style.isGraphTier();
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
        case COL_TIER_NAME:
          System.out.println("Tier name is not changeable!");
          break;
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
          if (Boolean.TRUE.equals(value)) {
            style.setGlyphDepth(2);
          } else {
            style.setGlyphDepth(1);
          }
          break;
        case COL_LABEL_FIELD:
          style.setLabelField((String) value);
          break;
        case COL_HUMAN_NAME:
	  if (style != default_annot_style) {
	    style.setHumanName((String) value);
	  }
          break;
        default:
          System.out.println("Unknown column selected: " + col);;
      }
      fireTableCellUpdated(row, col);
      } catch (Exception e) {
        // exceptions should not happen, but must be caught if they do
        System.out.println("Exception in TierPrefsView.setValueAt(): " + e);
      }

      if (autoApplyChanges()) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            applyChanges();
          }
        });
      }
    }

    /** Parse an integer, using the given fallback if any exception occurrs.
     *  @param s  The String to parse.
     *  @param empty_string  the value to return if the input is an empty string.
     *  @param fallback  the value to return if the input String is unparseable.
     */
    int parseInteger(String s, int empty_string, int fallback) {
      //System.out.println("Parsing string: '" + s + "'");
      int i = fallback;
      try {
        if ("".equals(s.trim())) {i = empty_string; }
        else { i = Integer.parseInt(s); }
      }
      catch (Exception e) {
        //System.out.println("Exception: " + e);
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
   *  as a pop-up dialog for setting annotation styles.
   */
  /*public static JFrame showFrame() {
    if (static_frame == null) {
      static_frame = new JFrame(WINDOW_NAME);
      static_instance = new TierPrefsView(false, false);
      static_frame.getContentPane().add(static_instance);

      static_frame.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          //static_instance.refreshSeqMapView();
          // save window size
        }
      });
      static_frame.pack();
      // restore saved window size
    }

    static_instance.refreshList();


    static_frame.setVisible(true);
    return static_frame;
  }*/

  /** Used for testing.  Opens a window with the TierPrefsView in it. */
  /*public static void main(String[] args) {

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
    f.setVisible(true);
  }*/

  public String getName() {
    return "Tiers";
  }

  public String getHelpTextHTML() {
    StringBuffer sb = new StringBuffer();

    sb.append("<h1>" + this.getName() + "</h1>\n");
    sb.append("<p>\n");
    sb.append("Use this panel to change properties of annotation tiers.  ");
    sb.append("Changes do not require re-start, and settings for most annotation types ");
    sb.append("will be remembered between sessions.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>Default Row</h2>\n");
    sb.append("Properties on the default row apply when loading annotation types ");
    sb.append("for which you have not previously set specific properties.  Once properties have been set for ");
    sb.append("any annotation type, those settings will be remembered and used instead of the default properties. ");
    sb.append("Certain properties of the default row, such as "+HUMAN_NAME+", are not editable, since ");
    sb.append("it would not be desirable for all annotation types to be given the same "+HUMAN_NAME+".");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>"+TIER_NAME+"</h2>\n");
    sb.append("The name of the tier.  ");
    sb.append("If the name is shown in <i>italic</i> font, the settings for that tier ");
    sb.append("will apply only to the current session.  ");
    sb.append("If the tier name is shown in normal font, the settings will persist between sessions.  ");
    sb.append("Non-persistent settings are used for graphs and temporary data resulting from arithmetic manipulations,  ");
    sb.append("such as intersections and unions of tiers.  ");
    //sb.append("Such settings that are not remembered between sessions are indicated by a tier name in <i>italics</i>.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>"+HUMAN_NAME+"</h2>\n");
    sb.append("Sets the name to display as the tier label.  By default, this is the same as "+TIER_NAME+", ");
    sb.append("but you can change the displayed name if you wish.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>Color and Background</h2>\n");
    sb.append("Sets the foreground and background colors.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>"+SEPARATE+"</h2>\n");
    sb.append("Whether to display annotations in two tiers (+) and (-), or one (+/-).  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>"+COLLAPSED+"</h2>\n");
    sb.append("Whether to collapse the tier to its minimum height.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>"+MAX_DEPTH+"</h2>\n");
    sb.append("The maximum rows of annotations to show in tiers that are <em>not</em> collapsed.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>"+GLYPH_DEPTH+"</h2>\n");
    sb.append("Whether to connect groups of exons into transcripts.  ");
    sb.append("Should be false for data with no intron-exon structure,  ");
    sb.append("such as repeats or contigs.  ");
    sb.append("Setting this to false for items that <em>do</em> have intron-exon structure ");
    sb.append("may affect whether labels are displayed.  See below.");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>"+LABEL_FIELD+"</h2>\n");
    sb.append("The name of the field to use to construct labels.  ");
    sb.append("For most annotation types you may choose to use 'id' to turn labels on ");
    sb.append("or leave this blank to turn off labels. ");
    sb.append("Turning labels off reduces the memory required by the program.  ");
    sb.append("Not all annotation types have 'id' fields, so it isn't possible to use 'id' for all types.  ");
    sb.append("Features in some annotation types have additional properties that you may choose to use for a label.  ");
    sb.append("For example, with 'RefSeq' data, you may choose to use 'gene name'.  ");
    sb.append("Check in the 'Selection Info' window to find the names of the properties for items of any type.  ");
    sb.append("</p><p>");
    sb.append("Note that when "+GLYPH_DEPTH+" is true, the label is constructed from the property value of the whole transcript.");
    sb.append("When false, the label is constructed from the property values of each individual exon.  ");
    sb.append("Since the individual exons and the whole transcripts may have properties with different names, ");
    sb.append("changing the setting of "+GLYPH_DEPTH+" may affect which labels are shown.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>"+APPLY_DEFAULT_BG+"</h2>\n");
    sb.append("Copies the background color from the default row to all other rows.");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>"+REFRESH_MAP+"</h2>\n");
    sb.append("Redraws the display applying any changes that have been made in the table.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>"+AUTO_REFRESH+"</h2>\n");
    sb.append("If selected, the display will refresh automatically after each change.");
    sb.append("</p>\n");

    return sb.toString();
  }

  public Icon getIcon() {
    return null;
  }

  public String getInfoURL() {
    return "";
  }

  public String getToolTip() {
    return "Set Tier Colors and Properties";
  }

  // implementation of IPrefEditorComponent
  public void refresh() {
    refreshList();
  }

	private void stopEditing() {
		if (table != null && table.getCellEditor() != null) {
			table.getCellEditor().stopCellEditing();
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible) {
			stopEditing();
		}
	}

	public void windowClosed(WindowEvent e) {
		stopEditing();
	}

  	public void windowOpened(WindowEvent e) { }

	public void windowClosing(WindowEvent e) { }

	public void windowIconified(WindowEvent e) { }

	public void windowDeiconified(WindowEvent e) { }

	public void windowActivated(WindowEvent e) { }

	public void windowDeactivated(WindowEvent e) { }
}
