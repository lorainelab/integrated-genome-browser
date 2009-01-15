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

package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.GraphSymFloat;
import java.awt.Color;
import java.awt.Component;
import java.io.*;
import javax.swing.*;
import java.util.*;
import java.util.List;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.FloatTransformer;
import com.affymetrix.genometryImpl.util.FloatTransformer.IdentityTransform;
import com.affymetrix.igb.util.GraphSaverFileChooser;
import com.affymetrix.igb.util.GraphSymUtils;
import java.text.NumberFormat;
import java.text.ParseException;

public class GraphAdjusterView {

  static FileTracker load_dir_tracker = FileTracker.DATA_DIR_TRACKER;

  public static List transformGraphs(List grafs, String trans_name, FloatTransformer transformer) {
    int gcount = grafs.size();
    List newgrafs = new ArrayList(grafs.size());
    for (int i=0; i<gcount; i++) {
      GraphSym graf = (GraphSym)grafs.get(i);

      //float[] old_ycoords = graf.getGraphYCoords();
      float[] new_ycoords;
      
      if (transformer instanceof IdentityTransform && graf instanceof GraphSymFloat) {
        new_ycoords = ((GraphSymFloat) graf).getGraphYCoords();
      } else {
        int pcount = graf.getPointCount();
        new_ycoords = new float[pcount];
        for (int k=0; k<pcount; k++) {
          new_ycoords[k] = transformer.transform(graf.getGraphYCoord(k));
        }
      }
      String newname = trans_name + " (" + graf.getGraphName() + ") ";
      
     // Transforming on this one seq only, not the whole genome
      String newid = trans_name + " (" + graf.getID() + ") ";
      newid = GraphSymUtils.getUniqueGraphID(newid, graf.getGraphSeq());
      GraphSym newgraf;
      if (graf instanceof GraphIntervalSym) {
        newgraf = new GraphIntervalSym(graf.getGraphXCoords(), 
          ((GraphIntervalSym) graf).getGraphWidthCoords(),
          new_ycoords, newid, graf.getGraphSeq());
      } else {
        newgraf = new GraphSymFloat(graf.getGraphXCoords(), 
          new_ycoords, newid, graf.getGraphSeq());
      }
      
      newgraf.setProperty(GraphSym.PROP_GRAPH_STRAND, graf.getProperty(GraphSym.PROP_GRAPH_STRAND));
      
      
      GraphStateI newstate = newgraf.getGraphState();
      newstate.copyProperties(graf.getGraphState());
      newstate.getTierStyle().setHumanName(newname); // this is redundant
      if (! (transformer instanceof IdentityTransform)) {
        // unless this is an identity transform, do not copy the min-max range
        newstate.setVisibleMinY(Float.NEGATIVE_INFINITY);
        newstate.setVisibleMaxY(Float.POSITIVE_INFINITY);
      }

      ((MutableAnnotatedBioSeq)newgraf.getGraphSeq()).addAnnotation(newgraf);
      newgrafs.add(newgraf);
    }
    return newgrafs;
  }

  public static void deleteGraphs(GenometryModel gmodel, SeqMapView gviewer, List grafs) {
    int gcount = grafs.size();
    for (int i=0; i<gcount; i++) {
      GraphSym graf = (GraphSym)grafs.get(i);
      deleteGraph(gmodel, gviewer, graf);
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
  public static void deleteGraph(GenometryModel gmodel, SeqMapView gviewer, GraphSym gsym) {
    //System.out.println("deleting graph: " + gsym);

    AnnotatedBioSeq aseq = (AnnotatedBioSeq)gsym.getGraphSeq();
    if (aseq instanceof MutableAnnotatedBioSeq) {
      MutableAnnotatedBioSeq mut = (MutableAnnotatedBioSeq) aseq;
      mut.removeAnnotation(gsym);
    }

    /*
    if (Application.ALLOW_DELETING_DATA && aseq instanceof SmartAnnotBioSeq) {
      String method = SmartAnnotBioSeq.determineMethod(gsym);
      gmodel.getSelectedSeqGroup().removeType(method);
    }*/

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


  public static void saveGraphs(SeqMapView gviewer, GenometryModel gmodel, List grafs) {
    int gcount = grafs.size();
    if (gcount > 1) {
      // actually shouldn't get here, since save button is disabled if more than one graph
      ErrorHandler.errorPanel("Can only save one graph at a time");
    }
    else if (gcount == 1) {
      GraphSym gsym = (GraphSym)grafs.get(0);
      try {
        GraphSaverFileChooser chooser = new GraphSaverFileChooser(gsym);
        chooser.setCurrentDirectory(load_dir_tracker.getFile());
        int option = chooser.showSaveDialog(gviewer.getFrame());
        if (option == JFileChooser.APPROVE_OPTION) {          
          load_dir_tracker.setFile(chooser.getCurrentDirectory());
          File fil = chooser.getSelectedFile();
          GraphSymUtils.writeGraphFile(gsym, gmodel.getSelectedSeqGroup(), fil.getAbsolutePath());
        }
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel("Error saving graph", ex);
      }
    }
  }

  public static void changeColor(List graf_syms, SeqMapView gviewer) {
    int scount = graf_syms.size();
    if (scount > 0) {
      // Set an initial color so that the "reset" button will work.
      GraphSym graf_0 = (GraphSym) graf_syms.get(0);
      GraphGlyph gl_0 = (GraphGlyph) gviewer.getSeqMap().getItem(graf_0);
      Color initial_color = Color.GREEN;
      if (gl_0 != null) {
        // gl_0 could be null if there is a selected graph that isn't visible in
        // the current view.
        initial_color = gl_0.getColor();
      }
      Color col = JColorChooser.showDialog((Component) gviewer.getSeqMap(),
        "Graph Color Chooser", initial_color);
      // Note: If the user selects "Cancel", col will be null
      if (col != null) {
	for (int i=0; i<scount; i++) {
	  GraphSym graf = (GraphSym) graf_syms.get(i);
	  // using getItems() instead of getItem(), in case graph sym is represented by multiple graph glyphs
	  List glist = gviewer.getSeqMap().getItems(graf);
	  if (glist != null) {
	    int glyphcount = glist.size();
	    for (int k=0; k<glyphcount; k++) {
	      GraphGlyph gl = (GraphGlyph)glist.get(k);
	      gl.setColor(col); // this automatically sets the GraphState color
	      // if graph is in a tier, change foreground color of tier also
	      //   (which in turn triggers change in color for TierLabelGlyph...)
	      if (gl.getParent() instanceof TierGlyph) {
		gl.getParent().setForegroundColor(col);
                List views = gl.getParent().getScene().getViews();
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

  public static NumberFormat numberParser = NumberFormat.getNumberInstance();

  /** Parse a String floating-point number that may optionally end with a "%" symbol. */
  public static float parsePercent(String text) throws ParseException {
    if (text.endsWith("%")) {
      text = text.substring(0, text.length()-1);
    }

    return numberParser.parse(text).floatValue();
  }
}
