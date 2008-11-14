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

package com.affymetrix.igb.bookmarks;

import java.net.*;
import java.util.*;
import javax.swing.*;
import java.util.regex.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.Das2LoadView3;
import com.affymetrix.igb.das2.*;
import com.affymetrix.igb.event.UrlLoaderThread;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;

/**
 *  A way of allowing IGB to be controlled via hyperlinks.
 *  (This used to be an implementation of HttpServlet, but it isn't now.)
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
public class UnibrowControlServlet {
  static boolean DEBUG_DAS2_LOAD = false;
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static final Pattern query_splitter = Pattern.compile("[;\\&]");

  Application uni;

  public void setUnibrowInstance(Application uni) {
    this.uni = uni;
  }

  /** Convenience method for retreiving a String parameter from a parameter map
   *  of an HttpServletRequest.
   *  @param map Should be a Map, such as from {@link javax.servlet.ServletRequest#getParameterMap()},
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
   *  {@link javax.servlet.ServletRequest#getParameterMap()}.
   */
  public static void goToBookmark(Application uni, Map parameters) throws NumberFormatException {
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
      BookmarkController.loadGraphsEventually(uni.getMapView(), parameters);
    }

    /*
     AnnotatedSeqGroup seq_group = gmodel.getSeqGroup(version);
    Das2VersionedSource das_version = null;
    if (seq_group != null && (seq_group instanceof Das2SeqGroup)) {
      das_version = ((Das2SeqGroup)seq_group).getOriginalVersionedSource();
    }
*/

    String[] das2_query_urls = (String[]) parameters.get(Bookmark.DAS2_QUERY_URL);
    String[] das2_server_urls = (String[]) parameters.get(Bookmark.DAS2_SERVER_URL);
    String[] data_urls = (String[]) parameters.get(Bookmark.DATA_URL);
    String[] url_file_extensions = (String[]) parameters.get(Bookmark.DATA_URL_FILE_EXTENSIONS);
    loadDataFromURLs(uni, data_urls, url_file_extensions, null);
    loadDataFromDas2(uni, das2_server_urls, das2_query_urls);

    String selectParam = getStringParameter(parameters, "select");
    if (selectParam != null){
      performSelection(selectParam);
    }

  }


  /**
   *  find Das2ServerInfo (or create if not already existing), based on das2_server_url
   *       to add later?  If no
   *  find Das2VersionedSource based on Das2ServerInfo and das2_query_url (search for version's FEATURE capability URL matching path of das2_query_url)
   *  create Das2FeatureRequestSym
   *  call processFeatureRequests(request_syms, update_display, thread_requests)
   *       (which in turn call Das2ClientOptimizer.loadFeatures(request_sym))
   */
  public static void loadDataFromDas2(final Application uni, final String[] das2_server_urls, final String[] das2_query_urls) {
    if (das2_server_urls == null || das2_query_urls == null || das2_query_urls.length == 0) { return; }
    else if (das2_server_urls.length != das2_query_urls.length) { return; }
    if (DEBUG_DAS2_LOAD)  { System.out.println("UnibrowControlServlet.loadDataFromDas2 called"); }
    ArrayList das2_requests = new ArrayList();
    ArrayList opaque_requests = new ArrayList();
    for (int i=0; i<das2_server_urls.length; i++) {
      String das2_server_url = URLDecoder.decode(das2_server_urls[i]);
      String das2_query_url = URLDecoder.decode(das2_query_urls[i]);

      String cap_url = null;
      String seg_uri = null;
      String type_uri = null;
      String overstr = null;
      String format = null;

      boolean use_optimizer = true;

      int qindex = das2_query_url.indexOf('?');
      if (qindex > -1) {
	cap_url = das2_query_url.substring( 0, qindex );
	if (DEBUG_DAS2_LOAD)  { System.out.println("     capability: " + cap_url); }
	String query = das2_query_url.substring( qindex+1 );
	String[] query_array = query_splitter.split(query);
	for (int k=-0; k<query_array.length; k++)  {
	  String tagval = query_array[k];
	  int eqindex = tagval.indexOf('=');
	  String tag = tagval.substring(0, eqindex);
	  String val = tagval.substring(eqindex+1);
	  if (DEBUG_DAS2_LOAD)  { System.out.println("     query param, tag = : " + tag + ", val = " + val); }
	  if (tag.equals("format") && (format == null)) { format = val; }
	  else if (tag.equals("type") && (type_uri == null)) { type_uri = val; }
	  else if (tag.equals("segment") && (seg_uri == null)) { seg_uri = val; }
	  else if (tag.equals("overlaps") && (overstr == null)) { overstr = val; }
	  else {
	    use_optimizer = false;
	    break;
	  }
	}
	if (type_uri == null || seg_uri== null || overstr == null) { use_optimizer = false; }
      }
      else { use_optimizer = false; }

      //
      // only using optimizer if query has 1 segment, 1 overlaps, 1 type, 0 or 1 format, no other params
      // otherwise treat like any other opaque data url via loadDataFromURLs call
      //
      if (use_optimizer) {
	try {
	  Das2ServerInfo server = Das2Discovery.getDas2Server(das2_server_url);
	  if (server == null) { server = Das2Discovery.addDas2Server(das2_server_url, das2_server_url); }
	  if (DEBUG_DAS2_LOAD)  { System.out.println("     server: " + server.getID()); }
	  server.getSources(); // forcing initialization of server sources, versioned sources, version sources capabilities

	  Das2VersionedSource version = (Das2VersionedSource)Das2Discovery.getCapabilityMap().get(cap_url);
	  if (DEBUG_DAS2_LOAD)  { System.out.println("     version: " + version.getID()); }
	  Das2Type dtype = (Das2Type)version.getTypes().get(type_uri);
	  Das2Region segment = (Das2Region)version.getSegments().get(seg_uri);
	  String[] minmax = overstr.split(":");
	  int min = Integer.parseInt(minmax[0]);
	  int max = Integer.parseInt(minmax[1]);
	  SeqSpan overlap = new SimpleSeqSpan(min, max, segment.getAnnotatedSeq());
	  Das2FeatureRequestSym request = new Das2FeatureRequestSym(dtype, segment, overlap, null);
	  request.setFormat(format);
	  if (DEBUG_DAS2_LOAD)  { System.out.println("adding das2 request: " + das2_query_url); }
	  das2_requests.add(request);
	}
	catch (Exception ex)  {
	  // something went wrong with deconstructing DAS/2 query URL, so just add URL to list of opaque requests
	  ex.printStackTrace();
	  use_optimizer = false;
	  opaque_requests.add(das2_query_url);
	}
      }

    }

    if (das2_requests.size() > 0)  {
      Das2LoadView3.processFeatureRequests(das2_requests, true);
    }
    if (opaque_requests.size() > 0) {
      String[] data_urls = new String[opaque_requests.size()];
      for (int r=0; r<opaque_requests.size(); r++)  { data_urls[r] = (String)opaque_requests.get(r); }
      loadDataFromURLs(uni, data_urls, null, null);
    }
  }

  public static void loadDataFromURLs(final Application uni, final String[] data_urls, final String[] extensions, final String[] tier_names) {
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
      Application.errorPanel("Error loading bookmark\nData URL malformed\n", e);
    }
    catch (InterruptedException ex){}
  }

  static boolean goToBookmark(Application uni, String seqid, String version,
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

  static boolean goToBookmark(Application uni, String seqid, String version, int start, int end,
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
  static boolean goToBookmark(final Application uni, final String seqid, final String version,
      final int start, final int end, final int selstart, final int selend,
      final String[] graph_files) {

    final SeqMapView gviewer = uni.getMapView();
    //final SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    final AnnotatedSeqGroup selected_group = gmodel.getSelectedSeqGroup();
    final AnnotatedSeqGroup book_group;
    if (version == null || "unknown".equals(version) || version.trim().equals("")) {
      book_group = selected_group;
    } else {
      book_group = gmodel.getSeqGroup(version);
    }

    if (book_group == null) {
      Application.errorPanel("Bookmark genome version seq group '"+version+"' not found.\n"+
          "You may need to choose a different QuickLoad server.");
      return false; // cancel
    }

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          // System.out.println("current group: " + ((selected_group == null) ? "null" : selected_group.getID()) );
          //System.out.println("bookmark group: " + ((book_group == null) ? "null" : book_group.getID()) );
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


          // System.out.println("current seq: " + ((selected_seq == null) ? "null" : selected_seq.getID()) );
          //System.out.println("bookmark seq: " + ((book_seq == null) ? "null" : book_seq.getID()) );

          if (seqid == null || "unknown".equals(seqid) || seqid.trim().equals("")) {
            book_seq = selected_seq;
            if (book_seq == null && gmodel.getSelectedSeqGroup().getSeqCount() > 0) {
              book_seq = gmodel.getSelectedSeqGroup().getSeq(0);
            }
          } else {
            book_seq = book_group.getSeq(seqid);
          }

          if (book_seq == null) {
            Application.errorPanel("No seqid", "The bookmark did not specify a valid seqid: specified '"+seqid+"'");
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

    List<SeqSymmetry> sym_list = new ArrayList<SeqSymmetry>(ids.length);
    for (int i=0; i<ids.length; i++) {
      sym_list.addAll(gmodel.getSelectedSeqGroup().findSyms(ids[i]));
    }

    gmodel.setSelectedSymmetriesAndSeq(sym_list, UnibrowControlServlet.class);
  }
}
