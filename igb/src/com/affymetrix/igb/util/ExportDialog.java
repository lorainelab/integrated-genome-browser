package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.shared.FileTracker;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * Code referred from IGV
 * @author nick
 */
public class ExportDialog {

	private static ExportDialog singleton;
	private JFileChooser fileChooser;
	private File exportDirectory = new File(FileTracker.EXPORT_DIR_TRACKER.getFile().getPath() + "/");
	private File exportFile = new File("export.jpeg");
	public JTextField filePathTextField = new JTextField(exportDirectory.getAbsolutePath()
			+ "/"
			+ exportFile.getName());
	private static final LinkedHashMap<ExportFileType, ExportFileFilter> FILTER_LIST = new LinkedHashMap<ExportFileType, ExportFileFilter>();
	public static final String[] EXT = {
		"Joint Photographic Experts Group Files (*.jpeg)",
		"Portable Network Graphics Files (*.png)",
		"Scalable Vector Graphics Files (*.svg)"
	};
	public static final ExportFileType JPEG = new ExportFileType(".jpeg", EXT[0]);
	public static final ExportFileType PNG = new ExportFileType(".png", EXT[1]);
	public static final ExportFileType SVG = new ExportFileType(".svg", EXT[2]);

	static {
		FILTER_LIST.put(JPEG, new ExportFileFilter(JPEG));
		FILTER_LIST.put(PNG, new ExportFileFilter(PNG));
//		FILTER_LIST.put(SVG, new ExportFileFilter(SVG));
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

	public static String getFileExtension(String filePath) {

		String extension = null;

		int indexOfExtension = filePath.lastIndexOf(".");
		if (indexOfExtension >= 0) {
			extension = filePath.substring(indexOfExtension, filePath.length());
		}
		return extension;
	}

	public static void doComponentExport(Component component, File file, String ext) {

		int width = component.getWidth();
		int height = component.getHeight();

		if (ext.equals(JPEG.getExtension())) {
			exportScreenShotJPEG(component, file, width, height);
		} else if (ext.equals(PNG.getExtension())) {
			exportScreenShotPNG(component, file, width, height);
		} else if (ext.equals(SVG.getExtension())) {
			exportScreenShotSVG(component, file);
		}
	}

	private static void exportScreenShotJPEG(Component component, File selectedFile, int width, int height) {

		BufferedImage image = getDeviceCompatibleImage(width, height);
		Graphics g = image.createGraphics();
		component.paintAll(g);

		if (selectedFile != null) {

			if (!selectedFile.getName().toLowerCase().endsWith(JPEG.getExtension())) {
				String correctedFilename = selectedFile.getAbsolutePath() + JPEG.getExtension();
				selectedFile = new File(correctedFilename);
			}
			writeImage(image, JPEG.getExtension().substring(1), selectedFile);
		}
	}

	private static void exportScreenShotPNG(Component component, File selectedFile, int width, int height) {

		BufferedImage image = getDeviceCompatibleImage(width, height);
		Graphics g = image.createGraphics();
		component.paintAll(g);

		if (selectedFile != null) {

			if (!selectedFile.getName().toLowerCase().endsWith(PNG.getExtension())) {
				String correctedFilename = selectedFile.getAbsolutePath() + PNG.getExtension();
				selectedFile = new File(correctedFilename);
			}
			writeImage(image, PNG.getExtension().substring(1), selectedFile);
		}
	}

	private static void exportScreenShotSVG(Component component, File selectedFile) {
		try {
			DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

			// Create an instance of org.w3c.dom.Document.
			String svgNS = "http://www.w3.org/2000/svg";
			Document document = domImpl.createDocument(svgNS, SVG.getExtension().substring(1), null);

			// Create an instance of the SVG Generator.
			SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

			component.paintAll(svgGenerator);

			Writer out = new BufferedWriter(new FileWriter(selectedFile));

			svgGenerator.stream(out, true);
		} catch (Exception e) {
			Logger.getLogger(ExportDialog.class.getName()).log(
					Level.SEVERE, "Error encountered creating SVG file: " + e.toString());
		}
	}

	private static void writeImage(BufferedImage image, String ext, File f) {
		try {
			ImageIO.write(image, ext, f);
		} catch (IOException e) {
			Logger.getLogger(ExportDialog.class.getName()).log(
					Level.SEVERE, "Error creating: " + f.getAbsolutePath());
		}
	}

	private static BufferedImage getDeviceCompatibleImage(int width, int height) {

		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice screenDevice = graphicsEnvironment.getDefaultScreenDevice();
		GraphicsConfiguration graphicConfiguration = screenDevice.getDefaultConfiguration();
		BufferedImage image = graphicConfiguration.createCompatibleImage(width, height);

		return image;
	}

	final public static File changeFileExtension(File file, String extension) {

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
	
	private ExportFileType getType(String description)
	{
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

	public boolean okButtonActionPerformed(Component component) {
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