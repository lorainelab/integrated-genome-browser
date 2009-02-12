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

package com.affymetrix.genoviz.widget.neoseq;
import com.affymetrix.genoviz.glyph.*;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.util.*;

import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.bioviews.*;

/**
 * contains other WrapGlyphs.
 * A WrapAnnot does not draw itself.
 * It is used merely to cashe residues per line for it's children.
 * There may well be a better way.
 */
public class WrapAnnot extends WrapGlyph {

	public WrapAnnot() {
		children = new Vector<GlyphI>();
	}

	public void addChild(GlyphI glyph)  {
		if (glyph instanceof WrapGlyph) {
			((WrapGlyph)glyph).setResiduesPerLine(residues_per_line);
		}
		super.addChild(glyph);
	}

	public void addChild(GlyphI glyph, int position) {
		if (glyph instanceof WrapGlyph) {
			((WrapGlyph)glyph).setResiduesPerLine(residues_per_line);
		}
		super.addChild(glyph, position);
	}

	/**
	 * does nothing.
	 * It's children will draw themselves.
	 */
	public void draw(ViewI v) {
	}

}
