package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *
 * @author hiralv
 */
public interface FloaterGlyph extends GlyphI{

	/**
	 *  Should only have to modify view to set Y part of transform to identity
	 *     transform.
	 *  not sure if need to set view's coord box...
	 */
	public void drawTraversal(ViewI view);
    
}
