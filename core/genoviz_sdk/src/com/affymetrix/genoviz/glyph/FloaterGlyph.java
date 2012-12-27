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
	
	/**
	 *  Checks to make sure the the boundaries of a floating glyph are
	 *  inside the map view.
	 *  If the glyph is not a floating glyph, this will have no effect on it.
	 */
	public void checkBounds(GlyphI gl, NeoMap map);
}
