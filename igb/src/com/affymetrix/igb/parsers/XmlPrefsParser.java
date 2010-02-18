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

import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.igb.IGBConstants;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.awt.Color;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;

import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.view.PluginInfo;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.glyph.MapViewGlyphFactoryI;

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

Optionally specifying color:
(Colors <b>IGNORED</b> by all standard glyph factories starting with IGB version 4.01)
Method 1: specify single color for this annot type by adding red, green, blue attributes:
<annotation_style annot_type="test" red="200" green="0" blue="200" />
Method 2: specify multiple colors to be used for this annot type by adding <color> sub-elements
to the <annotation_style> element:
<annotation_style annot_type="test">
<color name="parent_color" red="255" green="255" blue="255"/>
<color name="child_color" red="200" green="255" blue="100"/>
</annotation_style>

The name attribute value for a color element should _not_ match any attribute id
in the annotation_style. Colors specified this way will be passed to the glyph factory
used for this annotation style as part of the initialization hash, as entries of form
{ "name_value" ==> new Color(redval, greenval, blueval) }  It is up to the glyph
factory to decide how to use these colors, based on their names.  If red/green/blue
attributes are included in <annotation_style> element (Method 1), the resulting Color
is also added to the hash with key = "color".

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

"glyph_depth":
<b>IGNORED</b> starting with IGB 4.01.

 *</pre>
 * @version $Id$
 */
public final class XmlPrefsParser {

	private static final Class<?> default_factory_class =
			com.affymetrix.igb.glyph.GenericAnnotGlyphFactory.class;
	private static final Set<PluginInfo> plugins = new LinkedHashSet<PluginInfo>();

	private XmlPrefsParser() {
	}

	public static void parse(InputStream istr) {
		try {
			InputSource insrc = new InputSource(istr);
			parse(insrc);
		} catch (Exception ex) {
			System.err.println("ERROR while reading preferences");
			System.err.println("  " + ex.toString());
			ex.printStackTrace();
		}
	}

	private static void parse(InputSource insource) {
		try {
			Document prefsdoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(insource);
			processDocument(prefsdoc);
		} catch (Exception ex) {
			System.err.println("ERROR while reading preferences");
			System.err.println("  " + ex.toString());
			ex.printStackTrace();
		}
	}

