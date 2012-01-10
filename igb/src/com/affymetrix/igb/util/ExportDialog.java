package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.shared.FileTracker;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/**
 * 
 * @author nick
 */
public class ExportDialog {

	private static ExportDialog singleton;
	public static ImageInfo imageInfo;
	private JFileChooser fileChooser;
	private File exportDirectory = new File(FileTracker.EXPORT_DIR_TRACKER.getFile().getPath() + "/");
	private File exportFile = new File("export.jpeg");
	public JTextField filePathTextField = new JTextField(exportDirectory.getAbsolutePath()
			+ "/"
			+ exportFile.getName());
	private static final LinkedHashMap<ExportFileType, ExportFileFilter> 
			FILTER_LIST = new LinkedHashMap<ExportFileType, ExportFileFilter>();
	public static final String[] EXT = {".jpeg", ".png"};
	public static final String[] DESCRIPTION = {
		"Joint Photographic Experts Group Files (*.jpeg)",
		"Portable Network Graphics Files (*.png)"
	};
	public static final ExportFileType JPEG = new ExportFileType(EXT[0], DESCRIPTION[0]);
	public static final ExportFileType PNG = new ExportFileType(EXT[1], DESCRIPTION[1]);

	static {
		FILTER_LIST.put(JPEG, new ExportFileFilter(JPEG));
		FILTER_LIST.put(PNG, new ExportFileFilter(PNG));
	}
	JComboBox extComboBox = new JComboBox(FILTER_LIST.keySet().toArray());

	public static FileFilter[] getAllExportFileFilters() {
		return FILTER_LIST.values().toArray(new FileFilter[FILTER_LIST.size()]);
	}

	public static ExportDialog getSingleton() {
		if (singleton == null) {
			singleton = new ExportDialog();
		}

		return singleton;
	}

	public static void initImageInfo(Component c) {
		if (imageInfo == null) {
			imageInfo = new ImageInfo(c.getWidth(), c.getHeight());
		} else {
			imageInfo.setWidth(c.getWidth());
			imageInfo.setHeight(c.getHeight());
		}
	}

	public static String getFileExtension(String filePath) {

		String extension = null;

		int indexOfExtension = filePath.lastIndexOf(".");
		if (indexOfExtension >= 0) {
			extension = filePath.substring(indexOfExtension, filePath.length());
		}
		return extension;
	}

	public static void doComponentExport(Component c, File file, String ext) throws IOException {
		if (ext.equals(JPEG.getExtension())) {
			exportScreenshot(c, file, JPEG);
		} else if (ext.equals(PNG.getExtension())) {
			exportScreenshot(c, file, PNG);
		}
	}

	private static void exportScreenshot(Component c, File selectedFile, ExportFileType type) throws IOException {
		BufferedImage image = getDeviceCompatibleImage(c.getWidth(), c.getHeight());
		Graphics g = image.createGraphics();
		c.paintAll(g);

		if (selectedFile != null) {

			if (!selectedFile.getName().toLowerCase().endsWith(type.getExtension())) {
				String correctedFilename = selectedFile.getAbsolutePath() + type.getExtension();
				selectedFile = new File(correctedFilename);
			}

			if (isSizeChanged(c)) {
				image = resizeImage(image);
			}

			writeImage(image, type.getExtension(), selectedFile);
		}
	}

	private static boolean isSizeChanged(Component c) {
		if (imageInfo.getWidth() != c.getWidth()
				|| imageInfo.getHeight() != c.getHeight()) {
			return true;
		}

		return false;
	}

