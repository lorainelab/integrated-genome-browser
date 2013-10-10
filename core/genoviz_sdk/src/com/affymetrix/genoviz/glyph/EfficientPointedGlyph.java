package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 *
 * @author hiralv
 */
public class EfficientPointedGlyph extends PointedGlyph{
	
	private static final int ratio = 3;
	
	public void draw(ViewI view) {
		view.transformToPixels(this.getCoordBox(), this.getPixelBox());
		if (this.getPixelBox().width == 0) { this.getPixelBox().width = 1; }
		if (this.getPixelBox().height == 0) { this.getPixelBox().height = 1; }
		
		int halfThickness = 1; 
		switch(this.getOrientation()){
			case HORIZONTAL:
				halfThickness = (getPixelBox().height-1)/2;
				if(getPixelBox().width > halfThickness*ratio){
					super.draw(view);
					return;
				}
				break;
			case VERTICAL:
				halfThickness = (getPixelBox().width-1)/2;
				if(getPixelBox().height > halfThickness*ratio){
					super.draw(view);
					return;
				}
				break;
		}
		
		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());
		
		// temp fix for AWT drawing bug when rect gets too big -- GAH 2/6/98
		Rectangle compbox = view.getComponentSizeRect();
		setPixelBox(getPixelBox().intersection(compbox));

		// If the coordbox was specified with negative width or height,
		// convert pixelbox to equivalent one with positive width and height.
		// Constrain abs(width) or abs(height) by min_pixels.
		// Here I'm relying on the fact that min_pixels is positive.
		if (getCoordBox().width < 0) {
			getPixelBox().width = -Math.min(getPixelBox().width, -getMinPixelsWidth());
			getPixelBox().x -= getPixelBox().width;
		}
		else {
			getPixelBox().width = Math.max ( getPixelBox().width, getMinPixelsWidth() );
		}
		if (getCoordBox().height < 0) {
			getPixelBox().height = -Math.min(getPixelBox().height, -getMinPixelsHeight());
			getPixelBox().y -= getPixelBox().height;
		}
		else {
			getPixelBox().height = Math.max ( getPixelBox().height, getMinPixelsHeight() );
		}

		// draw the box
		g.fillRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height);

	}
}
