package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.Color;
import java.awt.Polygon;

/**
 *
 * @author hiralv
 */
public class PointedOutlinedGlyph extends PointedGlyph {

	private Color bgcolor = Color.white;
	Polygon polygon = new Polygon();
	
	@Override
	public void draw(ViewI view) {
		super.draw(view);
		if (this.getPixelBox().width > 3 && this.getPixelBox().height > 3) {
			polygon.xpoints = x;
			polygon.ypoints = y;
			polygon.npoints = 5;
			view.getGraphics().setColor(bgcolor);
			view.getGraphics().draw(polygon);
		}
	}

	/**
	 * Sets the outline color; the fill color is automatically calculated as a
	 * darker shade.
	 */
	@Override
	public void setColor(Color c) {
		super.setColor(c);
		bgcolor = c.darker();
	}
}
