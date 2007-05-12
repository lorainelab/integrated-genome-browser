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
import javax.swing.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.igb.das.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.event.*;
import com.affymetrix.swing.threads.SwingWorker;

public class DasLoadView extends JComponent
  implements ItemListener, SeqSelectionListener, GroupSelectionListener, DataRequestListener {

  SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  JComboBox das_serverCB;
  JComboBox das_sourceCB;
  JComboBox das_entryCB;
  JPanel types_panel;
  Map das_servers;
  DasServerInfo current_server;
  DasSource current_source;
  DasEntryPoint current_entry;
  DasLoadView myself = null;

  String server_filler = "Choose a DAS server";
  String source_filler = "Choose a DAS source";
  String entry_filler = "Choose a DAS seq";

  /*
   *  choices for DAS annot loading range:
   *    whole genome
   *    whole chromosome
   *    specified range on current chromosome
   *    gviewer's view bounds on current chromosome
   */

  /**
   *  A Panel for Loading from a DAS/1 server.
   *  This panel can be included as a tab in the DataLoadView.
   *  It should eventually replace the DasFeaturesAction2 pop-up window.
   */
  public DasLoadView() {
    myself = this;
    das_serverCB = new JComboBox();
    das_sourceCB = new JComboBox();
    das_entryCB = new JComboBox();
    types_panel = new JPanel();
    types_panel.setLayout(new BoxLayout(types_panel, BoxLayout.Y_AXIS));
    JScrollPane scrollpane = new JScrollPane(types_panel);

    das_serverCB.addItem(server_filler);

    das_servers = DasDiscovery.getDasServers();
    Iterator iter = das_servers.keySet().iterator();
    while (iter.hasNext()) {
      String server_name = (String)iter.next();
      das_serverCB.addItem(server_name);
    }

    this.setLayout(new BorderLayout());
    JPanel panA = new JPanel(new GridLayout(6,1));

    //    JButton test_button = new JButton("Test Button");
    //    this.add("North", test_button);
    panA.add(new JLabel("DAS Server: "));
    panA.add(das_serverCB);
    panA.add(new JLabel("DAS Source: " ));
    panA.add(das_sourceCB);
    panA.add(new JLabel("DAS Entry Point: "));
    panA.add(das_entryCB);
    
    // putting panA inside panelB rather than directly in the main panel
    // prevents the combo boxes from being stretched vertically
    JPanel panelB = new JPanel(new BorderLayout());
    panelB.add("North", panA);

    this.add("West", panelB);
    this.add("Center", scrollpane);

    //    das_serverCB.addActionListener(this);
    das_serverCB.addItemListener(this);
    das_sourceCB.addItemListener(this);
    das_entryCB.addItemListener(this);

    gmodel.addSeqSelectionListener(this);
    gmodel.addGroupSelectionListener(this);
  }

  public void itemStateChanged(ItemEvent evt) {
    //    System.out.println("DasLoadView received ItemEvent: " + evt);
    Object src = evt.getSource();

    // selection of DAS server
    if ((src == das_serverCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("DasLoadView received SELECTED ItemEvent on server combobox");
      String server_name = (String)evt.getItem();
      if (server_name != server_filler) {
	System.out.println("DAS server selected: " + server_name);
	current_server = (DasServerInfo)das_servers.get(server_name);
	System.out.println(current_server);
	setSources();
      }
    }

    // selection of DAS source
    else if ((src == das_sourceCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("DasLoadView received SELECTED ItemEvent on source combobox");
      String source_name = (String)evt.getItem();
      if (source_name != source_filler) {
	System.out.println("source name: " + source_name);
	Map sources = current_server.getDataSources();
	current_source = (DasSource)sources.get(source_name);
	System.out.println(current_source);
	System.out.println("  genome id: " + current_source.getGenome().getID());
	//	setEntryPoints();
	//	setTypes();
	setEntriesAndTypes();
      }
    }

    // selection of DAS entry point
    else if ((src == das_entryCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("DasLoadView received SELECTED ItemEvent on entry combobox");
      String entry_name = (String)evt.getItem();
      if (entry_name != entry_filler) {
	System.out.println("entry seq: " + entry_name);
      }
    }
  }

  public void setSources() {
    das_sourceCB.removeItemListener(this);
    das_sourceCB.removeAllItems();
    das_entryCB.removeAllItems();
    types_panel.removeAll();
    types_panel.validate();
    types_panel.repaint();

    //    types_panel.setEnabled(false);
    das_serverCB.setEnabled(false);
    das_sourceCB.setEnabled(false);
    das_entryCB.setEnabled(false);

    final SwingWorker worker = new SwingWorker() {
	Map sources = null;
	public Object construct() {
	  sources = current_server.getDataSources();
	  return null;  // or could have it return types, shouldn't matter...
	}
	public void finished() {
	  das_sourceCB.addItem(source_filler);
	  Iterator iter = sources.values().iterator();
	  while (iter.hasNext()) {
	    DasSource source = (DasSource)iter.next();
	    //      das_sourceCB.addItem(source.getName() + "(" + source.getID() + ")");
	    das_sourceCB.addItem(source.getID());
	  }

	  //	  das_sourceCB.addItemListener(listener);
	  das_sourceCB.addItemListener(myself);
	  das_serverCB.setEnabled(true);
	  das_sourceCB.setEnabled(true);
	  das_entryCB.setEnabled(true);

	}
      };
    worker.start();
  }


  public void setEntriesAndTypes() {
    types_panel.setEnabled(false);
    das_serverCB.setEnabled(false);
    das_sourceCB.setEnabled(false);
    das_entryCB.setEnabled(false);

    das_entryCB.removeAllItems();
    types_panel.removeAll();
    types_panel.validate();
    types_panel.repaint();

    // alternative would be to use a QueuedExecutor (from ~.concurrent package)
    //    and two runnables, one for entries and one for types...
    Runnable runner = new Runnable() {
	public void run() {
	  final Map seqs = current_source.getEntryPoints();

	  SwingUtilities.invokeLater(new Runnable() {
	      public void run() {
		das_entryCB.addItem(entry_filler);
		Iterator iter = seqs.values().iterator();
		while (iter.hasNext()) {
		  DasEntryPoint entry = (DasEntryPoint)iter.next();
		  das_entryCB.addItem(entry.getID());
		}
		//		das_entryCB.addItemListener(myself);
		das_entryCB.setEnabled(true);
	      }
	    } );

	  final Map types = current_source.getTypes();
	  SwingUtilities.invokeLater(new Runnable() {
	      public void run()  {
		types_panel.removeAll();
		Iterator iter = types.values().iterator();
		while (iter.hasNext()) {
		  DasType dtype = (DasType)iter.next();
		  String typeid = dtype.getID();
		  JCheckBox box = new JCheckBox(typeid);
		  types_panel.add(box);
		}
		types_panel.validate();
		types_panel.repaint();
		//		this.setEnabled(true);
		//		setEnabled(true);
		types_panel.setEnabled(true);
		das_serverCB.setEnabled(true);
		das_sourceCB.setEnabled(true);
	      }
	    } );
	}
      };
    Thread runthread = new Thread(runner);
    runthread.start();
  }

  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (IGB.DEBUG_EVENTS)  {
      System.out.println("DasLoadView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    AnnotatedBioSeq newseq = evt.getSelectedSeq();
  }

  public void groupSelectionChanged(GroupSelectionEvent evt) {
    if (IGB.DEBUG_EVENTS)  { System.out.println("DasLoadView received GroupSelectionEvent: " + evt); }
    AnnotatedSeqGroup group = evt.getSelectedGroup();
  }

  public boolean dataRequested(DataRequestEvent evt) {
    return false;
  }

  public static void main(String[] args) {
    DasLoadView testview = new DasLoadView();
    JFrame frm = new JFrame();
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", testview);
    //    frm.setSize(new Dimension(600, 400));
    frm.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) { System.exit(0);}
    });
    frm.pack();
    frm.show();
  }

  
}
