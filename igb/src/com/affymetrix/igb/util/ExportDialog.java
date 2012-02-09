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
	private static final LinkedHashMap<ExportFileType, ExportFileFilter> FILTER_LIST = new LinkedHashMap<ExportFileType, ExportFileFilter>();
	public static final ExportFileType JPEG = new ExportFileType(EXTENSION[0], DESCRIPTION[0]);
	public static final ExportFileType PNG = new ExportFileType(EXTENSION[1], DESCRIPTION[1]);

	static {
		FILTER_LIST.put(JPEG, new ExportFileFilter(JPEG));
		FILTER_LIST.put(PNG, new ExportFileFilter(PNG));
	}
	JComboBox extComboBox = new JComboBox(FILTER_LIST.keySet().toArray());
	JComboBox resolutionComboBox = new JComboBox(RESOLUTION);
	JComboBox unitComboBox = new JComboBox(UNIT);
	JTextField filePathTextField = new JTextField();
	static JSpinner widthSpinner = new JSpinner();
	static JSpinner heightSpinner = new JSpinner();
	JLabel previewLabel = new JLabel();
	JLabel sizeLabel = new JLabel();

	public static FileFilter[] getAllExportFileFilters() {
		return FILTER_LIST.values().toArray(new FileFilter[FILTER_LIST.size()]);
	}

	public static ExportDialog getSingleton() {
		if (singleton == null) {
			singleton = new ExportDialog();
		}

		return singleton;
	}
	public static Component component; // Export component
	public static ImageInfo imageInfo;
	private static ImageInfo originalInfo;
	private static String unit;
	private boolean isWidthSpinner = false; // Prevent multiple triggering each other
	private boolean isHeightSpinner = false;

	public void init() {
		String file = exportNode.get(PREF_FILE, DEFAULT_FILE);
		if (file.equals(DEFAULT_FILE)) {
			exportFile = new File(FileTracker.EXPORT_DIR_TRACKER.getFile().getPath(), file);
		} else {
			exportFile = new File(file);
		}

		imageInfo.setResolution(exportNode.getInt(PREF_RESOLUTION, imageInfo.getResolution()));

		unit = exportNode.get(PREF_UNIT, (String) UNIT[0]);

		filePathTextField.setText(exportFile.getAbsolutePath());

		ExportFileType type = getType(exportNode.get(PREF_EXT, DESCRIPTION[1]));
		extComboBox.setSelectedItem(type);
		resolutionComboBox.setSelectedItem(imageInfo.getResolution());
		unitComboBox.setSelectedItem(unit);

		double width = originalInfo.getWidth();
		double height = originalInfo.getHeight();
		if (unit.equals(UNIT[1])) {
			width /= originalInfo.getResolution();
			height /= originalInfo.getResolution();
		}

		SpinnerModel sm = new SpinnerNumberModel(width, 0, 10000, 1);
		widthSpinner.setModel(sm);
		sm = new SpinnerNumberModel(height, 0, 10000, 1);
		heightSpinner.setModel(sm);

		resetWidthHeight(width, height);
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

		originalInfo = new ImageInfo(imageInfo.getWidth(),
				imageInfo.getHeight(),
				imageInfo.getResolution());

		widthSpinner.setValue((double) component.getWidth());
		heightSpinner.setValue((double) component.getHeight());
	}

	public static String getFileExtension(String filePath) {
		String extension = null;

		int indexOfExtension = filePath.lastIndexOf(".");
		if (indexOfExtension >= 0) {
			extension = filePath.substring(indexOfExtension, filePath.length());
		}
		return extension;
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

	private static void setJPEG_DPI(IIOMetadata metadata) throws IIOInvalidTreeException {
		IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
		IIOMetadataNode jfif = (IIOMetadataNode) tree.getElementsByTagName("app0JFIF").item(0);
		jfif.setAttribute("Xdensity", Integer.toString(imageInfo.getResolution()));
		jfif.setAttribute("Ydensity", Integer.toString(imageInfo.getResolution()));
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
		String previousFile = exportFile.getAbsolutePath();
		String newFile = filePathTextField.getText();
		String ext = ParserController.getExtension(newFile);
		FileFilter filter = null;

		exportFile = new File(newFile);

		if (!exportFile.getParentFile().isDirectory() || !isExt(ext)) {
			exportFile = new File(previousFile);
			filePathTextField.setText(previousFile);
		}

		filter = getFilter(ext);
		fileChooser = new ExportFileChooser(exportFile.getParentFile(), exportFile, filter);
		fileChooser.setDialogTitle("Save view as...");
		fileChooser.showDialog(panel, "Select");

		if (fileChooser.getSelectedFile() != null) {
			String path = fileChooser.getSelectedFile().getAbsolutePath();
			filter = fileChooser.getFileFilter();
			ExportFileType type = getType(filter.getDescription());
			extComboBox.setSelectedItem(type);
			if (ParserController.getExtension(path).equals("")) {
				path += type.getExtension();
			}

			filePathTextField.setText(path);
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
		String newPath = filePathTextField.getText();
		String previousPath = exportFile.getAbsolutePath();
		String ext = ((ExportFileType) extComboBox.getSelectedItem()).getExtension();

		exportFile = new File(newPath);

		if (!exportFile.getParentFile().isDirectory()) {
			exportFile = new File(previousPath);
			filePathTextField.setText(previousPath);
		}

		if (isExt(ParserController.getExtension(newPath))) {
			// Check if the extention is valid or not
			exportFile = changeFileExtension(exportFile, ext);
		} else {
			newPath += ext;
			exportFile = new File(newPath);
		}

		filePathTextField.setText(exportFile.getAbsolutePath());
	}

	public boolean okButtonActionPerformed() throws IOException {
		String previousPath = exportFile.getAbsolutePath();
		String newPath = filePathTextField.getText();
		exportFile = new File(newPath);

		if (!isValidExportFile(previousPath)) {
			return false;
		}

		String ext = ((ExportFileType) extComboBox.getSelectedItem()).getExtension();
		exportScreenshot(exportFile, ext);

		exportNode.put(PREF_FILE, exportFile.getAbsolutePath());
		exportNode.put(PREF_EXT, extComboBox.getSelectedItem().toString());
		exportNode.putInt(PREF_RESOLUTION, imageInfo.getResolution());
		exportNode.put(PREF_UNIT, unit);

		return true;
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
					(int) imageInfo.getWidth(), (int) imageInfo.getHeight());

			writeImage(image, ext, f);
		}
	}

	private boolean isValidExportFile(String previousPath) {
		if (!exportFile.getParentFile().isDirectory()) {
			// if output path is invalid, reset to previous correct path
			ErrorHandler.errorPanel("The output path is invalid.");
			filePathTextField.setText(previousPath);
			filePathTextField.grabFocus();
			exportFile = new File(previousPath);
			return false;
		}

		String ext = ParserController.getExtension(exportFile.getAbsolutePath());

		if (!isExt(ext)) {
			// if file format is not exist, add current selected format to the end
			ext = ((ExportFileType) extComboBox.getSelectedItem()).getExtension();
			String newPath = exportFile.getAbsolutePath() + ext;

			exportFile = new File(newPath);
		}

		// if image size is too large...
		long heapFreeSize = Runtime.getRuntime().freeMemory();
		long size = (long) imageInfo.getWidth() * (long) imageInfo.getHeight();
		if (size > heapFreeSize) {
			ErrorHandler.errorPanel("The image size is invalid.");
			return false;
		}

		if (exportFile.exists()) {
			// give the user the choice to overwrite the existing file or not
			// The option pane used differs from the confirmDialog only in
			// that "No" is the default choice.
			String[] options = {"Yes", "No"};
			if (JOptionPane.NO_OPTION == JOptionPane.showOptionDialog(
					null, "Overwrite Existing File?", "File Exists",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					options, options[1])) {
				return false;
			}
		}

		return true;
	}

	public void resetButtonActionPerformed() {
		widthSpinner.setValue(originalInfo.getWidth());
		heightSpinner.setValue(originalInfo.getHeight());
		resolutionComboBox.setSelectedItem(originalInfo.getResolution());
		unitComboBox.setSelectedItem(UNIT[0]);
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
			double newWidth = ((Double) widthSpinner.getValue()).doubleValue();
			double newHeight = newWidth * originalInfo.getHeightWidthRate();

			isWidthSpinner = true;
			heightSpinner.setValue(newHeight);
			isWidthSpinner = false;

			resetWidthHeight(newWidth, newHeight);
		}
	}

	public void heightSpinnerStateChanged() {
		if (!isWidthSpinner) {
			double newHeight = ((Double) heightSpinner.getValue()).doubleValue();
			double newWidth = newHeight * originalInfo.getWidthHeightRate();

			isHeightSpinner = true;
			widthSpinner.setValue(newWidth);
			isHeightSpinner = false;

			resetWidthHeight(newWidth, newHeight);
		}
	}

	public void unitComboBoxActionPerformed() {
		unit = (String) unitComboBox.getSelectedItem();

		double newWidth = ((Double) widthSpinner.getValue()).doubleValue();

		if (unit.equals(UNIT[0])) {
			newWidth *= imageInfo.getResolution();
		} else {
			newWidth /= imageInfo.getResolution();
		}

		widthSpinner.setValue(newWidth);
	}

	private void resetWidthHeight(double width, double height) {
		if (unit != null) {
			if (unit.equals(UNIT[1])) {
				// Convert back from inches to pixels
				width *= imageInfo.getResolution();
				height *= imageInfo.getResolution();
			}

			imageInfo.setWidth(width);
			imageInfo.setHeight(height);

			sizeLabel.setText(String.valueOf((int) width)
					+ " x "
					+ String.valueOf((int) height)
					+ " pixels");
		}
	}

	public void resolutionComboBoxActionPerformed() {
		imageInfo.setResolution((Integer) resolutionComboBox.getSelectedItem());

		if (unit.equals(UNIT[1])) {
			double width = imageInfo.getWidth();
			width /= imageInfo.getResolution();
			widthSpinner.setValue(width);
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
