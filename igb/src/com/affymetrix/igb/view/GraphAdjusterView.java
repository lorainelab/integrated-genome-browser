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
import java.io.*;
import javax.swing.*;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.glyph.GraphState;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.util.FloatTransformer;
import com.affymetrix.igb.util.GraphSymUtils;

public class GraphAdjusterView {

  static FileTracker load_dir_tracker = FileTracker.DATA_DIR_TRACKER;

  public static java.util.List transformGraphs(java.util.List grafs, String trans_name, FloatTransformer transformer) {
    int gcount = grafs.size();
    java.util.List newgrafs = new ArrayList(grafs.size());
    for (int i=0; i<gcount; i++) {
      GraphSym graf = (GraphSym)grafs.get(i);

      float[] old_ycoords = graf.getGraphYCoords();
      float[] new_ycoords;

      if (transformer instanceof IdentityTransform) {
        new_ycoords = old_ycoords;
      } else {
        int pcount = old_ycoords.length;
        new_ycoords = new float[pcount];
        for (int k=0; k<pcount; k++) {
          new_ycoords[k] = transformer.transform(old_ycoords[k]);
        }
      }
      String newname = trans_name + " (" + graf.getGraphName() + ") ";
      
     // Transforming on this one seq only, not the whole genome
      String newid = trans_name + " (" + graf.getID() + ") ";
      newid = GraphSymUtils.getUniqueGraphID(newid, graf.getGraphSeq());
      GraphSym newgraf =
        new GraphSym(graf.getGraphXCoords(), new_ycoords, newid, graf.getGraphSeq());
      
      GraphState newstate = newgraf.getGraphState();
      newstate.copyProperties(graf.getGraphState());
      newstate.getTierStyle().setHumanName(newname); // this is redundant

      ((MutableAnnotatedBioSeq)newgraf.getGraphSeq()).addAnnotation(newgraf);
      newgrafs.add(newgraf);
    }
    return newgrafs;
  }

  public static void cloneGraphs(SeqMapView gviewer, java.util.List grafs) {
    //System.out.println("cloning graphs");
    int gcount = grafs.size();
    try  {
      for (int i=0; i<gcount; i++) {
        GraphSym oldsym = (GraphSym)grafs.get(i);
        
        String newid = "Clone (" + oldsym.getID() + ") ";
        newid = GraphSymUtils.getUniqueGraphID(newid, oldsym.getGraphSeq());
      
        GraphSym newsym = new GraphSym(oldsym.getGraphXCoords(), oldsym.getGraphYCoords(),
          newid, oldsym.getGraphSeq());

        newsym.getGraphState().copyProperties(oldsym.getGraphState());
        String newname = "Clone (" + oldsym.getGraphName() + ") ";
        newsym.setGraphName(newname);

        AnnotatedBioSeq aseq = (AnnotatedBioSeq)newsym.getGraphSeq();
        if (aseq instanceof MutableAnnotatedBioSeq) {
          MutableAnnotatedBioSeq mut = (MutableAnnotatedBioSeq) aseq;
          mut.addAnnotation(newsym);
        }
      }
    }
    catch (Exception ex)  {
      ErrorHandler.errorPanel("ERROR", "Error while trying to clone graphs", gviewer.getSeqMap(), ex);
    }
    updateViewer(gviewer);
    //    nwidg.updateWidget();
  }

