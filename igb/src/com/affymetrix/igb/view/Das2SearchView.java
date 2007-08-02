package com.affymetrix.igb.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;

import javax.swing.EditableComboBox;

import com.affymetrix.igb.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.*;
import com.affymetrix.igb.das2.*;

/**
 *  Strategy is to call Das2VersionedSource.getFeaturesByName()
 *     (either only the "current version" or all known versions that match current seq group)
 *  Should get back list of results as syms (returned from DAS/2 server in das2feature XML format)
 *  Howevever, want to configure parsing so that results are NOT added as annotations to seqs,
 *      rather they are displayed as a list to choose from
 *  When a result is selected, the result sym's range is used as a guide to construct other DAS/2 queries
 *      via Das2LoadView3.loadFeatures() / loadFeaturesInView(), (potentially in optimized formats)
 *      and to set view to results sym's seq and span
 */
public class Das2SearchView extends JPanel implements ActionListener {
  //  static String default_query = "http://localhost:9092/das2/H_sapiens_May_2004/features?name=APP";
  //  static Das2ServerInfo default_server = Das2Discovery.getServerInfo("http://localhost:9092/das2/genome");
  static Das2ServerInfo default_server;
  static Das2Source default_source;
  static Das2VersionedSource default_version;
  static String default_query_name = "APP";

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  static {
    default_server = Das2Discovery.getDas2Server("localhost");
    //    default_server = Das2Discovery.getDas2Server("NetAffx");  
    System.out.println("default_server: " + default_server);
    //    default_source = default_server.getSource("H_sapiens");  // use if default server is NetAffx
    default_source = default_server.getSource("Human");  // use if default server is localhost

    System.out.println("default_source: " + default_source);
    default_version = default_source.getVersion("H_sapiens_Mar_2006");
    System.out.println("default_version: " + default_version);
  }

  JTextField searchTF = new JTextField(40);
  EditableComboBox searchCB = new EditableComboBox();

  //  Das2VersionedSource current_version = null;
  SeqMapView gviewer;
  JTable results_table;
  Das2LoadView3 parent_view;
  Map search_results_history = new LinkedHashMap();  // keeping history of search results 

  public Das2SearchView(Das2LoadView3 view) {
    super();
    gviewer = Application.getSingleton().getMapView();
    this.parent_view = view;
    this.setLayout(new BorderLayout());

    //    this.add(new JLabel("Name searcH: "));
    //    this.add(searchTF);
    JPanel pan1 = new JPanel();
    pan1.add(new JLabel("Name searcH: "));
    //    pan1.add(searchTF);
    pan1.add(searchCB);
    this.add("North", pan1);
    searchTF.addActionListener(this);
    searchCB.addActionListener(this);

    results_table = new JTable();
    results_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    results_table.setModel(new SearchResultsTableModel(new java.util.ArrayList())); // initializing table with empty searhc results model
    JScrollPane table_scroller = new JScrollPane(results_table);

    //    this.setLayout(new BorderLayout());
    //    JPanel types_panel = new JPanel(new BorderLayout());
    //    types_panel.setBorder(new TitledBorder("Recently Accessed Annotation Types"));
    //    types_panel.add("Center", table_scroller);
    this.add("Center", table_scroller);
    results_table.addMouseListener(  new MouseAdapter()  {
	public void mouseClicked(MouseEvent e)  {
	  if (e.getClickCount() == 2)  {
            Point p = e.getPoint();
            int row = results_table.rowAtPoint(p);
            int column = results_table.columnAtPoint(p); // This is the view column!
	    SearchResultsTableModel mod = (SearchResultsTableModel)results_table.getModel();
	    SimpleDas2Feature feat = mod.getSearchResult(row);
	    displaySearchResult(feat);
	    // loading of WHOLE_SEQUENCE strategy types already dealt with by Das2LoadView3.seqSelectionChanged
	    //    but also want to at least for range of selected annot load annots of the 
	    //       type that has been selected (including itself)
	    //    maybe force type strategy of selected annot's type to change to VISIBLE_RANGE 
	    //       (or WHOLE_SEQ if type hint present)
	    // parent_view.loadFeatures(Das2TypeState.WHOLE_SEQUENCE, false); 
	    // parent_view.loadFeatures(Das2TypeState.VISIBLE_RANGE, false);
	  }
	}
      } );

  }

  public void displaySearchResult(SimpleDas2Feature feat) {
    System.out.println("displaying result of name search: " + feat);
    SeqSpan span = feat.getSpan(0);
    MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)span.getBioSeq();
    if (gmodel.getSelectedSeq() != seq) { gmodel.setSelectedSeq(seq); }
    gviewer.zoomTo(span);
  }


  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    String command = evt.getActionCommand();
    String name = null;
    if (src == searchTF) {
      name = searchTF.getText();
    }
    else if ((src == searchCB) && (command.equalsIgnoreCase("comboBoxChanged"))) {
      name = (String)searchCB.getSelectedItem();
    }
    if (name != null) {
      System.out.println("@@@@@  trying to search for annotation name: " + name + "  @@@@@") ;
      searchFeaturesByName(name);
    }
  }

  public java.util.List searchFeaturesByName(String name) {
    if (name == null || name.equals(""))  { return null; }
    //    if (current_version != null) {
    // Das2VersionedSource.getFeaturesByName() should also add features as annotations to seqs...
    //    java.util.List feats = default_version.getFeaturesByName(default_query_name, false);

    // if already searched with this name, then use cached results
    java.util.List feats = (java.util.List)search_results_history.get(name); 
    if (feats == null) {
      feats = default_version.getFeaturesByName(name, false); 
      MutableComboBoxModel mcb = (MutableComboBoxModel)searchCB.getModel();
      mcb.addElement(name);
      search_results_history.put(name, feats);
    }
    System.out.println("features found: " + feats.size());
    Collections.sort(feats, new Das2FeatureComparator(true));  // sort based on name / ID
    SearchResultsTableModel tmodel = new SearchResultsTableModel(feats);
    results_table.setModel(tmodel);
    return feats;
  }


}

class SearchResultsTableModel extends AbstractTableModel {
  static int NAME_COLUMN = 0;
  static int ID_COLUMN = 1;
  static int TYPE_COLUMN = 2;
  static int CHROM_COLUMN = 3;
  static int MIN_COLUMN = 4;
  static int MAX_COLUMN = 5;
  static int STRAND_COLUMN = 6;

  java.util.List search_results;

  String[] column_names = { "name", "ID", "type", "chromosome", "min", "max", "strand" };
  public SearchResultsTableModel(java.util.List results) {
    search_results = results;
  }

  public Object getValueAt(int row, int col) {
    SimpleDas2Feature feat = getSearchResult(row);
    SeqSpan span = feat.getSpan(0);
    Object result = "NOT_ASSIGNED";
    if (col == NAME_COLUMN) { result = feat.getName(); }
    else if (col == ID_COLUMN) { result = feat.getID(); }
    else if (col == TYPE_COLUMN) { result = feat.getType(); }
    else if (col == CHROM_COLUMN) { result = span.getBioSeq().getID(); }
    else if (col == MIN_COLUMN) { result = span.getMin(); }
    else if (col == MAX_COLUMN) { result = span.getMax(); }
    else if (col == STRAND_COLUMN) { result = (span.isForward() ? "+" : "-"); }
    return result;
  }

  public int getColumnCount() { return column_names.length; }
  public String getColumnName(int col) {
    return column_names[col];
  }
  public int getRowCount() { return search_results.size(); }

  public SimpleDas2Feature getSearchResult(int row) {
    return (SimpleDas2Feature)search_results.get(row);
  }

}

