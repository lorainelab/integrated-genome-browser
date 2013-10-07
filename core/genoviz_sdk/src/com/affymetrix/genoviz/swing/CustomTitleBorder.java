package com.affymetrix.genoviz.swing;

/**
 *
 * @author dcnorris
 */
import java.awt.*;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;

/**
 * The source code is the same as TitledBorder in JDK 1.4.2 with some small
 * changes to customize the paint method to allow hyper link styled text to be
 * appended to end.
 */
public class CustomTitleBorder extends AbstractBorder {

	private static final long serialVersionUID = 1L;
	protected String title;
	protected String linkText;
	protected Border border;
	protected int titlePosition;
	protected int titleJustification;
	protected Font titleFont;
	protected Color titleColor;
	private Point textLoc = new Point();
	static public final int DEFAULT_POSITION = 0;
	static public final int ABOVE_TOP = 1;
	static public final int TOP = 2;
	static public final int BELOW_TOP = 3;
	static public final int ABOVE_BOTTOM = 4;
	static public final int BOTTOM = 5;
	static public final int BELOW_BOTTOM = 6;
	static public final int DEFAULT_JUSTIFICATION = 0;
	static public final int LEFT = 1;
	static public final int CENTER = 2;
	static public final int RIGHT = 3;
	static public final int LEADING = 4;
	static public final int TRAILING = 5;
	static protected final int EDGE_SPACING = 2;
	static protected final int TITLE_MARGIN = 5;
	static protected final int TEXT_SPACING = 2;
	static protected final int TEXT_INSET_H = 5;

	public CustomTitleBorder(String title, String link) {
		this(null, title, link, LEADING, TOP, null, null);
	}

