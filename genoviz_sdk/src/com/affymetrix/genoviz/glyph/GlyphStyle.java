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

package com.affymetrix.genoviz.glyph;

import java.awt.*;
import java.util.*;

/**
 * A Glyph Style is associated with each glyph,
 * keeping it's background color, foreground color and Font.
 * In combination with the {@link GlyphStyleFactory},
 * it uses the Flyweight pattern.
 * Only one object exists for each used combination of colors and fonts.
 *
 * <p> There is no empty constructor because GlyphStyle is immutable.
 * It has no set accessors.
 * The get accessors return pointers.
 * However, the properties are each immutable objects themselves.
 * Hence, the properties of a GlyphStyle cannot be changed.
 */
public class GlyphStyle {

  private Color background_color = null;
  private Color foreground_color = null;
  private Font fnt = null;

  public GlyphStyle( Color fg, Color gb, Font fnt ) {
      this.background_color = gb;
      this.foreground_color = fg;
      this.fnt = fnt;
      if ( fg == null || gb == null || fnt == null )
        throw new NullPointerException ( "Can't make GlyphStyle with null constructor argument." );
  }

  public Color getBackgroundColor () {
    return this.background_color;
  }

  public Color getForegroundColor () {
    return this.foreground_color;
  }

  public Font getFont() {
    return this.fnt;
  }

  public boolean equals( Object obj ) {
    if( obj instanceof GlyphStyle ) {
      return equals( (GlyphStyle) obj );
    }
    else {
      return false;
    }
  }

  public boolean equals( GlyphStyle theStyle ) {
    if ( theStyle == this ) return true;
    if ( null == theStyle ) return false;
    return ( this.getFont().equals( theStyle.getFont() ) &&
             this.getForegroundColor().equals( theStyle.getForegroundColor() ) &&
             this.getBackgroundColor().equals( theStyle.getBackgroundColor() ) );
  }

  public int hashCode() {
    return getForegroundColor().hashCode() + getBackgroundColor().hashCode() + getFont().hashCode();
  }

}
