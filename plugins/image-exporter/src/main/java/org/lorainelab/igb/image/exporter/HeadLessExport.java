package org.lorainelab.igb.image.exporter;

import com.affymetrix.common.PreferenceUtils;
import static org.lorainelab.igb.image.exporter.ExportImageInfo.DEFAULT_RESOLUTION;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
import org.jfree.svg.SVGGraphics2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

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
    protected static final String[] EXTENSION = {".svg", ".png", ".jpeg", ".jpg", ".pdf"};
    protected static final String[] DESCRIPTION = {
        "Scalable Vector Graphics (*.svg)",
        "Portable Network Graphics (*.png)",
        "Joint Photographic Experts Group (*.jpeg)",
        "Portable Document Format (*.pdf)"};

    protected final Properties svgProperties;
    protected BufferedImage exportImage;
    protected ExportImageInfo imageInfo;

    public HeadLessExport() {
        svgProperties = new Properties();
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

                SVGGraphics2D svgGenerator = new SVGGraphics2D( (int) imageInfo.getWidth(), (int) imageInfo.getHeight());
                component.paint(svgGenerator);

                try (Writer writer = new FileWriter(f.getAbsolutePath())){

                    writer.write(svgGenerator.getSVGElement());
                }



            } else if (ext.equals(EXTENSION[2]) && f.getAbsolutePath().endsWith(EXTENSION[4])) {

                try {

                    //Creating a jpeg image
                    String newPath = f.getAbsolutePath().substring(0, f.getAbsolutePath().length() - 4) + EXTENSION[2];
                    f = new File(newPath);
                    exportImage(EXTENSION[2], f);

                    // Load PDF document
                    PDDocument document = new PDDocument();

                    // Load image
                    PDImageXObject image = PDImageXObject.createFromFile(f.getAbsolutePath(), document);
                    PDRectangle rectangle = new PDRectangle(image.getWidth(), image.getHeight());

                    // Create a PDF page with the same size as the image
                    PDPage page = new PDPage(rectangle);
                    document.addPage(page);

                    // Draw image on PDF page
                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                        contentStream.drawImage(image, 0, 0, image.getWidth(), image.getHeight());
                    }

                    String imagePath = f.getAbsolutePath();

                    if (imagePath.toLowerCase().endsWith(EXTENSION[2])) {
                        // Replace ".jpeg" with ".pdf"
                        imagePath = imagePath.substring(0, imagePath.length() - 5) + EXTENSION[4];
                    }

                    // Save and close document
                    document.save(imagePath);
                    document.close();

                    if (f.exists()) {

                        f.delete();

                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {

                exportImage(ext, f);

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

    private void exportImage(String ext, File f) throws IOException {

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

}
