package com.affymetrix.igb.shared;

import java.awt.Graphics;
import java.util.BitSet;

import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genoviz.bioviews.ViewI;


/**
 *
 * @author jnicol
 *
 * A glyph that shows a sequence of aligned residues.
 * At low resolution (small scale) as a solid background rectangle
 * and at high resolution overlays the residue letters.
 *
 * Residues can be masked out if they agree with a reference sequence.
 *
 */
public final class AlignedResidueGlyph extends AbstractAlignedTextGlyph {
	//By default mask the residues.
	private boolean defaultShowMask = true;
	private static final ResidueColorHelper helper = ResidueColorHelper.getColorHelper();


	public void setDefaultShowMask(boolean show){
		defaultShowMask = show;
	}
	
	@Override
	protected boolean getShowMask(){
		Object mod = this.getInfo();
		if (mod instanceof SymWithProps) {
			SymWithProps swp = (SymWithProps)mod;
			Object show_mask = swp.getProperty(BAM.SHOWMASK);
			if(show_mask != null){
				return Boolean.parseBoolean(show_mask.toString());
			}
		}
		return defaultShowMask;
	}

	@Override
	protected void drawResidueRectangles(ViewI view, double pixelsPerBase, 
			char[] charArray, int seqBegIndex, int seqEndIndex, BitSet residueMask) {
		
		Graphics g = view.getGraphics();
		int intPixelsPerBase = (int) Math.ceil(pixelsPerBase);
		for (int j = 0; j < charArray.length; j++) {

			if(getShowMask() && !residueMask.get(j)) {
				continue;	// skip drawing of this residue
			}
			g.setColor(helper.determineResidueColor(charArray[j]));

			//Create a colored rectangle.
			//We calculate the floor of the offset as we want the offset to stay to the extreme left as possible.
			int offset = (int) (j * pixelsPerBase);
			//ceiling is done to the width because we want the width to be as wide as possible to avoid losing pixels.
			g.fillRect(getPixelBox().x + offset, getPixelBox().y, intPixelsPerBase, getPixelBox().height);
		}
	}
	
	String baseQuality;
	public void setBaseQuality(String baseQuality) {
		this.baseQuality = baseQuality;
	}
}
