/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.seq.CompositeNegSeq;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.LeafSingletonSymmetry;
import com.affymetrix.genometry.util.DNAUtils;
import com.affymetrix.genometryImpl.TypedSym;
import com.affymetrix.genometryImpl.NibbleBioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.tiers.AnnotStyle;
import java.awt.Insets;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;


/**
 *  A new GUI for searching for residues.  Not yet finished, but
 *  may replace the existing GUI in a future release.
 *  Compare to FindAnnotsPanel.
 */
public class FindResiduesPanel extends JPanel {
  
  public FindResiduesPanel() {
    initComponents();
  }
  
  private void initComponents() {
    buttonGroup1 = new ButtonGroup();
    jPanel1 = new JPanel();
    exact_RB = new JRadioButton();
    regex_RB = new JRadioButton();
    exact_TF = new JTextField();
    regex_TF = new JTextField();
    reset_button = new JButton();
    
    jPanel1.setBorder(BorderFactory.createTitledBorder("Find Residues By..."));
    
    buttonGroup1.add(exact_RB);
    exact_RB.setMnemonic('E');
    exact_RB.setText("Exact Residues");
    exact_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    exact_RB.setMargin(new Insets(0, 0, 0, 0));
    exact_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        ID_exact_RBStateChanged(evt);
      }
    });
    
    buttonGroup1.add(regex_RB);
    regex_RB.setMnemonic('x');
    regex_RB.setText("Regular Expression");
    regex_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    regex_RB.setMargin(new Insets(0, 0, 0, 0));
    regex_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        ID_regex_RBStateChanged(evt);
      }
    });
    
    exact_TF.setEnabled(false);
    regex_TF.setEnabled(false);
    
    GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(GroupLayout.LEADING)
      .add(jPanel1Layout.createSequentialGroup()
      .addContainerGap()
      .add(jPanel1Layout.createParallelGroup(GroupLayout.LEADING)
      .add(jPanel1Layout.createSequentialGroup()
      .add(exact_RB)
      .addPreferredGap(LayoutStyle.RELATED)
      .add(exact_TF, GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE))
      .add(jPanel1Layout.createSequentialGroup()
      .add(regex_RB)
      .addPreferredGap(LayoutStyle.RELATED)
      .add(regex_TF, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)))
      .addContainerGap())
      );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createSequentialGroup()
      .addContainerGap()
      .add(jPanel1Layout.createParallelGroup(GroupLayout.BASELINE)
      .add(exact_RB)
      .add(exact_TF, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
      .addPreferredGap(LayoutStyle.RELATED)
      .add(jPanel1Layout.createParallelGroup(GroupLayout.BASELINE)
      .add(regex_RB)
      .add(regex_TF, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
      .addContainerGap()
      );
    
    reset_button.setMnemonic('R');
    reset_button.setText("Reset");
    reset_button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        reset_buttonActionPerformed(evt);
      }
    });
    
    GroupLayout layout = new GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.LEADING)
      .add(layout.createSequentialGroup()
      .addContainerGap()
      .add(layout.createParallelGroup(GroupLayout.LEADING)
      .add(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
      .add(reset_button))
      .addContainerGap())
      );
    layout.setVerticalGroup(
      layout.createParallelGroup(GroupLayout.LEADING)
      .add(
      layout.createSequentialGroup()
      .addContainerGap()
      .add(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
      .addPreferredGap(LayoutStyle.RELATED)
      .add(reset_button)
      .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );
  }
  
  private void reset_buttonActionPerformed(ActionEvent evt) {
    exact_TF.setText("");
    regex_TF.setText("");
  }
  
  private void ID_regex_RBStateChanged(ChangeEvent evt) {
    regex_TF.setEnabled(regex_RB.isSelected());
  }
  
  private void ID_exact_RBStateChanged(ChangeEvent evt) {
    exact_TF.setEnabled(exact_RB.isSelected());
  }
  
  private JRadioButton exact_RB;
  private JTextField exact_TF;
  private JRadioButton regex_RB;
  private JTextField regex_TF;
  private ButtonGroup buttonGroup1;
  private JPanel jPanel1;
  private JButton reset_button;
  // End of variables declaration
  
  public void initialize() {
    exact_RB.setSelected(true);
    regex_RB.setEnabled(false); // Not yet working so disable
  }
  
  SingletonGenometryModel gmodel;
  
  public void reinitialize(SingletonGenometryModel gmodel) {
  }
  
  static final int MAX_HITS = AnnotBrowserView.THE_LIMIT;
  
  public List searchForSyms(AnnotatedSeqGroup seq_group) {
    List results = new ArrayList();
    for (int i=0; i<seq_group.getSeqCount(); i++) {
      results = searchForSyms(seq_group.getSeq(i), results);
    }
    return results;
  }
  
  List searchForSyms(MutableAnnotatedBioSeq seq, List results) {
    if (exact_RB.isSelected()) {
      results = searchForExactSeq(seq, results);
    } else {
      // search by regular expression
    }
    return results;
  }
  
  /** Searches for matches. Results are appended to given List. */
  List searchForExactSeq(MutableAnnotatedBioSeq seq, List results) {
    String searchstring = exact_TF.getText();
    if (searchstring.length() < 3) {
      throw new RuntimeException("Must use at least three residues.");
      //return Collections.EMPTY_LIST;
    }
    //TODO: check that it only contains legal characters, AGCT, etc.
    
    results = searchForExactSeq(seq, results, searchstring, true);
    results = searchForExactSeq(seq, results, searchstring, false);
    
    return results;
  }
  
  /** Searches for exact matches. Results are appended to given List. */
  List searchForExactSeq(MutableAnnotatedBioSeq vseq, List results, String raw_searchstring, boolean forward) {
    
    String searchstring = forward ? raw_searchstring : DNAUtils.reverseComplement(raw_searchstring);
    
    int residue_offset = 0;
    if (vseq instanceof CompositeNegSeq) {
      residue_offset = ((CompositeNegSeq)vseq).getMin();
    }
    GlyphI seq_glyph;
    
    boolean use_nibseq = (vseq instanceof NibbleBioSeq);
    NibbleBioSeq nibseq = null;
    String residues = null;
    int res_index = -1;
    
    if (use_nibseq) {
      nibseq = (NibbleBioSeq) vseq;
      if (nibseq.isComplete()) {
        searchstring = searchstring.toUpperCase();
        res_index = nibseq.indexOf(searchstring, 0);
      }
    } else {
      residues = vseq.getResidues();
      res_index = residues.indexOf(searchstring, 0);
    }
    
    int length = searchstring.length();
    int hit_count = 0;
    while (res_index >= 0 && hit_count < MAX_HITS) {
      SeqSpan span = null;
      if (forward) {
        span = new SimpleSeqSpan(res_index, res_index+length, vseq);
      } else {
        span = new SimpleSeqSpan(res_index+length, res_index, vseq);
      }
      String tier_name = "Residues '" + raw_searchstring + "'";
      AnnotStyle.getInstance(tier_name, false);
      SeqSymmetry sym = new SearchResultSym(span, tier_name);
      results.add(new AnnotBrowserView.SearchResult("Search Result", sym, span));
      vseq.addAnnotation(sym);
      hit_count++;
      
      if (use_nibseq) {
        if (nibseq.isComplete()) {
          res_index = nibseq.indexOf(searchstring, res_index+1);
        } else {
          res_index = -1;
        }
      } else {
        res_index = residues.indexOf(searchstring, res_index+1);
      }
    }
    
    return results;
    
  }
  
  public class SearchResultSym extends LeafSingletonSymmetry implements TypedSym {
    String type;
    public SearchResultSym(SeqSpan span, String method) {
      super(span);
      this.type = method;
    }
    public String getType() {
      return type;
    }
  }
  
}
