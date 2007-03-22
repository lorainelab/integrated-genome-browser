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

package com.affymetrix.igb.servlets;

import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.event.UrlLoaderThread;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.swing.DisplayUtils;

/**
 *  A way of allowing IGB to be controlled via hyperlinks.
 * <pre>
 *  Can specify:
 *      genome version
 *      chromosome
 *      start of region in view
 *      end of region in view
 *  and bring up corect version, chromosome, and region with (at least)
 *      annotations that can be loaded via QuickLoaderView
 *  If the currently loaded genome doesn't match the one requested, might
 *      ask the user before switching.
 *</pre>
 */
public class UnibrowControlServlet extends HttpServlet {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  IGB uni;

  public void init() { }

  public void setUnibrowInstance(IGB uni) {
    this.uni = uni;
  }
  
  public void service(HttpServletRequest request, HttpServletResponse response) throws
    ServletException {

    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    if (request.getParameter("ping") != null) {
      // query from another instance of unibrow to see if port is occupied...
      System.out.println("Received ping request");
      return;
    } else {
      System.out.println("Received bookmark request");
    }
    
    //  restore and focus on IGB when a unibrow call is made
    try {
      DisplayUtils.bringFrameToFront(IGB.getSingletonIGB().getFrame());
      goToBookmark(this.uni, request.getParameterMap());
    } catch (Exception e) {
      System.out.println("Error while processing IGB bookmark: " + e.toString());
      ServletException se = new ServletException("Exception while processing IGB bookmark", e);
      throw se;
    }
  }

  /** Convenience method for retreiving a String parameter from a parameter map
   *  of an HttpServletRequest.
   *  @param map Should be a Map, such as from {@link HttpServletRequest#getParameterMap()},
   *  where the only keys are String and String[] objects.
   *  @param key Should be a key where you only want a single String object as result.
   *  If the value in the map is a String[], only the first item in the array will
   *  be returned.
   */
  public static String getStringParameter(Map map, String key) {
    Object o = map.get(key);
    if (o instanceof String) {
      return (String) o;
    } else if (o instanceof String[]) {
      return ((String[]) o)[0];
    } else if (o != null) {
      // This is a temporary case, for handling Integer objects holding start and end
      // in the old BookMarkAction.java class.  The new version of that class
      // puts everything into String[] objects, so this case can go away.
      return o.toString();
    }
    return null;
  }

  /** Loads a bookmark.
   *  @param parameters Must be a Map where the only values are String and String[]
   *  objects.  For example, this could be the Map returned by
   *  {@link HttpServletRequest#getParameterMap()}.
   */
  public static void goToBookmark(IGB uni, Map parameters) throws NumberFormatException {
    String seqid = getStringParameter(parameters, Bookmark.SEQID);
    String version = getStringParameter(parameters, Bookmark.VERSION);
    String start_param = getStringParameter(parameters, Bookmark.START);
    String end_param = getStringParameter(parameters, Bookmark.END);
    String select_start_param = getStringParameter(parameters, Bookmark.SELECTSTART);
    String select_end_param = getStringParameter(parameters, Bookmark.SELECTEND);

    // For historical reasons, there are two ways of specifying graphs in a bookmark
    // Eventually, they should be treated more similarly, but for now some
    // differences remain
    // parameter "graph_file" can be handled by goToBookmark()
    //    Does not check whether the file was previously loaded
    //    Loads in GUI-friendly thread
    //    Must be a file name, not a generic URL
    // parameter "graph_source_url_0", "graph_source_url_1", ... is handled elsewhere
    //    Checks to avoid double-loading of files
    //    Loading can freeze the GUI
    //    Can be any URL, not just a file
    String[] graph_files = (String[]) parameters.get("graph_file");
    boolean has_graph_source_urls = (parameters.get("graph_source_url_0") != null);

    boolean ok = goToBookmark(uni, seqid, version, start_param, end_param, select_start_param, select_end_param, graph_files);
    if (!ok) { return; /* user cancelled the change of genome, or something like that */}

    if (has_graph_source_urls) {
      com.affymetrix.igb.bookmarks.BookmarkController.loadGraphsEventually(uni.getMapView(), parameters);
    }

    String[] data_urls = (String[]) parameters.get(Bookmark.DATA_URL);
    String[] url_file_extensions = (String[]) parameters.get(Bookmark.DATA_URL_FILE_EXTENSIONS);
    loadDataFromURLs(uni, data_urls, url_file_extensions, null);
    
    String selectParam = getStringParameter(parameters, "select");
    if (selectParam != null){
      performSelection(selectParam);
    }
    
  }

