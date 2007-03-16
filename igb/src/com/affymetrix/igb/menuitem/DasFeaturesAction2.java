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

package com.affymetrix.igb.menuitem;

import com.affymetrix.igb.event.UrlLoaderThread;

// Java
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import org.xml.sax.*;

import org.w3c.dom.Document;

import com.affymetrix.genoviz.bioviews.*;

import com.affymetrix.igb.das.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.NibbleBioSeq; // should replace with Versioned interface...
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.util.UnibrowPrefsUtil;

public class DasFeaturesAction2 extends org.xml.sax.helpers.DefaultHandler implements ActionListener {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static boolean DEBUG = false;
  static String user_dir = System.getProperty("user.dir");

  static int DAS_NAME = 0;
  static int DAS_URI = 1;
  static int INFO_URI = 2;

  final static String DAS_SOURCE_REQUEST = "dsn";
  final static String DAS_ENTRY_REQUEST = "entry_points";
  final static String DAS_TYPES_REQUEST = "types";
  final static String DAS_FEAT_REQUEST = "features?";

  SeqMapView gviewer;
  String current_das_server = null;
  String current_das_source = null;
  String current_das_seq = null;
  JComboBox select_serverCB = new JComboBox();
  JComboBox select_sourceCB = new JComboBox();
  JComboBox select_seqCB = new JComboBox();
  JLabel min_field_label = new JLabel("Range Min");
  JLabel max_field_label = new JLabel("Range Max");
  JTextField min_fieldTF = new JTextField("");
  JTextField max_fieldTF = new JTextField("");
  JPanel checklist;
  JPanel checkpanel = new JPanel();
  Vector selected_types = new Vector();
  Vector typeboxes = new Vector();
  JTextField termTF = new JTextField("");
  DefaultListModel lmodel = new DefaultListModel();
  JList seglist = new JList(lmodel);
  JTabbedPane tab_pane = new JTabbedPane();
  Map segment_hash = new HashMap();

  SynonymLookup lookup = SynonymLookup.getDefaultLookup();
  boolean server_supports_termquery = false;
  boolean server_supports_bps = false;
  boolean server_supports_minmax = false;

  /*
   *  Plan for keeping track of DAS queries
   *      (to support minmax constraints, etc.)
   *
   * Nested hash structure
   * Top level is server2genomes hash
   *  server2genomes :  "server_name" ==> genome2query hash
   *     genome2query  :  "genome_version" ==> seq2type hash
   *        seq2type :       "seq_id" ==> type2collector hash
   *           type2collector:  "type_name" ==> query_collector
   *
   *  query_colllector is a SeqSymmetry (or possibly List)
   *     whose children represent each bps supporting, minmax supporting, DAS query
   *        for features of type id "type_name"
   *        along seq with seq id "seq_id"
   *        from source with source id "genome_version"
   *        served by DAS server with name/url "server_name"
   */

  //  Assuming for now that DAS query tracking info is discarded when switching
  //     to a different DAS data source
  Map server2genomes = new Hashtable();
  Map seq2type;
  DasClientOptimizer optimizer;
  boolean TEST_OPTIMIZER = false;

  public static final String PREF_SHOW_DAS_QUERY_GENOMETRY = "SHOW_DAS_QUERY_GENOMETRY";
  public static final boolean default_show_das_query_genometry = false;
  
