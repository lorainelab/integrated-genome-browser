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

import java.awt.*;
import javax.swing.*;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

/**
 *  A panel that shows the preferences for graph properties.
 */
public final class GraphsView extends JPanel implements IPrefEditorComponent  {
    
  public GraphsView() {
    super();
    this.setName("Graphs");
    this.setLayout(new BorderLayout());

    JPanel main_box = new JPanel();
    main_box.setBorder(new javax.swing.border.EmptyBorder(5,5,5,5));
    
    JScrollPane scroll_pane = new JScrollPane(main_box);
    this.add(scroll_pane);
    
    JPanel graphs_box = new JPanel();
    graphs_box.setLayout(new BoxLayout(graphs_box, BoxLayout.Y_AXIS));
    graphs_box.setAlignmentX(0.0f);
    main_box.add(graphs_box);
        
    /*graphs_box.add(UnibrowPrefsUtil.createCheckBox("Use floating graphs by default", GraphGlyphUtils.getGraphPrefsNode(),
      GraphGlyphUtils.PREF_USE_FLOATING_GRAPHS, GraphGlyphUtils.default_use_floating_graphs));
    */
		
    graphs_box.add(UnibrowPrefsUtil.createCheckBox("Use file URL as graph name", GraphGlyphUtils.getGraphPrefsNode(),
      GraphGlyphUtils.PREF_USE_URL_AS_NAME, GraphGlyphUtils.default_use_url_as_name));

    graphs_box.add(Box.createRigidArea(new Dimension(0,5)));
    
    graphs_box.add(UnibrowPrefsUtil.createCheckBox("Make graphs from scored interval ('egr' and 'sin') files",
						 UnibrowPrefsUtil.getTopNode(),
						 ScoredIntervalParser.PREF_ATTACH_GRAPHS,
						 ScoredIntervalParser.default_attach_graphs));
    
    JComboBox heat_cb = UnibrowPrefsUtil.createComboBox(UnibrowPrefsUtil.getTopNode(), HeatMap.PREF_HEATMAP_NAME,
        HeatMap.getStandardNames(), HeatMap.def_heatmap_name.toString());
    Box heat_row = Box.createHorizontalBox();
    heat_row.add(new JLabel("Preferred Heatmap"));
    heat_row.add(Box.createRigidArea(new Dimension(6,0)));
    heat_row.add(heat_cb);
    heat_row.setAlignmentX(0.0f);
    graphs_box.add(heat_row);
    graphs_box.add(Box.createRigidArea(new Dimension(0,5)));
        
    validate();
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
    sb.append("This panel allows you to change default options for newly-created graphs.  ");
    sb.append("Changes have no effect on existing graphs, but apply to any new graphs ");
    sb.append("created afterwards.  ");
    sb.append("Graph bookmarks can contain information about the properties  ");
    sb.append("of each graph.  Properties specified in bookmarks will take precedence over these defaults.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>Use floating graphs by default</h2>\n");
    sb.append("Whether new graphs should be floating by defualt. ");
    sb.append("Has no effect on graphs loaded through bookmarks since they explicitly specify floating or not-floating.  ");
    sb.append("Has no effect on graphs created from '.egr' files.  ");
    sb.append("Recommend: false.");
    //sb.append("<br><br>Changes do not require re-start.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>Use file URL as graph name</h2>\n");
    sb.append("Whether to use the complete URL for the name of newly-loaded graphs.  ");
    sb.append("True uses the complete URL 'file:///home/graph.gr';  ");
    sb.append("False uses the shorter filename 'graph.gr'.  ");
    sb.append("Has no effect on graphs loaded through bookmarks if they explicitly set a graph name.  ");
    sb.append("Recommend: false.");
    //sb.append("<br><br>Changes do not require re-start.  ");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>Make graphs from scored-interval files</h2>\n");
    sb.append("When loading data from an '.egr' file (sometimes called a '.sin' file) ");
    sb.append("the program can automatically convert the score or scores into a graph or graphs. ");
    sb.append("Usually you want this to happen, so set this to true. ");
    sb.append("");
    sb.append("Recommend: true.");
    sb.append("</p>\n");

    sb.append("<p>\n");
    sb.append("<h2>Preferred Heatmap</h2>\n");
    sb.append("Default heatmap to use for graphs created from scored-interval files. ");
    sb.append("The 'blue/yellw' and 'black/white' maps are good choices in general, " );
    sb.append("but the 'red/black/green' map is preferred for some uses. ");
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
