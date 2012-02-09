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
		view.transformToPixels(this.coordbox, this.pixelbox);
		if (this.pixelbox.width == 0) { this.pixelbox.width = 1; }
		if (this.pixelbox.height == 0) { this.pixelbox.height = 1; }
		
		int halfThickness = 1; 
		switch(this.getOrientation()){
			case HORIZONTAL:
				halfThickness = (pixelbox.height-1)/2;
				if(pixelbox.width > halfThickness*ratio){
					super.draw(view);
					return;
				}
				break;
			case VERTICAL:
				halfThickness = (pixelbox.width-1)/2;
				if(pixelbox.height > halfThickness*ratio){
					super.draw(view);
					return;
				}
				break;
		}
		
		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());
		
		// temp fix for AWT drawing bug when rect gets too big -- GAH 2/6/98
		Rectangle compbox = view.getComponentSizeRect();
		pixelbox = pixelbox.intersection(compbox);

		// If the coordbox was specified with negative width or height,
		// convert pixelbox to equivalent one with positive width and height.
		// Constrain abs(width) or abs(height) by min_pixels.
		// Here I'm relying on the fact that min_pixels is positive.
		if (coordbox.width < 0) {
			pixelbox.width = -Math.min(pixelbox.width, -getMinPixelsWidth());
			pixelbox.x -= pixelbox.width;
		}
		else pixelbox.width = Math.max ( pixelbox.width, getMinPixelsWidth() );
		if (coordbox.height < 0) {
			pixelbox.height = -Math.min(pixelbox.height, -getMinPixelsHeight());
			pixelbox.y -= pixelbox.height;
		}
		else pixelbox.height = Math.max ( pixelbox.height, getMinPixelsHeight() );

		// draw the box
		g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

	}
}