	public CustomTitleBorder(Border border, String title, String link,
			int titleJustification, int titlePosition,
			Font titleFont, Color titleColor) {

		this.title = title;
		this.border = border;
		this.titleFont = titleFont;
		this.titleColor = titleColor;
		this.linkText = " " + link + "  "; //add spacing
		setTitleJustification(titleJustification);
		setTitlePosition(titlePosition);
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Border border = getBorder();
		String title = getTitle();
		String link = getLink();
		String emptyTitle = "TEMP";

		if (title == null || title.length() == 0 && link.length() == 0) {
			if (border != null) {
				border.paintBorder(c, g, x, y, width, height);
			}
			return;
		}
		if (title.length() == 0 || title.equals(" ")) {
			title = emptyTitle;
		}
		Rectangle grooveRect = new Rectangle(x + EDGE_SPACING, y
				+ EDGE_SPACING, width - (EDGE_SPACING << 1), height
				- (EDGE_SPACING << 1));
		Font font = g.getFont();
		Color color = g.getColor();

		g.setFont(getFont(c));

		FontMetrics fm = g.getFontMetrics();
		int fontHeight = fm.getHeight();
		int descent = fm.getDescent();
		int ascent = fm.getAscent();
		int diff;
		int stringWidth;
		if (title.equals(emptyTitle)) {
			stringWidth = fm.stringWidth(linkText);
		} else {
			stringWidth = fm.stringWidth(title + linkText);
		}
		Insets insets;

		if (border != null) {
			insets = border.getBorderInsets(c);
		} else {
			insets = new Insets(0, 0, 0, 0);
		}

		int titlePos = getTitlePosition();
		switch (titlePos) {
			case ABOVE_TOP:
				diff = ascent
						+ descent
						+ (Math.max(EDGE_SPACING, TEXT_SPACING << 1) - EDGE_SPACING);
				grooveRect.y += diff;
				grooveRect.height -= diff;
				textLoc.y = grooveRect.y - (descent + TEXT_SPACING);
				break;
			case TOP:
			case DEFAULT_POSITION:
				diff = Math.max(0, ((ascent >> 1) + TEXT_SPACING)
						- EDGE_SPACING);
				grooveRect.y += diff;
				grooveRect.height -= diff;
				textLoc.y = (grooveRect.y - descent)
						+ ((insets.top + ascent + descent) >> 1);
				break;
			case BELOW_TOP:
				textLoc.y = grooveRect.y + insets.top + ascent
						+ TEXT_SPACING;
				break;
			case ABOVE_BOTTOM:
				textLoc.y = (grooveRect.y + grooveRect.height)
						- (insets.bottom + descent + TEXT_SPACING);
				break;
			case BOTTOM:
				grooveRect.height -= fontHeight >> 1;
				textLoc.y = ((grooveRect.y + grooveRect.height) - descent)
						+ (((ascent + descent) - insets.bottom) >> 1);
				break;
			case BELOW_BOTTOM:
				grooveRect.height -= fontHeight;
				textLoc.y = grooveRect.y + grooveRect.height + ascent
						+ TEXT_SPACING;
				break;
		}

		int justification = getTitleJustification();
		if (c.getComponentOrientation().isLeftToRight()) {
			if (justification == LEADING
					|| justification == DEFAULT_JUSTIFICATION) {
				justification = LEFT;
			} else if (justification == TRAILING) {
				justification = RIGHT;
			}
		} else {
			if (justification == LEADING
					|| justification == DEFAULT_JUSTIFICATION) {
				justification = RIGHT;
			} else if (justification == TRAILING) {
				justification = LEFT;
			}
		}

		switch (justification) {
			case LEFT:
				textLoc.x = grooveRect.x + TEXT_INSET_H + insets.left;
				break;
			case RIGHT:
				textLoc.x = (grooveRect.x + grooveRect.width)
						- (stringWidth + TEXT_INSET_H + insets.right);
				break;
			case CENTER:
				textLoc.x = grooveRect.x
						+ ((grooveRect.width - stringWidth) >> 1);
				break;
		}

		// If title is positioned in middle of border AND its fontsize
		// is greater than the border's thickness, we'll need to paint 
		// the border in sections to leave space for the component's background 
		// to show through the title.
		//
		if (border != null) {
			if (((titlePos == TOP || titlePos == DEFAULT_POSITION)
					&& (grooveRect.y > textLoc.y - ascent))
					|| (titlePos == BOTTOM
					&& (grooveRect.y + grooveRect.height < textLoc.y + descent))) {

				Rectangle clipRect = new Rectangle();

				// save original clip
				Rectangle saveClip = g.getClipBounds();

				// paint strip left of text
				clipRect.setBounds(saveClip);
				if (computeIntersection(clipRect, x, y, textLoc.x - 1 - x, height)) {
					g.setClip(clipRect);
					border.paintBorder(c, g, grooveRect.x, grooveRect.y,
							grooveRect.width - 5, grooveRect.height);
				}

				// paint strip right of text
				clipRect.setBounds(saveClip);
				if (computeIntersection(clipRect, textLoc.x + stringWidth + 1, y,
						x + width - (textLoc.x + stringWidth + 1), height)) {
					g.setClip(clipRect);
					border.paintBorder(c, g, grooveRect.x, grooveRect.y,
							grooveRect.width, grooveRect.height);
				}

				if (titlePos == TOP || titlePos == DEFAULT_POSITION) {
					// paint strip below text
					clipRect.setBounds(saveClip);
					if (computeIntersection(clipRect, textLoc.x - 1, textLoc.y + descent,
							stringWidth + 2, y + height - textLoc.y - descent)) {
						g.setClip(clipRect);
						border.paintBorder(c, g, grooveRect.x, grooveRect.y,
								grooveRect.width, grooveRect.height);
					}

				} else { // titlePos == BOTTOM
					// paint strip above text
					clipRect.setBounds(saveClip);
					if (computeIntersection(clipRect, textLoc.x - 1, y,
							stringWidth + 2, textLoc.y - ascent - y)) {
						g.setClip(clipRect);
						border.paintBorder(c, g, grooveRect.x, grooveRect.y,
								grooveRect.width, grooveRect.height);
					}
				}

				// restore clip
				g.setClip(saveClip);

			} else {
				border.paintBorder(c, g, grooveRect.x, grooveRect.y,
						grooveRect.width, grooveRect.height);
			}
		}
		if (title.equals(emptyTitle)) {
			title=""; //set back to empty string
		}
		Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
		fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		Font boldUnderline = new Font("SansSerif", Font.BOLD, 13).deriveFont(fontAttributes);
		Font plainFont = new Font("SansSerif", Font.PLAIN, 13);
		AttributedString as = new AttributedString(title + link);
		if (title.length() != 0) {
			as.addAttribute(TextAttribute.FONT, plainFont, 1,
					title.length());
		}
		as.addAttribute(TextAttribute.FONT, boldUnderline, title.length(), title.length() + link.length());
		as.addAttribute(TextAttribute.FOREGROUND, Color.blue, title.length(), title.length() + link.length());
		as.addAttribute(TextAttribute.UNDERLINE,
				TextAttribute.UNDERLINE_ON, title.length() + 1, title.length() + link.length() - 2);

		g.setColor(getTitleColor());
		g.drawString(as.getIterator(), textLoc.x, textLoc.y);
		g.setFont(font);
		g.setColor(color);
	}

