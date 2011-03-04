package com.affymetrix.igb.util;

import com.affymetrix.igb.IGB;
import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.swing.ImageIcon;

/**
 *
 * @author hiralv
 */
public class IGBUtils {

	/** Returns the icon stored in the jar file.
	 *  It is expected to be at com.affymetrix.igb
	 *  @return null if the image file is not found or can't be opened.
	 */
	public static ImageIcon getIcon(String name) {
		ImageIcon icon = null;
		try {
			URL url = IGB.class.getResource(name);
			if (url != null) {
				return new ImageIcon(url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// It isn't a big deal if we can't find the icon, just return null
		}
		return icon;
	}

	public static Cursor createCursor(String name, Cursor cursor) {
		ImageIcon imageIcon = getIcon(name);
		if (imageIcon != null) {
			BufferedImage iconImage = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = iconImage.createGraphics();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
			Rectangle2D.Double rect = new Rectangle2D.Double(0, 0, 32, 32);
			g.fill(rect);

			g = iconImage.createGraphics();
			g.drawImage(imageIcon.getImage(), 0, 0, null);
			cursor = Toolkit.getDefaultToolkit().createCustomCursor(iconImage, new Point(0, 0), name);
		}
		return cursor;
	}

}
