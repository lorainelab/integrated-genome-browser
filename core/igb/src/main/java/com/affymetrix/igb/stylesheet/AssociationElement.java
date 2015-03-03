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

import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.view.factories.MapTierGlyphFactoryI;
import com.lorainelab.igb.genoviz.extensions.SeqMapViewExtendedI;

public final class AssociationElement implements DrawableElement {
    /*
     Element name can be "METHOD_ASSOCIATION", "METHOD_REGEX_ASSOCIATION",
     or "TYPE_ASSOCIATION"

     <!ELEMENT METHOD_ASSOCIATION (PROPERTY*)>
     <!ATTLIST METHOD_ASSOCIATION
     method CDATA #REQUIRED
     style CDATA #REQUIRED
     >
     */

    static final String ATT_STYLE = "style";

    private String elementName;
    private PropertyMap propertyMap; //Come back to check this... tmp fix (dcn/nr)
    private String styleName;
    private String paramName;  // method, type, or regex
    private String paramValue;

    // If the styleName starts with "com.", then try to instantiate an old-fashioned factory
    private MapTierGlyphFactoryI factory = null;

    private AssociationElement(String elementName,
            String paramName, String paramValue, String styleName) {

        this.propertyMap = new PropertyMap();
        this.elementName = elementName;
        this.paramName = paramName;
        this.paramValue = paramValue;
        this.styleName = styleName;

        if (styleName.startsWith("com.")) {
//      this.factory = getFactoryByClassname(styleName);
        } else {
            this.factory = null;
        }

    }

    static final String METHOD_ASSOCIATION = "METHOD_ASSOCIATION";
    static final String ATT_METHOD = "method";
    static final String METHOD_REGEX_ASSOCIATION = "METHOD_REGEX_ASSOCIATION";
    static final String ATT_REGEX = "regex";
    static final String TYPE_ASSOCIATION = "TYPE_ASSOCIATION";
    static final String ATT_TYPE = "type";
    static final String FILE_TYPE_ASSOCIATION = "FILE_TYPE_ASSOCIATION";
    static final String ATT_FILE_TYPE = "file_type";

    static AssociationElement getMethodAssocation(String method, String styleName) {
        return new AssociationElement(METHOD_ASSOCIATION,
                ATT_METHOD, method, styleName);
    }

    static AssociationElement getMethodRegexAssocation(String method, String styleName) {
        return new AssociationElement(METHOD_REGEX_ASSOCIATION,
                ATT_REGEX, method, styleName);
    }

    static AssociationElement getTypeAssocation(String method, String styleName) {
        return new AssociationElement(TYPE_ASSOCIATION,
                ATT_TYPE, method, styleName);
    }

    public static AssociationElement getFileTypeAssocation(String method) {
        return new AssociationElement(FILE_TYPE_ASSOCIATION,
                ATT_FILE_TYPE, method, "");
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        StyleElement clone = (StyleElement) super.clone();
        if (propertyMap != null) {
            clone.propertyMap = (PropertyMap) this.propertyMap.clone();
        }
        return clone;
    }

    public GlyphI symToGlyph(SeqMapViewExtendedI gviewer, SeqSymmetry sym, GlyphI container,
            Stylesheet stylesheet, PropertyMap context) {
        GlyphI glyph = null;

        PropertyMap oldContext = propertyMap.getContext();
        this.propertyMap.setContext(context);

        if (factory == null) {
            StyleElement se = stylesheet.getStyleByName(styleName);
            if (se == null) {
                se = stylesheet.getDefaultStyleElement();
            }
            glyph = se.symToGlyph(gviewer, sym, container, stylesheet, propertyMap);
        } else {
            glyph = null; // TODO: maybe change the MapViewGlyphFactoryI interface to return a GlyphI ?
        }

        this.propertyMap.setContext(oldContext);
        return glyph;
    }

    public StringBuffer appendXML(String indent, StringBuffer sb) {
        sb.append(indent).append('<').append(elementName);
        XmlStylesheetParser.appendAttribute(sb, paramName, paramValue);
        XmlStylesheetParser.appendAttribute(sb, ATT_STYLE, styleName);
        sb.append(">\n");
        this.propertyMap.appendXML(indent + "  ", sb);
        sb.append(indent).append("</").append(elementName).append(">\n");
        return sb;
    }

    MapTierGlyphFactoryI getGlyphFactory() {
        return factory;
    }

    public PropertyMap getPropertyMap() {
        return propertyMap;
    }
}
