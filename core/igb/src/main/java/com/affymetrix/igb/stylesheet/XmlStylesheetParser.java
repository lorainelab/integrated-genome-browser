/**
 * Copyright (c) 2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.stylesheet;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.XMLUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Loads an XML document using the igb_stylesheet_1.dtd.
 */
public final class XmlStylesheetParser {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(XmlStylesheetParser.class);
    private Stylesheet stylesheet = new Stylesheet();
    private static Stylesheet system_stylesheet = null;
    private static Stylesheet user_stylesheet = null;
    private static Stylesheet added_stylesheets = null;

    // This resource should in the top-level igb source directory, or top level of jar file
    private static final String system_stylesheet_resource_name = "/igb_system_stylesheet.xml";
    private static final String user_stylesheet_resource_name = "user_stylesheet.xml";
    private static final Map<String, Stylesheet> added_stylesheet_map = new HashMap<String, Stylesheet>();

    /**
     * Set the system stylesheet to null, so that the next call to
     * getSystemStylesheet() will re-load it from storage.
     */
    public static synchronized void refreshSystemStylesheet() {
        system_stylesheet = null;
    }

    public static synchronized Stylesheet getSystemStylesheet() {
        if (system_stylesheet == null) {
            InputStream istr = null;
            try {
                logger.info(
                        "Loading system stylesheet from resource:" + system_stylesheet_resource_name);
                XmlStylesheetParser parser = new XmlStylesheetParser();
                // If using class.getResource... use name beginning with "/"
                istr = XmlStylesheetParser.class.getResourceAsStream(system_stylesheet_resource_name);
                // If using getContextClassLoader... use name NOT beginning with "/"
                system_stylesheet = parser.parse(istr);
            } catch (Exception e) {
                logger.error("Couldn't initialize system stylesheet.");
                e.printStackTrace();
                system_stylesheet = null;
            } finally {
                GeneralUtils.safeClose(istr);
            }
            if (system_stylesheet == null) {
                system_stylesheet = new Stylesheet();
            }
        }
        return system_stylesheet;
    }

    /**
     * Set the user stylesheet to null, so that the next call to
     * getSystemStylesheet() will re-load it from storage.
     */
    public static synchronized void refreshUserStylesheet() {
        system_stylesheet = null;
        user_stylesheet = null;
    }

    public static synchronized File getUserStylesheetFile() {
        String app_dir = PreferenceUtils.getAppDataDirectory();
        File f = new File(app_dir, user_stylesheet_resource_name);
        return f;
    }

    public static synchronized void removeUserStylesheetFile() {
        String app_dir = PreferenceUtils.getAppDataDirectory();
        File f = new File(app_dir, user_stylesheet_resource_name);
        if (f.exists()) {
            f.delete();
        }
    }

    public static synchronized Stylesheet getUserStylesheet() {
        if (user_stylesheet == null) {
            InputStream istr = null;
            XmlStylesheetParser parser = new XmlStylesheetParser();
            try {
                File f = getUserStylesheetFile();

                if (f.exists()) {
                    istr = new FileInputStream(f);
                    logger.info(
                            "Loading user stylesheet from resource: " + user_stylesheet_resource_name);
                    user_stylesheet = parser.parse(istr);
                }
            } catch (Exception e) {
                logger.error("Couldn't initialize user stylesheet.");
                e.printStackTrace();
            } finally {
                GeneralUtils.safeClose(istr);
            }
            if (user_stylesheet == null) {
                user_stylesheet = new Stylesheet();
            }
        }
        return user_stylesheet;
    }

    /**
     * Set the added stylesheet to null, so that the next call to
     * getAddedStylesheets() will re-load it from storage.
     */
    public static synchronized void refreshAddedStylesheet() {
        added_stylesheets = null;
    }

    public static synchronized Stylesheet getAddedStylesheets() {
        if (added_stylesheets == null) {
            for (String name : added_stylesheet_map.keySet()) {
                Stylesheet stylesheet = added_stylesheet_map.get(name);
                if (added_stylesheets == null) {
                    added_stylesheets = stylesheet;
                } else {
                    added_stylesheets.merge(stylesheet);
                }
            }
            if (added_stylesheets == null) {
                added_stylesheets = new Stylesheet();
            }
        }
        return added_stylesheets;
    }

    public static java.util.Map<String, AssociationElement> getSystemFileTypeAssociation() {
        return getSystemStylesheet().filetype2association;
    }

    public static java.util.Map<String, AssociationElement> getUserFileTypeAssociation() {
        return getUserStylesheet().filetype2association;
    }

