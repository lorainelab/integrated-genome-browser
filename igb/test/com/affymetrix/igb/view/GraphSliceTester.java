package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.GraphSliceOptimizer;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.affymetrix.genoviz.util.Timer;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometryImpl.CompositeGraphSym;
import com.affymetrix.igb.Application;

public class GraphSliceTester extends JComponent
    implements ActionListener  {

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  SeqMapView gviewer;
  JButton loadB;
  JButton initB;
  String test_file = "c:/data/graph_slice_test/test.bar";
  boolean initialized = false;

  public GraphSliceTester()  {
    gviewer = Application.getSingleton().getMapView();
    loadB = new JButton("Get Graph Slice in View");
    this.setLayout(new FlowLayout());
    this.add(loadB);
    loadB.addActionListener(this);
  }

  public void actionPerformed(ActionEvent evt) {
    Timer tim = new Timer();
    tim.start();
    //    System.out.println("GraphSliceTester received ActionEvent: " + evt);
    SeqSpan viewspan = gviewer.getVisibleSpan();
    boolean any_new_points = false;
    MutableAnnotatedBioSeq visible_seq = (MutableAnnotatedBioSeq)viewspan.getBioSeq();
    //    System.out.println("want to get all graph coords for region: " +
    //		       viewspan.getMin() + " to " + viewspan.getMax() + ", length = " + viewspan.getLength());

    if (! initialized) {
      // need to initialize load of graph,
      // which mainly means constructing CompositeGraphSym and adding as annotation to visible_seq
      CompositeGraphSym gsym = new CompositeGraphSym("composite_test", visible_seq);
      gsym.setGraphName("slice test graph");
      gsym.addSpan(new SimpleSeqSpan(0, visible_seq.getLength(), visible_seq));
      // need to add sym as annotation to visible_seq,

      // need to fix GraphSym/GraphGlyph/etc. so that will still work if no xcoords/ycoords,
      visible_seq.addAnnotation(gsym);
      initialized = true;
    }

    // iterate through top-level annotation on visible_seq, and pass any CompositeGraphSyms to
    int acount = visible_seq.getAnnotationCount();
    int points_added = 0;
    for (int i=0; i<acount; i++) {
      SeqSymmetry sym = visible_seq.getAnnotation(i);
      if (sym instanceof CompositeGraphSym) {
	CompositeGraphSym gsym = (CompositeGraphSym)sym;
	int prev_point_count = gsym.getPointCount();
	boolean new_points = false;
        try {
          new_points = GraphSliceOptimizer.loadSlice(gsym, gmodel, viewspan);
        } catch (IOException ioe) {
          ioe.printStackTrace(System.err);
        }
	any_new_points = (any_new_points || new_points);
	int new_point_count = gsym.getPointCount() - prev_point_count;
	points_added += new_point_count;
      }
    }

    if (any_new_points)  {
      long t1 = tim.read();
      System.out.println("points added via graph slices: " + points_added +
			 ",  time to add: " + (t1/1000f) + " kpoints/second = " + (int)(points_added / t1));
      //      System.out.println("in GraphSliceTester, refreshing SeqMapView");
      gviewer.setAnnotatedSeq(visible_seq, true, true);
    }
  }

}
