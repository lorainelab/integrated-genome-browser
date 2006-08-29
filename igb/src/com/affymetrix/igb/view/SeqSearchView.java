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
    this.add("Center", new JPanel());// a blank panel: improves appearance in JDK1.5

    coordsearchTF.addActionListener(this);
    idsearchTF.addActionListener(this);
    entryTF.addActionListener(this);
    regexTF.addActionListener(this);
    clear_button.addActionListener(this);
  }

  private void clearAll() {
    NeoMap map = gviewer.getSeqMap();
    coordsearchTF.setText("");
    idsearchTF.setText("");
    entryTF.setText("");
    regexTF.setText("");
    idHitCountL.setText(" No hits");
    hitCountL.setText(" No hits ");
    regexHitCountL.setText(" No hits");
    clearResults();
    map.updateWidget();
  }

  // remove the previous search results from the map.
  private void clearResults() {
    if (glyphs.size()>0) {
      // NOTE: map.removeItem(Vector) is VERY slow!
      //NeoMap map = gviewer.getSeqMap();
      //map.removeItem(glyphs); 

      // Simply re-drawing the SeqMapView is faster in cases where there
      // are lots of results to erase, and not too slow to use all the time.
      gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, true);
    }
    glyphs.clear();
  }
  
  public void actionPerformed(ActionEvent evt) {
    //    System.out.println("SeqSearchView received action event: " + evt);
    Object src = evt.getSource();
    NeoMap map = gviewer.getSeqMap();
    BioSeq vseq = gviewer.getViewSeq();

    if (src==clear_button) {
      clearAll();
    }
    else if (src == coordsearchTF && coordsearchTF.getText().trim().length()>0) {
      try {
        int pos = Integer.parseInt(coordsearchTF.getText());
       //  if (vseq != null && pos >= vseq.getMin() && pos <= vseq.getMax()) {
        if (vseq != null && pos >= 0 && pos <= vseq.getLength()) {
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
      clearResults();
      
      Timer tim = new Timer();
      GlyphI seq_glyph = null;
      //    AnnotatedBioSeq vseq = gviewer.getSeq();
      if (vseq==null || ! vseq.isComplete()) {
        if (src == entryTF) {
          hitCountL.setText(" No hits");
        } else if (src == regexTF) {
          regexHitCountL.setText(" No hits");
        }
        IGB.errorPanel("Residues for seq not available, search aborted");
        return;
      }
      int residue_offset = 0;
      if (vseq instanceof CompositeNegSeq) {
        residue_offset = ((CompositeNegSeq)vseq).getMin();
      }
      TransformTierGlyph axis_tier = gviewer.getAxisTier();
      //      IntList positions = new IntList(1000);
      // find the sequence glyph on axis tier...
      for (int i=0; i<axis_tier.getChildCount(); i++) {
        if (axis_tier.getChild(i) instanceof AbstractResiduesGlyph) {
          seq_glyph = axis_tier.getChild(i);
          break;
        }
      }

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
        int res_index = -1;
	try {
	  if (use_nibseq) {
	    System.out.flush();
	    searchstring = searchstring.toUpperCase();
	    System.out.println("searching NibbleBioSeq for ocurrences of \"" +
			       searchstring + "\" in sequence");
	    nibseq = (NibbleBioSeq)vseq;
	    res_index = nibseq.indexOf(searchstring, 0);
	  }
	  else {
	    System.out.println("searching for ocurrences of \"" + searchstring + "\" in residues");
	    System.out.flush();
	    residues = vseq.getResidues();
	    res_index = residues.indexOf(searchstring, 0);
	  }
	}
	catch (Exception ex) {
	  IGB.errorPanel("Only partial residues loaded, must have all residues for seq loaded first");
	  return;
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
            // when adding as a child of the CharSeqGlyph, it automatically gets re-positioned, so we move it back where we want it
            gl.setCoords(seq_index, seq_glyph.getCoordBox().y, length, seq_glyph.getCoordBox().height/2);
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
          System.out.println("searching NibbleBioSeq for ocurrences of \"" + rev_searchstring + "\" in sequence");
          res_index = nibseq.indexOf(rev_searchstring, 0);
        }
        else {
          System.out.println("searching for ocurrences of \"" + rev_searchstring + "\" in residues");
          res_index = residues.indexOf(rev_searchstring, 0);
        }

        while (res_index >= 0 && hit_count < MAX_HITS) {
          GlyphI gl = new FillRectGlyph();
          seq_index = res_index + residue_offset;
	  gl.setColor(hitcolor);
          if (seq_glyph != null) {
            gl.setCoords(seq_index, seq_glyph.getCoordBox().y + 5, length, seq_glyph.getCoordBox().height);
            seq_glyph.addChild(gl);
            // when adding as a child of the CharSeqGlyph, it automatically gets re-positioned, so we move it back where we want it
            gl.setCoords(seq_index, seq_glyph.getCoordBox().y + seq_glyph.getCoordBox().height/2, length, seq_glyph.getCoordBox().height/2);
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
    //System.out.println("looking for id: " + id);
    SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    
    java.util.List sym_list = group.findSyms(id);
    
    if (sym_list != null && ! sym_list.isEmpty()) {
      idHitCountL.setText(sym_list.size() + " matches found");
      gmodel.setSelectedSymmetriesAndSeq(sym_list, this);
    }
    else  {
      idHitCountL.setText(" no matches");
    }
  }
}

