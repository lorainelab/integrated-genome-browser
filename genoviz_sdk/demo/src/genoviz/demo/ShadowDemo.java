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

package genoviz.demo;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.glyph.*;
import java.awt.geom.Rectangle2D;
import javax.swing.JScrollBar;

/**
 *  Demo of "shadowing" a NeoSeq on a NeoMap via
 *  RangeChangeEvents and TransientGlyphs
 *
 * @version $Id$
 */
public class ShadowDemo extends Applet
	implements MouseMotionListener, MouseListener,
			   NeoRangeListener
{

	protected NeoMap  mapview;
	protected NeoSeq seqview;
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
	boolean mapframe_active = true;

	protected Color text_annot_color = Color.blue;
	protected Color back_annot_color = Color.green;

	// "Selection" by the mouse of the shadow

	private boolean shadowActive = false;

	final int SEQVIEW_WIDTH = 300;
	final int SEQVIEW_HEIGHT = 150;
	final int MAP_WIDTH = 400;
	final int MAP_HEIGHT = 200;

	protected double mouse_offset;

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

		//System.out.println("Newseq length: " + newseq.length());

		seqview.setResidues(newseq);

		// Set up the map view and its shadow

		mapview    = setUpMap(newseq);
		shadowRect = setUpShadowRect(seqview, mapview);

		// listening for visible range of both NeoSeq and NeoMap changing,
		// so can track accordingly.

		seqview.addRangeListener(this);

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
		((Component)seqview).setSize(SEQVIEW_WIDTH, SEQVIEW_HEIGHT);

		Panel seq_pan = new NeoPanel();
		seq_pan.setLayout(new BorderLayout());
		seq_pan.add("Center", (Component)seqview);
		add("Center", seq_pan);

		/*   testing NeoSeq.preferredSize() methods...
			 seqview.setPreferredSize(50, 15);
			 System.out.println(seqview.getPreferredSize());
			 seqview.setSize(getPreferredSize());;
			 Panel extraPanel = new Panel();
			 extraPanel.add(seqview);
			 add("Center", extraPanel);
			 */

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
		if ( mapframe != null ) mapframe.setVisible(true);
		mapframe_active = true;
		mapview.updateWidget();
	}

	public void stop() {
		Container parent;
		parent = this.getParent();
		while ( null != parent && ! ( parent instanceof Frame ) ) {
			parent = parent.getParent();
		}
		super.stop();
		mapframe.setVisible(false);
	}


	public void destroy() {
		if ( this.mapframe != null )  {
			this.mapframe.setVisible( false );
			this.mapframe.dispose();
			this.mapframe = null;
		}
		super.destroy();
	}

	/*
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
	   }*/

	public void addBackgroundAnnotation() {
		Object annot = seqview.addAnnotation(seqview.getSelectedStart(),
				seqview.getSelectedEnd(),
				back_annot_color);
		annotations.addElement(annot);
		seqview.deselect();
		seqview.updateWidget();
	}


	// sets up a map of size length

	public NeoMap setUpMap(String seqstring) {
		mapview = new NeoMap(true, false); // turn off internal vertical scroller

		// Make the mapview listen for the mouse events

		mapview.addMouseListener (this);
		mapview.addMouseMotionListener (this);
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
			(SequenceGlyph) mapview.addItem (0, seqlength); sg.setResidues(seqstring);
		sg.setBackgroundColor (Color.lightGray);

		// Build up the UI around the mapview

		mapframe = new Frame("NeoMap - Shadowing Demo");
		mapframe.setLayout(new BorderLayout());
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		mapframe.setLocation((screen_size.width-MAP_WIDTH)/4,
				(screen_size.height-MAP_HEIGHT)/3);

		mapview.setSize(MAP_WIDTH, MAP_HEIGHT);
		mapview.setBackground(new Color(180, 250, 250));

		JScrollBar hzoom = new JScrollBar(JScrollBar.VERTICAL);
		mapview.setZoomer(mapview.X, hzoom);

		NeoPanel map_pan = new NeoPanel();
		map_pan.setLayout(new BorderLayout());
		map_pan.add("Center", mapview);
		mapframe.add("Center", map_pan);

		NeoPanel zoom_pan = new NeoPanel();
		zoom_pan.setLayout(new BorderLayout());
		zoom_pan.add("Center", hzoom);

		mapframe.add("West", zoom_pan);
		mapframe.pack();
		mapframe.show();
		mapframe.toFront();
		mapframe.addWindowListener(
				new WindowAdapter() {
					public void windowClosing(WindowEvent we) {
						((Window)we.getSource()).setVisible(false);
						mapframe_active = false;
					}
					public void windowDeactivated(WindowEvent we) {
						if ( mapframe_active && mapframe != null) mapframe.toFront();
					}
				});
		return mapview;
	}

	/**
	 *  Sets up a shadow of the source on the destination
	 */

	public GlyphI setUpShadowRect (NeoSeq source, NeoMap destination) {

		TransientGlyph tg      = new TransientGlyph();
		GlyphI         rglyph  = new OutlineRectGlyph();

		// currently need to make TransientGlyph the full size of the map!

		Rectangle2D.Double sbox = destination.getScene().getCoordBox();
		tg.setCoords(sbox.x, -20, sbox.width, 40);
		rglyph.setCoords(0, -20, 1, 40);
		rglyph.setColor(Color.red);
		rglyph.setSelectable(true);
		destination.getScene().addGlyph(tg);

		// add rglyph as a child of transient glyph

		destination.addItem(tg, rglyph);
		mapview.updateWidget();
		return rglyph;
	}

	/* EVENT HANDLING */


	/** MouseMotionListener Implementation */

	public void mouseDragged(MouseEvent e) {

		// Make sure this is a NeoMouseEvent at work

		if (!(e instanceof NeoMouseEvent) || (e.getSource() != mapview))
			return;

		if (!shadowActive)
			return;

		// Try to move the shadowRect relatively, in the X axis only.

		NeoMouseEvent nmEvent = (NeoMouseEvent) e;

		double mouseCurrentLoc = nmEvent.getCoordX();

		// Insure that the user doesn't try to drag the shadow outside
		// the bounds

		if ((((shadowRect.getCoordBox()).x + mouse_offset) < 0.0) ||
				(((shadowRect.getCoordBox()).x + mouse_offset) > (mapview.getCoordBounds()).width))
			return;

		// The above was not sufficient -- additional bounds check
		double newShadowLoc = mouseCurrentLoc - mouse_offset;
		if (newShadowLoc < 0.0 || newShadowLoc > (mapview.getCoordBounds()).width)
			return;


		shadowRect.getCoordBox().x = newShadowLoc;
		mapview.setZoomBehavior ( NeoWidgetI.X, NeoWidgetI.CONSTRAIN_COORD,
				mouseCurrentLoc - mouse_offset );
		// Update the new "base" of the mouse location

		mapview.updateWidget();

		// Tell the ShadowDemo of the new
		this.shadowMoved (new NeoRangeEvent (shadowRect,
					shadowRect.getCoordBox().x,
					shadowRect.getCoordBox().x +
					shadowRect.getCoordBox().width));
	}


	public void mouseMoved(MouseEvent e) {}

	/** MouseListener Implementation */

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e)  {}

	public void mousePressed(MouseEvent e) {

		// Make sure this is a NeoMouseEvent at work

		if (!(e instanceof NeoMouseEvent) ||
				(e.getSource() != mapview))
			return;

		NeoMouseEvent nmEvent    = (NeoMouseEvent) e;
		Vector glyphsIntersected = nmEvent.getItems();

		if (glyphsIntersected.contains (shadowRect)) {
			shadowActive = true;
			mouse_offset = nmEvent.getCoordX() - shadowRect.getCoordBox().x;
		}
	}

	public void mouseReleased(MouseEvent e) {

		// Make sure this is a NeoMouseEvent at work

		if (!(e instanceof NeoMouseEvent) ||
				(e.getSource() != mapview))
			return;
		shadowActive = false;
	}



	/** RangeListener Implementation */

	public void rangeChanged(NeoRangeEvent evt) {
		int sbeg   = (int) evt.getVisibleStart();
		int swidth = (int) evt.getVisibleEnd() - sbeg + 1;
		Rectangle2D.Double sbox = mapview.getCoordBounds(shadowRect);
		shadowRect.setCoords(sbeg, sbox.y, swidth, sbox.height);
		mapview.updateWidget();
	}

	/** Tell the ShadowDemo that the shadowRect has moved */

	private void shadowMoved (NeoRangeEvent nre) {

		// Make sure that it's the shadowRect calling this

		if (nre.getSource() != shadowRect)
			return;

		seqview.scrollSequence((int) nre.getVisibleStart());
		seqview.updateWidget();
	}

}
