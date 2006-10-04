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

package com.affymetrix.igb.event;

import java.net.*;
import java.io.*;
import javax.swing.SwingUtilities;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.parsers.BpsParser;
import com.affymetrix.igb.parsers.Das1FeatureSaxParser;
import com.affymetrix.igb.parsers.PSLParser;

public class UrlLoaderThread extends Thread {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  final URL[] urls;
  final String[] tier_names;
  final SeqMapView gviewer;

  /** A convenience method that makes it easier to get an instance for loading
   *  a <i>single</i> URL.  For loading multiple URLs, use the main constructor.
   */
  public static UrlLoaderThread getUrlLoaderThread(SeqMapView smv, URL das_url, String tier_name) {
    URL[] das_urls = new URL[1];
    das_urls[0] = das_url;
    String[] tier_names = null;
    if (tier_name != null) {tier_names = new String[] {tier_name};}
    return new UrlLoaderThread(smv, das_urls, tier_names);
  }

  /**
   *  Creates a thread that can be used to load data.
   *  A ThreadProgressMonitor will be opened to show the user that something is
   *  happening.
   *  @param smv The SeqMapView instance to load data into
   *  @param das_urls  The URLs that will load PSL data from a DAS server
   *  @param tier_names  The names for the data tiers.  If you specify <code>null</code>,
   *  the tier names will be determined from the "type" parameter of each URL.
   *  If a non-null array is provided, the length must match the length of the
   *  das_urls array.
   */
  public UrlLoaderThread(SeqMapView smv, URL[] das_urls, String[] tier_names) {
    if (tier_names != null && das_urls.length != tier_names.length) {
      throw new IllegalArgumentException("Array lengths do not match");
    }
    this.gviewer = smv;
    this.urls = das_urls;
    this.tier_names = tier_names;
  }

  public void run() {
    ThreadProgressMonitor monitor = null;
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    AnnotatedSeqGroup seq_group = gmodel.getSelectedSeqGroup();
    try {
      // should really move to using gmodel's currently selected  _group_ of sequences rather than
      //    a single sequence...
      if (aseq == null) {
        throw new RuntimeException("UrlLoaderThread: aborting because there is no currently selected seq");
      }
      if (seq_group == null) {
        throw new RuntimeException("UrlLoaderThread: aborting because there is no currently selected seq group");
      }
      //System.out.println("in UrlLoaderThread, get selected seq: " + aseq.getID());

      monitor = new ThreadProgressMonitor(
        gviewer.getFrame(),
        "Loading...",
        "Loading data from URL",
        this,
        false, false);
      monitor.showDialogEventually();

      for (int i=0; i<urls.length; i++) {
        if (isInterrupted() || monitor.isCancelled()) {break;}

        URL url = urls[i];
         String tier_name = null;
        if (tier_names != null) {
          tier_name = tier_names[i];
        } else {
          tier_name = parseTermName(url, "DAS_Data");
        }

        System.out.println("Attempting to load data from URL: "+url.toExternalForm());

        String where_from = url.getHost();
        if (where_from == null || where_from.length()==0) {
          where_from = url.getPath();
        }
        monitor.setMessageEventually("Connecting to "+where_from);
        if (isInterrupted() || monitor.isCancelled()) {break;}

        URLConnection connection = url.openConnection();
        connection.connect(); // throws an exception if no connection available

        monitor.setMessageEventually("Loading data from "+where_from);
        if (isInterrupted() || monitor.isCancelled()) {break;}

        try {
	  //  parseDataFromURL(gviewer, connection, aseq, tier_name);
	  parseDataFromURL(gviewer, connection, tier_name);
        }
        catch (IOException ex){handleException(ex); continue;}

        monitor.setMessageEventually("Updating view");
        if (isInterrupted() || monitor.isCancelled()) {break;}

        // update the view, except for the last time where we let the "finally" block do it
        if (i<urls.length) {updateViewer(gviewer, aseq);}
      }

    } catch (Exception e) {
      if (! (e instanceof InterruptedException)) { handleException(e); }
    } finally {
      if (monitor != null) {monitor.closeDialogEventually();}
      // update the view again, mainly in case the thread was interrupted
      updateViewer(gviewer, aseq);
    }
  }

