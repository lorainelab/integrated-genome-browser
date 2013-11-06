package com.affymetrix.igb.util;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.freehep.graphicsio.svg.SVGGraphics2D;

import com.affymetrix.genometryImpl.util.DisplayUtils;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.AltSpliceView;

/**
 * An Export Image class for IGB. It is designed to export different part of IGB
 * views to image file. Support format: SVG, PNG and JPG
 *
 * TODO: - Preview image size should changed automatically basis on real
 * percentile. - Support PDF format. - Action performed to the view(not export
 * view), this should not trigger to activate refresh button. Only current
 * export view changed should activate refresh button.
 *
 * @author nick
 */
public class ExportDialog implements ExportConstants {
	private static ExportDialog singleton;
	
	private JFrame static_frame = null;
	private File defaultDir;
	private AffyTieredMap seqMap;
	private AffyTieredMap svseqMap;
	private Component wholeFrame;
	private Component mainView;
	private Component mainViewWithLabels;
	private Component slicedView;
	private Component component; // Export component
	private BufferedImage exportImage;
	private ImageInfo imageInfo;
	private String unit = "";
	private boolean isWidthSpinner = false; // Prevent multiple triggering each other
	private boolean isHeightSpinner = false;
	private File exportFile;
	private FileFilter extFilter;
	private File fileFilter;
	private ExportFileChooser fileChooser;
	private String selectedExt;
	private LinkedHashMap<ExportFileType, ExportFileFilter> FILTER_LIST;
	
	JComboBox extComboBox = new JComboBox();
	JComboBox resolutionComboBox = new JComboBox(RESOLUTION);
	JComboBox unitComboBox = new JComboBox(UNIT);
	JTextField filePathTextField = new JTextField();
	JSpinner widthSpinner = new JSpinner();
	JSpinner heightSpinner = new JSpinner();
	JLabel previewLabel = new JLabel();
	JLabel sizeLabel = new JLabel();
	JButton browseButton = new JButton();
	JButton cancelButton = new JButton();
	JButton okButton = new JButton();
	JButton refreshButton = new JButton();
	JRadioButton svRadioButton = new JRadioButton();
	JRadioButton wfRadioButton = new JRadioButton();
	JRadioButton mvRadioButton = new JRadioButton();
	JRadioButton mvlRadioButton = new JRadioButton();
	JPanel buttonsPanel = new JPanel();
	// detect export view size changed and activate refresh button.
	private final ComponentListener resizelistener = new ComponentListener() {

		public void componentResized(ComponentEvent e) {
			if (isVisible()) {
				enableRefreshButton();
			}
		}

		public void componentMoved(ComponentEvent ce) {}
		public void componentShown(ComponentEvent ce) {}
		public void componentHidden(ComponentEvent ce) {}
	};
	// detect export view range changed and activate refresh button.
	private final NeoRangeListener rangeListener = new NeoRangeListener() {

		@Override
		public void rangeChanged(NeoRangeEvent evt) {
			if (isVisible()) {
				enableRefreshButton();
			}
		}
	};
	// detect export view changed and activate refresh button. (Just for Seq View)
	private final AdjustmentListener adjustmentlistener = new AdjustmentListener() {

		public void adjustmentValueChanged(AdjustmentEvent ae) {
			if (isVisible()) {
				enableRefreshButton();
			}
		}
	};

	public ExportDialog() {
		FILTER_LIST = new LinkedHashMap<ExportFileType, ExportFileFilter>();
		FILTER_LIST.put(SVG, new ExportFileFilter(SVG));
		FILTER_LIST.put(PNG, new ExportFileFilter(PNG));
		FILTER_LIST.put(JPEG, new ExportFileFilter(JPEG));
	}
	
	/**
	 * @return ExportDialog instance
	 */
	public static ExportDialog getSingleton() {
		if (singleton == null) {
			singleton = new ExportDialog();
		}

		return singleton;
	}

	/**
	 * Display the export panel and initialize all related objects for export
	 * process.
	 *
	 * @param isSequenceViewer
	 */
	public synchronized void display(boolean isSequenceViewer) {
		initRadioButton(isSequenceViewer);
		initFrame();
		DisplayUtils.bringFrameToFront(static_frame);

		refreshButton.setEnabled(false);
		previewImage();
	}

	/**
	 * @return whether the export panel is initialized and visible or not.
	 */
	private boolean isVisible() {
		return static_frame != null && static_frame.isVisible();
	}

