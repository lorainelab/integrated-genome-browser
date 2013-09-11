package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 *
 * @author hiralv
 */
public final class CoordFloaterGlyph extends Glyph implements FloaterGlyph {

	private final LinearTransform childtrans = new LinearTransform();
	Rectangle pixelRect = new Rectangle();
	Rectangle2D.Double internalPickRect = new Rectangle2D.Double();
	
	@Override
	public void drawTraversal(ViewI view) {
		LinearTransform vtrans = view.getTransform();
		setChildTransform(view);
		super.drawTraversal(view);
		view.setTransform(vtrans);
	}

	@Override
	public boolean intersects(Rectangle2D.Double rect, ViewI view) {
		return isVisible();
	}

	@Override
	public boolean withinView(ViewI view) {
		return true;
	}
	
	@Override
	public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList, ViewI view) {
		LinearTransform vtrans = view.getTransform();
		view.transformToPixels(pickRect, pixelRect);
		setChildTransform(view);
		view.transformToCoords(pixelRect, internalPickRect);
		super.pickTraversal(internalPickRect, pickList, view);
		view.setTransform(vtrans);
	}

	private void setChildTransform(ViewI view) {
		LinearTransform vtrans = view.getTransform();
		double height = view.getCoordBox().height;
		if (getChildren() != null) {
			for (GlyphI child : getChildren()) {
				height += child.getCoordBox().height;
			}
		}

		childtrans.setTransform(vtrans.getScaleX(), 0, 0, view.getPixelBox().getHeight() / height, vtrans.getTranslateX(), vtrans.getTranslateY());
		view.setTransform(childtrans);
		Rectangle2D.Double vbox = view.getCoordBox();
		Rectangle2D.Double coordbox = new Rectangle2D.Double(vbox.x, 0, vbox.width, height);
		setCoordBox(coordbox);
	}
	
	@Override
	public void removeAllChildren(){
		super.removeAllChildren();
		setCoords(0, 0, 0, 0);
	}
	
	@Override
	public void addChild(GlyphI child){
		super.addChild(child);
		setCoords(getCoordBox().x, 0, getCoordBox().width, getCoordBox().height + child.getCoordBox().height);
	}
	
	@Override
	public void checkBounds(GlyphI gl, ViewI view) {
		Rectangle2D.Double mapbox = getCoordBox(); //If this is used then glyph snaps back at top when something changes.
//		Rectangle2D.Double mapbox = view.getCoordBox(); //If this is used then glyph cannot be moved at the end.
		Rectangle2D.Double gbox = gl.getCoordBox();
		if (gbox.y < mapbox.y) {
			gl.setCoords(gbox.x, mapbox.y, gbox.width, gbox.height);
		} else if (gbox.y > (mapbox.y + mapbox.height - gbox.height)) {
			gl.setCoords(gbox.x, mapbox.y + mapbox.height - gbox.height, gbox.width, gbox.height);
		}
	}
	
	@Override
	public Rectangle2D.Double getFloatCoords(Glyph glyph, ViewI view) {
		return glyph.getCoordBox();
	}

	@Override
	public Rectangle2D.Double getUnfloatCoords(Glyph glyph, ViewI view) {
		return glyph.getCoordBox();
	}
}