  public static void loadDataFromURLs(final IGB uni, final String[] data_urls, final String[] extensions, final String[] tier_names) {
    try {
      if (data_urls != null && data_urls.length != 0) {
        URL[] urls = new URL[data_urls.length];
        for (int i=0; i<data_urls.length; i++) {
          urls[i] = new URL(data_urls[i]);
        }
        final UrlLoaderThread t = new UrlLoaderThread(uni.getMapView(), urls, extensions, tier_names);
        t.runEventually();
        t.join();
      }
    } catch (MalformedURLException e) {
      IGB.errorPanel("Error loading bookmark\nData URL malformed\n", e);
    }
    catch (InterruptedException ex){}
  }

  static boolean goToBookmark(IGB uni, String seqid, String version,
                           String start_param, String end_param,
                           String select_start_param, String select_end_param,
                           final String[] graph_files) throws NumberFormatException {

    int start = 0;
    int end = Integer.MAX_VALUE;
    if (start_param == null || start_param.equals("")) {
      System.err.println("No start value found in the bookmark URL");
    }
    else {
      start = Integer.parseInt(start_param);
    }
    if (end_param == null || end_param.equals("")) {
      System.err.println("No end value found in the bookmark URL");
    }
    else {
      end = Integer.parseInt(end_param);
    }
    int selstart = -1;
    int selend = -1;
    if (select_start_param != null && select_end_param != null
        && select_start_param.length()>0 && select_end_param.length()>0) {
      selstart = Integer.parseInt(select_start_param);
      selend = Integer.parseInt(select_end_param);
    }
    return goToBookmark(uni, seqid, version, start, end, selstart, selend, graph_files);
  }

  static boolean goToBookmark(IGB uni, String seqid, String version, int start, int end,
                           final String[] graph_files) {
    return goToBookmark(uni, seqid, version, start, end, -1, -1, graph_files);
  }

