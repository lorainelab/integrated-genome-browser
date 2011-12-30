package com.affymetrix.igb.util;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Original Author: Romain Guy
 * Dual-licensed under LGPL (Sun and Romain Guy) and BSD (Romain Guy).
 * Copyright 2005 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 * Copyright (c) 2006 Romain Guy <romain.guy@mac.com>
 * All rights reserved.
 * 
 * 
 * 
 * Altered by: David Norris 
 */
public class GraphicsUtil {

    private GraphicsUtil() {
    }

    // Returns the graphics configuration for the primary screen
    private static GraphicsConfiguration getGraphicsConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration();
    }

    public static BufferedImage createCompatibleImage(BufferedImage image,
            int width, int height) {
        return getGraphicsConfiguration().createCompatibleImage(width, height,
                image.getTransparency());
    }

    public static BufferedImage resizeImage(BufferedImage image,
            int newWidth, int newHeight) {

        if (image.getWidth() == newWidth && image.getHeight() == newHeight) {
            return image;
        }

        BufferedImage temp = createCompatibleImage(image, newWidth, newHeight);
        Graphics2D g2 = temp.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2.dispose();

        return temp;
    }
}
