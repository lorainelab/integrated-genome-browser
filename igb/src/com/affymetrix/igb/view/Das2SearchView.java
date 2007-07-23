package com.affymetrix.igb.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

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
public class Das2SearchView extends JPanel implements ActionListener  {
  static String default_query = "http://localhost:9092/das2/H_sapiens_May_2004/features?name=APP";

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  JTextField searchTF = new JTextField(40);
  Das2VersionedSource current_version = null;
  SeqMapView gviewer;

  public Das2SearchView() {
    super();
    gviewer = Application.getSingleton().getMapView();
    this.add(new JLabel("Name searcH: "));
    this.add(searchTF);
    searchTF.addActionListener(this);

  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == searchTF) {
      String name = searchTF.getText();
      System.out.println("trying to search for annotation name: " + name);
      loadFeaturesByName(name);
      MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();
      gviewer.setAnnotatedSeq(aseq, true, true);
    }
  }

  public void loadFeaturesByName(String name) {
    if (current_version != null) {
      // Das2VersionedSource.getFeaturesByName() should also add features as annotations to seqs...
      java.util.List feats = current_version.getFeaturesByName(name);
    }
  }


}

/*
class SearchResultsTableModel extends AbstractTableModel {
  java.util.List search_results;
  public SearchResultsTableModel(java.util.List results) {

  }
}
*/
