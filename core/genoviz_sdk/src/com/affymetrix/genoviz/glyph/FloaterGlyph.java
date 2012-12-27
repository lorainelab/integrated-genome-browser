package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author hiralv
 */
public interface FloaterGlyph extends GlyphI{

	/**
	 *  Checks to make sure the the boundaries of a floating glyph are
	 *  inside the map view.
	 *  If the glyph is not a floating glyph, this will have no effect on it.
	 */
	public void checkBounds(GlyphI gl, ViewI view);
	
	/**
	 * Returns coordinates to be used when a glyph is set to float
	 * @param glyph
	 * @param view
	 * @return 
	 */
	public Rectangle2D.Double getFloatCoords(Glyph glyph, ViewI view);
	
	/**
	 * Returns coordiantes to be used when a glyph is set to unfloat.
	 * @param glyph
	 * @param view
	 * @return 
	 */
	public Rectangle2D.Double getUnfloatCoords(Glyph glyph, ViewI view);
}