  public DasFeaturesAction2(SeqMapView gviewer) {
    this.gviewer = gviewer;
    optimizer = new DasClientOptimizer(null);
    seglist.addListSelectionListener(
      new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent evt) {
          selectDASSegment((String) seglist.getSelectedValue());
        }
      }
    );

    // adding DAS server selection items to combo-box
    select_serverCB.addItem("");
    refreshDasServersComboBox();

    PopupMenuListener select_server_pml = new PopupMenuListener() {
      public void popupMenuCanceled(PopupMenuEvent e) {}

      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        refreshDasServersComboBox();
      }
    };
    select_serverCB.addPopupMenuListener(select_server_pml);
    

    checkpanel.setLayout(new BoxLayout(checkpanel, BoxLayout.Y_AXIS));
    JPanel panA = new JPanel();
    panA.setLayout(new GridLayout(5, 2));

    panA.add(new JLabel("DAS Server"));
    panA.add(select_serverCB);

    // DAS data-source selection
    panA.add(new JLabel("Data Source"));
    panA.add(select_sourceCB);
    // get data sources for each DAS server...
    select_sourceCB.addItem("");

    // DAS sequence selection
    panA.add(new JLabel("Sequence"));
    panA.add(select_seqCB);
    // get entry points for each data source...
    select_seqCB.addItem("");

    panA.add(min_field_label);
    panA.add(min_fieldTF);

    panA.add(max_field_label);
    panA.add(max_fieldTF);

    checkpanel.add(panA);

    checklist = new JPanel();
    checklist.setLayout(new BoxLayout(checklist, BoxLayout.Y_AXIS));

    JPanel termpane = new JPanel();
    termpane.setLayout(new BorderLayout());

    JPanel panB = new JPanel();
    panB.setLayout(new GridLayout(1, 2));
    panB.add(new JLabel("Search Term:"));
    panB.add(termTF);
    termpane.add("North", panB);
    JScrollPane term_result_pane = new JScrollPane(seglist);
    termpane.add("Center", term_result_pane);

    JScrollPane scrollpane = new JScrollPane(checklist);
    scrollpane.setPreferredSize(new Dimension(100, 300));
    tab_pane.addTab("Available Annotations", scrollpane);
    termpane.setEnabled(false);
    tab_pane.addTab("Term Query", termpane);
    checkpanel.add(tab_pane);

    select_serverCB.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent evt) {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
          selectDASServer((String) evt.getItem());
        }
      }}
    );
    select_sourceCB.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent evt) {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
          selectDASSource((String) evt.getItem());
        }
      }
    });
    select_seqCB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
	selectDASSeq((String)select_seqCB.getSelectedItem());
      }
    });
    termTF.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        String term = termTF.getText();
        handleTermQuery(current_das_server, current_das_source, term);
      }
    });
  }

  /** Refreshes list of DAS servers in the servers JComboBox so that it matches
   *  the list in the Set from DasDiscovery.getDasServers().keySet().
   */
  public void refreshDasServersComboBox() {
    // Get the list of what is currently in the combo-box    
    int item_count = select_serverCB.getItemCount();
    Vector current_list = new Vector(item_count);
    for (int i=0; i<item_count; i++) {
      current_list.add((String) select_serverCB.getItemAt(i));
    }
    
    // Add any servers to the combo box that aren't already there
    Set das_servers = DasDiscovery.getDasServers().keySet();
    Iterator iter = das_servers.iterator();
    while (iter.hasNext()) {
      String server_name = (String) iter.next();
      if (!current_list.contains(server_name)) {
        select_serverCB.addItem(server_name);
      }
    }
    
    // Remove servers from the combo box that aren't in the das_servers list
    for (int i=0; i<item_count; i++) {
      String server_name = (String) current_list.get(i);
      if (! "".equals(server_name) && ! das_servers.contains(server_name)) {
        select_serverCB.removeItem(server_name);
      }
    }
  }

  Document getDasDocument(String url) {
    Document doc = null;
    try {
      doc = DasLoader.getDocument(url);
      if (doc == null) {
        ErrorHandler.errorPanel("DAS Request returned no data\nRequest = "+url);
      }
    } catch (Exception e) {
      ErrorHandler.errorPanel("Error getting data from DAS\nRequest = "+url, e);
    }
    return doc;
  }

  void handleTermQuery(String das_server, String das_source, String term) {
    lmodel.clear();
    segment_hash.clear();
    AnnotatedSeqGroup seq_group = SingletonGenometryModel.getGenometryModel().getSelectedSeqGroup();
    if (seq_group == null) {
      ErrorHandler.errorPanel("Cannot perform Term Query when no sequence is loaded.");
      return;
    }

    if (das_server == null || das_source == null) {
      ErrorHandler.errorPanel("DAS server or source is null");
      return;
    }

    String das_request = das_server + das_source + "/segment_query?term=" + term;
    if (DEBUG) System.out.println("DAS request: " + das_request);

    Document doc = getDasDocument(das_request);
    if (doc != null) {
      Map segment_hash_2 = DasLoader.parseTermQuery(doc, seq_group);

      // Copy the lables into the ListModel
      Iterator labels = segment_hash_2.keySet().iterator();
      while (labels.hasNext()) {
        String s = (String) labels.next();
        lmodel.addElement(s);
      }

      // Copy the results to the given segment_hash
      segment_hash.putAll(segment_hash_2);
    }
  }

  /** Opens the DAS Dialog (by calling {@link #showDasDialog()}).
   *  This is normally called from a menu item in the main Unibrow window.
   */
  public void actionPerformed(ActionEvent evt) {
    boolean ok = showDasDialog();
    if (ok) {
      processDASRequest();
    }
  }

  /** Uses the values selected in the dialogs to formulate and process
   *  a request to a DAS server.
   *  This method can take a while to run.  Ideally it should be in a
   *  user-interruptible thread.
   **/
  void processDASRequest() {
    //    query_input = input_field.getText();
    //    System.out.println("query input: " + query_input);
    selected_types.removeAllElements();

    for (int i=0; i<typeboxes.size(); i++) {
      JCheckBox box = (JCheckBox)typeboxes.elementAt(i);
      boolean selected = box.isSelected();
      if (selected) {
        String typeid = box.getText();
        selected_types.add(typeid);
      }
    }

    String server_name = (String)select_serverCB.getSelectedItem();

    // hack for dealing with pseudo-das PslQueryServlet, until it can handle
    //   feature requests with multiple features types
    if (server_name.startsWith("Affy-Genometry") || server_supports_bps) {
      java.util.List the_list = doFeatureRequestHack();
      if (the_list != null && !the_list.isEmpty()) try {
        URL[] urls = new URL[the_list.size()];
        String[] tier_names = new String[the_list.size()];
        for (int i=0; i<the_list.size(); i++) {
          String[] array = (String[]) the_list.get(i);
          urls[i] = new URL(array[0]);
          tier_names[i] = array[1];
        }
        UrlLoaderThread t = new UrlLoaderThread(gviewer, urls, null, tier_names);
        t.runEventually();
      } catch (MalformedURLException mfe) {
        ErrorHandler.errorPanel("Problem with DAS\n" +
                           "Malformed URL: " + mfe.getMessage());
      }
    }
    else {
      String das_feat_request = composeDasFeatRequest();
      try {
        // want to allow decomposition of DAS feature request if some areas are already covered
        // String[] das_feat_request = composeDasFeatRequest();
        System.out.println("DAS request1: " + das_feat_request);
        if (das_feat_request != null) {
          URL url = new URL(das_feat_request);
          UrlLoaderThread t = UrlLoaderThread.getUrlLoaderThread(gviewer, url, null, "Unknown");
          t.runEventually();
        }
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel("Problem with DAS\n"+
                           "URI = " + das_feat_request+"\n", ex);
      }
    }
  }


  /**
   *  Returns a List of String[2] arrays s, where each s[0] is a URL and s[1] is a tier name.
   */
  java.util.List doFeatureRequestHack() {
    boolean SHOW_DAS_QUERY_GENOMETRY = UnibrowPrefsUtil.getTopNode().getBoolean(
      PREF_SHOW_DAS_QUERY_GENOMETRY, default_show_das_query_genometry);

    java.util.List the_list = new ArrayList(4); // it is usually a short list, so initialize with a small length
    MutableAnnotatedBioSeq current_seq =
      (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();

    if (current_seq == null) {
      ErrorHandler.errorPanel("You must first load a sequence");
      return null;
    }
    if (DEBUG)  { System.out.println("doing hack to request features from bps DAS server"); }
    String das_feat_request = null;
    try {
      String seqid = (String)select_seqCB.getSelectedItem();
      int seqstart = Integer.parseInt(min_fieldTF.getText());
      int seqstop = Integer.parseInt(max_fieldTF.getText());
      Map type2collector = (Map)seq2type.get(seqid);
      if (type2collector == null) {
        ErrorHandler.errorPanel("WARNING -- type2collector has for seq = " + seqid + " not found!");
      }
      SeqSpan query_span = new SimpleSeqSpan(seqstart, seqstop, current_seq);

      String das_source_root = current_das_server +   //  "/"  already included in current_das_server
	current_das_source + "/";
      if (TEST_OPTIMIZER)  {
	optimizer.loadAnnotations(das_source_root, "test", query_span, selected_types);
      }
      for (int i=0; i<selected_types.size(); i++) {
        String type = (String)selected_types.elementAt(i);

        // for testing/debugging of more sophisticated client-side processing
        //   of DAS query, adding annotations to show spans of all DAS queries...
        //      MutableSeqSymmetry query_sym = new SimpleMutableSeqSymmetry();
        SimpleSymWithProps query_sym = new SimpleSymWithProps();
        query_sym.setProperty("method",
                              //  current_das_server + ", " + current_das_source + ":" +
                              ("das_raw_query:" + type));
	//        SeqSpan query_span = new SimpleSeqSpan(seqstart, seqstop, current_seq);
        query_sym.addSpan(query_span);
        if (SHOW_DAS_QUERY_GENOMETRY) { current_seq.addAnnotation(query_sym); }
        SeqSymmetry exclusive_sym;

        MutableSeqSymmetry query_collector = (MutableSeqSymmetry)type2collector.get(type);
        if (query_collector == null) {
          query_collector = new SimpleSymWithProps();
          type2collector.put(type, query_collector);
          exclusive_sym = query_sym;
        }
        else {
          exclusive_sym = SeqUtils.exclusive(query_sym, query_collector, current_seq);
          //          System.out.println("Exclusive Sym: ");
          //          SeqUtils.printSymmetry(exclusive_sym);
        }

        SeqSpan exclusive_span = exclusive_sym.getSpan(current_seq);

        if (server_supports_minmax) {
          if (exclusive_span != null) {
            int first_hard_min;
            int last_hard_max;
            SimpleSymWithProps exclusive_swp = new SimpleSymWithProps();
            SeqUtils.copyToMutable(exclusive_sym, exclusive_swp);
            exclusive_swp.setProperty("method", ("das_filtered_query:" + type));
            if (SHOW_DAS_QUERY_GENOMETRY) { current_seq.addAnnotation(exclusive_swp); }

            // trying to find hard_min and hard_max
            //     get flattened, sorted union of all query spans on current_seq
            //        (since union, for any index i in list: span[i].max < span[i+1].min)
            //     do binary search with exclusive_span to find
            //          first_hard_min (max of previous span in index) and
            //          last_hard_max (min of next span in index with min >= exclusive_span.max)
            //  If exclusive_swp has no children:
            //      then can do single das query with start = exclusive.min, stop = exclusive.max,
            //          hard_min = first_hard_min, hard_max = last_hard_max
            //  Otherwise (if exclusive_swp has children) this means that exclusive_swp
            //          overlaps with one or more previous queries
            //      So do a das query for each child of exclusive_swp, with following constraints:
            //         for "internal" edges, hard_min = min, hard_max = max,
            //         and for first edge, hard_min = first_hard_min
            //         and for last edge, hard_max = last_hard_max

            //        SeqSymmetry union_queries = SeqUtils.union(query_collector, query_sym, current_seq);
            java.util.List temp_query_list = new ArrayList();
            temp_query_list.add(query_collector);
            SeqSymmetry union_queries = SeqUtils.union(temp_query_list, current_seq);

            java.util.List union_spans = SeqUtils.getLeafSpans(union_queries, current_seq);
            SeqSpanComparator spancomp = new SeqSpanComparator();
            Collections.sort(union_spans, spancomp);
            int insert = Collections.binarySearch(union_spans, exclusive_span, spancomp);
            //            System.out.println("exclusive span: " + SeqUtils.spanToString(exclusive_span));
            //            System.out.println("union spans:");
            //            for (int m=0; m<union_spans.size(); m++) {
            //              SeqUtils.printSpan((SeqSpan)union_spans.get(m));
            //            }
            if (insert < 0) {
              insert = -insert -1;
            }
            //            System.out.println("insertion point: " + insert);
            if (insert == 0) { first_hard_min = 0; }
            else { first_hard_min = ((SeqSpan)union_spans.get(insert-1)).getMax(); }
            // since sorted by min, need to make sure that we are at the insert index
            //   at which get(index).min >= exclusive_span.max,
            //   so increment till this (or end) is reached
            while ((insert < union_spans.size()) &&
                   (((SeqSpan)union_spans.get(insert)).getMin() < exclusive_span.getMax()))  {
              insert++;
            }
            if (insert == union_spans.size()) { last_hard_max = current_seq.getLength(); }
            else { last_hard_max = ((SeqSpan)union_spans.get(insert)).getMin(); }

            int childcount = exclusive_swp.getChildCount();
            if (childcount == 0 || childcount == 1) {
              SimpleSymWithProps hard_edges = new SimpleSymWithProps();
              hard_edges.addSpan(new SimpleSeqSpan(first_hard_min, last_hard_max, current_seq));
              hard_edges.addChild(new SingletonSeqSymmetry(first_hard_min, first_hard_min, current_seq));
              hard_edges.addChild(new SingletonSeqSymmetry(last_hard_max, last_hard_max, current_seq));
              hard_edges.setProperty("method", ("das_exclusion_edges:" + type));
              if (SHOW_DAS_QUERY_GENOMETRY) { current_seq.addAnnotation(hard_edges); }

              das_feat_request =
                current_das_server +   //  "/"  already included in current_das_server
                URLEncoder.encode(current_das_source) + "/" +
                "features?" +
                "segment=" + URLEncoder.encode(seqid) 
                   + ":" + exclusive_span.getMin() + "," + exclusive_span.getMax() +
                ";type=" + URLEncoder.encode(type) +
                ";minmin=" + first_hard_min +
                ";maxmax=" + last_hard_max;
              the_list.add(new String[] {das_feat_request, type});
            }
            else {
              int cur_hard_min;
              int cur_hard_max;
              for (int k=0; k<childcount; k++) {
                SeqSymmetry csym = exclusive_swp.getChild(k);
                SeqSpan childspan = csym.getSpan(current_seq);

                if (k == 0) { cur_hard_min = first_hard_min; }
                else { cur_hard_min = childspan.getMin(); }
                if (k == (childcount-1)) { cur_hard_max = last_hard_max; }
                else { cur_hard_max = childspan.getMax(); }

                SimpleSymWithProps hard_edges = new SimpleSymWithProps();
                hard_edges.addSpan(new SimpleSeqSpan(cur_hard_min, cur_hard_max, current_seq));
                hard_edges.addChild(new SingletonSeqSymmetry(cur_hard_min, cur_hard_min, current_seq));
                hard_edges.addChild(new SingletonSeqSymmetry(cur_hard_max, cur_hard_max, current_seq));
                hard_edges.setProperty("method", ("das_exclusion_edges:" + type) );
                if (SHOW_DAS_QUERY_GENOMETRY) { current_seq.addAnnotation(hard_edges); }

                das_feat_request =
                  current_das_server +   //  "/"  already included in current_das_server
                  URLEncoder.encode(current_das_source) + "/" +
                  "features?" +
                  "segment=" + URLEncoder.encode(seqid) 
                     + ":" + childspan.getMin() + "," + childspan.getMax() +
                  ";type=" + URLEncoder.encode(type) +
                  ";minmin=" + cur_hard_min +
                  ";maxmax=" + cur_hard_max;

                the_list.add(new String[] {das_feat_request, type});
              }
            }
          }
          query_collector.addChild(query_sym);
        }
        else {
          das_feat_request =
            current_das_server +   //  "/"  already included in current_das_server
            URLEncoder.encode(current_das_source) + "/" +
            "features?" +
            "segment=" + URLEncoder.encode(seqid) + ":" + seqstart + "," + seqstop +
            ";type=" + URLEncoder.encode(type);
          the_list.add(new String[] {das_feat_request, type});
        }
      }
      gviewer.setAnnotatedSeq(current_seq, true, true);
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("problem with DAS, uri = " + das_feat_request, ex);
      the_list.clear();
    }
    return the_list;
  }

  /** Checks that all user-entered values are OK.  Pops-up error messages if not.
   *  @return true if all data is OK.
   */
  boolean validateUserInput() {
    if (current_das_server == null) {
      ErrorHandler.errorPanel("Please select a DAS Server");
      select_serverCB.grabFocus();
      return false;
    }

    if (current_das_source == null) {
      ErrorHandler.errorPanel("Please select a DAS Source");
      select_sourceCB.grabFocus();
      return false;
    }

    if (current_das_seq == null) {
      ErrorHandler.errorPanel("Please select a sequence");
      select_seqCB.grabFocus();
      return false;
    }

    int seqstart = -1;
    try {
      seqstart = Integer.parseInt(min_fieldTF.getText());
    } catch (NumberFormatException nfe) {
      seqstart = -1;
    }
    if (seqstart < 0) {
      ErrorHandler.errorPanel("Must set '"+min_field_label.getText()+"' to a positive integer");
      min_fieldTF.grabFocus();
      return false;
    }

    int seqstop = -1;
    try {
      seqstop = Integer.parseInt(max_fieldTF.getText());
    } catch (NumberFormatException nfe) {
      seqstop = -1;
    }
    if (seqstop < 0) {
      ErrorHandler.errorPanel("Must set '"+max_field_label.getText()+"' to a positive integer");
      max_fieldTF.grabFocus();
      return false;
    }
    return true;
  }

  /** Trys to construct a URL for requesting features from a DAS server.
   *  Makes use of selected_types, min_fieldTF, max_fieldTF, gviewer.
   *  Will return null if errors prevent it from constructing the URL.
   **/
  public String composeDasFeatRequest() {
    boolean SHOW_DAS_QUERY_GENOMETRY = UnibrowPrefsUtil.getTopNode().getBoolean(
      PREF_SHOW_DAS_QUERY_GENOMETRY, default_show_das_query_genometry);
    String das_feat_request = null;

    try {
      int seqstart = Integer.parseInt(min_fieldTF.getText());
      int seqstop = Integer.parseInt(max_fieldTF.getText());
      
      if (seqstart == 0) {
        // due to a bug on the UCSC DAS server, re-set any start of 0 to 1.
        // The only possible features that this could cause you to miss are those
        // that are at position 0 and are 1 base long; which is not likely to happen.
        seqstart = 1;
      }

    MutableAnnotatedBioSeq current_seq =
      (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    String seqid = (String)select_seqCB.getSelectedItem();
    if (SHOW_DAS_QUERY_GENOMETRY) {
      if (current_seq.equals(seqid) || lookup.isSynonym(current_seq.getID(), seqid)) {
        SimpleSymWithProps query_sym = new SimpleSymWithProps();
        query_sym.setProperty("method", "das_query");
        SeqSpan query_span = new SimpleSeqSpan(seqstart, seqstop, current_seq);
        query_sym.addSpan(query_span);
        current_seq.addAnnotation(query_sym);
      }
    }

    StringBuffer buf = new StringBuffer();
    for (int i=0; i<selected_types.size(); i++) {
      String type = (String)selected_types.elementAt(i);
      buf.append(";type=");
      buf.append(URLEncoder.encode(type));
    }
    String types_modifier = buf.toString();

    das_feat_request =
      current_das_server +   //  "/"  already included in current_das_server
      URLEncoder.encode(current_das_source) + "/" +
      "features?" +
      "segment=" + URLEncoder.encode(seqid) + ":" + seqstart + "," + seqstop +
      types_modifier;
    } catch (Exception e) {
      ErrorHandler.errorPanel("Error constructing DAS feature request\n"+e.toString());
    }
    return das_feat_request;
  }

  /** Call this to select NO DAS Server.  Resets the GUI and the internal
   *  state such that no server is chosen, thus no DASSource can be chosen
   *  yet, etc.
   */
  public void clearDASServer() {
    if (DEBUG)  { System.out.println("* starting *clear DAS Server*"); }
    if (DEBUG)  { System.out.println("* ending *clear DAS Server*"); }
  }


  /** This should only be called from an action listener on a combo box. */
  public void selectDASServer(String server_name) {
    if (server_name != null)  { System.out.println("selected server: '" + server_name+"'"); }
    current_das_server = null;
    clearDASSource();
    if (server_name == null || server_name.trim().length()==0) {
      return;
    }
    DasServerInfo server = (DasServerInfo) DasDiscovery.getDasServers().get(server_name);
    String server_uri = (String) server.getRootUrl() + "/";
    if (server_uri == null) {
      clearDASServer();
      ErrorHandler.errorPanel("Unknown server '"+server_name+"'");
      return;
    }
    System.out.println("server uri: " + server_uri);

    current_das_server = server_uri;
    String das_request = current_das_server + DAS_SOURCE_REQUEST;
    BioSeq cur_seq = gmodel.getSelectedSeq();
    String cur_seq_version = null;
    if (cur_seq instanceof NibbleBioSeq) {
      cur_seq_version = ((NibbleBioSeq)cur_seq).getVersion();
    }

    Document doc = getDasDocument(das_request);
    if (doc != null) {
      java.util.List sources = DasLoader.parseSourceList(doc);

      String matching_id = null;
      Iterator iter = sources.iterator();
      while (iter.hasNext()) {
        String source = (String) iter.next();
        select_sourceCB.addItem(source);
        if (lookup.isSynonym(cur_seq_version, source)) {
          if (DEBUG) System.out.println("====== got synonym: " + cur_seq_version + ", " + source);
          matching_id = source;
        }
      }

      if (matching_id != null) {
        select_sourceCB.setSelectedItem(matching_id);
      } else {
        System.out.println("*** no synonym match for version:" + cur_seq_version + ":");
      }
    }
  }

  void clearDASSource() {
    if (DEBUG)  { System.out.println("Starting clearDASSource()"); }
    select_sourceCB.removeAllItems();
    select_sourceCB.addItem(""); // Note that this leads to selectDASSource("")
    if (DEBUG)  { System.out.println("ending clearDASSource"); }
  }

  /** This should only be called from an action listener on select_sourceCB. */
  void selectDASSource(String source_name) {
    if (source_name != null && (!(source_name.equals(""))))  {
      System.out.println("selected source: '" + source_name+ "'");
    }
      if (source_name == null || source_name.trim().length()==0) {
        current_das_source = null;
      } else {
        current_das_source = source_name;
      }
      seq2type = new HashMap();
      server_supports_termquery = false;
      server_supports_bps = false;
      server_supports_minmax = false;

      if (DEBUG)  { System.out.println("*** checklist remove all x"); }
      checklist.removeAll();
      checklist.validate();
      checklist.repaint();
      //tab_pane.validate();

      clearDASSeq();

      if (current_das_source == null || current_das_server == null) {
        setTermqueryEnabled(false);
        return;
      }

      //
      //  Querying entry points
      //
      String das_request = current_das_server + current_das_source + "/" +  DAS_ENTRY_REQUEST;
      Collection seqs = null;
      if (current_das_server != null && current_das_source != null) try {
        System.out.println("DAS request: " + das_request);

        URL request_url = new URL(das_request);
        URLConnection request_con = request_url.openConnection();

        String das_version = request_con.getHeaderField("X-DAS-Version");
        String das_status = request_con.getHeaderField("X-DAS-Status");
        String das_capabilities = request_con.getHeaderField("X-DAS-Capabilities");
        System.out.println("DAS server version: " + das_version + ", status: " + das_status);
        System.out.println("DAS capabilities: " + das_capabilities);

        if ((das_capabilities != null) && (das_capabilities.indexOf("segment-search") >= 0)) {
          tab_pane.setEnabledAt(1, true);   // enable term query tab
          server_supports_termquery = true;
        }
        else {
          tab_pane.setEnabledAt(1, false);  // disable term query tab
          // force selection of annotation-type tab (in case term tab currently selected)
          tab_pane.setSelectedIndex(0);
          server_supports_termquery = false;
        }
        if (das_capabilities != null) {
          server_supports_bps       = (das_capabilities.indexOf("bps_features") >= 0);
          server_supports_minmax    = (das_capabilities.indexOf("minmin_maxmax") >= 0);
        }

        Document doc = DasLoader.getDocument(request_con);
        seqs = DasLoader.parseSegmentsFromEntryPoints(doc);
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel("Error parsing DAS Source \n"+ex.toString());
      }

      if (seqs != null && !seqs.isEmpty()) {
        BioSeq cur_seq = gmodel.getSelectedSeq();
        String cur_seq_id = null;
        if (cur_seq != null) {
          cur_seq_id = cur_seq.getID();
        }
        String found_current_seq_synonym = null;
        Iterator seqs_iter = seqs.iterator();
        while (seqs_iter.hasNext()) {
          String id = (String) seqs_iter.next();
          seq2type.put(id, new HashMap());
          select_seqCB.addItem(id);
          if (cur_seq_id != null && lookup.isSynonym(cur_seq_id, id)) {
            found_current_seq_synonym = id;
          }
        }
        if (found_current_seq_synonym != null) {
          setSeq(found_current_seq_synonym);
        } else {
          setSeq("");
        }
      }

      //
      //  Querying feature types
      //
      String full_types_request = current_das_server + current_das_source + "/" +  DAS_TYPES_REQUEST;
      try {
        parseTypeRequest(full_types_request);
      }
      catch (Exception ex) {
        System.out.println("problem with DAS, uri = " + full_types_request);
        ex.printStackTrace();
      }

      setTermqueryEnabled(server_supports_termquery);
  }


  void setTermqueryEnabled(boolean b) {
      if (b) {
        if (DEBUG)  { System.out.println("Enabling termquery"); }
        tab_pane.setEnabledAt(1, true);   // enable term query tab
      } else {
        if (DEBUG)  { System.out.println("Disabling termquery"); }
        // force selection of annotation-type tab (in case term tab currently selected)
        if (tab_pane.getSelectedIndex()==1) {tab_pane.setSelectedIndex(0);}
        tab_pane.setEnabledAt(1, false);  // disable term query tab
      }
  }


  void clearDASSeq() {
    if (DEBUG)  { System.out.println("Starting Clear DAS Seq"); }
    select_seqCB.removeAllItems();
    select_seqCB.addItem(""); // Note that this results in selectDASSeq("") being called
    if (DEBUG)  { System.out.println("Ending clear DAS Seq"); }
  }

  /** Use this to manipulate the value of the select_seqCB.
   *  Doing so then automatically results in a call to selectDASSeq.
   **/
  void setSeq(String id) {
    if (DEBUG)  { System.err.println("Setting seq to '"+id+"'"); }
    if (id == null || id.trim().length()==0) {
      select_seqCB.setSelectedItem("");
    } else {
      select_seqCB.setSelectedItem(id);
    }
  }

  /** This should only be called from the action listener on select_seqCB. */
  void selectDASSeq(String seq_name) {
    System.out.println("called DasFeatursAction2.selectDASSeq()");
    if (seq_name != null && (! (seq_name.equals(""))))  {
      System.out.println("selected seq: '" + seq_name+ "'");
    }
    if (seq_name == null || seq_name.trim().length()==0) {
      current_das_seq = null;
    } else {
      current_das_seq = seq_name;
    }
    if (current_das_seq == null) { return; }
    else {
      //      System.out.println("    in selectDASSeq(), setting min and max coords");
      Rectangle2D vbox  = gviewer.getSeqMap().getView().getCoordBox();
      min_fieldTF.setText(Integer.toString((int)vbox.x));
      max_fieldTF.setText(Integer.toString((int)(vbox.x + vbox.width)));
    }
  }

  int total_annot_count = 0;
  public void parseTypeRequest(String uri) {
    //    System.out.println("parsing type request: " + uri);
    System.out.println("DAS request: " + uri);
    typeboxes = new Vector();
    checklist.removeAll();
    checklist.validate();
    checklist.repaint();
    try {
      XMLReader reader = new org.apache.xerces.parsers.SAXParser();
      InputSource isrc = new InputSource(uri);
      reader.setContentHandler(this);
      reader.parse(isrc);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    if (DEBUG)  { System.out.println("total annot count: " + total_annot_count); }
    total_annot_count = 0;
  }

  public void startElement(String uri, String name, String qname, Attributes atts) {
    if (name.equals("TYPE")) {
      String type_id = atts.getValue("id");
      //      System.out.println(type_id);
      JCheckBox cb = new JCheckBox(type_id);
      typeboxes.add(cb);
      checklist.add(cb);
    }
  }

  public void characters(char[] ch, int start, int length) {
    String cstr = new String(ch, start, length);
    try {
      // System.out.println("characters: '"+cstr+"'");
      int count = Integer.parseInt(cstr);
      total_annot_count += count;
    }
    catch (Exception ex) {
    }
  }


  public boolean showDasDialog() {

    // figure out if any entry points in DAS match the current annotated sequence
    // if one does, set it as selected entry point, and set the range to query
    // for annotations to the current range of annotated sequence being viewed in
    // SeqMapView
    if (DEBUG)  { System.out.println("***** in showDasDialog()"); }
    BioSeq cur_seq = gmodel.getSelectedSeq();
    if (cur_seq != null && cur_seq.getID() != null) {
      String cur_seq_id = cur_seq.getID();
      int max_item = select_seqCB.getItemCount();
      if (DEBUG)  {
	System.out.println("***** in showDasDialog(), current seq: " + cur_seq_id + ", max_item: " + max_item);
      }
      for (int i=0; i<max_item; i++) {
        String item_id = (String)select_seqCB.getItemAt(i);
        if (lookup.isSynonym(cur_seq_id, item_id)) {
          if (DEBUG)  {
	    System.out.println("***** in showDasDialog(), got synonym: " + cur_seq_id + ", " + item_id);
	  }
          // have a match...
          setSeq(item_id);
          break;
        }
      }
    }

    final JOptionPane opt_pane = new JOptionPane(
      checkpanel,
      JOptionPane.PLAIN_MESSAGE,
      JOptionPane.OK_CANCEL_OPTION
    );
    final JDialog dialog = new JDialog(gviewer.getFrame(), "DAS/1 Feature Loader", true);
    dialog.setContentPane(opt_pane);

    opt_pane.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        Object value = opt_pane.getValue();
        if (dialog.isVisible() && (evt.getSource() == opt_pane)
          && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
          boolean ok = false;

          if (value instanceof Integer) {
            int value_int = ((Integer) value).intValue();
            if (value_int == JOptionPane.YES_OPTION) {
              ok = validateUserInput();
              if (ok) {
                dialog.setVisible(false);
              }
              // Keep window open if user selected ok, but validation failed
            } else {
              // User selected "cancel", so simply close the window
              dialog.setVisible(false);
            }
          }

          //Reset the JOptionPane's value.
          //Otherwise, if the user presses the same button again, no event will be fired.
          if (!ok) {opt_pane.setValue(JOptionPane.UNINITIALIZED_VALUE);}
        }
      }
    });
    dialog.pack();
    if (DEBUG)  { System.out.println("***** in showDasDialog(), showing dialog"); }
    dialog.setVisible(true);

    Object choice_obj = opt_pane.getValue();
    int choice = JOptionPane.CANCEL_OPTION;
    if (choice_obj instanceof Integer) {
      choice = ((Integer) choice_obj).intValue();
    }

    // only continue if user clicked OK button
    return (choice == JOptionPane.OK_OPTION);
  }

  public void selectDASSegment(String label) {
    System.out.println("DasFeaturesAction2 selected segment: '"+label+"'");
    if (label==null || label.trim().length()==0) {
      System.out.println("DAS segment de-selection not implemented");
    } else {
      SeqSpan span = (SeqSpan)segment_hash.get(label);
      System.out.println("chosen span: ");
      SeqUtils.printSpan(span);
      MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)span.getBioSeq();
      gmodel.setSelectedSeq(seq);
      //      gviewer.setAnnotatedSeq(seq);  // replaced gviewer call with setting seq selection in gmodel
      gviewer.zoomTo(span);
      min_fieldTF.setText("" + span.getMin());
      max_fieldTF.setText("" + span.getMax());
      select_seqCB.setSelectedItem(span.getBioSeq().getID());
    }
  }
}

