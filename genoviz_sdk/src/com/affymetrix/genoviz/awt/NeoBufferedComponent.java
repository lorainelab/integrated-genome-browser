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

package com.affymetrix.genoviz.awt;

import java.awt.*;

import com.affymetrix.genoviz.util.Debug;

/**
 * a double buffered component that should draw quickly.
 */
public class NeoBufferedComponent extends Container {
  public boolean buf_debug = false;
  static boolean rememberToFlush = true;
  static boolean rememberToDispose = true;

  protected boolean buffered = true;
  protected boolean opaque = true;

  protected Image offScreenImage = null;
  protected Graphics offScreenGraphics = null;
  protected Dimension imageSize = null;
  protected Dimension graphicsSize = null;

  public void paint(Graphics g) {
    if (buf_debug) { System.out.println("----------------------------------------------"); }
    Dimension d = getSize();

    // don't even bother if size <= 0   GAH 3-2002
    if ((d.width <=  0) || d.height <=0) { super.paint(g); return; } 

    if (buffered) { 
      if (buf_debug) { System.out.println("calling bufferedPaint()"); }
      bufferedPaint(g); 
    }
    else { 
      if (opaque) {
        if (buf_debug) { System.out.println("opaque, filling in background"); }
        g.setColor(this.getBackground());
        g.fillRect(0, 0, d.width, d.height);
      }
      if (buf_debug) { System.out.println("calling directPaint()"); }
      directPaint(g); 
    }
    // let Container superclass handle painting of any children
    if (buf_debug)  { System.out.println("calling super.paint()"); }
    super.paint(g);
    if (buf_debug) { System.out.println("*********************************************"); }
  }

  /**
   *  subclasses should override directPaint() rather than paint() to 
   *  control appearance
   */
  public void directPaint(Graphics g) {
  }

  /**
   *  bufferedPaint takes over the double-buffering responsibility usually handled 
   *  in update() -- done here instead because of differences in lightweight 
   *  component handling in jdks -- in some implementations Containers don't call 
   *  update on their lightweight children but instead call paint() directly
   */
  public void bufferedPaint(Graphics g) {
    // still need to add ability to copy over image from g (via copyArea()) if 
    // component is not opaque!  For now not worrying about transparency though.
    Dimension d = getSize();
    if((offScreenImage == null) || (d.width != graphicsSize.width) ||
       (d.height != graphicsSize.height))  {
        getNewOffscreenImage();
    }
    // may want to try and clip offScreenGraphics same as g...
    // offScreenGraphics.setClip(...)  
    if (opaque) {
      offScreenGraphics.setColor(this.getBackground());
      offScreenGraphics.fillRect(0, 0, d.width, d.height);
    }
    directPaint(offScreenGraphics);

    // adding check for offScreenImage == null to avoid consequences of 
    //   race condition... GAH 11-24-98
    // but force draw for now in order to recreate these bugs... GAH 12-1-98
    if (offScreenImage == null) {
      Debug.inform("**** hit a NeoCanvas.update() " + 
                         "where offScreenImage == null! ****");
    }
    else {
      g.drawImage(offScreenImage, 0, 0, this);
    }
  }

  public void update(Graphics g) {
    paint(g);
  }

  /** turns double buffering on or off. */
  public void setDoubleBuffered(boolean b) {
    buffered = b;
  }

  /** @return true iff double buffering is on. */
  public boolean isDoubleBuffered() {
    return buffered;
  }

  /**
   * sets the opacity of the component.
   * Since these are lightweight components
   * they can be transparent.
   *
   * @param b true iff the component should be opaque.
   */
  public void setOpaque(boolean b) {
    opaque = b;
  }
  
  /**
   * gets the opacity of the component.
   */
  public boolean isOpaque() {
    return opaque;
  }

  public void getNewOffscreenImage() {
    Dimension d = getSize();

    if (rememberToFlush && offScreenImage != null) {
      offScreenImage.flush();
    }
    offScreenImage = createImage(d.width, d.height);
    imageSize = new Dimension(d.width, d.height);
    graphicsSize = new Dimension(d.width, d.height);
    if (rememberToDispose && offScreenGraphics != null)  { 
      offScreenGraphics.dispose(); 
    }
    offScreenGraphics = offScreenImage.getGraphics();

  }

  public void nullOffscreenImage() {
    // GAH 12-3-98
    //  trying to minimize damage caused by JVM image memory leak 
    //  see Java Developer's Connection, Bug ID = 4014323
    if (rememberToFlush && offScreenImage != null) {
      offScreenImage.flush();
    }

    offScreenImage = null;
    imageSize = null;
    graphicsSize = null;

    // GAH 12-3-98 another attempt  to minimize potential memory leaks
    if (rememberToDispose && offScreenGraphics != null) { 
      offScreenGraphics.dispose(); 
    }
    offScreenGraphics = null;
  }

  /**
   *  If offscreen double buffering is being used, will return the offscreen
   *  Image, otherwise will return null
   *  (this can then be used to generate GIFs from the NeoCanvas's
   *   offscreen buffer)
   */
  public Image getBufferedImage() {
    if (this.isDoubleBuffered())  {
      return offScreenImage;
    }
    else {
      return null;
    }
  }


}