	/**
	 * Returns the insets of the border.
	 *
	 * @param c the component for which this border insets value applies
	 */
	@Override
	public Insets getBorderInsets(Component c) {
		return getBorderInsets(c, new Insets(0, 0, 0, 0));
	}

	/**
	 * Reinitialize the insets parameter with this Border's current Insets.
	 *
	 * @param c the component for which this border insets value applies
	 * @param insets the object to be reinitialized
	 */
	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		FontMetrics fm;
		int descent = 0;
		int ascent = 16;
		int height = 16;

		Border border = getBorder();
		if (border != null) {
			if (border instanceof AbstractBorder) {
				((AbstractBorder) border).getBorderInsets(c, insets);
			} else {
				// Can't reuse border insets because the Border interface
				// can't be enhanced.
				Insets i = border.getBorderInsets(c);
				insets.top = i.top;
				insets.right = i.right;
				insets.bottom = i.bottom;
				insets.left = i.left;
			}
		} else {
			insets.left = insets.top = insets.right = insets.bottom = 0;
		}

		insets.left += EDGE_SPACING
				+ (insets.left > 0 ? TEXT_SPACING : 0);
		insets.right += EDGE_SPACING
				+ (insets.right > 0 ? TEXT_SPACING : 0);
		insets.top += EDGE_SPACING
				+ (insets.top > 0 ? TEXT_SPACING : 0);
		insets.bottom += EDGE_SPACING
				+ (insets.bottom > 0 ? TEXT_SPACING : 0);

		if (c == null || getTitle() == null || (getTitle().length() == 0 && getLink().length() == 0)) {
			return insets;
		}
		Font font = getFont(c);
		fm = c.getFontMetrics(font);

		if (fm != null) {
			descent = fm.getDescent();
			ascent = fm.getAscent();
			height = fm.getHeight();
		}

