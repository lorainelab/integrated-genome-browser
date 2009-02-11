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

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genoviz.util.ErrorHandler;

import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

final class FindAnnotationsPanel extends JPanel {
  
  JDialog the_dialog;

  /** Creates new form FindAnnotationsFinal */
  public FindAnnotationsPanel() {
    initComponents();
  }
  
  private void initComponents() {
    buttonGroup1 = new ButtonGroup();
    buttonGroup2 = new ButtonGroup();
    buttonGroup3 = new ButtonGroup();
    jPanel1 = new JPanel();
    ID_exact_RB = new JRadioButton();
    ID_starts_with_RB = new JRadioButton();
    ID_regex_RB = new JRadioButton();
    ID_exact_TF = new JTextField();
    ID_starts_with_TF = new JTextField();
    ID_regex_TF = new JTextField();
    ID_all_RB = new JRadioButton();
    ID_between_1_TF = new JTextField();
    ID_between_2_TF = new JTextField();
    ID_and_Label = new JLabel();
    ID_between_RB = new JRadioButton();
    jPanel2 = new JPanel();
    SEQ_all_RB = new JRadioButton();
    SEQ_name_RB = new JRadioButton();
    sequence_CB = new JComboBox();
    reset_button = new JButton();

    jPanel1.setBorder(BorderFactory.createTitledBorder("Find Annotations By..."));
    buttonGroup1.add(ID_exact_RB);
    ID_exact_RB.setMnemonic('E');
    ID_exact_RB.setText("Exact ID");
    ID_exact_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    ID_exact_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));
    ID_exact_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        ID_exact_RBStateChanged(evt);
      }
    });

    buttonGroup1.add(ID_starts_with_RB);
    ID_starts_with_RB.setMnemonic('D');
    ID_starts_with_RB.setText("ID starts with");
    ID_starts_with_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    ID_starts_with_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));
    ID_starts_with_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        ID_starts_with_RBStateChanged(evt);
      }
    });

    buttonGroup1.add(ID_regex_RB);
    ID_regex_RB.setMnemonic('x');
    ID_regex_RB.setText("Regular Expression");
    ID_regex_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    ID_regex_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));
    ID_regex_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        ID_regex_RBStateChanged(evt);
      }
    });

    ID_exact_TF.setEnabled(false);

    ID_starts_with_TF.setEnabled(false);

    ID_regex_TF.setEnabled(false);

    buttonGroup1.add(ID_all_RB);
    ID_all_RB.setMnemonic('I');
    ID_all_RB.setText("All IDs");
    ID_all_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    ID_all_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));

    ID_between_1_TF.setEnabled(false);

    ID_between_2_TF.setEnabled(false);

    ID_and_Label.setText("and");

    buttonGroup1.add(ID_between_RB);
    ID_between_RB.setMnemonic('b');
    ID_between_RB.setText("ID between");
    ID_between_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    ID_between_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));
    ID_between_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        ID_between_RBStateChanged(evt);
      }
    });

    GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(GroupLayout.LEADING)
      .add(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .add(jPanel1Layout.createParallelGroup(GroupLayout.LEADING)
          .add(ID_all_RB)
          .add(jPanel1Layout.createSequentialGroup()
            .add(ID_regex_RB)
            .addPreferredGap(LayoutStyle.RELATED)
            .add(ID_regex_TF, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
          .add(jPanel1Layout.createSequentialGroup()
            .add(ID_between_RB)
            .addPreferredGap(LayoutStyle.RELATED)
            .add(ID_between_1_TF, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
            .addPreferredGap(LayoutStyle.RELATED)
            .add(ID_and_Label)
            .addPreferredGap(LayoutStyle.RELATED)
            .add(ID_between_2_TF, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE))
          .add(jPanel1Layout.createSequentialGroup()
            .add(ID_starts_with_RB)
            .addPreferredGap(LayoutStyle.RELATED)
            .add(ID_starts_with_TF, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
          .add(jPanel1Layout.createSequentialGroup()
            .add(ID_exact_RB)
            .addPreferredGap(LayoutStyle.RELATED)
            .add(ID_exact_TF, GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE)))
        .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(GroupLayout.LEADING)
      .add(jPanel1Layout.createSequentialGroup()
        .add(ID_all_RB)
        .addPreferredGap(LayoutStyle.RELATED)
        .add(jPanel1Layout.createParallelGroup(GroupLayout.BASELINE)
          .add(ID_exact_RB)
          .add(ID_exact_TF, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(LayoutStyle.RELATED)
        .add(jPanel1Layout.createParallelGroup(GroupLayout.BASELINE)
          .add(ID_starts_with_RB)
          .add(ID_starts_with_TF, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(LayoutStyle.RELATED)
        .add(jPanel1Layout.createParallelGroup(GroupLayout.BASELINE)
          .add(ID_between_RB)
          .add(ID_between_2_TF, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
          .add(ID_and_Label)
          .add(ID_between_1_TF, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(LayoutStyle.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .add(jPanel1Layout.createParallelGroup(GroupLayout.BASELINE)
          .add(ID_regex_RB)
          .add(ID_regex_TF, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .add(52, 52, 52))
    );

    jPanel2.setBorder(BorderFactory.createTitledBorder("Sequence"));
    buttonGroup2.add(SEQ_all_RB);
    SEQ_all_RB.setMnemonic('q');
    SEQ_all_RB.setText("All Sequences");
    SEQ_all_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    SEQ_all_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));

    buttonGroup2.add(SEQ_name_RB);
    SEQ_name_RB.setMnemonic('S');
    SEQ_name_RB.setText("Sequence");
    SEQ_name_RB.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    SEQ_name_RB.setMargin(new java.awt.Insets(0, 0, 0, 0));
    SEQ_name_RB.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        SEQ_name_RBStateChanged(evt);
      }
    });

    sequence_CB.setEnabled(false);

    GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
    jPanel2.setLayout(jPanel2Layout);
    jPanel2Layout.setHorizontalGroup(
      jPanel2Layout.createParallelGroup(GroupLayout.LEADING)
      .add(jPanel2Layout.createSequentialGroup()
        .addContainerGap()
        .add(jPanel2Layout.createParallelGroup(GroupLayout.LEADING)
          .add(SEQ_all_RB)
          .add(jPanel2Layout.createSequentialGroup()
            .add(SEQ_name_RB)
            .addPreferredGap(LayoutStyle.RELATED)
            .add(sequence_CB, 0, 246, Short.MAX_VALUE)))
        .addContainerGap())
    );
    jPanel2Layout.setVerticalGroup(
      jPanel2Layout.createParallelGroup(GroupLayout.LEADING)
      .add(jPanel2Layout.createSequentialGroup()
        .add(SEQ_all_RB)
        .addPreferredGap(LayoutStyle.RELATED)
        .add(jPanel2Layout.createParallelGroup(GroupLayout.BASELINE)
          .add(SEQ_name_RB)
          .add(sequence_CB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
          .add(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .add(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .add(reset_button))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(GroupLayout.LEADING)
      .add(layout.createSequentialGroup()
        .addContainerGap()
        .add(jPanel1, GroupLayout.PREFERRED_SIZE, 149, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.RELATED)
        .add(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(LayoutStyle.RELATED)
        .add(reset_button)
        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
  }

  private void reset_buttonActionPerformed(ActionEvent evt) {                                             
    ID_exact_TF.setText("");
    ID_between_1_TF.setText("");
    ID_between_2_TF.setText("");
    ID_regex_TF.setText("");
    ID_starts_with_TF.setText("");
    ID_all_RB.setSelected(true);
    SEQ_all_RB.setSelected(true);
  }                                            

  private void SEQ_name_RBStateChanged(ChangeEvent evt) {                                         
    sequence_CB.setEnabled(SEQ_name_RB.isSelected());
  }                                        

  private void ID_regex_RBStateChanged(ChangeEvent evt) {                                         
    ID_regex_TF.setEnabled(ID_regex_RB.isSelected());
  }                                        

  private void ID_between_RBStateChanged(ChangeEvent evt) {                                           
    ID_between_1_TF.setEnabled(ID_between_RB.isSelected());
    ID_between_2_TF.setEnabled(ID_between_RB.isSelected());
  }                                          

  private void ID_starts_with_RBStateChanged(ChangeEvent evt) {                                               
    ID_starts_with_TF.setEnabled(ID_starts_with_RB.isSelected());
  }                                              

  private void ID_exact_RBStateChanged(ChangeEvent evt) {                                         
    ID_exact_TF.setEnabled(ID_exact_RB.isSelected());
  }                                        
  
  
  private JRadioButton ID_all_RB;
  private JLabel ID_and_Label;
  private JTextField ID_between_1_TF;
  private JTextField ID_between_2_TF;
  private JRadioButton ID_between_RB;
  private JRadioButton ID_exact_RB;
  private JTextField ID_exact_TF;
  private JRadioButton ID_regex_RB;
  private JTextField ID_regex_TF;
  private JRadioButton ID_starts_with_RB;
  private JTextField ID_starts_with_TF;
  private JRadioButton SEQ_all_RB;
  private JRadioButton SEQ_name_RB;
  private ButtonGroup buttonGroup1;
  private ButtonGroup buttonGroup2;
  private ButtonGroup buttonGroup3;
  private JPanel jPanel1;
  private JPanel jPanel2;
  private JButton reset_button;
  private JComboBox sequence_CB;
  // End of variables declaration                   

  public void initialize() {
    ID_all_RB.setSelected(true);
    SEQ_all_RB.setSelected(true);
  }
  
  GenometryModel gmodel;

  public void reinitialize(GenometryModel gmodel) {
    // set-up the sequence combo_box
    sequence_CB.removeAllItems();
    this.gmodel = gmodel;
    AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    if (group != null) {
      for (int i=0; i<group.getSeqCount(); i++) {
        BioSeq seq = group.getSeq(i);
        sequence_CB.addItem(seq.getID());
      }
    }
    
    BioSeq selected_seq = gmodel.getSelectedSeq();
    if (selected_seq != null) {
      sequence_CB.setSelectedItem(selected_seq.getID());
    }    
  }
    
  /** Returns a Set of IDs */
  public Set searchForID(AnnotatedSeqGroup group) {
    Set results;
    
    if (ID_all_RB.isSelected()) {
     results = group.getSymmetryIDs();
    
    } else if (ID_exact_RB.isSelected()) {

      results = new HashSet();
      results.add(ID_exact_TF.getText());

    } else if (ID_starts_with_RB.isSelected()) {

      String start = ID_starts_with_TF.getText();
      String end = ID_starts_with_TF.getText() + Character.toString(Character.MAX_VALUE);
      //String end = TF_ID_starts_with.getText() + "Z";
      results = findIDsStartToEnd(group, start, end);

    } else if (ID_between_RB.isSelected()) {
      
      String start = ID_between_1_TF.getText();
      String end = ID_between_2_TF.getText();
      results = findIDsStartToEnd(group, start, end);

    } else if (ID_regex_RB.isSelected()) {
      
      String pattern_string = ID_regex_TF.getText();

      try {
        results = findIDsByRegex(group, pattern_string);
        
      } catch (PatternSyntaxException pse) {
        
        ErrorHandler.errorPanel("Bad Regex", "Invalid Regular Expression: " + pattern_string, pse);
        results = Collections.EMPTY_SET;
      }
      
      
    } else {
      throw new RuntimeException("Requested search function not implemented.");
    }

    return results;
  }

  SynonymLookup lookup = SynonymLookup.getDefaultLookup();

  public boolean filterBySequence(BioSeq seq) {
    if (seq == null) {
      return false;
    }
    
    if (SEQ_all_RB.isSelected()) {
      
      // When "All sequences" is selected, what we really want to see
      // is all sequences in the current seq group.  Not really all sequences.

      return gmodel.getSelectedSeqGroup().getSeqList().contains(seq);

    } else if (SEQ_name_RB.isSelected()) {
      
      String selected_seq_id = (String) sequence_CB.getSelectedItem();
      if (selected_seq_id == null) {
        return false;
      }

      if (lookup.isSynonym(selected_seq_id, seq.getID())) {
        return true;
      }
    }

    return false;
  }

  MutableSeqSpan span_1 = new SimpleMutableSeqSpan();
  
  protected Set findIDsStartToEnd(AnnotatedSeqGroup seq_group, String start, String end) {
    if (seq_group == null) {
      return Collections.EMPTY_SET;
    }

    Set sym_ids;
    // if end<start, then switch the order of the search,
    // but don't do that if start or end is blank, because 
    // "a" to "" is different from "" to "a" and both searches are valid
    if (start.length() > 0 && end.length() > 0 && start.compareTo(end) > 0) {
      sym_ids = seq_group.getSymmetryIDs(end, start);
    } else {
      sym_ids = seq_group.getSymmetryIDs(start, end);
    }
    
    return sym_ids;
  }
  
  List findByIDs(AnnotatedSeqGroup seq_group, Collection sym_ids) {
    List the_list = new ArrayList();

    Iterator sym_iter = sym_ids.iterator();
    while (sym_iter.hasNext() && the_list.size() < AnnotBrowserView.THE_LIMIT) {
      String key = (String) sym_iter.next();
      the_list.addAll(seq_group.findSyms(key));
    }
    
    return the_list;
  }

  Set findIDsByRegex(AnnotatedSeqGroup seq_group, String regex) throws PatternSyntaxException {
    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher("");
    
    Set matched_ids = new HashSet();
    
    Set all_ids = seq_group.getSymmetryIDs();
    Iterator iterator = all_ids.iterator();
    
    int i=0;
    while (iterator.hasNext()) {
      String id = (String) iterator.next();
      i++;
    matcher.reset(id);
      if (matcher.matches()) {
        matched_ids.add(id);
      }
    }
    
    return matched_ids;
  }

  public List searchForSyms(AnnotatedSeqGroup seq_group) {
    List results = new ArrayList();

    if (seq_group == null) {
      return results;
    }

    Set sym_ids = searchForID(seq_group);

    List entries = new ArrayList(sym_ids);
    
    Iterator iter = sym_ids.iterator();
    while (iter.hasNext() && results.size() < AnnotBrowserView.THE_LIMIT) {
      String key = (String) iter.next();
      List the_list = seq_group.findSyms(key);
      
      for (int k=0; k<the_list.size(); k++) {
        SeqSymmetry sym = (SeqSymmetry) the_list.get(k);

        int span_count = sym.getSpanCount();
        for (int i=0; i<span_count; i++) {
          SeqSpan span = sym.getSpan(i);
          if (span == null) continue;

          BioSeq seq = span.getBioSeq();
          if (filterBySequence(seq)) /* (seq_list.contains(seq)*/ {
        
//        if (filterBySpan(span)) {
              results.add(new AnnotBrowserView.SearchResult(key, sym, span));
//            }
          }
        }
      }
    }
    
    return results;
  }
}
