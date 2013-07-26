package com.affymetrix.igb.prefs;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.PreferenceUtils;


/**
 *
 * @version $Id: WebLink.java 11425 2012-05-04 15:54:04Z lfrohman $
 */
public final class WebLink {
	private static final String separator = System.getProperty("line.separator");
	
	// TYPE is feature name, ID is annotation ID
	public enum RegexType {

		TYPE, ID
	};
	private static final boolean DEBUG = false;
	private String url = null;
	private String name = "";
	private String species = "";
	private String id_field_name = null; // null implies use getId(); "xxx" means use getProperty("xxx");
	private String original_regex = null;
	private String type = null; // server or local source
	private RegexType regexType = RegexType.TYPE;	// matching on type or id
	
	private Pattern pattern = null;
	private static final List<WebLink> local_weblink_list = new ArrayList<WebLink>();
	private static final List<WebLink> server_weblink_list = new ArrayList<WebLink>();
	
	public static final String LOCAL = "local";
	private static final Pattern DOUBLE_DOLLAR_PATTERN = Pattern.compile("[$][$]");	//A pattern that matches the string "$$"
	private static final Pattern DOLLAR_GENOME_PATTERN = Pattern.compile("[$][:]genome[:][$]");	// A pattern that matches the string "$:genome:$"

	public WebLink() {
	}

	private WebLink(String name, String regex, String url, RegexType regexType) throws PatternSyntaxException {
		this();
		setName(name);
		setRegex(regex);
		setUrl(url);
		setRegexType(regexType);
	}