	private void enableRefreshButton() {
		if (!refreshButton.isEnabled()) {
			refreshButton.setEnabled(true);
		}
	}

	/**
	 * Set export component by determined which radio button is selected. If the
	 * method is triggered by sequence viewer, radio buttons panel will be set
	 * to invisible and set sequence viewer as export component.
	 *
	 * @param isSequenceViewer
	 */
	private void initRadioButton(boolean isSequenceViewer) {
		if (!isSequenceViewer) {
			initView();

			if (svRadioButton.isSelected()) {
				setComponent(slicedView);
			} else if (mvRadioButton.isSelected()) {
				setComponent(mainView);
			} else if (mvlRadioButton.isSelected()) {
				setComponent(mainViewWithLabels);
			} else {
				setComponent(wholeFrame);
				wfRadioButton.setSelected(true);
			}

			initImageInfo();

			mvRadioButton.setEnabled(!seqMap.getTiers().isEmpty());
			mvlRadioButton.setEnabled(!seqMap.getTiers().isEmpty());
			svRadioButton.setEnabled(!svseqMap.getTiers().isEmpty());
		}

		buttonsPanel.setVisible(!isSequenceViewer);
	}

	/**
	 * Initialize the reference components from different part views of IGB.
	 */
	private void initView() {
		if (seqMap == null) {
			seqMap = IGB.getSingleton().getMapView().getSeqMap();
			seqMap.addComponentListener(resizelistener);
			((NeoMap) seqMap).addRangeListener(rangeListener);

			wholeFrame = IGB.getSingleton().getFrame();

			mainView = seqMap.getNeoCanvas();

			AffyLabelledTierMap tm = (AffyLabelledTierMap) seqMap;
			mainViewWithLabels = tm.getSplitPane();

			AltSpliceView slice_view = (AltSpliceView) ((IGB) IGB.getSingleton()).getView(AltSpliceView.class.getName());
			slicedView = ((AffyLabelledTierMap) slice_view.getSplicedView().getSeqMap()).getSplitPane();
			slicedView.addComponentListener(resizelistener);
			svseqMap = slice_view.getSplicedView().getSeqMap();
			((NeoMap) slice_view.getSplicedView().getSeqMap()).addRangeListener(rangeListener);
		}
	}

	/**
	 * Initialize export panel.
	 */
	private void initFrame() {
		if (static_frame == null) {
			String file = exportNode.get(PREF_FILE, DEFAULT_FILE);
			if (file.equals(DEFAULT_FILE)) {
				exportFile = new File(defaultDir, file);
			} else {
				exportFile = new File(file);
			}

			defaultDir = new File(exportNode.get(PREF_DIR, FileTracker.EXPORT_DIR_TRACKER.getFile().getAbsolutePath()));

			imageInfo.setResolution(exportNode.getInt(PREF_RESOLUTION, imageInfo.getResolution()));

			filePathTextField.setText(exportFile.getAbsolutePath());

			ExportFileType type = getType(exportNode.get(PREF_EXT, DESCRIPTION[1]));
			extComboBox.setModel(new DefaultComboBoxModel(FILTER_LIST.keySet().toArray()));
			extComboBox.setSelectedItem(type);
			selectedExt = type.getExtension();
			resolutionComboBox.setSelectedItem(imageInfo.getResolution());

			unit = exportNode.get(PREF_UNIT, (String) UNIT[0]);
			unitComboBox.setSelectedItem(unit);

			initSpinner(unit);

			static_frame = PreferenceUtils.createFrame(TITLE, new ExportDialogGUI(this));
			static_frame.setLocationRelativeTo(IGB.getSingleton().getFrame());
		} else {
			initSpinner((String) unitComboBox.getSelectedItem());
		}
	}

	/**
	 * Bind two listeners(detect size and view changed) to current export seq
	 * view.
	 *
	 * @param seqView
	 */
	public void initSeqViewListener(Component seqView, Adjustable scroller) {
		seqView.addComponentListener(resizelistener);
		scroller.addAdjustmentListener(adjustmentlistener);
	}

	public FileFilter[] getAllExportFileFilters() {
		return FILTER_LIST.values().toArray(new FileFilter[FILTER_LIST.size()]);
	}
	