	public static Set<PluginInfo> getPlugins() {
		return Collections.<PluginInfo>unmodifiableSet(plugins);
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

		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			name = child.getNodeName();
			if (child instanceof Element) {
				el = (Element) child;
				if (name.equalsIgnoreCase("annotation_style")) {
					processAnnotStyle(el);
				} else if (name.equalsIgnoreCase("annotation_url")) {
					processLinkUrl(el);
				} else if (name.equalsIgnoreCase("plugin")) {
					processPlugin(el);
				} else if (name.equalsIgnoreCase("server")) {
					String server_type = el.getAttribute("type").toLowerCase();
					String server_name = el.getAttribute("name");
					String server_url = el.getAttribute("url");
					if (IGBConstants.DEBUG) {
						System.out.println("XmlPrefsParser adding " + server_type + " server: " + server_name + ",  " + server_url);
					}
					if (server_type.equalsIgnoreCase(ServerType.DAS.toString())) {
						ServerList.addServer(ServerType.DAS, server_name, server_url);
					} else if (server_type.equalsIgnoreCase(ServerType.DAS2.toString())) {
						ServerList.addServer(ServerType.DAS2, server_name, server_url);
					} else if (server_type.equalsIgnoreCase(ServerType.QuickLoad.toString())) {
						ServerList.addServer(ServerType.QuickLoad, server_name, server_url);
					}
				}
			}
		}
	}

	private static void processPlugin(Element el) {
		String loadstr = el.getAttribute("load");
		String plugin_name = el.getAttribute("name");
		String class_name = el.getAttribute("class");
		//String description = el.getAttribute("description");
		//String info_url = el.getAttribute("info_url");
		boolean load = (loadstr == null ? true : (!loadstr.equalsIgnoreCase("false")));
		if (plugin_name != null && class_name != null) {
			System.out.println("plugin, name = " + plugin_name + ", class = " + class_name);
			PluginInfo pinfo = new PluginInfo(class_name, plugin_name, load);
			plugins.add(pinfo);
		}
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
		Map<String, String> attmap = XmlPrefsParser.getAttributeMap(el);
		String url = attmap.get("url");
		if (url == null || url.trim().length() == 0) {
			System.out.println("ERROR: Empty data in preferences file for an 'annotation_url':" + el.toString());
			return;
		}

		WebLink.RegexType type_regex = WebLink.RegexType.TYPE;
		String annot_regex_string = attmap.get("annot_type_regex");
		if (annot_regex_string == null || annot_regex_string.trim().length() == 0) {
			type_regex = WebLink.RegexType.ID;
			annot_regex_string = attmap.get("annot_id_regex");
		}
		if (annot_regex_string == null || annot_regex_string.trim().length() == 0) {
			System.out.println("ERROR: Empty data in preferences file for an 'annotation_url':" + el.toString());
			return;
		}

		String name = attmap.get("name");
		try {
			WebLink link = new WebLink();
			link.setRegexType(type_regex);
			link.setName(name);
			link.setUrl(url);
			if ("false".equalsIgnoreCase(attmap.get("match_case"))) {
				link.setRegex("(?-i)" + annot_regex_string);
				//regex = Pattern.compile(annot_regex_string);
			} else {
				link.setRegex(annot_regex_string);
				//regex = Pattern.compile(annot_regex_string, Pattern.CASE_INSENSITIVE);
			}

			WebLink.addWebLink(link);
		} catch (PatternSyntaxException pse) {
			System.out.println("ERROR: Regular expression syntax error in preferences\n" + pse.getMessage());
		}
	}

	private static void processAnnotStyle(Element el) {
		/*  Builds two hash tables:
		 *  type2factory ==> hash of "annot_type" attribute mapped to MapViewGlyphFactoryI
		 *  regex2factory ==> hash of RE objects derived from "annot_starts_with",
		 *      "annot_ends_with", and "annot_regex" fields mapped to MapViewGlyphFactoryI
		 */
		Class<?> factory_class = default_factory_class;
		Map<String, String> attmap = XmlPrefsParser.getAttributeMap(el);
		// add colors
		XmlPrefsParser.addColors(el, attmap);

		// annotation_style element _must_ have and annot_type attribute
		// planning to relax this at some point to allow for element to have one (and only one) of:
		//     annot_type, annot_type_starts_with, annot_type_ends_with, annot_type_regex...
		if (attmap.get("factory") != null) {
			String factory_name = null;
			try {
				factory_name = attmap.get("factory");
				factory_class = Class.forName(factory_name);
			} catch (ClassNotFoundException ex) {
				System.out.println("ERROR: Class '" + factory_name + "' specified in the preferences file can not be found");
				factory_class = default_factory_class;
			}
		}
		try {
			MapViewGlyphFactoryI factory = (MapViewGlyphFactoryI) factory_class.newInstance();
			factory.init(attmap);
		} catch (InstantiationException ex) {
			System.out.println("ERROR: Could not instantiate a glyph factory while processing preferences file: " + factory_class);
		} catch (IllegalAccessException ex) {
			System.out.println("ERROR: Could not instantiate a glyph factory while processing preferences file: " + factory_class);
		} catch (ClassCastException ex) {
			System.out.println("ERROR: Could not instantiate a glyph factory while processing preferences file: " + factory_class + " is not an instance of MapViewGlyphFactoryI");
		} catch (Exception ex) {
			System.out.println("ERROR: Exception while parsing preferences: " + ex.toString());
		}
	}

	@SuppressWarnings("unchecked")
	private static void addColors(Element el, Map hash) {
		if (el.hasAttribute("red")) {
			int red = Integer.parseInt(el.getAttribute("red"));
			int green = Integer.parseInt(el.getAttribute("green"));
			int blue = Integer.parseInt(el.getAttribute("blue"));
			Color col = new Color(red, green, blue);
			hash.put("color", col);
		}
		/*
		else if (el.getAttribute("color")) {
		// handle color="r,g,b" attribute...
		}
		 */
		// check for color elements in child nodes of annotation_style element
		NodeList children = el.getChildNodes();
		int child_count = children.getLength();
		for (int i = 0; i < child_count; i++) {
			Node child = children.item(i);
			String name = child.getNodeName();
			if (child instanceof Element) {
				Element cel = (Element) child;
				if (name.equalsIgnoreCase("color")) {
					int red = Integer.parseInt(cel.getAttribute("red"));
					int green = Integer.parseInt(cel.getAttribute("green"));
					int blue = Integer.parseInt(cel.getAttribute("blue"));
					String color_name = cel.getAttribute("name");
					Color col = new Color(red, green, blue);
					hash.put(color_name, col);
				}
			}
		}
	}

	private static Map<String, String> getAttributeMap(Element el) {
		HashMap<String, String> amap = new HashMap<String, String>();
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