  static void updateViewer(SeqMapView gviewer)  {
    final SeqMapView current_viewer = gviewer;
    final SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        current_viewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
      }
    });
  }

  public static void deleteGraphs(SingletonGenometryModel gmodel, SeqMapView gviewer, java.util.List grafs) {
    int gcount = grafs.size();
    for (int i=0; i<gcount; i++) {
      GraphSym graf = (GraphSym)grafs.get(i);
      deleteGraph(gviewer, graf);
    }
    gmodel.clearSelectedSymmetries(GraphAdjusterView.class);
    gviewer.getSeqMap().updateWidget();
  }

  /**
   *  Removes a GraphSym from the annotated bio seq it is annotating (if any),
   *     and tries to make sure the GraphSym can be garbage collected.
   *  Tries to delete the GraphGlyph representing the GraphSym.  If the GraphSym
   *  happens to be a child of a tier in the widget, and the tier has no children
   *  left after deleting the graph, then delete the tier as well.
   */
  public static void deleteGraph(SeqMapView gviewer, GraphSym gsym) {
    //System.out.println("deleting graph: " + gsym);

    AnnotatedBioSeq aseq = (AnnotatedBioSeq)gsym.getGraphSeq();
    if (aseq instanceof MutableAnnotatedBioSeq) {
      MutableAnnotatedBioSeq mut = (MutableAnnotatedBioSeq) aseq;
      mut.removeAnnotation(gsym);
    }
    GraphGlyph gl = (GraphGlyph) gviewer.getSeqMap().getItem(gsym);
    if (gl != null) {
      gviewer.getSeqMap().removeItem(gl);
      // clean-up references to the graph, allowing garbage-collection, etc.
      gviewer.select(Collections.EMPTY_LIST);

      // if this is not a floating graph, then it's in a tier,
      //    so check tier -- if this graph is only child, then get rid of the tier also
      if (gviewer.getSeqMap() instanceof AffyTieredMap &&
          (! gl.getGraphState().getFloatGraph()) ) {
        AffyTieredMap map = (AffyTieredMap)gviewer.getSeqMap();
        GlyphI parentgl = gl.getParent();
        parentgl.removeChild(gl);
        if (parentgl.getChildCount() == 0) {  // if no children left in tier, then remove it
          if (parentgl instanceof TierGlyph) {
            map.removeTier((TierGlyph)parentgl);
            
            // This method doesn't exist. But could if we really cared
            // about cleaning-up references to this GraphState and its Tier
            //gviewer.deleteGraphTier(gsym.getGraphState());
            
            map.packTiers(false, true, false);
            map.stretchToFit(false, false);
          }
        }
      }
    }
  }


  public static void saveGraphs(SeqMapView gviewer, java.util.List grafs) {
    int gcount = grafs.size();
    if (gcount > 1) {
      // actually shouldn't get here, since save button is disabled if more than one graph
      ErrorHandler.errorPanel("Can only save one graph at a time");
    }
    else if (gcount == 1) {
      GraphSym gsym = (GraphSym)grafs.get(0);
      try {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(load_dir_tracker.getFile());
        int option = chooser.showSaveDialog(gviewer.getFrame());
        if (option == JFileChooser.APPROVE_OPTION) {
          load_dir_tracker.setFile(chooser.getCurrentDirectory());
          File fil = chooser.getSelectedFile();
          GraphSymUtils.writeGraphFile(gsym, fil.getAbsolutePath());
        }
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel("Error saving graph", ex);
      }
    }
  }

  public static void changeColor(java.util.List graf_syms, SeqMapView gviewer) {
    int scount = graf_syms.size();
    if (scount > 0) {
      // Set an initial color so that the "reset" button will work.
      GraphSym graf_0 = (GraphSym) graf_syms.get(0);
      GraphGlyph gl_0 = (GraphGlyph) gviewer.getSeqMap().getItem(graf_0);
      Color initial_color = gl_0.getColor();
      Color col = JColorChooser.showDialog((Component) gviewer.getSeqMap(),
        "Graph Color Chooser", initial_color);
      // Note: If the user selects "Cancel", col will be null
      if (col != null) {
	for (int i=0; i<scount; i++) {
	  GraphSym graf = (GraphSym) graf_syms.get(i);
	  // using getItems() instead of getItem(), in case graph sym is represented by multiple graph glyphs
	  java.util.List glist = gviewer.getSeqMap().getItems(graf);
	  if (glist != null) {
	    int glyphcount = glist.size();
	    for (int k=0; k<glyphcount; k++) {
	      GraphGlyph gl = (GraphGlyph)glist.get(k);
	      gl.setColor(col); // this automatically sets the GraphState color
	      // if graph is in a tier, change foreground color of tier also
	      //   (which in turn triggers change in color for TierLabelGlyph...)
	      if (gl.getParent() instanceof TierGlyph) {
		gl.getParent().setForegroundColor(col);
                Vector views = gl.getParent().getScene().getViews();
                for (int qq=0; qq<views.size(); qq++) {
                  ViewI v = (ViewI) views.get(qq);
                  if (gl.withinView(v)) {
                    gl.draw(v);
                  }
                }
	      }
	    }
	  }
	}
      }
      gviewer.getSeqMap().updateWidget();
    }
  }

  /** Parse a String floating-point number that may optionally end with a "%" symbol. */
  public static float parsePercent(String text) throws NumberFormatException {
    if (text.endsWith("%")) {
      text = text.substring(0, text.length()-1);
    }

    float f = Float.parseFloat(text);
    return f;
  }
}