		switch (getTitlePosition()) {
			case ABOVE_TOP:
				insets.top += ascent
						+ descent
						+ (Math.max(EDGE_SPACING, TEXT_SPACING << 1) - EDGE_SPACING);
				break;
			case TOP:
			case DEFAULT_POSITION:
				insets.top += ascent + descent;
				break;
			case BELOW_TOP:
				insets.top += ascent + descent + TEXT_SPACING;
				break;
			case ABOVE_BOTTOM:
				insets.bottom += ascent + descent + TEXT_SPACING;
				break;
			case BOTTOM:
				insets.bottom += ascent + descent;
				break;
			case BELOW_BOTTOM:
				insets.bottom += height;
				break;
		}
		return insets;
	}

	/**
	 * Returns whether or not the border is opaque.
	 */
	@Override
	public boolean isBorderOpaque() {
		return false;
	}

	/**
	 * Returns the title of the titled border.
	 */
	public String getTitle() {
		return title;
	}

	public String getLink() {
		return linkText;
	}

	/**
	 * Returns the border of the titled border.
	 */
	public Border getBorder() {
		Border b = border;
		if (b == null) {
			b = UIManager.getBorder("TitledBorder.border");
		}
		return b;
	}

	/**
	 * Returns the title-position of the titled border.
	 */
	public int getTitlePosition() {
		return titlePosition;
	}

	/**
	 * Returns the title-justification of the titled border.
	 */
	public int getTitleJustification() {
		return titleJustification;
	}

	/**
	 * Returns the title-font of the titled border.
	 */
	public Font getTitleFont() {
		Font f = titleFont;
		if (f == null) {
			f = UIManager.getFont("TitledBorder.font");
		}
		return f;
	}

	/**
	 * Returns the title-color of the titled border.
	 */
	public Color getTitleColor() {
		Color c = titleColor;
		if (c == null) {
			c = UIManager.getColor("TitledBorder.titleColor");
		}
		return c;
	}

	// REMIND(aim): remove all or some of these set methods?
	/**
	 * Sets the title of the titled border. param title the title for the border
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Sets the border of the titled border.
	 *
	 * @param border the border
	 */
	public void setBorder(Border border) {
		this.border = border;
	}

	/**
	 * Sets the title-position of the titled border.
	 *
	 * @param titlePosition the position for the border
	 */
	public void setTitlePosition(int titlePosition) {
		switch (titlePosition) {
			case ABOVE_TOP:
			case TOP:
			case BELOW_TOP:
			case ABOVE_BOTTOM:
			case BOTTOM:
			case BELOW_BOTTOM:
			case DEFAULT_POSITION:
				this.titlePosition = titlePosition;
				break;
			default:
				throw new IllegalArgumentException(titlePosition
						+ " is not a valid title position.");
		}
	}

	/**
	 * Sets the title-justification of the titled border.
	 *
	 * @param titleJustification the justification for the border
	 */
	public void setTitleJustification(int titleJustification) {
		switch (titleJustification) {
			case DEFAULT_JUSTIFICATION:
			case LEFT:
			case CENTER:
			case RIGHT:
			case LEADING:
			case TRAILING:
				this.titleJustification = titleJustification;
				break;
			default:
				throw new IllegalArgumentException(titleJustification
						+ " is not a valid title justification.");
		}
	}

	/**
	 * Sets the title-font of the titled border.
	 *
	 * @param titleFont the font for the border title
	 */
	public void setTitleFont(Font titleFont) {
		this.titleFont = titleFont;
	}

	/**
	 * Sets the title-color of the titled border.
	 *
	 * @param titleColor the color for the border title
	 */
	public void setTitleColor(Color titleColor) {
		this.titleColor = titleColor;
	}

	/**
	 * Returns the minimum dimensions this border requires in order to fully
	 * display the border and title.
	 *
	 * @param c the component where this border will be drawn
	 */
	public Dimension getMinimumSize(Component c) {
		Insets insets = getBorderInsets(c);
		Dimension minSize = new Dimension(insets.right + insets.left,
				insets.top + insets.bottom);
		Font font = getFont(c);
		FontMetrics fm = c.getFontMetrics(font);
		switch (titlePosition) {
			case ABOVE_TOP:
			case BELOW_BOTTOM:
				minSize.width = Math.max(fm.stringWidth(getTitle()),
						minSize.width);
				break;
			case BELOW_TOP:
			case ABOVE_BOTTOM:
			case TOP:
			case BOTTOM:
			case DEFAULT_POSITION:
			default:
				minSize.width += fm.stringWidth(getTitle());
		}
		return minSize;
	}

	protected Font getFont(Component c) {
		Font font;
		if ((font = getTitleFont()) != null) {
			return font;
		} else if (c != null && (font = c.getFont()) != null) {
			return font;
		}
		return new Font("Dialog", Font.PLAIN, 12);
	}

	private static boolean computeIntersection(Rectangle dest, int rx,
			int ry, int rw, int rh) {
		int x1 = Math.max(rx, dest.x);
		int x2 = Math.min(rx + rw, dest.x + dest.width);
		int y1 = Math.max(ry, dest.y);
		int y2 = Math.min(ry + rh, dest.y + dest.height);
		dest.x = x1;
		dest.y = y1;
		dest.width = x2 - x1;
		dest.height = y2 - y1;

		if (dest.width <= 0 || dest.height <= 0) {
			return false;
		}
		return true;
	}
}
