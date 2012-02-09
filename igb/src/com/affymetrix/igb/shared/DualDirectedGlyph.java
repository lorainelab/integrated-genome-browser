package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.DirectedGlyph;
import com.affymetrix.igb.tiers.TrackConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author hiralv
 */
public class DualDirectedGlyph extends DirectedGlyph implements TrackConstants {
	
	private final int mix_direction_width = 2;
	private boolean isFirst = false;
	private boolean isLast = false;
	private Color startColor = Color.GREEN;
	private Color endColor = Color.RED;
	private DIRECTION_TYPE type;
	private Rectangle2D.Double stcb;
	private Rectangle2D.Double edcb;
	private int x[] = new int[6];
	private int y[] = new int[6];
	
	public DualDirectedGlyph(boolean isFirst, boolean isLast, DIRECTION_TYPE direction_type){
		super();
		setPosition(isFirst, isLast);
		setDirectionType(direction_type);
	}
	
	public final void setDirectionType(DIRECTION_TYPE type){
		this.type = type;
	}
	
	public final void setPosition(boolean isFirst, boolean isLast){
		this.isFirst = isFirst;
		this.isLast = isLast;
	}
	
	public void draw(ViewI view) {
		if(type == DIRECTION_TYPE.ARROW){
			drawArrow(view);
		}else if(type == DIRECTION_TYPE.COLOR){
			drawColored(view);
		}else{
			drawNone(view);
		}
		super.draw(view);
	}
	
	private void drawNone(ViewI view){
		view.transformToPixels(coordbox, pixelbox);

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
	
	private void drawArrow(ViewI view){
		if((isFirst && isForward()) || (isLast && !isForward()) ){
			drawNone(view);
			return;
		}
		view.transformToPixels(this.coordbox, this.pixelbox);
		if (this.pixelbox.width == 0) { this.pixelbox.width = 1; }
		if (this.pixelbox.height == 0) { this.pixelbox.height = 1; }
		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());
		int halfThickness = 1;
		if (HORIZONTAL == this.getOrientation() && this.isForward()) {
			halfThickness = (pixelbox.height-1)/2;
			x[0] = pixelbox.x;
			x[2] = pixelbox.x+pixelbox.width;
			x[1] = Math.max(x[0]+1, (x[2]-halfThickness));
			x[3] = x[1]-1;
			x[4] = x[0];
			y[0] = pixelbox.y;
			y[1] = y[0];
			y[2] = y[0] + halfThickness;
			y[3] = y[0] + pixelbox.height;
			y[4] = y[3];
		}
		else if (HORIZONTAL == this.getOrientation() && !this.isForward()) {
			halfThickness = (pixelbox.height-1)/2;
			x[0] = pixelbox.x;
			x[2] = x[0] + pixelbox.width;
			x[1] = Math.min(x[2]-1, x[0]+halfThickness);
			x[3] = x[2];
			x[4] = x[1]+1;
			y[1] = pixelbox.y;
			y[0] = y[1] + halfThickness;
			y[2] = y[1];
			y[3] = y[1] + pixelbox.height;
			y[4] = y[3];
		}
		else if (VERTICAL == this.getOrientation() && this.isForward()) {
			halfThickness = (pixelbox.width-1)/2;
			x[0] = pixelbox.x;
			x[1] = pixelbox.x+pixelbox.width;
			x[3] = x[0] + halfThickness;
			x[2] = x[1];
			x[4] = x[0];
			y[0] = pixelbox.y;
			y[1] = y[0];
			y[3] = y[0] + pixelbox.height;
			y[2] = Math.max(y[3]-halfThickness, y[0])-1;
			y[4] = y[2];
		}
		else if (VERTICAL == this.getOrientation() && !this.isForward()) {
			halfThickness = (pixelbox.width)/2;
			x[0] = pixelbox.x + pixelbox.width;
			x[1] = pixelbox.x;
			x[2] = x[1];
			x[4] = x[0];
			x[3] = x[1] + halfThickness;
			y[3] = pixelbox.y;
			y[0] = y[3]+pixelbox.height;
			y[1] = y[0];
			y[2] = Math.min(y[3]+halfThickness, y[0]);
			y[4] = y[2];
		}
		g.fillPolygon(x, y, 5);
	}
	
	public void setCoords(double x, double y, double width, double height) {
		super.setCoords(x, y, width, height);
		if (HORIZONTAL == this.getOrientation()) {
			if(isForward()){
				stcb = new Rectangle2D.Double(coordbox.x, coordbox.y, 3, coordbox.height);
				edcb = new Rectangle2D.Double(coordbox.x + coordbox.width - 3, coordbox.y, 3, coordbox.height);
			}else{
				stcb = new Rectangle2D.Double(coordbox.x + coordbox.width - 3, coordbox.y, 3, coordbox.height);
				edcb = new Rectangle2D.Double(coordbox.x, coordbox.y, 3, coordbox.height);
			}
		}
	}
	
	private void drawColored(ViewI view){
		drawNone(view);
		if (view.getTransform().getScaleX() < 0.1) {
			return;
		}
		
		Graphics g = view.getGraphics();
		Rectangle pb = new Rectangle();
		if (HORIZONTAL == this.getOrientation() && this.isForward()) {
			if(isFirst){
				view.transformToPixels(stcb, pb);
				g.setColor(startColor);
				g.fillRect(pb.x, pixelbox.y, Math.max(mix_direction_width, pb.width), pixelbox.height);
			}
			
			if(isLast){
				view.transformToPixels(edcb, pb);
				g.setColor(endColor);
				g.fillRect(pb.x, pixelbox.y, Math.max(mix_direction_width, pb.width), pixelbox.height);
			}
		}
		else if (HORIZONTAL == this.getOrientation() && !this.isForward()) {
			if(isFirst){
				view.transformToPixels(stcb, pb);
				g.setColor(endColor);
				g.fillRect(pb.x, pixelbox.y, Math.max(mix_direction_width, pb.width), pixelbox.height);
			}
			
			if(isLast){
				view.transformToPixels(edcb, pb);
				g.setColor(startColor);
				g.fillRect(pb.x, pixelbox.y, Math.max(mix_direction_width, pb.width), pixelbox.height);
			}
		}
		else if (VERTICAL == this.getOrientation() && this.isForward()) {}
		else if (VERTICAL == this.getOrientation() && !this.isForward()) {}
	}
}
