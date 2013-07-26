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
package com.affymetrix.igb.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.genometryImpl.util.ServerUtils;

import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.prefs.WebLinkList;

/**
 *Class for parsing preferences for IGB.
 *<pre>
Description of <annotation_style> element format

---------------------------------------------------------------------

One form of type attribute is required, possible attribute names are:
annot_type="val"
annot_type_starts_with="val"
annot_type_ends_with="val"
annot_type_regex="val"
There can also be any combination of these type attributes.

So the simplest <annotation_style> example possible would be:
<annotation_style annot_type="xyz" />

Although note that the above example is not very useful without using some
of the options discussed below.  Because if an annotation is loaded that has no
<annotation_style> entry in the prefs file(s), then a default annotation style
is automatically assigned, which has the same effect as the above
<annotation_style> entry...

All other attributes and sub-elements are optional, and are either
unnecessary or automatically assigned a default if not present.

Optionally specifying the factory class:
The "factory" attribute specifies a class to be instantiated
as the glyph factory to "glyphify" annotations of the given annotation type.
Other attributes of an <annotation_style> element are passed to the factory
in an initialization step as a Map with key/value pairs of form
{ attribute_name ==> attribute_value }, and it is up to the the specific
factory implementation to decide what to do with this information.  This
means that there are no restrictions on the attribute names in the
<annotation_style> element, as different factories may recognize different
attributes.
Example:
<annotation_style annot_type="abc" factory="com.affymetrix.igb.glyph.GenericGraphGlyphFactory" />

The usual default factory is the GenericAnnotGlyphFactory.
Attributes that GenericAnnotGlyphFactory recognizes currently include:

"child glyph": This attribute specifies what glyph to use to render
the (visible) leaf spans of the annotation
"parent_glyph": This attribute specifies what glyph to use to connect
the child glyphs
Example:
<annotation_style annot_type="test2"
parent_glyph="com.affymetrix.igb.glyph.EfficientOutlineContGlyph"
child_glyph="com.affymetrix.igb.glyph.EfficientFillRectGlyph"  />
 *</pre>
 *
 * @version $Id: XmlPrefsParser.java 10766 2012-03-15 22:38:43Z lfrohman $
 */
public final class XmlPrefsParser {
	private static final boolean DEBUG = false;
	
	private XmlPrefsParser() {
	}

