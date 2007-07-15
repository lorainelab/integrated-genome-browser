/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.igb.stylesheet;

import com.affymetrix.genometry.DerivedSeqSymmetry;
import com.affymetrix.genometry.MutableSeqSpan;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.symmetry.SimpleDerivedSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.ArrowGlyph;
import com.affymetrix.genoviz.glyph.BridgeGlyph;
import com.affymetrix.genoviz.glyph.DirectedGlyph;
import com.affymetrix.genoviz.glyph.PointedGlyph;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.tiers.ExpandPacker;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;
import java.util.*;

public class GlyphElement implements Cloneable, XmlAppender {
/*
<!ELEMENT GLYPH (PROPERTY*, GLYPH*, CHILDREN?)>
<!ATTLIST GLYPH
    type (box | filled_box | arrow | hidden | line | pointed | span | none) #REQUIRED
    position CDATA #IMPLIED
  >
*/
    
  public static String NAME = "GLYPH";
  public static String ATT_TYPE = "type";
  public static String ATT_POSITION = "position";
  
  static ExpandPacker expand_packer;
  static {
      expand_packer = new ExpandPacker();
      expand_packer.setParentSpacer(3);
      expand_packer.setStretchHorizontal(false);
  }
  
  public static String TYPE_BOX = "box";
  public static String TYPE_FILLED_BOX = "filled_box";
  public static String TYPE_LINE = "line";
  public static String TYPE_ARROW = "arrow";
  public static String TYPE_POINTED = "pointed";
  public static String TYPE_SPAN = "span";

  public static String TYPE_NONE = "none";
  public static String TYPE_INVISIBLE = "hidden";

  static String[] knownTypes = new String[] {
    TYPE_BOX, TYPE_FILLED_BOX, TYPE_LINE, 
    TYPE_ARROW, TYPE_POINTED, TYPE_SPAN,
    TYPE_NONE,
    TYPE_INVISIBLE,
  };
  
  /**
   *  Indicates a color; the value shoule be a six-digit RRGGBB hex String.
   */
  public static String PROP_KEY_COLOR = "color";
  
  /** Set to "true" (default) or "false" to indicate that the map.setInfo()
   *  should be called on the indicated glyph.
   *  Default is true.  False is useful when there are multiple
   *  glyphs representing the same symmetry.
   */
  public static String PROP_KEY_INDEXED = "indexed";

  /** Whether the glyph is labeled or not.
   */
  public static String PROP_KEY_LABELED = "labeled";

  /**
   *  Which property name to use to determine the label, if labeled is true.
   */
  public static String PROP_KEY_LABEL_FIELD = "label_field";

  /** Set to "5to3" (default) or "3to5" to
   *  indicate the direction of directed glyphs, such as arrows.
   */
  public static String PROP_KEY_DIRECTION = "direction";
  
  public static String PROP_VALUE_DIRECTION_5to3 = "5to3";
  public static String PROP_VALUE_DIRECTION_3to5 = "3to5";
  
  public static Color default_color = Color.GREEN;
  
  PropertyMap propertyMap;
  List enclosedGlyphElements = null;
  ChildrenElement childrenElement = null;
  String position;
  String type;

  int glyph_height = 10;
  static int diff_height = 3;
  
  DerivedSeqSymmetry der; // used for transforming spans
  MutableSeqSpan derSpan; // used for transforming spans
  
  public Object clone() throws CloneNotSupportedException {
    GlyphElement clone = (GlyphElement) super.clone();
    if (this.enclosedGlyphElements != null) {
      clone.enclosedGlyphElements = new ArrayList(enclosedGlyphElements.size());
      for (int i=0; i<enclosedGlyphElements.size(); i++) {
        GlyphElement ge = (GlyphElement) enclosedGlyphElements.get(i);
        GlyphElement new_glyph_element = (GlyphElement) ge.clone();
        clone.enclosedGlyphElements.add(new_glyph_element);
      }
    }
    if (propertyMap != null) {
      clone.propertyMap = (PropertyMap) this.propertyMap.clone();
    }
    if (childrenElement != null) {
      clone.childrenElement = (ChildrenElement) this.childrenElement.clone();
    }
    
    return clone;
  }
  
  public GlyphElement() {
    this.propertyMap = new PropertyMap();
    der = new SimpleDerivedSeqSymmetry();
    derSpan = new SimpleMutableSeqSpan();
    der.addSpan(derSpan);
  }

