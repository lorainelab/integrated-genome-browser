/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

package com.affymetrix.igb.menuitem;

// Java
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.event.ThreadProgressMonitor;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.genometry.GraphSym;
import com.affymetrix.igb.util.GraphSymUtils;
import com.affymetrix.igb.util.UniFileFilter;
import com.affymetrix.igb.parsers.Streamer;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.util.LocalUrlCacher;

public class OpenGraphAction extends AbstractAction {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  int graph_count = 0;
  double default_yloc = 10;
  FileTracker load_dir_tracker;
  SeqMapView gviewer;

  public OpenGraphAction(SeqMapView gview, FileTracker ft) {
    this.gviewer = gview;
    this.load_dir_tracker = ft;
  }

  public void actionPerformed(ActionEvent e) {
    // allowing for multiple file selection, so may have multiple graphs
    BioSeq aseq = gmodel.getSelectedSeq();
    if (aseq == null) {
      IGB.errorPanel("Must load a sequence before loading a graph");
      return;
    }

    File[] files = chooseGraphs();

    if (files == null) return;

    try {
      Thread t = loadAndShowGraphs(files, aseq, gviewer);
      t.start();
    } catch (MalformedURLException m) {
      IGB.errorPanel("Error loading graphs", m);
    }
  }

  public static Thread loadAndShowGraphs(File[] files, BioSeq aseq, SeqMapView gviewer)
  throws MalformedURLException {
    URL[] urls = new URL[files.length];
    for (int i=0; i<files.length; i++) {
      urls[i] = files[i].toURL();
    }
    return loadAndShowGraphs(urls, aseq, gviewer);
  }

  public static Thread loadAndShowGraphs(final URL[] files, final BioSeq aseq, final SeqMapView gviewer) {
    Thread t = new Thread() {
      ThreadProgressMonitor monitor = null;
      public void run() {
        monitor = new ThreadProgressMonitor(
          gviewer.getFrame(),
          "Loading graphs...",
          "Loading graphs",
          this,
          false, false);
        monitor.showDialogEventually();
        Vector graphs = null;
        try {
          Map shash = IGB.getGenometryModel().getSelectedSeqGroup().getSeqs();
          graphs = loadGraphFiles(files, shash, aseq, true, monitor, gviewer);
        } catch (final Throwable t) { // catch Out-Of-Memory Errors, etc.
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              if (monitor != null) monitor.closeDialog();
              IGB.errorPanel("Error loading graphs", t);
            }
	    });
        }
        if (monitor != null) {
          monitor.closeDialogEventually();
          monitor = null;
        }
      }
    };
    return t;
  }

  static void updateViewer(final SeqMapView gviewer) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        gviewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
      }
    });
  }

  public File[] chooseGraphs() {
    JFileChooser chooser = getFileChooser();
    chooser.setMultiSelectionEnabled(true);
    chooser.setCurrentDirectory(load_dir_tracker.getFile());
    int option = chooser.showOpenDialog(gviewer.getFrame());
    if (option != JFileChooser.APPROVE_OPTION) {  return null; }
    File[] files = chooser.getSelectedFiles();
    load_dir_tracker.setFile(chooser.getCurrentDirectory());
    return files;
  }

  /** Loads graphs from a set of files.
   *  The return Vector will contain all sucessfully loaded GraphSym objects;
   *  there may be more or fewer graphs than given files.
   *  @param aseq  a BioSeq. If null, this routine will return an empty Vector
   *  @param update_viewer  If true, this will call {@link #updateViewer(SeqMapView)} after
   *   each graph file is loaded.
   *  @throws OutOfMemoryError  Any routine can throw an OutOfMemoryError, but
   *  this is a reminder that this is fairly common here.  (You have to catch
   *  "Throwable" rather than "Exception" to catch these.)
   */
  static Vector loadGraphFiles(URL[] files, Map seqhash, BioSeq aseq, boolean update_viewer, ThreadProgressMonitor monitor, SeqMapView gviewer)
  throws IOException, OutOfMemoryError {
    Vector graphs = new Vector();
    if (aseq != null) {
      for (int i=0; i<files.length; i++) {
        if (monitor != null) {monitor.setMessageEventually("Loading graph from: "+files[i].getPath());}
        Vector v = loadGraphFile(files[i], seqhash, aseq);
        graphs.addAll(v);
        if (update_viewer && ! v.isEmpty()) {updateViewer(gviewer);}
      }
    }
    return graphs;
  }

  static Vector loadGraphFile(URL furl, Map seqhash, BioSeq aseq) throws IOException, OutOfMemoryError {
    Vector graphs = new Vector();
    InputStream fis = null;
    try {
      String name = furl.getPath();
      if (IGB.CACHE_GRAPHS)  {
        String graph_url = furl.toExternalForm();
        System.out.println("in OpenGraphAction.loadGraphFile(), url external form: " + graph_url);
        fis = LocalUrlCacher.getInputStream(graph_url);
      }
      else {
        fis = furl.openStream();
      }

      if (GraphSymUtils.isAGraphFilename(name)) {
        java.util.List multigraphs = GraphSymUtils.readGraphs(fis, furl.toExternalForm(), seqhash);
        graphs.addAll(multigraphs);
      } else {
        throw new IOException("Filename does not match any known type of graph:\n" + name);
      }
    } finally {
      if (fis != null) try { fis.close(); } catch (IOException ioe) {}
    }
    return graphs;
  }

  static JFileChooser chooser = null;

  static JFileChooser getFileChooser() {
    if (chooser == null) {
      chooser = new JFileChooser();
      chooser.setMultiSelectionEnabled(true);
      // set directory later // chooser.setCurrentDirectory(new File((String) System.getProperties().get("user.dir")));
            chooser.addChoosableFileFilter(new UniFileFilter("bar"));
//      chooser.addChoosableFileFilter(new UniFileFilter(new String[] {"bar", "mbar"}));
      chooser.addChoosableFileFilter(new UniFileFilter("gr", "Text Graph"));
//      chooser.addChoosableFileFilter(new UniFileFilter("sbar"));
      chooser.addChoosableFileFilter(new UniFileFilter("bgr"));
      chooser.addChoosableFileFilter(new UniFileFilter("sgr"));
      HashSet all_known_endings = new HashSet();
      javax.swing.filechooser.FileFilter[] filters = chooser.getChoosableFileFilters();
      for (int i=0; i<filters.length; i++) {
        if (filters[i] instanceof UniFileFilter) {
          UniFileFilter uff = (UniFileFilter) filters[i];
          uff.addCompressionEndings(Streamer.compression_endings);
          all_known_endings.addAll(uff.getExtensions());
        }
      }
      UniFileFilter all_known_types = new UniFileFilter(
        (String[]) all_known_endings.toArray(new String[all_known_endings.size()]),
        "Known Graph Types");
      all_known_types.setExtensionListInDescription(false);
      all_known_types.addCompressionEndings(Streamer.compression_endings);
      chooser.addChoosableFileFilter(all_known_types);
      //chooser.setFileFilter(filters[0]); // set to "All Files"
    }
    return chooser;
  }

}



