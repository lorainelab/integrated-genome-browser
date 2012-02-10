package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.*;

/**
 * A glyph used to display a text string.
 * Not to be confused with {@link LabelGlyph}
 * (which is used to label other glyphs with text).
 *
 * <p> There are currently some placement problems with StringGlyph.
 * If placement needs to be anything other than {@link NeoConstants#CENTER},
 * {@link NeoConstants#LEFT}, or {@link NeoConstants#WEST},
 * a LabelGlyph may be a better choice.
 */
public class StringGlyph extends SolidGlyph implements NeoConstants  {

	private static final boolean DEBUG_PIXELBOX = false;
	final static Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

	private Rectangle debug_rect;

	private String str;
	private Font fnt = DEFAULT_FONT;
	private int hPlacement, vPlacement;
	private boolean show_background = false;


	@Override
	public String toString() {
		return ("StringGlyph: string: \""+str+"\"  +coordbox: "+getCoordBox());
	}

	public StringGlyph (String str) {
		this();
		this.str = str;
	}

	public StringGlyph () {
		hPlacement = CENTER;
		vPlacement = CENTER;
		if (DEBUG_PIXELBOX) {
			debug_rect = new Rectangle();
		}
	}

	public void setString (String str) {
		this.str = str;
	}
	public String getString () {
		return str;
	}

	public void setShowBackground(boolean show) {
		show_background = show;
	}
	public boolean getShowBackground() {
		return show_background;
	}

	@Override
	public void draw(ViewI view) {
		Graphics g = view.getGraphics();
		g.setPaintMode();
		if ( null != fnt ) {
			g.setFont(fnt);
		}
		FontMetrics fm = g.getFontMetrics();
		int text_width = 0;
		if ( null != str ) {
			text_width = fm.stringWidth(str);
		}
		// int text_height = fm.getAscent() + fm.getDescent();
		int text_height = fm.getAscent();
		int blank_width = fm.charWidth ('z')*2;

		view.transformToPixels(getCoordBox(), getPixelBox());
		if (DEBUG_PIXELBOX) {
			debug_rect.setBounds(getPixelBox().x, getPixelBox().y,
					getPixelBox().width, getPixelBox().height);
		}
		if (hPlacement == LEFT) {
		}
		else if (hPlacement == RIGHT) {
			getPixelBox().x += getPixelBox().width + blank_width;
		}
		else {
			getPixelBox().x += getPixelBox().width/2 - text_width/2;
		}
		
		if (vPlacement == ABOVE) {
		}
		else if (vPlacement == BELOW) {
			getPixelBox().y += getPixelBox().height;
		}
		else {
			getPixelBox().y += getPixelBox().height/2 + text_height/2;
		}
		getPixelBox().width = text_width;
		getPixelBox().height = text_height+1; // +1 for an extra pixel below the text
		// so letters like 'g' still have at
		// least one pixel below them

		if( getShowBackground() ) { // show background
			Color bgc = getBackgroundColor();
			if ( null != bgc ) {
				g.setColor( getBackgroundColor() );
				g.fillRect( getPixelBox().x, getPixelBox().y - getPixelBox().height,
						getPixelBox().width, getPixelBox().height);
			}
		}


		if ( null != str ) {
			// display string
			g.setColor( getForegroundColor() );
			// define adjust such that: ascent-adjust = descent+adjust
			// (But see comment above about the extra -1 pixel)
			//			int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0) -1;
			// changed from -1 to +2 so descent is overhanging (rather than ascent)
			int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0) + 2;
			g.drawString (str, getPixelBox().x, getPixelBox().y -getPixelBox().height/2 + adjust);
		}

		if (DEBUG_PIXELBOX) {
			// testing pixbox...
			g.setColor(Color.red);
			g.drawRect(getPixelBox().x, getPixelBox().y, getPixelBox().width, getPixelBox().height);
			g.setColor(Color.yellow);
			g.drawRect(debug_rect.x, debug_rect.y,
					debug_rect.width, debug_rect.height);
		}
		super.draw(view);
	}

	/** Sets the font.  If you attemt to set the font to null, it will set itself
	 *  to a default font.
	 */
	@Override
	public void setFont(Font f) {
		if (f==null) {
			this.fnt = DEFAULT_FONT;
		} else {
			this.fnt = f;
		}
	}

	@Override
	public Font getFont() {
		return this.fnt;
	}

	public void setHorizontalPlacement(int placement){
		hPlacement = placement;
	}
	
	public int getHorizontalPlacement() {
		return hPlacement;
	}
	
	public void setVerticalPlacement(int placement) {
		vPlacement = placement;
	}

	public int getVerticalPlacement() {
		return vPlacement;
	}
	

	/**
	 * @deprecated use {@link #setForegroundColor}.
	 * Also see {@link #setBackgroundColor}.
	 */
	@Deprecated
		public void setColor( Color c ) {
			setForegroundColor( c );
		}

	/**
	 * @deprecated use {@link #getForegroundColor}.
	 * Also see {@link #setBackgroundColor}.
	 */
	@Deprecated
		public Color getColor() {
			return getForegroundColor();
		}

}
