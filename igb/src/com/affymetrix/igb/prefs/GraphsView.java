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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.prefs.*;
import javax.swing.*;

import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

/**
 *  A panel that shows the preferences for particular special URLs and file locations. 
 */
public class GraphsView extends JPanel implements IPrefEditorComponent  {
    
  public GraphsView() {
    super();
    this.setName("Graphs");
    this.setLayout(new BorderLayout());

    JPanel main_box = new JPanel();
    main_box.setBorder(new javax.swing.border.EmptyBorder(5,5,5,5));
    
    JScrollPane scroll_pane = new JScrollPane(main_box);
//    this.add(scroll_pane, BorderLayout.CENTER);
    this.add(scroll_pane);
    
    JPanel graphs_box = new JPanel();
    graphs_box.setLayout(new BoxLayout(graphs_box, BoxLayout.Y_AXIS));
    //graphs_box.setBorder(new javax.swing.border.TitledBorder("Graph Settings"));
    main_box.add(graphs_box);
    
    Box box_0 = new Box(BoxLayout.X_AXIS);
    box_0.add(UnibrowPrefsUtil.createCheckBox("Use floating graphs", GraphGlyphUtils.getGraphPrefsNode(),
      GraphGlyphUtils.PREF_USE_FLOATING_GRAPHS, GraphGlyphUtils.default_use_floating_graphs));
    box_0.add(Box.createHorizontalGlue());
    graphs_box.add(box_0);
    
    graphs_box.add(Box.createVerticalStrut(5));

    JLabel graph_height_label = new JLabel("Floating graph default pixel height: ");
    JTextField graph_height_field = UnibrowPrefsUtil.createNumberTextField(
      GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_FLOATING_PIXEL_HEIGHT, Integer.toString(GraphGlyphUtils.default_pix_height), Integer.class);
    Box height_box = new Box(BoxLayout.Y_AXIS);
    height_box.add(graph_height_label);
    height_box.add(Box.createHorizontalStrut(5));
    height_box.add(graph_height_field);
    height_box.add(Box.createHorizontalGlue());
    graphs_box.add(height_box);

    JLabel graph_height_label2 = new JLabel("Tiered graph default coord height: ");
    JTextField graph_height_field2 = UnibrowPrefsUtil.createNumberTextField(
      GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_ATTACHED_COORD_HEIGHT, Integer.toString(GraphGlyphUtils.default_coord_height), Integer.class);
    Box height_box2 = new Box(BoxLayout.Y_AXIS);
    height_box2.add(graph_height_label2);
    height_box2.add(Box.createHorizontalStrut(5));
    height_box2.add(graph_height_field2);
    height_box2.add(Box.createHorizontalGlue());
    graphs_box.add(height_box2);

    graphs_box.add(Box.createVerticalStrut(5));

    Box defpan5 = new Box(BoxLayout.Y_AXIS);
    defpan5.add(new JLabel("When making a tier from a floating graph, use: "));
    String[] combo_options = new String[] {GraphGlyphUtils.USE_CURRENT_HEIGHT, GraphGlyphUtils.USE_DEFAULT_HEIGHT};
    JComboBox float2attachCB = UnibrowPrefsUtil.createComboBox(
      GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_ATTACH_HEIGHT_MODE, combo_options, GraphGlyphUtils.default_attach_mode);
    defpan5.add(float2attachCB);
    graphs_box.add(defpan5);

    graphs_box.add(Box.createVerticalStrut(5));
    
    Box graph_colors_box = new Box(BoxLayout.X_AXIS);
    graph_colors_box.setBorder(new javax.swing.border.TitledBorder("Default Graph Colors"));
    graph_colors_box.add(UnibrowPrefsUtil.createColorButton(null, GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_GRAPH_COLOR_PREFIX+"0", GraphGlyphUtils.default_graph_colors[0]));
    graph_colors_box.add(UnibrowPrefsUtil.createColorButton(null, GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_GRAPH_COLOR_PREFIX+"1", GraphGlyphUtils.default_graph_colors[1]));
    graph_colors_box.add(UnibrowPrefsUtil.createColorButton(null, GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_GRAPH_COLOR_PREFIX+"2", GraphGlyphUtils.default_graph_colors[2]));
    graph_colors_box.add(UnibrowPrefsUtil.createColorButton(null, GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_GRAPH_COLOR_PREFIX+"3", GraphGlyphUtils.default_graph_colors[3]));
    graph_colors_box.add(UnibrowPrefsUtil.createColorButton(null, GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_GRAPH_COLOR_PREFIX+"4", GraphGlyphUtils.default_graph_colors[4]));
    graph_colors_box.add(UnibrowPrefsUtil.createColorButton(null, GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_GRAPH_COLOR_PREFIX+"5", GraphGlyphUtils.default_graph_colors[5]));
    graphs_box.add(graph_colors_box);
    
    validate();
  }

  public void destroy() {
    removeAll();
  }

  /** A main method for testing. */
  public static void main(String[] args) throws Exception {
    GraphsView p = new GraphsView();
   
    JDialog d = new JDialog();
    d.setTitle(p.getName());
    d.getContentPane().add(p);
    d.pack();
    
    d.setVisible(true);
    d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    d.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
        System.exit(0);
      }
    }
    );
  }
  
  public String getHelpTextHTML() {
    StringBuffer sb = new StringBuffer();

    sb.append("<h1>" + this.getName() + "</h1>\n");
    sb.append("<p>\n");
    sb.append("This panel allows you to change default options for graphs.  ");
    sb.append("Changes have no effect on existing graphs, but apply to any new graphs ");
    sb.append("created afterwards.  ");
    sb.append("</p>\n");
        
    sb.append("<h2>Graph Bookmarks</h2>\n");
    sb.append("<p>\n");
    sb.append("Since graph bookmarks contain information about the color and size  ");
    sb.append("of each graph, the options here have no effect on graphs loaded from bookmarks.  ");
    sb.append("</p>\n");
    return sb.toString();
  }
  
  public Icon getIcon() {
    return null;
  }
  
  public String getToolTip() {
    return "Edit Default Graph Properties";
  }
  
  public String getInfoURL() {
    return null;
  }    
  
  public void refresh() {
  }
  
}