	private static void writeImage(BufferedImage image, String ext, File f) throws IOException {
		Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(ext.substring(1)); // need to remove "."
		while (iw.hasNext()) {
			ImageWriter writer = iw.next();
			ImageWriteParam writeParam = writer.getDefaultWriteParam();
			ImageTypeSpecifier typeSpecifier =
					ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
			IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
			if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
				continue;
			}

			if (ext.equals(EXT[0])) {
				setJPEG_DPI(metadata);
			} else {
				setPNG_DPI(metadata);
			}

			final ImageOutputStream stream = ImageIO.createImageOutputStream(f);
			try {
				writer.setOutput(stream);
				writer.write(metadata, new IIOImage(image, null, metadata), writeParam);
			} finally {
				stream.close();
			}
			break;
		}
	}

	private static void setPNG_DPI(IIOMetadata metadata) throws IIOInvalidTreeException {
		IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
		horiz.setAttribute("value", Double.toString(imageInfo.getXResolution() * 0.039));

		IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
		vert.setAttribute("value", Double.toString(imageInfo.getYResolution() * 0.039));

		IIOMetadataNode dim = new IIOMetadataNode("Dimension");
		dim.appendChild(horiz);
		dim.appendChild(vert);

		IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
		root.appendChild(dim);

		metadata.mergeTree("javax_imageio_1.0", root);
	}

	private static void setJPEG_DPI(IIOMetadata metadata) throws IIOInvalidTreeException {
		IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
		IIOMetadataNode jfif = (IIOMetadataNode) tree.getElementsByTagName("app0JFIF").item(0);
		jfif.setAttribute("Xdensity", Integer.toString(imageInfo.getXResolution()));
		jfif.setAttribute("Ydensity", Integer.toString(imageInfo.getYResolution()));
		jfif.setAttribute("resUnits", "1"); // density is dots per inch 
		metadata.setFromTree("javax_imageio_jpeg_image_1.0", tree);
	}

	private static BufferedImage getDeviceCompatibleImage(int width, int height) {

		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice screenDevice = graphicsEnvironment.getDefaultScreenDevice();
		GraphicsConfiguration graphicConfiguration = screenDevice.getDefaultConfiguration();
		BufferedImage image = graphicConfiguration.createCompatibleImage(width, height);

		return image;
	}

	private static BufferedImage resizeImage(BufferedImage originalImage) {

		int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
		BufferedImage resizedImage = new BufferedImage(imageInfo.getWidth(), imageInfo.getHeight(), type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, imageInfo.getWidth(), imageInfo.getHeight(), null);
		g.dispose();

		return resizedImage;
	}

	public static File changeFileExtension(File file, String extension) {

		if ((file == null) || (extension == null) || extension.trim().equals("")) {
			return null;
		}

		String path = file.getAbsolutePath();
		String newPath = "";
		String filename = file.getName().trim();

		if ((filename != null) && !filename.equals("")) {

			int periodIndex = path.lastIndexOf(".");

			newPath = path.substring(0, periodIndex);
			newPath += extension;
		}

		return new File(newPath);
	}

	public void browseButtonActionPerformed(JPanel panel) {
		String previousDirectory = exportDirectory.getAbsolutePath();
		String previousFile = exportFile.getName();
		String path = filePathTextField.getText();
		String directory = path.substring(0, path.lastIndexOf("/"));
		String file = path.substring(path.lastIndexOf("/"));
		String ext = path.substring(path.lastIndexOf("."));
		FileFilter filter = null;
		exportDirectory = new File(directory);
		exportFile = new File(file);

		if (!exportDirectory.isDirectory() || !isExt(ext)) {
			exportDirectory = new File(previousDirectory);
			exportFile = new File(previousFile);
			filePathTextField.setText(previousDirectory + "/" + previousFile);
		}

		filter = getFilter(ext);
		fileChooser = new ExportFileChooser(exportDirectory, exportFile, filter);
		fileChooser.setDialogTitle("Save view as...");
		fileChooser.showDialog(panel, "Select");

		if (fileChooser.getSelectedFile() != null) {
			filePathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
			filter = fileChooser.getFileFilter();
			ExportFileType type = getType(filter.getDescription());
			extComboBox.setSelectedItem(type);
		}
	}

	private ExportFileType getType(String description) {
		for (ExportFileType type : FILTER_LIST.keySet()) {
			if (type.getDescription().equals(description)) {
				return type;
			}
		}

		return null;
	}

	private ExportFileFilter getFilter(String ext) {
		for (ExportFileFilter filter : FILTER_LIST.values()) {
			if (filter.getExtension().equals(ext)) {
				return filter;
			}
		}

		return null;
	}

	public void extComboBoxActionPerformed() {
		String path = filePathTextField.getText();
		String directory = path.substring(0, path.lastIndexOf("/"));
		String file = path.substring(path.lastIndexOf("/"));
		String ext = ".jpeg";
		exportFile = new File(file);

		for (ExportFileType type : FILTER_LIST.keySet()) {
			if (type.equals(extComboBox.getSelectedItem())) {
				ext = type.getExtension();
			}
		}

		exportFile = changeFileExtension(exportFile, ext);

		filePathTextField.setText(directory + "/" + exportFile.getName());
	}

	public boolean okButtonActionPerformed(Component component) throws IOException {
		String previousDirectory = exportDirectory.getAbsolutePath();
		String previousFile = exportFile.getName();
		String path = filePathTextField.getText();
		String ext = path.substring(path.lastIndexOf("."));
		String directoryStr = path.substring(0, path.lastIndexOf("/"));
		File directory = new File(directoryStr);

		if (!directory.isDirectory() || !isExt(ext)) {
			ErrorHandler.errorPanel("The path or image format is invalid.");
			filePathTextField.setText(previousDirectory + "/" + previousFile);
			filePathTextField.grabFocus();
			return false;
		}

		File file = new File(path);
		doComponentExport(component, file, ext);
		return true;
	}

	public static boolean isExt(String ext) {
		for (ExportFileType type : FILTER_LIST.keySet()) {
			if (type.getExtension().equals(ext)) {
				return true;
			}
		}

		return false;
	}
}

class ImageInfo {

	private int width;
	private int height;
	private int xResolution = 300;
	private int yResolution = 300;

	ImageInfo(int w, int h) {
		width = w;
		height = h;
	}

	ImageInfo(int w, int h, int x, int y) {
		width = w;
		height = h;
		xResolution = x;
		yResolution = y;
	}

	public void setWidth(int w) {
		width = w;
	}

	public int getWidth() {
		return width;
	}

	public void setHeight(int h) {
		height = h;
	}

	public int getHeight() {
		return height;
	}

	public int getXResolution() {
		return xResolution;
	}

	public int getYResolution() {
		return yResolution;
	}
}

class ExportFileType {

	private String fileExtension;
	private String fileDescription;

	ExportFileType(String extension, String description) {
		fileExtension = extension;
		fileDescription = description;
	}

	public String getExtension() {
		return fileExtension;
	}

	public String getDescription() {
		return fileDescription;
	}

	@Override
	public String toString() {
		return getDescription();
	}
}

class ExportFileFilter extends FileFilter {

	public ExportFileType type;

	public ExportFileFilter(ExportFileType type) {
		this.type = type;
	}

	public boolean accept(File file) {

		if (file.isDirectory()) {
			return true;
		}

		return file.getName().toLowerCase().endsWith(type.getExtension());
	}

	public String getDescription() {
		return type.getDescription();
	}

	public String getExtension() {
		return type.getExtension();
	}

	public boolean accept(File file, String name) {
		return name.toLowerCase().endsWith(type.getExtension());
	}
}
