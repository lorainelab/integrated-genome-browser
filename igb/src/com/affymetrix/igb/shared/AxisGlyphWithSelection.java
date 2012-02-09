package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;

import java.awt.geom.Rectangle2D;

/**
 *
 * @author hiralv
 */
public class AxisGlyphWithSelection extends AxisGlyph {
	private GlyphI sel_glyph;

	public AxisGlyphWithSelection(){
		super();
		setSelectable(true);
	}

	public AxisGlyphWithSelection(int orientation){
		super(orientation);
		setSelectable(true);
	}
	
	public boolean supportsSubSelection() {
		return true;
	}

	public Rectangle2D.Double getSelectedRegion() {
		if (sel_glyph == null) {
			if (isSelected()) {
				return this.getCoordBox();
			}
			else {
				return null;
			}
		}
		return sel_glyph.getCoordBox();
	}
	
	/**
	 * Calls super.setCoords and resets the reference space.
	 * Also resets the coords for all the children.
	 */
	public void setCoords(double x, double y, double width, double height) {
		super.setCoords(x, y, width, height);
		if (getChildren() != null) {
			GlyphI child;
			Rectangle2D.Double childbox;
			for (int i=0; i<getChildren().size(); i++) {
				child = getChildren().get(i);
				childbox = child.getCoordBox();
				child.setCoords(childbox.x, y, childbox.width, height);
			}
		}
		if (sel_glyph != null) {
			Rectangle2D.Double selbox = sel_glyph.getCoordBox();
			sel_glyph.setCoords(selbox.x, Math.max(0, y + labelThickness), selbox.width, height + labelThickness);
		}
	}

	/**
	 * This turns around and calls setCoords.
	 */
	public void setCoordBox( Rectangle2D.Double theBox ) {
		super.setCoordBox(theBox);
		setCoords( theBox.x, theBox.y, theBox.width, theBox.height );
	}

	/**
	 * Overriding glyph.select(x,y,width,height) to ignore y & height.
	 * Just use x start and end (x+width).
	 * Should probably go in a LinearGlyph superclass...
	 */
	public void select(double x, double y, double width, double height) {
		if (orient == HORIZONTAL) {
			select(x, x+width);
		} else if (orient == VERTICAL) {
			select(y, y+height);
		}
	}

	/**
	 * @see #select(int, int)
	 */
	public void select (double start, double end) {
		select((int)start, (int)end);
	}
	
	/**
	 * Selects a range of residues.
	 *
	 * @param start the first residue to be selected.
	 * @param end the last residue to be selected.
	 */
	public void select(int start, int end) {
		setSelected(true);
		if (end >= start) { end += 1; }
		else { start += 1; }
		if (sel_glyph == null) {
			sel_glyph = new TransientFillRectGlyph();
		}
		if (orient == HORIZONTAL) {
			if (start <= end) {
				if (start < coordbox.x) {
					start = (int)coordbox.x; }
				if (end > (coordbox.x + coordbox.width)) {
					end = (int)(coordbox.x + coordbox.width); }
			}
			else {
				if (end < coordbox.x) {
					end = (int)coordbox.x; }
				if (start > (coordbox.x + coordbox.width)) {
					start = (int)(coordbox.x + coordbox.width); }
			}
			sel_glyph.setCoords(start, Math.max(0, coordbox.y - labelThickness), end-start, coordbox.height + labelThickness);
		} else if (orient == VERTICAL) {
			if (start <= end) {
				if (start < coordbox.y) {
					start = (int)coordbox.y; }
				if (end > (coordbox.y + coordbox.height)) {
					end = (int)(coordbox.y + coordbox.height); }
			}
			else {
				if (end < coordbox.y) {
					end = (int)coordbox.y; }
				if (start > (coordbox.y + coordbox.height)) {
					start = (int)(coordbox.y + coordbox.height); }
			}
			sel_glyph.setCoords(Math.max(0, coordbox.x - labelThickness), start, coordbox.width + labelThickness, end-start);
		}
	}

	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		if (!isSelected()) {
			sel_glyph = null;
		}
	}

	@Override
	protected void drawSelectedOutline(ViewI view) {
		if (sel_glyph != null)  {
			draw(view);
			sel_glyph.setBackgroundColor(view.getScene().getSelectionColor());
			sel_glyph.drawTraversal(view);
		}
		else {
			super.drawSelectedOutline(view);
		}
	}

	private static class TransientFillRectGlyph extends FillRectGlyph {

		public void drawTraversal(ViewI view) {
			view.getGraphics().setXORMode(view.getComponent().getBackground());
			super.drawTraversal(view);
			view.getGraphics().setPaintMode();
		}
	}
}
