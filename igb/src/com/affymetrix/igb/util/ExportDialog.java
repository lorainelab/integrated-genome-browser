package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.ParserController;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.view.AltSpliceView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/**
 * 
 * @author nick
 */
public class ExportDialog implements ExportConstants {

	private static Preferences exportNode = PreferenceUtils.getExportPrefsNode();
	private static ExportDialog singleton;
	private JFileChooser fileChooser;
	private File exportFile;
	private File exportDirectory;
	private static final LinkedHashMap<ExportFileType, ExportFileFilter> FILTER_LIST = new LinkedHashMap<ExportFileType, ExportFileFilter>();
	public static final ExportFileType JPEG = new ExportFileType(EXTENSION[0], DESCRIPTION[0]);
	public static final ExportFileType PNG = new ExportFileType(EXTENSION[1], DESCRIPTION[1]);
	private static String slash = "/";

	static {
		FILTER_LIST.put(JPEG, new ExportFileFilter(JPEG));
		FILTER_LIST.put(PNG, new ExportFileFilter(PNG));
	}
	JComboBox extComboBox = new JComboBox(FILTER_LIST.keySet().toArray());
	JTextField filePathTextField = new JTextField();
	static JSpinner heightSpinner = new JSpinner();
	static JSpinner widthSpinner = new JSpinner();
	JSpinner xSpinner = new JSpinner();
	JSpinner ySpinner = new JSpinner();
	JLabel previewLabel = new JLabel();

	public static FileFilter[] getAllExportFileFilters() {
		return FILTER_LIST.values().toArray(new FileFilter[FILTER_LIST.size()]);
	}

	public static ExportDialog getSingleton() {
		if (singleton == null) {
			singleton = new ExportDialog();
			if (isWindows()) {
				slash = "\\";
			}
		}

		return singleton;
	}
	
	private static boolean isWindows() {
 
		String os = System.getProperty("os.name").toLowerCase();
		// windows
		return (os.indexOf("win") >= 0);
 
	}
	
	public static Component component; // Export component
	public static ImageInfo imageInfo;
	private static ImageInfo originalInfo;
	private boolean isWidthSpinner = false; // Prevent multiple triggering each other
	private boolean isHeightSpinner = false;

	public void init() {
		exportFile = new File(exportNode.get(PREF_FILE, DEFAULT_FILE));
		exportDirectory = new File(exportNode.get(PREF_DIR,
				FileTracker.EXPORT_DIR_TRACKER.getFile().getPath() + slash));
		imageInfo.setXResolution(exportNode.getInt(PREF_X, imageInfo.getXResolution()));
		imageInfo.setYResolution(exportNode.getInt(PREF_Y, imageInfo.getYResolution()));

		filePathTextField.setText(exportDirectory.getAbsolutePath()
				+ slash + exportFile.getName());

		ExportFileType type = getType(exportNode.get(PREF_EXT, DESCRIPTION[1]));
		extComboBox.setSelectedItem(type);

		SpinnerModel sm = new SpinnerNumberModel(imageInfo.getWidth(), 0, 10000, 1);
		widthSpinner.setModel(sm);
		sm = new SpinnerNumberModel(imageInfo.getHeight(), 0, 10000, 1);
		heightSpinner.setModel(sm);
		sm = new SpinnerNumberModel(imageInfo.getXResolution(), 0, 1000, 1);
		xSpinner.setModel(sm);
		sm = new SpinnerNumberModel(imageInfo.getYResolution(), 0, 1000, 1);
		ySpinner.setModel(sm);
	}

	public static void setComponent(Component c) {
		component = c;

		initImageInfo();
	}

	public static void initImageInfo() {
		if (imageInfo == null) {
			imageInfo = new ImageInfo(component.getWidth(), component.getHeight());
		} else {
			imageInfo.setWidth(component.getWidth());
			imageInfo.setHeight(component.getHeight());
		}

		originalInfo = new ImageInfo(imageInfo.getWidth(), imageInfo.getHeight(),
				imageInfo.getXResolution(), imageInfo.getYResolution());

		widthSpinner.setValue(component.getWidth());
		heightSpinner.setValue(component.getHeight());
	}

	public static String getFileExtension(String filePath) {
		String extension = null;

		int indexOfExtension = filePath.lastIndexOf(".");
		if (indexOfExtension >= 0) {
			extension = filePath.substring(indexOfExtension, filePath.length());
		}
		return extension;
	}

