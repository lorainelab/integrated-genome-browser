package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.util.StringUtils;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.StringGlyph;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 *
 * @author hiralv
 */
public class WrappedStringGlyph extends StringGlyph {

	public WrappedStringGlyph(String str) {
		super(str);
	}
	
	@Override
	public void draw(ViewI view) {
		Graphics g = view.getGraphics();
		g.setPaintMode();
		view.transformToPixels(getCoordBox(), pixelbox);
		
		Rectangle boundingPixelBox = view.getPixelBox();
		String label = getString();
		
		if(getFont() != null){
			g.setFont(getFont());
		}
		g.setColor(getForegroundColor());
		FontMetrics fm = g.getFontMetrics();
		//int text_height = fm.getAscent() + fm.getDescent();
		int text_height = fm.getHeight();

		// Lower bound of visible glyph
		int lowerY = Math.max(pixelbox.y, boundingPixelBox.y);

		// Upper bound of visible glyph
		int upperY = Math.min(
				pixelbox.y + pixelbox.height,
				boundingPixelBox.y + boundingPixelBox.height);

		int text_width = fm.stringWidth(label);
		
		
		if (text_width > pixelbox.width) {
			drawWrappedLabel(label, fm, g, lowerY, upperY, text_height, pixelbox);
		} else {
			// if glyph's pixelbox wider than text, then center text
			pixelbox.x += pixelbox.width / 2 - text_width / 2;
			g.drawString(label, pixelbox.x, (lowerY + upperY + text_height) / 2);
		}
	}
	
	private static void drawWrappedLabel(String label, FontMetrics fm, Graphics g, int lowerY, int upperY, int text_height, Rectangle pixelbox) {
		int pbBuffer_x = 3;
		int maxLines = (upperY - lowerY) / text_height;
		if(maxLines == 0)  { return; }
		String[] lines = StringUtils.wrap(label, fm, pixelbox.width - pbBuffer_x, maxLines);
		pixelbox.x += pbBuffer_x;
		int height =  (upperY + lowerY - text_height*(lines.length - 2)) / 2;
		for (String line : lines) {
			//Remark: the "height-3" parameter in the drawString function is a fine-tune to center vertically.
			g.drawString(line, pixelbox.x, height-3);
			height += text_height;
		}
	}
}
