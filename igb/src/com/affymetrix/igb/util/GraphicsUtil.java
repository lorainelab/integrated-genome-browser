package com.affymetrix.igb.util;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 
 * @author nick
 */
public class GraphicsUtil {
	
	public static BufferedImage getDeviceCompatibleImage(int width, int height) {
		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice screenDevice = graphicsEnvironment.getDefaultScreenDevice();
		GraphicsConfiguration graphicConfiguration = screenDevice.getDefaultConfiguration();
		BufferedImage image = graphicConfiguration.createCompatibleImage(width, height);

		return image;
	}

	public static BufferedImage resizeImage(BufferedImage image,
			int newWidth, int newHeight) {
		if (image.getWidth() == newWidth && 
				image.getHeight() == newHeight) {
			return image;
		}
		
		int type = image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType();
		BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, type);
		Graphics2D g = resizedImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, newWidth, newHeight, null);
		g.dispose();

		return resizedImage;
	}
}