  public String getPosition() {
    return this.position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public String getType() {
    return this.type;
  }

  public void setType(String type) {
    this.type = type;
  }
  
  public List getEnclosedGlyphElements() {
    return this.enclosedGlyphElements;
  }
  
  public void addGlyphElement(GlyphElement ge) {
    if (enclosedGlyphElements == null) {
      enclosedGlyphElements = new ArrayList();
    }
    enclosedGlyphElements.add(ge);
  }
  
  public void setChildrenElement(ChildrenElement c) {
    this.childrenElement = c;
  }
  
  public ChildrenElement getChildrenElement() {
    return this.childrenElement;
  }
  
  static boolean knownGlyphType(String type) {
    for (int i=0; i<knownTypes.length; i++) {
      if (type.equals(knownTypes[i])) { return true; }
    }
    return false;
  }
  
  public GlyphI makeGlyph(String type, SeqSpan span) {
    boolean use_label = false;
    if ("true".equals(propertyMap.getProperty(PROP_KEY_LABELED))) {
      use_label = true;
    }

    GlyphI gl = null;
    if (TYPE_NONE.equals(type)) {
      gl = null;
    } else if (TYPE_BOX.equals(type)) {
      gl = new EfficientOutlineContGlyph();
    } else if (TYPE_FILLED_BOX.equals(type)) {
      gl = new EfficientOutlinedRectGlyph();
    } else if (TYPE_POINTED.equals(type)) {
      gl = new PointedGlyph();
    } else if (TYPE_LINE.equals(type)) {
      if (use_label) {
        gl = new EfficientLabelledLineGlyph();
      } else {
        gl = new EfficientLineContGlyph();
      }
    } else if (TYPE_ARROW.equals(type)) {
      gl = new ArrowGlyph();
    } else if (TYPE_SPAN.equals(type)) {
      gl = new BridgeGlyph();
    } else if (TYPE_INVISIBLE.equals(type)) {
      gl = new InvisibleBoxGlyph();
    } else {
      // this will be caught by knownGlyphType() method
      System.out.println("GLYPH Type Not Known: " + type);
    }
    return gl;
  }
  
  public GlyphI symToGlyph(SeqMapView gviewer, SeqSymmetry insym, GlyphI parent_glyph, 
      Stylesheet stylesheet, PropertyMap context) {
    
    if (insym == null) { 
      return null; 
    }
    
    // NOTE: some of the glyphs below are very picky about the order various
    // properties are set in relative to the adding of children and packing.  
    // So do lots of testing if you re-arrange any of this.

    PropertyMap oldContext = propertyMap.getContext();
    propertyMap.setContext(context);
    
    GlyphI gl = null;
    if (knownGlyphType(type)) {
      TierGlyph tier_glyph = (TierGlyph) context.getProperty(TierGlyph.class.getName());
      
      //SeqSymmetry transformed_sym = gviewer.transformForViewSeq(insym);
      //SeqSpan span = transformed_sym.getSpan(gviewer.getViewSeq());
      SeqSpan span = transformForViewSeq(gviewer, insym);

      if (span == null) {
        // TODO: In future we must take into account the possibility
        // that an item may have children which map to the current seq
        // even though the parent does not.  Thus we need to loop over
        // the children even when the span for the current item is null.
        //
        // Would be nice if a SeqSymmetry could report whether all its children
        // are or are not enclosed in its bounds, then we would know which
        // syms we really have to worry about that for.
        gl = null; 
        // NOTE: important not to simply call "return null" before
        // taking care of restoring the context.
      } else if (span.getLength() == 0 && parent_glyph instanceof TierGlyph) {
        gl = null;
      }
      else {

      gl = makeGlyph(type, span);
      boolean is_labeled_glyph = (gl instanceof LabelledGlyph);

      if (gl != null) {
        gl.setCoords(span.getMin(), 0, span.getLength(), glyph_height);
        
        if (is_labeled_glyph) {
          configureLabel((LabelledGlyph) gl, insym, tier_glyph);
        }
        
        gl.setColor(findColor(propertyMap));
        
        addToParent(parent_glyph, gl, this.position);
        
        indexGlyph(propertyMap, gviewer, gl, insym);
      }

      
      // Normally, the container for sub-glyphs and children is the glyph itself,
      // but if no glyph was drawn, use the parent glyph
      GlyphI container = gl;
      if (gl == null) {
        container = parent_glyph;
      }
      
      // Now do <GLYPH> elements enclosed inside a <GLYPH> element.
      // These re-draw the same sym, not the children
      drawEnclosedGlyphs(gviewer, container, insym, stylesheet);

      if (childrenElement != null) {
        // Always use "insym" rather than "transformed_sym" for children.
        // The transformed_sym may not have the same number of levels of nesting.
        childrenElement.childSymsToGlyphs(gviewer, insym, container, stylesheet, propertyMap);
      }
      
      packGlyph(gviewer, container);
      
      // Setting the direction of a directed glyph must come after
      // adding the children to it.  Not sure why.
      if (gl instanceof DirectedGlyph) {
        ((DirectedGlyph) gl).setForward(false);
        if (PROP_VALUE_DIRECTION_3to5.equalsIgnoreCase((String) propertyMap.getProperty(PROP_KEY_DIRECTION))) {
          ((DirectedGlyph) gl).setForward(! span.isForward());
        } else {
          ((DirectedGlyph) gl).setForward(span.isForward());
        }
      }
    }
    }

    propertyMap.setContext(oldContext);
    return gl;
  }

  void addToParent(GlyphI parent, GlyphI child, String position) {
    parent.addChild(child);
    //TODO: use position
    // One way to do it: make a special glyph interface StyledGlyphI where
    // all implementations of that interface know how to position themselves
    // inside their parents
  }

  void packGlyph(SeqMapView gviewer, GlyphI container) {
    if (container != null) {
      if (/* ! (container instanceof labeledGlyph) && */
           ! (container instanceof TierGlyph)) {
        //System.out.println("Packing: " + container.getClass().getName() + ", " + container.getChildCount());
        // packing with labeled glyphs doesn't work right, so skip it.
        container.setPacker(expand_packer);
        container.pack(gviewer.getSeqMap().getView());
      }
    }
  }
  
  void drawEnclosedGlyphs(SeqMapView gviewer, GlyphI container, SeqSymmetry insym, Stylesheet stylesheet) {
    if (enclosedGlyphElements != null) {
      // inside the parent, not inside the glyph.
      Iterator iter = enclosedGlyphElements.iterator();
      while (iter.hasNext()) {
        GlyphElement kid = (GlyphElement) iter.next();
        kid.symToGlyph(gviewer, insym, container, stylesheet, this.propertyMap);
      }
    }
  }
  
  static Color findColor(PropertyMap pm) {
    Color color = (Color) pm.getColor(PROP_KEY_COLOR);
    if (color == null || "".equals(color)) {
      IAnnotStyleExtended style = (IAnnotStyleExtended) pm.get(IAnnotStyleExtended.class.getName());
      if (style != null) {
        color = style.getColor();
      }
    }
    if (color == null) {
      color = default_color;
    }
    return color;
  }

  void configureLabel(LabelledGlyph lgl, SeqSymmetry insym, TierGlyph tier_glyph) {
    String the_label = null;
    if (insym instanceof SymWithProps) {
      String label_property_name = (String) this.propertyMap.getProperty(PROP_KEY_LABEL_FIELD);
      if (null == label_property_name) {
        the_label = insym.getID();
      } else {
        the_label = (String) ((SymWithProps) insym).getProperty(label_property_name);
        if (the_label == null) {
          the_label = label_property_name + "=???";
        }
      }
    } else {
      the_label = insym.getID();
    }

    // go ahead and set the height big enough for a label, even if it is null,
    // because (1) we want to keep constant heights with other labeled glyphs, and
    // (2) instances of LabelledGlyph expect that.
    lgl.getCoordBox().height *= 2;
    if (! (the_label == null)) {
      lgl.setLabel(the_label);
      if (tier_glyph.getDirection() == TierGlyph.DIRECTION_REVERSE) {
        lgl.setLabelLocation(LabelledGlyph.SOUTH);
      } else {
        lgl.setLabelLocation(LabelledGlyph.NORTH);
      }
    }
  }
  
  static void indexGlyph(PropertyMap pm, SeqMapView gviewer, GlyphI gl, SeqSymmetry insym) {
    if (! "false".equals(pm.getProperty(PROP_KEY_INDEXED))) {
      // This will call GlyphI.setInfo() as a side-effect.
      gviewer.getSeqMap().setDataModelFromOriginalSym(gl, insym);
    } else {
      // Even if we don't add the glyph to the map's data model,
      // it is still important to call GlyphI.setInfo() so that slicing will work.
      if (insym instanceof DerivedSeqSymmetry)  {
        gl.setInfo(((DerivedSeqSymmetry) insym).getOriginalSymmetry());
      } else {
        gl.setInfo(insym);
      }
    }
  }
   
  
  /** An efficient method to transform a single span. */
  SeqSpan transformForViewSeq(SeqMapView gviewer, SeqSymmetry insym) {

    der.clear();

    // copy the span into derSpan
    insym.getSpan(gviewer.getAnnotatedSeq(), derSpan);

    der.addSpan(derSpan);

    if (gviewer.getAnnotatedSeq() != gviewer.getViewSeq()) {

      //der.setOriginalSymmetry(null);

      SeqUtils.transformSymmetry(der, gviewer.getTransformPath());

      //der.setOriginalSymmetry(insym);
    }

    SeqSpan result_span = gviewer.getViewSeqSpan(der);

    return result_span;
  }
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    sb.append(indent).append('<').append(NAME);
    XmlStylesheetParser.appendAttribute(sb, ATT_TYPE, type);
    XmlStylesheetParser.appendAttribute(sb, ATT_POSITION, position);
    sb.append(">\n");
    if (this.propertyMap != null) {
      propertyMap.appendXML(indent + "  ", sb);
    }
    
    if (this.enclosedGlyphElements != null) {
      Iterator iter = enclosedGlyphElements.iterator();
      while (iter.hasNext()) {
       GlyphElement kid = (GlyphElement) iter.next();
       kid.appendXML(indent + "  ", sb);
      }
    }
    
    if (childrenElement != null) {
      childrenElement.appendXML(indent + "  ", sb);
    }

    sb.append(indent).append("</").append(NAME).append(">\n");
    return sb;
  }
}
