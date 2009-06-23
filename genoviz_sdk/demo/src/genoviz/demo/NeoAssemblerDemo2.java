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

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.datamodel.*;
import com.affymetrix.genoviz.widget.*;

import genoviz.demo.adapter.AssemblyAdapter;
import genoviz.demo.datamodel.Assembly;
import genoviz.demo.parser.AlignmentParser;
import genoviz.demo.parser.SequenceParser;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;

/**
 * This is a reworking of the old NeoAssemblerDemo
 *
 * It adds a button to clear and reload the NeoAssembler.
 * This to check a bug reported by Genentech.
 *
 * @version $Id$
 */
public class NeoAssemblerDemo2 extends Applet {

	Assembly assem; // the data model

	NeoAssembler map; // the widget

	public NeoAssemblerDemo2() {

		// setting up buttons to demo layout configurations
		Panel buttonpanel = buttonSetup();

		this.map = new NeoAssembler();

		/**
		 *  Assembler inherits from Panel, and thus all of the methods of
		 *  java.awt.Component, Container, and Panel are available,
		 *  such as setting the background color, and resizing
		 */
		((Component)this.map).setBackground(new Color(180, 250, 250));
		((Component)this.map).setSize( 840, 200);

		// Let's give the labels lots of room.
		this.map.setLabelWidth( 200 );

		// Use the NeoAssembler's built-in selection methods.
		this.map.setSelectionEvent(map.ON_MOUSE_DOWN);
		this.map.setSelectionBehavior(map.SELECT_RESIDUES);

		/**
		  In order for the assembly map to automatically respond to resize events
		  by filling the available space, it is highly recommended a
		  BorderLayout (or similarly flexible layout manager) be used for the
		  Container that holds the BasicMap, and add the BasicMap in the center.
		  */
		this.setLayout(new BorderLayout());

		add("North", buttonpanel);

		NeoPanel widg_pan = new NeoPanel();
		widg_pan.setLayout(new BorderLayout());
		widg_pan.add("Center", (Component)this.map);
		add("Center", widg_pan);

	}


	URL seq_URL = null, align_URL = null;
	public void init() {
		int alignwidth = Integer.parseInt(getParameter("alignwidth"));
		try {
			seq_URL = new URL(this.getDocumentBase(), getParameter("seq_file"));
			align_URL = new URL(this.getDocumentBase(), getParameter("map_file"));
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		assem = loadData(seq_URL, align_URL);

		//---- set range of assembly map ----
		// this is required due to current bug in Assembler
		map.setRange(0, assem.getLength()+1);

		AssemblyAdapter adapter = new AssemblyAdapter(map);
		map.addDataAdapter(adapter);
		map.addData(this.assem);
		map.getSelectedObjects();
		map.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				showStatus( e.getItem().toString() );
			}
		} );
	}


	/**
	 * puts buttons on a panel so the user can instigate actions.
	 */
	public Panel buttonSetup() {
		Panel bp = new Panel();

		Button labelB = new Button("Labels Left");
		bp.add(labelB);
		labelB.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				Button b = (Button) e.getSource();
				int id = map.getPlacement(map.LABELS);
				if (id == map.PLACEMENT_LEFT) {
					map.configureLayout( map.LABELS, map.PLACEMENT_RIGHT );
					b.setLabel( "Labels Right" );
				}
				else if ( id == map.PLACEMENT_RIGHT ) {
					map.configureLayout( map.LABELS, map.PLACEMENT_LEFT );
					b.setLabel( "Labels Left" );
				}
			}
		});

		Button consensusB = new Button("Consensus Top");
		bp.add(consensusB);
		consensusB.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				Button b = (Button) e.getSource();
				int id = map.getPlacement(map.CONSENSUS);
				if (id == map.PLACEMENT_BOTTOM) {
					map.configureLayout(map.CONSENSUS, map.PLACEMENT_TOP);
					b.setLabel("Consensus Top");
				}
				else if (id == map.PLACEMENT_TOP) {
					map.configureLayout(map.CONSENSUS, map.PLACEMENT_BOTTOM);
					b.setLabel("Consensus Bottom");
				}
			}
		} );

		Button axisB = new Button("Axis Scroller Bottom");
		bp.add(axisB);
		axisB.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				Button b = (Button) e.getSource();
				int id = map.getPlacement(map.AXIS_SCROLLER);
				if (id == map.PLACEMENT_BOTTOM) {
					map.configureLayout(map.AXIS_SCROLLER, map.PLACEMENT_TOP);
					b.setLabel("Axis Scroller Top");
				}
				else if (id == map.PLACEMENT_TOP) {
					map.configureLayout(map.AXIS_SCROLLER, map.PLACEMENT_BOTTOM);
					b.setLabel("Axis Scroller Bottom");
				}
			}
		} );

		Button offsetB = new Button("Offset Scroller Left");
		bp.add(offsetB);
		offsetB.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				Button b = (Button) e.getSource();
				int id = map.getPlacement(map.OFFSET_SCROLLER);
				if (id == map.PLACEMENT_LEFT) {
					map.configureLayout(map.OFFSET_SCROLLER, map.PLACEMENT_RIGHT);
					b.setLabel("Offset Scroller Right");
				}
				else if (id == map.PLACEMENT_RIGHT) {
					map.configureLayout(map.OFFSET_SCROLLER, map.PLACEMENT_LEFT);
					b.setLabel("Offset Scroller Left");
				}
			}
		} );

		Button resetB = new Button( "Clear" );
		bp.add( resetB );
		resetB.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				Button b = (Button) e.getSource();
				if ( b.getLabel().equals( "Clear" ) ) {
					map.clearWidget();
					b.setLabel( "Reload" );
				}
				else {
					NeoWidgetI aw = map.getWidget( NeoAssemblerI.ALIGNMENTS );
					NeoWidgetI lw = map.getWidget( NeoAssemblerI.LABELS );
					((NeoMap)aw).setMapOffset( 0, 0 );
					((NeoMap)lw).setMapOffset( 0, 0 );
					assem = loadData(seq_URL, align_URL);
					map.addData(assem);
					map.setRange(0, assem.getLength()+1);
					map.zoom( map.X, map.getMaxZoom( map.X ) );
					map.scroll( map.Y, 1 );
					map.scroll( map.Y, -1 );
					validate();
					b.setLabel( "Clear" );
				}
		map.updateWidget();
			}
		} );

		return bp;
	}

	public Assembly loadData(URL seq_URL, URL align_URL) {
		Vector seqs = null;
		Vector aligns = null;
		Hashtable seqhash = new Hashtable();
		seqs = SequenceParser.getSequences(seq_URL);
		aligns = AlignmentParser.getAlignments(align_URL);
		// This assumes consensus is FIRST in alignment/mapping input
		Mapping consmap = (Mapping)aligns.elementAt(0);
		Assembly model = new Assembly(consmap, aligns, seqs);
		return model;
	}


}
