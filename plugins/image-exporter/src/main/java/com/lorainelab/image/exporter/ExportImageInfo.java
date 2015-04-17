package com.lorainelab.image.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class ExportImageInfo {

    private static final Logger logger = LoggerFactory.getLogger(ExportImageInfo.class);
    public static final int DEFAULT_IMAGE_HEIGHT = 800;
    public static final int DEFAULT_IMAGE_WIDTH = 600;
    public static final int DEFAULT_RESOLUTION = 300;

    private double width;
    private double height;
    private int resolution;

    public ExportImageInfo() {
        this(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, DEFAULT_RESOLUTION);
    }

    public ExportImageInfo(double w, double h) {
        this(w, h, DEFAULT_RESOLUTION);
    }

    public ExportImageInfo(double w, double h, int r) {
        width = w;
        height = h;
        resolution = r;
        validateDimensions();
    }

    public void reset(int w, int h, int r) {
        width = w;
        height = h;
        resolution = r;
        validateDimensions();
    }

    private void validateDimensions() {
        if (width <= 0) {
            width = DEFAULT_IMAGE_WIDTH;
        }
        if (height <= 0) {
            height = DEFAULT_IMAGE_HEIGHT;
        }
        if (resolution <= 0) {
            resolution = DEFAULT_RESOLUTION;
        }
    }

    public void setWidth(double w) {
        if (w > 0) {
            width = w;
        } else {
            width = DEFAULT_IMAGE_WIDTH;
        }
    }

    public double getWidth() {
        return width;
    }

    public void setHeight(double h) {
        if (h > 0) {
            height = h;
        } else {
            height = DEFAULT_IMAGE_HEIGHT;
        }
    }

    public double getHeight() {
        return height;
    }

    public void setResolution(int r) {
        if (r > 0) {
            resolution = r;
        } else {
            resolution = DEFAULT_RESOLUTION;
        }
    }

    public int getResolution() {
        return resolution;
    }

    public double getWidthHeightRate() {
        return width / height;
    }

    public double getHeightWidthRate() {
        return height / width;
    }
}
