package com.affymetrix.igb.prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.SpeciesLookup;

/**
 *
 * @author hiralv
 */
public class WebLinkList {
	
	private static final WebLinkList SERVER_WEBLINK_LIST = new WebLinkList("default", false);
	private static final WebLinkList LOCAL_WEBLINK_LIST = new WebLinkList("local", true);
	
	private static final boolean DEBUG = false;
	
	private final String type;
	private final boolean allowDuplicates;
	private final List<WebLink> weblink_list;
	
	public static WebLinkList getWebLinkListForType(String type){
		if(SERVER_WEBLINK_LIST.getType().equalsIgnoreCase(type)){
			return SERVER_WEBLINK_LIST;
		}
		return LOCAL_WEBLINK_LIST;
	}
	
	public static WebLinkList getServerWebLinkList(){
		return SERVER_WEBLINK_LIST;
	}
	
	public static WebLinkList getLocalWebLinkList(){
		return LOCAL_WEBLINK_LIST;
	}
	
	private WebLinkList(String type, boolean allowDuplicates){
		this.type = type;
		this.allowDuplicates = allowDuplicates;
		this.weblink_list	 = new ArrayList<WebLink>();
	}
	
	public String getType(){
		return type;
	}
	
	public List<WebLink> getWebList() {
		return weblink_list;
	}
	
	public void addWebLink(WebLink wl) {
		if (wl.getName() == null || wl.getName().trim().length() == 0) {
			return;
		}
		//TODO: Allow duplicates for local
		if(allowDuplicates){
			weblink_list.add(wl);
		} else if (!isContained(weblink_list, wl)) {
			weblink_list.add(wl);
		}
	}
	
		/**
	 *  Remove a WebLink from the static list.
	 */
	public void removeWebLink(WebLink link) {
		Iterator<WebLink> it = weblink_list.iterator();
		while (it.hasNext()) {
			WebLink item = it.next();
			if (link == item) {
				it.remove();
			}
		}
	}
	
	public void sortList() {
		Collections.sort(weblink_list, webLinkComp);
	}
	
	
	/** Get all web-link patterns for the given method name.
	 *  These can come from regular-expression matching from the semi-obsolete
	 *  XML-based preferences file, or from UCSC-style track lines in the
	 *  input files.  It is entirely possible that some of the WebLinks in the
	 *  array will have the same regular expression or point to the same URL.
	 *  You may want to filter-out such duplicate results.
	 */
	public List<WebLink> getWebLinks(SeqSymmetry sym) {
		// Most links come from matching the tier name (i.e. method)
		// to a regular expression.
		String method = BioSeq.determineMethod(sym);
		if (method == null) { // rarely happens, but can
			return Collections.<WebLink>emptyList();
		}

		List<WebLink> results = new ArrayList<WebLink>();

		// If the method name has already been used, then the annotStyle must have already been created
		ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method);
		String style_url = style.getUrl();
		if (style_url != null && style_url.length() > 0) {
			WebLink link = new WebLink("Track Line URL", null, style_url, WebLink.RegexType.TYPE);
			results.add(link);
		}

		if (DEBUG) {
			System.out.println("method is : " + method);
			System.out.println("ID is : " + sym.getID());
		}

		Set<WebLink> webLinks = new HashSet<WebLink>();
		webLinks.addAll(getWebLink(sym, method));

	//	if (webLinks.isEmpty()) {
			if (style.getFeature() != null) {
				webLinks.addAll(getWebLink(sym, style.getFeature().featureName));
			}
	//	}

		results.addAll(webLinks);
		
		Collections.sort(results, webLinkComp);
		return results;
	}

	private List<WebLink> getWebLink(SeqSymmetry sym, String method) {
		List<WebLink> results = new ArrayList<WebLink>();
		List<WebLink> temp = new ArrayList<WebLink>();

		for (WebLink link : weblink_list) {
			if (link.getUrl() == null) {
				continue;
			}
			if (link.getSpeciesName().length() > 0) {
				String current_version = GenometryModel.getGenometryModel().getSelectedSeqGroup().getID();
				String current_species = SpeciesLookup.getSpeciesName(current_version);
				boolean isSynonym = SpeciesLookup.isSynonym(current_species, link.getSpeciesName());
				if (!isSynonym) {
					continue;
				}
			}
			if (link.getIDField() != null) {
				// Allow matching of arbitrary id_field

				if (!(sym instanceof SymWithProps)) {
					continue;
				}
				String property = (String) ((SymWithProps) sym).getProperty(link.getIDField());
				if (property != null && link.matches(property)) {
					if (DEBUG) {
						System.out.println("link " + link + " matches property:" + property);
					}
					results.add(link);
				}
				continue;
			}

			if (link.getRegexType() == WebLink.RegexType.TYPE && link.matches(method)) {
				if (DEBUG) {
					System.out.println("link " + link + " matches method.");
				}
				results.add(link);
			} else if (link.getRegexType() == WebLink.RegexType.ID && link.matches(sym.getID())) {
				if (DEBUG) {
					System.out.println("link " + link + " matches ID.");
				}
				results.add(link);
			}
		}

		return results;
	}
	
	private static boolean isContained(List<WebLink> list, WebLink link) {
		for (WebLink l : list) {
			if (l.getName().equals(link.getName())
					&& l.getUrl().equals(link.getUrl())
					&& l.getRegex().equals(link.getRegex())) {
				return true;
			}
		}

		return false;
	}
	
	private static Comparator<WebLink> webLinkComp = new Comparator<WebLink>() {

		private String sortString(WebLink wl) {
			return wl.getName() + ", " + wl.getRegex() + ", " + wl.getUrl() + ", " + wl.getIDField();
		}

		public int compare(WebLink o1, WebLink o2) {
			return (sortString(o1).compareTo(sortString(o2)));
		}
	};
}
