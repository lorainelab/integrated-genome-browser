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

package com.affymetrix.igb.servlets;

import java.awt.Frame;
import java.io.*;
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
 *  If the currently loaded genome doesn't match the one requested, will
 *      ask the user before switching.
 *
 *  WARNING:
 *     Using version=unknown to suprress genome checking is no longer supported!
 *</pre>
 */
public class UnibrowControlServlet extends HttpServlet {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static int graph_height = 60;

  IGB uni;

  public void init() { }

  public void setUnibrowInstance(IGB uni) {
    this.uni = uni;
  }
  
  public void service(HttpServletRequest request, HttpServletResponse response) throws
    ServletException, IOException, NumberFormatException {
    System.out.println("UnibrowControlServlet received request");
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    if (request.getParameter("ping") != null) {
      // query from another instance of unibrow to see if port is occupied...
      System.out.println("received ping request");
      return;
    }
    
    //  restore and focus on IGB when a unibrow call is made
    if ((IGB.getSingletonIGB().getFrame().getExtendedState() & Frame.ICONIFIED) == 1)
    {
    	IGB.getSingletonIGB().getFrame().setExtendedState(Frame.NORMAL);
    }
    IGB.getSingletonIGB().getFrame().toFront();
    goToBookmark(this.uni, request.getParameterMap());
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
    loadDataFromURLs(uni, data_urls, null);
    
    String selectParam = getStringParameter(parameters, "select");
    if (selectParam != null){
    	performSelection(selectParam);
    }
    
  }

