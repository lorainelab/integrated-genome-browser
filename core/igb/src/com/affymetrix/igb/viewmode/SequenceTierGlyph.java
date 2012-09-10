package com.affymetrix.igb.viewmode;

import java.awt.geom.Rectangle2D;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.AbstractTierGlyph;

/**
 *  copy / modification of TierGlyph for ViewModeGlyph for sequences
 */
public class SequenceTierGlyph extends AbstractTierGlyph{

	public SequenceTierGlyph(ITrackStyleExtended style) {
		super();
		style.setSeparable(false);
		style.setSeparate(false);
		setHitable(false);
		setStyle(style);
	}

	// overriding pack to ensure that tier is always the full width of the scene
	@Override
	public void pack(ViewI view) {
		super.pack(view);
		Rectangle2D.Double mbox = getScene().getCoordBox();
		Rectangle2D.Double cbox = this.getCoordBox();

		this.setCoords(mbox.x, cbox.y, mbox.width, cbox.height);
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

}