	/**
	 * Initialize height and width Spinner. Support Unit: pixels and Inches
	 *
	 * @param unit
	 */
	public void initSpinner(String unit) {
		double width = imageInfo.getWidth();
		double height = imageInfo.getHeight();

		if (unit.equals(UNIT[1])) {
			width /= imageInfo.getResolution();
			height /= imageInfo.getResolution();
		}

		SpinnerModel sm = new SpinnerNumberModel(width, 0, 10000, 1);
		widthSpinner.setModel(sm);
		sm = new SpinnerNumberModel(height, 0, 10000, 1);
		heightSpinner.setModel(sm);

		resetWidthHeight(width, height);
	}

	/**
	 * Set component to export: Whole frame, main view, main view with label,
	 * sliced view and seq view.
	 *
	 * @param c
	 */
	public void setComponent(Component c) {
		component = c;
	}

	/**
	 * Saved image information basis on component.
	 */
	public void initImageInfo() {
		if (imageInfo == null) {
			imageInfo = new ImageInfo(component.getWidth(), component.getHeight());
		} else {
			imageInfo.setWidth(component.getWidth());
			imageInfo.setHeight(component.getHeight());
		}
	}

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

	/**
	 * Passed file and changed its extension.
	 *
	 * @param file
	 * @param extension
	 * @return file with new extension
	 */
	public File changeFileExtension(File file, String extension) {
		if ((file == null) || (extension == null) || extension.trim().isEmpty()) {
			return null;
		}

		String path = file.getAbsolutePath();
		String ext = GeneralUtils.getExtension(path);
		if (ext.equalsIgnoreCase(extension)) {
			return file;
		}

		String filename = file.getName().trim();

		if (filename != null && !filename.isEmpty()) {

			int periodIndex = path.lastIndexOf('.');

			if (periodIndex > 0) {
				path = path.substring(0, periodIndex) + extension;
			} else {
				path += extension;
			}
		}

		return new File(path);
	}

	public void cancelButtonActionPerformed() {
		static_frame.setVisible(false);
	}

	/**
	 * Display a file chooser panel and let user choose output path.
	 *
	 * @param panel
	 */
	public void browseButtonActionPerformed(JPanel panel) {
		if (fileFilter == null) {
			fileFilter = new File(defaultDir, "export");
		}

		if (extFilter == null) {
			extFilter = getFilter(selectedExt);
		}

		fileChooser = new ExportFileChooser(defaultDir, fileFilter, extFilter, this);
		fileChooser.setDialogTitle("Save view as...");
		fileChooser.showDialog(panel, "Select");

		if (fileChooser.getSelectedFile() != null) {
			String newPath = fileChooser.getSelectedFile().getAbsolutePath();
			fileFilter = fileChooser.getSelectedFile();
			defaultDir = fileChooser.getCurrentDirectory();
			extFilter = fileChooser.getFileFilter();
			String ext = GeneralUtils.getExtension(newPath);
			String des = fileChooser.getFileFilter().getDescription();

			// the output file extension is based on user's input
			if (isExt(ext) && !des.equals(getDescription(ext))) {
				des = getDescription(ext);
			}

			ExportFileType type = getType(des);
			extComboBox.setSelectedItem(type);
			resetPath(newPath);
		}
	}

	/**
	 * Reset export file and file path text field to the new path.
	 *
	 * @param path
	 */
	private void resetPath(String path) {
		exportFile = new File(path);
		filePathTextField.setText(path);
	}

	/**
	 * Get export file type by passed image format description.
	 */
	private ExportFileType getType(String description) {
		for (ExportFileType type : FILTER_LIST.keySet()) {
			if (type.getDescription().equals(description)) {
				return type;
			}
		}

		return null;
	}

	/**
	 * Get export filter by passed image format extension.
	 */
	private ExportFileFilter getFilter(String ext) {
		for (ExportFileFilter filter : FILTER_LIST.values()) {
			if (filter.getExtension().equals(ext)) {
				return filter;
			}
		}

		return null;
	}

	/**
	 * Get image format description by passed image format extension.
	 */
	private String getDescription(String ext) {
		if (ext.equalsIgnoreCase(EXTENSION[0])) {
			return DESCRIPTION[0];
		} else if (ext.equalsIgnoreCase(EXTENSION[1])) {
			return DESCRIPTION[1];
		} else {
			return DESCRIPTION[2];
		}
	}