  void handleException(final Exception e) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (e instanceof UnknownHostException) {
          com.affymetrix.igb.IGB.getSingletonIGB().setStatus("Unknown host: "+e.getMessage());
        } else if (e instanceof FileNotFoundException) {
          ErrorHandler.errorPanel(gviewer.getFrame(), "File not found",
            "File missing or not readable:\n "+e.getMessage(), null);
        } else {
          com.affymetrix.igb.IGB.getSingletonIGB().setStatus(e.getMessage());
        }
      }
    });
  }


  void updateViewer(final SeqMapView gviewer, final MutableAnnotatedBioSeq seq) {
    if (gviewer==null || seq==null) return;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          gviewer.setAnnotatedSeq(seq, true, true);
        } catch (Exception e) {
          handleException(e);
        }
      }
    });
  }

  /** Use SwingUtilities.invokeLater() to schedule this Thread to be
   *  started later.  This lets Swing finish-up whatever else it was doing
   *  before calling start() on this Thread.  (The Thread itself doesn't actually
   *  run on the Swing event thread.)
   *
   *  In many cases, you could just call start() instead of calling this.
   *  But, if you already have some events pending on the Swing event thread,
   *  then calling this will make sure they finish first.  For example,
   *  this method is needed in the UnibrowControlServlet when a manipulation
   *  of the QuickLoad GUI needs to be followed by a file load.
   */
  public void runEventually() {
    // Note: we do NOT want to simply call SwingUtilities.invokeLater(this)
    // because that would cause this thread to actually run ON the Swing thread
    // (potentially freezing the GUI)
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        start();
      }
    });
  }

  // Parses term names from a "type" string.
  // Returns default_value if parsing fails, or there is no "type" string.
  // Example:  "type=one;two;three"  -> {"one", "two", "three"}
  static String parseTermName(URL url, String default_value) {
    //TODO: unlike the rest of this class, this IS specific to DAS and should be moved to DasUtils
    String value = null;
    String query = url.getQuery();
    try {
      int index1 = query.indexOf("type=");
      if (index1 > -1) {
        int index1b = index1 + 5;
        int index2 = query.indexOf(';', index1b);
        if (index2 == -1) {
          index2 = query.length();
        }
        value = query.substring(index1b, index2);
      }
    } catch (Exception e) {
      // do nothing.  Just use the default string value
    }
    if (value == null || value.length() == 0) {
      value = default_value;
    }
    return value;
  }

   /**
   *  Opens a binary data stream from the given url and adds the resulting
   *  data to the given BioSeq.
   *  @param type  a parameter passed on to parsePSL
   */
  static void parseDataFromURL(SeqMapView gviewer, URLConnection feat_request_con, String type)
    throws java.net.UnknownHostException, java.io.IOException {

    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    AnnotatedSeqGroup seq_group = gmodel.getSelectedSeqGroup();
    //TODO: This is an important part of the data loading code, but it could be improved.

    String content_type = feat_request_con.getContentType();
    int content_length = feat_request_con.getContentLength();
    if (content_length == 0) { // Note: length == -1 means "length unknown"
      throw new IOException("\n" + feat_request_con.getURL() + " returned no data.");
    }

    URL url = feat_request_con.getURL();
    if (content_type==null) {content_type="content/unknown";} // to avoid null pointer
    if (content_type == null ||
	content_type.startsWith("content/unknown") ||
	content_type.startsWith("application/zip") ||
	content_type.startsWith("application/octet-stream") ||
	"file".equals(url.getProtocol().toLowerCase()))
    {
      System.out.println("Attempting to load data from: " + url.toExternalForm());

      // Note: we want the filename so we can guess the filetype from the ending, like ".psl" or ".psl.gz"
      // url.getPath() is OK for this purpose, url.getFile() is not because
      // url.getFile() = url.getPath() + url.getQuery()
      String filename = url.getPath();

      InputStream stream = feat_request_con.getInputStream();
      LoadFileAction.load(gviewer, stream,  filename, aseq);
    }
    else if (content_type.startsWith("binary/bps")) {
      parseBinaryBps(feat_request_con, type);
    }
    else if (content_type.startsWith("text/plain") ||
               content_type.startsWith("text/html") ||
               content_type.startsWith("text/xml")
        ) {
      // Note that some http servers will return "text/html" even when that is untrue.
      // we could try testing whether the filename extension is a recognized extension, like ".psl"
      // and if so passing to LoadFileAction.load(.. feat_request_con.getInputStream() ..)
      parseDasGff(feat_request_con);
    }
    else if (content_type.startsWith("text/psl")) {
      parsePSL(feat_request_con, type);
    }
    else {
      throw new IOException("Declared data type "+content_type+" cannot be processed");
    }
  }

  /**
   *  Opens a text input stream from the given url, parses it has a
   *  PSL file, and then adds the resulting data to the given BioSeq,
   *  using the parser {@link PSLParser}.
   */
  static void parsePSL(URLConnection feat_request_con, String type)
  throws IOException {
    MutableAnnotatedBioSeq new_seq = null;
    InputStream result_stream = null;
    BufferedInputStream bis = null;
    try {
      result_stream = feat_request_con.getInputStream();
      bis = new BufferedInputStream(result_stream);
      AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
      PSLParser parser = new PSLParser();
      parser.enableSharedQueryTarget(true);
      parser.parse(bis, type, null, group, false, true);
      group.symHashChanged(parser);
    } finally {
      if (bis != null) try {bis.close();} catch (Exception e) {}
      if (result_stream != null) try {result_stream.close();} catch (Exception e) {}
    }
  }

  /**
   *  Opens a text input stream from the given url and adds the resulting
   *  data to the given BioSeq.
   */
  static void parseDasGff(URLConnection feat_request_con)
  throws IOException {
    InputStream result_stream = null;
    BufferedInputStream bis = null;
    try {
      result_stream = feat_request_con.getInputStream();
      bis = new BufferedInputStream(result_stream);
      Das1FeatureSaxParser das_parser = new Das1FeatureSaxParser();
      das_parser.parse(result_stream, gmodel.getSelectedSeqGroup());
      
    } finally {
      
      if (bis != null) try {bis.close();} catch (Exception e) {}
      if (result_stream != null) try {result_stream.close();} catch (Exception e) {}
    }
  }

  /**
   *  Opens a binary data stream from the given url and adds the resulting
   *  binary/bps data to the given BioSeq.
   *  @param type  a parameter passed on to
   *  {@link BpsParser#parse(DataInputStream, String, Map)}.
   */
  static void parseBinaryBps(URLConnection feat_request_con, String type)
  throws IOException {
    InputStream result_stream = null;
    BufferedInputStream bis = null;
    DataInputStream dis = null;
    try {
      result_stream = feat_request_con.getInputStream();
      bis = new BufferedInputStream(result_stream);
      dis = new DataInputStream(bis);

      BpsParser bps_parser = new BpsParser();
      AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
      
      bps_parser.parse(dis, type, null, group, false, true);
      group.symHashChanged(bps_parser);
    } finally {
      if (dis != null) try {dis.close();} catch (Exception e) {}
      if (bis != null) try {bis.close();} catch (Exception e) {}
      if (result_stream != null) try {result_stream.close();} catch (Exception e) {}
    }
  }

}
