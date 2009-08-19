package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.parsers.XmlPrefsParser;
import com.affymetrix.igb.tiers.AnnotStyle;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.affymetrix.igb.IGBConstants.UTF8;

public final class WebLink {

	public enum RegexType { TYPE, ID };
	private static final boolean DEBUG = false;
	private String url = null;
	private String name = "";
	private String id_field_name = null; // null implies use getId(); "xxx" means use getProperty("xxx");
	private String original_regex = null;
	private RegexType regexType = RegexType.TYPE;	// matching on type or id
	private static final String separator = System.getProperty("line.separator");
	private Pattern pattern = null;
	private static final List<WebLink> weblink_list = new ArrayList<WebLink>();
	private static final String FILE_NAME = "weblinks.xml";	// Name of the xml file used to store the web links data.
	private static final Pattern DOUBLE_DOLLAR_PATTERN = Pattern.compile("[$][$]");	//A pattern that matches the string "$$"
	private static final Pattern DOLLAR_GENOME_PATTERN = Pattern.compile("[$][:]genome[:][$]");	// A pattern that matches the string "$:genome:$"

	public WebLink() {
	}

	public WebLink(String name, String regex, String url, RegexType regexType) throws PatternSyntaxException {
		this();
		setName(name);
		setRegex(regex);
		setUrl(url);
		setRegexType(regexType);
	}

