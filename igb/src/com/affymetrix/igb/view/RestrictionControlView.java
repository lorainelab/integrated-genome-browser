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

package com.affymetrix.igb.view;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.CompositeNegSeq;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.*;

public final class RestrictionControlView extends JComponent
     implements ListSelectionListener, ActionListener  {
  SeqMapView gviewer;
  Hashtable site_hash = new Hashtable();
  JList siteList;
  JPanel labelP;
  Vector sites = new Vector();
  static Color colors[] = {
    Color.magenta,
    new Color(0x00cd00),
    Color.orange,
    new Color(0x00d7d7),
    new Color(0xb50000),
    Color.blue,
    Color.gray,
    Color.pink};
  JLabel labels[];
  JButton actionB;
  JButton clearB;
  
  public RestrictionControlView() {
    super();
    this.gviewer = Application.getSingleton().getMapView();
    boolean load_success  = true;

    //String rest_file = "rest_enzymes"; // located in same directory as the IGB class
	String rest_file = "/rest_enzymes";
    InputStream file_input_str =
    Application.class.getResourceAsStream(rest_file);
	/*
    if (file_input_str == null) {
      // Look for restriction enzymes file as both rest_enzymes" and "/rest_enzymes".
      rest_file = "/" + rest_file;
      file_input_str = Application.class.getResourceAsStream(rest_file);
	 */
      if (file_input_str == null) {
        Application.errorPanel("Cannot open restriction enzyme file",
        "Cannot find restriction enzyme file '"+rest_file+"'.\n"+
        "Restriction mapping will not be available.");
      }
    /* } */
    
    DataInputStream distr = null;
    if (file_input_str == null) {
      load_success = false;
    }
    else try {
      distr = new DataInputStream(file_input_str);
      StringTokenizer string_toks;
      String site_name, site_dna;
      String reply_string;
      //    String reply_string = distr.readLine();
      int rcount = 0;
      while ((reply_string = distr.readLine()) != null) {
	//	System.out.println(reply_string);
	string_toks = new StringTokenizer(reply_string);
	site_name = string_toks.nextToken();
	site_dna = string_toks.nextToken();
	site_hash.put(site_name, site_dna);
	sites.add(site_name);
	rcount++;
	//	System.out.println("site " + site_name + ", " + site_dna);
	//      reply_string = distr.readLine();
      }
      //System.out.println("Loaded restriction sites for RestrictionControlView, count: " + rcount);
    }
    catch (Exception ex) {
      load_success = false;
      Application.errorPanel("Problem loading restriction site file, aborting load\n"+
      ex.toString());
    } finally {
			GeneralUtils.safeClose(distr);
			GeneralUtils.safeClose(file_input_str);
    }

    if (load_success) {
      siteList = new JList(sites);
      JScrollPane scrollPane = new JScrollPane(siteList);
      labelP = new JPanel();
      labelP.setBackground(Color.white);
      labelP.setLayout(new GridLayout(colors.length, 1));

      labels = new JLabel[colors.length];
      JLabel label;
      for(int i=0; i<colors.length; i++) {
	label = new JLabel();
	label.setForeground(colors[i]);
	//      label.setBackground(colors[i]);
	label.setText("           ");
	labelP.add(label);
	labels[i] = label;
      }

      this.setLayout(new BorderLayout());
      //    this.setLayout(new GridLayout(1, 2));
      scrollPane.setPreferredSize(new Dimension(100, 100));
      //    this.add(scrollPane);
      //    this.add(labelP);
      this.add("West", scrollPane);
      actionB = new JButton("Map Selected Restriction Sites");
      clearB = new JButton("Clear");
      this.add("Center", new JScrollPane(labelP));

      Container button_container = new JPanel();
      button_container.setLayout(new GridLayout(5,1));
      button_container.add(actionB);
      button_container.add(clearB);
      this.add("East", button_container);

      siteList.addListSelectionListener(this);
      actionB.addActionListener(this);
      clearB.addActionListener(this);
    }
    else {
      this.setLayout(new BorderLayout());
      JLabel lab = new JLabel("Restriction site mapping not available.");
      this.add("North", lab);
    }
  }

  public void valueChanged(ListSelectionEvent evt) {
    Object src = evt.getSource();
    if (src == siteList) {
      Object[] selected_names = siteList.getSelectedValues();
      for (int i=0; i<labels.length; i++) {
	if (i < selected_names.length) {
	  labels[i].setText((String)(selected_names[i]));
	}
	else {
	  labels[i].setText("");
	}
      }
    }
  }

  private void clearAll() {
    clearGlyphs();
    siteList.clearSelection();
  }

  private void clearGlyphs() {
    NeoMap map = gviewer.getSeqMap();
    map.removeItem(glyphs);
    glyphs.clear();
  }

  public void actionPerformed(ActionEvent evt) {

    if (evt.getSource()==clearB) {
      clearAll();
      return;
    }

    clearGlyphs();

    BioSeq vseq = gviewer.getViewSeq();
    if (vseq==null || ! vseq.isComplete()) {
      Application.errorPanel("Residues for seq not available, search aborted.");
      return;
    }

    for (int i=0; i<labels.length; i++) {
      String site_name = labels[i].getText();
      // done when hit first non-labelled JLabel
      if (site_name == null || site_name.equals("")) {
	break;
      }
      String site_residues = (String)site_hash.get(site_name);
      if (site_residues == null) { continue; }
      glyphifyMatches(site_residues, colors[i]);
    }
  }

  /**
   *  keep track of added glyphs
   */
  Vector glyphs = new Vector();

  public void glyphifyMatches(String site, Color col) {
    NeoMap map = gviewer.getSeqMap();
    TransformTierGlyph axis_tier = gviewer.getAxisTier();
    GlyphI seq_glyph = null;
    //    AnnotatedBioSeq aseq = gviewer.getSeq();
    BioSeq vseq = gviewer.getViewSeq();
    if (vseq==null || ! vseq.isComplete()) {
      Application.errorPanel("Residues for seq not available, search aborted.");
      return;
    }
    int residue_offset = 0;
    String residues = null;
    SmartAnnotBioSeq nibseq = null;
    boolean use_nibseq = (vseq instanceof SmartAnnotBioSeq);
    if (use_nibseq) {
      nibseq = (SmartAnnotBioSeq)vseq;
    }
    else {
      residues = vseq.getResidues();
    }
    if (vseq instanceof CompositeNegSeq) {
      residue_offset = ((CompositeNegSeq)vseq).getMin();
    }
    // find the sequence glyph on axis tier...
    for (int i=0; i<axis_tier.getChildCount(); i++) {
      if (axis_tier.getChild(i) instanceof SequenceGlyph) {
	seq_glyph = axis_tier.getChild(i);
	break;
      }
    }

    System.out.println("searching for occurrences of \"" + site + "\" in sequence");
    int res_index;
    if (use_nibseq)  { res_index = nibseq.indexOf(site, 0); }
    else  { res_index = residues.indexOf(site, 0); }
    int seq_index;
    int length = site.length();
    int hit_count = 0;
    while (res_index >= 0) {
      seq_index = res_index + residue_offset;
      GlyphI gl = new FillRectGlyph();
      gl.setColor(col);
      if (seq_glyph != null) {
	gl.setCoords(seq_index, seq_glyph.getCoordBox().y,
		     length, seq_glyph.getCoordBox().height);
	seq_glyph.addChild(gl);
      }
      else {
	gl.setCoords(seq_index, 10, length, 10);
	axis_tier.addChild(gl);
      }
      glyphs.add(gl);
      hit_count++;
      if (use_nibseq) { res_index = nibseq.indexOf(site, res_index+1); }
      else { res_index = residues.indexOf(site, res_index+1); }
    }
    //    hitCountL.setText("" + hit_count + " hits");
    System.out.println(site + ", hits = " + hit_count);
    map.updateWidget();
  }

}
