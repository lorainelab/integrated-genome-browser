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

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.SimpleTrackStyle;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.bookmarks.Bookmark.SYM;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
 *
 * @version $Id: UnibrowControlServlet.java 7505 2011-02-10 20:27:35Z hiralv $
 *</pre>
 */
public final class BookmarkUnibrowControlServlet {

	private static final BookmarkUnibrowControlServlet instance = new BookmarkUnibrowControlServlet();
	private static final Logger ourLogger
			= Logger.getLogger(BookmarkUnibrowControlServlet.class.getPackage().getName());

	private BookmarkUnibrowControlServlet() {
		super();
	}

	public static final BookmarkUnibrowControlServlet getInstance() {
		return instance;
	}
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final Pattern query_splitter = Pattern.compile("[;\\&]");

	/** Convenience method for retrieving a String parameter from a parameter map
	 *  of an HttpServletRequest.
	 *  @param map Should be a Map, such as from {@link javax.servlet.ServletRequest#getParameterMap()},
	 *  where the only keys are String and String[] objects.
	 *  @param key Should be a key where you only want a single String object as result.
	 *  If the value in the map is a String[], only the first item in the array will
	 *  be returned.
	 */
	String getStringParameter(Map<String, ?> map, String key) {
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
	public void goToBookmark(final IGBService igbService, final Map<String, String[]> parameters) throws NumberFormatException {
		String batchFileStr = getStringParameter(parameters, IGBService.SCRIPTFILETAG);
		if (batchFileStr != null && batchFileStr.length() > 0) {
			igbService.doActions(batchFileStr);
			return;
		}

		CThreadWorker<Object, Void> worker = new CThreadWorker<Object, Void>("goToBookmark") {

			@Override
			protected Object runInBackground() {				
				String seqid = getStringParameter(parameters, Bookmark.SEQID);
				String version = getStringParameter(parameters, Bookmark.VERSION);
				String start_param = getStringParameter(parameters, Bookmark.START);
				String end_param = getStringParameter(parameters, Bookmark.END);
//				String comment_param = getStringParameter(parameters, Bookmark.COMMENT);
				String select_start_param = getStringParameter(parameters, Bookmark.SELECTSTART);
				String select_end_param = getStringParameter(parameters, Bookmark.SELECTEND);
				boolean loadResidue = Boolean.valueOf(getStringParameter(parameters, Bookmark.LOADRESIDUES));
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
				boolean has_properties = (parameters.get(SYM.FEATURE_URL + "0") != null);
				boolean loaddata = true;
				boolean loaddas2data = true;
				int start = 0;
				int end = 0;
				
				//missing seqid or start or end? Attempt to set to current view
				if (missingString(new String[]{seqid, start_param, end_param})){				
					boolean pickOne = false;
					//get AnnotatedSeqGroup for bookmark
					AnnotatedSeqGroup bookMarkGroup = gmodel.getSeqGroup(version);
					if (bookMarkGroup != null){
						//same genome version as that in view?
						SeqMapViewI currentSeqMap = igbService.getSeqMapView();
						if (currentSeqMap != null){
							//get visible span
							SeqSpan currentSpan = currentSeqMap.getVisibleSpan();
							if (currentSpan != null){
								//check genome version, if same then set coordinates
								AnnotatedSeqGroup currentGroup = currentSpan.getBioSeq().getSeqGroup();
								if (currentGroup.equals(bookMarkGroup)){
									start = currentSpan.getStart();
									end = currentSpan.getEnd();
									seqid = currentSpan.getBioSeq().getID();
								}
								else pickOne = true;
							}
							else pickOne = true;
						}
						//pick first chromosome and 1M span
						else pickOne = true;
					}
					//pick something, only works if version was loaded.
					if (pickOne){
						BioSeq bs = bookMarkGroup.getSeq(0);
						if (bs != null){
							int len = bs.getLength();
							seqid = bs.getID();
							start = len/3 - 500000;
							if (start < 0) start = 0;
							end = start + 500000;
							if (end > len) end = len-1;
						}
					}
				}
				
				//attempt to parse from bookmark?
				if (start == 0 && end == 0){
					int values[] = parseValues(start_param, end_param, select_start_param, select_end_param);
					start = values[0];
					end = values[1];
				}

				String[] server_urls = parameters.get(Bookmark.SERVER_URL);
				String[] query_urls = parameters.get(Bookmark.QUERY_URL);
				GenericServer[] gServers = null;

				if (server_urls == null || query_urls == null
						|| query_urls.length == 0 || server_urls.length != query_urls.length) {
					loaddata = false;
				} else {
					gServers = loadServers(igbService, server_urls);
				}

				String[] das2_query_urls = parameters.get(Bookmark.DAS2_QUERY_URL);
				String[] das2_server_urls = parameters.get(Bookmark.DAS2_SERVER_URL);

				GenericServer[] gServers2 = null;

				if (das2_server_urls == null || das2_query_urls == null || das2_query_urls.length == 0 || das2_server_urls.length != das2_query_urls.length) loaddas2data = false; 
				else gServers2 = loadServers(igbService, das2_server_urls);
				
				final BioSeq seq = goToBookmark(igbService, seqid, version, start, end);

				if (null == seq) {
					if(loaddata){
						AnnotatedSeqGroup seqGroup = gmodel.getSelectedSeqGroup();
						if(seqGroup == null || !seqGroup.isSynonymous(version)){
							seqGroup = gmodel.addSeqGroup(version);
						}
						loadChromosomesFor(igbService, seqGroup, gServers, query_urls);
					}
					return null; /* user cancelled the change of genome, or something like that */
				}

				if (loaddata) {
					// TODO: Investigate edge case at max
					if(seq.getMin() == start && seq.getMax() == end){
						end -= 1;
					}
					
					GenericFeature[] gFeatures = loadData(igbService, gmodel.getSelectedSeqGroup(), gServers, query_urls, start, end);

					if (has_properties) {
						List<String> graph_urls = getGraphUrls(parameters);
						final Map<String, ITrackStyleExtended> combos = new HashMap<String, ITrackStyleExtended>();
						
						for (int i = 0; parameters.get(SYM.FEATURE_URL.toString() + i) != null; i++) {
							String combo_name = BookmarkUnibrowControlServlet.getInstance().getStringParameter(parameters, Bookmark.GRAPH.COMBO.toString() + i);
							if (combo_name != null) {
								ITrackStyleExtended combo_style = combos.get(combo_name);
								if (combo_style == null) {
									combo_style = new SimpleTrackStyle("Joined Graphs", true);
									combo_style.setTrackName("Joined Graphs");
									combo_style.setExpandable(true);
									//combo_style.setCollapsed(true);
									//combo_style.setLabelBackground(igbService.getDefaultBackgroundColor());
									combo_style.setBackground(igbService.getDefaultBackgroundColor());
									//combo_style.setLabelForeground(igbService.getDefaultForegroundColor());	
									combo_style.setForeground(igbService.getDefaultForegroundColor());
									combo_style.setTrackNameSize(igbService.getDefaultTrackSize());
									combos.put(combo_name, combo_style);
								}
							}
						}
						
						for (int i = 0; i < gFeatures.length; i++) {
							final GenericFeature feature = gFeatures[i];

							if (feature != null && graph_urls.contains(feature.getURI().toString())) {
								ThreadUtils.getPrimaryExecutor(feature).execute(new Runnable() {

									@Override
									public void run() {
										BookmarkController.applyProperties(igbService, seq, parameters, feature, combos);
									}
								});
							}
						}
					}
				}

				if (loaddas2data) {
					loadOldBookmarks(igbService, gServers2, das2_query_urls, start, end);
				}

				//loadDataFromDas2(uni, das2_server_urls, das2_query_urls);
				//String[] data_urls = parameters.get(Bookmark.DATA_URL);
				//String[] url_file_extensions = parameters.get(Bookmark.DATA_URL_FILE_EXTENSIONS);
				//loadDataFromURLs(uni, data_urls, url_file_extensions, null);
				String selectParam = getStringParameter(parameters, "select");
				if (selectParam != null) {
					igbService.performSelection(selectParam);
				}

				if (loadResidue) {
					BioSeq vseq = GenometryModel.getGenometryModel().getSelectedSeq();
					SeqSpan span = new SimpleMutableSeqSpan(start, end, vseq);
					igbService.loadResidues(span, true);
				}
				return null;
			}

			@Override
			protected void finished() {
			}
		};
		CThreadHolder.getInstance().execute(parameters, worker);
	}

	/**Checks for nulls or Strings with zero length.*/
	public static boolean missingString(String[] params){
		for (String s : params){
			if (s == null) return true;
			if (s.trim().length() == 0) return true;
		}
		return false;
	}

	public List<String> getGraphUrls(Map<String, String[]> map) {
		List<String> graph_paths = new ArrayList<String>();
		for (int i = 0; map.get(SYM.FEATURE_URL.toString() + i) != null; i++) {
			graph_paths.add(getStringParameter(map, SYM.FEATURE_URL.toString() + i));
		}
		return graph_paths;
	}

	private void loadOldBookmarks(final IGBService igbService, GenericServer[] gServers, String[] das2_query_urls, int start, int end) {
		List<String> opaque_requests = new ArrayList<String>();
		for (int i = 0; i < das2_query_urls.length; i++) {
			String das2_query_url = GeneralUtils.URLDecode(das2_query_urls[i]);
			String seg_uri = null;
			String type_uri = null;
			String overstr = null;
			String format = null;
			boolean use_optimizer = true;
			int qindex = das2_query_url.indexOf('?');
			if (qindex > -1) {
				String query = das2_query_url.substring(qindex + 1);
				String[] query_array = query_splitter.split(query);
				for (int k = -0; k < query_array.length; k++) {
					String tagval = query_array[k];
					int eqindex = tagval.indexOf('=');
					String tag = tagval.substring(0, eqindex);
					String val = tagval.substring(eqindex + 1);
					if (tag.equals("format") && (format == null)) {
						format = val;
					} else if (tag.equals("type") && (type_uri == null)) {
						type_uri = val;
					} else if (tag.equals("segment") && (seg_uri == null)) {
						seg_uri = val;
					} else if (tag.equals("overlaps") && (overstr == null)) {
						overstr = val;
					} else {
						use_optimizer = false;
						break;
					}
				}
				if (type_uri == null || seg_uri == null || overstr == null) {
					use_optimizer = false;
				}
			} else {
				use_optimizer = false;
			}
			//
			// only using optimizer if query has 1 segment, 1 overlaps, 1 type, 0 or 1 format, no other params
			// otherwise treat like any other opaque data url via loadDataFromURLs call
			//
			if (!use_optimizer) {
				opaque_requests.add(das2_query_url);
				continue;
			}

			GenericFeature feature = getFeature(igbService, gmodel.getSelectedSeqGroup(), gServers[i], type_uri);
			if (feature != null) {
				loadFeature(igbService, feature, start, end);
			}
		}

		if (!opaque_requests.isEmpty()) {
			String[] data_urls = new String[opaque_requests.size()];
			for (int r = 0; r < opaque_requests.size(); r++) {
				data_urls[r] = opaque_requests.get(r);
			}
			loadDataFromURLs(igbService, data_urls, null, null);
		}
	}

	private GenericFeature[] loadData(final IGBService igbService, final AnnotatedSeqGroup seqGroup, final GenericServer[] gServers, final String[] query_urls, int start, int end) {
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		GenericFeature[] gFeatures = new GenericFeature[query_urls.length];
		for (int i = 0; i < query_urls.length; i++) {
			gFeatures[i] = getFeature(igbService, seqGroup, gServers[i], query_urls[i]);
		}

		for (int i = 0; i < gFeatures.length; i++) {
			GenericFeature gFeature = gFeatures[i];
			if (gFeature != null) {
				loadFeature(igbService, gFeature, start, end);
			}
		}
		igbService.updateGeneralLoadView();

		return gFeatures;
	}

	private void loadChromosomesFor(final IGBService igbService, final AnnotatedSeqGroup seqGroup, final GenericServer[] gServers, final String[] query_urls){
		GenericFeature[] gFeatures = getFeatures(igbService, seqGroup, gServers, query_urls);
		for (int i = 0; i < gFeatures.length; i++) {
			GenericFeature gFeature = gFeatures[i];
			if (gFeature != null) {
				igbService.loadChromosomes(gFeature);
			}
		}
	}
	
	private GenericFeature[] getFeatures(final IGBService igbService, final AnnotatedSeqGroup seqGroup, final GenericServer[] gServers, final String[] query_urls){
		GenericFeature[] gFeatures = new GenericFeature[query_urls.length];
		boolean show_message = false;
		for (int i = 0; i < query_urls.length; i++) {
			gFeatures[i] = getFeature(igbService, seqGroup, gServers[i], query_urls[i]);
			if(gFeatures[i] != null){
				gFeatures[i].setVisible();
				gFeatures[i].setPreferredLoadStrategy(LoadStrategy.VISIBLE);
				if(gFeatures[i].getLoadStrategy() == LoadStrategy.VISIBLE /*||
					gFeatures[i].getLoadStrategy() == LoadStrategy.CHROMOSOME*/){
					show_message = true;
				}	
			}
		}

		igbService.updateGeneralLoadView();
		
		// Show message on how to load
		if(show_message){
			igbService.infoPanel(GenericFeature.howtoloadmsg, 
					GenericFeature.show_how_to_load, GenericFeature.default_show_how_to_load);
		}
		
		return gFeatures;
	}
	
	private GenericFeature getFeature(final IGBService igbService, final AnnotatedSeqGroup seqGroup, final GenericServer gServer, final String query_url) {

		if (gServer == null) {
			return null;
		}

		// If server requires authentication then.
		// If it cannot be authenticated then don't add the feature.
		// This method of authentication does not work for Das2
		//if (!LocalUrlCacher.isValidURL(query_url)) {
		//	return null;
		//}

		GenericFeature feature = igbService.getFeature(seqGroup, gServer, query_url, false);

		if (feature == null) {
			Logger.getLogger(GeneralUtils.class.getName()).log(
					Level.SEVERE, "Couldn''t find feature for bookmark url {0}", query_url);
		}

		return feature;
	}

	private void loadFeature(IGBService igbService, GenericFeature gFeature, int start, int end) {
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		SeqSpan overlap = new SimpleSeqSpan(start, end, seq);
		gFeature.setVisible();
		gFeature.setPreferredLoadStrategy(LoadStrategy.VISIBLE);
		if (gFeature.getLoadStrategy() != LoadStrategy.VISIBLE) {
			overlap = new SimpleSeqSpan(seq.getMin(), seq.getMax(), seq);
		}
		igbService.loadAndDisplaySpan(overlap, gFeature);
	}

	private GenericServer[] loadServers(IGBService igbService, String[] server_urls) {
		GenericServer[] gServers = new GenericServer[server_urls.length];

		for (int i = 0; i < server_urls.length; i++) {
			String server_url = server_urls[i];
			gServers[i] = igbService.loadServer(server_url);
		}

		return gServers;
	}

	private void loadDataFromURLs(final IGBService igbService, final String[] data_urls, final String[] extensions, final String[] tier_names) {
		try {
			if (data_urls != null && data_urls.length != 0) {
				URL[] urls = new URL[data_urls.length];
				for (int i = 0; i < data_urls.length; i++) {
					urls[i] = new URL(data_urls[i]);
				}
				final UrlLoaderThread t = new UrlLoaderThread(igbService, urls, extensions, tier_names);
				t.runEventually();
				t.join();
			}
		} catch (MalformedURLException e) {
			ErrorHandler.errorPanel("Error loading bookmark\nData URL malformed\n", e);
		} catch (InterruptedException ex) {
		}
	}

	private int[] parseValues(String start_param, String end_param,
			String select_start_param, String select_end_param)
			throws NumberFormatException {

		int start = 0;
		int end = 0;
		if (start_param == null || start_param.length() == 0) {
			Logger.getLogger(BookmarkUnibrowControlServlet.class.getName()).log(Level.WARNING,
					"No start value found in the bookmark URL. Setting start={0}", start);
		} else {
			start = Integer.parseInt(start_param);
		}
		if (end_param == null || end_param.length() == 0) {
			end = start + 100000;
			Logger.getLogger(BookmarkUnibrowControlServlet.class.getName()).log(Level.WARNING,
					"No end value found in the bookmark URL. Setting end={0}", end);
		} else {
			end = Integer.parseInt(end_param);
		}
		int selstart = -1;
		int selend = -1;
		if (select_start_param != null && select_end_param != null && select_start_param.length() > 0 && select_end_param.length() > 0) {
			selstart = Integer.parseInt(select_start_param);
			selend = Integer.parseInt(select_end_param);
		}
		return new int[]{start, end, selstart, selend};
	}
	

	/** Loads the sequence and goes to the specified location.
	 *  If version doesn't match the currently-loaded version,
	 *  asks the user if it is ok to proceed.
	 *  NOTE:  This schedules events on the AWT event queue.  If you want
	 *  to make sure that everything has finished before you do something
	 *  else, then you have to schedule that something else to occur
	 *  on the AWT event queue.
	 *  @param graph_files it is ok for this parameter to be null.
	 *  @return true indicates that the action succeeded
	 */
	private BioSeq goToBookmark(final IGBService igbService, final String seqid, final String version, int start, int end) {
		
		final AnnotatedSeqGroup book_group = igbService.determineAndSetGroup(version);
		if (book_group == null) {
			ErrorHandler.errorPanel("Bookmark genome version seq group '" + version + "' not found.\n"
					+ "You may need to choose a different server.");
			return null; // cancel
		}

		final BioSeq book_seq = determineSeq(seqid, book_group);
		if (book_seq == null) {
			ErrorHandler.errorPanel("No seqid", "The bookmark did not specify a valid seqid: specified '" + seqid + "'");
			return null;
		} else {
			// gmodel.setSelectedSeq() should trigger a gviewer.setAnnotatedSeq() since
			//     gviewer is registered as a SeqSelectionListener on gmodel
			if (book_seq != gmodel.getSelectedSeq()) {
				gmodel.setSelectedSeq(book_seq);
			}
		}
		igbService.getSeqMapView().setRegion(start, end, book_seq);

		return book_seq;
	}

	public static BioSeq determineSeq(String seqid, AnnotatedSeqGroup group) {
		// hopefully setting gmodel's selected seq group above triggered population of seqs
		//   for group if not already populated
		BioSeq book_seq;
		if (seqid == null || "unknown".equals(seqid) || seqid.trim().length() == 0) {
			book_seq = gmodel.getSelectedSeq();
			if (book_seq == null && gmodel.getSelectedSeqGroup().getSeqCount() > 0) {
				book_seq = gmodel.getSelectedSeqGroup().getSeq(0);
			}
		} else {
			book_seq = group.getSeq(seqid);
		}
		return book_seq;
	}
}
