package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.widget.NeoMap;

/**
 *
 * @author hiralv
 */
public interface FloaterGlyph extends GlyphI{

	public void drawTraversal(ViewI view);
	
	public void checkBounds(GlyphI gl, NeoMap map);
}
