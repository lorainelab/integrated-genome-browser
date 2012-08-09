package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.widget.tieredmap.PaddedPackerI;
import com.affymetrix.igb.shared.CollapsePacker;
import com.affymetrix.igb.shared.ViewModeGlyph;

import java.awt.Color;
import java.util.*;
import java.awt.geom.Rectangle2D;

/**
 *  copy / modification of TierGlyph for ViewModeGlyph for sequences
 */
public class SequenceGlyph extends ViewModeGlyph {
	// extending solid glyph to inherit hit methods (though end up setting as not hitable by default...)
	private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		PREFERENCES = Collections.unmodifiableMap(temp);
	}

	private double spacer = 2;

	private CollapsePacker packer = new CollapsePacker();
	
	public SequenceGlyph(ITrackStyleExtended style) {
		super();
		setHitable(false);
		setSpacer(spacer);
		setPacker(packer);
		setStyle(style);
	}

	@Override
	public void addChild(GlyphI glyph, int position) {
		throw new RuntimeException("SequenceGlyph.addChild(glyph, position) not allowed, "
				+ "use SequenceGlyph.addChild(glyph) instead");
	}

	// overriding pack to ensure that tier is always the full width of the scene
	@Override
	public void pack(ViewI view) {
		super.pack(view);
		Rectangle2D.Double mbox = getScene().getCoordBox();
		Rectangle2D.Double cbox = this.getCoordBox();

		this.setCoords(mbox.x, cbox.y, mbox.width, cbox.height);
	}

	/**
	 *  Overridden to allow background shading by a collection of non-child
	 *    "middleground" glyphs.  These are rendered after the solid background but before
	 *    all of the children (which could be considered the "foreground").
	 */
	@Override
	public void draw(ViewI view) {
		//drawMiddle(view);
		super.draw(view);
	}

	private void setSpacer(double spacer) {
		this.spacer = spacer;
		((PaddedPackerI) packer).setParentSpacer(spacer);
	}

	// very, very deprecated
	@Override
	public Color getColor() {
		return getForegroundColor();
	}

	// very, very deprecated
	@Override
	public void setColor(Color c) {
		setForegroundColor(c);
	}

	@Override
	public void setForegroundColor(Color color) {
		if (style.getForeground() != color) {
			style.setForeground(color);
		}
	}

	@Override
	public Color getForegroundColor() {
		return style.getForeground();
	}

	@Override
	public void setBackgroundColor(Color color) {
		setFillColor(color);
	}

	@Override
	public Color getBackgroundColor() {
		return getFillColor();
	}

	@Override
	public int getActualSlots() {
		return 1;
	}

	public void setPreferredHeight(double height, ViewI view){
		height = height - 2 * getSpacing();

		if(useLabel()) {
			height = height / 2;
		}

		double percent = ((height * 100)/style.getHeight() - 100)/100;
		style.setHeight(height);

		setChildHeight(percent, getChildren(), view);
	}

	private static void setChildHeight(double percent, List<GlyphI> sibs, ViewI view){
		int sibs_size = sibs.size();

		GlyphI child;
		Rectangle2D.Double coordbox;
		for (int i = 0; i < sibs_size; i++) {
			child =  sibs.get(i);
			coordbox = child.getCoordBox();
			child.setCoords(coordbox.x, 0, coordbox.width, coordbox.height + (coordbox.height * percent));
			if(child.getChildCount() > 0){
				setChildHeight(percent, child.getChildren(), view);
			}
			child.pack(view);
		}

	}

	private boolean useLabel() {
		String label_field = style.getLabelField();
		boolean use_label = label_field != null && (label_field.trim().length() > 0);
		if (use_label) {
			return true;
		}

		return false;
	}

	/** Not implemented.  Will behave the same as drawSelectedOutline(ViewI). */
	@Override
	protected void drawSelectedFill(ViewI view) {
		this.drawSelectedOutline(view);
	}

	/** Not implemented.  Will behave the same as drawSelectedOutline(ViewI). */
	@Override
	protected void drawSelectedReverse(ViewI view) {
		this.drawSelectedOutline(view);
	}

	@Override
	public Map<String, Class<?>> getPreferences() {
		return new HashMap<String, Class<?>>(PREFERENCES);
	}

	@Override
	public void setPreferences(Map<String, Object> preferences) {
	}
}
