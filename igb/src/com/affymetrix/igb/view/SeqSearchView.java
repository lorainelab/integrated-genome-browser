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
import java.util.*;
import java.util.regex.*;
import javax.swing.*;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.Rectangle2D;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.util.Timer;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genoviz.glyph.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.CompositeNegSeq;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.tiers.*;

public class SeqSearchView extends JComponent implements ActionListener  {

  // A maximum number of hits that can be found in a search.
  // This helps protect against out-of-memory errors.
  final static int MAX_HITS = 100000;
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  JLabel coordsearchL;
  JTextField coordsearchTF;

  JLabel idsearchL;
  JTextField idsearchTF;
  JLabel idHitCountL;

  JTextField entryTF;
  JLabel entryL;
  JLabel hitCountL;

  JTextField regexTF;
  JLabel regexL;
  JLabel regexHitCountL;

  JButton clear_button = new JButton("Clear");

  SeqMapView gviewer;
  Vector glyphs = new Vector();
  Color hitcolor = new Color(150, 150, 255);

  //  public SeqSearchView(SeqMapView gviewer) {
  public SeqSearchView() {
    super();
    gviewer = IGB.getSingletonIGB().getMapView();
    this.setLayout(new BorderLayout());
    JPanel pan1 = new JPanel();
    if (IGB.isSequenceAccessible()) {
      pan1.setLayout(new GridLayout(4, 3));
    }
    else {
      pan1.setLayout(new GridLayout(2, 3));
    }

    coordsearchL = new JLabel("enter coord to center on");
    coordsearchTF = new JTextField("", 30);

    idsearchL = new JLabel("enter id of annotation to find: ");
    idsearchTF = new JTextField("", 30);
    idHitCountL = new JLabel(" No hits");

    entryL = new JLabel("enter residues to find in sequence: ");
    entryTF = new JTextField("", 30);
    hitCountL = new JLabel(" No hits ");

    regexL = new JLabel("enter regex to find in sequence: ");
    regexTF = new JTextField("", 30);
    regexHitCountL = new JLabel(" No hits");

    pan1.add(coordsearchL);
    pan1.add(coordsearchTF);
    pan1.add(clear_button);
    pan1.add(idsearchL);
    pan1.add(idsearchTF);
    pan1.add(idHitCountL);

    if (IGB.isSequenceAccessible()) {
      pan1.add(entryL);
      pan1.add(entryTF);
      pan1.add(hitCountL);
      pan1.add(regexL);
      pan1.add(regexTF);
      pan1.add(regexHitCountL);
    }
    this.add("North", pan1);

    coordsearchTF.addActionListener(this);
    idsearchTF.addActionListener(this);
    entryTF.addActionListener(this);
    regexTF.addActionListener(this);
    clear_button.addActionListener(this);
  }

  private void clearAll() {
    NeoMap map = gviewer.getSeqMap();
    idsearchTF.setText("");
    entryTF.setText("");
    regexTF.setText("");
    idHitCountL.setText(" No hits");
    hitCountL.setText(" No hits ");
    regexHitCountL.setText(" No hits");
    // NOTE: map.removeItem(Vector) is VERY slow!
    if (glyphs.size()>0) { map.removeItem(glyphs); }
    glyphs.clear();
    map.updateWidget();
  }

