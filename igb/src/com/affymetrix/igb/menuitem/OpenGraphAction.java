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

package com.affymetrix.igb.menuitem;

import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import com.affymetrix.genometry.*;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.event.ThreadProgressMonitor;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genometryImpl.parsers.Streamer;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.style.IAnnotStyle;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.util.LocalUrlCacher;

public class OpenGraphAction extends AbstractAction {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  int graph_count = 0;
  double default_yloc = 10;
  FileTracker load_dir_tracker;
  SeqMapView gviewer;

  /*
  public OpenGraphAction(SeqMapView gview, FileTracker ft) {
    this.gviewer = gview;
    this.load_dir_tracker = ft;
  }*/

  public void actionPerformed(ActionEvent e) {
    // allowing for multiple file selection, so may have multiple graphs
    BioSeq aseq = gmodel.getSelectedSeq();
    if (aseq == null) {
      ErrorHandler.errorPanel("Must load a sequence before loading a graph");
      return;
    }

    File[] files = chooseGraphs();

    if (files == null) return;

    try {
      Thread t = loadAndShowGraphs(files, aseq, gviewer);
      t.start();
    } catch (MalformedURLException m) {
      ErrorHandler.errorPanel("Error loading graphs", m);
    }
  }

  public static Thread loadAndShowGraphs(File[] files, BioSeq aseq, SeqMapView gviewer)
  throws MalformedURLException {
    URL[] urls = new URL[files.length];
    for (int i=0; i<files.length; i++) {
      urls[i] = files[i].toURI().toURL();
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
        List graphs = null;
        try {
          AnnotatedSeqGroup seq_group = SingletonGenometryModel.getGenometryModel().getSelectedSeqGroup();
          graphs = loadGraphFiles(files, seq_group, aseq, true, monitor, gviewer);
        } catch (final Throwable t) { // catch Out-Of-Memory Errors, etc.
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              if (monitor != null) monitor.closeDialog();
              ErrorHandler.errorPanel("Error loading graphs", t);
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
   *  The return List will contain all sucessfully loaded GraphSym objects;
   *  there may be more or fewer graphs than given files.
   *  @param aseq  a BioSeq. If null, this routine will return an empty List
   *  @param update_viewer  If true, this will call {@link #updateViewer(SeqMapView)} after
   *   each graph file is loaded.
   *  @throws OutOfMemoryError  Any routine can throw an OutOfMemoryError, but
   *  this is a reminder that this is fairly common here.  (You have to catch
   *  "Throwable" rather than "Exception" to catch these.)
   */
  static List loadGraphFiles(URL[] files, AnnotatedSeqGroup seq_group, BioSeq aseq, boolean update_viewer, ThreadProgressMonitor monitor, SeqMapView gviewer)
  throws IOException, OutOfMemoryError {
    List graphs = Collections.EMPTY_LIST;
    if (aseq != null) {
      for (int i=0; i<files.length; i++) {
        if (monitor != null) {monitor.setMessageEventually("Loading graph from: "+files[i].getPath());}
        graphs = loadGraphFile(files[i], seq_group, aseq);
        if (update_viewer && ! graphs.isEmpty()) {updateViewer(gviewer);}
      }
    }
    return graphs;
  }

  //TODO: Make a version that works with an InputStream instead of a url
  public static List loadGraphFile(URL furl, AnnotatedSeqGroup seq_group, BioSeq aseq) throws IOException, OutOfMemoryError {
    List graphs = Collections.EMPTY_LIST;
    InputStream fis = null;
    try {
      String path = furl.getPath();
      if (! GraphSymUtils.isAGraphFilename(path)) {
        throw new IOException("Filename does not match any known type of graph:\n" + path);
      }
      if (Application.CACHE_GRAPHS)  {
        String graph_url = furl.toExternalForm();
        System.out.println("in OpenGraphAction.loadGraphFile(), url external form: " + graph_url);
        fis = LocalUrlCacher.getInputStream(graph_url);
      }
      else {
        fis = furl.openStream();
      }

      graphs = GraphSymUtils.readGraphs(fis, furl.toExternalForm(), gmodel, seq_group);
      
      String graph_name = getGraphNameForURL(furl);
      // Now set the graph names (either the URL or the filename, possibly with an integer appended)
      for (int i=0; i<graphs.size(); i++) {
                com.affymetrix.genometryImpl.GraphSym gg = (com.affymetrix.genometryImpl.GraphSym) graphs.get(i);

        IAnnotStyle style = gg.getGraphState().getTierStyle();
        
        String name = graph_name;
        if (graphs.size() > 1) {
          name = name + " " + (i+1);
        }
        if (style.getHumanName().equals(gg.getID())) {
          //Only apply a new graph name it current name is the same as the ID.
          //(Because the ID is mainly for internal use and if a different name
          // has already been set by the parser, it is probably a good one.)
          style.setHumanName(name);
        }
      }      
      
    } finally {
      if (fis != null) try { fis.close(); } catch (IOException ioe) {}
    }
    return graphs;
  }

  /**
   *  Return a graph name for the given URL.  The graph name is typically just
   *  the last portion of the URL, but the entire URL may be used, depending on
   *  the preference GraphGlyphUtils.PREF_USE_URL_AS_NAME.
   */
  public static String getGraphNameForURL(URL furl) {
    String name;
    boolean use_full_url = GraphGlyphUtils.getGraphPrefsNode().getBoolean(
        GraphGlyphUtils.PREF_USE_URL_AS_NAME, GraphGlyphUtils.default_use_url_as_name);
    if (use_full_url) {
      name = furl.toExternalForm();
    } else { // use only the filename, not the whole url
      name = furl.getFile();
      int index = name.lastIndexOf('/');
      if (index > 0) {
        String last_name = name.substring(index+1);
        if (last_name.length()>0) { 
          name = URLDecoder.decode(last_name); 
        }
      }
    }
    return name;
  }

  public static String getGraphNameForFile(String name) {
    boolean use_full_url = GraphGlyphUtils.getGraphPrefsNode().getBoolean(
        GraphGlyphUtils.PREF_USE_URL_AS_NAME, GraphGlyphUtils.default_use_url_as_name);
    if (use_full_url) {
      // leave the name alone
    } else { // use only the filename, not the whole url
      int index = name.lastIndexOf(System.getProperty("file.separator"));
      if (index > 0) {
        String last_name = name.substring(index+1);
        if (last_name.length()>0) { 
          // shouldn't need to do URLDecoder.decode()
          name = last_name; 
        }
      }
    }
    return name;
  }
  
  static JFileChooser chooser = null;

  static JFileChooser getFileChooser() {
    if (chooser == null) {
      chooser = new JFileChooser();
      chooser.setMultiSelectionEnabled(true);
      chooser.addChoosableFileFilter(new UniFileFilter("bar"));
      chooser.addChoosableFileFilter(new UniFileFilter("gr", "Text Graph"));
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