	public static void parse(String url) throws IOException {
		InputStream stream = null;
		try {
			// Don't cache.  Don't warn user if the synonyms file doesn't exist.  Do not allow html for preferences.xml
			stream = LocalUrlCacher.getInputStream(url, true, null, true, false);
			if (stream == null) {
				return;
			}
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
					"Preferences found at: {0}", url);
			parse(stream);
		} catch (IOException ioe) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING, "Unable to load preferences from '" + url + "'", ioe);
		} finally {
			GeneralUtils.safeClose(stream);
		}
	}
	
	public static void parse(InputStream istr) throws IOException {
		InputSource insource = new InputSource(istr);

		try {
			Document prefsdoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(insource);
			processDocument(prefsdoc);
		} catch (ParserConfigurationException ex) {
			throw new IOException(ex);
		} catch (SAXException ex) {
			throw new IOException(ex);
		}
	}

	private static void processDocument(Document prefsdoc) {
		Element top_element = prefsdoc.getDocumentElement();
		String topname = top_element.getTagName();
		if (!(topname.equalsIgnoreCase("prefs"))) {
			System.err.println("not a prefs file -- can't parse in prefs!");
		}
		NodeList children = top_element.getChildNodes();
		Node child;
		String name;
		Element el;
		boolean isWebLinkXML = false;

		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			name = child.getNodeName();
			if (child instanceof Element) {
				el = (Element) child;
				if (name.equalsIgnoreCase("annotation_url")) {
					isWebLinkXML = true;
					processLinkUrl(el);
				} else if (name.equalsIgnoreCase("server")) {
					processServer(el, ServerList.getServerInstance(), getServerType(el.getAttribute("type")));
				} else if (name.equalsIgnoreCase("repository")) {
					processServer(el, ServerList.getRepositoryInstance(), null);
				}
			}
		}

		if (isWebLinkXML) {
			WebLinkList.getServerList().sortList();
			WebLinkList.getLocalList().sortList();
		}
	}

	private static void processServer(Element el, ServerList serverList, ServerTypeI server_type) {
		String server_name = el.getAttribute("name");
		String server_url = el.getAttribute("url");
		String mirror_url = el.getAttribute("mirror"); //qlmirror
		String en = el.getAttribute("enabled");
		String orderString = el.getAttribute("order");
		Integer order = orderString == null || orderString.isEmpty() ? 0 : Integer.valueOf(orderString);
		Boolean enabled = en == null || en.isEmpty() ? true : Boolean.valueOf(en);
		String pr = el.getAttribute("primary");
		Boolean primary = pr == null || pr.isEmpty() ? false : Boolean.valueOf(pr);
		String d = el.getAttribute("default");
		Boolean isDefault = d == null || d.isEmpty() ? false : Boolean.valueOf(d);
		
		if (DEBUG) {
			System.out.println("XmlPrefsParser adding " + server_type 
					+ " server: " + server_name + ",  " + server_url + " mirror: " + mirror_url
					+ ", enabled: " + enabled + "default: " + isDefault);
		}
		serverList.addServer(server_type, server_name, server_url, 
				enabled, primary, order.intValue(), isDefault, mirror_url); //qlmirror
	}

	private static ServerTypeI getServerType(String type) {
		for (ServerTypeI t : ServerUtils.getServerTypes()) {
			if (type.equalsIgnoreCase(t.getName())) {
				return t;
			}
		}
		return ServerTypeI.DEFAULT;
	}

	/**
	 *  Sets up a regular-expression matching between a method name or id and a url,
	 *  which can be used, for example, in SeqMapView to "get more info" about
	 *  an item.
	 *  For example:
	 *  <p>
	 *  <code>&gt;annotation_url annot_type_regex="google" match_case="false" url="http://www.google.com/search?q=$$" /&lt;</code>
	 * <code>&gt;annotation_url annot_id_regex="^AT*" match_case="false" url="http://www.google.com/search?q=$$" /&lt;</code>
	 *  <p>
	 *  Note that the url can contain "$$" which will later be substituted with the
	 *  "id" of the annotation to form a link.
	 *  By default, match is case-insensitive;  use match_case="true" if you want
	 *  to require an exact match.
	 */
	private static void processLinkUrl(Element el) {
		Map<String, Object> attmap = XmlPrefsParser.getAttributeMap(el);
		String url = (String)attmap.get("url");
		if (url == null || url.trim().length() == 0) {
			System.out.println("ERROR: Empty data in preferences file for an 'annotation_url':" + el.toString());
			return;
		}

		WebLink.RegexType type_regex = WebLink.RegexType.TYPE;
		String annot_regex_string = (String)attmap.get("annot_type_regex");
		if (annot_regex_string == null || annot_regex_string.trim().length() == 0) {
			type_regex = WebLink.RegexType.ID;
			annot_regex_string = (String)attmap.get("annot_id_regex");
		}
		if (annot_regex_string == null || annot_regex_string.trim().length() == 0) {
			System.out.println("ERROR: Empty data in preferences file for an 'annotation_url':" + el.toString());
			return;
		}

		String name = (String)attmap.get("name");
		String species = (String)attmap.get("species");
		String IDField = (String)attmap.get("id_field");
		String type = (String)attmap.get("type");
		if (type == null) {
			type = WebLink.LOCAL;
		}
		WebLink link = new WebLink();
		link.setRegexType(type_regex);
		link.setName(name);
		link.setIDField(IDField);
		link.setUrl(url);
		link.setType(type);
		link.setSpeciesName(species);	
		try {
			if ("false".equalsIgnoreCase((String)attmap.get("match_case"))) {
				link.setRegex("(?-i)" + annot_regex_string);
			} else {
				link.setRegex(annot_regex_string);
			}
		} catch (PatternSyntaxException pse) {
			System.out.println("ERROR: Regular expression syntax error in preferences\n" + pse.getMessage());
		}
		WebLinkList.getWebLinkList(type).addWebLink(link);
	}

	private static Map<String, Object> getAttributeMap(Element el) {
		HashMap<String, Object> amap = new HashMap<String, Object>();
		NamedNodeMap atts = el.getAttributes();
		int attcount = atts.getLength();
		for (int i = 0; i < attcount; i++) {
			Attr att = (Attr) atts.item(i);
			String tag = att.getName();
			String val = att.getValue();
			amap.put(tag, val);
		}
		return amap;
	}
}
