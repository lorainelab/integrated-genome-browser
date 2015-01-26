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

import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.igb.general.ServerList.RepositoryElementHandler;
import com.affymetrix.igb.general.ServerList.ServerElementHandler;
import com.affymetrix.igb.prefs.WebLinkUtils.WeblinkElementHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
		
		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			name = child.getNodeName();
			if (child instanceof Element) {
				el = (Element) child;
				if (name.equalsIgnoreCase("annotation_url")) {
					(new WeblinkElementHandler()).processElement(el);
				} else if (name.equalsIgnoreCase("server")) {
					(new ServerElementHandler()).processElement(el);
				} else if (name.equalsIgnoreCase("repository")) {
					(new RepositoryElementHandler()).processElement(el);
				}
			}
		}
	}
	
	public static interface ElementHandler {
		public void processElement(Element el);
		public String getElementTag();
	}
}