	public static void exportScreenshot(File f, String ext) throws IOException {
		BufferedImage image = GraphicsUtil.getDeviceCompatibleImage(
				component.getWidth(), component.getHeight());
		Graphics g = image.createGraphics();
		component.paintAll(g);

		if (f != null) {

			if (!f.getName().toLowerCase().endsWith(ext)) {
				String correctedFilename = f.getAbsolutePath() + ext;
				f = new File(correctedFilename);
			}

			image = GraphicsUtil.resizeImage(image,
					imageInfo.getWidth(), imageInfo.getHeight());

			writeImage(image, ext, f);
		}
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

			if (ext.equals(EXTENSION[0])) {
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
		String directory = path.substring(0, path.lastIndexOf(slash));
		String file = path.substring(path.lastIndexOf(slash));
		String ext = ParserController.getExtension(path);
		FileFilter filter = null;
		exportDirectory = new File(directory);
		exportFile = new File(file);

		if (!exportDirectory.isDirectory() || !isExt(ext)) {
			exportDirectory = new File(previousDirectory);
			exportFile = new File(previousFile);
			filePathTextField.setText(previousDirectory + slash + previousFile);
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
		String directory = path.substring(0, path.lastIndexOf(slash));
		String file = path.substring(path.lastIndexOf(slash));
		String ext = EXTENSION[0];
		exportFile = new File(file);

		for (ExportFileType type : FILTER_LIST.keySet()) {
			if (type.equals(extComboBox.getSelectedItem())) {
				ext = type.getExtension();
			}
		}

		exportFile = changeFileExtension(exportFile, ext);

		filePathTextField.setText(directory + slash + exportFile.getName());
	}

	public boolean okButtonActionPerformed() throws IOException {
		String previousDirectory = exportDirectory.getAbsolutePath();
		String previousFile = exportFile.getName();
		String path = filePathTextField.getText();
		String ext = ParserController.getExtension(path);
		String directoryStr = path.substring(0, path.lastIndexOf(slash));
		File directory = new File(directoryStr);

		if (!directory.isDirectory() || !isExt(ext)) {
			ErrorHandler.errorPanel("The path or image format is invalid.");
			filePathTextField.setText(previousDirectory + slash + previousFile);
			filePathTextField.grabFocus();
			return false;
		}

		File file = new File(path);
		exportScreenshot(file, ext);

		saveToPref(file.getName(), directory.getAbsolutePath(),
				extComboBox.getSelectedItem().toString(),
				imageInfo.getXResolution(),
				imageInfo.getYResolution());

		return true;
	}

	private void saveToPref(String file, String directory, String ext, int x, int y) {
		exportNode.put(PREF_FILE, file);
		exportNode.put(PREF_DIR, directory);
		exportNode.put(PREF_EXT, ext);
		exportNode.putInt(PREF_X, x);
		exportNode.putInt(PREF_Y, y);
	}

	public void resetButtonActionPerformed() {
		widthSpinner.setValue(originalInfo.getWidth());
		heightSpinner.setValue(originalInfo.getHeight());
		xSpinner.setValue(originalInfo.getXResolution());
		ySpinner.setValue(originalInfo.getYResolution());
	}

	public void previewImage() {
		BufferedImage image = GraphicsUtil.getDeviceCompatibleImage(
				component.getWidth(), component.getHeight());
		Graphics g = image.createGraphics();
		component.printAll(g);

		image = GraphicsUtil.resizeImage(image,
				previewLabel.getWidth(), previewLabel.getHeight());

		ImageIcon icon = new ImageIcon(image);
		previewLabel.setIcon(icon);
	}

	public static boolean isExt(String ext) {
		for (ExportFileType type : FILTER_LIST.keySet()) {
			if (type.getExtension().equals(ext)) {
				return true;
			}
		}

		return false;
	}

	public void widthSpinnerStateChanged() {
		if (!isHeightSpinner) {
			int newWidth = (Integer) widthSpinner.getValue();
			int oldWidth = imageInfo.getWidth();
			int oldHeight = imageInfo.getHeight();
			int newHeight = newWidth * oldHeight / oldWidth;
			isWidthSpinner = true;
			heightSpinner.setValue(newHeight);
			isWidthSpinner = false;
		}
	}

	public void heightSpinnerStateChanged() {
		if (!isWidthSpinner) {
			int newHeight = (Integer) heightSpinner.getValue();
			int oldWidth = imageInfo.getWidth();
			int oldHeight = imageInfo.getHeight();
			int newWidth = newHeight * oldWidth / oldHeight;
			isHeightSpinner = true;
			widthSpinner.setValue(newWidth);
			isHeightSpinner = false;
		}
	}

	public static Component determineSlicedComponent() {
		AltSpliceView slice_view = (AltSpliceView) ((IGB) IGB.getSingleton()).getView(AltSpliceView.class.getName());
		if (slice_view == null) {
			return null;
		}

		return ((AffyLabelledTierMap) slice_view.getSplicedView().getSeqMap()).getSplitPane();
	}
}
