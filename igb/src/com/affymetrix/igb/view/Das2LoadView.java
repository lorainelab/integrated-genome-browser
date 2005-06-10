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
import javax.swing.*;

import com.affymetrix.igb.IGB;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.igb.das2.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.event.*;
import com.affymetrix.swing.threads.SwingWorker;

import com.affymetrix.igb.util.GenometryViewer;  // temporary visualization till hooked into IGB

public class Das2LoadView extends JComponent
  implements ItemListener, ActionListener, SeqSelectionListener, GroupSelectionListener  {

  SingletonGenometryModel gmodel = IGB.getGenometryModel();
  SeqMapView gviewer = null;
  JComboBox das_serverCB;
  JComboBox das_sourceCB;
  JComboBox das_versionCB;
  JComboBox das_regionCB;
  JTextField minTF;
  JTextField maxTF;
  JButton load_featuresB;
  JPanel types_panel;
  Map das_servers;
  LinkedHashMap checkbox2type;

  Das2LoadView myself = null;
  Das2ServerInfo current_server;
  Das2Source current_source;
  Das2VersionedSource current_version;
  Das2Region current_region;

  String server_filler = "Choose a DAS2 server";
  String source_filler = "Choose a DAS2 source";
  String version_filler = "Choose a DAS2 version";
  String region_filler = "Choose a DAS2 seq";

  /**
   *  choices for DAS2 annot loading range:
   *    whole genome
   *    whole chromosome
   *    specified range on current chromosome
   *    gviewer's view bounds on current chromosome
   */
  public Das2LoadView() {
    myself = this;
    gviewer = IGB.getSingletonIGB().getMapView();

    das_serverCB = new JComboBox();
    das_sourceCB = new JComboBox();
    das_versionCB = new JComboBox();
    das_regionCB = new JComboBox();
    minTF = new JTextField(10);
    maxTF = new JTextField(10);
    load_featuresB = new JButton("Load Features");
    types_panel = new JPanel();
    types_panel.setLayout(new BoxLayout(types_panel, BoxLayout.Y_AXIS));
    JScrollPane scrollpane = new JScrollPane(types_panel);

    checkbox2type = new LinkedHashMap();
    das_serverCB.addItem(server_filler);

    das_servers = Das2Discovery.getDas2Servers();
    Iterator iter = das_servers.keySet().iterator();
    while (iter.hasNext()) {
      String server_name = (String)iter.next();
      das_serverCB.addItem(server_name);
    }

    this.setLayout(new BorderLayout());
    JPanel panA = new JPanel();
    panA.setLayout(new GridLayout(6, 2));

    panA.add(new JLabel("DAS2 Server: "));
    panA.add(das_serverCB);
    panA.add(new JLabel("DAS2 Source: " ));
    panA.add(das_sourceCB);
    panA.add(new JLabel("DAS2 Version: "));
    panA.add(das_versionCB);
    panA.add(new JLabel("DAS2 Region: "));
    panA.add(das_regionCB);
    panA.add(new JLabel("min base: "));
    panA.add(minTF);
    panA.add(new JLabel("max base: "));
    panA.add(maxTF);

    this.add("North", panA);
    this.add("Center", scrollpane);
    this.add("South", load_featuresB);

    das_serverCB.addItemListener(this);
    das_sourceCB.addItemListener(this);
    das_versionCB.addItemListener(this);
    das_regionCB.addItemListener(this);
    load_featuresB.addActionListener(this);

    gmodel.addSeqSelectionListener(this);
    gmodel.addGroupSelectionListener(this);
  }

  public void itemStateChanged(ItemEvent evt) {
    //    System.out.println("Das2LoadView received ItemEvent: " + evt);
    Object src = evt.getSource();

    // selection of DAS server
    if ((src == das_serverCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("Das2LoadView received SELECTED ItemEvent on server combobox");
      String server_name = (String)evt.getItem();
      if (server_name != server_filler) {
	System.out.println("DAS server selected: " + server_name);
	current_server = (Das2ServerInfo)das_servers.get(server_name);
	System.out.println(current_server);
	setSources();
      }
    }

    // selection of DAS source
    else if ((src == das_sourceCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("Das2LoadView received SELECTED ItemEvent on source combobox");
      String source_name = (String)evt.getItem();
      if (source_name != source_filler) {
	System.out.println("source name: " + source_name);
	Map sources = current_server.getSources();
	current_source = (Das2Source)sources.get(source_name);
	System.out.println(current_source);
	//	System.out.println("  genome id: " + current_source.getGenome().getID());
	//	setRegionsAndTypes();
	setVersions();
      }
    }

    else if ((src == das_versionCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("Das2LoadView received SELECTED ItemEvent on version combobox");
      String version_name = (String)evt.getItem();
      if (version_name != version_filler) {
	System.out.println("version name: " + version_name);
	Map versions = current_source.getVersions();
	current_version = (Das2VersionedSource)versions.get(version_name);
	System.out.println(current_version);
	System.out.println("  version id: " + current_version.getGenome().getID());
	setRegionsAndTypes();
      }
    }

    // selection of DAS region point
    else if ((src == das_regionCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
      System.out.println("Das2LoadView received SELECTED ItemEvent on region combobox");
      String region_name = (String)evt.getItem();
      if (region_name != region_filler) {
	System.out.println("region seq: " + region_name);
	Map regions = current_version.getRegions();
	current_region = (Das2Region)regions.get(region_name);
      }
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == load_featuresB) {
      System.out.println("Das2LoadView received ActionEvent on load features button");
      loadFeatures();
    }
  }

  public void setVersions() {
    das_versionCB.removeItemListener(this);
    das_versionCB.removeAllItems();
    das_regionCB.removeAllItems();
    checkbox2type.clear();
    types_panel.removeAll();
    types_panel.validate();
    types_panel.repaint();

    das_serverCB.setEnabled(false);
    das_sourceCB.setEnabled(false);
    das_versionCB.setEnabled(false);
    das_regionCB.setEnabled(false);

    final SwingWorker worker = new SwingWorker() {
	Map versions = null;
	public Object construct() {
	  versions = current_source.getVersions();
	  return null;  // or could have it return types, shouldn't matter...
	}
	public void finished() {
	  das_versionCB.addItem(version_filler);
	  Iterator iter = versions.values().iterator();
	  while (iter.hasNext()) {
	    Das2VersionedSource version = (Das2VersionedSource)iter.next();
	    das_versionCB.addItem(version.getID());
	  }

	  das_versionCB.addItemListener(myself);
	  das_serverCB.setEnabled(true);
	  das_sourceCB.setEnabled(true);
	  das_versionCB.setEnabled(true);
	  das_regionCB.setEnabled(true);

	}
      };
    worker.start();

  }

  public void setSources() {
    das_sourceCB.removeItemListener(this);
    das_sourceCB.removeAllItems();
    das_regionCB.removeAllItems();
    checkbox2type.clear();
    types_panel.removeAll();
    types_panel.validate();
    types_panel.repaint();

    //    types_panel.setEnabled(false);
    das_serverCB.setEnabled(false);
    das_sourceCB.setEnabled(false);
    das_versionCB.setEnabled(false);
    das_regionCB.setEnabled(false);

    final SwingWorker worker = new SwingWorker() {
	Map sources = null;
	public Object construct() {
	  sources = current_server.getSources();
	  return null;  // or could have it return types, shouldn't matter...
	}
	public void finished() {
	  das_sourceCB.addItem(source_filler);
	  Iterator iter = sources.values().iterator();
	  while (iter.hasNext()) {
	    Das2Source source = (Das2Source)iter.next();
	    //      das_sourceCB.addItem(source.getName() + "(" + source.getID() + ")");
	    das_sourceCB.addItem(source.getID());
	  }

	  //	  das_sourceCB.addItemListener(listener);
	  das_sourceCB.addItemListener(myself);
	  das_serverCB.setEnabled(true);
	  das_sourceCB.setEnabled(true);
	  das_versionCB.setEnabled(true);
	  das_regionCB.setEnabled(true);

	}
      };
    worker.start();
  }


  public void setRegionsAndTypes() {
    types_panel.setEnabled(false);
    das_serverCB.setEnabled(false);
    das_sourceCB.setEnabled(false);
    das_versionCB.setEnabled(false);
    das_regionCB.setEnabled(false);
    load_featuresB.setEnabled(false);

    das_regionCB.removeAllItems();
    checkbox2type.clear();
    types_panel.removeAll();
    types_panel.validate();
    types_panel.repaint();

    // alternative would be to use a QueuedExecutor (from ~.concurrent package)
    //    and two runnables, one for entries and one for types...
    Runnable runner = new Runnable() {
	public void run() {
	  final Map seqs = current_version.getRegions();

	  SwingUtilities.invokeLater(new Runnable() {
	      public void run() {
		das_regionCB.addItem(region_filler);
		Iterator iter = seqs.values().iterator();
		while (iter.hasNext()) {
		  Das2Region region = (Das2Region)iter.next();
		  das_regionCB.addItem(region.getID());
		}
		//		das_regionCB.addItemListener(myself);
		das_regionCB.setEnabled(true);
	      }
	    } );

	  final Map types = current_version.getTypes();
	  SwingUtilities.invokeLater(new Runnable() {
	      public void run()  {
		// types_panel.removeAll();  // already done at start of setRegionsAndTypes();
		// checkbox2type.clear(); // already done at start of setRegionsAndTypes();
		Iterator iter = types.values().iterator();
		while (iter.hasNext()) {
		  Das2Type dtype = (Das2Type)iter.next();
		  String typeid = dtype.getID();
		  //		  String typesource = dtype.getDerivation();
		  JCheckBox box = new JCheckBox(typeid);
		  types_panel.add(box);
		  checkbox2type.put(box, dtype);
		}
		types_panel.validate();
		types_panel.repaint();
		//		this.setEnabled(true);
		//		setEnabled(true);
		types_panel.setEnabled(true);
		das_serverCB.setEnabled(true);
		das_sourceCB.setEnabled(true);
		das_versionCB.setEnabled(true);
		load_featuresB.setEnabled(true);
	      }
	    } );
	}
      };
    Thread runthread = new Thread(runner);
    runthread.start();
  }

  public void loadFeatures() {
    MutableAnnotatedBioSeq aseq = current_region.getAnnotatedSeq();
    String minstr = minTF.getText();
    String maxstr = maxTF.getText();
    int min = 0;
    int max = 0;
    try { min = Integer.parseInt(minstr); }
    catch(Exception ex) {
      ex.printStackTrace();
      minTF.setText("");
      return;
    }
    try {
      max = Integer.parseInt(maxstr);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      maxTF.setText("");
      return;
    }

    SeqSpan overlap = new SimpleSeqSpan(min, max, aseq);
    System.out.println("region = " + current_region.getID() + ", seq = " + aseq.getID() +
		       ", min = " + min + ", max = " + max);
    int selected_type_count = 0;
    Iterator iter = checkbox2type.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry keyval = (Map.Entry)iter.next();
      JCheckBox box = (JCheckBox)keyval.getKey();
      Das2Type dtype = (Das2Type)keyval.getValue();
      boolean selected = box.isSelected();
      if (selected) {
	selected_type_count++;
	String typeid = dtype.getID();
	System.out.println("selected type: " + typeid);
	Das2FeatureRequestSym request_sym =
	  new Das2FeatureRequestSym(dtype, current_region, overlap, null);
	current_region.getFeatures(request_sym);
	aseq.addAnnotation(request_sym);
      }
    }
    if (selected_type_count > 0) {
      if (gviewer != null) {
	gviewer.setAnnotatedSeq(aseq, true, true);
      }
    }
  }


  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (IGB.DEBUG_EVENTS)  {
      System.out.println("Das2LoadView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    AnnotatedBioSeq newseq = evt.getSelectedSeq();
  }

  public void groupSelectionChanged(GroupSelectionEvent evt) {
    if (IGB.DEBUG_EVENTS)  { 
      System.out.println("Das2LoadView received GroupSelectionEvent: " + evt); 
    }
    java.util.List groups = evt.getSelectedGroups();
    if (groups != null && groups.size() > 0) {
      AnnotatedSeqGroup group = (AnnotatedSeqGroup)groups.get(0);
    }
  }

  /*
  public static void main(String[] args) {
    Das2LoadView testview = new Das2LoadView();
    JFrame frm = new JFrame();
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", testview);
    frm.setSize(new Dimension(400, 400));
    frm.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) { System.exit(0);}
    });
    //    frm.pack();
    frm.show();
  }
  */
}