/*
 * Logarithm base change: log_base_b(x) = log_base_a(x)/log_base_a(b)
 * For example:
 *     log10(x) = ln(x)/ln(10) = ln(x)/2.30258 = 0.4343 * ln(x)
 *
 *  use Math.ln(x) for ln(x)
 *  use Math.exp(x) for e^x (inverse of ln(x))
 *  use Math.pow(y, x) for y^x (inverse of log_base_y(x)
 *
 */
class LogNatural implements FloatTransformer {
  static float LN1 = (float)Math.log(1); // should be 0...
  public float transform(float x) {
    // could pick any threshold > 0 to cut off low end at,
    // but thresholding at 1 for similarity to GTRANS
    return (x <= 1) ? LN1 : (float)Math.log(x);
  }
  public float inverseTransform(float y) {
    throw new RuntimeException("LogNatural.inverseTransform called, " +
                               "but LogNatural is not an invertible function");
  }
  /** not invertible because values < 1 before transform cannot be recovered... */
  public boolean isInvertible()  { return false; }
}


class LogBase10 implements FloatTransformer {
  static double LN10 = Math.log(10);
  static float LOG10_1 = (float)(Math.log(1)/LN10);
  public float transform(float x) {
    // return (float)(Math.log(x)/LN10);
    return (x <= 1) ? LOG10_1 : (float)(Math.log(x)/LN10);
  }
  public float inverseTransform(float x) {
    throw new RuntimeException("LogBase10.inverseTransform called, " +
                               "but LogBase10 is not an invertible function");
  }
  public boolean isInvertible()  { return false; }
}


class LogBase2 implements FloatTransformer {
  static double LN2 = Math.log(2);
  static float LOG2_1 = (float)(Math.log(1)/LN2);
  public float transform(float x) {
    return (x <= 1) ? LOG2_1 : (float)(Math.log(x)/LN2);
  }
  public float inverseTransform(float x) {
    throw new RuntimeException("LogBase2.inverseTransform called, " +
                               "but LogBase2 is not an invertible function");
  }
  public boolean isInvertible()  { return false; }
}

class LogTransform implements FloatTransformer {
  double base;
  double LN_BASE;
  float LOG_1;
  public LogTransform(double base) {
    this.base = base;
    LN_BASE = Math.log(base);
    LOG_1 = (float)(Math.log(1)/LN_BASE);
  }
  public float transform(float x) {
    return (x <= 1) ? LOG_1 : (float)(Math.log(x)/LN_BASE);
  }
  public float inverseTransform(float x) {
    return (float)(Math.pow(base, x));
  }
  public boolean isInvertible() { return true; }
}

/**
 *   Generalized replacement for LogNatural, LogBase2, LogBase10, etc.
 *    transforms x to base raised to the x (base^x)
 */
class PowTransform implements FloatTransformer {
  double base;
  double LN_BASE;
  float LOG_1;
  public PowTransform(double base) {
    this.base = base;
    // if base == Math.E, then LN_BASE will be 1
    LN_BASE = Math.log(base);
    LOG_1 = (float)(Math.log(1)/LN_BASE);
  }
  public float transform(float x) {
    return (float)Math.pow(base, x);
  }
  public float inverseTransform(float x) {
    //    throw new RuntimeException("LogBase2.inverseTransform called, " +
    //                               "but LogBase2 is not an invertible function");
    return (x <= 1) ? LOG_1 : (float)(Math.log(x)/LN_BASE);
  }
  public boolean isInvertible() { return true; }
}

/**
 *  alternative implementation of PowTransform.
 *  since raising to a power is inverse of taking logarithm,
 *     should be able to implement as inverse of LogTransform
 *     (transform() calls LogTransform.inverseTransform(),
 *      inverseTransform() calls LogTransform.transform())
 */
class InverseLogTransform implements FloatTransformer {
  LogTransform inner_trans;
  public InverseLogTransform(double base) {
    inner_trans = new LogTransform(base);
  }
  public float transform(float x) { return inner_trans.inverseTransform(x); }
  public float inverseTransform(float x) { return inner_trans.transform(x); }
  public boolean isInvertible() { return true; }
}

class IdentityTransform implements FloatTransformer {
  public IdentityTransform() {}
  public float transform(float x) { return x; }
  public float inverseTransform(float x) { return x; }
  public boolean isInvertible() { return true; }
}
