package com.affymetrix.igb.glyph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.bioviews.ViewI;

/**
 * Creating special class for BAM insertion glyph to be able to draw at low zoom.
 * @author hiralv
 */
public class InsertionSeqGlyph extends SequenceGlyph {

	public boolean packerClip = false;
	private Color bgcolor = Color.white;

	@Override
	public void draw(ViewI view) {
		if (packerClip) {
			return;	// don't draw residues
		}

		Rectangle pixelbox = view.getScratchPixBox();
		view.transformToPixels(this.getCoordBox(), pixelbox);

		pixelbox = fixAWTBigRectBug(view, pixelbox);

		pixelbox.width = Math.max(pixelbox.width, min_pixels_width);
		pixelbox.height = Math.max(pixelbox.height, min_pixels_height);

		Graphics g = view.getGraphics();
		g.setColor(getColor());
		g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
		if (pixelbox.width > 2 && pixelbox.height > 2) {
			g.setColor(bgcolor);
			g.fillRect(pixelbox.x + 1, pixelbox.y + 1, pixelbox.width - 2, pixelbox.height - 2);
		}

		super.draw(view);
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
