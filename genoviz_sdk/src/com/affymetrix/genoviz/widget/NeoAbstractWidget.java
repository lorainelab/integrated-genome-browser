/**
*   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.widget;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.affymetrix.genoviz.awt.NeoBufferedComponent;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;

import com.affymetrix.genoviz.util.GeneralUtils;

/**
 * Provides basic functionallity for all genoviz Widgets.
 */
public abstract class NeoAbstractWidget extends NeoBufferedComponent
  implements MouseListener, MouseMotionListener, KeyListener {

  protected Dimension pref_widg_size = new Dimension(1, 1);

  protected Vector mouse_listeners = new Vector();
  protected Vector mouse_motion_listeners = new Vector();
  protected Vector key_listeners = new Vector();

  protected Hashtable glyph_hash = new Hashtable();
  protected Hashtable model_hash = new Hashtable();

  protected static Hashtable colormap = GeneralUtils.getColorMap();

  protected int scroll_behavior[] = new int[2];

  // a list of selected glyphs
  protected Vector selected = new Vector();

  public void setDataModel(GlyphI gl, Object datamodel) {
    // glyph to datamodel must be one-to-one
    // datamodel to glyph can be one-to-many

    glyph_hash.put(gl, datamodel);
    gl.setInfo(datamodel);

    // more than one glyph may be associated with the same datamodel!
    // therefore check and see if already a glyph associated with this datamodel
    // if so, create a Vector and add glyphs to it (or extend the pre-exisiting one)
    Object previous = model_hash.get(datamodel);
    if (previous == null) {
      model_hash.put(datamodel, gl);
    }
    else {
      if (previous instanceof Vector) {
        ((Vector)previous).addElement(gl);
      }
      else {
        Vector glyphs = new Vector();
        glyphs.addElement(previous);
        glyphs.addElement(gl);
        model_hash.put(datamodel, glyphs);
      }
    }
  }

  public Object getDataModel(GlyphI glyph) {
    return glyph.getInfo();
  }

  /**
   *  This should be static, except interface methods can't be static
   */
  public void addColor(String name, Color col) {
    if (null == name) {
      throw new IllegalArgumentException("can't addColor without a name.");
    }
    if (null == col) {
      throw new IllegalArgumentException("can't add a null color.");
    }
    colormap.put(name, col);
  }

  public Color getColor(String name) {
    if (null == name) {
      throw new IllegalArgumentException("can't getColor without a name.");
    }
    return (Color)colormap.get(name);
  }
  public String getColorName(Color theColor) {
    if (null == theColor) {
      throw new IllegalArgumentException("can't get a name for a null color.");
    }
    Enumeration it = colormap.keys();
    while (it.hasMoreElements()) {
      String candidate = (String)it.nextElement();
      if (theColor.equals(colormap.get(candidate))) {
        return candidate;
      }
    }
    return null;
  }
  public Enumeration getColorNames() {
    return colormap.keys();
  }


  /**
   * Gets the visibility of an item in the widget.
   *
   * @param gl the item in question.
   */
  public boolean getVisibility(GlyphI gl) {
    return gl.isVisible();
  }


  public void moveAbsolute(GlyphI glyph, double x, double y) {
    glyph.moveAbsolute(x, y);
  }

  public void moveAbsolute(Vector glyphs, double x, double y) {
    for (int i=0; i<glyphs.size(); i++) {
      moveAbsolute((GlyphI)glyphs.elementAt(i), x, y);
    }
  }

  public void moveRelative(GlyphI glyph, double diffx, double diffy) {
    glyph.moveRelative(diffx, diffy);
  }

  public void moveRelative(Vector glyphs, double x, double y) {
    for (int i=0; i<glyphs.size(); i++) {
      moveRelative((GlyphI)glyphs.elementAt(i), x, y);
    }
  }


  public void setScrollIncrementBehavior(int id, int behavior) {
    scroll_behavior[id] = behavior;
  }

  public int getScrollIncrementBehavior(int id) {
    return scroll_behavior[id];
  }


  /** Subclasses should implement this. Default does nothing. 
   *  Implementations should add selections to the Vector 'selected',
   *  in addition to any other tasks specific to those classes.
   */
  public void select(GlyphI g) {
    // Implement in subclasses
  }

  public void select(Vector vec) {
    if (vec == null) {
      return;
    }
    for (int i=0; i<vec.size(); i++) {
      select((GlyphI)vec.elementAt(i));
    }
  }

  /**
   *  Clears all selections by actaually calling {@link #deselect(GlyphI)}
   *  on each one as well as removing them from the vector of selections.
   */
  public void clearSelected() {
    while (selected.size() > 0) {
      // selected.size() shrinks because deselect(glyph)
      //    calls selected.removeElement()
      Object gl = selected.elementAt(0);
      if (gl == null) { selected.removeElementAt(0); }
      else {
        deselect((GlyphI)gl);
      }
    }
    selected.removeAllElements();
  }

  /** Subclasses should implement this. Default does nothing.
   *  Implementations should remove selections from the Vector 'selected',
   *  in addition to any other tasks specific to those classes.
   */
  public void deselect(GlyphI gl) {}

  public void deselect(Vector vec) {
    // need to special case if vec argument is ref to same Vector as selected,
    //   since the deselect(Object) will cause shrinking of vec size as
    //   it is being looped through
    if (vec == null) {
      return;
    }
    if (vec == selected) {
      clearSelected();
    }
    for (int i=0; i<vec.size(); i++) {
      deselect((GlyphI)vec.elementAt(i));
    }
  }

  public Vector getSelected() {
    return selected;
  }
  
  /** Clears all graphs from the widget.
   *  This default implementation simply removes all elements from the
   *  list of selections.  (It does this without calling clearSelected(),
   *  because it is faster to skip an explict call to deselect(GlyphI)
   *  for each Glyph.)
   *  Subclasses should call this method during their own implementation.
   *  Subclasses may choose to call clearSelected() before calling this
   *  method if they require an explicit call to deselect(GlyphI) for
   *  each Glyph.
   */
  public void clearWidget() {
    selected.removeAllElements();
  }


  // implementing MouseListener interface and collecting mouse events
  public void mouseClicked(MouseEvent e) { heardMouseEvent(e); }
  public void mouseEntered(MouseEvent e) { heardMouseEvent(e); }
  public void mouseExited(MouseEvent e) { heardMouseEvent(e); }
  public void mousePressed(MouseEvent e) { heardMouseEvent(e); }
  public void mouseReleased(MouseEvent e) { heardMouseEvent(e); }

  // implementing MouseMotionListener interface and collecting mouse events
  public void mouseDragged(MouseEvent e) { heardMouseEvent(e); }
  public void mouseMoved(MouseEvent e) { heardMouseEvent(e); }

  public void heardMouseEvent(MouseEvent evt) {
    // override in subclasses!
  }

  public void addMouseListener(MouseListener l) {
    if (!mouse_listeners.contains(l)) {
      mouse_listeners.addElement(l);
    }
  }

  public void removeMouseListener(MouseListener l) {
    mouse_listeners.removeElement(l);
  }

  public void addMouseMotionListener(MouseMotionListener l) {
    if (!mouse_motion_listeners.contains(l)) {
      mouse_motion_listeners.addElement(l);
    }
  }

  public void removeMouseMotionListener(MouseMotionListener l) {
    mouse_motion_listeners.removeElement(l);
  }

  public void addKeyListener(KeyListener l) {
    if (!key_listeners.contains(l)) {
      key_listeners.addElement(l);
    }
  }

  public void removeKeyListener(KeyListener l) {
    key_listeners.removeElement(l);
  }

  public void destroy() {
    key_listeners.removeAllElements();
    mouse_motion_listeners.removeAllElements();
    mouse_listeners.removeAllElements();
    glyph_hash.clear();
    model_hash.clear();
    selected.removeAllElements();
  }

    // Implementing KeyListener interface and collecting key events
    public void keyPressed(KeyEvent e) { heardKeyEvent(e); }
    public void keyReleased(KeyEvent e) { heardKeyEvent(e); }
    public void keyTyped(KeyEvent e) { heardKeyEvent(e); }

    public void heardKeyEvent(KeyEvent e) {
      int id = e.getID();
      if (key_listeners.size() > 0) {
        KeyEvent nevt =
          new KeyEvent(this, id, e.getWhen(), e.getModifiers(),
              e.getKeyCode(), e.getKeyChar());
        KeyListener kl;
        for (int i=0; i<key_listeners.size(); i++) {
          kl = (KeyListener)key_listeners.elementAt(i);
          if (id == e.KEY_PRESSED) {
            kl.keyPressed(nevt);
          }
          else if (id == e.KEY_RELEASED) {
            kl.keyReleased(nevt);
          }
          else if (id == e.KEY_TYPED) {
            kl.keyTyped(nevt);
          }
        }
      }
    }

  /**
   *  Reshapes the component.
   *  Due to the way the Component class source code from Sun is written, it is this
   *  method that we must override, not setBounds(), even though this method
   *  is deprecated.
   *  <p>
   *  Users of this class should call setBounds(), but
   *  when extending this class, override this, not setBounds().
   *
   *  @deprecated use {@link #setBounds(int,int,int,int)}.
   */
  public void reshape(int x, int y, int width, int height) {
    pref_widg_size.setSize(width, height);
    super.reshape(x, y, width, height);
  }

  public Dimension preferredSize() {
    return getPreferredSize();
  }

  public Dimension getPreferredSize() {
    return pref_widg_size;
  }

  public void setPreferredSize(Dimension d) {
    pref_widg_size = d;
  }

  public void setCursor(Cursor cur) {
    Component comp[] = this.getComponents();
    for (int i=0; i<comp.length; i++) {
      comp[i].setCursor(cur);
    }
    super.setCursor(cur);
  }
}