	/**
	 * Start export process when OK Button action performed.
	 */
	public void okButtonActionPerformed() throws IOException {
		String previousPath = exportFile.getAbsolutePath();
		String newPath = filePathTextField.getText();
		exportFile = new File(newPath);

		if (!isValidExportFile(previousPath)) {
			return;
		}

		String path = exportFile.getAbsolutePath();
		String ext = GeneralUtils.getExtension(path);

		if (!isExt(ext)) {
			ext = selectedExt;

			int index = path.lastIndexOf('.');
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

		if (exportFile.exists()) {
			if (!isOverwrite()) {
				return;
			}
		}

		exportScreenshot(exportFile, selectedExt, false);

		String des = getDescription(ext);
		extComboBox.setSelectedItem(getType(des));

		exportNode.put(PREF_FILE, path);
		exportNode.put(PREF_EXT, des);
		exportNode.put(PREF_DIR, defaultDir.getAbsolutePath());
		exportNode.putInt(PREF_RESOLUTION, imageInfo.getResolution());
		exportNode.put(PREF_UNIT, unit);

		static_frame.setVisible(false);
	}

	/**
	 * Test whether the current export path is valid or not. If not, reset
	 * current export path to previous export path and return false. Also, test
	 * whether the image size is valid or not.
	 */
	private boolean isValidExportFile(String previousPath) {
		if (exportFile.getParentFile() == null || !exportFile.getParentFile().isDirectory()) {
			// if output path is invalid, reset to previous correct path
			ErrorHandler.errorPanel("The output path is invalid.");
			resetPath(previousPath);
			filePathTextField.grabFocus();
			return false;
		}

		// if image size is too large...
		long heapFreeSize = Runtime.getRuntime().freeMemory();
		long size = (long) imageInfo.getWidth() * (long) imageInfo.getHeight();
		if (size > heapFreeSize) {
			ErrorHandler.errorPanel("The image size is invalid.");
			return false;
		}

		return true;
	}

	/**
	 * Give the user the choice to overwrite the existing file or not.
	 */
	private boolean isOverwrite() {
		String[] options = {"Yes", "No"};
		if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
				null, "Overwrite Existing File?", "File Exists",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				options, options[1])) {
			return true;
		}

