/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package demo.genoviz;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;

import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.glyph.*;

/**
 * Demo of "shadowing" a NeoSeq on a NeoMap
 * via a Shadow object.
 */
public class ShadowDemo2 extends Applet
{

  protected NeoMap  mapview;
  protected NeoSeqI seqview;
  protected GlyphI  shadowRect;

  protected Frame mapframe;

  protected Vector annotations;
  protected Label sizeL, spacingL, stripeL, firstL, lastL;
  protected TextField sizeT, spacingT, stripeT;
  protected Button visibleB, selectedB, removeAnnotB,
    addTextAnnotB, addBackAnnotB;

  protected Label orientL;
  protected Choice orientC;

  protected Label fontL;
  protected Choice fontC;

  protected String seq = "AAACGTGGGGGGGGGGGGGGGAAAAAAAAAAAAATTTTTTTTTTTTTTTTTTTTTTTTTT";
  protected String seq2 = "GATTACAGATTACAGATTACATTTAAAGGGCCCTACTACTACTTTCAGG";
  protected String seq3 = "GAATTCTATAACCCCCGGGGTT";
  protected String newseq;
  protected boolean num_left = true;

  protected Color text_annot_color = Color.blue;
  protected Color back_annot_color = Color.green;

  // "Selection" by the mouse of the shadow

  private boolean shadowActive = false;
  private float mouseLastLoc;


  public String getAppletInfo() {
    return ("Demonstration of Widget Shadowing");
  }

 public void init() {
    StringBuffer seqbuf;

    annotations = new Vector();

    seqview = new NeoSeq();

    // Fill the seqview with a bunch of strings

    seqbuf = new StringBuffer(seq);

    for (int i=0; i<5; i++) {
      seqbuf.append(seq);
    }
    seqbuf.append(seq2);
    seqbuf.append(seq);
    seqbuf.append(seq3);
    seqbuf.append("GAATTC");
    seq = seqbuf.toString();
    for (int i=0; i<40; i++) {
      seqbuf.append(seq);
    }
    newseq = seqbuf.toString();

    seqview.setResidues(newseq);

    // Set up the map view and its shadow

    mapview    = setUpMap(newseq);
    Shadow s = new Shadow( mapview, com.affymetrix.genoviz.util.NeoConstants.HORIZONTAL );
    s.setResidueRange( true );
    seqview.addRangeListener( s );

    // Set the color scheme.

    seqview.setBackground(seqview.RESIDUES, Color.white);
    seqview.setBackground(seqview.NUMBERS, Color.white);
    seqview.setResidueColor(Color.black);

    Color[] okayColors = {
      Color.lightGray,
      null // This will make the first stripe transparent.
    };
    seqview.setStripeColors(okayColors);

    Panel controlP = new Panel();

    this.setLayout(new BorderLayout());

    // hardwired sizing of seqview...
    ((Component)seqview).setSize(600, 250);

    Panel seq_pan = new NeoPanel();
    seq_pan.setLayout(new BorderLayout());
    seq_pan.add("Center", (Component)seqview);
    add("Center", seq_pan);

    Container parent;
    parent = this.getParent();
    while ( null != parent && ! ( parent instanceof Frame ) ) {
      parent = parent.getParent();
    }

 }

  public void start() {
    Container parent;
    parent = this.getParent();
    while ( null != parent && ! ( parent instanceof Frame ) ) {
      parent = parent.getParent();
    }
    super.start();
  }

  public void stop() {
    Container parent;
    parent = this.getParent();
    while ( null != parent && ! ( parent instanceof Frame ) ) {
      parent = parent.getParent();
    }
    super.stop();
  }


  public void destroy() {
    if ( this.mapframe != null )  {
      this.mapframe.setVisible( false );
      this.mapframe.dispose();
      this.mapframe = null;
    }
    super.destroy();
  }


  private void showEnds(String theSequence) {
    String message;
    if (20 < theSequence.length()) {
      message = theSequence.substring(0, 20) + " ... "
        + theSequence.substring(theSequence.length()-20);
    }
    else {
      message = theSequence;
    }
    try {
      showStatus(message);
    }
    catch (Exception e) {
      System.out.println(message);
    }
  }

  public void addBackgroundAnnotation() {
    Object annot = seqview.addAnnotation(seqview.getSelectedStart(),
        seqview.getSelectedEnd(),
        back_annot_color);
    annotations.addElement(annot);
    seqview.deselect();
    seqview.updateWidget();
  }

  private void addTextAnnotation() {
    Object annot =
      seqview.addTextColorAnnotation(seqview.getSelectedStart(),
          seqview.getSelectedEnd(),
          text_annot_color);
    annotations.addElement(annot);
    seqview.deselect();
    seqview.updateWidget();
  }

  private void removeAnnotation() {
    if (annotations.size() > 0) {
      GlyphI annot = (GlyphI)annotations.lastElement();
      seqview.removeAnnotation(annot);
      annotations.removeElement(annot);
      seqview.updateWidget();
    }
  }

  // sets up a map of size length

  public NeoMap setUpMap(String seqstring) {
    mapview = new NeoMap(true, false); // turn off internal vertical scroller

    // Make the mapview listen for the mouse events

    mapview.setRubberBandBehavior (false);


    int seqlength = seqstring.length();

    // optimize map for fast redraw of transient shadow glyph

    mapview.setTransientOptimized(true);

    mapview.setMapRange(0, seqlength);
    mapview.setMapOffset(-100, 100);
    mapview.addAxis(0);

    mapview.configure(
        "-offset -45 -glyphtype com.affymetrix.genoviz.glyph.SequenceGlyph -width 16");

    // Add a SequenceGlyph to the map

    SequenceGlyph sg =
      (SequenceGlyph) mapview.addItem (0, seqlength);
    sg.setResidues(seqstring);

    // Build up the UI around the mapview

    mapframe = new Frame();
    mapframe.setLayout(new BorderLayout());

    mapview.setSize(400, 200);
    mapview.setBackground(new Color(180, 250, 250));

    NeoScrollbar hzoom = new NeoScrollbar(NeoScrollbar.HORIZONTAL);
    mapview.setZoomer(mapview.X, hzoom);

    NeoPanel map_pan = new NeoPanel();
    map_pan.setLayout(new BorderLayout());
    map_pan.add("Center", mapview);
    mapframe.add("Center", map_pan);

    NeoPanel zoom_pan = new NeoPanel();
    zoom_pan.setLayout(new BorderLayout());
    zoom_pan.add("Center", hzoom);

    mapframe.add("North", zoom_pan);
    mapframe.pack();
    mapframe.show();
    mapframe.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        ((Window)e.getSource()).setVisible(false);
      }
    } );

    return mapview;
  }

}
