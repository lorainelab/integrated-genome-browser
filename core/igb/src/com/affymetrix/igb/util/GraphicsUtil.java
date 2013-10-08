package com.affymetrix.igb.util;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

/**
 * A Graphics Util class for IGB, it is designed to add and implement all the
 * graphic related methods and use globally.
 *
 * @author nick
 */
public class GraphicsUtil {

	/**
	 * Get device compatible image by passed width and height.
	 */
	public static BufferedImage getDeviceCompatibleImage(int width, int height) {
		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice screenDevice = graphicsEnvironment.getDefaultScreenDevice();
		GraphicsConfiguration graphicConfiguration = screenDevice.getDefaultConfiguration();
		BufferedImage image = graphicConfiguration.createCompatibleImage(width, height);
				//, Transparency.TRANSLUCENT);

		return image;
	}

	/**
	 * Resize the passed image by passed width and height.
	 */
	public static BufferedImage resizeImage(BufferedImage image,
			int width, int height) {
		if (image.getWidth() == width
				&& image.getHeight() == height) {
			return image;
		}

		int type = image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType();
		BufferedImage resizedImage = new BufferedImage(width, height, type);
		Graphics2D g = resizedImage.createGraphics();
//		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, width, height, null);
		g.dispose();

		return resizedImage;
	}
}