  public void actionPerformed(ActionEvent evt) {
    //    System.out.println("SeqSearchView received action event: " + evt);
    Object src = evt.getSource();
    NeoMap map = gviewer.getSeqMap();
    BioSeq vseq = gviewer.getViewSeq();

    TransformTierGlyph axis_tier = gviewer.getAxisTier();
    if (src==clear_button) {
      clearAll();
    }
    else if (src == coordsearchTF && coordsearchTF.getText().trim().length()>0) {
      try {
        int pos = Integer.parseInt(coordsearchTF.getText());
        if (vseq != null && pos >= vseq.getMin() && pos <= vseq.getMax()) {
          Rectangle2D vbox = map.getViewBounds();
          double map_start = pos - vbox.width/2;
          map.scroll(map.X, map_start);
          gviewer.setZoomSpotX((double) pos);
          map.updateWidget();
        } else {
          IGB.errorPanel("Position "+pos+" is out of bounds");
          coordsearchTF.setText("");
        }
      } catch (NumberFormatException nfe) {
        IGB.errorPanel("Position "+coordsearchTF.getText()+" is not a number");
      }
    }
    else if (src == entryTF || src == regexTF) {
      Timer tim = new Timer();
      GlyphI seq_glyph = null;
      //    AnnotatedBioSeq vseq = gviewer.getSeq();
      if (vseq==null || ! vseq.isComplete()) {
        IGB.errorPanel("Residues for seq not available, search aborted");
        return;
      }
      int residue_offset = 0;
      if (vseq instanceof CompositeNegSeq) {
        residue_offset = ((CompositeNegSeq)vseq).getMin();
      }
      //      IntList positions = new IntList(1000);
      // find the sequence glyph on axis tier...
      for (int i=0; i<axis_tier.getChildCount(); i++) {
        if (axis_tier.getChild(i) instanceof SequenceGlyph) {
          seq_glyph = axis_tier.getChild(i);
          break;
        }
      }
      if (glyphs.size()>0) {map.removeItem(glyphs);}
      glyphs.clear();

      if (src == entryTF) {
        String searchstring = entryTF.getText();
        if (searchstring.length() == 0) {
          hitCountL.setText(" No hits");
          return;
        }
        if (searchstring.length() < 3) {
          IGB.errorPanel("Must use at least three residues.");
          hitCountL.setText(" No hits");
          return;
        }
       try {
        tim.start();
        boolean use_nibseq = (vseq instanceof NibbleBioSeq);
        NibbleBioSeq nibseq = null;
        String residues = null;
        int res_index;
        if (use_nibseq) {
          System.out.println("searching NibbleBioSeq for ocurrences of \"" +
                             searchstring + "\" in sequence");
          System.out.flush();
	  searchstring = searchstring.toUpperCase();
          nibseq = (NibbleBioSeq)vseq;
          res_index = nibseq.indexOf(searchstring, 0);
        }
        else {
          System.out.println("searching for ocurrences of \"" + searchstring + "\" in residues");
          System.out.flush();
          residues = vseq.getResidues();
          res_index = residues.indexOf(searchstring, 0);
        }

        int seq_index;
        int length = searchstring.length();
        int hit_count = 0;
        while (res_index >= 0 && hit_count < MAX_HITS) {
          //        positions.add(seq_index);
          GlyphI gl = new FillRectGlyph();
          seq_index = res_index + residue_offset;
          //        System.out.println("got hit at: " + seq_index);
          gl.setColor(hitcolor);
          if (seq_glyph != null) {
            gl.setCoords(seq_index, seq_glyph.getCoordBox().y, length, seq_glyph.getCoordBox().height);
            seq_glyph.addChild(gl);
          } else {
            gl.setCoords(seq_index, 10, length, 10);
            axis_tier.addChild(gl);
          }
          glyphs.add(gl);
          hit_count++;

          if (use_nibseq) {
            res_index = nibseq.indexOf(searchstring, res_index+1);
          }
          else {
            res_index = residues.indexOf(searchstring, res_index+1);
          }
        }

	// Search for reverse complement of query string
	//   flip searchstring around, and redo nibseq search...
	String rev_searchstring = DNAUtils.reverseComplement(searchstring);
        if (use_nibseq) {
          System.out.println("searching NibbleBioSeq for ocurrences of \"" + searchstring + "\" in sequence");
          res_index = nibseq.indexOf(rev_searchstring, 0);
        }
        else {
          System.out.println("searching for ocurrences of \"" + searchstring + "\" in residues");
          res_index = residues.indexOf(rev_searchstring, 0);
        }

        while (res_index >= 0 && hit_count < MAX_HITS) {
          GlyphI gl = new FillRectGlyph();
          seq_index = res_index + residue_offset;
	  gl.setColor(hitcolor);
          if (seq_glyph != null) {
            gl.setCoords(seq_index, seq_glyph.getCoordBox().y + 5, length, seq_glyph.getCoordBox().height);
            seq_glyph.addChild(gl);
          } else {
            gl.setCoords(seq_index, 15, length, 10);
            axis_tier.addChild(gl);
          }
          glyphs.add(gl);
          hit_count++;

          if (use_nibseq) {
            res_index = nibseq.indexOf(rev_searchstring, res_index+1);
          }
          else {
            res_index = residues.indexOf(rev_searchstring, res_index+1);
          }
        }

        float match_time = tim.read()/1000f;

        if (hit_count == MAX_HITS) {
          hitCountL.setText(" " + hit_count + " or more hits");
        } else {
          hitCountL.setText(" " + hit_count + " hits");
        }
        System.out.println("time to run search: " + match_time);
       } catch (Exception e) {
         IGB.errorPanel("Exception", e);
       }
        map.updateWidget();
      }
      else if (src == regexTF) {
        if (regexTF.getText().length() == 0) {
          regexHitCountL.setText(" No hits");
          return;
        }
        regexHitCountL.setText(" Working...");
        tim.start();
        String residues = vseq.getResidues();
        try {
          int hit_count = 0;
          int res_index = 0;
          String str = regexTF.getText();
          if (str.length() < 3) {
            IGB.errorPanel("Regular expression must contain at least 3 characters");
            return;
          }
          // It is possible to add the flag Pattern.CASE_INSENSITIVE, but that
          // makes the search slower.  Better to let the user decide whether
          // to add the "(?i)" flag for themselves
          Pattern regex = Pattern.compile(str);
          Matcher matcher = regex.matcher(residues);
          while (matcher.find()) {
            int start = matcher.start(0) + residue_offset;
            int end = matcher.end(0) + residue_offset;
            GlyphI gl = new FillRectGlyph();
            gl.setColor(hitcolor);
            if (seq_glyph != null) {
              gl.setCoords(start, seq_glyph.getCoordBox().y, end-start, seq_glyph.getCoordBox().height);
              seq_glyph.addChild(gl);
            }
            else {
              gl.setCoords(start, 10, end-start, 10);
              axis_tier.addChild(gl);
            }
            glyphs.add(gl);
            hit_count++;
          }
          // WARNING -- need to also search for reverse complement of query string
          //   NOT YET IMPLEMENTED
          // reverse complement query string and search against residues
          // can't just reverse-complement regular expression -- would be a complicated
          //   process...
          float match_time = tim.read()/1000f;
          System.out.println("time to run search: " + match_time);
          regexHitCountL.setText(" " + hit_count + " hits");
          map.updateWidget();
        }
        catch (PatternSyntaxException pse) {
          IGB.errorPanel("Regular expression syntax error...\n" + pse.getMessage());
        }
        catch (Exception ex) {
          IGB.errorPanel("Problem with regular expression...", ex);
        }
      }
    }
    else if (src == idsearchTF) {
      if (idsearchTF.getText().length()==0) {
        idHitCountL.setText(" No hits");
        return;
      }
      String id = idsearchTF.getText().intern();
      findSym(id);
    }

  }