	/** Used to compute the hashCode and in the equals() method. */
	private String toComparisonString() {
		// Do NOT consider the "name" in tests of equality.
		// We do not want to allow two links that are identical except for name.
		// This is important in allowing users to over-ride the default links.
		return original_regex + ", " + url.toString() + ", " + id_field_name;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof WebLink)) {
			return false;
		}
		WebLink w = (WebLink) o;
		return toComparisonString().equals(w.toComparisonString());
	}

	@Override
	public int hashCode() {
		return toComparisonString().hashCode();
	}

	public static void addWebLink(WebLink wl) {
		if (wl.getName() == null || wl.getName().trim().length() == 0) {
			return;
		}

		if (!wl.getType().equals(LOCAL)) {
			if (!isContained(server_weblink_list, wl)) {
				server_weblink_list.add(wl);
			}
		} else {
			local_weblink_list.add(wl);
		}
	}

	public static void sortList() {
		Collections.sort(server_weblink_list, webLinkComp);
		Collections.sort(local_weblink_list, webLinkComp);
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
			return wl.name + ", " + wl.original_regex + ", " + wl.url.toString() + ", " + wl.id_field_name;
		}

		public int compare(WebLink o1, WebLink o2) {
			return (sortString(o1).compareTo(sortString(o2)));
		}
	};

	/**
	 *  Remove a WebLink from the static list.
	 */
	public static void removeLocalWebLink(WebLink link) {
		Iterator<WebLink> it = local_weblink_list.iterator();
		while (it.hasNext()) {
			WebLink item = it.next();
			if (link == item) {
				it.remove();
			}
		}
	}

	/** Get all web-link patterns for the given method name.
	 *  These can come from regular-expression matching from the semi-obsolete
	 *  XML-based preferences file, or from UCSC-style track lines in the
	 *  input files.  It is entirely possible that some of the WebLinks in the
	 *  array will have the same regular expression or point to the same URL.
	 *  You may want to filter-out such duplicate results.
	 */
	public static List<WebLink> getWebLinks(SeqSymmetry sym) {
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
			WebLink link = new WebLink("Track Line URL", null, style_url, RegexType.TYPE);
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

	private static List<WebLink> getWebLink(SeqSymmetry sym, String method) {
		List<WebLink> results = new ArrayList<WebLink>();
		List<WebLink> temp = new ArrayList<WebLink>();
		temp.addAll(server_weblink_list);
		temp.addAll(local_weblink_list);

		for (WebLink link : temp) {
			if (link.url == null) {
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

			if (link.regexType == RegexType.TYPE && link.matches(method)) {
				if (DEBUG) {
					System.out.println("link " + link + " matches method.");
				}
				results.add(link);
			} else if (link.regexType == RegexType.ID && link.matches(sym.getID())) {
				if (DEBUG) {
					System.out.println("link " + link + " matches ID.");
				}
				results.add(link);
			}
		}

		return results;
	}

	public static List<WebLink> getServerWebList() {
		return server_weblink_list;
	}

	public static List<WebLink> getLocalWebList() {
		return local_weblink_list;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		if (name == null || "null".equals(name)) {
			this.name = "";
		} else {
			this.name = name;
		}
	}

	public void setIDField(String IDField) {
		this.id_field_name = IDField;
	}

	public String getIDField() {
		return this.id_field_name;
	}

	public String getSpeciesName() {
		return this.species;
	}

	public void setSpeciesName(String name) {
		if (name == null || "null".equals(name)) {
			this.species = "";
		} else {
			this.species = name;
		}
	}

	public String getRegex() {
		return original_regex;
	}

	/** Sets the regular expression that must be matched.
	 *  The special value <b>null</b> is also allowed, and matches every String.
	 *  If the Regex does not begin with "(?i)", then this will be pre-pended
	 *  automatically to generate a case-insensitive pattern.  If you want a
	 *  case-sensitive pattern, start your regex with "(?-i)" and this will
	 *  cancel-out the effect of the "(?i)" flag.
	 */
	public void setRegex(String regex) throws PatternSyntaxException {
		if (regex == null || ".*".equals(regex) || "(?i).*".equals(regex)) {
			pattern = null;
			original_regex = regex;
			return;
		}

		// delete any double, triple, etc., "(?i)" strings caused by a bug in a previous version
		while (regex.startsWith("(?i)(?i)")) {
			regex = regex.substring(4);
		}
		if (!regex.startsWith("(?i)") && !type.equals(LOCAL)) {
			regex = "(?i)" + regex; // force all server web link matches to be case-insensitive
		}
		original_regex = regex;
		pattern = Pattern.compile(regex);

	}

	/** Return the compiled form of the regular expression. */
	public Pattern getPattern() {
		return pattern;
	}

	/** Returns the URL (or URL pattern) associated with this WebLink.
	 *  If the URL pattern contains any "$$" characters, those should be
	 *  replaced with URL-Encoded annotation IDs to get the final URL.
	 *  Better to use {@link #getURLForSym(SeqSymmetry)}.
	 */
	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setRegexType(RegexType regexType) {
		this.regexType = regexType;
	}

	public RegexType getRegexType() {
		return this.regexType;
	}

	private boolean matches(String s) {
		return (pattern == null
				|| pattern.matcher(s).matches());
	}

	/**
	 * replace all "$$" in the url pattern with the given id, URLEncoded
	 * @param url
	 * @param id
	 * @return url
	 */
	public static String replacePlaceholderWithId(String url, String id) {
		if (url == null || id == null) {
			return url;
		}
		String encoded_id = "";
		try {
			encoded_id = URLEncoder.encode(id, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(WebLink.class.getName()).log(Level.SEVERE, null, ex);
		}
		url = DOUBLE_DOLLAR_PATTERN.matcher(url).replaceAll(encoded_id);

		return url;
	}

	/**
	 * replace all "$:genome:$" in the url pattern with the current seqGroup id, URLEncoded
	 * @param url
	 * @return url
	 */
	public static String replaceGenomeId(String url) {
		if (url == null) {
			return url;
		}
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		if (group != null) {
			String encoded_id = "";
			try {
				encoded_id = URLEncoder.encode(group.getID(), "UTF-8");
			} catch (UnsupportedEncodingException ex) {
				Logger.getLogger(WebLink.class.getName()).log(Level.SEVERE, null, ex);
			}
			url = DOLLAR_GENOME_PATTERN.matcher(url).replaceAll(encoded_id);
		}

		return url;
	}

	public String getURLForSym(SeqSymmetry sym) {
		String url2 = getURLForSym_(sym);
		return replaceGenomeId(url2);
	}

	private String getURLForSym_(SeqSymmetry sym) {
		// Currently this just replaces any "$$" with the ID, but it could
		// do something more sophisticated later, like replace "$$" with
		// some other sym property.
		if (id_field_name == null) {
			return replacePlaceholderWithId(getUrl(), sym.getID());
		}

		Object field_value = null;
		if (sym instanceof SymWithProps) {
			field_value = ((SymWithProps) sym).getProperty(id_field_name);
		}

		if (field_value == null) {
			Logger.getLogger(WebLink.class.getName()).log(Level.WARNING,
					"Selected item has no value for property ''{0}'' which is needed to construct the web link.", id_field_name);
			return replacePlaceholderWithId(getUrl(), "");
		}
		return replacePlaceholderWithId(getUrl(), field_value.toString());
	}

	@Override
	public String toString() {
		return "WebLink: name=" + name
				+ ", regex=" + getRegex()
				+ ", regexType=" + this.regexType.toString()
				+ ", url=" + url
				+ ", id_field_name=" + id_field_name;
	}

	public String toXML() {
		String annotRegexString = (this.regexType == RegexType.TYPE) ? "annot_type_regex" : "annot_id_regex";

		StringBuilder sb = new StringBuilder();
		sb.append("<annotation_url ").append(separator);
		sb.append(" ").append(annotRegexString).append("=\"").
				append(escapeXML(getRegex() == null ? ".*" : getRegex())).append("\"").append(separator);
		sb.append(" name=\"").append(escapeXML(name)).
				append("\"").append(separator).append(" species=\"").
				append(escapeXML(species)).append("\"").append(separator);
		if (this.id_field_name != null) {
			sb.append(" id_field=\"").append(escapeXML(id_field_name)).
					append("\"").append(separator);
		}
		sb.append(" url=\"").append(escapeXML(url)).append("\"").append(separator);
		sb.append(" type=\"").append(escapeXML(type)).append("\"").append(separator).append("/>");
		return sb.toString();
	}

	private static String escapeXML(String s) {
		if (s == null) {
			return null;
		}
		return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt").replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
	}

}
