package com.affymetrix.igb.shared;

import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.util.GraphicsUtil;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import org.freehep.graphicsio.svg.SVGExportFileType;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hiralv
 */
public class HeadLessExport {

    protected static Preferences exportNode = PreferenceUtils.getExportPrefsNode();
    private static final Logger logger = LoggerFactory.getLogger(HeadLessExport.class);
    protected static final String PREF_FILE = "File";
    protected static final String PREF_EXT = "Ext";
    protected static final String PREF_DIR = "Dir";
    protected static final String PREF_RESOLUTION = "Resolution"; // same resolution for horizontal and vertical
    protected static final String PREF_UNIT = "Unit";
    protected static final String[] EXTENSION = {".svg", ".png", ".jpeg", ".jpg"};
    protected static final String[] DESCRIPTION = {
        "Scalable Vector Graphics (*.svg)",
        "Portable Network Graphics (*.png)",
        "Joint Photographic Experts Group (*.jpeg)",};

    protected final Properties svgProperties = new Properties();
    protected final SVGExportFileType svgExport = new SVGExportFileType();
    protected BufferedImage exportImage;
    protected ImageInfo imageInfo;

    public void exportScreenshot(Component component, File f, String ext, boolean isScript) throws IOException {
        // From Script Loader, need to initialize the export image
        if (isScript) {
            exportImage = GraphicsUtil.getDeviceCompatibleImage(
                    component.getWidth(), component.getHeight());
            Graphics g = exportImage.createGraphics();
            component.paintAll(g);
            imageInfo = new ImageInfo(component.getWidth(), component.getHeight());
            imageInfo.setResolution(exportNode.getInt(PREF_RESOLUTION, DEFAULT_RESOLUTION));
        }

        if (ext.equals(EXTENSION[0])) {
            svgProperties.setProperty(SVGGraphics2D.class.getName() + ".ImageSize",
                    (int) imageInfo.getWidth() + ", " + (int) imageInfo.getHeight());
            svgExport.exportToFile(f, component, null, svgProperties, "");
        } else {
            exportImage = GraphicsUtil.resizeImage(exportImage, (int) imageInfo.getWidth(), (int) imageInfo.getHeight());
            Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(ext.substring(1)); // need to remove "."
            while (iw.hasNext()) {
                ImageWriter writer = iw.next();
                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                ImageTypeSpecifier typeSpecifier
                        = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
                IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
                if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
                    continue;
                }

                if (ext.equals(EXTENSION[1])) {
                    setPNG_DPI(metadata);
                } else {
                    setJPEG_DPI(metadata);
                }

                try (ImageOutputStream stream = ImageIO.createImageOutputStream(f)) {
                    writer.setOutput(stream);
                    writer.write(metadata, new IIOImage(exportImage, null, metadata), writeParam);
                }
                break;
            }
        }
    }
    private static final int DEFAULT_RESOLUTION = 300;

    /**
     * Passed meta data of PNG image and reset its DPI
     *
     * @param metadata
     * @throws IIOInvalidTreeException
     */
    private void setPNG_DPI(IIOMetadata metadata) throws IIOInvalidTreeException {
        IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
        horiz.setAttribute("value", Double.toString(imageInfo.getResolution() * 0.039));

        IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
        vert.setAttribute("value", Double.toString(imageInfo.getResolution() * 0.039));

        IIOMetadataNode dim = new IIOMetadataNode("Dimension");
        dim.appendChild(horiz);
        dim.appendChild(vert);

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(dim);

        metadata.mergeTree("javax_imageio_1.0", root);
    }

    /**
     * Passed meta data of JPEG image and reset its DPI
     *
     * @param metadata
     * @throws IIOInvalidTreeException
     */
    private void setJPEG_DPI(IIOMetadata metadata) throws IIOInvalidTreeException {
        IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
        IIOMetadataNode jfif = (IIOMetadataNode) tree.getElementsByTagName("app0JFIF").item(0);
        jfif.setAttribute("Xdensity", Integer.toString(imageInfo.getResolution()));
        jfif.setAttribute("Ydensity", Integer.toString(imageInfo.getResolution()));
        jfif.setAttribute("resUnits", "1"); // density is dots per inch
        metadata.setFromTree("javax_imageio_jpeg_image_1.0", tree);
    }

    public static class ImageInfo {

        private double width;
        private double height;
        private int resolution = DEFAULT_RESOLUTION;
        private static final int DEFAULT_IMAGE_HEIGHT = 800;
        private static final int DEFAULT_IMAGE_WIDTH = 600;

        public ImageInfo(double w, double h) {
            this(w, h, DEFAULT_IMAGE_WIDTH);
        }

        private void validateDimensions() {
            if (width <= 0) {
                logger.warn("Invalid image width, setting to default value.  Please report this error if the problem persist.");
                width = DEFAULT_IMAGE_WIDTH;
            }
            if (height <= 0) {
                height = DEFAULT_IMAGE_HEIGHT;
            }
            if (resolution <= 0) {
                resolution = DEFAULT_RESOLUTION;
            }
        }

        public ImageInfo(double w, double h, int r) {
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

        public void setWidth(double w) {
            if (w > 0) {
                width = w;
            } else {
                logger.warn("Invalid state detected, image width must be greater than 0.  Setting to default width of {}", DEFAULT_IMAGE_WIDTH);
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
                logger.warn("Invalid state detected, image height must be greater than 0.  Setting to default height of {}", DEFAULT_IMAGE_HEIGHT);
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
                logger.warn("Invalid Image resolution, setting to default resolution.");
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
}
