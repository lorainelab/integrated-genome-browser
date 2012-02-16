package com.affymetrix.igb.glyph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.StyleGlyphI;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;

public abstract class AbstractViewModeGlyph extends ViewModeGlyph implements StyleGlyphI {
	protected ITrackStyleExtended style;
	protected Direction direction = Direction.NONE;
	/*
	 * other_fill_color is derived from fill_color whenever setFillColor() is called.
	 * if there are any "middle" glyphs, then background is drawn with other_fill_color and
	 *    middle glyphs are drawn with fill_color
	 * if no "middle" glyphs, then background is drawn with fill_color
	 */
	protected Color other_fill_color = null;
	/** glyphs to be drawn in the "middleground" --
	 *    in front of the solid background, but behind the child glyphs
	 *    For example, to indicate how much of the xcoord range has been covered by feature retrieval attempts
	 */
	protected final List<GlyphI> middle_glyphs = new ArrayList<GlyphI>();
	protected String label = null;

	@Override
	public ITrackStyleExtended getAnnotStyle() {
		return style;
	}

	public void setStyle(ITrackStyleExtended style) {
		this.style = style;

		// most tier glyphs ignore their foreground color, but AffyTieredLabelMap copies
		// the fg color to the TierLabel glyph, which does pay attention to that color.
		setForegroundColor(style.getForeground());
		setFillColor(style.getBackground());

		setVisibility(!style.getShow());
		setLabel(style.getTrackName());
	}

	@Override
	public Direction getDirection() {
		return direction;
	}

	/**
	 *  Sets direction.  Must be one of DIRECTION_FORWARD, DIRECTION_REVERSE,
	 *  DIRECTION_BOTH or DIRECTION_NONE.
	 */
	@Override
	public void setDirection(Direction d) {
		direction = d;
	}

	@Override
	public void setLabel(String str) {
		label = str;
	}

	@Override
	public String getLabel() {
		return label;
	}

	/** Returns the color used to draw the tier background, or null
	if there is no background. */
	public Color getFillColor() {
		return style.getBackground();
	}

	/** Sets the color used to fill the tier background, or null if no color
	 *  @param col  A color, or null if no background color is desired.
	 */
	@Override
	public void setFillColor(Color col) {
		if (style.getBackground() != col) {
			style.setBackground(col);
		}

		// Now set the "middleground" color based on the fill color
		if (col == null) {
			other_fill_color = Color.DARK_GRAY;
		} else {
			int intensity = col.getRed() + col.getGreen() + col.getBlue();
			if (intensity == 0) {
				other_fill_color = Color.darkGray;
			} else if (intensity > (255 + 127)) {
				other_fill_color = col.darker();
			} else {
				other_fill_color = col.brighter();
			}
		}
	}

	protected void drawMiddle(ViewI view) {
		view.transformToPixels(getCoordBox(), getPixelBox());

		getPixelBox().width = Math.max(getPixelBox().width, getMinPixelsWidth());
		getPixelBox().height = Math.max(getPixelBox().height, getMinPixelsHeight());

		Graphics g = view.getGraphics();
		Rectangle vbox = view.getPixelBox();
		setPixelBox(getPixelBox().intersection(vbox));

		if (middle_glyphs.isEmpty()) { // no middle glyphs, so use fill color to fill entire tier
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				//Hack : Add one to height to resolve black line bug.
				g.fillRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height+1);
			}
		} else {
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				//Hack : Add one to height to resolve black line bug.
				g.fillRect(getPixelBox().x, getPixelBox().y, 2 * getPixelBox().width, getPixelBox().height+1);
			}

			// cycle through "middleground" glyphs,
			//   make sure their coord box y and height are set to same as TierGlyph,
			//   then call mglyph.draw(view)
			// TODO: This will draw middle glyphs on the Whole Genome, which appears to cause problems due to coordinates vs. pixels
			// See bug 3032785
			if(other_fill_color != null){
				for (GlyphI mglyph : middle_glyphs) {
					Rectangle2D.Double mbox = mglyph.getCoordBox();
					mbox.setRect(mbox.x, getCoordBox().y, mbox.width, getCoordBox().height);
					mglyph.setColor(other_fill_color);
					mglyph.drawTraversal(view);
				}
			}
		}
	}

	/**
	 *  Adds "middleground" glyphs, which are drawn in front of the background but
	 *    behind all "real" child glyphs.
	 *  These are generally not considered children of
	 *    the glyph.  The TierGlyph will render these glyphs, but they can't be selected since they
	 *    are not considered children in pickTraversal() method.
	 *  The only way to remove these is via removeAllChildren() method,
	 *    there is currently no external access to them.
	 */
	@Override
	public void addMiddleGlyph(GlyphI gl) {
		middle_glyphs.add(gl);
	}
}
