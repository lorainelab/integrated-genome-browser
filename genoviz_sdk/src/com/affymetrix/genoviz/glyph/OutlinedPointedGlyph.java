package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.Color;
import java.awt.Graphics;

/**
 *
 * @author hiralv
 */
public class OutlinedPointedGlyph extends PointedGlyph {
	private Color bgcolor = Color.white;
	int xx[] = new int[5];
	int yy[] = new int[5];
	
	@Override
	public void draw(ViewI view) {
		super.draw(view);
		Graphics g = view.getGraphics();
		if (getPixelBox().width > 2 && getPixelBox().height > 2 && HORIZONTAL == this.getOrientation()) {
			g.setColor(bgcolor);			
			if (this.isForward()) {	
				xx[0] = x[0] + 1;
				xx[2] = x[2] - 2;
				xx[1] = x[1] - 2;
				xx[3] = x[3] - 2;
				xx[4] = x[4];
				yy[0] = y[0] + 1;
				yy[1] = y[1] + 1;
				yy[2] = y[2] + 1;
				yy[3] = y[3] - 2;
				yy[4] = y[4] - 2;
			} else {
				xx[0] = x[0] + 1;
				xx[2] = x[2] - 2;
				xx[1] = x[1] + 1;
				xx[3] = x[3] - 2;
				xx[4] = x[4];
				yy[1] = y[1] + 1;
				yy[0] = y[0] + 1;
				yy[2] = y[2] + 1;
				yy[3] = y[3] - 2;
				yy[4] = y[4] - 2;
			} 
			g.fillPolygon(xx, yy, 5);
		}
	}
	
	/** Sets the outline color; the fill color is automatically calculated as  
	*  a darker shade. 
	*/
	@Override
	public void setColor(Color c) {
		super.setColor(c);
		bgcolor = c.darker();
	}
}
