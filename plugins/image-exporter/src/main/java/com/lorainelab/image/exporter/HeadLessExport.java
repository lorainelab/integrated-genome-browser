package com.lorainelab.image.exporter;

import com.affymetrix.common.PreferenceUtils;
import static com.lorainelab.image.exporter.ExportImageInfo.DEFAULT_RESOLUTION;
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

    protected static Preferences exportNode;
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

    protected final Properties svgProperties;
    protected final SVGExportFileType svgExport;
    protected BufferedImage exportImage;
    protected ExportImageInfo imageInfo;

    public HeadLessExport() {
        svgProperties = new Properties();
        svgExport = new SVGExportFileType();
         exportNode = PreferenceUtils.getExportPrefsNode();
    }

    public void headlessComponentExport(Component component, File f, String ext, boolean isHeadlessExport) {
        try {
            // From Script Loader, need to initialize the export image
            if (isHeadlessExport) {
                exportImage = GraphicsUtil.getDeviceCompatibleImage(component.getWidth(), component.getHeight());
                Graphics g = exportImage.createGraphics();
                component.paintAll(g);
                imageInfo = new ExportImageInfo(component.getWidth(), component.getHeight());
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
                        setPngDpi(metadata);
                    } else {
                        setJpegDpi(metadata);
                    }

                    try (ImageOutputStream stream = ImageIO.createImageOutputStream(f)) {
                        writer.setOutput(stream);
                        writer.write(metadata, new IIOImage(exportImage, null, metadata), writeParam);
                    }
                    break;
                }
            }
        } catch (IOException ex) {
            logger.error("Error while attempting to export an image.", ex);
        }
    }

    /**
     * Passed meta data of PNG image and reset its DPI
     *
     * @param metadata
     * @throws IIOInvalidTreeException
     */
    private void setPngDpi(IIOMetadata metadata) throws IIOInvalidTreeException {
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
    private void setJpegDpi(IIOMetadata metadata) throws IIOInvalidTreeException {
        IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
        IIOMetadataNode jfif = (IIOMetadataNode) tree.getElementsByTagName("app0JFIF").item(0);
        jfif.setAttribute("Xdensity", Integer.toString(imageInfo.getResolution()));
        jfif.setAttribute("Ydensity", Integer.toString(imageInfo.getResolution()));
        jfif.setAttribute("resUnits", "1"); // density is dots per inch
        metadata.setFromTree("javax_imageio_jpeg_image_1.0", tree);
    }

}