  /** Loads the sequence and goes to the specified location.
   *  If version doesn't match the currently-loaded version,
   *  asks the user if it is ok to proceed.
   *  NOTE:  This schedules events on the AWT event queue.  If you want
   *  to make sure that everything has finished before you do something
   *  else, then you have to schedule that something else to occur
   *  on the AWT event queue.
   *  @param graph_files it is ok for this parameter to be null.
   *  @return true indicates that the action suceeded
   */
  static boolean goToBookmark(final IGB uni, final String seqid, final String version,
      final int start, final int end, final int selstart, final int selend,
      final String[] graph_files) {
    
    final SeqMapView gviewer = uni.getMapView();
    final SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    final AnnotatedSeqGroup selected_group = gmodel.getSelectedSeqGroup();
    final AnnotatedSeqGroup book_group;
    if (version == null || "unknown".equals(version) || version.trim().equals("")) {
      book_group = selected_group;
    } else {
      book_group = gmodel.getSeqGroup(version);
    }
    
    if (book_group == null) {
      IGB.errorPanel("Bookmark genome version seq group '"+version+"' not found.\n"+
          "You may need to choose a different QuickLoad server.");
      return false; // cancel
    }
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          System.out.println("current group: " + ((selected_group == null) ? "null" : selected_group.getID()) );
          System.out.println("bookmark group: " + ((book_group == null) ? "null" : book_group.getID()) );
          if (book_group != null && ! book_group.equals(selected_group)) {
            if (selected_group != null && ! selected_group.equals("null")) {
              // confirmation panel not needed since curations are not now possible
              //if (uni.confirmPanel("Do you want to switch the Genome version to '" + version )) {
                gmodel.setSelectedSeqGroup(book_group);
              //}
            } else {
              gmodel.setSelectedSeqGroup(book_group);
            }
          }
       
          // hopefully setting gmodel's selected seq group above triggered population of seqs
          //   for group if not already populated
          MutableAnnotatedBioSeq selected_seq = gmodel.getSelectedSeq();
          MutableAnnotatedBioSeq book_seq = book_group.getSeq(seqid);
          
          
          System.out.println("current seq: " + ((selected_seq == null) ? "null" : selected_seq.getID()) );
          System.out.println("bookmark seq: " + ((book_seq == null) ? "null" : book_seq.getID()) );
          
          if (seqid == null || "unknown".equals(seqid) || seqid.trim().equals("")) {
            book_seq = selected_seq;
            if (book_seq == null && gmodel.getSelectedSeqGroup().getSeqCount() > 0) {
              book_seq = gmodel.getSelectedSeqGroup().getSeq(0);
            }
          } else {
            book_seq = book_group.getSeq(seqid);
          }
          
          if (book_seq == null) {
            IGB.errorPanel("No seqid", "The bookmark did not specify a valid seqid: specified '"+seqid+"'");
          } else {
            // gmodel.setSelectedSeq() should trigger a gviewer.setAnnotatedSeq() since
            //     gviewer is registered as a SeqSelectionListener on gmodel
            if (book_seq != selected_seq)  {
              gmodel.setSelectedSeq(book_seq);
            }
            
            final SingletonSeqSymmetry regionsym = new SingletonSeqSymmetry(selstart, selend, book_seq);
            final SeqSpan view_span = new SimpleSeqSpan(start, end, book_seq);
            final double middle = (start + end)/2.0;
            if (start >=0 && end > 0 && end != Integer.MAX_VALUE) {
              gviewer.setZoomSpotX(middle);
              gviewer.zoomTo(view_span);
            }
            if (selstart >= 0 && selend >= 0) {
              gviewer.setSelectedRegion(regionsym, true);
            }
          }

          // Now process "graph_files" url's
          if (graph_files != null) {
            URL[] graph_urls = new URL[graph_files.length];
            for (int i = 0; i < graph_files.length; i++) {
              graph_urls[i] = new URL(graph_files[i]);
            }
            Thread t = com.affymetrix.igb.menuitem.OpenGraphAction.
                loadAndShowGraphs(graph_urls, gmodel.getSelectedSeq(), gviewer);
            t.start();
          }

        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    return true; // was not cancelled, was sucessful
  }
  
  
  /**
   * This handles the "select" API parameter.  The "select" parameter can be followed by one 
   * or more comma separated IDs in the form: &select=<id_1>,<id_2>,...,<id_n>
   * Example:  "&select=EPN1,U2AF2,ZNF524" 
   * Each ID that exists in IGB's ID to symmetry hash will be selected, even if the symmetries 
   * lie on different sequences.
   * @param selectParam The select parameter passed in through the API
   */
  private static void performSelection(String selectParam) {

    if (selectParam == null){return;}
    
    // split the parameter by commas
    String[] ids = selectParam.split(",");
    
    if (selectParam.length() == 0) {return;}
    
    List sym_list = new ArrayList(ids.length);
    for (int i=0; i<ids.length; i++) {
      sym_list.addAll(gmodel.getSelectedSeqGroup().findSyms(ids[i]));
    }
    
    gmodel.setSelectedSymmetriesAndSeq(sym_list, UnibrowControlServlet.class);    
  }
}
