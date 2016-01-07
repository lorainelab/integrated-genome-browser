/**
 * Copyright (c) 2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License").
 * A copy of the license must be included with any distribution of
 * this source code.
 * Distributions from Affymetrix, Inc., place this in the
 * IGB_LICENSE.html file.
 *
 * The license is also available at
 * http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.stylesheet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StyleElement {
    /*
     <!ELEMENT STYLE (PROPERTY*, ((MATCH+,ELSE?) | GLYPH))>
     <!ATTLIST STYLE
     name CDATA #IMPLIED
     container CDATA #IMPLIED
     >
     */

    static String NAME = "STYLE";

    static String ATT_NAME = "name";
    static String ATT_CONTAINER = "container";

    static Map<String, StyleElement> names2styles = new HashMap<>();

    String childContainer = ".";
    PropertyMap propertyMap;
    List<MatchElement> matchElements;
    String name;

    StyleElement() {
        this.propertyMap = new PropertyMap();
    }

    /**
     * Not yet implemented. Needs to do a deep copy.
     */
    public Object clone() throws CloneNotSupportedException {
        StyleElement clone = (StyleElement) super.clone();
        if (propertyMap != null) {
            clone.propertyMap = (PropertyMap) this.propertyMap.clone();
        }
        if (matchElements != null) {
            clone.matchElements = new ArrayList<>(matchElements.size());
            for (MatchElement me : matchElements) {
                MatchElement new_me = (MatchElement) me.clone();
                clone.matchElements.add(new_me);
            }
        }
        return clone;
    }

    final String getName() {
        return this.name;
    }

    void addMatchElement(MatchElement me) {
        if (matchElements == null) {
            matchElements = new ArrayList<>();
        }
        matchElements.add(me);
    }

    public String toString() {
        return "StyleElement [name=" + name + "]";
    }

    public StringBuffer appendXML(String indent, StringBuffer sb) {
        sb.append(indent).append('<').append(NAME);
        XmlStylesheetParser.appendAttribute(sb, ATT_NAME, name);
        XmlStylesheetParser.appendAttribute(sb, ATT_CONTAINER, childContainer);
        sb.append(">\n");
        this.propertyMap.appendXML(indent + "  ", sb);
        if (matchElements != null) {
            for (MatchElement kid : matchElements) {
                kid.appendXML(indent + "  ", sb);
            }
        }
        sb.append(indent).append("</").append(NAME).append(">\n");
        return sb;
    }
}