    private Stylesheet parse(InputStream istr) throws IOException {
        InputSource insrc = new InputSource(istr);
        parse(insrc);
        return stylesheet;
    }

    private Stylesheet parse(InputSource insource) throws IOException {
        try {
            Document prefsdoc = XMLUtils.nonValidatingFactory().newDocumentBuilder().parse(insource);

            processDocument(prefsdoc);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception ex) {
            throw new IOException("Error processing stylesheet file", ex);
        }
        return stylesheet;
    }

    private void processDocument(Document prefsdoc) throws IOException {

        Element top_element = prefsdoc.getDocumentElement();
        String topname = top_element.getTagName();
        if (!(topname.equalsIgnoreCase("igb_stylesheet"))) {
            throw new IOException("Can't parse file: Initial Element is not <IGB_STYLESHEET>.");
        }
        NodeList children = top_element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element el = (Element) child;

                if (name.equalsIgnoreCase("import")) {
                    processImport(el);
                } else if (name.equalsIgnoreCase("styles")) {
                    processStyles(el);
                } else if (name.equalsIgnoreCase("associations")) {
                    processAssociations(el);
                } else {
                    cantParse(el);
                }
            }
        }
    }

    private static void cantParse(Element n) {
        logger.warn(" Stylesheet: Cannot parse element: " + n.getNodeName());
    }

    private static void cantParse(Element n, String msg) {
        logger.warn(" Stylesheet: Cannot parse element: " + n.getNodeName());
        System.out.println("        " + msg);
    }

    private static void notImplemented(String s) {
        logger.warn(" Stylesheet: Not yet implemented: " + s);
    }

    private static boolean isBlank(String s) {
        return (s == null || s.trim().length() == 0);
    }

    private static void processImport(Element el) throws IOException {
        notImplemented("<IMPORT>");
    }

    private void processAssociations(Element associations) throws IOException {

        NodeList children = associations.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element el = (Element) child;

                AssociationElement associationElement = null;

                if (name.equalsIgnoreCase(AssociationElement.TYPE_ASSOCIATION)) {
                    String type = el.getAttribute(AssociationElement.ATT_TYPE);
                    String style = el.getAttribute(AssociationElement.ATT_STYLE);
                    if (isBlank(type) || isBlank(style)) {
                        throw new IOException("ERROR in stylesheet: missing method or style in METHOD_ASSOCIATION");
                    }
                    associationElement = AssociationElement.getTypeAssocation(type, style);
                    stylesheet.type2association.put(type, associationElement);
                } else if (name.equalsIgnoreCase(AssociationElement.METHOD_ASSOCIATION)) {
                    String method = el.getAttribute(AssociationElement.ATT_METHOD);
                    String style = el.getAttribute(AssociationElement.ATT_STYLE);
                    if (isBlank(method) || isBlank(style)) {
                        throw new IOException("ERROR in stylesheet: missing method or style in METHOD_ASSOCIATION");
                    }
                    associationElement = AssociationElement.getMethodAssocation(method, style);
                    stylesheet.meth2association.put(method, associationElement);
                } else if (name.equalsIgnoreCase(AssociationElement.FILE_TYPE_ASSOCIATION)) {
                    String method = el.getAttribute(AssociationElement.ATT_FILE_TYPE);
                    if (isBlank(method)) {
                        throw new IOException("ERROR in stylesheet: missing method in FILE_TYPE_ASSOCIATION");
                    }
                    associationElement = AssociationElement.getFileTypeAssocation(method);
                    stylesheet.filetype2association.put(method, associationElement);
                } else if (name.equalsIgnoreCase(AssociationElement.METHOD_REGEX_ASSOCIATION)) {
                    String regex = el.getAttribute(AssociationElement.ATT_REGEX);
                    String style = el.getAttribute(AssociationElement.ATT_STYLE);
                    if (isBlank(regex) || isBlank(style)) {
                        throw new IOException("ERROR in stylesheet: missing method or style in METHOD_ASSOCIATION");
                    }
                    try {
                        Pattern pattern = Pattern.compile(regex);
                        associationElement = AssociationElement.getMethodRegexAssocation(regex, style);
                        stylesheet.regex2association.put(pattern, associationElement);
                    } catch (PatternSyntaxException pse) {
                        IOException ioe = new IOException("ERROR in stylesheet: Regular Expression not valid: '"
                                + regex + "'");
                        ioe.initCause(pse);
                        throw ioe;
                    }
                } else {
                    cantParse(el);
                }

                //Now read the properties maps
                NodeList grand_children = child.getChildNodes();
                for (int j = 0; j < grand_children.getLength(); j++) {
                    Node grand_child = grand_children.item(j);
                    if (grand_child instanceof Element) {
                        if (grand_child.getNodeName().equalsIgnoreCase(PropertyMap.PROP_ELEMENT_NAME)) {
                            processProperty((Element) grand_child, associationElement.getPropertyMap());
                        } else {
                            cantParse(el);
                        }
                    }
                }
            }
        }
    }

    private void processStyles(Element stylesNode) throws IOException {
        NodeList children = stylesNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element el = (Element) child;

                if (name.equalsIgnoreCase(StyleElement.NAME) || name.equalsIgnoreCase(Stylesheet.WrappedStyleElement.NAME)) {
                    processStyle(el, true);
                }
            }
        }
    }

    private StyleElement processStyle(Element styleel, boolean top_level) throws IOException {

        // node name should be STYLE, COPY_STYLE or USE_STYLE
        String node_name = styleel.getNodeName();

        StyleElement se = null;
        if (StyleElement.NAME.equalsIgnoreCase(node_name)) {
            String styleName = styleel.getAttribute(StyleElement.ATT_NAME);
            se = stylesheet.createStyle(styleName, top_level);
            se.childContainer = styleel.getAttribute(StyleElement.ATT_CONTAINER);

        } else if (Stylesheet.WrappedStyleElement.NAME.equalsIgnoreCase(node_name)) {
            String styleName = styleel.getAttribute(StyleElement.ATT_NAME);
            if (styleName == null || styleName.trim().length() == 0) {
                throw new IOException("Can't have a USE_STYLE element with no name");
            }

            se = stylesheet.getWrappedStyle(styleName);
            // Not certain this will work
            se.childContainer = styleel.getAttribute(StyleElement.ATT_CONTAINER);

            return se; // do not do any other processing on a USE_STYLE element
        } else {
            cantParse(styleel);
        }

        if (se == null) {
            cantParse(styleel);
        }

        if (top_level) {
            if (isBlank(se.getName())) {
                logger.warn(" Stylesheet: All top-level styles must have a name!");
            } else {
                stylesheet.addToIndex(se);
            }
        }

        NodeList children = styleel.getChildNodes();

        // there can be multiple <PROPERTY> children
        // There should only be one child <GLYPH> OR one or more <MATCH> and <ELSE> elements
        // <COPY_STYLE> is not supposed to have <PROPERTIES>, but it is allowed to here
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element el = (Element) child;

                if (name.equalsIgnoreCase(GlyphElement.NAME)) {
                    GlyphElement ge2 = processGlyph(el);
                    se.setGlyphElement(ge2);
                } else if (name.equalsIgnoreCase(PropertyMap.PROP_ELEMENT_NAME)) {
                    processProperty(el, se.propertyMap);
                } else if (name.equalsIgnoreCase(MatchElement.NAME) || name.equalsIgnoreCase(ElseElement.NAME)) {
                    MatchElement me = processMatchElement(el);
                    se.addMatchElement(me);
                } else {
                    cantParse(el);
                }
            }
        }

        return se;
    }

    private GlyphElement processGlyph(Element glyphel) throws IOException {
        GlyphElement ge = new GlyphElement();

        String type = glyphel.getAttribute(GlyphElement.ATT_TYPE);
        if (GlyphElement.knownGlyphType(type)) {
            ge.setType(type);
        } else {
            logger.warn("<GLYPH type='" + type + "'> not understood");
            ge.setType(GlyphElement.TYPE_BOX);
        }

        String position = glyphel.getAttribute(GlyphElement.ATT_POSITION);
        ge.setPosition(position);

        NodeList children = glyphel.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element el = (Element) child;

                if (name.equalsIgnoreCase(GlyphElement.NAME)) {
                    GlyphElement ge2 = processGlyph(el);
                    ge.addGlyphElement(ge2);
                } else if (name.equalsIgnoreCase(ChildrenElement.NAME)) {
                    ChildrenElement ce = processChildrenElement(el);
                    ge.setChildrenElement(ce);
                } else if (name.equalsIgnoreCase(PropertyMap.PROP_ELEMENT_NAME)) {
                    processProperty(el, ge.propertyMap);
                } else {
                    cantParse(el);
                }
            }
        }

        return ge;
    }

    private ChildrenElement processChildrenElement(Element childel) throws IOException {
        ChildrenElement ce = new ChildrenElement();

        String position = childel.getAttribute(ChildrenElement.ATT_POSITIONS);
        ce.setPosition(position);
        String container = childel.getAttribute(ChildrenElement.ATT_CONTAINER);
        ce.setChildContainer(container);

        NodeList children = childel.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element el = (Element) child;

                if (name.equalsIgnoreCase(StyleElement.NAME) || name.equalsIgnoreCase(Stylesheet.WrappedStyleElement.NAME)) {
                    StyleElement se = processStyle(el, false);
                    ce.setStyleElement(se);
                } else if (name.equalsIgnoreCase(MatchElement.NAME) || name.equalsIgnoreCase(ElseElement.NAME)) {
                    MatchElement me = processMatchElement(el);
                    ce.addMatchElement(me);
                } else if (name.equalsIgnoreCase(PropertyMap.PROP_ELEMENT_NAME)) {
                    processProperty(el, ce.propertyMap);
                } else {
                    cantParse(el);
                }
            }
        }
        return ce;

    }

    private MatchElement processMatchElement(Element matchel) throws IOException {
        MatchElement me;

        if (MatchElement.NAME.equalsIgnoreCase(matchel.getNodeName())) {
            me = new MatchElement();
            String type = matchel.getAttribute(MatchElement.ATT_TEST);
            String param = matchel.getAttribute(MatchElement.ATT_PARAM);
            if (!isBlank(type)) {
                if (!MatchElement.knownTestType(type)) {
                    cantParse(matchel, "Unknown test type, test='" + type + "'");
                }

                me.match_test = type;
                if (!isBlank(param)) {
                    me.match_param = param;

                    if (MatchElement.MATCH_BY_METHOD_REGEX.equals(type)) {
                        try {
                            me.match_regex = Pattern.compile(param);
                        } catch (PatternSyntaxException pse) {
                            throw new IOException("ERROR in stylesheet: Regular Expression not valid: '"
                                    + param + "'", pse);
                        }
                    }
                }
            }

        } else if (ElseElement.NAME.equalsIgnoreCase(matchel.getNodeName())) {
            // an "ELSE" element is just like MATCH,
            //  except that it always matches as true
            me = new ElseElement();
        } else {
            cantParse(matchel);
            me = new ElseElement(); // treat it like an ELSE element
        }

        NodeList children = matchel.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element el = (Element) child;

                if (name.equalsIgnoreCase(StyleElement.NAME) || name.equalsIgnoreCase(Stylesheet.WrappedStyleElement.NAME)) {
                    StyleElement se = processStyle(el, false);
                    me.setStyle(se);
                } else if (name.equalsIgnoreCase(MatchElement.NAME) || name.equalsIgnoreCase(ElseElement.NAME)) {
                    MatchElement me2 = processMatchElement(el);
                    me.subMatchList.add(me2);
                } else if (name.equalsIgnoreCase(PropertyMap.PROP_ELEMENT_NAME)) {
                    processProperty(el, me.propertyMap);
                } else {
                    cantParse(el);
                }
            }
        }
        return me;
    }

    private void processProperty(Element properElement, PropertyMap propertied)
            throws IOException {
        String key = properElement.getAttribute(PropertyMap.PROP_ATT_KEY);
        String value = properElement.getAttribute(PropertyMap.PROP_ATT_VALUE);
        if (key == null) {
            throw new IOException("ERROR: key or value of <PROPERTY> is null");
        }
        propertied.setProperty(key, value);
    }

    private static String escapeXML(String s) {
        if (s == null) {
            return "";
        } else {
            return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt").replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
        }
    }

    static void appendAttribute(StringBuffer sb, String name, String value) {
        if (value != null && value.trim().length() > 0) {
            sb.append(" ").append(name).append("='").append(escapeXML(value)).append("'");
        }
    }

    private static synchronized Stylesheet getStylesheet(String name, InputStream istr) {
        Stylesheet stylesheet = null;
        XmlStylesheetParser parser = new XmlStylesheetParser();
        try {
            logger.debug("Loading stylesheet: {}", name);
            stylesheet = parser.parse(istr);
        } catch (Exception ex) {
            logger.error(" Couldn't initialize stylesheet " + name, ex);
        } finally {
            GeneralUtils.safeClose(istr);
        }
        return stylesheet;
    }

    public static void addStyleSheet(String name, InputStream istr) {
        Stylesheet stylesheet = getStylesheet(name, istr);
        added_stylesheet_map.put(name, stylesheet);
        refreshAddedStylesheet();
    }

    public static void removeStyleSheet(String name) {
        added_stylesheet_map.remove(name);
        refreshAddedStylesheet();
    }
}