  public static void loadDataFromURLs(final IGB uni, final String[] das_urls, final String[] tier_names) {
    try {
      if (das_urls != null && das_urls.length != 0) {
        URL[] urls = new URL[das_urls.length];
        for (int i=0; i<das_urls.length; i++) {
          urls[i] = new URL(das_urls[i]);
        }
        final UrlLoaderThread t = new UrlLoaderThread(uni.getMapView(), urls, tier_names);
        t.runEventually();
        t.join();
      }
    } catch (MalformedURLException e) {
      IGB.errorPanel("Error loading bookmark\nDAS URL malformed\n", e);
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
      System.err.println("No start value found in the request URL");
    }
    else {
      start = Integer.parseInt(start_param);
    }
    if (end_param == null || end_param.equals("")) {
      System.err.println("No end value found in the request URL");
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
  static boolean goToBookmark(IGB uni, String seqid, String version,
			   int start, int end, final int selstart, final int selend,
			   final String[] graph_files) {

    if (version == null || version.trim().equals("")) {
      IGB.errorPanel("Genome version not specified in bookmark.");
      return false; // cancel
    }
    if (seqid == null || seqid.trim().equals("")) {
      IGB.errorPanel("Sequence id not specified in bookmark.");
      return false; // cancel
    }

    // no longer special-casing for version = "unknown"...
    final SeqMapView gviewer = uni.getMapView();
    final SingletonGenometryModel gmodel = IGB.getGenometryModel();
    AnnotatedSeqGroup selected_group = gmodel.getSelectedSeqGroup();
    AnnotatedSeqGroup book_group = gmodel.getSeqGroup(version);

    if (book_group == null) {
      IGB.errorPanel("Bookmark genome version seq group '"+version+"' not found.");
      return false; // cancel
    }
    System.out.println("bookmark group: " + ((book_group == null) ? "null" : book_group.getID()) );
    System.out.println("selected group: " + ((selected_group == null) ? "null" : selected_group.getID()) );
    if (book_group != null && ! book_group.equals(selected_group)) {
      if (selected_group != null && ! selected_group.equals("null")) {
        if (!uni.confirmPanel("Do you want to switch the Genome version to '" +
                              version +
                              "' ?\n Please be aware you will loose your current work")) {
          return false; // was cancelled
        }
      }
      gmodel.setSelectedSeqGroup(book_group);
    }
    // quick_view.testLoad(version);
    // quick_view.setSelection(version);

    // gmodel.setSelectedSeq() should trigger a gviewer.setAnnotatedSeq() since
    //     gviewer is registered as a SeqSelectionListener on gmodel

    // hopefully setting gmodel's selected seq group above triggered population of seqs
    //   for group if not already populated
    MutableAnnotatedBioSeq selected_seq = gmodel.getSelectedSeq();
    final MutableAnnotatedBioSeq book_seq = book_group.getSeq(seqid);


    System.out.println("bookmark seq: " + ((book_seq == null) ? "null" : book_seq.getID()) );
    System.out.println("selected seq: " + ((selected_seq == null) ? "null" : selected_seq.getID()) );

    if (book_seq == null) {
      IGB.errorPanel("No seqid", "The bookmark did not specify a valid seqid");
      return false;
    } else {
      if (book_seq != selected_seq)  {
        gmodel.setSelectedSeq(book_seq);
      }

      final SeqSpan view_span = new SimpleSeqSpan(start, end, book_seq);
      final double middle = (start + end)/2.0;
//      System.out.println("graph files: " + graph_files);
      SwingUtilities.invokeLater(new Runnable() {
	  public void run() {
	    try {
//	      MutableAnnotatedBioSeq view_seq = (MutableAnnotatedBioSeq)
//		view_span.getBioSeq();
//	      if (view_seq != gmodel.getSelectedSeq()) {
//		gviewer.setAnnotatedSeq(view_seq);
//                gviewer.setAnnotatedSeq(aseq);
//	      }
          gviewer.setZoomSpotX(middle);
	      gviewer.zoomTo(view_span);
	      if (selstart >= 0 && selend >= 0) {
		SingletonSeqSymmetry regionsym = new SingletonSeqSymmetry(selstart, selend, book_seq);
		gviewer.setSelectedRegion(regionsym, true);
	      }
	      if (graph_files != null) {
		URL[] graph_urls = new URL[graph_files.length];
		for (int i = 0; i < graph_files.length; i++) {
		  graph_urls[i] = new URL(graph_files[i]);
		}
		Thread t = com.affymetrix.igb.menuitem.OpenGraphAction.
		  loadAndShowGraphs(graph_urls, gmodel.getSelectedSeq(), gviewer);
		t.start();
	      }
	    }
	    catch (Exception ex) {
	      ex.printStackTrace();
	    }
	  }
	});
    }
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
  private static void performSelection (String selectParam)
  {  
	  try
	  {
		  if (selectParam == null){return;}
		  HashMap seq2SymsHash = new HashMap();
		  // split the parameter by commas
		  String[] ids = selectParam.split(",");
		  MutableAnnotatedBioSeq seq;
		  SeqSymmetry sym = null;
		  SingletonGenometryModel gmodel = IGB.getGenometryModel();
		  
		  // for each ID found in the ID2sym hash, add it to the owning sequences 
		  //  list of selected symmetries
		  for (int i=0; i<ids.length; i++)
		  {
			  sym = (SeqSymmetry)IGB.getSymHash().get(ids[i]);
		      if (sym != null && 
		         (seq = gmodel.getSelectedSeqGroup().getSeq(sym)) != null)
		      {   	          
		    	  // prepare the list to add the sym to based on the seq ID
		          ArrayList symlist = (ArrayList)seq2SymsHash.get(seq);
		    	  if (symlist == null)
		    	  {
		    		  symlist = new ArrayList();
		    		  seq2SymsHash.put(seq, symlist);
		    	  }
		    	  // add the sym to the list for the correct seq ID
		          symlist.add(sym);	      
		      }	
		  }
		 
		  // if at least one sym was found, then select all the syms for each seq and
		  //  focus on the first matched sequence
		  
		  // clear all the existing selections first
		  gmodel.clearSelectedSymmetries(new Object());
		  
		  // now perform the selections for each sequence that was matched
		  for(Iterator i=seq2SymsHash.keySet().iterator();i.hasNext();) 
		  {
			  seq = (MutableAnnotatedBioSeq)i.next();
			  gmodel.setSelectedSymmetries((List)seq2SymsHash.get(seq), new Object(), seq);
		  }	  	  
	  }
	  catch (Exception ex){ex.printStackTrace();}
  }

}
