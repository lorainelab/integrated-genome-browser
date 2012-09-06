package com.affymetrix.igb.viewmode;

import java.awt.geom.Rectangle2D;
import java.util.*;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.widget.tieredmap.PaddedPackerI;
import com.affymetrix.igb.shared.AbstractViewModeGlyph;
import com.affymetrix.igb.shared.CollapsePacker;

/**
 *  copy / modification of TierGlyph for ViewModeGlyph for sequences
 */
public class SequenceGlyph extends AbstractViewModeGlyph{
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

	private void setSpacer(double spacer) {
		this.spacer = spacer;
		((PaddedPackerI) packer).setParentSpacer(spacer);
	}

	public void setPreferredHeight(double height, ViewI view){
		height = height - 2 * getSpacing();

		if(useLabel(style)) {
			height = height / 2;
		}

		double percent = ((height * 100)/style.getHeight() - 100)/100;
		style.setHeight(height);

		scaleChildHeights(percent, getChildren(), view);
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
