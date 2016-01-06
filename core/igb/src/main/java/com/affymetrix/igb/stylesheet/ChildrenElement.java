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

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.view.layout.ExpandPacker;
import java.util.ArrayList;
import java.util.List;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;

final class ChildrenElement implements Cloneable, XmlAppender {

    /*
     <!ELEMENT CHILDREN (PROPERTY*, ((MATCH+,ELSE?) | (STYLE|USE_STYLE)))>
     <!ATTLIST CHILDREN
     container CDATA #IMPLIED
     child_positions CDATA #IMPLIED
     >
     */
    static String NAME = "CHILDREN";

    static String ATT_CONTAINER = "container";
    static String ATT_POSITIONS = "child_positions";

    private String childContainer = ".";
    private String childPositions; // becomes default position for children glyphs if they don't override it
    private List<MatchElement> matchElements;
    StyleElement styleElement;

    PropertyMap propertyMap;

    static ExpandPacker expand_packer;

    static {
        expand_packer = new ExpandPacker();
        expand_packer.setParentSpacer(3);
        expand_packer.setStretchHorizontal(false);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ChildrenElement clone = (ChildrenElement) super.clone();
        if (styleElement != null) {
            clone.styleElement = (StyleElement) styleElement.clone();
        }
        if (matchElements != null) {
            clone.matchElements = new ArrayList<>(matchElements.size());
            for (MatchElement me : matchElements) {
                MatchElement new_me = (MatchElement) me.clone();
                clone.matchElements.add(new_me);
            }
        }
        if (propertyMap != null) {
            clone.propertyMap = (PropertyMap) this.propertyMap.clone();
        }
        return clone;
    }

    ChildrenElement() {
        this.propertyMap = new PropertyMap();
    }

    static GlyphI findContainer(GlyphI gl, String container) {
        GlyphI container_glyph = gl;

        if (container == null || "".equals(container)) {
            container_glyph = gl;
        } else if (".".equals(container)) {
            container_glyph = gl;
        } else if ("..".equals(container)) {
            container_glyph = parent(gl);
        } else if ("../..".equals(container)) {
            container_glyph = parent(parent(gl));
        } else if ("../../..".equals(container)) {
            container_glyph = parent(parent(parent(gl)));
        } else if ("../../../..".equals(container)) {
            container_glyph = parent(parent(parent(parent(gl))));

            /// TODO: handle arbitrary nesting levels
        } else if ("/".equals(container)) {
            container_glyph = gl;
            while (!(container_glyph instanceof TierGlyph)) {
                container_glyph = parent(container_glyph);
            }
        }
        return container_glyph;
    }

    private static GlyphI parent(GlyphI gl) {
        if (gl instanceof TierGlyph) {
            return gl;
        } else {
            return gl.getParent();
        }
    }

    void setStyleElement(StyleElement styleElement) {
        this.styleElement = styleElement;
    }

    void addMatchElement(MatchElement me) {
        if (matchElements == null) {
            matchElements = new ArrayList<>();
        }
        matchElements.add(me);
    }

    public StringBuffer appendXML(String indent, StringBuffer sb) {
        sb.append(indent).append('<').append(NAME);
        XmlStylesheetParser.appendAttribute(sb, ATT_CONTAINER, childContainer);
        XmlStylesheetParser.appendAttribute(sb, ATT_POSITIONS, childPositions);
        sb.append(">\n");

        if (this.propertyMap != null) {
            propertyMap.appendXML(indent + "  ", sb);
        }

        if (matchElements != null) {
            for (MatchElement kid : matchElements) {
                kid.appendXML(indent + "  ", sb);
            }
        }

        if (styleElement != null) {
            styleElement.appendXML(indent + "  ", sb);
        }

        sb.append(indent).append("</").append(NAME).append(">\n");
        return sb;
    }

    void setChildContainer(String child_container) {
        this.childContainer = child_container;
    }

    void setPosition(String position) {
        this.childPositions = position;
    }
}