	/** Used to compute the hashCode and in the equals() method. */
	String toComparisonString() {
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

	/**
	 *  A a WebLink to the static list.  Multiple WebLinks with the same
	 *  regular expressions are allowed, as long as they have different URLs.
	 *  WebLinks that differ only in name are not allowed; the one added last
	 *  will be the one that is kept, unless the one added first had a name and
	 *  the one added later does not.
	 */
	public static void addWebLink(WebLink wl) {
		int index = weblink_list.indexOf(wl);
		if (index >= 0) {
			if (wl.getName() == null || wl.getName().trim().length() == 0) {
				//Application.getSingleton().logInfo("Not adding duplicate web link for regex: '" + wl.getRegex() + "'");
			} else {
				//Application.getSingleton().logInfo("---------- Renaming Web Link To: " + wl.getName());
				weblink_list.remove(index);
				weblink_list.add(wl);
			}
		} else {
			weblink_list.add(wl);
		}
		Collections.sort(weblink_list, webLinkComp);
	}

	static Comparator<WebLink> webLinkComp = new Comparator<WebLink>() {

		String sortString(WebLink wl) {
			return wl.name + ", " + wl.original_regex + ", " + wl.url.toString() + ", " + wl.id_field_name;
		}

		public int compare(WebLink o1, WebLink o2) {
			return (sortString(o1).compareTo(sortString(o2)));
		}
	};

	/**
	 *  Remove a WebLink from the static list.
	 */
	public static void removeWebLink(WebLink wl) {
		weblink_list.remove(wl);
	}

	/** Get all web-link patterns for the given method name.
	 *  These can come from regular-expression matching from the semi-obsolete
	 *  XML-based preferences file, or from UCSC-style track lines in the
	 *  input files.  It is entirely possible that some of the WebLinks in the
	 *  array will have the same regular expression or point to the same URL.
	 *  You may want to filter-out such duplicate results.
	 */
	public static List<WebLink> getWebLinks(String method, String ID) {
		if (method == null) { // rarely happens, but can
			return Collections.<WebLink>emptyList();
		}

		ArrayList<WebLink> results = new ArrayList<WebLink>();

		// If the method name has already been used, then the annotStyle must have already been created
		AnnotStyle style = AnnotStyle.getInstance(method, false);
		String style_url = style.getUrl();
		if (style_url != null && style_url.length() > 0) {
			WebLink link = new WebLink("Track Line URL", null, style_url, RegexType.TYPE);
			results.add(link);
		}

		if (method != null) {
			if (DEBUG) {
				System.out.println("method is : " + method);
			}
			for (WebLink link : weblink_list) {
				if (link.url != null && link.matches(method)) {
					if (DEBUG) {
						System.out.println("link " + link + " matches. ");
					}
					results.add(link);
				}
			}
		}

		return results;
	}

	/** Returns the list of WebLink items. */
	public static List<WebLink> getWebList() {
		return weblink_list;
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
			original_regex = null;
			return;
		}

		// delete any double, triple, etc., "(?i)" strings caused by a bug in a previous version
		while (regex.startsWith("(?i)(?i)")) {
			regex = regex.substring(4);
		}
		if (!regex.startsWith("(?i)")) {
			regex = "(?i)" + regex; // force all web link matches to be case-insensitive
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

	public void setRegexType(RegexType regexType) {
		this.regexType = regexType;
	}

	private boolean matches(String s) {
		return (pattern == null ||
						pattern.matcher(s).matches());
	}

	/**
	 * replace all "$$" in the url pattern with the given id, URLEncoded
	 * @param url
	 * @param id
	 * @return
	 */
	public static String replacePlaceholderWithId(String url, String id) {
		if (url == null || id == null) {
			return url;
		}
		String encoded_id = "";
		try {
			encoded_id = URLEncoder.encode(id, UTF8);
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(WebLink.class.getName()).log(Level.SEVERE, null, ex);
		}
		url = DOUBLE_DOLLAR_PATTERN.matcher(url).replaceAll(encoded_id);

		return url;
	}

	/**
	 * replace all "$:genome:$" in the url pattern with the current seqGroup id, URLEncoded
	 * @param url
	 * @return
	 */
	public static String replaceGenomeId(String url) {
		if (url == null) {
			return url;
		}
		SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		if (group != null) {
			String encoded_id = "";
			try {
				encoded_id = URLEncoder.encode(group.getID(), UTF8);
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
			Application.logWarning("Selected item has no value for property '" + id_field_name +
							"' which is needed to construct the web link.");
			return replacePlaceholderWithId(getUrl(), "");
		}
		return replacePlaceholderWithId(getUrl(), field_value.toString());
	}

	@Override
	public String toString() {
		return "WebLink: name=" + name +
						", regex=" + getRegex() +
						", regexType=" + this.regexType.toString() + 
						", url=" + url +
						", id_field_name=" + id_field_name;
	}

	/**
	 *  Returns the file that is used to store the user-edited web links.
	 */
	private static File getLinksFile() {
		return new File(UnibrowPrefsUtil.getAppDataDirectory(), FILE_NAME);
	}

	public static void importWebLinks(File f) throws FileNotFoundException, IOException {
		importWebLinks(new FileInputStream(f));
	}

	public static void importWebLinks(InputStream st) throws IOException {
		// The existing XmlPrefsParser is capable of importing the web links
		XmlPrefsParser parser = new XmlPrefsParser();
		// Create a Map named "foo", which will be discarded after parsing
		parser.parse(st, "foo", new HashMap<String,Map>());
	}

	public static void exportWebLinks(File f, boolean include_warning) throws IOException {
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {

			fw = new FileWriter(f);
			bw = new BufferedWriter(fw);
			bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			bw.write(separator);
			bw.write("");
			bw.write(separator);
			bw.write("<!--");
			bw.write(separator);
			bw.write("  This file was generated by " + Application.getSingleton().getApplicationName() + " " + Application.getSingleton().getVersion() + "\n");
			bw.write(separator);
			if (include_warning) {
				bw.write("  WARNING: This file is automatically created by the application.");
				bw.write(separator);
				bw.write("  Edit the Web-Links from inside the application.");
				bw.write(separator);
			}
			bw.write("-->");
			bw.write(separator);
			bw.write("");
			bw.write(separator);
			bw.write("<prefs>");
			bw.write(separator);

			for (WebLink link : weblink_list) {
				String xml = link.toXML();
				bw.write(xml);
				bw.write(separator);
			}

			bw.write("</prefs>");
			bw.write(separator);
			bw.write(separator);
		} finally {
			GeneralUtils.safeClose(bw);
			GeneralUtils.safeClose(fw);
		}
	}

	private String toXML() {
		String annotRegexString = (this.regexType == RegexType.TYPE) ?  "annot_type_regex" : "annot_id_regex";

		StringBuffer sb = new StringBuffer();
		sb.append("<annotation_url ").append(separator);
		sb.append(" " + annotRegexString+"=\"").append(escapeXML(getRegex() == null ? ".*" : getRegex())).append("\"").append(separator);
		sb.append(" name=\"").append(escapeXML(name)).append("\"").append(separator).append(" url=\"").append(escapeXML(url)).append("\"").append(separator).append("/>");
		return sb.toString();
	}

	private static String escapeXML(String s) {
		if (s == null) {
			return null;
		}
		return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt").replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
	}

	/**
	 *  Loads links from the file specified by {@link #getLinksFile()}.
	 */
	public static void autoLoad() {
		File f = getLinksFile();
		if (f == null || !f.exists()) {
			return;
		}
		String filename = f.getAbsolutePath();
		try {
			Application.logInfo("Loading web links from file \"" + filename + "\"");

			WebLink.importWebLinks(f);
		} catch (Exception ioe) {

			Application.logError("Could not load web links from file \"" + filename + "\"");
		}
	}

	/** Save the current web links into the file that was specified
	 *  by {@link #getLinksFile()}.
	 *  @return true for sucessfully saving the file
	 */
	public static boolean autoSave() {
		File f = getLinksFile();
		String filename = f.getAbsolutePath();
		try {
			Application.logInfo("Saving web links to file \"" + filename + "\"");
			File parent_dir = f.getParentFile();
			if (parent_dir != null) {
				parent_dir.mkdirs();
			}
			WebLink.exportWebLinks(f, true);
			return true;
		} catch (IOException ioe) {
			Application.logError("Error while saving web links to \"" + filename + "\"");
		}
		return false;
	}
}
