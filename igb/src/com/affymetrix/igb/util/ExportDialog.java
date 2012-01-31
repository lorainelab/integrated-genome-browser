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
		}

		return singleton;
	}
	public static Component component; // Export component
	public static ImageInfo imageInfo;
	private static ImageInfo originalInfo;
	private boolean isWidthSpinner = false; // Prevent multiple triggering each other
	private boolean isHeightSpinner = false;

	public void init() {
		String file = exportNode.get(PREF_FILE, DEFAULT_FILE);
		if (file.equals(DEFAULT_FILE)) {
			exportFile = new File(FileTracker.EXPORT_DIR_TRACKER.getFile().getPath(), file);
		} else {
			exportFile = new File(file);
		}

		imageInfo.setXResolution(exportNode.getInt(PREF_X, imageInfo.getXResolution()));
		imageInfo.setYResolution(exportNode.getInt(PREF_Y, imageInfo.getYResolution()));

		filePathTextField.setText(exportFile.getAbsolutePath());

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
		String newFile = filePathTextField.getText();
		String previousFile = exportFile.getAbsolutePath();
		String ext = ((ExportFileType) extComboBox.getSelectedItem()).getExtension();

		exportFile = new File(newFile);

		if (!exportFile.getParentFile().isDirectory()) {
			exportFile = new File(previousFile);
			filePathTextField.setText(previousFile);
		}

		exportFile = changeFileExtension(exportFile, ext);

		filePathTextField.setText(exportFile.getAbsolutePath());
	}

	public boolean okButtonActionPerformed() throws IOException {
		String previousPath = exportFile.getAbsolutePath();
		String newPath = filePathTextField.getText();
		String ext = ParserController.getExtension(newPath);

		exportFile = new File(newPath);

		if (!isValidExportFile(exportFile, ext, previousPath)) {
			return false;
		}

		exportScreenshot(exportFile, ext);

		exportNode.put(PREF_FILE, exportFile.getAbsolutePath());
		exportNode.put(PREF_EXT, extComboBox.getSelectedItem().toString());
		exportNode.putInt(PREF_X, imageInfo.getXResolution());
		exportNode.putInt(PREF_Y, imageInfo.getYResolution());

		return true;
	}

	private boolean isValidExportFile(File file, String ext, String path) {
		if (!exportFile.getParentFile().isDirectory() || !isExt(ext)) {
			ErrorHandler.errorPanel("The path or image format is invalid.");
			filePathTextField.setText(path);
			filePathTextField.grabFocus();
			exportFile = new File(path);
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
			int newWidth = ((Double) widthSpinner.getValue()).intValue();
			double newHeight = newWidth * originalInfo.getHeightWidthRate();
			isWidthSpinner = true;
			heightSpinner.setValue((int) newHeight);
			isWidthSpinner = false;

			setWidthHeight(newWidth, (int) newHeight);
		}
	}

	public void heightSpinnerStateChanged() {
		if (!isWidthSpinner) {
			int newHeight = ((Double) heightSpinner.getValue()).intValue();
			double newWidth = newHeight * originalInfo.getWidthHeightRate();
			isHeightSpinner = true;
			widthSpinner.setValue((int) newWidth);
			isHeightSpinner = false;

			setWidthHeight((int) newWidth, newHeight);
		}
	}

	private void setWidthHeight(int width, int height) {
		imageInfo.setWidth(width);
		imageInfo.setHeight(height);
	}

	public void xSpinnerStateChanged() {
		imageInfo.setXResolution((Integer) xSpinner.getValue());
	}

	public void ySpinnerStateChanged() {
		imageInfo.setYResolution((Integer) ySpinner.getValue());
	}

	public static Component determineSlicedComponent() {
		AltSpliceView slice_view = (AltSpliceView) ((IGB) IGB.getSingleton()).getView(AltSpliceView.class.getName());
		if (slice_view == null) {
			return null;
		}

		return ((AffyLabelledTierMap) slice_view.getSplicedView().getSeqMap()).getSplitPane();
	}
}