		return false;
	}

	/**
	 * Export image to passed file by passed extension.
	 *
	 * @param f
	 * @param ext
	 * @param isScript (is from command line or not)
	 * @throws IOException
	 */
	public void exportScreenshot(File f, String ext, boolean isScript) throws IOException {
		if (ext.equals(EXTENSION[0])) {
			svgProperties.setProperty(SVGGraphics2D.class.getName() + ".ImageSize",
					(int) imageInfo.getWidth() + ", " + (int) imageInfo.getHeight());
			svgExport.exportToFile(f, component, null, svgProperties, "");
		} else {
			// From Script Loader, need to initialize the export image
			if (isScript) {
				exportImage = GraphicsUtil.getDeviceCompatibleImage(
						component.getWidth(), component.getHeight());
				Graphics g = exportImage.createGraphics();
				component.paintAll(g);
				initImageInfo();
				imageInfo.setResolution(exportNode.getInt(PREF_RESOLUTION, imageInfo.getResolution()));
			}
			
			exportImage = GraphicsUtil.resizeImage(exportImage, (int) imageInfo.getWidth(), (int) imageInfo.getHeight());
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
					writer.write(metadata, new IIOImage(exportImage, null, metadata), writeParam);
				} finally {
					stream.close();
				}
				break;
			}
		}
	}

	/**
	 * Creates a new buffered image by component and reset label's icon.
	 */
	public void previewImage() {
		exportImage = GraphicsUtil.getDeviceCompatibleImage(
				component.getWidth(), component.getHeight());
		Graphics2D g = exportImage.createGraphics();
		if(component instanceof JFrame){
			drawTitleBar( g);
		}
		component.printAll(g);

		Image previewImage = GraphicsUtil.resizeImage(exportImage,
				previewLabel.getWidth(), previewLabel.getHeight());

		previewLabel.setIcon(new ImageIcon(previewImage));
	}

	/**
	 * A hack to force export to draw title bar
	 * @param g 
	 */
	private void drawTitleBar(Graphics2D g) {
		// Draw Background
		g.setColor(component.getBackground().darker());
		g.fillRect(0, 0, component.getWidth(), component.getHeight());
		
		// Draw Border
		g.setColor(Color.BLACK);
		g.fillRect(0, 20, component.getWidth(), 2);
		
		// Draw Title
		g.setFont(g.getFont().deriveFont(FONT_SIZE));
		int x_offset = (component.getWidth() - g.getFontMetrics().stringWidth(((JFrame)component).getTitle()))/2;
		int y_offset = 14;
		g.drawString(((JFrame)component).getTitle(), x_offset, y_offset);
	}
		
	/**
	 * Return whether the passed extention is contained in IGB support image
	 * extention list or not.
	 */
	public boolean isExt(String ext) {
		for (String s : EXTENSION) {
			if (s.equalsIgnoreCase(ext)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Reset height Spinner's value when width Spinner changed.
	 */
	public void widthSpinnerStateChanged() {
		if (!isHeightSpinner) {
			double newWidth = ((Double) widthSpinner.getValue()).doubleValue();
			double newHeight = newWidth * imageInfo.getHeightWidthRate();

			isWidthSpinner = true;
			heightSpinner.setValue(newHeight);
			isWidthSpinner = false;

			resetWidthHeight(newWidth, newHeight);
		}
	}

	/**
	 * Reset width Spinner's value when height Spinner changed.
	 */
	public void heightSpinnerStateChanged() {
		if (!isWidthSpinner) {
			double newHeight = ((Double) heightSpinner.getValue()).doubleValue();
			double newWidth = newHeight * imageInfo.getWidthHeightRate();

			isHeightSpinner = true;
			widthSpinner.setValue(newWidth);
			isHeightSpinner = false;

			resetWidthHeight(newWidth, newHeight);
		}
	}

	/**
	 * Update the file path text field value when image format combo box action
	 * performed.
	 */
	public void extComboBoxActionPerformed() {
		String path = filePathTextField.getText();
		String ext = GeneralUtils.getExtension(path);
		selectedExt = ((ExportFileType) extComboBox.getSelectedItem()).getExtension();

		int index = path.lastIndexOf('.');
		int length = 0;
		if (index > 0) {
			length = path.substring(index).length();
		}

		// keep user input extension, if it's not a support extension
		if (!isExt(ext) && length > 1) {
			return;
		} else {
			if (!ext.equalsIgnoreCase(selectedExt)) {
				if (selectedExt.equals(EXTENSION[2])
						&& ext.equalsIgnoreCase(EXTENSION[3])) { // special case for jpg
					return;
				}

				if (length > 0) {
					path = path.substring(0, index);
				}
				path += selectedExt;
				resetPath(path);
				filePathTextField.grabFocus();
			}
		}

		if (selectedExt.equals(ExportConstants.EXTENSION[0])) {
			mvRadioButton.setSelected(true);
			mvRadioButtonActionPerformed();
			wfRadioButton.setEnabled(false);
			resolutionComboBox.setEnabled(false);
		} else {
			wfRadioButton.setEnabled(true);
			resolutionComboBox.setEnabled(true);
		}
	}

	/**
	 * Update width and height spinner's value when unit combo box action
	 * performed.
	 *
	 * Reset width spinner will trigger to reset height spinner.
	 */
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

	/**
	 * Reset size label text by passed width and height.
	 *
	 * @param width
	 * @param height
	 */
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

			// Allow user to reset width and height back to current size
			enableRefreshButton();
		}
	}

	/**
	 * Update output image resolution value when resolution combo box action
	 * performed. If selected unit is 'inches', update width and height
	 * spinner's value(reset width spinner will trigger to reset height
	 * spinner).
	 */
	public void resolutionComboBoxActionPerformed() {
		imageInfo.setResolution((Integer) resolutionComboBox.getSelectedItem());

		if (unit.equals(UNIT[1])) {
			double width = imageInfo.getWidth();
			width /= imageInfo.getResolution();
			widthSpinner.setValue(width);
		}
	}

	public void refreshButtonActionPerformed() {
		refreshPreview();
		refreshButton.setEnabled(false);
	}

	public void mvRadioButtonActionPerformed() {
		component = mainView;
		refreshPreview();
	}

	public void mvlRadioButtonActionPerformed() {
		component = mainViewWithLabels;
		refreshPreview();
	}

	public void wfRadioButtonActionPerformed() {
		component = wholeFrame;
		refreshPreview();
	}

	public void svRadioButtonActionPerformed() {
		component = slicedView;
		refreshPreview();
	}

	private void refreshPreview() {
		initImageInfo();
		initSpinner((String) unitComboBox.getSelectedItem());
		previewImage();
	}
}
