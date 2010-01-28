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
  
  public String getHelpTextHTML() {
	  return "<h1>" + this.getName() + "</h1>\n" +
			  "<p>\n" +
			  "This panel allows you to change default options for newly-created graphs.  " +
			  "Changes have no effect on existing graphs, but apply to any new graphs " +
			  "created afterwards.  " +
			  "Graph bookmarks can contain information about the properties  " +
			  "of each graph.  Properties specified in bookmarks will take precedence over these defaults.  " +
			  "</p>" +
			  "<p>" +
			  "<h2>Use floating graphs by default</h2>" +
			  "Whether new graphs should be floating by defualt. " +
			  "Has no effect on graphs loaded through bookmarks since they explicitly specify floating or not-floating.  " +
			  "Has no effect on graphs created from '.egr' files.  " +
			  "Recommend: false." +
			  "</p>" +
			  "<p>" +
			  "<h2>Use file URL as graph name</h2>" +
			  "Whether to use the complete URL for the name of newly-loaded graphs.  " +
			  "True uses the complete URL 'file:///home/graph.gr';  " +
			  "False uses the shorter filename 'graph.gr'.  " +
			  "Has no effect on graphs loaded through bookmarks if they explicitly set a graph name.  " +
			  "Recommend: false." +
			  "</p>" +
			  "<p>" +
			  "<h2>Make graphs from scored-interval files</h2>" +
			  "When loading data from an '.egr' file (sometimes called a '.sin' file) " +
			  "the program can automatically convert the score or scores into a graph or graphs. " +
			  "Usually you want this to happen, so set this to true. " +
			  "Recommend: true." +
			  "</p>" +
			  "<p>" +
			  "<h2>Preferred Heatmap</h2>" +
			  "Default heatmap to use for graphs created from scored-interval files. " +
			  "The 'blue/yellow' and 'black/white' maps are good choices in general, " +
			  "but the 'red/black/green' map is preferred for some uses. ";
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
