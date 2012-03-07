package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.ParserController;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.util.GeneralUtils;
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
	private String selectedExt;
	private static final LinkedHashMap<ExportFileType, ExportFileFilter> FILTER_LIST = new LinkedHashMap<ExportFileType, ExportFileFilter>();
	public static final ExportFileType SVG = new ExportFileType(EXTENSION[0], DESCRIPTION[0]);
	public static final ExportFileType PNG = new ExportFileType(EXTENSION[1], DESCRIPTION[1]);
	public static final ExportFileType JPEG = new ExportFileType(EXTENSION[2], DESCRIPTION[2]);

	static {
//		FILTER_LIST.put(SVG, new ExportFileFilter(SVG));
		FILTER_LIST.put(PNG, new ExportFileFilter(PNG));
		FILTER_LIST.put(JPEG, new ExportFileFilter(JPEG));
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
	private static String unit = "";
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

		filePathTextField.setText(exportFile.getAbsolutePath());

		ExportFileType type = getType(exportNode.get(PREF_EXT, DESCRIPTION[1]));
		extComboBox.setSelectedItem(type);
		selectedExt = type.getExtension();
		resolutionComboBox.setSelectedItem(imageInfo.getResolution());

		unit = exportNode.get(PREF_UNIT, (String) UNIT[0]);
		unitComboBox.setSelectedItem(unit);

		initSpinner(unit);
	}

	public void initSpinner(String unit) {
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
		String ext = ParserController.getExtension(path);
		if (ext.equalsIgnoreCase(extension)) {
			return file;
		}

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
		String path = filePathTextField.getText();
		FileFilter filter = null;

		exportFile = new File(path);

		if (!exportFile.getParentFile().isDirectory()) {
			exportFile = new File(previousFile);
			filePathTextField.setText(previousFile);
		}

		filter = getFilter(selectedExt);
		fileChooser = new ExportFileChooser(exportFile.getParentFile(), exportFile, filter);
		fileChooser.setDialogTitle("Save view as...");
		fileChooser.showDialog(panel, "Select");

		if (fileChooser.getSelectedFile() != null) {
			String newPath = fileChooser.getSelectedFile().getAbsolutePath();
			String ext = ParserController.getExtension(newPath);
			String des = fileChooser.getFileFilter().getDescription();

			// the output file extension is based on user's input
			if (isExt(ext) && !des.equals(getDescription(ext))) {
				des = getDescription(ext);
			}

			ExportFileType type = getType(des);
			extComboBox.setSelectedItem(type);
			filePathTextField.setText(newPath);
			exportFile = new File(newPath);
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

	private String getDescription(String ext) {
		if (ext.equalsIgnoreCase(EXTENSION[0])) {
			return DESCRIPTION[0];
		} else if (ext.equalsIgnoreCase(EXTENSION[1])) {
			return DESCRIPTION[1];
		} else {
			return DESCRIPTION[2];
		}
	}

	public boolean okButtonActionPerformed() throws IOException {
		String previousPath = exportFile.getAbsolutePath();
		String newPath = filePathTextField.getText();
		exportFile = new File(newPath);

		if (!isValidExportFile(previousPath)) {
			return false;
		}

		String path = exportFile.getAbsolutePath();
		String ext = ParserController.getExtension(path);

		if (!isExt(ext)) {
			ext = selectedExt;

			int index = path.lastIndexOf(".");
			int length = 0;
			if (index > 0) {
				length = path.substring(index).length();
			}
			if (length < 2) {
				if (length == 1) {
					path = path.substring(0, index);
				}
				path += selectedExt;
				exportFile = new File(path);
			}
		}

		exportScreenshot(exportFile, selectedExt);

		String des = getDescription(ext);
		extComboBox.setSelectedItem(getType(des));

		exportNode.put(PREF_FILE, path);
		exportNode.put(PREF_EXT, des);
		exportNode.putInt(PREF_RESOLUTION, imageInfo.getResolution());
		exportNode.put(PREF_UNIT, unit);

		return true;
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

	public static void exportScreenshot(File f, String ext) throws IOException {
		if (ext.equals(EXTENSION[0])) {
		//	Export To Svg
		} else {
			BufferedImage image = GraphicsUtil.getDeviceCompatibleImage(
					component.getWidth(), component.getHeight());
			Graphics g = image.createGraphics();
			component.paintAll(g);

			image = GraphicsUtil.resizeImage(image,
					(int) imageInfo.getWidth(), (int) imageInfo.getHeight());

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

				if (ext.equals(EXTENSION[1])) {
					setPNG_DPI(metadata);
				} else {
					setJPEG_DPI(metadata);
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
	}

	public void resetButtonActionPerformed() {
		unitComboBox.setSelectedItem(UNIT[0]);
		widthSpinner.setValue(originalInfo.getWidth());
		heightSpinner.setValue(originalInfo.getHeight());
		resolutionComboBox.setSelectedItem(originalInfo.getResolution());
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
		for (String s : EXTENSION) {
			if (s.equalsIgnoreCase(ext)) {
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

	public void extComboBoxActionPerformed() {
		String path = filePathTextField.getText();
		String ext = ParserController.getExtension(path);
		selectedExt = ((ExportFileType) extComboBox.getSelectedItem()).getExtension();

		int index = path.lastIndexOf(".");
		int length = 0;
		if (index > 0) {
			length = path.substring(index).length();
		}

		// keep user input extension, if it's not a support extension
		if (!isExt(ext) && length > 1) {
			return;
		} else {
			if (!ext.equalsIgnoreCase(selectedExt)) {
				if (selectedExt.equals(EXTENSION[2]) &&
					ext.equalsIgnoreCase(EXTENSION[3])) { // special case for jpg
					return;
				}

				if (length > 0) {
					path = path.substring(0, index);
				}
				path += selectedExt;
				exportFile = new File(path);
				filePathTextField.setText(path);
				filePathTextField.grabFocus();
			}
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