  public void findSym(String id)  {
    if (id == null) { return; }
    System.out.println("looking for id: " + id);
    Map id2sym_hash = IGB.getSymHash();
    NeoMap seqmap = gviewer.getSeqMap();
    SeqSymmetry hitsym = (SeqSymmetry)id2sym_hash.get(id);
    int hitcount = 0;
    int symcount = 0;
    if (hitsym == null) {
      Hashtable modelhash = seqmap.getModelMapping();
      Enumeration enum = modelhash.keys();
      Vector syms = new Vector();
      while (enum.hasMoreElements()) {
	Object obj = enum.nextElement();
	if (obj instanceof SymWithProps) {
	  SymWithProps swp = (SymWithProps)obj;
	  String swpid = (String)swp.getProperty("id");
	  if (swpid == null) { swpid = (String)swp.getProperty("transcript_id"); }
	  // slower, but no assumptions of string interning
	  if ((swpid != null) && (swpid.equals(id)))  {
	    hitsym = swp;
	    break;
	  }
	}
      }
    }
    if (hitsym != null) {
      findSym(hitsym);
      idHitCountL.setText(" match found");
    }
    else  {
      idHitCountL.setText(" no matches");
    }
  }

  public boolean findSym(SeqSymmetry hitsym) {
    boolean found = false;
    if (hitsym != null) {
      MutableAnnotatedBioSeq seq = gmodel.getSelectedSeqGroup().getSeq(hitsym);
      if (seq != null) {
	gmodel.setSelectedSeq(seq);  // event propagation will trigger gviewer to focus on sequence
	ArrayList symlist = new ArrayList(1);
	symlist.add(hitsym);
	gmodel.setSelectedSymmetries(symlist, this);
	//	gviewer.select(hitsym, false, true, true);
	//	gviewer.zoomToSelections();
	found = true;
      }
    }
    return found;
  }

}

